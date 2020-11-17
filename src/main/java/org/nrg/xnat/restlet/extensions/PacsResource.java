/*
 * org.nrg.xnat.restlet.extensions.PacsResource
 * DQR is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.nrg.xnat.restlet.extensions;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.nrg.dqr.domain.entities.Pacs;
import org.nrg.xdat.security.helpers.Roles;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.restlet.XnatRestlet;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.StringRepresentation;
import org.restlet.resource.Variant;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.zip.DataFormatException;

@XnatRestlet("/pacs/{PACS_ID}")
@Slf4j
public class PacsResource extends PacsAdminResource {
    public PacsResource(final Context context, final Request request, final Response response) {
        super(context, request, response);
    }

    @Override
    public Representation represent(final Variant variant) {
        try {
            final Pacs pacs = retrievePacs();
            return jsonRepresentation(pacs);
        } catch (final PacsNotFoundException e) {
            respondWithPacsNotFound();
        } catch (DataFormatException e) {
            respondWithInvalidPacsId(e.getMessage());
        }
        return null;
    }

    @Override
    public boolean allowPut() {
        return true;
    }

    @Override
    public void handlePut() {
        final UserI user = getUser();
        if (Roles.isSiteAdmin(user)) {
            try {
                final Pacs pacs = retrievePacs();
                buildPacsFromRequest(pacs);
                getPacsEntityService().update(pacs);
                respondWithSuccessNoContent();
            } catch (final PacsNotFoundException e) {
                respondWithPacsNotFound();
            } catch (final InvalidRequestBodyException e) {
                respondWithInvalidRequestBody();
            } catch (final ConstraintViolationException e) {
                respondWithDuplicateAeError(e);
            } catch (final DataIntegrityViolationException e) {
                respondWithDataIntegrityError(e);
            } catch (DataFormatException e) {
                e.printStackTrace();
            }
        } else {
            final String message = String.format("User %s is not an administrator and can't edit or create PACs configurations.", user.getUsername());
            log.info(message);
            getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN, message);
        }
    }

    @Override
    public boolean allowDelete() {
        return true;
    }

    @Override
    public void handleDelete() {
        final UserI user = getUser();
        if (Roles.isSiteAdmin(user)) {
            try {
                final Pacs pacs = retrievePacs();
                getPacsEntityService().delete(pacs);
                getPacsAvailabilityEntityService().deleteAllForPacs(pacs.getId());
                respondWithSuccessNoContent();
            } catch (final PacsNotFoundException e) {
                respondWithPacsNotFound();
            } catch (DataFormatException e) {
                respondWithInvalidPacsId(e.getMessage());
            }
        } else {
            final String message = String.format("User %s is not an administrator and can't delete PACs configurations.", user.getUsername());
            log.info(message);
            getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN, message);
        }
    }

    private Pacs retrievePacs() throws PacsNotFoundException, DataFormatException {
        final String pacsId = getPacsId();
        try {
            final long parsed   = Long.parseLong(pacsId);
            final Pacs pacs = getPacsEntityService().retrieve(parsed);
            if (null == pacs) {
                throw new PacsNotFoundException(parsed);
            }
            return pacs;
        } catch (final NumberFormatException e) {
            throw new DataFormatException(pacsId);
        }
    }

    private Representation jsonRepresentation(final Pacs pacs) {
        try {
            return new StringRepresentation(String.format(FORMAT, writeValue(pacs)), MediaType.APPLICATION_JSON);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void respondWithPacsNotFound() {
        getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND, "No PACS were found that match this request.");
    }

    private void respondWithInvalidPacsId(final String pacsId) {
        getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND, "The PACS ID submitted is invalid: " + pacsId);
    }

    private void respondWithSuccessNoContent() {
        getResponse().setStatus(Status.SUCCESS_NO_CONTENT, "The operation was successful.");
    }

    private static final String FORMAT = "{\"ResultSet\":{\"Result\": %s}}";
}

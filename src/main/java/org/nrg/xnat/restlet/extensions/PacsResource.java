/*
 * org.nrg.xnat.restlet.extensions.PacsResource
 * TIP is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.nrg.xnat.restlet.extensions;

import javax.validation.ConstraintViolationException;

import org.apache.log4j.Logger;
import org.nrg.tip.domain.entities.Pacs;
import org.nrg.xdat.security.helpers.Roles;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.restlet.XnatRestlet;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.StringRepresentation;
import org.restlet.resource.Variant;
import org.springframework.dao.DataIntegrityViolationException;

@XnatRestlet("/pacs/{PACS_ID}")
public class PacsResource extends PacsAdminResource {
    static Logger logger = Logger.getLogger(PacsResource.class);

    public PacsResource(final Context context, final Request request, final Response response) {
        super(context, request, response);
    }

    @Override
    public Representation represent(final Variant variant) throws ResourceException {
        try {
            final Pacs pacs = retrievePacs();
            return jsonRepresentation(pacs);
        } catch (final PacsNotFoundException e) {
            respondWithPacsNotFound();
            return null;
        }
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
                respondWithEntityValidationError(e);
            } catch (final DataIntegrityViolationException e) {
                respondWithDataIntegrityError(e);
            }
        }
        else{
            final String message = String.format("User %s is not an administrator and can't edit or create PACs configurations.", user.getUsername());
            logger.info(message);
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
                respondWithSuccessNoContent();
            } catch (final PacsNotFoundException e) {
                respondWithPacsNotFound();
            }
        }
        else{
            final String message = String.format("User %s is not an administrator and can't delete PACs configurations.", user.getUsername());
            logger.info(message);
            getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN, message);
        }
    }

    private Pacs retrievePacs() throws PacsNotFoundException {
        try {
            final Pacs pacs = getPacsEntityService().retrieve(Long.valueOf(getPacsId()));
            if (null == pacs) {
                throw new PacsNotFoundException();
            }
            return pacs;
        } catch (final NumberFormatException e) {
            throw new PacsNotFoundException();
        }
    }

    private Representation jsonRepresentation(final Pacs pacs) {
        Representation r;
        try {
            r = new StringRepresentation("{\"ResultSet\":{\"Result\":" + getObjectMapper().writeValueAsString(pacs)
                    + "}}");
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
        r.setMediaType(MediaType.APPLICATION_JSON);
        return r;
    }

    private void respondWithPacsNotFound() {
        getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND, "No PACS were found that match this request.");
    }

    private void respondWithSuccessNoContent() {
        getResponse().setStatus(Status.SUCCESS_NO_CONTENT, "The operation was successful.");
    }
}

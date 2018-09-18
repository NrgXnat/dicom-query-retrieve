/*
 * org.nrg.xnat.restlet.extensions.PacsListResource
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

import java.util.List;

@XnatRestlet("/pacs")
@Slf4j
public class PacsListResource extends PacsAdminResource {
    public PacsListResource(final Context context, final Request request, final Response response) {
        super(context, request, response);
    }

    @Override
    public Representation represent(final Variant variant) {
        if(getUser().isGuest()){
            getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN, "You must be logged in to query a PACS.");
            return null;
        }
        boolean checkStorable = this.getQueryVariable("storable")!=null;
        boolean checkQueryable = this.getQueryVariable("queryable")!=null;

        if(checkQueryable) {
            if(checkStorable) {
                final List<Pacs> allPacs = getPacsEntityService().findAllQueryableAndStorable();
                return jsonRepresentation(allPacs);
            }
            else {
                final List<Pacs> allPacs = getPacsEntityService().findAllQueryable();
                return jsonRepresentation(allPacs);
            }

        }
        else {
            if (checkStorable) {
                final List<Pacs> allPacs = getPacsEntityService().findAllStorable();
                return jsonRepresentation(allPacs);
            } else {
                final List<Pacs> allPacs = getPacsEntityService().getAll();
                return jsonRepresentation(allPacs);
            }
        }

    }

    @Override
    public boolean allowPost() {
        return true;
    }

    @Override
    public void handlePost() {
        final UserI user = getUser();
        if (Roles.isSiteAdmin(user)) {
            try {
                final Pacs pacs = buildPacsFromRequest(null);
                getPacsEntityService().create(pacs);
                getResponse().setLocationRef("pacs/" + String.valueOf(pacs.getId()));
                respondWithSuccessCreated();
            } catch (final InvalidRequestBodyException e) {
                respondWithInvalidRequestBody();
            } catch (final DataIntegrityViolationException e) {
                respondWithDataIntegrityError(e);
            } catch (final ConstraintViolationException e) {
                respondWithDuplicateAeError(e);
            }
        }
        else{
            final String message = String.format("User %s is not an administrator and can't create PACs configurations.", user.getUsername());
            logger.info(message);
            getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN, message);
        }
    }

    private Representation jsonRepresentation(final List<Pacs> results) {
        Representation r;
        try {
            r = new StringRepresentation("{\"ResultSet\":{\"Result\":" + getObjectMapper().writeValueAsString(results)
                    + ", \"resultSetSize\":" + getObjectMapper().writeValueAsString(results.size()) + "}}");
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
        r.setMediaType(MediaType.APPLICATION_JSON);
        return r;
    }

    private void respondWithSuccessCreated() {
        getResponse().setStatus(Status.SUCCESS_CREATED,
                "The location header contains the URI of the newly created PACS.");
    }
}

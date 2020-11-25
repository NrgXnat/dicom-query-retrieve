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
import org.nrg.dqr.domain.entities.PacsAvailability;
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

import java.time.DayOfWeek;
import java.util.List;
import java.util.stream.IntStream;

@XnatRestlet("/pacs")
@Slf4j
public class PacsListResource extends PacsAdminResource {
    public PacsListResource(final Context context, final Request request, final Response response) {
        super(context, request, response);
    }

    @Override
    public Representation represent(final Variant variant) {
        if (getUser().isGuest()) {
            getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN, "You must be logged in to query a PACS.");
            return null;
        }

        final boolean checkStorable  = getQueryVariable("storable") != null;
        final boolean checkQueryable = getQueryVariable("queryable") != null;

        final List<Pacs> allPacs = checkQueryable ? (checkStorable ? getPacsEntityService().findAllQueryableAndStorable() : getPacsEntityService().findAllQueryable()) : (checkStorable ? getPacsEntityService().findAllStorable() : getPacsEntityService().getAll());
        log.debug("Got {} for queryable and {} for storable, found {} PACS for those properties", checkQueryable, checkStorable, allPacs.size());
        return jsonRepresentation(allPacs);
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
                final long pacsId = getPacsEntityService().create(buildPacsFromRequest(null)).getId();
                IntStream.range(1, 8).mapToObj(day -> PacsAvailability.builder().dayOfWeek(DayOfWeek.of(day)).pacsId(pacsId).threads(1).utilizationPercent(100).availabilityStart("00:00").availabilityEnd("00:00").build()).forEach(availability -> getPacsAvailabilityEntityService().create(availability));
                getResponse().setLocationRef("pacs/" + pacsId);
                respondWithSuccessCreated();
            } catch (final InvalidRequestBodyException e) {
                respondWithInvalidRequestBody();
            } catch (final DataIntegrityViolationException e) {
                respondWithDataIntegrityError(e);
            } catch (final ConstraintViolationException e) {
                respondWithDuplicateAeError(e);
            }
        } else {
            final String message = String.format("User %s is not an administrator and can't create PACs configurations.", user.getUsername());
            log.info(message);
            getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN, message);
        }
    }

    private Representation jsonRepresentation(final List<Pacs> results) {
        try {
            return new StringRepresentation(String.format(FORMAT, writeValue(results), writeValue(results.size())), MediaType.APPLICATION_JSON);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void respondWithSuccessCreated() {
        getResponse().setStatus(Status.SUCCESS_CREATED, "The location header contains the URI of the newly created PACS.");
    }

    private static final String FORMAT = "{\"ResultSet\":{\"Result\": %s, \"resultSetSize\": %s}}";
}

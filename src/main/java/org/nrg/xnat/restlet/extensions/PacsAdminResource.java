/*
 * org.nrg.xnat.restlet.extensions.PacsAdminResource
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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.apache.commons.lang.StringUtils;
import org.hibernate.exception.ConstraintViolationException;
import org.nrg.dqr.domain.entities.Pacs;
import org.nrg.dqr.services.PacsAvailabilityEntityService;
import org.nrg.dqr.services.PacsEntityService;
import org.nrg.xdat.XDAT;
import org.restlet.Context;
import org.restlet.data.*;
import org.restlet.resource.Variant;
import org.springframework.dao.DataIntegrityViolationException;

@Getter(AccessLevel.PROTECTED)
@Accessors(prefix = "_")
public abstract class PacsAdminResource extends PacsSerializingResource {
    public PacsAdminResource(final Context context, final Request request, final Response response) {
        super(context, request, response);
        getVariants().add(new Variant(MediaType.ALL));

        _pacsEntityService = XDAT.getContextService().getBean(PacsEntityService.class);
        _pacsAvailabilityEntityService = XDAT.getContextService().getBean(PacsAvailabilityEntityService.class);
    }

    protected Pacs buildPacsFromRequest(final Pacs incomingPacs) throws InvalidRequestBodyException {
        try {
            final Form requestForm = getRequest().getEntityAsForm();
            final Pacs pacs        = incomingPacs == null ? new Pacs() : incomingPacs;
            pacs.setAeTitle(requestForm.getFirstValue("aeTitle"));
            pacs.setHost(requestForm.getFirstValue("host"));
            pacs.setLabel(requestForm.getFirstValue("label"));
            pacs.setStorable(Boolean.parseBoolean(convertCheckboxToBoolean(requestForm.getFirstValue("storable"))));
            pacs.setDefaultStoragePacs(Boolean.valueOf(convertCheckboxToBoolean(requestForm.getFirstValue("defaultStoragePacs"))));
            pacs.setQueryable(Boolean.parseBoolean(convertCheckboxToBoolean(requestForm.getFirstValue("queryable"))));
            pacs.setQueryRetrievePort(null);
            if (!StringUtils.isBlank(requestForm.getFirstValue("queryRetrievePort"))) {
                pacs.setQueryRetrievePort(Integer.valueOf(requestForm.getFirstValue("queryRetrievePort")));
            }
            pacs.setDefaultQueryRetrievePacs(Boolean.valueOf(convertCheckboxToBoolean(requestForm.getFirstValue("defaultQueryRetrievePacs"))));
            pacs.setSupportsExtendedNegotiations(Boolean.parseBoolean(convertCheckboxToBoolean(requestForm.getFirstValue("extendedNegotiations"))));
            pacs.setOrmStrategySpringBeanId(requestForm.getFirstValue("ormStrategySpringBeanId"));
            return pacs;
        } catch (final NumberFormatException e) {
            throw new InvalidRequestBodyException();
        }
    }

    protected String getPacsId() {
        return (String) getParameter(getRequest(), "PACS_ID");
    }

    protected void respondWithInvalidRequestBody() {
        getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "The request body did not contain all of the PACS data fields, or they were not of the correct types.");
    }

    protected void respondWithDataIntegrityError(final DataIntegrityViolationException e) {
        getResponse().setStatus(Status.CLIENT_ERROR_CONFLICT, e, "Duplicate Entity Error");
        getResponse().setEntity("The specified application entity (AE) title already exists in the system.\n", MediaType.TEXT_PLAIN);
    }

    protected void respondWithDuplicateAeError(final ConstraintViolationException e) {
        getResponse().setStatus(Status.CLIENT_ERROR_CONFLICT, e, "Duplicate Entity Error");
        getResponse().setEntity("The specified application entity (AE) title already exists in the system.", MediaType.TEXT_PLAIN);
    }

    protected static class InvalidRequestBodyException extends Exception {
    }

    private String convertCheckboxToBoolean(final String checkboxValue) {
        return StringUtils.trimToEmpty(checkboxValue).equalsIgnoreCase("on") ? "true" : checkboxValue;
    }

    private final PacsEntityService             _pacsEntityService;
    private final PacsAvailabilityEntityService _pacsAvailabilityEntityService;
}

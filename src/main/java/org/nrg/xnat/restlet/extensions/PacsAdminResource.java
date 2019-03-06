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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.DefaultSerializerProvider;
import org.apache.commons.lang.StringUtils;
import org.hibernate.exception.ConstraintViolationException;
import org.nrg.dqr.domain.entities.Pacs;
import org.nrg.dqr.restlet.NullValueSerializer;
import org.nrg.dqr.services.PacsAvailabilityEntityService;
import org.nrg.dqr.services.PacsEntityService;
import org.nrg.xdat.XDAT;
import org.nrg.xnat.restlet.resources.SecureResource;
import org.restlet.Context;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Variant;
import org.springframework.dao.DataIntegrityViolationException;

public abstract class PacsAdminResource extends SecureResource {

    public PacsAdminResource(final Context context, final Request request, final Response response) {
        super(context, request, response);
        this.getVariants().add(new Variant(MediaType.ALL));
        pacsEntityService = initEntityService();
        pacsAvailabilityEntityService = initAvailabilityEntityService();
    }

    private PacsEntityService initEntityService() {
        return XDAT.getContextService().getBean(PacsEntityService.class);
    }

    private PacsAvailabilityEntityService initAvailabilityEntityService() {
        return XDAT.getContextService().getBean(PacsAvailabilityEntityService.class);
    }

    protected PacsEntityService getPacsEntityService() {
        return pacsEntityService;
    }

    protected PacsAvailabilityEntityService getPacsAvailabilityEntityService() {
        return pacsAvailabilityEntityService;
    }

    protected ObjectMapper getObjectMapper() {
        return MAPPER;
    }

    protected Pacs buildPacsFromRequest(Pacs pacs) throws InvalidRequestBodyException {
        try {
            final Form requestForm = getRequest().getEntityAsForm();
            if (pacs == null) {
                pacs = new Pacs();
            }
            pacs.setAeTitle(requestForm.getFirstValue("aeTitle"));
            pacs.setHost(requestForm.getFirstValue("host"));
            pacs.setLabel(requestForm.getFirstValue("label"));
            pacs.setStorable(Boolean.valueOf(convertCheckboxToBoolean(requestForm.getFirstValue("storable"))));
            pacs.setDefaultStoragePacs(Boolean.valueOf(convertCheckboxToBoolean(requestForm
                    .getFirstValue("defaultStoragePacs"))));

            pacs.setQueryable(Boolean.valueOf(convertCheckboxToBoolean(requestForm.getFirstValue("queryable"))));
            pacs.setQueryRetrievePort(null);
            if (!StringUtils.isBlank(requestForm.getFirstValue("queryRetrievePort"))) {
                pacs.setQueryRetrievePort(Integer.valueOf(requestForm.getFirstValue("queryRetrievePort")));
            }
            pacs.setDefaultQueryRetrievePacs(Boolean.valueOf(convertCheckboxToBoolean(requestForm.getFirstValue("defaultQueryRetrievePacs"))));
            pacs.setSupportsExtendedNegotiations(Boolean.valueOf(convertCheckboxToBoolean(requestForm.getFirstValue("extendedNegotiations"))));
            pacs.setOrmStrategySpringBeanId(requestForm.getFirstValue("ormStrategySpringBeanId"));
            return pacs;
        } catch (final NumberFormatException e) {
            throw new InvalidRequestBodyException();
        }
    }

    private String convertCheckboxToBoolean(final String checkboxValue) {
        if (StringUtils.trimToEmpty(checkboxValue).equalsIgnoreCase("on")) {
            return "true";
        } else {
            return checkboxValue;
        }
    }

    protected String getPacsId() {
        return (String) getParameter(getRequest(), "PACS_ID");
    }

    protected static class InvalidRequestBodyException extends Exception {
        private static final long serialVersionUID = 1L;
    }

    protected void respondWithInvalidRequestBody() {
        getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST,
                "The request body did not contain all of the PACS data fields, or they were not of the correct types.");
    }

    protected void respondWithDataIntegrityError(final DataIntegrityViolationException e) {
        getResponse().setStatus(Status.CLIENT_ERROR_CONFLICT, e, "Duplicate Entity Error");
        getResponse().setEntity(
                "The specified application entity (AE) title already exists in the system."
                        + System.getProperty("line.separator"), MediaType.TEXT_PLAIN);
    }

    protected void respondWithDuplicateAeError(final ConstraintViolationException e) {
        getResponse().setStatus(Status.CLIENT_ERROR_CONFLICT, e, "Duplicate Entity Error");
        getResponse().setEntity(
                "The specified application entity (AE) title already exists in the system."
                        + System.getProperty("line.separator"), MediaType.TEXT_PLAIN);
    }


    private static final ObjectMapper MAPPER = new ObjectMapper();
    static {
        final DefaultSerializerProvider provider = new DefaultSerializerProvider.Impl();
        provider.setNullValueSerializer(new NullValueSerializer());
        MAPPER.setSerializerProvider(provider);
    }

    private final PacsEntityService pacsEntityService;
    private final PacsAvailabilityEntityService pacsAvailabilityEntityService;
}

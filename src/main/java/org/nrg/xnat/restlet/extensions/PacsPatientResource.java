/*
 * org.nrg.xnat.restlet.extensions.PacsPatientResource
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

import org.nrg.dqr.domain.Patient;
import org.nrg.dqr.restlet.JsonViews;
import org.nrg.xdat.XDAT;
import org.nrg.xnat.restlet.XnatRestlet;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

@XnatRestlet("/services/pacs/{PACS_ID}/search/patients/{PATIENT_ID}")
public class PacsPatientResource extends PacsServiceResource {

    public PacsPatientResource(final Context context, final Request request, final Response response) {
        super(context, request, response);
    }

    @Override
    public Representation represent(final Variant variant) throws ResourceException {
        if(getUser().isGuest()){
            respondWithNeedToBeLoggedIn();
            return null;
        }
        try {
            final Patient patient = getPacsService().getPatientById(XDAT.getUserDetails(), getPacs(), getPatientId());
            if (null == patient) {
                respondWithNotFound();
                return null;
            } else {
                return jsonRepresentation(patient, JsonViews.PatientRootView.class);
            }
        } catch (final PacsNotFoundException e) {
            respondWithPacsNotFound();
            return null;
        }
    }

    private String getPatientId() {
        return (String) getParameter(getRequest(), "PATIENT_ID");
    }
}

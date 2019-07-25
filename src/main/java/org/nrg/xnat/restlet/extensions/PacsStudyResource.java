/*
 * org.nrg.xnat.restlet.extensions.PacsStudyResource
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

import org.nrg.dqr.domain.Study;
import org.nrg.dqr.restlet.JsonViews;
import org.nrg.dqr.preferences.DqrPreferences;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.security.helpers.Roles;
import org.nrg.xnat.restlet.XnatRestlet;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

@XnatRestlet("/services/pacs/{PACS_ID}/search/studies/{STUDY_ID}")
public class PacsStudyResource extends PacsServiceResource {

    public PacsStudyResource(final Context context, final Request request, final Response response) {
        super(context, request, response);
    }

    @Override
    public Representation represent(final Variant variant) throws ResourceException {
        try {
            if(getUser().isGuest()){
                respondWithNeedToBeLoggedIn();
                return null;
            }
            else if(!Roles.checkRole(getUser(),"Administrator") && !Roles.checkRole(getUser(),"Dqr") && !XDAT.getContextService().getBean(DqrPreferences.class).getAllowAllUsersToUseDqr()){
                getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN, "Your user does not have permission to search the PACS.");
                return null;
            }
            final Study study = getPacsService().getStudyById(XDAT.getUserDetails(), getPacs(), getStudyId());
            if (null == study) {
                respondWithNotFound();
                return null;
            } else {
                return jsonRepresentation(study, JsonViews.StudyRootView.class);
            }
        } catch (final PacsNotFoundException e) {
            respondWithPacsNotFound();
            return null;
        }
    }

    private String getStudyId() {
        return (String) getParameter(getRequest(), "STUDY_ID");
    }
}

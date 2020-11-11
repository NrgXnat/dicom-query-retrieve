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
import org.nrg.xdat.XDAT;
import org.nrg.xnat.restlet.XnatRestlet;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Representation;
import org.restlet.resource.Variant;

@XnatRestlet("/services/pacs/{PACS_ID}/search/studies/{STUDY_ID}")
public class PacsStudyResource extends PacsServiceResource {
    public PacsStudyResource(final Context context, final Request request, final Response response) {
        super(context, request, response);
    }

    @Override
    protected Representation representImpl(final Variant variant) {
        try {
            final Study study = getPacsService().getStudyById(XDAT.getUserDetails(), getPacs(), getStudyId());
            if (null == study) {
                respondWithNotFound();
                return null;
            }
            return jsonRepresentation(study, JsonViews.StudyRootView.class);
        } catch (final PacsNotFoundException e) {
            respondWithPacsNotFound();
            return null;
        }
    }

    private String getStudyId() {
        return (String) getParameter(getRequest(), "STUDY_ID");
    }
}

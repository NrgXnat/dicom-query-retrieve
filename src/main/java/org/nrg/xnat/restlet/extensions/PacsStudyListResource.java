/*
 * org.nrg.xnat.restlet.extensions.PacsStudyListResource
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

import org.nrg.tip.dicom.command.cfind.SearchCriteriaTooVagueException;
import org.nrg.tip.domain.Study;
import org.nrg.tip.dto.PacsSearchCriteria;
import org.nrg.tip.dto.PacsSearchResults;
import org.nrg.tip.restlet.InvalidStudyDateRangeException;
import org.nrg.tip.restlet.JsonViews;
import org.nrg.tip.restlet.RequestUtils;
import org.nrg.xdat.XDAT;
import org.nrg.xnat.restlet.XnatRestlet;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

@XnatRestlet("/services/pacs/{PACS_ID}/search/studies")
public class PacsStudyListResource extends PacsServiceResource {

    public PacsStudyListResource(final Context context, final Request request, final Response response) {
        super(context, request, response);
    }

    @Override
    public boolean allowGet() {
        // must query w/ POST -- don't want patient names in browser history/server logs/other unwanted places
        return false;
    }

    @Override
    public Representation represent(final Variant variant) throws ResourceException {
        try {
            final PacsSearchCriteria searchCriteria = RequestUtils.buildSearchCriteriaFromRequest(getRequest());
            final PacsSearchResults<String, Study> studies = getPacsService().getStudiesByExample(
                    XDAT.getUserDetails(), getPacs(), searchCriteria);
            if (0 == studies.getResults().size()) {
                respondWithNotFound();
                return null;
            } else {
                return jsonRepresentation(studies, JsonViews.StudyRootView.class);
            }
        } catch (final PacsNotFoundException e) {
            respondWithPacsNotFound();
            return null;
        } catch (final InvalidStudyDateRangeException e) {
            respondWithBadRequest(e.getMessage());
            return null;
        } catch (final SearchCriteriaTooVagueException e) {
            respondWithBadRequest("Please specify at least one of the patient search criteria.");
            return null;
        }
    }

    @Override
    public boolean allowPost() {
        return true;
    }

    @Override
    public void handlePost() {
        returnDefaultRepresentation();
    }
}

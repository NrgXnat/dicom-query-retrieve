/*
 * org.nrg.xnat.restlet.extensions.PacsStudySeriesList
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

import org.nrg.dqr.domain.Series;
import org.nrg.dqr.domain.Study;
import org.nrg.dqr.dto.PacsSearchResults;
import org.nrg.dqr.restlet.JsonViews;
import org.nrg.xnat.restlet.XnatRestlet;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Representation;
import org.restlet.resource.Variant;

import java.util.zip.DataFormatException;

@XnatRestlet("/services/pacs/{PACS_ID}/search/studies/{STUDY_ID}/series")
public class PacsStudySeriesList extends PacsServiceResource {
    public PacsStudySeriesList(final Context context, final Request request, final Response response) {
        super(context, request, response);
    }

    @Override
    protected Representation representImpl(final Variant variant) {
        try {
            final Study                             study  = buildStudyFromRequest();
            final PacsSearchResults<String, Series> series = getPacsService().getSeriesByStudy(getUser(), getPacs(), study);
            if (series.getResults().isEmpty()) {
                respondWithNotFound();
                return null;
            }
            return jsonRepresentation(series, JsonViews.SeriesRootView.class);
        } catch (final PacsNotFoundException e) {
            respondWithPacsNotFound();
        } catch (DataFormatException e) {
            respondWithInvalidPacsId(e.getMessage());
        }
        return null;
    }

    private Study buildStudyFromRequest() {
        final Study study = new Study();
        study.setStudyInstanceUid((String) getParameter(getRequest(), "STUDY_ID"));
        return study;
    }
}

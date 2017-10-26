/*
 * org.nrg.tip.services.PacsService
 * TIP is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.nrg.tip.services;

import org.nrg.tip.domain.Patient;
import org.nrg.tip.domain.Series;
import org.nrg.tip.domain.Study;
import org.nrg.tip.domain.entities.Pacs;
import org.nrg.tip.dto.PacsSearchCriteria;
import org.nrg.tip.dto.PacsSearchResults;
import org.nrg.xdat.om.XnatImagescandata;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xft.security.UserI;

public interface PacsService {

    PacsSearchResults<String, Patient> getPatientsByExample(UserI user, final Pacs pacs,
                                                            final PacsSearchCriteria searchCriteria);

    Patient getPatientById(UserI user, final Pacs pacs, String patientId);

    PacsSearchResults<String, Study> getStudiesByExample(UserI user, final Pacs pacs,
                                                         final PacsSearchCriteria searchCriteria);

    Study getStudyById(UserI user, final Pacs pacs, final String studyInstanceUid);

    PacsSearchResults<String, Series> getSeriesByStudy(UserI user, final Pacs pacs, final Study study);

    Series getSeriesById(UserI user, final Pacs pacs, final String seriesInstanceUid);

    void importSeries(UserI user, final Pacs pacs, final Study study, final Series series);

    void exportSeries(UserI user, final Pacs pacs, final XnatImagescandata series);
}

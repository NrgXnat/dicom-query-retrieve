/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.dicom.command.cfind.dcm4che.tool.CFindSCUSeriesLevelByStudy
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.dicom.command.cfind.dcm4che.tool;

import org.apache.commons.lang3.StringUtils;
import org.nrg.xnatx.dqr.dicom.command.cecho.CEchoSCU;
import org.nrg.xnatx.dqr.dicom.command.cfind.SearchCriteriaTooVagueException;
import org.nrg.xnatx.dqr.dicom.net.DicomConnectionProperties;
import org.nrg.xnatx.dqr.dicom.strategy.orm.OrmStrategy;
import org.nrg.xnatx.dqr.dto.PacsSearchCriteria;
import org.nrg.xnatx.dqr.preferences.DqrPreferences;
import org.nrg.xnatx.dqr.services.SeriesRetrievalRequestService;

public class CFindSCUSeriesLevelByStudy extends CFindSCUSeriesLevel {

    public CFindSCUSeriesLevelByStudy(final DqrPreferences preferences,
                                      final DicomConnectionProperties dicomConnectionProperties,
                                      final CEchoSCU cechoSCU,
                                      final OrmStrategy ormStrategy,
                                      final SeriesRetrievalRequestService seriesRetrievalRequestService) {
        super(preferences, dicomConnectionProperties, cechoSCU, ormStrategy, seriesRetrievalRequestService);
    }

    @Override
    protected void validatePacsSearchCriteria(final PacsSearchCriteria searchCriteria)
        throws SearchCriteriaTooVagueException {
        if (StringUtils.isBlank(searchCriteria.getStudyInstanceUid())) {
            throw new SearchCriteriaTooVagueException();
        }
    }
}

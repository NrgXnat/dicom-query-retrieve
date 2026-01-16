/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.dicom.command.cmove.dcm4che.tool.CFindSCUSeriesLevelByIdWithCMove
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2024, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.dicom.command.cmove.dcm4che.tool;

import org.apache.commons.lang3.StringUtils;
import org.dcm4che3.data.Attributes;
import org.nrg.xnatx.dqr.dicom.command.cecho.CEchoSCU;
import org.nrg.xnatx.dqr.dicom.command.cfind.SearchCriteriaTooVagueException;
import org.nrg.xnatx.dqr.dicom.command.cfind.dcm4che.tool.CFindSCUSeriesLevel;
import org.nrg.xnatx.dqr.dicom.net.DicomConnectionProperties;
import org.nrg.xnatx.dqr.dicom.strategy.orm.OrmStrategy;
import org.nrg.xnatx.dqr.dto.PacsSearchCriteria;
import org.nrg.xnatx.dqr.preferences.DqrPreferences;

import java.util.List;

/**
 * C-FIND at SERIES level with C-MOVE on results.
 * Note: C-MOVE functionality requires dcm4che3 implementation - currently throws UnsupportedOperationException.
 */
public class CFindSCUSeriesLevelByIdWithCMove extends CFindSCUSeriesLevel {

    public CFindSCUSeriesLevelByIdWithCMove(final DqrPreferences preferences,
                                             final DicomConnectionProperties dicomConnectionProperties,
                                             final CEchoSCU cechoSCU,
                                             final OrmStrategy ormStrategy) {
        super(preferences, dicomConnectionProperties, cechoSCU, ormStrategy);
    }

    @Override
    protected void validatePacsSearchCriteria(final PacsSearchCriteria searchCriteria)
            throws SearchCriteriaTooVagueException {
        if (StringUtils.isBlank(searchCriteria.getSeriesInstanceUid())) {
            throw new SearchCriteriaTooVagueException();
        }
    }

    @Override
    protected boolean cMoveRequestedOnResults() {
        return true;
    }

    @Override
    protected void performCMoveOnResults(final PacsSearchCriteria searchCriteria, final List<Attributes> dicomResults) {
        // TODO: Implement C-MOVE using dcm4che3 API
        throw new UnsupportedOperationException("C-MOVE not yet implemented for dcm4che3. " +
                "Use DICOMweb WADO-RS for retrieval or implement Dcm4che3CMoveSCU.");
    }
}

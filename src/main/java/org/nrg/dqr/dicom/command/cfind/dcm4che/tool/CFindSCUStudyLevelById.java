/*
 * CFindSCUStudyLevelById
 * DQR is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.nrg.dqr.dicom.command.cfind.dcm4che.tool;

import org.apache.commons.lang3.StringUtils;
import org.nrg.dqr.dicom.command.cfind.SearchCriteriaTooVagueException;
import org.nrg.dqr.dicom.net.DicomConnectionProperties;
import org.nrg.dqr.dicom.strategy.orm.OrmStrategy;
import org.nrg.dqr.dto.PacsSearchCriteria;
import org.nrg.dqr.dicom.command.cecho.CEchoSCU;
import org.nrg.dqr.preferences.DqrPreferences;

public class CFindSCUStudyLevelById extends CFindSCUStudyLevel {

    public CFindSCUStudyLevelById(final DqrPreferences preferences, final DicomConnectionProperties dicomConnectionProperties, final CEchoSCU cechoSCU,
                                  final OrmStrategy ormStrategy) {
        super(preferences, dicomConnectionProperties, cechoSCU, ormStrategy);
    }

    @Override
    protected void validatePacsSearchCriteria(final PacsSearchCriteria searchCriteria)
            throws SearchCriteriaTooVagueException {
        if (StringUtils.isBlank(searchCriteria.getStudyInstanceUid())) {
            throw new SearchCriteriaTooVagueException();
        }
    }
}

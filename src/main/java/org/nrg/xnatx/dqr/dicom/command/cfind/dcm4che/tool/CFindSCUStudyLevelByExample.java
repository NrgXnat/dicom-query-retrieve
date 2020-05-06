/*
 * CFindSCUStudyLevelByExample
 * DQR is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.nrg.xnatx.dqr.dicom.command.cfind.dcm4che.tool;

import org.nrg.xnatx.dqr.dicom.command.cfind.SearchCriteriaTooVagueException;
import org.nrg.xnatx.dqr.dicom.net.DicomConnectionProperties;
import org.nrg.xnatx.dqr.dicom.strategy.orm.OrmStrategy;
import org.nrg.xnatx.dqr.dto.PacsSearchCriteria;
import org.nrg.xnatx.dqr.dicom.command.cecho.CEchoSCU;

import org.nrg.xnatx.dqr.preferences.DqrPreferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CFindSCUStudyLevelByExample extends CFindSCUStudyLevel {

    private final static Logger log = LoggerFactory.getLogger(CFindSCUStudyLevelByExample.class);

    public CFindSCUStudyLevelByExample(final DqrPreferences preferences, final DicomConnectionProperties dicomConnectionProperties,
                                       final CEchoSCU cechoSCU, final OrmStrategy ormStrategy) {
        super(preferences, dicomConnectionProperties, cechoSCU, ormStrategy);
    }

    @Override
    protected void validatePacsSearchCriteria(final PacsSearchCriteria searchCriteria)
            throws SearchCriteriaTooVagueException {
        if (!getOrmStrategy().getResultSetLimitStrategy().searchCriteriaIsSufficientlySpecific(searchCriteria)) {
            throw new SearchCriteriaTooVagueException();
        }
    }
}

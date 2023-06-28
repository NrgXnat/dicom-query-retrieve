/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.dicom.command.cfind.dcm4che.tool.CFindSCUPatientLevelByExample
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.dicom.command.cfind.dcm4che.tool;

import org.nrg.xnatx.dqr.dicom.command.cecho.CEchoSCU;
import org.nrg.xnatx.dqr.dicom.command.cfind.SearchCriteriaTooVagueException;
import org.nrg.xnatx.dqr.dicom.net.DicomConnectionProperties;
import org.nrg.xnatx.dqr.dicom.strategy.orm.OrmStrategy;
import org.nrg.xnatx.dqr.dto.PacsSearchCriteria;
import org.nrg.xnatx.dqr.preferences.DqrPreferences;

public class CFindSCUPatientLevelByExample extends CFindSCUPatientLevel {

    public CFindSCUPatientLevelByExample(final DqrPreferences preferences,
                                         final DicomConnectionProperties dicomConnectionProperties,
                                         final CEchoSCU cechoSCU,
                                         final OrmStrategy ormStrategy) {
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

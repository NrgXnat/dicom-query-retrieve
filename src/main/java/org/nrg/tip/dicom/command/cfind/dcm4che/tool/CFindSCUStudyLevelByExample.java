/*
 * org.nrg.tip.dicom.command.cfind.dcm4che.tool.CFindSCUStudyLevelByExample
 * TIP is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.nrg.tip.dicom.command.cfind.dcm4che.tool;

import org.nrg.tip.dicom.command.cecho.CEchoSCU;
import org.nrg.tip.dicom.command.cfind.SearchCriteriaTooVagueException;
import org.nrg.tip.dicom.net.DicomConnectionProperties;
import org.nrg.tip.dicom.strategy.orm.OrmStrategy;
import org.nrg.tip.dto.PacsSearchCriteria;

public class CFindSCUStudyLevelByExample extends CFindSCUStudyLevel {

    public CFindSCUStudyLevelByExample(final DicomConnectionProperties dicomConnectionProperties,
                                       final CEchoSCU cechoSCU, final OrmStrategy ormStrategy) {
        super(dicomConnectionProperties, cechoSCU, ormStrategy);
    }

    @Override
    protected void validatePacsSearchCriteria(final PacsSearchCriteria searchCriteria)
            throws SearchCriteriaTooVagueException {
        if (!getOrmStrategy().getResultSetLimitStrategy().searchCriteriaIsSufficientlySpecific(searchCriteria)) {
            throw new SearchCriteriaTooVagueException();
        }
    }
}

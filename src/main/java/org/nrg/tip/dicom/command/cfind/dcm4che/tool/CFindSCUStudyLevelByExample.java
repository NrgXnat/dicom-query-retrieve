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

import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.nrg.tip.dicom.command.cecho.CEchoSCU;
import org.nrg.tip.dicom.command.cfind.SearchCriteriaTooVagueException;
import org.nrg.tip.dicom.net.DicomConnectionProperties;
import org.nrg.tip.dicom.strategy.orm.DicomPersonNameSearchCriteria;
import org.nrg.tip.dicom.strategy.orm.OrmStrategy;
import org.nrg.tip.domain.Study;
import org.nrg.tip.dto.PacsSearchCriteria;
import org.nrg.tip.dto.PacsSearchResults;
import org.nrg.tip.util.TipRuntimeException;
import org.apache.commons.lang.StringUtils;

import java.util.List;
import org.dcm4che2.tool.dcmqr.DcmQR;
import org.dcm4che2.tool.dcmqr.DcmQR.QueryRetrieveLevel;
import org.nrg.xnat.utils.DateRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CFindSCUStudyLevelByExample extends CFindSCUStudyLevel {

    private final static Logger log = LoggerFactory.getLogger(CFindSCUStudyLevelByExample.class);

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

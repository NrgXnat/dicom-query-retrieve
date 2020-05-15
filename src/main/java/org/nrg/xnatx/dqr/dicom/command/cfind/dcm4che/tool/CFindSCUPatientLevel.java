/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.dicom.command.cfind.dcm4che.tool.CFindSCUPatientLevel
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.dicom.command.cfind.dcm4che.tool;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.tool.dcmqr.DcmQR.QueryRetrieveLevel;
import org.nrg.xnatx.dqr.dicom.command.cecho.CEchoSCU;
import org.nrg.xnatx.dqr.dicom.net.DicomConnectionProperties;
import org.nrg.xnatx.dqr.dicom.strategy.orm.OrmStrategy;
import org.nrg.xnatx.dqr.domain.Patient;
import org.nrg.xnatx.dqr.dto.PacsSearchResults;
import org.nrg.xnatx.dqr.dto.StudyDateRangeLimitResults;
import org.nrg.xnatx.dqr.preferences.DqrPreferences;

public abstract class CFindSCUPatientLevel extends CFindSCUSpecificLevel<Patient> {
    public CFindSCUPatientLevel(final DqrPreferences preferences, final DicomConnectionProperties dicomConnectionProperties, final CEchoSCU cechoSCU, final OrmStrategy ormStrategy) {
        super(preferences, dicomConnectionProperties, cechoSCU, ormStrategy);
    }

    @Override
    protected QueryRetrieveLevel getQueryLevel() {
        return QueryRetrieveLevel.PATIENT;
    }

    @Override
    protected List<Integer> getReturnTagPaths() {
        // the DcmQR defaults are fine for me
        return Collections.emptyList();
    }

    @Override
    protected Patient mapDicomObjectToDomainObject(final DicomObject dicomObject) {
        return new Patient(dicomObject, getOrmStrategy());
    }

    @Override
    protected PacsSearchResults<String, Patient> wrapResults(final Map<String, Patient> results, final boolean hasLimitedResults, final StudyDateRangeLimitResults studyDateRangeLimitResults) {
        return PacsSearchResults.<String, Patient>builder().results(results).hasLimitedResultSetSize(hasLimitedResults).studyDateRangeLimitResults(studyDateRangeLimitResults).build();
    }
}

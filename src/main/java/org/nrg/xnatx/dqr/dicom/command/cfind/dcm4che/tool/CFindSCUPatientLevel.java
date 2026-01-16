/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.dicom.command.cfind.dcm4che.tool.CFindSCUPatientLevel
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2024, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.dicom.command.cfind.dcm4che.tool;

import org.dcm4che3.data.Attributes;
import org.nrg.xnatx.dqr.dicom.command.cecho.CEchoSCU;
import org.nrg.xnatx.dqr.dicom.command.dcm4che3.QueryRetrieveLevel;
import org.nrg.xnatx.dqr.dicom.net.DicomConnectionProperties;
import org.nrg.xnatx.dqr.dicom.strategy.orm.OrmStrategy;
import org.nrg.xnatx.dqr.domain.Patient;
import org.nrg.xnatx.dqr.dto.PacsSearchResults;
import org.nrg.xnatx.dqr.dto.StudyDateRangeLimitResults;
import org.nrg.xnatx.dqr.preferences.DqrPreferences;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public abstract class CFindSCUPatientLevel extends CFindSCUSpecificLevel<Patient> {

    public CFindSCUPatientLevel(final DqrPreferences preferences,
                                 final DicomConnectionProperties dicomConnectionProperties,
                                 final CEchoSCU cechoSCU,
                                 final OrmStrategy ormStrategy) {
        super(preferences, dicomConnectionProperties, cechoSCU, ormStrategy);
    }

    @Override
    protected QueryRetrieveLevel getQueryLevel() {
        return QueryRetrieveLevel.PATIENT;
    }

    @Override
    protected List<Integer> getReturnTagPaths() {
        // the default return keys are fine
        return Collections.emptyList();
    }

    @Override
    protected Patient mapAttributesToDomainObject(final Attributes attributes) {
        return Patient.from(attributes,
                patientName -> getOrmStrategy().getPatientNameStrategy().dicomPatientNameToDqrPatientName(patientName));
    }

    @Override
    protected PacsSearchResults<Patient> wrapResults(final Collection<Patient> results,
                                                      final boolean hasLimitedResults,
                                                      final StudyDateRangeLimitResults studyDateRangeLimitResults) {
        return PacsSearchResults.<Patient>builder()
                .results(results)
                .hasLimitedResultSetSize(hasLimitedResults)
                .studyDateRangeLimitResults(studyDateRangeLimitResults)
                .build();
    }
}

/*
 * CFindSCUPatientLevel
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

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.tool.dcmqr.DcmQR.QueryRetrieveLevel;
import org.nrg.dqr.dicom.command.cecho.CEchoSCU;
import org.nrg.dqr.dicom.net.DicomConnectionProperties;
import org.nrg.dqr.dicom.strategy.orm.OrmStrategy;
import org.nrg.dqr.domain.Patient;
import org.nrg.dqr.dto.PacsSearchResults;
import org.nrg.dqr.dto.StudyDateRangeLimitResults;

public abstract class CFindSCUPatientLevel extends CFindSCUSpecificLevel<Patient> {

    private final static int[] EMPTY_RETURN_TAG_PATHS = new int[0];

    public CFindSCUPatientLevel(final DicomConnectionProperties dicomConnectionProperties, final CEchoSCU cechoSCU,
                                final OrmStrategy ormStrategy) {
        super(dicomConnectionProperties, cechoSCU, ormStrategy);
    }

    @Override
    protected QueryRetrieveLevel getQueryLevel() {
        return QueryRetrieveLevel.PATIENT;
    }

    @Override
    protected int[] getReturnTagPaths() {
        // the DcmQR defaults are fine for me
        return EMPTY_RETURN_TAG_PATHS;
    }

    @Override
    protected Patient mapDicomObjectToDomainObject(final DicomObject d) {
        return mapDicomObjectToPatient(d, getOrmStrategy());
    }

    static Patient mapDicomObjectToPatient(final DicomObject d, OrmStrategy ormStrategy) {
        final Patient patient = new Patient();
        patient.setId(StringUtils.trim(d.getString(Tag.PatientID)));
        patient.setName(ormStrategy.getPatientNameStrategy().dicomPatientNameToDqrPatientName(
                StringUtils.trim(d.getString(Tag.PatientName))));
        if (!StringUtils.isBlank(d.getString(Tag.PatientBirthDate))) {
            patient.setBirthDate(d.getDate(Tag.PatientBirthDate));
        }
        patient.setSex(StringUtils.trim(d.getString(Tag.PatientSex)));
        return patient;
    }

    @Override
    protected PacsSearchResults<String, Patient> wrapResults(final Map<String, Patient> results,
                                                             boolean hasLimitedResults, final StudyDateRangeLimitResults studyDateRangeLimitResults) {
        return new PacsSearchResults<String, Patient>(results, hasLimitedResults, studyDateRangeLimitResults);
    }
}

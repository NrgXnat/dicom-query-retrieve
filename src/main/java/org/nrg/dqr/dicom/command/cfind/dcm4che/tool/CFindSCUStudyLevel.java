/*
 * CFindSCUStudyLevel
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
import org.dcm4che2.data.PersonName;
import org.dcm4che2.data.Tag;
import org.dcm4che2.tool.dcmqr.DcmQR.QueryRetrieveLevel;
import org.nrg.dqr.dicom.net.DicomConnectionProperties;
import org.nrg.dqr.dicom.strategy.orm.OrmStrategy;
import org.nrg.dqr.domain.ReferringPhysicianName;
import org.nrg.dqr.domain.Study;
import org.nrg.dqr.dto.PacsSearchResults;
import org.nrg.dqr.dto.StudyDateRangeLimitResults;
import org.nrg.dqr.dicom.command.cecho.CEchoSCU;
import org.nrg.dqr.preferences.DqrPreferences;

public abstract class CFindSCUStudyLevel extends CFindSCUSpecificLevel<Study> {

    private final static int[] RETURN_TAG_PATHS = new int[]{
            Tag.PatientID,
            Tag.PatientName,
            Tag.PatientBirthDate,
            Tag.PatientSex,
            Tag.StudyDescription,
            Tag.ReferringPhysicianName,
            Tag.ModalitiesInStudy
    };

    public CFindSCUStudyLevel(final DqrPreferences preferences, final DicomConnectionProperties dicomConnectionProperties, final CEchoSCU cechoSCU,
                              final OrmStrategy ormStrategy) {
        super(preferences, dicomConnectionProperties, cechoSCU, ormStrategy);
    }

    @Override
    protected QueryRetrieveLevel getQueryLevel() {
        return QueryRetrieveLevel.STUDY;
    }

    @Override
    protected int[] getReturnTagPaths() {
        return RETURN_TAG_PATHS;
    }

    @Override
    protected Study mapDicomObjectToDomainObject(final DicomObject d) {
        final Study study = new Study();
        study.setPatient(CFindSCUPatientLevel.mapDicomObjectToPatient(d, getOrmStrategy()));
        study.setStudyInstanceUid(StringUtils.trim(d.getString(Tag.StudyInstanceUID)));
        if (!StringUtils.isBlank(d.getString(Tag.StudyDate))) {
            study.setStudyDate(d.getDate(Tag.StudyDate));
        }
        study.setReferringPhysicianName(new ReferringPhysicianName(new PersonName((StringUtils.trim(d
                .getString(Tag.ReferringPhysicianName))))));
        study.setStudyId(StringUtils.trim(d.getString(Tag.StudyID)));
        study.setAccessionNumber(StringUtils.trim(d.getString(Tag.AccessionNumber)));
        study.setStudyDescription(StringUtils.trim(d.getString(Tag.StudyDescription)));
        study.setModalitiesInStudy(StringUtils.trim(d.getString(Tag.ModalitiesInStudy)));
        return study;
    }

    @Override
    protected PacsSearchResults<String, Study> wrapResults(final Map<String, Study> results,
                                                           final boolean hasLimitedResults, final StudyDateRangeLimitResults studyDateRangeLimitResults) {
        return new PacsSearchResults<String, Study>(results, hasLimitedResults, studyDateRangeLimitResults);
    }
}

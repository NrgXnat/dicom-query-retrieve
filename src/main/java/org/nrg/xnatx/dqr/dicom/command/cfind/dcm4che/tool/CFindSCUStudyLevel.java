/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.dicom.command.cfind.dcm4che.tool.CFindSCUStudyLevel
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.dicom.command.cfind.dcm4che.tool;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.PersonName;
import org.dcm4che2.data.Tag;
import org.dcm4che2.tool.dcmqr.DcmQR.QueryRetrieveLevel;
import org.nrg.xnatx.dqr.dicom.command.cecho.CEchoSCU;
import org.nrg.xnatx.dqr.dicom.net.DicomConnectionProperties;
import org.nrg.xnatx.dqr.dicom.strategy.orm.OrmStrategy;
import org.nrg.xnatx.dqr.domain.Patient;
import org.nrg.xnatx.dqr.domain.ReferringPhysicianName;
import org.nrg.xnatx.dqr.domain.Study;
import org.nrg.xnatx.dqr.dto.PacsSearchResults;
import org.nrg.xnatx.dqr.dto.StudyDateRangeLimitResults;
import org.nrg.xnatx.dqr.preferences.DqrPreferences;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@Slf4j
public abstract class CFindSCUStudyLevel extends CFindSCUSpecificLevel<Study> {
    public CFindSCUStudyLevel(final DqrPreferences preferences, final DicomConnectionProperties dicomConnectionProperties, final CEchoSCU cechoSCU, final OrmStrategy ormStrategy) {
        super(preferences, dicomConnectionProperties, cechoSCU, ormStrategy);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected QueryRetrieveLevel getQueryLevel() {
        return QueryRetrieveLevel.STUDY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<Integer> getReturnTagPaths() {
        return RETURN_TAG_PATHS;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Study mapDicomObjectToDomainObject(final DicomObject dicomObject) {
        final Study study = new Study();
        study.setPatient(new Patient(dicomObject, getOrmStrategy()));
        study.setStudyInstanceUid(StringUtils.trim(dicomObject.getString(Tag.StudyInstanceUID)));
        study.setStudyId(StringUtils.trim(dicomObject.getString(Tag.StudyID)));
        study.setAccessionNumber(StringUtils.trim(dicomObject.getString(Tag.AccessionNumber)));
        if (dicomObject.containsValue(Tag.StudyDate) && !StringUtils.isBlank(dicomObject.getString(Tag.StudyDate))) {
            study.setStudyDate(dicomObject.getDate(Tag.StudyDate));
        }
        if (dicomObject.containsValue(Tag.ReferringPhysicianName)) {
            study.setReferringPhysicianName(new ReferringPhysicianName(new PersonName(StringUtils.trim(dicomObject.getString(Tag.ReferringPhysicianName)))));
        }
        if (dicomObject.containsValue(Tag.StudyDescription)) {
            study.setStudyDescription(StringUtils.trim(dicomObject.getString(Tag.StudyDescription)));
        }
        if (dicomObject.containsValue(Tag.ModalitiesInStudy)) {
            study.setModalitiesInStudy(Arrays.asList(dicomObject.getStrings(Tag.ModalitiesInStudy)));
        }
        return study;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PacsSearchResults<Study> wrapResults(final Collection<Study> results, final boolean hasLimitedResults, final StudyDateRangeLimitResults studyDateRangeLimitResults) {
        return PacsSearchResults.<Study>builder().results(results).hasLimitedResultSetSize(hasLimitedResults).studyDateRangeLimitResults(studyDateRangeLimitResults).build();
    }

    private final static List<Integer> RETURN_TAG_PATHS = Arrays.asList(Tag.PatientID, Tag.PatientName, Tag.PatientBirthDate, Tag.PatientSex, Tag.StudyDescription, Tag.ReferringPhysicianName, Tag.ModalitiesInStudy);
}

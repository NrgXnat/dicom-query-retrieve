/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.dicom.command.cfind.dcm4che.tool.CFindSCUStudyLevel
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2024, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.dicom.command.cfind.dcm4che.tool;

import lombok.extern.slf4j.Slf4j;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.nrg.xnatx.dqr.dicom.command.cecho.CEchoSCU;
import org.nrg.xnatx.dqr.dicom.command.dcm4che3.QueryRetrieveLevel;
import org.nrg.xnatx.dqr.dicom.net.DicomConnectionProperties;
import org.nrg.xnatx.dqr.dicom.strategy.orm.OrmStrategy;
import org.nrg.xnatx.dqr.domain.Study;
import org.nrg.xnatx.dqr.dto.PacsSearchResults;
import org.nrg.xnatx.dqr.dto.StudyDateRangeLimitResults;
import org.nrg.xnatx.dqr.preferences.DqrPreferences;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@Slf4j
public abstract class CFindSCUStudyLevel extends CFindSCUSpecificLevel<Study> {

    public CFindSCUStudyLevel(final DqrPreferences preferences,
                               final DicomConnectionProperties dicomConnectionProperties,
                               final CEchoSCU cechoSCU,
                               final OrmStrategy ormStrategy) {
        super(preferences, dicomConnectionProperties, cechoSCU, ormStrategy);
    }

    @Override
    protected QueryRetrieveLevel getQueryLevel() {
        return QueryRetrieveLevel.STUDY;
    }

    @Override
    protected List<Integer> getReturnTagPaths() {
        return RETURN_TAG_PATHS;
    }

    @Override
    protected Study mapAttributesToDomainObject(final Attributes attributes) {
        return Study.from(attributes,
                patientName -> getOrmStrategy().getPatientNameStrategy().dicomPatientNameToDqrPatientName(patientName));
    }

    @Override
    protected PacsSearchResults<Study> wrapResults(final Collection<Study> results,
                                                    final boolean hasLimitedResults,
                                                    final StudyDateRangeLimitResults studyDateRangeLimitResults) {
        return PacsSearchResults.<Study>builder()
                .results(results)
                .hasLimitedResultSetSize(hasLimitedResults)
                .studyDateRangeLimitResults(studyDateRangeLimitResults)
                .build();
    }

    private static final List<Integer> RETURN_TAG_PATHS = Arrays.asList(
            Tag.PatientID,
            Tag.PatientName,
            Tag.PatientBirthDate,
            Tag.PatientSex,
            Tag.StudyDescription,
            Tag.ReferringPhysicianName,
            Tag.ModalitiesInStudy
    );
}

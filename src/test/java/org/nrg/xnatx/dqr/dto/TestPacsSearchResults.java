/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.dto.TestPacsSearchResults
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.dto;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.nrg.framework.configuration.SerializerConfig;
import org.nrg.framework.services.SerializerService;
import org.nrg.xnatx.dqr.domain.DqrPersonName;
import org.nrg.xnatx.dqr.domain.Patient;
import org.nrg.xnatx.dqr.domain.Study;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = SerializerConfig.class)
@Slf4j
public class TestPacsSearchResults {
    @Test
    public void testPacsSearchResultsSerialization() throws JsonProcessingException {
        final Patient patient = Patient.builder()
                                       .name(DqrPersonName.builder()
                                                          .firstName("Patient")
                                                          .lastName("0001")
                                                          .build())
                                       .id("0001")
                                       .birthDate(PATIENT_0001_DOB)
                                       .sex("F")
                                       .build();
        final Study study1 = Study.builder().studyInstanceUid(PATIENT_0001_STUDY_1_UID)
                                  .patient(patient)
                                  .studyDate(PATIENT_0001_STUDY_1_DATE)
                                  .studyId("00010001")
                                  .studyDescription("Study 01-01")
                                  .modalityInStudy("MR")
                                  .build();
        final Study study2 = Study.builder().studyInstanceUid(PATIENT_0001_STUDY_2_UID)
                                  .patient(patient)
                                  .studyDate(PATIENT_0001_STUDY_2_DATE)
                                  .studyId("00010002")
                                  .studyDescription("Study 01-02")
                                  .modalityInStudy("MR")
                                  .build();
        final Study study3 = Study.builder().studyInstanceUid(PATIENT_0001_STUDY_3_UID)
                                  .patient(patient)
                                  .studyDate(PATIENT_0001_STUDY_3_DATE)
                                  .studyId("00010003")
                                  .studyDescription("Study 01-03")
                                  .modalityInStudy("MR")
                                  .build();
        final List<Study> studies = Stream.of(study1, study2, study3).collect(Collectors.toList());
        patient.setStudies(studies);
        final PacsSearchResults<Study> results = PacsSearchResults.<Study>builder()
                                                                  .hasLimitedResultSetSize(false)
                                                                  .studyDateRangeLimitResults(StudyDateRangeLimitResults.builder()
                                                                                                                        .limitType(StudyDateRangeLimitResults.LimitType.NO_LIMIT)
                                                                                                                        .build())
                                                                  .results(studies)
                                                                  .build();
        assertThat(results.getResults()).hasSize(3).containsOnly(study1, study2, study3).allSatisfy(study -> StringUtils.equals("0001", study.getPatient().getId()));
        assertThat(results).hasFieldOrPropertyWithValue("hasLimitedResultSetSize", false);
        assertThat(results.getStudyDateRangeLimitResults())
            .hasFieldOrPropertyWithValue("dateRange", null)
            .hasFieldOrPropertyWithValue("limitExplanation", null)
            .hasFieldOrPropertyWithValue("unlimited", true)
            .hasFieldOrPropertyWithValue("limited", false);
        assertThat(results.getStudyDateRangeLimitResults().isUnlimited()).isTrue();
        assertThat(results.getStudyDateRangeLimitResults().isLimited()).isFalse();

        final ObjectMapper mapper     = getDefaultObjectMapper();
        final String       serialized = mapper.writeValueAsString(results);
        assertThat(serialized).isNotBlank();
    }

    private ObjectMapper getDefaultObjectMapper() {
        return _serializer.getObjectMapper();
    }

    private static final Date   PATIENT_0001_DOB          = new GregorianCalendar(1973, Calendar.JANUARY, 11).getTime();
    private static final String PATIENT_0001_STUDY_1_UID  = "1.2.276.0.7230010.3.1.2.0.36575.1589387652.304847";
    private static final Date   PATIENT_0001_STUDY_1_DATE = new GregorianCalendar(2019, Calendar.MARCH, 21).getTime();
    private static final String PATIENT_0001_STUDY_2_UID  = "1.2.276.0.7230010.3.1.2.0.91562.2020051410.5938";
    private static final Date   PATIENT_0001_STUDY_2_DATE = new GregorianCalendar(2019, Calendar.JULY, 12).getTime();
    private static final String PATIENT_0001_STUDY_3_UID  = "1.2.840.113654.2.45.2.108105";
    private static final Date   PATIENT_0001_STUDY_3_DATE = new GregorianCalendar(2019, Calendar.NOVEMBER, 3).getTime();

    @Autowired
    private SerializerService _serializer;
}

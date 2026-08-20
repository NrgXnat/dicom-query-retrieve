package org.nrg.xnatx.dqr.services.impl;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.nrg.config.services.ConfigService;
import org.nrg.xapi.exceptions.DataFormatException;
import org.nrg.dcm.scp.DicomSCPManager;
import org.nrg.xdat.security.user.XnatUserProvider;
import org.nrg.xdat.services.StudyRoutingService;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.processor.services.ArchiveProcessorInstanceService;
import org.nrg.xnatx.dqr.dicom.RetrieveLevel;
import org.nrg.xnatx.dqr.domain.DqrPersonName;
import org.nrg.xnatx.dqr.domain.Patient;
import org.nrg.xnatx.dqr.domain.Series;
import org.nrg.xnatx.dqr.domain.Study;
import org.nrg.xnatx.dqr.domain.entities.Pacs;
import org.nrg.xnatx.dqr.domain.entities.QueuedPacsRequest;
import org.nrg.xnatx.dqr.dto.PacsImportRequest;
import org.nrg.xnatx.dqr.dto.PacsSearchCriteria;
import org.nrg.xnatx.dqr.dto.PacsSearchResults;
import org.nrg.xnatx.dqr.dto.StudyImportInformation;
import org.nrg.xnatx.dqr.services.PacsClientRoutingService;
import org.nrg.xnatx.dqr.services.PacsClientService;
import org.nrg.xnatx.dqr.services.PacsService;
import org.nrg.xnatx.dqr.services.QueuedPacsRequestService;
import org.nrg.xnatx.dqr.services.impl.basic.BasicDicomQueryRetrieveService;
import org.springframework.jms.core.JmsTemplate;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Queueing a study-level import has to avoid the series-level query that expands a study, since
 * skipping it is most of the point. That query was also where the queue and history views got the
 * study's descriptive fields, so this covers where those come from instead.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TestImportFromPacsQueueing {

    private static final String STUDY_UID  = RandomStringUtils.randomNumeric(20);
    private static final String STUDY_ID   = "STUDY-42";
    private static final String ACCESSION  = "A00012345";
    private static final String PATIENT_ID = "P-9";
    private static final Date   STUDY_DATE = Date.from(LocalDate.of(2024, 3, 7).atStartOfDay(ZoneId.systemDefault()).toInstant());

    @Mock private DicomSCPManager                 dicomSCPManager;
    @Mock private QueuedPacsRequestService        queuedPacsRequestService;
    @Mock private ConfigService                   configService;
    @Mock private StudyRoutingService             studyRoutingService;
    @Mock private PacsService                     pacsService;
    @Mock private JmsTemplate                     jmsTemplate;
    @Mock private ArchiveProcessorInstanceService archiveProcessorInstanceService;
    @Mock private XnatUserProvider                userProvider;
    @Mock private PacsClientRoutingService        pacsClientRoutingService;
    @Mock private PacsClientService               pacsClientService;
    @Mock private UserI                           user;

    private BasicDicomQueryRetrieveService service;
    private Pacs                           pacs;

    @BeforeEach
    void setUp() throws Exception {
        pacs = Pacs.builder().host("pacs.xnat.org").queryRetrievePort(4242).aeTitle("PACS").label("Pacs").queryable(true).build();

        when(pacsService.retrieve(1L)).thenReturn(pacs);
        when(pacsClientRoutingService.getPacsClientService(pacs)).thenReturn(pacsClientService);
        when(userProvider.getLogin()).thenReturn("admin");
        when(user.getUsername()).thenReturn("someone");
        when(queuedPacsRequestService.create(any(QueuedPacsRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service = new BasicDicomQueryRetrieveService(dicomSCPManager, queuedPacsRequestService, configService,
                studyRoutingService, pacsService, jmsTemplate, archiveProcessorInstanceService, userProvider,
                pacsClientRoutingService);
    }

    @Test
    void aStudyLevelImportSkipsTheSeriesQueryAndTakesItsDetailsFromTheStudy() throws Exception {
        when(pacsClientService.getStudy(pacs, STUDY_UID)).thenReturn(Optional.of(study()));

        service.importFromPacs(user, request(RetrieveLevel.STUDY));

        // Expanding the study into its series is exactly what study-level retrieve avoids
        verify(pacsClientService, never()).querySeries(any(Pacs.class), any(PacsSearchCriteria.class));

        final QueuedPacsRequest queued = captureQueuedRequest();
        assertThat(queued.getRetrieveLevel()).isEqualTo(RetrieveLevel.STUDY);
        assertThat(queued.getSeriesIds()).isEmpty();
        assertThat(queued.getStudyInstanceUid()).isEqualTo(STUDY_UID);
        assertThat(queued.getStudyId()).isEqualTo(STUDY_ID);
        assertThat(queued.getAccessionNumber()).isEqualTo(ACCESSION);
        assertThat(queued.getPatientId()).isEqualTo(PATIENT_ID);
        assertThat(queued.getPatientName()).isEqualTo("Doe^John");
        // Stored the way DICOM writes a date, matching what a series-level import stores
        assertThat(queued.getStudyDate()).isEqualTo("20240307");
    }

    @Test
    void aStudyMissingFromThePacsIsNotQueued() throws Exception {
        when(pacsClientService.getStudy(pacs, STUDY_UID)).thenReturn(Optional.empty());

        final List<QueuedPacsRequest> queued = service.importFromPacs(user, request(RetrieveLevel.STUDY));

        assertThat(queued).isEmpty();
        verify(queuedPacsRequestService, never()).create(any(QueuedPacsRequest.class));
    }

    @Test
    void aSeriesLevelImportStillExpandsTheStudyAndTakesItsDetailsFromTheFirstSeries() throws Exception {
        when(pacsClientService.querySeries(any(Pacs.class), any(PacsSearchCriteria.class))).thenReturn(seriesResults());

        service.importFromPacs(user, request(RetrieveLevel.SERIES));

        verify(pacsClientService, never()).getStudy(any(Pacs.class), anyString());

        final QueuedPacsRequest queued = captureQueuedRequest();
        assertThat(queued.getRetrieveLevel()).isEqualTo(RetrieveLevel.SERIES);
        assertThat(queued.getSeriesIds()).containsExactly("1.2.3.1", "1.2.3.2");
        assertThat(queued.getStudyId()).isEqualTo(STUDY_ID);
        assertThat(queued.getStudyDate()).isEqualTo("20240307");
    }

    @Test
    void namingSeriesForAStudyLevelRetrieveIsRejectedRatherThanQuietlyDowngraded() {
        // Downgrading meant a PACS configured to retrieve whole studies never actually did, since
        // the import screen always named the series it had selected
        final PacsImportRequest request = PacsImportRequest.builder()
                .pacsId(1L).aeTitle("XNAT").port(8104).projectId("PROJ")
                .retrieveLevel(RetrieveLevel.STUDY)
                .study(StudyImportInformation.builder()
                        .studyInstanceUid(STUDY_UID)
                        .seriesInstanceUids(Collections.singletonList("1.2.3.1"))
                        .build())
                .build();

        assertThatThrownBy(() -> service.importFromPacs(user, request))
                .isInstanceOf(DataFormatException.class)
                .hasMessageContaining(STUDY_UID)
                .hasMessageContaining("SERIES level");

        verifyNoInteractions(queuedPacsRequestService);
    }

    @Test
    void namingSeriesDescriptionsForAStudyLevelRetrieveIsAlsoRejected() {
        final PacsImportRequest request = PacsImportRequest.builder()
                .pacsId(1L).aeTitle("XNAT").port(8104).projectId("PROJ")
                .retrieveLevel(RetrieveLevel.STUDY)
                .study(StudyImportInformation.builder()
                        .studyInstanceUid(STUDY_UID)
                        .seriesDescriptions(Collections.singletonList("T1"))
                        .build())
                .build();

        assertThatThrownBy(() -> service.importFromPacs(user, request))
                .isInstanceOf(DataFormatException.class);
    }

    @Test
    void namingSeriesIsStillFineWhenThePacsRetrievesPerSeries() throws Exception {
        when(pacsClientService.querySeries(any(Pacs.class), any(PacsSearchCriteria.class))).thenReturn(seriesResults());

        final PacsImportRequest request = PacsImportRequest.builder()
                .pacsId(1L).aeTitle("XNAT").port(8104).projectId("PROJ")
                .retrieveLevel(RetrieveLevel.SERIES)
                .study(StudyImportInformation.builder()
                        .studyInstanceUid(STUDY_UID)
                        .seriesInstanceUids(Collections.singletonList("1.2.3.1"))
                        .build())
                .build();

        service.importFromPacs(user, request);

        final QueuedPacsRequest queued = captureQueuedRequest();
        assertThat(queued.getRetrieveLevel()).isEqualTo(RetrieveLevel.SERIES);
        assertThat(queued.getSeriesIds()).containsExactly("1.2.3.1");
    }

    @Test
    void askingForAStudyLevelRetrieveFromADicomWebPacsIsRejectedBeforeAnythingIsQueued() {
        // The PACS setting can't be study level, so a level this high can only have come from the
        // request itself. Queueing it would produce requests that can only fail at the PACS.
        final Pacs dicomWebPacs = dicomWebPacs();
        when(pacsService.retrieve(2L)).thenReturn(dicomWebPacs);
        when(pacsClientRoutingService.getPacsClientService(dicomWebPacs)).thenReturn(pacsClientService);

        assertThatThrownBy(() -> service.importFromPacs(user, request(2L, RetrieveLevel.STUDY)))
                .isInstanceOf(DataFormatException.class)
                .hasMessageContaining("Orthanc")
                .hasMessageContaining("DIMSE");

        verify(queuedPacsRequestService, never()).create(any(QueuedPacsRequest.class));
    }

    @Test
    void aSeriesLevelImportFromADicomWebPacsIsStillAccepted() throws Exception {
        final Pacs dicomWebPacs = dicomWebPacs();
        when(pacsService.retrieve(2L)).thenReturn(dicomWebPacs);
        when(pacsClientRoutingService.getPacsClientService(dicomWebPacs)).thenReturn(pacsClientService);
        when(pacsClientService.querySeries(any(Pacs.class), any(PacsSearchCriteria.class))).thenReturn(seriesResults());

        service.importFromPacs(user, request(2L, RetrieveLevel.SERIES));

        assertThat(captureQueuedRequest().getRetrieveLevel()).isEqualTo(RetrieveLevel.SERIES);
    }

    private static Pacs dicomWebPacs() {
        return Pacs.builder().label("Orthanc").aeTitle("ORTHANC").dicomWebEnabled(true).queryable(true).build();
    }

    private QueuedPacsRequest captureQueuedRequest() {
        final ArgumentCaptor<QueuedPacsRequest> captor = ArgumentCaptor.forClass(QueuedPacsRequest.class);
        verify(queuedPacsRequestService).create(captor.capture());
        return captor.getValue();
    }

    private static PacsImportRequest request(final RetrieveLevel retrieveLevel) {
        return request(1L, retrieveLevel);
    }

    private static PacsImportRequest request(final long pacsId, final RetrieveLevel retrieveLevel) {
        return PacsImportRequest.builder()
                .pacsId(pacsId).aeTitle("XNAT").port(8104).projectId("PROJ")
                .retrieveLevel(retrieveLevel)
                .study(StudyImportInformation.builder().studyInstanceUid(STUDY_UID).build())
                .build();
    }

    private static Study study() {
        return Study.builder()
                .studyInstanceUid(STUDY_UID)
                .studyId(STUDY_ID)
                .accessionNumber(ACCESSION)
                .studyDate(STUDY_DATE)
                .patient(Patient.builder().id(PATIENT_ID).name(new DqrPersonName("John", "Doe")).build())
                .build();
    }

    private static PacsSearchResults<Series> seriesResults() {
        return PacsSearchResults.<Series>builder()
                .results(Arrays.asList(
                        Series.builder().seriesInstanceUid("1.2.3.1").studyId(STUDY_ID).studyDate("20240307").accessionNumber(ACCESSION).patientId(PATIENT_ID).build(),
                        Series.builder().seriesInstanceUid("1.2.3.2").studyId(STUDY_ID).studyDate("20240307").accessionNumber(ACCESSION).patientId(PATIENT_ID).build()))
                .hasLimitedResultSetSize(false)
                .build();
    }
}

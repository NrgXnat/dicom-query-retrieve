package org.nrg.xnatx.dqr.services.impl;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.nrg.config.services.ConfigService;
import org.nrg.dcm.scp.DicomSCPInstance;
import org.nrg.dcm.scp.DicomSCPManager;
import org.nrg.xdat.security.user.XnatUserProvider;
import org.nrg.xdat.services.StudyRoutingService;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.processor.services.ArchiveProcessorInstanceService;
import org.nrg.xnatx.dqr.dicom.RetrieveLevel;
import org.nrg.xnatx.dqr.domain.Series;
import org.nrg.xnatx.dqr.domain.Study;
import org.nrg.xnatx.dqr.domain.entities.ExecutedPacsRequest;
import org.nrg.xnatx.dqr.domain.entities.Pacs;
import org.nrg.xnatx.dqr.exceptions.DqrException;
import org.nrg.xnatx.dqr.services.PacsClientRoutingService;
import org.nrg.xnatx.dqr.services.PacsClientService;
import org.nrg.xnatx.dqr.services.PacsService;
import org.nrg.xnatx.dqr.services.QueuedPacsRequestService;
import org.nrg.xnatx.dqr.services.impl.basic.BasicDicomQueryRetrieveService;
import org.springframework.jms.core.JmsTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The stored retrieve level decides whether a queued request turns into one retrieve for the study
 * or one per series. Getting that branch wrong either silently pulls data nobody asked for or keeps
 * hammering the PACS per series, so both directions are pinned down here.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TestImportFromPacsRequestBranching {

    private static final String STUDY_UID   = RandomStringUtils.randomNumeric(20);
    private static final String AE_TITLE    = "XNAT";
    private static final List<String> SERIES_UIDS = Arrays.asList("1.2.3.1", "1.2.3.2", "1.2.3.3");

    @Mock private DicomSCPManager                 dicomSCPManager;
    @Mock private DicomSCPInstance                dicomSCPInstance;
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
    void setUp() {
        pacs = Pacs.builder().host("pacs.xnat.org").queryRetrievePort(4242).aeTitle("PACS").label("Pacs").queryable(true).build();

        when(pacsService.retrieve(any(Long.class))).thenReturn(pacs);
        when(pacsClientRoutingService.getPacsClientService(pacs)).thenReturn(pacsClientService);
        when(dicomSCPInstance.isEnabled()).thenReturn(true);
        when(dicomSCPInstance.getAeTitle()).thenReturn(AE_TITLE);
        when(dicomSCPManager.getDicomSCPInstances()).thenReturn(Collections.singletonMap(AE_TITLE, dicomSCPInstance));

        service = new BasicDicomQueryRetrieveService(dicomSCPManager, queuedPacsRequestService, configService,
                studyRoutingService, pacsService, jmsTemplate, archiveProcessorInstanceService, userProvider,
                pacsClientRoutingService);
    }

    @Test
    void aStudyLevelRequestIssuesOneRetrieveForTheWholeStudy() throws DqrException {
        service.importFromPacsRequest(request(RetrieveLevel.STUDY), user);

        verify(pacsClientService, times(1)).importStudy(eq(pacs), eq(user), any(Study.class), eq(AE_TITLE));
        verify(pacsClientService, never()).importSeries(any(), any(), any(), any(), any());
    }

    @Test
    void aSeriesLevelRequestIssuesOneRetrievePerSeries() throws DqrException {
        service.importFromPacsRequest(request(RetrieveLevel.SERIES), user);

        verify(pacsClientService, times(SERIES_UIDS.size())).importSeries(eq(pacs), eq(user), any(Study.class), any(Series.class), eq(AE_TITLE));
        verify(pacsClientService, never()).importStudy(any(), any(), any(), any());
    }

    @Test
    void aRequestWithNoStoredLevelStillRetrievesPerSeries() throws DqrException {
        // Requests queued before the level existed read back as SERIES and must behave as they did
        service.importFromPacsRequest(request(null), user);

        verify(pacsClientService, times(SERIES_UIDS.size())).importSeries(eq(pacs), eq(user), any(Study.class), any(Series.class), eq(AE_TITLE));
        verify(pacsClientService, never()).importStudy(any(), any(), any(), any());
    }

    private static ExecutedPacsRequest request(final RetrieveLevel retrieveLevel) {
        final ExecutedPacsRequest request = ExecutedPacsRequest.builder()
                .pacsId(1L)
                .username("someone")
                .studyInstanceUid(STUDY_UID)
                .seriesIds(SERIES_UIDS)
                .destinationAeTitle(AE_TITLE)
                .build();
        request.setRetrieveLevel(retrieveLevel);
        return request;
    }
}

package org.nrg.xnatx.dqr;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nrg.config.services.ConfigService;
import org.nrg.mail.services.MailService;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.preferences.SiteConfigPreferences;
import org.nrg.xdat.security.helpers.Users;
import org.nrg.xdat.security.user.XnatUserProvider;
import org.nrg.xdat.services.StudyRoutingService;
import org.nrg.xft.event.persist.PersistentWorkflowI;
import org.nrg.xft.event.persist.PersistentWorkflowUtils;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.archive.Operation;
import org.nrg.xnat.helpers.prearchive.PrearcDatabase;
import org.nrg.xnat.helpers.prearchive.PrearcUtils;
import org.nrg.xnat.helpers.prearchive.SessionData;
import org.nrg.xnat.helpers.prearchive.SessionDataTriple;
import org.nrg.xnat.services.messaging.prearchive.PrearchiveOperationRequest;
import org.nrg.xnatx.dqr.dicom.command.cmove.CMoveFailureException;
import org.nrg.xnatx.dqr.domain.entities.ExecutedPacsRequest;
import org.nrg.xnatx.dqr.domain.entities.Pacs;
import org.nrg.xnatx.dqr.domain.entities.PacsAvailability;
import org.nrg.xnatx.dqr.domain.entities.QueuedPacsRequest;
import org.nrg.xnatx.dqr.events.PacsDequeueThread;
import org.nrg.xnatx.dqr.events.PacsThreads;
import org.nrg.xnatx.dqr.preferences.DqrPreferences;
import org.nrg.xnatx.dqr.services.DicomQueryRetrieveService;
import org.nrg.xnatx.dqr.services.ExecutedPacsRequestService;
import org.nrg.xnatx.dqr.services.PacsAvailabilityService;
import org.nrg.xnatx.dqr.services.PacsService;
import org.nrg.xnatx.dqr.services.QueuedPacsRequestService;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PacsDequeueThreadTest {

    private final Long pacsId = ThreadLocalRandom.current().nextLong(1, 10);
    @Mock PacsThreads threads;
    @Mock DicomQueryRetrieveService dqrService;
    @Mock PacsService pacsService;
    @Mock QueuedPacsRequestService queuedPacsRequestService;
    @Mock ExecutedPacsRequestService executedPacsRequestService;
    @Mock PacsAvailabilityService pacsAvailabilityService;
    @Mock StudyRoutingService studyRoutingService;
    @Mock DqrPreferences dqrPreferences;
    @Mock SiteConfigPreferences siteConfigPreferences;
    @Mock ConfigService configService;
    @Mock MailService mailService;
    @Mock XnatUserProvider primaryAdminUserProvider;
    @Mock PacsAvailability pacsAvailability;
    @Mock UserI admin;
    @Mock UserI user;
    @Mock Pacs pacs;
    @Mock PersistentWorkflowI workflow;

    private PacsDequeueThread pacsDequeueThread;

    @BeforeEach
    public void setup() {
        pacsDequeueThread = new PacsDequeueThread(pacsId, threads, dqrService, pacsService, queuedPacsRequestService, executedPacsRequestService, pacsAvailabilityService, studyRoutingService, dqrPreferences, siteConfigPreferences, configService, mailService, primaryAdminUserProvider);
    }

    @Test
    public void testFailedCmove_deleteFromPrearc() throws Exception {
        // Set up test data
        final String studyInstanceUid = RandomStringUtils.randomAlphabetic(5);
        final String username = RandomStringUtils.randomAlphabetic(5);
        final int numAvailableThreads = ThreadLocalRandom.current().nextInt(1, 3);

        // We stage this mock data so that two prearchive sessions will match the study instance UID,
        // but only one will match the project
        final String project = RandomStringUtils.randomAlphabetic(5);
        final SessionData sessionDataSameProject = mock(SessionData.class);
        when(sessionDataSameProject.getProject()).thenReturn(project);
        final SessionDataTriple sessionDataTriple = mock(SessionDataTriple.class);
        when(sessionDataSameProject.getSessionDataTriple()).thenReturn(sessionDataTriple);

        final String otherProject = RandomStringUtils.randomAlphabetic(5);
        final SessionData sessionDataDifferentProject = mock(SessionData.class);
        when(sessionDataDifferentProject.getProject()).thenReturn(otherProject);

        // The sessionDataList contains both sessions matching the studyInstanceUid.
        // But the sessionDataTripleList will only contain the session matching the project. That's what should be deleted.
        final List<SessionData> sessionDataList = Arrays.asList(sessionDataSameProject, sessionDataDifferentProject);
        final List<SessionDataTriple> sessionDataTripleList = Collections.singletonList(sessionDataTriple);
        final Map<SessionDataTriple, Boolean> deletionResult = Collections.singletonMap(sessionDataTriple, true);

        when(pacsAvailabilityService.findAvailableNow(pacsId)).thenReturn(Optional.of(pacsAvailability));
        when(pacsAvailability.getThreads()).thenReturn(numAvailableThreads);
        when(pacsAvailability.getUtilizationPercent()).thenReturn(100);
        when(threads.isOversubscribed(pacsId, numAvailableThreads)).thenReturn(false);
        when(primaryAdminUserProvider.get()).thenReturn(admin);
        when(pacsService.retrieve(pacsId)).thenReturn(pacs);
        when(dqrService.canConnect(admin, pacs)).thenReturn(true);
        when(admin.getUsername()).thenReturn("admin");
        when(dqrPreferences.getDqrMaxPacsCMOVEAttempts()).thenReturn("0");

        final QueuedPacsRequest queuedPacsRequest = QueuedPacsRequest.builder()
                .seriesIds(Collections.emptyList())
                .studyInstanceUid(studyInstanceUid)
                .xnatProject(project)
                .username(username)
                .build();
        when(queuedPacsRequestService.getQueuedOrFailedForPacsOrderedByPriorityAndDate(pacsId))
                .thenReturn(Collections.singletonList(queuedPacsRequest))
                .thenReturn(Collections.emptyList());  // This will break the loop in the method under test after one iteration

        // Simulate failed C-MOVE
        doThrow(new CMoveFailureException("C-MOVE failed"))
                .when(dqrService).importFromPacsRequest(any(ExecutedPacsRequest.class));

        try (MockedStatic<PrearcDatabase> prearcMock = mockStatic(PrearcDatabase.class);
             MockedStatic<Users> usersMock = mockStatic(Users.class);
             MockedStatic<PersistentWorkflowUtils> workflowUtilsMock = mockStatic(PersistentWorkflowUtils.class)) {
            // This is an uninteresting bit which we must mock so other parts of the code under test don't break
            usersMock.when(() -> Users.getUser(username)).thenReturn(user);
            workflowUtilsMock.when(() -> PersistentWorkflowUtils.buildOpenWorkflow(eq(user), any(), eq(studyInstanceUid), eq(project), any()))
                    .thenReturn(workflow);

            // This is the interesting bit to mock which we care about verifying
            prearcMock.when(() -> PrearcDatabase.getSessionByUID(studyInstanceUid)).thenReturn(sessionDataList);
            prearcMock.when(() -> PrearcDatabase.deleteSession(sessionDataTripleList)).thenReturn(deletionResult);

            // Call method under test
            pacsDequeueThread.runTask();

            // Verify that C-MOVE was attempted
            verify(dqrService, times(1)).importFromPacsRequest(any(ExecutedPacsRequest.class));

            // Verify calls were made to delete from prearchive
            prearcMock.verify(() -> PrearcDatabase.getSessionByUID(studyInstanceUid), times(1));
            prearcMock.verify(() -> PrearcDatabase.deleteSession(sessionDataTripleList), times(1));
        }
    }

    @Test
    public void testFailedCmove_multipleSessions_noDeleteFromPrearc() throws Exception {
        // Set up test data
        final String studyInstanceUid = RandomStringUtils.randomAlphabetic(5);
        final String username = RandomStringUtils.randomAlphabetic(5);
        final int numAvailableThreads = ThreadLocalRandom.current().nextInt(1, 3);

        // We stage this mock data so that two prearchive sessions will match the study instance UID,
        // and both match the project
        final String project = RandomStringUtils.randomAlphabetic(5);
        final SessionData sessionData1 = mock(SessionData.class);
        when(sessionData1.getProject()).thenReturn(project);
        final SessionDataTriple sessionDataTriple1 = mock(SessionDataTriple.class);
        when(sessionData1.getSessionDataTriple()).thenReturn(sessionDataTriple1);

        final SessionData sessionData2 = mock(SessionData.class);
        when(sessionData2.getProject()).thenReturn(project);
        final SessionDataTriple sessionDataTriple2 = mock(SessionDataTriple.class);
        when(sessionData2.getSessionDataTriple()).thenReturn(sessionDataTriple2);

        // The sessionDataList contains both sessions matching the studyInstanceUid.
        // And the sessionDataTripleList contains both sessions matching the project.
        final List<SessionData> sessionDataList = Arrays.asList(sessionData1, sessionData2);
        final List<SessionDataTriple> sessionDataTripleList = Arrays.asList(sessionDataTriple1, sessionDataTriple2);

        when(pacsAvailabilityService.findAvailableNow(pacsId)).thenReturn(Optional.of(pacsAvailability));
        when(pacsAvailability.getThreads()).thenReturn(numAvailableThreads);
        when(pacsAvailability.getUtilizationPercent()).thenReturn(100);
        when(threads.isOversubscribed(pacsId, numAvailableThreads)).thenReturn(false);
        when(primaryAdminUserProvider.get()).thenReturn(admin);
        when(pacsService.retrieve(pacsId)).thenReturn(pacs);
        when(dqrService.canConnect(admin, pacs)).thenReturn(true);
        when(admin.getUsername()).thenReturn("admin");
        when(dqrPreferences.getDqrMaxPacsCMOVEAttempts()).thenReturn("0");

        final QueuedPacsRequest queuedPacsRequest = QueuedPacsRequest.builder()
                .seriesIds(Collections.emptyList())
                .studyInstanceUid(studyInstanceUid)
                .xnatProject(project)
                .username(username)
                .build();
        when(queuedPacsRequestService.getQueuedOrFailedForPacsOrderedByPriorityAndDate(pacsId))
                .thenReturn(Collections.singletonList(queuedPacsRequest))
                .thenReturn(Collections.emptyList());  // This will break the loop in the method under test after one iteration

        // Simulate failed C-MOVE
        doThrow(new CMoveFailureException("C-MOVE failed"))
                .when(dqrService).importFromPacsRequest(any(ExecutedPacsRequest.class));

        try (MockedStatic<PrearcDatabase> prearcMock = mockStatic(PrearcDatabase.class);
             MockedStatic<Users> usersMock = mockStatic(Users.class);
             MockedStatic<PersistentWorkflowUtils> workflowUtilsMock = mockStatic(PersistentWorkflowUtils.class)) {
            // This is an uninteresting bit which we must mock so other parts of the code under test don't break
            usersMock.when(() -> Users.getUser(username)).thenReturn(user);
            workflowUtilsMock.when(() -> PersistentWorkflowUtils.buildOpenWorkflow(eq(user), any(), eq(studyInstanceUid), eq(project), any()))
                    .thenReturn(workflow);

            // This is the interesting bit to mock which we care about verifying
            prearcMock.when(() -> PrearcDatabase.getSessionByUID(studyInstanceUid)).thenReturn(sessionDataList);

            // Call method under test
            pacsDequeueThread.runTask();

            // Verify that C-MOVE was attempted
            verify(dqrService, times(1)).importFromPacsRequest(any(ExecutedPacsRequest.class));

            // Verify we read from the prearchive, but did not attempt to delete
            prearcMock.verify(() -> PrearcDatabase.getSessionByUID(studyInstanceUid), times(1));
            prearcMock.verify(() -> PrearcDatabase.deleteSession(sessionDataTripleList), times(0));
        }
    }

    @Test
    public void testFailedCmove_noProject_noDeleteFromPrearc() throws Exception {
        // Set up test data
        final String studyInstanceUid = RandomStringUtils.randomAlphabetic(5);
        final String username = RandomStringUtils.randomAlphabetic(5);
        final int numAvailableThreads = ThreadLocalRandom.current().nextInt(1, 3);

        // When no project is set we should not attempt to delete any sessions from the prearchive
        final String project = "";

        when(pacsAvailabilityService.findAvailableNow(pacsId)).thenReturn(Optional.of(pacsAvailability));
        when(pacsAvailability.getThreads()).thenReturn(numAvailableThreads);
        when(pacsAvailability.getUtilizationPercent()).thenReturn(100);
        when(threads.isOversubscribed(pacsId, numAvailableThreads)).thenReturn(false);
        when(primaryAdminUserProvider.get()).thenReturn(admin);
        when(pacsService.retrieve(pacsId)).thenReturn(pacs);
        when(dqrService.canConnect(admin, pacs)).thenReturn(true);
        when(admin.getUsername()).thenReturn("admin");
        when(dqrPreferences.getDqrMaxPacsCMOVEAttempts()).thenReturn("0");

        final QueuedPacsRequest queuedPacsRequest = QueuedPacsRequest.builder()
                .seriesIds(Collections.emptyList())
                .studyInstanceUid(studyInstanceUid)
                .xnatProject(project)
                .username(username)
                .build();
        when(queuedPacsRequestService.getQueuedOrFailedForPacsOrderedByPriorityAndDate(pacsId))
                .thenReturn(Collections.singletonList(queuedPacsRequest))
                .thenReturn(Collections.emptyList());  // This will break the loop in the method under test after one iteration

        // Simulate failed C-MOVE
        doThrow(new CMoveFailureException("C-MOVE failed"))
                .when(dqrService).importFromPacsRequest(any(ExecutedPacsRequest.class));

        try (MockedStatic<PrearcDatabase> prearcMock = mockStatic(PrearcDatabase.class);
             MockedStatic<Users> usersMock = mockStatic(Users.class);
             MockedStatic<PersistentWorkflowUtils> workflowUtilsMock = mockStatic(PersistentWorkflowUtils.class)) {
            // This is an uninteresting bit which we must mock so other parts of the code under test don't break
            usersMock.when(() -> Users.getUser(username)).thenReturn(user);
            workflowUtilsMock.when(() -> PersistentWorkflowUtils.buildOpenWorkflow(eq(user), any(), eq(studyInstanceUid), eq(project), any()))
                    .thenReturn(workflow);

            // Call method under test
            pacsDequeueThread.runTask();

            // Verify that C-MOVE was attempted
            verify(dqrService, times(1)).importFromPacsRequest(any(ExecutedPacsRequest.class));

            // Verify no calls were made to read or delete from prearchive
            prearcMock.verify(() -> PrearcDatabase.getSessionByUID(studyInstanceUid), times(0));
            prearcMock.verify(() -> PrearcDatabase.deleteSession(anyList()), times(0));
        }
    }

    @Test
    public void testSuccessfulCmove_submitRebuildRequest() throws Exception {
        // Set up test data
        final String studyInstanceUid = RandomStringUtils.randomAlphabetic(5);
        final String username = RandomStringUtils.randomAlphabetic(5);
        final int numAvailableThreads = ThreadLocalRandom.current().nextInt(1, 3);

        // We stage this mock data so that two prearchive sessions will match the study instance UID,
        // but only one will match the project
        final String foldername = RandomStringUtils.randomAlphabetic(5);
        final String timestamp = RandomStringUtils.randomAlphabetic(5);
        final String project = RandomStringUtils.randomAlphabetic(5);
        final SessionData sessionDataSameProject = mock(SessionData.class);
        when(sessionDataSameProject.getProject()).thenReturn(project);
        final SessionDataTriple sessionDataTriple = new SessionDataTriple(foldername, timestamp, project);
        when(sessionDataSameProject.getSessionDataTriple()).thenReturn(sessionDataTriple);
        final File prearcSessionDir = mock(File.class);

        final String otherProject = RandomStringUtils.randomAlphabetic(5);
        final SessionData sessionDataDifferentProject = mock(SessionData.class);
        when(sessionDataDifferentProject.getProject()).thenReturn(otherProject);

        // The sessionDataList contains both sessions matching the studyInstanceUid.
        final List<SessionData> sessionDataList = Arrays.asList(sessionDataSameProject, sessionDataDifferentProject);

        when(pacsAvailabilityService.findAvailableNow(pacsId)).thenReturn(Optional.of(pacsAvailability));
        when(pacsAvailability.getThreads()).thenReturn(numAvailableThreads);
        when(pacsAvailability.getUtilizationPercent()).thenReturn(100);
        when(threads.isOversubscribed(pacsId, numAvailableThreads)).thenReturn(false);
        when(primaryAdminUserProvider.get()).thenReturn(admin);
        when(pacsService.retrieve(pacsId)).thenReturn(pacs);
        when(dqrService.canConnect(admin, pacs)).thenReturn(true);
        when(admin.getUsername()).thenReturn("admin");
        when(user.getUsername()).thenReturn(username);

        final QueuedPacsRequest queuedPacsRequest = QueuedPacsRequest.builder()
                .seriesIds(Collections.emptyList())
                .studyInstanceUid(studyInstanceUid)
                .xnatProject(project)
                .username(username)
                .build();
        when(queuedPacsRequestService.getQueuedOrFailedForPacsOrderedByPriorityAndDate(pacsId))
                .thenReturn(Collections.singletonList(queuedPacsRequest))
                .thenReturn(Collections.emptyList());  // This will break the loop in the method under test after one iteration

        try (MockedStatic<PrearcDatabase> prearcDbMock = mockStatic(PrearcDatabase.class);
             MockedStatic<PrearcUtils> prearcUtilsMock = mockStatic(PrearcUtils.class);
             MockedStatic<Users> usersMock = mockStatic(Users.class);
             MockedStatic<PersistentWorkflowUtils> workflowUtilsMock = mockStatic(PersistentWorkflowUtils.class);
             MockedStatic<XDAT> xdatMock = mockStatic(XDAT.class)) {
            // This is an uninteresting bit which we must mock so other parts of the code under test don't break
            usersMock.when(() -> Users.getUser(username)).thenReturn(user);
            workflowUtilsMock.when(() -> PersistentWorkflowUtils.buildOpenWorkflow(eq(user), any(), eq(studyInstanceUid), eq(project), any()))
                    .thenReturn(workflow);

            // This is the interesting bit to mock which we care about verifying
            prearcDbMock.when(() -> PrearcDatabase.getSessionByUID(studyInstanceUid)).thenReturn(sessionDataList);
            prearcDbMock.when(() -> PrearcDatabase.getSession(foldername, timestamp, project)).thenReturn(sessionDataSameProject);
            prearcUtilsMock.when(() -> PrearcUtils.getPrearcSessionDir(user, project, timestamp, foldername, false)).thenReturn(prearcSessionDir);

            // Call method under test
            pacsDequeueThread.runTask();

            // Verify that C-MOVE was attempted
            verify(dqrService, times(1)).importFromPacsRequest(any(ExecutedPacsRequest.class));

            // Verify call was made to find sessions in prearchive by uid
            prearcDbMock.verify(() -> PrearcDatabase.getSessionByUID(studyInstanceUid), times(1));

            // Verify that the session rebuild request was submitted
            // It would be nice if we could verify this by building an expected PrearchiveOperationRequest object and passing it directly to verify(),
            //  but that doesn't work because PrearchiveOperationRequest doesn't implement equals()
            xdatMock.verify(() -> XDAT.sendJmsRequest(argThat(actualObj -> {
                final PrearchiveOperationRequest actual = (PrearchiveOperationRequest) actualObj;
                return StringUtils.equals(username, actual.getUsername())
                        && Operation.Rebuild.equals(actual.getOperation())
                        && sessionDataSameProject.equals(actual.getSessionData())
                        && prearcSessionDir.equals(actual.getSessionDir())
                        && Collections.emptyMap().equals(actual.getParameters())
                        && null == actual.getListenerId();
            })), times(1));
        }
    }

    @Test
    public void testSuccessfulCmove_multipleSessions_noSubmitRebuildRequest() throws Exception {
        // Set up test data
        final String studyInstanceUid = RandomStringUtils.randomAlphabetic(5);
        final String username = RandomStringUtils.randomAlphabetic(5);
        final int numAvailableThreads = ThreadLocalRandom.current().nextInt(1, 3);

        // We stage this mock data so that two prearchive sessions will match the study instance UID,
        // and both match the project
        final String project = RandomStringUtils.randomAlphabetic(5);
        final SessionData sessionData1 = mock(SessionData.class);
        when(sessionData1.getProject()).thenReturn(project);
        final SessionDataTriple sessionDataTriple1 = mock(SessionDataTriple.class);
        when(sessionData1.getSessionDataTriple()).thenReturn(sessionDataTriple1);

        final SessionData sessionData2 = mock(SessionData.class);
        when(sessionData2.getProject()).thenReturn(project);
        final SessionDataTriple sessionDataTriple2 = mock(SessionDataTriple.class);
        when(sessionData2.getSessionDataTriple()).thenReturn(sessionDataTriple2);

        // The sessionDataList contains both sessions matching the studyInstanceUid.
        final List<SessionData> sessionDataList = Arrays.asList(sessionData1, sessionData2);

        when(pacsAvailabilityService.findAvailableNow(pacsId)).thenReturn(Optional.of(pacsAvailability));
        when(pacsAvailability.getThreads()).thenReturn(numAvailableThreads);
        when(pacsAvailability.getUtilizationPercent()).thenReturn(100);
        when(threads.isOversubscribed(pacsId, numAvailableThreads)).thenReturn(false);
        when(primaryAdminUserProvider.get()).thenReturn(admin);
        when(pacsService.retrieve(pacsId)).thenReturn(pacs);
        when(dqrService.canConnect(admin, pacs)).thenReturn(true);
        when(admin.getUsername()).thenReturn("admin");

        final QueuedPacsRequest queuedPacsRequest = QueuedPacsRequest.builder()
                .seriesIds(Collections.emptyList())
                .studyInstanceUid(studyInstanceUid)
                .xnatProject(project)
                .username(username)
                .build();
        when(queuedPacsRequestService.getQueuedOrFailedForPacsOrderedByPriorityAndDate(pacsId))
                .thenReturn(Collections.singletonList(queuedPacsRequest))
                .thenReturn(Collections.emptyList());  // This will break the loop in the method under test after one iteration

        try (MockedStatic<PrearcDatabase> prearcDbMock = mockStatic(PrearcDatabase.class);
             MockedStatic<Users> usersMock = mockStatic(Users.class);
             MockedStatic<PersistentWorkflowUtils> workflowUtilsMock = mockStatic(PersistentWorkflowUtils.class);
             MockedStatic<XDAT> xdatMock = mockStatic(XDAT.class)) {
            // This is an uninteresting bit which we must mock so other parts of the code under test don't break
            usersMock.when(() -> Users.getUser(username)).thenReturn(user);
            workflowUtilsMock.when(() -> PersistentWorkflowUtils.buildOpenWorkflow(eq(user), any(), eq(studyInstanceUid), eq(project), any()))
                    .thenReturn(workflow);

            // This is the interesting bit to mock which we care about verifying
            prearcDbMock.when(() -> PrearcDatabase.getSessionByUID(studyInstanceUid)).thenReturn(sessionDataList);

            // Call method under test
            pacsDequeueThread.runTask();

            // Verify that C-MOVE was attempted
            verify(dqrService, times(1)).importFromPacsRequest(any(ExecutedPacsRequest.class));

            // Verify call was made to find sessions in prearchive by uid
            prearcDbMock.verify(() -> PrearcDatabase.getSessionByUID(studyInstanceUid), times(1));

            // Verify that the session rebuild request was not submitted
            xdatMock.verify(() -> XDAT.sendJmsRequest(any()), times(0));
        }
    }

    @Test
    public void testSuccessfulCmove_noProject_noSubmitRebuildRequest() throws Exception {
        // Set up test data
        final String studyInstanceUid = RandomStringUtils.randomAlphabetic(5);
        final String username = RandomStringUtils.randomAlphabetic(5);
        final int numAvailableThreads = ThreadLocalRandom.current().nextInt(1, 3);

        // When no project is set we should not attempt to do anything with the prearchive data
        final String project = "";

        when(pacsAvailabilityService.findAvailableNow(pacsId)).thenReturn(Optional.of(pacsAvailability));
        when(pacsAvailability.getThreads()).thenReturn(numAvailableThreads);
        when(pacsAvailability.getUtilizationPercent()).thenReturn(100);
        when(threads.isOversubscribed(pacsId, numAvailableThreads)).thenReturn(false);
        when(primaryAdminUserProvider.get()).thenReturn(admin);
        when(pacsService.retrieve(pacsId)).thenReturn(pacs);
        when(dqrService.canConnect(admin, pacs)).thenReturn(true);
        when(admin.getUsername()).thenReturn("admin");

        final QueuedPacsRequest queuedPacsRequest = QueuedPacsRequest.builder()
                .seriesIds(Collections.emptyList())
                .studyInstanceUid(studyInstanceUid)
                .xnatProject(project)
                .username(username)
                .build();
        when(queuedPacsRequestService.getQueuedOrFailedForPacsOrderedByPriorityAndDate(pacsId))
                .thenReturn(Collections.singletonList(queuedPacsRequest))
                .thenReturn(Collections.emptyList());  // This will break the loop in the method under test after one iteration

        try (MockedStatic<PrearcDatabase> prearcDbMock = mockStatic(PrearcDatabase.class);
             MockedStatic<Users> usersMock = mockStatic(Users.class);
             MockedStatic<PersistentWorkflowUtils> workflowUtilsMock = mockStatic(PersistentWorkflowUtils.class);
             MockedStatic<XDAT> xdatMock = mockStatic(XDAT.class)) {
            // This is an uninteresting bit which we must mock so other parts of the code under test don't break
            usersMock.when(() -> Users.getUser(username)).thenReturn(user);
            workflowUtilsMock.when(() -> PersistentWorkflowUtils.buildOpenWorkflow(eq(user), any(), eq(studyInstanceUid), eq(project), any()))
                    .thenReturn(workflow);

            // Call method under test
            pacsDequeueThread.runTask();

            // Verify that C-MOVE was attempted
            verify(dqrService, times(1)).importFromPacsRequest(any(ExecutedPacsRequest.class));

            // Verify no calls were made to find sessions in prearchive by uid
            prearcDbMock.verify(() -> PrearcDatabase.getSessionByUID(studyInstanceUid), times(0));

            // Verify that the session rebuild request was not submitted
            xdatMock.verify(() -> XDAT.sendJmsRequest(any()), times(0));
        }
    }
}
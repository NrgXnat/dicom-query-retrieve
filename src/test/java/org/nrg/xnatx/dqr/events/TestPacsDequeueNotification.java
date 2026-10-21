package org.nrg.xnatx.dqr.events;

import org.junit.jupiter.api.Test;
import org.nrg.xnatx.dqr.dicom.RetrieveLevel;
import org.nrg.xnatx.dqr.domain.entities.ExecutedPacsRequest;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The import notification counted the series it had requested, which reads as "0 selected DICOM
 * series requested" for a study-level import that asked for everything.
 */
class TestPacsDequeueNotification {

    @Test
    void aStudyLevelImportIsAnnouncedAsAStudyRatherThanACountOfNothing() {
        assertThat(PacsDequeueThread.notificationSubject(request(RetrieveLevel.STUDY, Collections.emptyList()), "XNAT"))
                .isEqualTo("[XNAT] Complete DICOM study requested");
    }

    @Test
    void aSeriesLevelImportStillCountsWhatWasRequested() {
        assertThat(PacsDequeueThread.notificationSubject(request(RetrieveLevel.SERIES, Arrays.asList("1.2.3.1", "1.2.3.2")), "XNAT"))
                .isEqualTo("[XNAT] 2 selected DICOM series requested");
    }

    @Test
    void aRequestWithNoStoredLevelIsTreatedAsSeriesLevel() {
        assertThat(PacsDequeueThread.notificationSubject(request(null, Collections.singletonList("1.2.3.1")), "XNAT"))
                .isEqualTo("[XNAT] 1 selected DICOM series requested");
    }

    private static ExecutedPacsRequest request(final RetrieveLevel retrieveLevel, final List<String> seriesIds) {
        final ExecutedPacsRequest request = ExecutedPacsRequest.builder()
                .studyInstanceUid("1.2.840.113619.2.55.3")
                .seriesIds(seriesIds)
                .build();
        request.setRetrieveLevel(retrieveLevel);
        return request;
    }
}

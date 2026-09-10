package org.nrg.xnatx.dqr.services.impl;

import org.apache.commons.lang3.RandomStringUtils;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.net.Status;
import org.junit.jupiter.api.Test;
import org.nrg.xnatx.dqr.domain.Series;
import org.nrg.xnatx.dqr.domain.Study;
import org.nrg.xnatx.dqr.domain.entities.Pacs;
import org.nrg.xnatx.dqr.exceptions.DqrException;
import org.nrg.xnatx.dqr.exceptions.PacsException;
import org.nrg.xnatx.dqr.exceptions.PacsRetrieveNotSupportedException;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Covers the identifier keys sent for each kind of retrieve, and the exception types that reach the
 * caller. The C-MOVE itself is stubbed out: what matters here is what would go on the wire.
 */
class TestDimsePacsClientServiceRetrieve {

    private static final String STUDY_UID  = RandomStringUtils.randomNumeric(20);
    private static final String SERIES_UID = RandomStringUtils.randomNumeric(20);
    private static final String DESTINATION_AE = "XNAT";

    /**
     * Captures what would be sent to the PACS instead of opening an association.
     */
    private static class CapturingDimsePacsClientService extends DimsePacsClientService {
        private Attributes capturedKeys;
        private String     capturedDestination;
        private PacsException failWith;

        CapturingDimsePacsClientService() {
            super(null, null, Collections.emptyMap());
        }

        @Override
        protected void doCMove(final Attributes attributes, final Pacs pacs, final String destination) throws PacsException {
            capturedKeys = attributes;
            capturedDestination = destination;
            if (failWith != null) {
                throw failWith;
            }
        }
    }

    private final CapturingDimsePacsClientService service = new CapturingDimsePacsClientService();

    @Test
    void aStudyRetrieveIdentifiesTheStudyAndNothingBelowIt() throws DqrException {
        service.importStudy(new Pacs(), null, Study.builder().studyInstanceUid(STUDY_UID).build(), DESTINATION_AE);

        assertThat(service.capturedKeys.getString(Tag.QueryRetrieveLevel)).isEqualTo("STUDY");
        assertThat(service.capturedKeys.getString(Tag.StudyInstanceUID)).isEqualTo(STUDY_UID);
        // A series key would contradict the level and give the PACS an identifier it may reject
        assertThat(service.capturedKeys.contains(Tag.SeriesInstanceUID)).isFalse();
        assertThat(service.capturedDestination).isEqualTo(DESTINATION_AE);
    }

    @Test
    void aSeriesRetrieveStillIdentifiesBothTheStudyAndTheSeries() throws DqrException {
        service.importSeries(new Pacs(), null,
                Study.builder().studyInstanceUid(STUDY_UID).build(),
                Series.builder().seriesInstanceUid(SERIES_UID).build(),
                DESTINATION_AE);

        assertThat(service.capturedKeys.getString(Tag.QueryRetrieveLevel)).isEqualTo("SERIES");
        assertThat(service.capturedKeys.getString(Tag.StudyInstanceUID)).isEqualTo(STUDY_UID);
        assertThat(service.capturedKeys.getString(Tag.SeriesInstanceUID)).isEqualTo(SERIES_UID);
    }

    @Test
    void aRefusedStudyRetrieveReachesTheCallerWithItsTypeIntact() {
        // The caller has to be able to tell a refusal from any other failure; wrapping it in a
        // plain DqrException would make that impossible
        service.failWith = new PacsRetrieveNotSupportedException("refused", Status.IdentifierDoesNotMatchSOPClass);

        assertThatThrownBy(() -> service.importStudy(new Pacs(), null, Study.builder().studyInstanceUid(STUDY_UID).build(), DESTINATION_AE))
                .isInstanceOf(PacsRetrieveNotSupportedException.class)
                .isInstanceOf(DqrException.class);
    }

    @Test
    void aRefusedSeriesRetrieveReachesTheCallerWithItsTypeIntact() {
        service.failWith = new PacsRetrieveNotSupportedException("refused", Status.IdentifierDoesNotMatchSOPClass);

        assertThatThrownBy(() -> service.importSeries(new Pacs(), null,
                Study.builder().studyInstanceUid(STUDY_UID).build(),
                Series.builder().seriesInstanceUid(SERIES_UID).build(),
                DESTINATION_AE))
                .isInstanceOf(PacsRetrieveNotSupportedException.class);
    }
}

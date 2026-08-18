package org.nrg.xnatx.dqr.exceptions;

import org.dcm4che3.net.Status;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code RetryablePacsOperation} decides whether to spend another attempt by asking the exception,
 * so these defaults and overrides determine whether a rejected request is retried pointlessly.
 */
class TestPacsExceptionRetryability {

    @Test
    void pacsFailuresAreRetryableUnlessSaidOtherwise() {
        assertThat(new PacsException("something went wrong").isRetryable()).isTrue();
        assertThat(new PacsQueryException("the query failed").isRetryable()).isTrue();
        assertThat(new PacsConnectionException("the connection dropped").isRetryable()).isTrue();
    }

    @Test
    void permanentFailuresAreNotRetryableAndCarryTheirStatus() {
        final PacsPermanentFailureException exception =
                new PacsPermanentFailureException("the move destination is unknown", Status.MoveDestinationUnknown);
        assertThat(exception.isRetryable()).isFalse();
        assertThat(exception.getDicomStatus()).isEqualTo(Status.MoveDestinationUnknown);
    }

    @Test
    void anUnsupportedRetrieveIsAPermanentFailure() {
        final PacsRetrieveNotSupportedException exception =
                new PacsRetrieveNotSupportedException("study-level retrieve refused", Status.IdentifierDoesNotMatchSOPClass);
        assertThat(exception).isInstanceOf(PacsPermanentFailureException.class);
        assertThat(exception.isRetryable()).isFalse();
        assertThat(exception.getDicomStatus()).isEqualTo(Status.IdentifierDoesNotMatchSOPClass);
    }
}

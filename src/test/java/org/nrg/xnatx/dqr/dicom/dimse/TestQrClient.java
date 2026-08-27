package org.nrg.xnatx.dqr.dicom.dimse;

import org.junit.jupiter.api.Test;
import org.nrg.xnatx.dqr.exceptions.PacsConnectionException;

import java.io.IOException;
import java.net.ServerSocket;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Covers what happens when the PACS isn't there at all, which is distinct from a PACS that answers
 * and refuses: one is worth another attempt, the other never will be.
 */
class TestQrClient {

    @Test
    void aPacsThatCannotBeReachedRaisesAConnectionFailure() throws IOException {
        final int unusedPort = findUnusedPort();

        assertThatThrownBy(() -> QrClient.builder()
                .localAe("LOCAL")
                .remoteHost("localhost")
                .remotePort(unusedPort)
                .remoteAe("REMOTE")
                .destination("XNAT")
                .build())
                .isInstanceOf(PacsConnectionException.class)
                // A PACS that can't be reached now may well be reachable on the next attempt, so
                // unlike a refusal this has to stay retryable
                .matches(thrown -> ((PacsConnectionException) thrown).isRetryable());
    }

    /**
     * Takes a port from the ephemeral range and releases it, so the connection attempt is refused
     * rather than left hanging until the connect timeout expires.
     */
    private static int findUnusedPort() throws IOException {
        try (final ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}

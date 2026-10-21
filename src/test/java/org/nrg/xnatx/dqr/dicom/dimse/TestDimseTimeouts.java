package org.nrg.xnatx.dqr.dicom.dimse;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The defaults have to reproduce the values that were hard-coded in {@link QrClient} before these
 * became configurable, so that upgrading without touching the new preferences changes nothing.
 */
class TestDimseTimeouts {

    @Test
    void defaultsMatchThePreviouslyHardCodedValues() {
        final DimseTimeouts defaults = DimseTimeouts.DEFAULTS;
        assertThat(defaults.getResponseTimeoutMs()).isEqualTo(60000);
        assertThat(defaults.getRetrieveTimeoutMs()).isEqualTo(60000);
        assertThat(defaults.getAcceptTimeoutMs()).isEqualTo(60000);
        assertThat(defaults.getConnectTimeoutMs()).isEqualTo(30000);
    }

    @Test
    void unsetValuesFallBackToTheDefaults() {
        final DimseTimeouts timeouts = DimseTimeouts.builder().retrieveTimeoutMs(1800000).build();
        assertThat(timeouts.getRetrieveTimeoutMs()).isEqualTo(1800000);
        assertThat(timeouts.getResponseTimeoutMs()).isEqualTo(DimseTimeouts.DEFAULT_RESPONSE_TIMEOUT_MS);
        assertThat(timeouts.getAcceptTimeoutMs()).isEqualTo(DimseTimeouts.DEFAULT_ACCEPT_TIMEOUT_MS);
        assertThat(timeouts.getConnectTimeoutMs()).isEqualTo(DimseTimeouts.DEFAULT_CONNECT_TIMEOUT_MS);
    }
}

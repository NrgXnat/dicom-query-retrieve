package org.nrg.xnatx.dqr.dicom.dimse;

import lombok.Builder;
import lombok.Value;

/**
 * Timeouts applied to the local connection used for DIMSE operations, in milliseconds.
 * <p>
 * The defaults reproduce the values that were hard-coded in {@link QrClient} before these became
 * configurable. A retrieve that pulls an entire study routinely takes longer than the default
 * response and retrieve timeouts allow, so sites doing study-level retrieves are expected to raise
 * them.
 */
@Value
@Builder
public class DimseTimeouts {
    public static final int DEFAULT_RESPONSE_TIMEOUT_MS = 60000;
    public static final int DEFAULT_RETRIEVE_TIMEOUT_MS = 60000;
    public static final int DEFAULT_ACCEPT_TIMEOUT_MS   = 60000;
    public static final int DEFAULT_CONNECT_TIMEOUT_MS  = 30000;

    /**
     * The timeouts used when a caller doesn't supply any, matching the behavior of the plugin
     * before these values became configurable.
     */
    public static final DimseTimeouts DEFAULTS = DimseTimeouts.builder().build();

    /**
     * How long to wait for a DIMSE response message before giving up on the association.
     */
    @Builder.Default
    int responseTimeoutMs = DEFAULT_RESPONSE_TIMEOUT_MS;

    /**
     * How long to wait between the sub-operation responses of a retrieve. This is the timeout that
     * bounds a C-MOVE, so it's the one that matters most for a long-running study-level retrieve.
     */
    @Builder.Default
    int retrieveTimeoutMs = DEFAULT_RETRIEVE_TIMEOUT_MS;

    /**
     * How long to wait for the remote AE to accept the association request.
     */
    @Builder.Default
    int acceptTimeoutMs = DEFAULT_ACCEPT_TIMEOUT_MS;

    /**
     * How long to wait for the TCP connection to the remote AE to be established.
     */
    @Builder.Default
    int connectTimeoutMs = DEFAULT_CONNECT_TIMEOUT_MS;
}

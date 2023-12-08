package org.nrg.xnatx.dqr.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DicomWebPingResult {
    boolean successful;
    Integer httpStatus;
    String reason;
}

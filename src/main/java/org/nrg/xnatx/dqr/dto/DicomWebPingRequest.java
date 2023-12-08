package org.nrg.xnatx.dqr.dto;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder
public class DicomWebPingRequest {
    String aeTitle;
    String rootUrl;
}

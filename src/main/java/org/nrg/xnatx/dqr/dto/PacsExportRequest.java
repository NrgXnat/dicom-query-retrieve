package org.nrg.xnatx.dqr.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PacsExportRequest {
    /**
     * The ID of the PACS to which the data should be exported.
     */
    private long pacsId;

    /**
     * The ID of the session to be exported.
     */
    private String sessionId;

    /**
     * The IDs of the scans in the session to be exported.
     */
    private List<String> scans;
}

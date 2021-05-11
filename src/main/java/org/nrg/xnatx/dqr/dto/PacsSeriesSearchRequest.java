package org.nrg.xnatx.dqr.dto;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class PacsSeriesSearchRequest {
    /**
     * The ID of the PACS to search.
     */
    private long pacsId;

    /**
     * The study instance UIDs of the sessions to search.
     */
    @Builder.Default
    private List<String> studyInstanceUids = new ArrayList<>();
}

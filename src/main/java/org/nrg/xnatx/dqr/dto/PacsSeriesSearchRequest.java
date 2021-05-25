package org.nrg.xnatx.dqr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PacsSeriesSearchRequest implements Serializable {
    private static final long serialVersionUID = 268284582078420330L;

    /**
     * The ID of the PACS to search.
     */
    long pacsId;

    /**
     * The study instance UIDs of the sessions to search.
     */
    @Builder.Default
    List<String> studyInstanceUids = new ArrayList<>();
}

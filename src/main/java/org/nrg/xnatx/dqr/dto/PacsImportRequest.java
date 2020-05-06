package org.nrg.xnatx.dqr.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.experimental.Accessors;

@Data
@Accessors(prefix = "_")
@Builder
public class PacsImportRequest {
    /**
     * The AE title to which the requested data should be sent.
     */
    private String _ae;

    /**
     * The project to which the requested data should be routed.
     */
    private String _project;

    /**
     * The study instance UID for the requested data.
     */
    private String _studyInstanceUid;

    /**
     * The series IDs for the requested data.
     */
    @Singular
    private List<String> _seriesIds;
}

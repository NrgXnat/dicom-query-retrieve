package org.nrg.xnatx.dqr.dto;

import lombok.*;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PacsExportRequest implements Serializable {
    private static final long serialVersionUID = 3155882003916837310L;

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
    @Singular
    private List<String> scans;
}

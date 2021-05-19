package org.nrg.xnatx.dqr.dto;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.io.Serializable;
import java.util.List;

@Value
@Builder
public class PacsExportRequest implements Serializable {
    private static final long serialVersionUID = 3155882003916837310L;

    /**
     * The ID of the PACS to which the data should be exported.
     */
    long pacsId;

    /**
     * The ID of the session to be exported.
     */
    String sessionId;

    /**
     * The IDs of the scans in the session to be exported.
     */
    @Singular
    List<String> scans;
}

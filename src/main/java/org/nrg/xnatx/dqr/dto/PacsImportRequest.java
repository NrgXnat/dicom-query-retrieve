/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.dto.PacsImportRequest
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.dto;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.List;

@Data
@Builder
public class PacsImportRequest {
    /**
     * The ID of the PACS from which the data should be imported.
     */
    private long pacsId;

    /**
     * The AE title to which the requested data should be sent.
     */
    private String aeTitle;

    /**
     * The port to which the requested data should be sent.
     */
    private int port;

    /**
     * The project to which the requested data should be routed.
     */
    private String projectId;

    @Builder.Default
    private boolean forceImport = false;

    /**
     * The list of studies to be imported.
     */
    @Singular("study")
    private List<StudyImportInformation> studies;
}

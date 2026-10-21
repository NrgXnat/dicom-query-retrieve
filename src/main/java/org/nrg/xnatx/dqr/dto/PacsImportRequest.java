/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.dto.PacsImportRequest
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.dto;

import lombok.*;
import org.nrg.xnatx.dqr.dicom.RetrieveLevel;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PacsImportRequest implements Serializable {
    private static final long serialVersionUID = 1012946910048078606L;

    /**
     * The ID of the PACS from which the data should be imported.
     */
    long pacsId;

    /**
     * The AE title to which the requested data should be sent.
     */
    String aeTitle;

    /**
     * The port to which the requested data should be sent.
     */
    int port;

    /**
     * The project to which the requested data should be routed.
     */
    String projectId;

    /**
     * An identifier for this request
     */
    String requestId;

    @Builder.Default
    boolean forceImport = false;

    /**
     * The query/retrieve level to use for this import, overriding whatever the PACS is configured
     * for. Null means the PACS setting applies. A study can only be retrieved at the study level
     * when the whole study is wanted, so this is ignored for any study that names the series to
     * import.
     */
    RetrieveLevel retrieveLevel;

    /**
     * The list of studies to be imported.
     */
    @Singular("study")
    List<StudyImportInformation> studies;
}

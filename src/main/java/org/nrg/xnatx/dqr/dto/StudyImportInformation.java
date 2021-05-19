/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.dto.StudyImportInformation
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.dto;

import lombok.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StudyImportInformation implements Serializable {
    private static final long serialVersionUID = -8705317972189793781L;

    @NonNull
    private String studyInstanceUid;

    @NonNull
    @Builder.Default
    private List<String> seriesDescriptions = new ArrayList<>();

    @NonNull
    @Builder.Default
    private List<String> seriesInstanceUids = new ArrayList<>();

    private String anonScript;

    @NonNull
    @Builder.Default
    private Map<String, String> relabelMap = new HashMap<>();

    @Override
    public String toString() {
        return "StudyImportInformation{" +
               "studyInstanceUid='" + studyInstanceUid +
               "', seriesDescriptions='" + seriesDescriptions +
               "', anonScript='" + anonScript +
               "', seriesInstanceUIDs=" + seriesInstanceUids +
               '}';
    }
}

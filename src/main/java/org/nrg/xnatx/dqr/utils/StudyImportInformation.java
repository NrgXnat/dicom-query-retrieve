/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.utils.StudyImportInformation
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.utils;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class StudyImportInformation {
    private List<String>        seriesDescriptions;
    private List<String>        seriesInstanceUids;
    private String              anonScript;
    private Map<String, String> relabelMap;

    @Override
    public String toString() {
        return "StudyImportInformation{" +
               "seriesDescriptions=" + seriesDescriptions +
               ", anonScript='" + anonScript + '\'' +
               ", seriesInstanceUIDs=" + seriesInstanceUids +
               '}';
    }
}

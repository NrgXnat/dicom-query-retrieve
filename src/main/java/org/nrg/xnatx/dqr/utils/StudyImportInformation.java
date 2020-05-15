/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.utils.StudyImportInformation
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.utils;

import java.util.List;
import java.util.Map;

public class StudyImportInformation {

    public StudyImportInformation() {
    }

    public StudyImportInformation(List<String> seriesDescriptions, List<String> seriesInstanceUIDs, String anonScript, Map<String, String> relabelMap) {
        this.seriesDescriptions = seriesDescriptions;
        this.seriesInstanceUIDs = seriesInstanceUIDs;
        this.anonScript = anonScript;
        this.relabelMap = relabelMap;
    }

    protected List<String>        seriesDescriptions;
    protected List<String>        seriesInstanceUIDs;
    protected String              anonScript;
    protected Map<String, String> relabelMap;

    public List<String> getSeriesDescriptions() {
        return seriesDescriptions;
    }

    public void setSeriesDescriptions(List<String> seriesDescriptions) {
        this.seriesDescriptions = seriesDescriptions;
    }

    public List<String> getSeriesInstanceUIDs() {
        return seriesInstanceUIDs;
    }

    public void setSeriesInstanceUIDs(List<String> seriesInstanceUIDs) {
        this.seriesInstanceUIDs = seriesInstanceUIDs;
    }

    public String getAnonScript() {
        return anonScript;
    }

    public void setAnonScript(String anonScript) {
        this.anonScript = anonScript;
    }

    public Map<String, String> getRelabelMap() {
        return relabelMap;
    }

    public void setRelabelMap(Map<String, String> relabelMap) {
        this.relabelMap = relabelMap;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        StudyImportInformation info = (StudyImportInformation) o;

        if (seriesDescriptions != null ? !seriesDescriptions.equals(info.seriesDescriptions) : info.seriesDescriptions != null) {
            return false;
        }
        if (anonScript != null ? !anonScript.equals(info.anonScript) : info.anonScript != null) {
            return false;
        }
        return seriesInstanceUIDs != null ? seriesInstanceUIDs.equals(info.seriesInstanceUIDs) : info.seriesInstanceUIDs == null;
    }

    @Override
    public int hashCode() {
        int result = seriesDescriptions != null ? seriesDescriptions.hashCode() : 0;
        result = 31 * result + (anonScript != null ? anonScript.hashCode() : 0);
        result = 31 * result + (seriesInstanceUIDs != null ? seriesInstanceUIDs.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "StudyImportInformation{" +
               "seriesDescriptions=" + seriesDescriptions +
               ", anonScript='" + anonScript + '\'' +
               ", seriesInstanceUIDs=" + seriesInstanceUIDs +
               '}';
    }
}

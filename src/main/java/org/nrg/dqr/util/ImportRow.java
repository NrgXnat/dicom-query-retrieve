package org.nrg.dqr.util;

import java.util.List;
import java.util.Map;

public class ImportRow {

    public ImportRow() {
    }

    public ImportRow(Map<String,String> relabelMap, List<String> studyInstanceUIDs) {
        this.relabelMap = relabelMap;
        this.studyInstanceUIDs = studyInstanceUIDs;
    }

    protected Map<String,String> relabelMap;
    protected List<String> studyInstanceUIDs;

    public Map<String,String> getRelabelMap() {
        return relabelMap;
    }

    public void setRelabelMap(Map<String,String> relabelMap) {
        this.relabelMap = relabelMap;
    }

    public List<String> getStudyInstanceUIDs() {
        return studyInstanceUIDs;
    }

    public void setStudyInstanceUIDs(List<String> studyInstanceUIDs) {
        this.studyInstanceUIDs = studyInstanceUIDs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ImportRow importRow = (ImportRow) o;

        if (relabelMap != null ? !relabelMap.equals(importRow.relabelMap) : importRow.relabelMap != null) return false;
        return studyInstanceUIDs != null ? studyInstanceUIDs.equals(importRow.studyInstanceUIDs) : importRow.studyInstanceUIDs == null;
    }

    @Override
    public int hashCode() {
        int result = relabelMap != null ? relabelMap.hashCode() : 0;
        result = 31 * result + (studyInstanceUIDs != null ? studyInstanceUIDs.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ImportRow{" +
                "anonScript='" + relabelMap + '\'' +
                ", studyInstanceUIDs=" + studyInstanceUIDs +
                '}';
    }
}

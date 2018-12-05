/*
 * Pacs
 * DQR is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.nrg.dqr.util;

import org.nrg.dqr.domain.Study;
import org.nrg.dqr.dto.PacsSearchCriteria;

import java.util.List;

public class SimpleCsvRow {

    public SimpleCsvRow() {
    }

    public SimpleCsvRow(String anonScript, List<String> studyInstanceUIDs) {
        this.anonScript = anonScript;
        this.studyInstanceUIDs = studyInstanceUIDs;
    }

    protected String anonScript;
    protected List<String> studyInstanceUIDs;

    public String getAnonScript() {
        return anonScript;
    }

    public void setAnonScript(String anonScript) {
        this.anonScript = anonScript;
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

        SimpleCsvRow csvRow = (SimpleCsvRow) o;

        if (anonScript != null ? !anonScript.equals(csvRow.anonScript) : csvRow.anonScript != null) return false;
        return studyInstanceUIDs != null ? studyInstanceUIDs.equals(csvRow.studyInstanceUIDs) : csvRow.studyInstanceUIDs == null;
    }

    @Override
    public int hashCode() {
        int result = anonScript != null ? anonScript.hashCode() : 0;
        result = 31 * result + (studyInstanceUIDs != null ? studyInstanceUIDs.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "CsvRow{" +
                "anonScript='" + anonScript + '\'' +
                ", studyInstanceUIDs=" + studyInstanceUIDs +
                '}';
    }
}

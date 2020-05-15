/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.utils.ImportRequest
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.utils;

import java.util.List;

public class ImportRequest {

    public ImportRequest() {
    }

    public ImportRequest(List<ImportRow> importRows, List<String> seriesDescriptions) {
        this.importRows = importRows;
        this.seriesDescriptions = seriesDescriptions;
    }

    protected List<ImportRow> importRows;
    protected List<String>    seriesDescriptions;

    public List<ImportRow> getImportRows() {
        return importRows;
    }

    public void setImportRows(List<ImportRow> importRows) {
        this.importRows = importRows;
    }

    public List<String> getSeriesDescriptions() {
        return seriesDescriptions;
    }

    public void setSeriesDescriptions(List<String> seriesDescriptions) {
        this.seriesDescriptions = seriesDescriptions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ImportRequest that = (ImportRequest) o;

        if (importRows != null ? !importRows.equals(that.importRows) : that.importRows != null) {
            return false;
        }
        return seriesDescriptions != null ? seriesDescriptions.equals(that.seriesDescriptions) : that.seriesDescriptions == null;
    }

    @Override
    public int hashCode() {
        int result = importRows != null ? importRows.hashCode() : 0;
        result = 31 * result + (seriesDescriptions != null ? seriesDescriptions.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ImportRequest{" +
               "importRows=" + importRows +
               ", seriesDescriptions=" + seriesDescriptions +
               '}';
    }

}

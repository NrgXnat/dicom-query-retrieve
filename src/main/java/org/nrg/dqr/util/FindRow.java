package org.nrg.dqr.util;

import org.nrg.dqr.domain.Study;
import org.nrg.dqr.dto.PacsSearchCriteria;

import java.util.List;
import java.util.Map;

public class FindRow {

    public FindRow() {
    }

    public FindRow(PacsSearchCriteria criteria, Map<String,String> relabelMap, List<Study> studies) {
        this.criteria = criteria;
        this.relabelMap = relabelMap;
        this.studies = studies;
    }

    protected PacsSearchCriteria criteria;
    protected Map<String,String> relabelMap;
    protected List<Study> studies;

    public PacsSearchCriteria getCriteria() {
        return criteria;
    }

    public void setCriteria(PacsSearchCriteria criteria) {
        this.criteria = criteria;
    }

    public Map<String,String> getRelabelMap() {
        return relabelMap;
    }

    public void setRelabelMap(Map<String,String> relabelMap) {
        this.relabelMap = relabelMap;
    }

    public List<Study> getStudies() {
        return studies;
    }

    public void setStudies(List<Study> studies) {
        this.studies = studies;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FindRow findRow = (FindRow) o;

        if (criteria != null ? !criteria.equals(findRow.criteria) : findRow.criteria != null) return false;
        if (relabelMap != null ? !relabelMap.equals(findRow.relabelMap) : findRow.relabelMap != null) return false;
        return studies != null ? studies.equals(findRow.studies) : findRow.studies == null;
    }

    @Override
    public int hashCode() {
        int result = criteria != null ? criteria.hashCode() : 0;
        result = 31 * result + (relabelMap != null ? relabelMap.hashCode() : 0);
        result = 31 * result + (studies != null ? studies.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "FindRow{" +
                "criteria=" + criteria +
                ", anonScript='" + relabelMap + '\'' +
                ", studies=" + studies +
                '}';
    }
}

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

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.nrg.dqr.domain.Study;
import org.nrg.dqr.domain.entities.PacsRequest;
import org.nrg.dqr.dto.PacsSearchCriteria;

import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.List;

public class CsvRow {

    public CsvRow() {
    }

    public CsvRow(PacsSearchCriteria criteria, String anonScript, List<Study> studies) {
        this.criteria = criteria;
        this.anonScript = anonScript;
        this.studies = studies;
    }

    protected PacsSearchCriteria criteria;
    protected String anonScript;
    protected List<Study> studies;

    public PacsSearchCriteria getCriteria() {
        return criteria;
    }

    public void setCriteria(PacsSearchCriteria criteria) {
        this.criteria = criteria;
    }

    public String getAnonScript() {
        return anonScript;
    }

    public void setAnonScript(String anonScript) {
        this.anonScript = anonScript;
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

        CsvRow csvRow = (CsvRow) o;

        if (criteria != null ? !criteria.equals(csvRow.criteria) : csvRow.criteria != null) return false;
        if (anonScript != null ? !anonScript.equals(csvRow.anonScript) : csvRow.anonScript != null) return false;
        return studies != null ? studies.equals(csvRow.studies) : csvRow.studies == null;
    }

    @Override
    public int hashCode() {
        int result = criteria != null ? criteria.hashCode() : 0;
        result = 31 * result + (anonScript != null ? anonScript.hashCode() : 0);
        result = 31 * result + (studies != null ? studies.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "CsvRow{" +
                "criteria=" + criteria +
                ", anonScript='" + anonScript + '\'' +
                ", studies=" + studies +
                '}';
    }
}

/*
 * PacsSessionExportRequest
 * DQR is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.nrg.dqr.messaging;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.nrg.dqr.domain.entities.Pacs;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xft.security.UserI;

public class PacsSessionExportRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Pacs pacs;

    private XnatImagesessiondata session;

    private Date dateRequested;

    private UserI requestingUser;

    private List<PacsScanExportRequest> scans;

    public PacsSessionExportRequest() {
        scans = new ArrayList<PacsScanExportRequest>();
    }

    public Pacs getPacs() {
        return pacs;
    }

    public void setPacs(final Pacs pacs) {
        this.pacs = pacs;
    }

    public XnatImagesessiondata getSession() {
        return session;
    }

    public void setSession(final XnatImagesessiondata session) {
        this.session = session;
    }

    public Date getDateRequested() {
        return dateRequested;
    }

    public void setDateRequested(final Date dateRequested) {
        this.dateRequested = dateRequested;
    }

    public UserI getRequestingUser() {
        return requestingUser;
    }

    public void setRequestingUser(final UserI requestingUser) {
        this.requestingUser = requestingUser;
    }

    public List<PacsScanExportRequest> getScans() {
        return scans;
    }

    public void setScans(final List<PacsScanExportRequest> scans) {
        this.scans = scans;
    }

    public void addScan(final PacsScanExportRequest pacsScanExportRequest) {
        scans.add(pacsScanExportRequest);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(89, 167).append(pacs).append(dateRequested).append(requestingUser).toHashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        final PacsSessionExportRequest other = (PacsSessionExportRequest) obj;
        return new EqualsBuilder().append(pacs, other.pacs).append(dateRequested, other.dateRequested)
                .append(requestingUser, other.requestingUser).isEquals();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append(pacs).append(session == null ? "null" : session.getId())
                .append(dateRequested).append(requestingUser == null ? "null" : requestingUser.getLogin())
                .append(scans).toString();
    }
}

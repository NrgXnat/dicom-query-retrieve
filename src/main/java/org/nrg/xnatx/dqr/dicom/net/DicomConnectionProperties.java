/*
 * DicomConnectionProperties
 * DQR is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.nrg.xnatx.dqr.dicom.net;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.nrg.xnatx.dqr.domain.entities.Pacs;

import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlTransient;

public final class DicomConnectionProperties {

    private final String localAeTitle;

    private final Pacs pacs;

    public DicomConnectionProperties(final String localAeTitle, final Pacs pacs) {
        this.localAeTitle = localAeTitle;
        this.pacs = pacs;
    }

    public String getLocalAeTitle() {
        return localAeTitle;
    }

    public String getRemoteHost() {
        return pacs.getHost();
    }

    public Integer getRemoteQueryRetrievePort() {
        return pacs.getQueryRetrievePort();
    }

    public Integer getRemoteStoragePort() {
        return pacs.getQueryRetrievePort();
    }

    /**
     * This is a decision-making property: it returns "the" remote port based on the settings of the implemented
     * {@link #getRemoteQueryRetrievePort() query/retrieve port}.
     * By default, if the query/retrieve port is not empty, that will be "the" remote port. If that is empty,
     * then the default DICOM port setting of 104 is returned.
     *
     * @return The remote port to use for DICOM connections.
     */
    @Transient
    @XmlTransient
    @JsonIgnore
    public int getRemotePort() {
        if (pacs.getQueryRetrievePort() != null) {
            return pacs.getQueryRetrievePort();
        }
        return 104;
    }

    public String getRemoteAeTitle() {
        return pacs.getAeTitle();
    }

    public boolean getSupportsExtendedNegotiations() {
        return pacs.isSupportsExtendedNegotiations();
    }
}

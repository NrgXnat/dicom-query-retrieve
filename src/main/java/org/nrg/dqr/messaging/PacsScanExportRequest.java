/*
 * PacsScanExportRequest
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

import org.nrg.xdat.om.XnatImagescandata;

public class PacsScanExportRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private XnatImagescandata scan;

    public XnatImagescandata getScan() {
        return scan;
    }

    public void setScan(final XnatImagescandata scan) {
        this.scan = scan;
    }

    @Override
    public String toString() {
        return scan == null ? "null" : scan.getId();
    }
}

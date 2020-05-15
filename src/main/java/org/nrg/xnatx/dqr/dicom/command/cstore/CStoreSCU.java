/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.dicom.command.cstore.CStoreSCU
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.dicom.command.cstore;

import org.nrg.xdat.om.XnatImagescandata;

public interface CStoreSCU {

    CStoreResults cstoreSeries(XnatImagescandata series) throws CStoreFailureException;
}

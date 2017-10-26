/*
 * org.nrg.tip.dicom.command.cstore.CStoreSCU
 * TIP is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.nrg.tip.dicom.command.cstore;

import org.nrg.xdat.om.XnatImagescandata;

public interface CStoreSCU {

    CStoreResults cstoreSeries(XnatImagescandata series) throws CStoreFailureException;
}

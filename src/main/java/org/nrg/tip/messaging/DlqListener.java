/*
 * org.nrg.tip.messaging.DlqListener
 * TIP is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.nrg.tip.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DlqListener {

    private final static Logger log = LoggerFactory.getLogger(DlqListener.class);

    public void onReceiveDeadLetter(final Object o) throws Exception {
        if (o instanceof PacsStudyImportRequest) {
            new PacsStudyImportRequestDlqListener().onPacsStudyImportRequest((PacsStudyImportRequest) o);
        } else if (o instanceof PacsSessionExportRequest) {
            new PacsSessionExportRequestDlqListener().onPacsSessionExportRequest((PacsSessionExportRequest) o);
        } else {
            final String error = "Received dead letter of unexpected type: " + (o == null ? "null" : o.getClass());
            log.error(error);
            final RuntimeException e = new RuntimeException(error);
            throw e;
        }
    }
}

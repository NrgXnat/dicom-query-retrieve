/*
 * DlqListener
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

import lombok.extern.slf4j.Slf4j;

@SuppressWarnings("unused")
@Slf4j
public class DlqListener {
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

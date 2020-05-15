/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.dicom.command.cstore.CStoreFailureException
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.dicom.command.cstore;

public class CStoreFailureException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final CStoreResults cstoreResults;

    public CStoreFailureException(final String arg0) {
        super(arg0);
        cstoreResults = null;
    }

    public CStoreFailureException(String message, Throwable cause) {
        super(message, cause);
        cstoreResults = null;
    }

    public CStoreFailureException(Throwable cause, final CStoreResults cstoreResults) {
        super(cause);
        this.cstoreResults = cstoreResults;
    }

    public CStoreFailureException(final CStoreResults cstoreResults) {
        this.cstoreResults = cstoreResults;
    }

    public CStoreResults getCstoreFailures() {
        return cstoreResults;
    }

    @Override
    public String toString() {
        return super.toString() + (null == getCstoreFailures() ? "" : getCstoreFailures().toString());
    }
}

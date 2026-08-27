/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.exceptions.PacsPermanentFailureException
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2025, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.exceptions;

/**
 * Thrown when a PACS refuses an operation in a way that submitting the identical request again
 * cannot resolve, such as rejecting the identifier or not recognizing the move destination.
 * Operations wrapped in a retry will fail fast on this rather than consuming their remaining
 * attempts.
 */
public class PacsPermanentFailureException extends PacsException {
    private static final long serialVersionUID = 6014576213498723355L;

    public PacsPermanentFailureException(final String message, final int dicomStatus) {
        super(message);
        _dicomStatus = dicomStatus;
    }

    public PacsPermanentFailureException(final String message, final int dicomStatus, final Exception cause) {
        super(message, cause);
        _dicomStatus = dicomStatus;
    }

    /**
     * The DIMSE status returned by the PACS, or -1 when the failure wasn't reported as a status.
     *
     * @return The DIMSE status.
     */
    public int getDicomStatus() {
        return _dicomStatus;
    }

    @Override
    public boolean isRetryable() {
        return false;
    }

    private final int _dicomStatus;
}

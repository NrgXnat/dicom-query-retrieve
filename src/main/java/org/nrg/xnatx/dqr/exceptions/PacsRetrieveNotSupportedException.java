/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.exceptions.PacsRetrieveNotSupportedException
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2025, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.exceptions;

/**
 * Thrown when a PACS rejects a retrieve as formed rather than failing to carry it out: it didn't
 * accept a presentation context for the operation, or it answered with a status meaning the
 * identifier or SOP class was unacceptable. In practice this is how a PACS says it doesn't support
 * a retrieve at the query/retrieve level that was requested, so it's the signal to reissue the
 * request a different way instead of reissuing it unchanged.
 */
public class PacsRetrieveNotSupportedException extends PacsPermanentFailureException {
    private static final long serialVersionUID = 2172846905530112884L;

    public PacsRetrieveNotSupportedException(final String message, final int dicomStatus) {
        super(message, dicomStatus);
    }

    public PacsRetrieveNotSupportedException(final String message, final int dicomStatus, final Exception cause) {
        super(message, dicomStatus, cause);
    }
}

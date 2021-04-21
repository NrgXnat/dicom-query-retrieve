/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.exceptions.InvalidStudyDateRangeException
 * XNAT https://www.xnat.org
 * Copyright (c) 2005-2021, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.exceptions;

public class InvalidStudyDateRangeException extends Exception {
    private static final long serialVersionUID = 687971493884085033L;

    public InvalidStudyDateRangeException(String message) {
        super(message);
    }
}

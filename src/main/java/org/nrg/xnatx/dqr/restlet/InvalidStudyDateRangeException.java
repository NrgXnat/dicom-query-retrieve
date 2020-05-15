/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.restlet.InvalidStudyDateRangeException
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.restlet;

public class InvalidStudyDateRangeException extends Exception {

    private static final long serialVersionUID = 1L;

    public InvalidStudyDateRangeException(String message) {
        super(message);
    }
}

/*
 * CStoreResults
 * DQR is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.nrg.xnatx.dqr.dicom.command.cstore;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CStoreResults {

    public static class CStoreFailure {
        private final String filename;
        private final String errorMessage;

        public CStoreFailure(final String filename, final String errorMessage) {
            this.filename = filename;
            this.errorMessage = errorMessage;
        }

        public String getFilename() {
            return filename;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        @Override
        public String toString() {
            return "Filename: " + getFilename() + ", error: " + getErrorMessage();
        }
    }

    public static class CStoreSuccess {
        private final String filename;

        public CStoreSuccess(final String filename) {
            this.filename = filename;
        }

        public String getFilename() {
            return filename;
        }

        @Override
        public String toString() {
            return "Filename: " + getFilename();
        }
    }

    private final List<CStoreSuccess> successes;

    private final List<CStoreFailure> failures;

    public CStoreResults() {
        successes = new ArrayList<CStoreSuccess>();
        failures = new ArrayList<CStoreFailure>();
    }

    public void addSuccess(final CStoreSuccess success) {
        successes.add(success);
    }

    public Iterator<CStoreSuccess> getSuccesses() {
        return successes.iterator();
    }

    public void addFailure(final CStoreFailure failure) {
        failures.add(failure);
    }

    public Iterator<CStoreFailure> getFailures() {
        return failures.iterator();
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder(1000);

        b.append("List of C-STORE Successes:\n");
        for (CStoreSuccess s : successes) {
            b.append(s.toString()).append("\n");
        }

        b.append("\nList of C-STORE Failures:\n");
        for (CStoreFailure f : failures) {
            b.append(f.toString()).append("\n");
        }

        return b.toString();
    }
}

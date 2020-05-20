/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.dicom.strategy.orm.BasicPatientNameStrategy
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.dicom.strategy.orm;

import org.apache.commons.lang3.StringUtils;
import org.dcm4che2.data.PersonName;
import org.nrg.xnatx.dqr.domain.DqrPersonName;
import org.nrg.xnatx.dqr.dto.PacsSearchCriteria;

public class BasicPatientNameStrategy implements PatientNameStrategy {
    @Override
    public DqrPersonName dicomPatientNameToDqrPatientName(final String dicomPatientName) {
        return new DqrPersonName(new PersonName(dicomPatientName));
    }

    @Override
    public DicomPersonNameSearchCriteria dqrSearchCriteriaToDicomSearchCriteria(PacsSearchCriteria searchCriteria) {
        DicomPersonNameSearchCriteria dicomPersonNameSearchCriteria = new DicomPersonNameSearchCriteria();
        if (StringUtils.isBlank(searchCriteria.getPatientName())) {
            dicomPersonNameSearchCriteria.addCriterion("");
        } else {
            String  patientNameString = myTrim(searchCriteria.getPatientName());
            boolean isQuoted          = patientNameString.startsWith("\"") & patientNameString.endsWith(("\""));
            boolean containsCaret     = patientNameString.contains("^");
            boolean containsComma     = patientNameString.contains(",");
            boolean containsSpace     = patientNameString.contains(" ");

            String processedPatientNameString = patientNameString;
            if (isQuoted) {
                processedPatientNameString = removeBoundingQuotes(patientNameString);
            } else if (containsCaret && containsComma && containsSpace) {
                processedPatientNameString = parseWithCaretCommaSpace(patientNameString);
            } else if (containsCaret && containsComma) {
                processedPatientNameString = parseWithCaretComma(patientNameString);
            } else if (containsCaret && containsSpace) {
                processedPatientNameString = parseWithCaret(patientNameString);
            } else if (containsComma && containsSpace) {
                processedPatientNameString = parseWithComma(patientNameString);
            } else if (containsCaret) {
                processedPatientNameString = parseWithCaret(patientNameString);
            } else if (containsComma) {
                processedPatientNameString = parseWithComma(patientNameString);
            } else if (containsSpace) {
                processedPatientNameString = parseWithSpace(patientNameString);
            }
            dicomPersonNameSearchCriteria.addCriterion(processedPatientNameString);
        }
        return dicomPersonNameSearchCriteria;
    }

    private static String removeBoundingQuotes(final String name) {
        return StringUtils.unwrap(name, "\"");
    }

    private static String myTrim(final String name) {
        return name.trim()
                   .replaceAll(" [ ]*", " ")
                   .replaceAll("[ ]*\\^[ ]*", "^")
                   .replaceAll("[ ]*,[ ]*", ",")
                   .replaceAll("[ ]*\\.[ ]*", ".")
                   .replaceAll("[ ]*'[ ]*", "'");
    }

    private static String parseWithCaretCommaSpace(final String name) {
        return name;
    }

    private static String parseWithCaretComma(final String name) {
        return name;
    }

    private static String parseWithCaret(final String name) {
        return name;
    }

    private static String parseWithComma(final String name) {
        return StringUtils.join(name.trim().split("\\s*,\\s*", 2), "^");
    }

    private static String parseWithSpace(final String name) {
        return StringUtils.join(name.trim().split("\\s+", 2), "^");
    }
}

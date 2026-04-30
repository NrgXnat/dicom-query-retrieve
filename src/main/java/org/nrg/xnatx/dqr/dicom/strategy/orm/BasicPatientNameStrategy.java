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
import org.dcm4che3.data.PersonName;
import org.nrg.xnatx.dqr.domain.DqrPersonName;
import org.nrg.xnatx.dqr.dto.PacsSearchCriteria;

import javax.annotation.Nonnull;

public class BasicPatientNameStrategy implements PatientNameStrategy {

    public static final String REGEX_DELIMITERS   = "^\\s*(?<first>\\S*)\\s*(?<delimiter>[\\^,.']?)\\s*(?<second>.*)$";
    public static final String REPLACE_DELIMITERS = "${first}${delimiter}${second}";

    @Override
    public DqrPersonName dicomPatientNameToDqrPatientName(final String dicomPatientName) {
        return new DqrPersonName(new PersonName(dicomPatientName));
    }

    @Override
    public DicomPersonNameSearchCriteria dqrSearchCriteriaToDicomSearchCriteria(final PacsSearchCriteria searchCriteria) {
        final DicomPersonNameSearchCriteria dicomPersonNameSearchCriteria = new DicomPersonNameSearchCriteria();
        final String                        patientName                   = searchCriteria.getPatientName();
        if (StringUtils.isBlank(patientName)) {
            dicomPersonNameSearchCriteria.addCriterion("");
        } else {
            final String  patientNameString = myTrim(patientName);
            final boolean isQuoted          = patientNameString.startsWith("\"") & patientNameString.endsWith(("\""));
            final boolean containsCaret     = patientNameString.contains("^");
            final boolean containsComma     = patientNameString.contains(",");
            final boolean containsSpace     = patientNameString.contains(" ");

            if (isQuoted) {
                dicomPersonNameSearchCriteria.addCriterion(removeBoundingQuotes(patientNameString));
            } else if (containsCaret && containsComma && containsSpace) {
                dicomPersonNameSearchCriteria.addCriterion(parseWithCaretCommaSpace(patientNameString));
            } else if (containsCaret && containsComma) {
                dicomPersonNameSearchCriteria.addCriterion(parseWithCaretComma(patientNameString));
            } else if (containsCaret && containsSpace) {
                dicomPersonNameSearchCriteria.addCriterion(parseWithCaret(patientNameString));
            } else if (containsComma && containsSpace) {
                dicomPersonNameSearchCriteria.addCriterion(parseWithComma(patientNameString));
            } else if (containsCaret) {
                dicomPersonNameSearchCriteria.addCriterion(parseWithCaret(patientNameString));
            } else if (containsComma) {
                dicomPersonNameSearchCriteria.addCriterion(parseWithComma(patientNameString));
            } else if (containsSpace) {
                dicomPersonNameSearchCriteria.addCriterion(parseWithSpace(patientNameString));
            } else {
                dicomPersonNameSearchCriteria.addCriterion(patientNameString);
            }
        }
        return dicomPersonNameSearchCriteria;
    }

    private static String removeBoundingQuotes(final String name) {
        return StringUtils.unwrap(name, "\"");
    }

    private static String myTrim(@Nonnull final String name) {
        if (StringUtils.containsAny(name, "^,.'")) {
            return name.replaceAll(REGEX_DELIMITERS, REPLACE_DELIMITERS);
        }
        return name.trim().replaceAll("\\s+", " ");
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

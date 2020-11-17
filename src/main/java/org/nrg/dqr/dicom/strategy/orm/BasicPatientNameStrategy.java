/*
 * BasicPatientNameStrategy
 * DQR is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.nrg.dqr.dicom.strategy.orm;

import org.apache.commons.lang3.StringUtils;
import org.dcm4che2.data.PersonName;
import org.nrg.dqr.domain.PatientName;
import org.nrg.dqr.dto.PacsSearchCriteria;

public class BasicPatientNameStrategy implements PatientNameStrategy {

    private static final String DICOM_PERSON_NAME_COMPONENT_SEPARATOR = "^";
    private static final String DICOM_PERSON_NAME_COMPONENT_SEPARATOR_REGEX_ESCAPED = "\\^";
    private static final String DICOM_WILD_CARD_INDICATOR = "*";
    private static final String DICOM_WILD_CARD_INDICATOR_REGEX_ESCAPED = "\\*";

    @Override
    public PatientName dicomPatientNameToDqrPatientName(String dicomPatientName) {
        return new PatientName(new PersonName(dicomPatientName));
    }

    @Override
    public DicomPersonNameSearchCriteria dqrSearchCriteriaToDicomSearchCriteria(PacsSearchCriteria searchCriteria) {
        DicomPersonNameSearchCriteria dicomPersonNameSearchCriteria = new DicomPersonNameSearchCriteria();
        if (StringUtils.isBlank(searchCriteria.getPatientName())) {
            dicomPersonNameSearchCriteria.addCriterion("");
        } else {
            String patientNameString = myTrim(searchCriteria.getPatientName());
            boolean isQuoted = patientNameString.startsWith("\"") & patientNameString.endsWith(("\""));
            boolean containsCaret = patientNameString.contains("^");
            boolean containsComma = patientNameString.contains(",");
            boolean containsSpace = patientNameString.contains(" ");

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

//            PatientName dqrPatientName = new PatientName(searchCriteria.getPatientName());
//            String firstNameComponent = StringUtils.trimToEmpty(dqrPatientName.getFirstName());
//            String lastNameComponent = StringUtils.trimToEmpty(dqrPatientName.getLastName());
//            if(StringUtils.isNotBlank(lastNameComponent)){
//                if(StringUtils.isNotBlank(firstNameComponent)){
//                    dicomPersonNameSearchCriteria.addCriterion(lastNameComponent+DICOM_PERSON_NAME_COMPONENT_SEPARATOR+firstNameComponent);
//                }
//                else{
//                    dicomPersonNameSearchCriteria.addCriterion(lastNameComponent+DICOM_PERSON_NAME_COMPONENT_SEPARATOR+DICOM_WILD_CARD_INDICATOR);
//                }
//            }
//            else{
//                if(StringUtils.isNotBlank(firstNameComponent)){
//                    dicomPersonNameSearchCriteria.addCriterion(DICOM_WILD_CARD_INDICATOR+DICOM_PERSON_NAME_COMPONENT_SEPARATOR+firstNameComponent);
//                }
//                else{
//                    dicomPersonNameSearchCriteria.addCriterion("");
//                }
//            }


        }
        return dicomPersonNameSearchCriteria;
    }

    private static String removeBoundingQuotes(String name) {
        int length = name.length();
        return name.substring(1, length-2);
    }

    private static String myTrim(String name) {
        String rtn = name.trim();
        rtn = rtn.replaceAll(" [ ]*", " ");

        rtn = rtn.replaceAll("[ ]*\\^[ ]*","^");
        rtn = rtn.replaceAll("[ ]*,[ ]*",",");
        rtn = rtn.replaceAll("[ ]*\\.[ ]*",".");
        rtn = rtn.replaceAll("[ ]*'[ ]*","'");

        return rtn;
    }

    private static String parseWithCaretCommaSpace(String name) {
        return name;
    }

    private static String parseWithCaretComma(String name) {
        return name;
    }

    private static String parseWithCaret(String name) {
        return name;
    }
    private static String parseWithComma(String name) {
        String tokens[] = name.split(",");
        String rtn = tokens[0].trim() + "^" + tokens[1].trim();
        return rtn;
    }
    private static String parseWithSpace(String name) {
        String tokens[] = name.split(" ");
        String rtn = tokens[1] + "^" + tokens[0];
        return rtn;
    }

}

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

import org.apache.commons.lang.StringUtils;
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
            PatientName dqrPatientName = new PatientName(searchCriteria.getPatientName());
            String firstNameComponent = StringUtils.trimToEmpty(dqrPatientName.getFirstName());
            String lastNameComponent = StringUtils.trimToEmpty(dqrPatientName.getLastName());
            if(StringUtils.isNotBlank(lastNameComponent)){
                if(StringUtils.isNotBlank(firstNameComponent)){
                    dicomPersonNameSearchCriteria.addCriterion(lastNameComponent+DICOM_PERSON_NAME_COMPONENT_SEPARATOR+firstNameComponent);
                }
                else{
                    dicomPersonNameSearchCriteria.addCriterion(lastNameComponent+DICOM_PERSON_NAME_COMPONENT_SEPARATOR+DICOM_WILD_CARD_INDICATOR);
                }
            }
            else{
                if(StringUtils.isNotBlank(firstNameComponent)){
                    dicomPersonNameSearchCriteria.addCriterion(DICOM_WILD_CARD_INDICATOR+DICOM_PERSON_NAME_COMPONENT_SEPARATOR+firstNameComponent);
                }
                else{
                    dicomPersonNameSearchCriteria.addCriterion("");
                }
            }
//            final Object[] nameComponents = new Object[]{
//                    lastNameComponent(searchCriteria),
//                    firstNameComponent(searchCriteria),
//                    componentDefaultIfEmpty(null),
//                    componentDefaultIfEmpty(null),
//                    componentDefaultIfEmpty(null),
//            };
//
//            String dicomPatientNameWildCardMatch = StringUtils.join(nameComponents,
//                    DICOM_PERSON_NAME_COMPONENT_SEPARATOR);
//
//            // this implementation doesn't like wildcard matching at the end of the first name (e.g. "Cartman^Eric*"),
//            // if the name you're passing is already an exact match.
//            // So, have the search try the exact match first (w/o wildcards), then try w/ wildcards if no results are
//            // found
//            if (searchCriteria.isFirstNamePresent()) {
//                String dicomPatientNameExactMatch = dicomPatientNameWildCardMatch
//                        .replaceAll(DICOM_PERSON_NAME_COMPONENT_SEPARATOR_REGEX_ESCAPED + "+",
//                                DICOM_PERSON_NAME_COMPONENT_SEPARATOR_REGEX_ESCAPED)
//                        .replaceFirst(DICOM_PERSON_NAME_COMPONENT_SEPARATOR_REGEX_ESCAPED + "$", "")
//                        .replaceFirst(DICOM_WILD_CARD_INDICATOR_REGEX_ESCAPED + "$", "");
//
//                dicomPersonNameSearchCriteria.addCriterion(dicomPatientNameExactMatch);
//            }
//            dicomPersonNameSearchCriteria.addCriterion(dicomPatientNameWildCardMatch);
        }
        return dicomPersonNameSearchCriteria;
    }

//    private String componentDefaultIfEmpty(String nameComponent) {
//        return StringUtils.trimToEmpty(nameComponent);
//    }
//
//    private String lastNameComponent(PacsSearchCriteria searchCriteria) {
//        PatientName dqrPatientName = new PatientName(searchCriteria.getPatientName());
//        String lastNameComponent = StringUtils.trimToEmpty(dqrPatientName.getLastName());
//        if (!StringUtils.isBlank(lastNameComponent)) {
//            lastNameComponent += (searchCriteria.isLastNamePartial() ? DICOM_WILD_CARD_INDICATOR : "");
//        }
//        return lastNameComponent;
//    }
//
//    private String firstNameComponent(PacsSearchCriteria searchCriteria) {
//        PatientName dqrPatientName = new PatientName(searchCriteria.getPatientName());
//        String firstNameComponent = StringUtils.trimToEmpty(dqrPatientName.getFirstName());
//        if (!StringUtils.isBlank(firstNameComponent)) {
//            firstNameComponent += (searchCriteria.isFirstNamePartial() ? DICOM_WILD_CARD_INDICATOR : "");
//        }
//        return firstNameComponent;
//    }

}

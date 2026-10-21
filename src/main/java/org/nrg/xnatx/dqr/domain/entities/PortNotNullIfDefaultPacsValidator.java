/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.domain.entities.PortNotNullIfDefaultPacsValidator
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.domain.entities;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PortNotNullIfDefaultPacsValidator implements ConstraintValidator<PortNotNullIfDefaultPacs, Pacs> {

    @Override
    public void initialize(final PortNotNullIfDefaultPacs constraintAnnotation) {
    }

    @Override
    public boolean isValid(final Pacs value, final ConstraintValidatorContext context) {
        if (!value.isDicomWebEnabled() && value.isDefaultStoragePacs() && value.getQueryRetrievePort() == null) {
            return false;
        }
        if (!value.isDicomWebEnabled() && value.isDefaultQueryRetrievePacs() && value.getQueryRetrievePort() == null) {
            return false;
        }
        return true;
    }

}

/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.domain.entities.AtLeastOnePortNotNullValidator
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.domain.entities;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class AtLeastOnePortNotNullValidator implements ConstraintValidator<AtLeastOnePortNotNull, Pacs> {

    @Override
    public void initialize(final AtLeastOnePortNotNull constraintAnnotation) {
    }

    @Override
    public boolean isValid(final Pacs value, final ConstraintValidatorContext context) {
        return value.getQueryRetrievePort() != null;
    }

}

/*
 * AtLeastOnePortNotNullValidator
 * DQR is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.nrg.dqr.domain.entities;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class AtLeastOnePortNotNullValidator implements ConstraintValidator<AtLeastOnePortNotNull, Pacs> {

    @Override
    public void initialize(final AtLeastOnePortNotNull constraintAnnotation) {
    }

    @Override
    public boolean isValid(final Pacs value, final ConstraintValidatorContext context) {
        return value.getQueryRetrievePort() != null;
    }

}

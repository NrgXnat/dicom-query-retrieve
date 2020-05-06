/*
 * PortNotNullIfDefaultPacsValidator
 * DQR is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.nrg.xnatx.dqr.domain.entities;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class PortNotNullIfDefaultPacsValidator implements ConstraintValidator<PortNotNullIfDefaultPacs, Pacs> {

    @Override
    public void initialize(final PortNotNullIfDefaultPacs constraintAnnotation) {
    }

    @Override
    public boolean isValid(final Pacs value, final ConstraintValidatorContext context) {
        if (value.isDefaultStoragePacs() && value.getQueryRetrievePort() == null) {
            return false;
        }
        if (value.isDefaultQueryRetrievePacs() && value.getQueryRetrievePort() == null) {
            return false;
        }
        return true;
    }

}

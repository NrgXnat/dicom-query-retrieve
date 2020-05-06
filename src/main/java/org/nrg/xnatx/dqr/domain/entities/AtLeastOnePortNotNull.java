/*
 * AtLeastOnePortNotNull
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

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

@Target({
        TYPE
})
@Retention(RUNTIME)
@Constraint(validatedBy = AtLeastOnePortNotNullValidator.class)
@Documented
public @interface AtLeastOnePortNotNull {

    String message() default "Either the Q/R port or the storage port must be populated.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

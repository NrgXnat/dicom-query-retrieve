/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.domain.entities.AtLeastOnePortNotNull
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.domain.entities;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

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

/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.domain.entities.PortNotNullIfDefaultPacs
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
import javax.validation.Constraint;
import javax.validation.Payload;

@Target({
            TYPE
        })
@Retention(RUNTIME)
@Constraint(validatedBy = PortNotNullIfDefaultPacsValidator.class)
@Documented
public @interface PortNotNullIfDefaultPacs {

    String message() default "If a PACS is the default Q/R pacs, the Q/R port must not be null.  Likewise for the default storage pacs/port.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

/*
 * org.nrg.xnatx.dqr.exceptions.PacsNotFoundException
 * DQR is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.nrg.xnatx.dqr.exceptions;

import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(prefix = "_")
public class PacsNotFoundException extends Exception {
    public PacsNotFoundException(final long id) {
        this(id, "The specified PACS system \"" + id + "\" was not found");
    }

    private PacsNotFoundException(final long id, final String message) {
        super(message);
        _id = id;
    }

    private final long _id;
}

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
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Getter
@Accessors(prefix = "_")
@ResponseStatus(value = HttpStatus.CONFLICT, reason = "The specified PACS system does not permit store operations")
public class PacsNotStorableException extends Exception {
    public PacsNotStorableException(final long id) {
        this(id, "The specified PACS system \"" + id + "\" does not permit query operations");
    }

    private PacsNotStorableException(final long id, final String message) {
        super(message);
        _id = id;
        _aeTitleAndPort = null;
    }

    public PacsNotStorableException(final String aeTitleAndPort) {
        this(aeTitleAndPort, "The specified PACS system \"" + aeTitleAndPort + "\" does not permit query operations");
    }

    private PacsNotStorableException(final String aeTitleAndPort, final String message) {
        super(message);
        _id = 0;
        _aeTitleAndPort = aeTitleAndPort;
    }

    private final long   _id;
    private final String _aeTitleAndPort;
}

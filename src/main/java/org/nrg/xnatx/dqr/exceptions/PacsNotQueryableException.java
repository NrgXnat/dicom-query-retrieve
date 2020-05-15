/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.exceptions.PacsNotQueryableException
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.exceptions;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Getter
@Accessors(prefix = "_")
@ResponseStatus(value = HttpStatus.CONFLICT, reason = "The specified PACS system does not permit query operations")
public class PacsNotQueryableException extends Exception {
    public PacsNotQueryableException(final long id) {
        this(id, "The specified PACS system \"" + id + "\" does not permit query operations");
    }

    private PacsNotQueryableException(final long id, final String message) {
        super(message);
        _id = id;
    }

    private final long _id;
}

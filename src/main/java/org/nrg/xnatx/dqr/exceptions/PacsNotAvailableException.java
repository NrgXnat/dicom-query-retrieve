/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.exceptions.PacsNotAvailableException
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.exceptions;

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.Accessors;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Value
@Accessors(prefix = "_")
@EqualsAndHashCode(callSuper = false)
@ResponseStatus(value = HttpStatus.SERVICE_UNAVAILABLE, reason = "The specified PACS system is not currently available")
public class PacsNotAvailableException extends PacsException {
    private static final long   serialVersionUID = 8411366497789328237L;
    private static final String MESSAGE_FORMAT   = "The specified PACS system \"%d\" is not currently available";

    public PacsNotAvailableException(final long pacsId) {
        super(pacsId, MESSAGE_FORMAT);
    }
}

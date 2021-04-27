/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.exceptions.PacsNotQueryableException
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
@ResponseStatus(value = HttpStatus.CONFLICT, reason = "The specified PACS system does not permit query operations")
public class PacsNotQueryableException extends Exception {
    long _pacsId;

    public PacsNotQueryableException(final long pacsId) {
        super("The specified PACS system \"" + pacsId + "\" does not permit query operations");
        _pacsId = pacsId;
    }
}

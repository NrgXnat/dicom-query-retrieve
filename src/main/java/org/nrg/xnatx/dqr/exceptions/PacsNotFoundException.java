/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.exceptions.PacsNotFoundException
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
@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "The specified PACS system was not found")
public class PacsNotFoundException extends Exception {
    long _pacsId;

    public PacsNotFoundException(final long pacsId) {
        super("The specified PACS system \"" + pacsId + "\" was not found");
        _pacsId = pacsId;
    }
}

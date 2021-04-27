/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.exceptions.PacsNotStorableException
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
@ResponseStatus(value = HttpStatus.CONFLICT, reason = "The specified PACS system does not permit store operations")
public class PacsNotStorableException extends Exception {
    long   _pacsId;
    String _aeTitleAndPort;

    public PacsNotStorableException(final long pacsId) {
        this("The specified PACS system \"" + pacsId + "\" does not permit query operations", pacsId, null);
    }

    public PacsNotStorableException(final String aeTitleAndPort) {
        this("The specified PACS system \"" + aeTitleAndPort + "\" does not permit query operations", 0, aeTitleAndPort);
    }

    private PacsNotStorableException(final String message, final long pacsId, final String aeTitleAndPort) {
        super(message);
        _pacsId = pacsId;
        _aeTitleAndPort = aeTitleAndPort;
    }
}

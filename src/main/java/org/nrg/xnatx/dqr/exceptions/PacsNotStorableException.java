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
import org.nrg.xnatx.dqr.utils.AeTitle;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Value
@Accessors(prefix = "_")
@EqualsAndHashCode(callSuper = false)
@ResponseStatus(value = HttpStatus.CONFLICT, reason = "The specified PACS system does not permit store operations")
public class PacsNotStorableException extends PacsException {
    private static final long   serialVersionUID          = 7277238822312819923L;
    private static final String MESSAGE_PACS_ID           = "The specified PACS system \"%d\" does not permit query operations";
    private static final String MESSAGE_AE_TITLE_AND_PORT = "The specified PACS system \"%s:%d\" does not permit query operations";

    public PacsNotStorableException(final long pacsId) {
        super(pacsId, MESSAGE_PACS_ID);
    }

    public PacsNotStorableException(final AeTitle aeTitle) {
        super(aeTitle, MESSAGE_AE_TITLE_AND_PORT);
    }
}

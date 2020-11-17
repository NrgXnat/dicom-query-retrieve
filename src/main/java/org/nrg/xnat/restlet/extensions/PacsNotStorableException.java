/*
 * org.nrg.xnat.restlet.extensions.PacsNotFoundException
 * DQR is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.nrg.xnat.restlet.extensions;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
@Getter
public class PacsNotStorableException extends Exception {
    public PacsNotStorableException(final long pacsId) {
        super("PACS " + pacsId + " does not allow C-STORE operations");
        _pacsId = pacsId;
    }

    private final long _pacsId;
}

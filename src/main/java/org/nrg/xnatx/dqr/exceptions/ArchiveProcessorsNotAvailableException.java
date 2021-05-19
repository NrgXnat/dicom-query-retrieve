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
import org.nrg.dcm.scp.DicomSCPInstance;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Value
@Accessors(prefix = "_")
@EqualsAndHashCode(callSuper = false)
@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "The specified PACS system is not currently available")
public class ArchiveProcessorsNotAvailableException extends Exception {
    private static final long serialVersionUID = 2291079889933081769L;

    int _id;
    String _aeTitle;
    int    _port;

    public ArchiveProcessorsNotAvailableException(final DicomSCPInstance instance) {
        super("The specified DICOM receiver (" + instance.getId() + ") at " + instance.getAeTitle() + ":" + instance.getPort() + " does not have custom processing enabled");
        _id = instance.getId();
        _aeTitle = instance.getAeTitle();
        _port = instance.getPort();
    }
}

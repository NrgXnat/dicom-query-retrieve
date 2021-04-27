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
import org.nrg.dcm.scp.DicomSCPInstance;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Value
@Accessors(prefix = "_")
@EqualsAndHashCode(callSuper = false)
@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "No remapping processors available for this SCP receiver, cannot remap DICOM fields")
public class DicomReceiverCustomProcessingDisabledException extends Exception {
    int    _id;
    String _aeTitle;
    int    _port;

    public DicomReceiverCustomProcessingDisabledException(final DicomSCPInstance instance) {
        super("No remapping processors available for the DICOM receiver (" + instance.getId() + ") at " + instance.getAeTitle() + ":" + instance.getPort() + ", cannot remap DICOM fields");
        _id = instance.getId();
        _aeTitle = instance.getAeTitle();
        _port = instance.getPort();
    }
}

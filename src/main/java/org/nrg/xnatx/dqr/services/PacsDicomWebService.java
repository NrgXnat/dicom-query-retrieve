package org.nrg.xnatx.dqr.services;

import org.nrg.xnatx.dqr.dto.DicomWebPingRequest;
import org.nrg.xnatx.dqr.dto.DicomWebPingResult;
import org.nrg.xnatx.dqr.exceptions.InvalidDicomWebPingRequestException;

public interface PacsDicomWebService {
    /**
     * Ping a dicom-web endpoint
     *
     * @param pingRequest - The ping request containing the aeTitle and the dicom-web root url
     * @return DicomWebPingResult
     */
    DicomWebPingResult ping(final DicomWebPingRequest pingRequest) throws InvalidDicomWebPingRequestException;
}

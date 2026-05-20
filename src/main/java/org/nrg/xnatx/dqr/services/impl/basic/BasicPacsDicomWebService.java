package org.nrg.xnatx.dqr.services.impl.basic;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.nrg.xnatx.dqr.dicom.http.DicomWebHttpClient;
import org.nrg.xnatx.dqr.dto.DicomWebCredential;
import org.nrg.xnatx.dqr.dto.DicomWebPingRequest;
import org.nrg.xnatx.dqr.dto.DicomWebPingResult;
import org.nrg.xnatx.dqr.exceptions.InvalidDicomWebPingRequestException;
import org.nrg.xnatx.dqr.preferences.DqrPreferences;
import org.nrg.xnatx.dqr.services.DicomWebCredentialService;
import org.nrg.xnatx.dqr.services.PacsDicomWebService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

@Service
@Slf4j
public class BasicPacsDicomWebService implements PacsDicomWebService {
    private final DicomWebCredentialService dicomWebCredentialService;
    private final DqrPreferences dqrPreferences;

    @Autowired
    public BasicPacsDicomWebService(final DicomWebCredentialService dicomWebCredentialService,
                                    final DqrPreferences dqrPreferences) {
        this.dicomWebCredentialService = dicomWebCredentialService;
        this.dqrPreferences = dqrPreferences;
    }

    @Override
    public DicomWebPingResult ping(final DicomWebPingRequest pingRequest) throws InvalidDicomWebPingRequestException {
        try {
            new URL(pingRequest.getRootUrl());
        } catch (MalformedURLException e) {
            throw new InvalidDicomWebPingRequestException("Invalid url");
        }

        if (StringUtils.isBlank(pingRequest.getAeTitle())) {
            throw new InvalidDicomWebPingRequestException("Invalid AE Title");
        }

        dicomWebCredentialService.load();

        final Optional<DicomWebCredential> credentialOptional = dicomWebCredentialService.getCredential(pingRequest.getAeTitle());
        try (final DicomWebHttpClient httpClient
                     = new DicomWebHttpClient(pingRequest.getRootUrl(), credentialOptional.orElse(null), dqrPreferences)) {
            return httpClient.ping();
        }
    }
}

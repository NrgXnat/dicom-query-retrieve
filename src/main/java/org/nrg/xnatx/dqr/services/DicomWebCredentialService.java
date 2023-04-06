package org.nrg.xnatx.dqr.services;

import org.nrg.xnatx.dqr.dto.DicomWebCredential;

import java.util.Optional;

public interface DicomWebCredentialService {
    Optional<DicomWebCredential> getCredential(String aeTitle);

    void load();
}

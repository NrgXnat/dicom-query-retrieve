package org.nrg.xnatx.dqr.services.impl.basic;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.nrg.xnatx.dqr.dto.DicomWebCredential;
import org.nrg.xnatx.dqr.services.DicomWebCredentialService;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BasicDicomWebCredentialService implements DicomWebCredentialService {
    private static final Path DICOMWEB_AUTH_FILE = Paths.get("config", "auth", "dicomweb-auth.json");
    private final ObjectMapper objectMapper;
    private final Map<String, DicomWebCredential> dicomWebCredentials;
    private final Path authFilePath;

    public BasicDicomWebCredentialService(final ObjectMapper objectMapper, final Path xnatHome) {
        this.objectMapper = objectMapper;
        this.authFilePath = xnatHome.resolve(DICOMWEB_AUTH_FILE);
        this.dicomWebCredentials = new ConcurrentHashMap<>();
        this.load();
    }

    @Override
    public Optional<DicomWebCredential> getCredential(final String aeTitle) {
        return Optional.ofNullable(dicomWebCredentials.get(aeTitle));
    }

    @Override
    public void load() {
        final Map<String, DicomWebCredential> credentials = loadCredentialsFromFile();
        synchronized (dicomWebCredentials) {
            log.debug("Clearing existing DICOMweb credentials");
            dicomWebCredentials.clear();
            dicomWebCredentials.putAll(credentials);
        }
    }

    private Map<String, DicomWebCredential> loadCredentialsFromFile() {
        log.debug("Loading DICOMweb credentials from file {}", this.authFilePath);
        File jsonFile = this.authFilePath.toFile();
        if (jsonFile.exists()) {
            try {
                return objectMapper.readValue(jsonFile, new TypeReference<List<DicomWebCredential>>() {}).stream()
                                .filter(this::aeTitleIsNotEmpty)
                                .collect(Collectors.toMap(DicomWebCredential::getAeTitle, Function.identity(), (a, b) -> a));
            } catch (IOException e) {
                log.error("Could not read DICOMweb credentials from file " + this.authFilePath, e);
            }
        } else {
            log.info("DICOMweb credentials file {} does not exist", this.authFilePath);
        }
        return Collections.emptyMap();
    }

    private boolean aeTitleIsNotEmpty(final DicomWebCredential dicomWebCredential) {
        String aeTitle = dicomWebCredential.getAeTitle();
        if (StringUtils.isBlank(aeTitle)) {
            log.warn("Blank aeTitle in DICOMweb credentials file {}", this.authFilePath);
            return false;
        }
        return true;
    }
}

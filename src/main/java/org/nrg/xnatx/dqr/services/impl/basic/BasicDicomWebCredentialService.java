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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
public class BasicDicomWebCredentialService implements DicomWebCredentialService {
    private static final Path DICOMWEB_AUTH_FILE = Paths.get("config", "auth", "dicomweb-auth.json");
    private final ObjectMapper objectMapper;
    private Map<String, DicomWebCredential> dicomWebCredentials = new HashMap<>();
    private final Path authFilePath;

    public BasicDicomWebCredentialService(final ObjectMapper objectMapper, final Path xnatHome) {
        this.objectMapper = objectMapper;
        this.authFilePath = xnatHome.resolve(DICOMWEB_AUTH_FILE);
        this.load();
    }

    @Override
    public Optional<DicomWebCredential> getCredential(final String aeTitle) {
        return Optional.ofNullable(dicomWebCredentials.get(aeTitle));
    }

    @Override
    public void load() {
        dicomWebCredentials = new HashMap<>();
        log.info("Load credentials from the local file {}", this.authFilePath);
        File jsonFile = this.authFilePath.toFile();
        if (jsonFile.exists()) {
            try {
                List<DicomWebCredential> dwcs = objectMapper.readValue(jsonFile, new TypeReference<List<DicomWebCredential>>() {
                });
                dwcs.forEach(this::addCredential);
            } catch (IOException e) {
                log.error("Read the local authentication file error ", e);
            }
        } else {
            log.info("File {} does not exist", this.authFilePath);
        }
    }

    private void addCredential(final DicomWebCredential dicomWebCredential) {
        String aeTitle = dicomWebCredential.getAeTitle();
        if (Objects.isNull(aeTitle) || StringUtils.isEmpty(aeTitle)) {
            log.error("aeTitle can not be null or empty");
            return;
        }
        if (this.dicomWebCredentials.containsKey(aeTitle)) {
            log.warn("The aeTitle is not unique, Only the first record will be saved");
            return;
        }
        this.dicomWebCredentials.put(aeTitle, dicomWebCredential);
    }
}

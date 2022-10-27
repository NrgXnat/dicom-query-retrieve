package org.nrg.xnatx.dqr.services.impl.basic;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.NotNull;
import org.nrg.xnat.eventservice.exceptions.UnauthorizedException;
import org.nrg.xnatx.dqr.domain.entities.Pacs;
import org.nrg.xnatx.dqr.dto.DicomWebCredential;
import org.nrg.xnatx.dqr.services.DicomWebCredentialService;
import org.nrg.xnatx.dqr.services.PacsDicomWebService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BasicPacsDicomWebService implements PacsDicomWebService {
    private static final DateFormat YMD = new SimpleDateFormat("yyyyMMdd");
    private static final String STUDIES = "studies?StudyDate=";
    private static final int AUTHENTICATION_MAX_ATTEMPTS = 2;
    private static final PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
    private final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
    private final DicomWebCredentialService dicomWebCredentialService;

    @Autowired
    public BasicPacsDicomWebService(final DicomWebCredentialService dicomWebCredentialService) {
        this.dicomWebCredentialService = dicomWebCredentialService;
    }

    private CloseableHttpClient getHttpClient() {
        return HttpClients
                .custom()
                .setDefaultCredentialsProvider(credentialsProvider)
                .setConnectionManager(connectionManager)
                .build();
    }

    @Override
    @Nullable
    public String getContent(final String url) throws UnauthorizedException {
        CloseableHttpResponse response = null;
        try {
            CloseableHttpClient httpClient = getHttpClient();
            response = httpClient.execute(new HttpGet(url));
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                try (
                        InputStreamReader is = new InputStreamReader(response.getEntity().getContent());
                        BufferedReader bf = new BufferedReader(is);
                ) {
                    return bf.lines().collect(Collectors.joining(System.lineSeparator()));
                }
            } else if (statusCode == 401) {
                throw new UnauthorizedException("Http return status code is 401, Authorization is failed.");
            } else {
                log.warn("The Http response code is {}", statusCode);
            }
        } catch (IOException | UnsupportedOperationException e) {
            log.error("Failed to get content from the remote pacs. ", e);
        } finally {
            if (null != response) {
                try {
                    EntityUtils.consume(response.getEntity());
                } catch (IOException e) {
                    log.error(" EntityUtils consume error. ", e);
                }
            }
        }
        return null;
    }

    @Override
    @NotNull
    public String buildStudyUrlByDay(final Date day, final String rootUrl) {
        return StringUtils.appendIfMissing(rootUrl, "/") + STUDIES + YMD.format(day);
    }

    @Override
    public void addCredentialForPacs(final Pacs pacs) {
        addCredentialForPacs(pacs.getAeTitle(), pacs.getDicomWebRootUrl());
    }

    @Override
    public void addCredentialForPacs(final String aeTitle, final String rootUrl) {
        Optional<DicomWebCredential> credentialOptional = this.dicomWebCredentialService.getCredential(aeTitle);
        if (credentialOptional.isPresent()) {
            DicomWebCredential dicomWebCredential = credentialOptional.get();
            try {
                URL url = new URL(rootUrl);
                AuthScope scope = new AuthScope(url.getHost(), url.getPort());
                Credentials credentials = new UsernamePasswordCredentials(dicomWebCredential.getUsername(), dicomWebCredential.getPassword());
                credentialsProvider.setCredentials(scope, credentials);
            } catch (MalformedURLException e) {
                log.error("Failed to add DicomWeb credential for Pacs ", e);
            }
        } else {
            log.info("There is no credentials for Pacs {}", aeTitle);
        }
    }

    @Override
    @Nullable
    public String dicomWebPing(final String aeTitle, final String rootUrl) throws UnauthorizedException {
        addCredentialForPacs(aeTitle, rootUrl);
        for (int attempt = 0; attempt < AUTHENTICATION_MAX_ATTEMPTS; attempt++) {
            try {
                return this.getContent(buildStudyUrlByDay(new Date(), rootUrl));
            } catch (UnauthorizedException e) {
                if (attempt >= AUTHENTICATION_MAX_ATTEMPTS - 1) {
                    log.error("The return code is 401. Authorization Error. ", e);
                    throw new UnauthorizedException("DicomWeb finding metadata error", e);
                }
                log.error("The return code is 401. Authorization Error. Will reload the credentials from the local file and give another try");
                this.dicomWebCredentialService.load();
                this.addCredentialForPacs(aeTitle, rootUrl);
            }
        }
        return null;
    }
}

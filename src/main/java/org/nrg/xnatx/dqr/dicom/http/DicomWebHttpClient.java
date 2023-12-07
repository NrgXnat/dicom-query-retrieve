package org.nrg.xnatx.dqr.dicom.http;

import org.nrg.xnatx.dqr.dicom.json.DicomCorrectingJsonParser;
import org.nrg.xnatx.dqr.exceptions.PacsConnectionException;
import org.nrg.xnatx.dqr.utils.RetryablePacsOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.json.JSONReader;
import org.nrg.framework.exceptions.NrgServiceRuntimeException;
import org.nrg.xnatx.dqr.domain.entities.Pacs;
import org.nrg.xnatx.dqr.dto.DicomWebCredential;
import org.nrg.xnatx.dqr.services.DicomWebCredentialService;

import javax.annotation.Nullable;
import javax.json.Json;
import javax.json.stream.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

@Slf4j
public class DicomWebHttpClient implements AutoCloseable {
    private static final String APPLICATION_DICOM_JSON = "application/dicom+json";
    private final CloseableHttpClient httpClient;
    private final HttpClientContext context;
    private final Pacs pacs;

    public interface Callback {
        void onDataset(final Attributes attributes);
    }

    public DicomWebHttpClient(final Pacs pacs, final DicomWebCredentialService dicomWebCredentialService) {
        this.pacs  = pacs;
        httpClient = HttpClientBuilder.create().build();
        context    = HttpClientContext.create();

        final URL url;
        try {
            url = new URL(pacs.getDicomWebRootUrl());
        } catch (MalformedURLException e) {
            throw new NrgServiceRuntimeException("Malformed dicom-web url for pacs: " + pacs.getId(), e);
        }

        final HttpHost httpHost                                       = new HttpHost(url.getHost(), url.getPort());
        final CredentialsProvider credentialsProvider                 = new BasicCredentialsProvider();
        final AuthCache authCache                                     = new BasicAuthCache();
        final AuthScope authScope                                     = new AuthScope(httpHost);
        final Optional<DicomWebCredential> dicomWebCredentialOptional = dicomWebCredentialService.getCredential(pacs.getAeTitle());

        if (dicomWebCredentialOptional.isPresent()) {
            final DicomWebCredential dicomWebCredential = dicomWebCredentialOptional.get();
            final Credentials credentials
                    = new UsernamePasswordCredentials(dicomWebCredential.getUsername(), dicomWebCredential.getPassword());
            log.debug("Using dicom-web authentication credentials for user: {}", dicomWebCredential.getUsername());
            credentialsProvider.setCredentials(authScope, credentials);
            if (dicomWebCredential.isPreemptiveAuth()) {
                log.debug("Using preemptive authentication for dicom-web endpoint: {}:{}", httpHost.getHostName(), httpHost.getPort());
                authCache.put(httpHost, new BasicScheme());
            }
        }

        context.setCredentialsProvider(credentialsProvider);
        context.setAuthCache(authCache);
    }

    public void getAttributes(final String url, final DicomWebHttpClient.Callback callback) throws PacsConnectionException {
        new RetryablePacsOperation<Void>(pacs) {
            @Override
            @Nullable
            public Void doOperationWithRetry() throws PacsConnectionException {
                try {
                    doGet(url, callback);
                } catch (IOException e) {
                    throw new PacsConnectionException("Failed to connect to remote pacs:"
                            + pacs.getAeTitle() + ":" + pacs.getHost());
                }
                return null;
            }
        }.call();
    }

    public Optional<Attributes> getAttributes(final String url) throws PacsConnectionException {
        return new RetryablePacsOperation<Optional<Attributes>>(pacs) {
            @Override
            public Optional<Attributes> doOperationWithRetry() throws PacsConnectionException {
                try {
                    return doGet(url);
                } catch (IOException e) {
                    throw new PacsConnectionException("Failed to connect to remote pacs:"
                            + pacs.getAeTitle() + ":" + pacs.getHost());
                }
            }
        }.call();
    }

    private Optional<Attributes> doGet(final String url)
            throws IOException {
        return doGet(url, null);
    }

    private Optional<Attributes> doGet(final String url, @Nullable final DicomWebHttpClient.Callback callback)
            throws IOException {
        final HttpUriRequest request = new HttpGet(url);
        request.setHeader(HttpHeaders.ACCEPT, APPLICATION_DICOM_JSON);
        try (final CloseableHttpResponse response = httpClient.execute(request, context)) {

            final int httpStatus = response.getStatusLine().getStatusCode();
            if (401 == httpStatus) {
                throw new NrgServiceRuntimeException("Failed to authenticate with Pacs "
                        + pacs.getAeTitle() + ":" + pacs.getHost() + ". Check dicom-web credentials");
            } else if (200 != httpStatus) {
                log.error("Response from server was {}. Not parsing results", httpStatus);
                return Optional.empty();
            }

            try (final InputStream inputStream = response.getEntity().getContent();
                 final JsonParser jsonParser = Json.createParser(inputStream);
                 final JsonParser filteredParser = new DicomCorrectingJsonParser(jsonParser)
            ) {
                final JSONReader jsonReader = new JSONReader(filteredParser);
                jsonReader.setSkipBulkDataURI(true);
                if (callback == null) {
                    return Optional.of(jsonReader.readDataset(null));
                }

                jsonReader.readDatasets((fmi, attributes) -> {
                    if (null != fmi) {
                        attributes.addAll(fmi);
                    }
                    callback.onDataset(attributes);
                });
                return Optional.empty();
            } catch (Throwable t) {
                log.error("dicom-web import {} failed", url, t);
                return Optional.empty();
            }
        }
    }

    @Override
    public void close() {
        try {
            httpClient.close();
        } catch (IOException e) {
            log.error("Failed to close {}", getClass().getSimpleName(), e);
        }
    }
}

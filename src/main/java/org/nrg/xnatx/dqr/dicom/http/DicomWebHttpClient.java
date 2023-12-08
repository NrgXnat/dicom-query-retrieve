package org.nrg.xnatx.dqr.dicom.http;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
import org.nrg.xnatx.dqr.dicom.json.DicomCorrectingJsonParser;
import org.nrg.xnatx.dqr.domain.entities.Pacs;
import org.nrg.xnatx.dqr.dto.DicomWebCredential;
import org.nrg.xnatx.dqr.dto.DicomWebPingResult;
import org.nrg.xnatx.dqr.exceptions.PacsConnectionException;
import org.nrg.xnatx.dqr.utils.RetryablePacsOperation;
import org.springframework.http.HttpStatus;

import javax.annotation.Nullable;
import javax.json.Json;
import javax.json.stream.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

@Slf4j
public class DicomWebHttpClient implements AutoCloseable {
    private static final DateFormat STUDY_DATE_FORMATTER = new SimpleDateFormat("yyyyMMdd");
    private static final String STUDIES = "studies?StudyDate=";

    private static final String APPLICATION_DICOM_JSON = "application/dicom+json";
    private final CloseableHttpClient httpClient;
    private final HttpClientContext context;
    private final String rootUrl;

    public interface Callback {
        void onDataset(final Attributes attributes);
    }

    public DicomWebHttpClient(final Pacs pacs, @Nullable final DicomWebCredential credentials) {
        this(pacs.getDicomWebRootUrl(), credentials);
    }

    public DicomWebHttpClient(final String rootUrl, @Nullable final DicomWebCredential credentials) {
        this.rootUrl = StringUtils.appendIfMissing(rootUrl, "/");

        httpClient = HttpClientBuilder.create().build();
        context = HttpClientContext.create();

        final URL url;
        try {
            url = new URL(rootUrl);
        } catch (MalformedURLException e) {
            throw new NrgServiceRuntimeException("Malformed dicom-web url: " + rootUrl, e);
        }

        final HttpHost httpHost = new HttpHost(url.getHost(), url.getPort());

        if (null != credentials) {
            final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            final AuthCache authCache = new BasicAuthCache();
            final AuthScope authScope = new AuthScope(httpHost);
            final Credentials basicCredentials
                    = new UsernamePasswordCredentials(credentials.getUsername(), credentials.getPassword());

            log.debug("Using dicom-web authentication credentials for user: {}", credentials.getUsername());
            credentialsProvider.setCredentials(authScope, basicCredentials);
            if (credentials.isPreemptiveAuth()) {
                log.debug("Using preemptive authentication for dicom-web endpoint: {}:{}",
                        httpHost.getHostName(), httpHost.getPort());
                authCache.put(httpHost, new BasicScheme());
            }

            context.setCredentialsProvider(credentialsProvider);
            context.setAuthCache(authCache);
        }
    }


    public void getAttributes(final String url, final DicomWebHttpClient.Callback callback) throws PacsConnectionException {
        new RetryablePacsOperation<Void>(rootUrl) {
            @Override
            @Nullable
            public Void doOperationWithRetry() throws PacsConnectionException {
                try {
                    doGet(url, callback);
                } catch (IOException e) {
                    throw new PacsConnectionException("Failed to connect to dicom-web endpoint: " + rootUrl, e);
                }
                return null;
            }
        }.call();
    }

    public Optional<Attributes> getAttributes(final String url) throws PacsConnectionException {
        return new RetryablePacsOperation<Optional<Attributes>>(rootUrl) {
            @Override
            public Optional<Attributes> doOperationWithRetry() throws PacsConnectionException {
                try {
                    return doGet(url);
                } catch (IOException e) {
                    throw new PacsConnectionException("Failed to connect to dicom-web endpoint: " + rootUrl, e);
                }
            }
        }.call();
    }

    public DicomWebPingResult ping() {
        final String requestUrl = rootUrl + STUDIES + STUDY_DATE_FORMATTER.format(new Date());
        final HttpUriRequest request = new HttpGet(requestUrl);
        request.setHeader(HttpHeaders.ACCEPT, APPLICATION_DICOM_JSON);

        try (final CloseableHttpResponse response = httpClient.execute(request, context)) {
            final HttpStatus status = HttpStatus.valueOf(response.getStatusLine().getStatusCode());
            return DicomWebPingResult.builder()
                    .successful(status.is2xxSuccessful())
                    .httpStatus(status.value())
                    .reason(status.getReasonPhrase())
                    .build();
        } catch (IOException e) {
            log.error("Failed to connect to dicom-web endpoint: {}", requestUrl, e);
            return DicomWebPingResult.builder()
                    .successful(false)
                    .reason("Unable to establish connection")
                    .build();
        }
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
                throw new NrgServiceRuntimeException("Failed to authenticate with dicom-web endpoint " + rootUrl);
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
                log.error("Failed to parse dicom-web response from url: {}", url, t);
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

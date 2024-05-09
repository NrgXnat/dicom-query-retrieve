package org.nrg.xnatx.dqr.dicom.http;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.json.JSONReader;
import org.dcm4che3.mime.MultipartInputStream;
import org.dcm4che3.mime.MultipartParser;
import org.nrg.framework.exceptions.NrgServiceRuntimeException;
import org.nrg.xnatx.dqr.dicom.json.DicomCorrectingJsonParser;
import org.nrg.xnatx.dqr.dto.DicomWebCredential;
import org.nrg.xnatx.dqr.dto.DicomWebPingResult;
import org.nrg.xnatx.dqr.exceptions.PacsAuthenticationException;
import org.nrg.xnatx.dqr.exceptions.PacsDataNotFoundException;
import org.nrg.xnatx.dqr.exceptions.DqrException;
import org.nrg.xnatx.dqr.exceptions.DqrRuntimeException;
import org.nrg.xnatx.dqr.exceptions.PacsConnectionException;
import org.nrg.xnatx.dqr.exceptions.PacsException;
import org.nrg.xnatx.dqr.utils.RetryablePacsOperation;
import org.springframework.http.HttpStatus;

import javax.annotation.Nullable;
import javax.json.Json;
import javax.json.stream.JsonParser;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class DicomWebHttpClient implements AutoCloseable {
    private static final Attributes EMPTY_ATTRIBUTES = new Attributes(false, 0);
    private static final DateFormat STUDY_DATE_FORMATTER = new SimpleDateFormat("yyyyMMdd");
    private static final String STUDIES = "studies?StudyDate=";

    private static final String BOUNDARY = "boundary";
    private static final String APPLICATION_DICOM_JSON = "application/dicom+json";
    private static final String MULTIPART_DICOM = "multipart/related; type=application/dicom";
    public static final String INCLUDEFIELD = "includefield";

    private static final int DICOMWEB_CONNECTION_POOL_SIZE = 50;
    private static final int DICOMWEB_CONNECT_TIMEOUT_MS = 5 * 1000;
    private static final int DICOMWEB_READ_TIMEOUT_MS = 20 * 1000;

    private final CloseableHttpClient httpClient;
    private final PoolingHttpClientConnectionManager connectionManager;
    private final HttpClientContext context;
    private final String rootUrl;
    private final URL rootUrlObj;

    public DicomWebHttpClient(final String rootUrl, @Nullable final DicomWebCredential credentials) {
        this.rootUrl = StringUtils.appendIfMissing(rootUrl, "/");

        connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(DICOMWEB_CONNECTION_POOL_SIZE);
        connectionManager.setDefaultMaxPerRoute(DICOMWEB_CONNECTION_POOL_SIZE);

        final RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(DICOMWEB_CONNECT_TIMEOUT_MS)
                .setConnectionRequestTimeout(DICOMWEB_CONNECT_TIMEOUT_MS)
                .setSocketTimeout(DICOMWEB_READ_TIMEOUT_MS)
                .build();
        httpClient = HttpClientBuilder.create()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .build();
        context = HttpClientContext.create();

        try {
            rootUrlObj = new URL(rootUrl);
        } catch (MalformedURLException e) {
            throw new NrgServiceRuntimeException("Malformed dicom-web url: " + rootUrl, e);
        }

        final HttpHost httpHost = new HttpHost(rootUrlObj.getHost(), rootUrlObj.getPort());

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

    public void getAttributes(final List<String> pathSegments, final Map<Integer, String> searchKeys, final Consumer<Attributes> callback) throws PacsException {
        _getAttributes(pathSegments, searchKeys, callback);
    }

    public Attributes getAttributes(final List<String> pathSegments, final Map<Integer, String> searchKeys) throws PacsException {
        return _getAttributes(pathSegments, searchKeys, null).orElse(EMPTY_ATTRIBUTES);
    }

    public Attributes getAttributes(final URIBuilder uriBuilder, final Map<Integer, String> searchKeys) throws PacsException {
        return _getAttributes(uriBuilder, searchKeys, null).orElse(EMPTY_ATTRIBUTES);
    }

    private Optional<Attributes> _getAttributes(final List<String> pathSegments, final Map<Integer, String> searchKeys, final Consumer<Attributes> callback) throws PacsException {
        return _getAttributes(buildUri(pathSegments, searchKeys), callback);
    }

    private Optional<Attributes> _getAttributes(final URIBuilder uriBuilder, final Map<Integer, String> searchKeys, final Consumer<Attributes> callback) throws PacsException {
        final URI uri = appendSearchKeysAndBuild(uriBuilder, searchKeys);
        return _getAttributes(uri, callback);
    }
    private Optional<Attributes> _getAttributes(final URI uri, final Consumer<Attributes> callback) throws PacsException {
        return new RetryablePacsOperation<Optional<Attributes>>() {
            @Override
            @Nullable
            public Optional<Attributes> doOperationWithRetry() throws PacsException {
                return doGet(uri, callback);
            }
        }.call();
    }

    public void getItem(final String retrieveUrl, final Map<Integer, String> queryParamsByTag, final BiConsumer<Integer, MultipartInputStream> callback) throws PacsException {
        final URI rawUri = URI.create(retrieveUrl);
        final URI uri = appendSearchKeysAndBuild(new URIBuilder(rawUri), queryParamsByTag);
        _getItem(uri, callback);
    }

    public void _getItem(final URI uri, final BiConsumer<Integer, MultipartInputStream> callback) throws PacsException {
        new RetryablePacsOperation<Void>() {
            @Override
            public Void doOperationWithRetry() throws PacsException {
                doGetItem(uri, callback);
                return null;
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

    private Optional<Attributes> doGet(final URI uri, @Nullable final Consumer<Attributes> callback) throws PacsException {
        final HttpUriRequest request = new HttpGet(uri);
        request.setHeader(HttpHeaders.ACCEPT, APPLICATION_DICOM_JSON);
        log.debug("Executing request: {} {} {}", request.getAllHeaders(), request.getMethod(), request.getURI());
        try (final CloseableHttpResponse response = httpClient.execute(request, context)) {

            throwForHttpErrors(uri, response);

            try (final InputStream inputStream = response.getEntity().getContent();
                 final JsonParser jsonParser = Json.createParser(inputStream);
                 final JsonParser filteredParser = new DicomCorrectingJsonParser(jsonParser)
            ) {
                final JSONReader jsonReader = new JSONReader(filteredParser);
                jsonReader.setSkipBulkDataURI(true);
                if (callback == null) {
                    return Optional.of(jsonReader.readDataset(null));
                }
                log.debug("Parsing dicom json results");
                jsonReader.readDatasets((fmi, attributes) -> {
                    if (null != fmi) {
                        attributes.addAll(fmi);
                    }
                    callback.accept(attributes);
                });
                return Optional.empty();
            }
        } catch (IOException e) {
            log.error("Failed to request dicom from url: {}", uri, e);
            throw new PacsException("Failed to request dicom from url: " + uri, e);
        }
    }

    private void doGetItem(final URI uri, final BiConsumer<Integer, MultipartInputStream> callback) throws PacsException {
        final HttpUriRequest request = new HttpGet(uri);
        request.setHeader(HttpHeaders.ACCEPT, MULTIPART_DICOM);
        log.debug("Executing request: {} {} {}", request.getAllHeaders(), request.getMethod(), request.getURI());
        try (final CloseableHttpResponse response = httpClient.execute(request, context)) {

            throwForHttpErrors(uri, response);

            final String boundary = parseBoundaryFromContentType(response.getFirstHeader(HttpHeaders.CONTENT_TYPE))
                    .orElseThrow(() -> new DqrException("Failed to parse multipart response from dicom-web endpoint " + rootUrl));

            try (final InputStream inputStream = response.getEntity().getContent();
                 final BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream)) {
                new MultipartParser(boundary).parse(bufferedInputStream, callback::accept);
            }
        } catch (DqrRuntimeException e) {
            // Note: this is a workaround for the callback not being able to throw a checked exception
            throw new PacsException("Could not read parse DICOMweb response from " + uri.toString(), e.getCause());
        } catch (Exception e) {
            log.error("Failed to request dicom from url: {}", uri, e);
            throw new PacsException("Failed to request dicom from url: " + uri, e);
        }
    }

    private void throwForHttpErrors(URI uri, CloseableHttpResponse response) throws PacsException {
        final int httpStatus = response.getStatusLine().getStatusCode();
        if (!(200 <= httpStatus && httpStatus < 300)) {
            String errorMessage;
            try {
                errorMessage = IOUtils.toString(response.getEntity().getContent(), Charset.defaultCharset());
            } catch (IOException e) {
                errorMessage = "<Could not read error message from response>";
            }
            log.error("Response from server was {}:\n{}", httpStatus, errorMessage);

            if (401 == httpStatus) {
                throw new PacsAuthenticationException("Failed to authenticate with dicom-web endpoint " + rootUrl);
            } else if (404 == httpStatus) {
                throw new PacsDataNotFoundException("No data found at " + uri.toString());
            } else {
                throw new PacsException("Failed to request dicom from url: " + uri.toString() + ". Server returned " + httpStatus);
            }
        }
    }

    @Override
    public void close() {
        try {
            httpClient.close();
            connectionManager.close();
        } catch (IOException e) {
            log.error("Failed to close {}", getClass().getSimpleName(), e);
        }
    }

    private URI buildUri(final Collection<String> pathSegments, final Map<Integer, String> searchKeys) throws PacsConnectionException {
        final List<String> combinedPathSegments = Stream.concat(
                        Arrays.stream(rootUrlObj.getPath().split("/")),
                        pathSegments.stream()
                )
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());

        final URIBuilder uriBuilder = new URIBuilder()
                .setScheme(rootUrlObj.getProtocol())
                .setHost(rootUrlObj.getHost())
                .setPort(rootUrlObj.getPort())
                .setPathSegments(combinedPathSegments);

        return appendSearchKeysAndBuild(uriBuilder, searchKeys);
    }

    private URI appendSearchKeysAndBuild(final URIBuilder uriBuilder, final Map<Integer, String> searchKeys) throws PacsConnectionException {
        final List<String> includeFields = new ArrayList<>();
        for (final Map.Entry<Integer, String> entry : searchKeys.entrySet()) {
            final String value = entry.getValue();
            final String key = String.format("%08X", entry.getKey());
            if (value == null) {
                includeFields.add(key);
            } else {
                uriBuilder.addParameter(key, value);
            }
        }
        if (!includeFields.isEmpty()) {
            uriBuilder.addParameter(INCLUDEFIELD, StringUtils.join(includeFields, ","));
        }
        try {
            return uriBuilder.build();
        } catch (URISyntaxException e) {
            throw new PacsConnectionException("Invalid DICOMweb URI", e);
        }
    }

    private static Optional<String> parseBoundaryFromContentType(final Header contentType) {
        return Arrays.stream(contentType.getElements())
                .map(headerElement -> headerElement.getParameterByName(BOUNDARY))
                .filter(Objects::nonNull)
                .map(NameValuePair::getValue)
                .findFirst();
    }
}

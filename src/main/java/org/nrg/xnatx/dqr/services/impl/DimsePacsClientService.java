package org.nrg.xnatx.dqr.services.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.ElementDictionary;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.nrg.dcm.scp.DicomSCPManager;
import org.nrg.xapi.exceptions.DataFormatException;
import org.nrg.xdat.om.XnatImagescandata;
import org.nrg.xnatx.dqr.dicom.command.cecho.CEchoSCU;
import org.nrg.xnatx.dqr.dicom.command.cecho.dcm4che.tool.Dcm4cheToolCEchoSCU;
import org.nrg.xnatx.dqr.dicom.command.cfind.CFindSCU;
import org.nrg.xnatx.dqr.dicom.command.cfind.dcm4che.tool.Dcm4cheToolCFindSCU;
import org.nrg.xnatx.dqr.dicom.command.cmove.CMoveSCU;
import org.nrg.xnatx.dqr.dicom.command.cmove.CMoveTargetNotFoundException;
import org.nrg.xnatx.dqr.dicom.command.cmove.dcm4che.tool.Dcm4cheToolCMoveSCU;
import org.nrg.xnatx.dqr.dicom.command.cstore.BasicCStoreSCU;
import org.nrg.xnatx.dqr.dicom.command.cstore.CStoreSCU;
import org.nrg.xnatx.dqr.dicom.dimse.QrClient;
import org.nrg.xnatx.dqr.dicom.net.DicomConnectionProperties;
import org.nrg.xnatx.dqr.dicom.strategy.orm.OrmStrategy;
import org.nrg.xnatx.dqr.domain.Patient;
import org.nrg.xnatx.dqr.domain.Series;
import org.nrg.xnatx.dqr.domain.Study;
import org.nrg.xnatx.dqr.domain.entities.Pacs;
import org.nrg.xnatx.dqr.dto.PacsSearchCriteria;
import org.nrg.xnatx.dqr.dto.PacsSearchResults;
import org.nrg.xnatx.dqr.exceptions.DqrException;
import org.nrg.xnatx.dqr.exceptions.DqrRuntimeException;
import org.nrg.xnatx.dqr.exceptions.PacsConnectionException;
import org.nrg.xnatx.dqr.exceptions.PacsException;
import org.nrg.xnatx.dqr.exceptions.PacsNotQueryableException;
import org.nrg.xnatx.dqr.exceptions.PacsQueryException;
import org.nrg.xnatx.dqr.preferences.DqrPreferences;
import org.nrg.xnatx.dqr.services.PacsClientService;
import org.nrg.xnatx.dqr.utils.RetryablePacsOperation;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

@Slf4j
@Service
public class DimsePacsClientService implements PacsClientService {
    private static final List<Integer> BLOCKED_RETURN_TAGS = Arrays.asList(Tag.NumberOfStudyRelatedInstances, Tag.NumberOfStudyRelatedSeries);

    final DqrPreferences preferences;
    final DicomSCPManager dicomSCPManager;
    final Map<String, OrmStrategy> ormStrategies;

    public DimsePacsClientService(final DqrPreferences preferences, final DicomSCPManager dicomSCPManager, final Map<String, OrmStrategy> ormStrategies) {
        this.preferences = preferences;
        this.dicomSCPManager = dicomSCPManager;
        this.ormStrategies = ormStrategies;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canConnect(Pacs pacs) {
        return buildCEchoSCU(pacs).canConnect();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Study> getStudy(Pacs pacs, String studyInstanceUid) throws PacsException {
        return buildCFindSCU(pacs).cfindStudyById(studyInstanceUid);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PacsSearchResults<Patient> queryPatients(Pacs pacs, PacsSearchCriteria searchCriteria) throws PacsException {
        return buildCFindSCU(pacs).cfindPatientsByExample(searchCriteria);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PacsSearchResults<Study> queryStudies(Pacs pacs, PacsSearchCriteria searchCriteria) throws PacsException, DataFormatException {
        try {
            return buildCFindSCU(pacs).cfindStudiesByExample(searchCriteria);
        } catch (DqrRuntimeException e) {
            throw new DataFormatException("A DQR run-time exception occurred, which usually indicates a problem performing a query to the PACS: " + Optional.ofNullable(e.getCause()).orElse(e).getMessage() + "\n\nThe search criteria for this query is: " + searchCriteria, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PacsSearchResults<Series> querySeries(Pacs pacs, PacsSearchCriteria searchCriteria) throws PacsException {
        try {
            return buildCFindSCU(pacs).cfindSeriesByExample(searchCriteria);
        } catch (DqrRuntimeException e) {
            throw new PacsQueryException("Could not find series", e);
        }
    }

    @Override
    public void queryPatients(Pacs pacs, Map<Integer, String> searchKeys, Consumer<Attributes> callback) throws PacsException {
        doCFind(pacs, buildQuery(searchKeys, QrClient.QueryRetrieveLevel.PATIENT), callback);
    }

    @Override
    public void queryStudies(Pacs pacs, Map<Integer, String> searchKeys, Consumer<Attributes> callback) throws PacsException {
        doCFind(pacs, buildQuery(searchKeys, QrClient.QueryRetrieveLevel.STUDY), callback);
    }

    @Override
    public void querySeries(Pacs pacs, Map<Integer, String> searchKeys, Consumer<Attributes> callback) throws PacsException {
        doCFind(pacs, buildQuery(searchKeys, QrClient.QueryRetrieveLevel.SERIES), callback);
    }

    @Override
    public void querySeries(Pacs pacs, String studyInstanceUid, Map<Integer, String> searchKeys, Consumer<Attributes> callback) throws PacsException {
        final Map<Integer, String> keys = new HashMap<>(searchKeys);
        keys.putIfAbsent(Tag.StudyInstanceUID, studyInstanceUid);
        keys.putIfAbsent(Tag.SeriesInstanceUID, null);
        querySeries(pacs, keys, callback);
    }

    @Override
    public void queryInstance(Pacs pacs, Map<Integer, String> searchKeys, Consumer<Attributes> callback) throws PacsException {
        doCFind(pacs, buildQuery(searchKeys, QrClient.QueryRetrieveLevel.IMAGE), callback);
    }

    @Override
    public void queryInstance(Pacs pacs, String studyInstanceUid, String seriesInstanceUid, Map<Integer, String> searchKeys, Consumer<Attributes> callback) throws PacsException {
        final Map<Integer, String> keys = new HashMap<>(searchKeys);
        keys.putIfAbsent(Tag.StudyInstanceUID, studyInstanceUid);
        keys.putIfAbsent(Tag.SeriesInstanceUID, seriesInstanceUid);
        keys.putIfAbsent(Tag.SOPInstanceUID, null);
        queryInstance(pacs, keys, callback);
    }

    @Override
    public Attributes getInstanceMetadata(Pacs pacs, String studyInstanceUid, String seriesInstanceUid, String sopInstanceUid, Map<Integer, String> searchKeys) throws PacsException {
        final Map<Integer, String> keys = new HashMap<>(searchKeys);
        keys.put(Tag.SOPInstanceUID, sopInstanceUid);

        final Attributes attributes = new Attributes();
        queryInstance(pacs, studyInstanceUid, seriesInstanceUid, keys, attributes::addAll);
        return attributes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void importSeries(final Pacs pacs, final Study study, final Series series, final String ae) throws DqrException {
        try {
            buildCMoveSCU(pacs, ae).cmoveSeries(study, series);
        } catch (final CMoveTargetNotFoundException exception) {
            log.warn("C-MOVE target not found somehow: PACS {}", pacs, exception);
            throw new DqrException(exception);
        }
    }

    @Override
    public void importInstance(Pacs pacs, String studyInstanceUid, String seriesInstanceUid, String sopInstanceUid, String destinationAe) throws PacsException {

        final Map<Integer, String> keys = new HashMap<>();
        keys.put(Tag.StudyInstanceUID, studyInstanceUid);
        keys.put(Tag.SeriesInstanceUID, seriesInstanceUid);
        keys.put(Tag.SOPInstanceUID, sopInstanceUid);

        doCMove(buildQuery(keys, QrClient.QueryRetrieveLevel.IMAGE), pacs, destinationAe);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void exportSeries(final Pacs pacs, final XnatImagescandata series) {
        buildCStoreSCU(pacs).cstoreSeries(series);
    }

    private void doCFind(final Pacs pacs, final Attributes attributes, final Consumer<Attributes> callback)
            throws PacsException {

        final QrClient.QrClientBuilder builder = QrClient.builder()
                .remoteAe(pacs.getAeTitle())
                .remoteHost(pacs.getHost())
                .remotePort(pacs.getQueryRetrievePort())
                .localAe(getLocalAETitle());

        new RetryablePacsOperation<Void>(pacs) {
            @Override
            @Nullable
            public Void doOperationWithRetry() throws PacsConnectionException {
                try (final QrClient client = builder.build()) {
                    client.query(attributes, callback);
                }
                return null;
            }
        }.call();
    }

    private void doCMove(final Attributes attributes, final Pacs pacs, final String destination) throws PacsException {

        final QrClient.QrClientBuilder builder = QrClient.builder()
                .remoteAe(pacs.getAeTitle())
                .remoteHost(pacs.getHost())
                .remotePort(pacs.getQueryRetrievePort())
                .localAe(getLocalAETitle())
                .destination(destination);

        new RetryablePacsOperation<Void>(pacs) {
            @Override
            @Nullable
            public Void doOperationWithRetry() throws PacsConnectionException {
                try (final QrClient client = builder.build()) {
                    log.debug("QRClient: Performing C-Move with keys: {}", attributes);
                    client.move(attributes);
                }
                return null;
            }
        }.call();
    }


    /**
     * Builds an attributes object for querying the PACS. The query tags map should use the integer value for DICOM tags
     * as the key. Any values to be used for filtering results should be set in the map, while any additional keys that
     * you want returned in the query result should be set to null or an empty ("") string.
     *
     * @param queryTags The tags to be added to the attributes object
     * @param level     The QR level
     *
     * @return The attributes object containing the various query keys and return tags.
     */
    private Attributes buildQuery(final Map<Integer, String> queryTags, final QrClient.QueryRetrieveLevel level) {
        final Attributes queryAttrs = new Attributes();

        queryTags.keySet().forEach(k -> {
            final String value = queryTags.get(k);
            if (StringUtils.isEmpty(value)) {
                queryAttrs.setNull(k, getVr(k, queryAttrs));
            } else {
                queryAttrs.setString(k, getVr(k, queryAttrs), value);
            }
        });

        BLOCKED_RETURN_TAGS.forEach(queryAttrs::remove);
        queryAttrs.setString(Tag.QueryRetrieveLevel, getVr(Tag.QueryRetrieveLevel, queryAttrs), level.toString());
        return queryAttrs;
    }

    private VR getVr(int tag, final Attributes attributes) {
        return ElementDictionary.vrOf(tag, attributes.getPrivateCreator(tag));
    }

    private CEchoSCU buildCEchoSCU(final Pacs pacs) {
        return new Dcm4cheToolCEchoSCU(preferences, buildDicomConnectionProperties(pacs));
    }

    private CFindSCU buildCFindSCU(final Pacs pacs) throws PacsNotQueryableException {
        if (!pacs.isQueryable()) {
            throw new PacsNotQueryableException(pacs.getId());
        }
        return new Dcm4cheToolCFindSCU(preferences, buildDicomConnectionProperties(pacs), getOrmStrategy(pacs));
    }

    private CMoveSCU buildCMoveSCU(final Pacs pacs, final String receiverAETitle) {
        return new Dcm4cheToolCMoveSCU(preferences, buildDicomConnectionProperties(pacs, receiverAETitle), getOrmStrategy(pacs));
    }

    private CStoreSCU buildCStoreSCU(final Pacs pacs) {
        return new BasicCStoreSCU(preferences, buildDicomConnectionProperties(pacs));
    }

    private String getLocalAETitle() {
        return dicomSCPManager.getDicomSCPInstances().values().iterator().next().getAeTitle();
    }

    private DicomConnectionProperties buildDicomConnectionProperties(final Pacs pacs) {
        return buildDicomConnectionProperties(pacs, null);
    }

    private DicomConnectionProperties buildDicomConnectionProperties(final Pacs pacs, final String receiverAETitle) {
        final String localAeTitle = StringUtils.isNotBlank(receiverAETitle) ? receiverAETitle : getLocalAETitle();
        return new DicomConnectionProperties(localAeTitle, pacs);
    }

    private OrmStrategy getOrmStrategy(final Pacs pacs) {
        final String beanId = pacs.getOrmStrategySpringBeanId();
        if (!ormStrategies.containsKey(beanId)) {
            throw new DqrRuntimeException(String.format("Failed to load the ORM strategy defined by bean '%s'", pacs.getOrmStrategySpringBeanId()));
        }
        return ormStrategies.get(beanId);
    }
}

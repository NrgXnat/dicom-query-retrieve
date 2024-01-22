package org.nrg.xnatx.dqr.services.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.dcm4che3.data.Attributes;
import org.nrg.dcm.scp.DicomSCPInstance;
import org.nrg.dcm.scp.DicomSCPManager;
import org.nrg.xapi.exceptions.DataFormatException;
import org.nrg.xdat.om.XnatImagescandata;
import org.nrg.xnatx.dqr.dicom.command.cecho.CEchoSCU;
import org.nrg.xnatx.dqr.dicom.command.cecho.dcm4che.tool.Dcm4cheToolCEchoSCU;
import org.nrg.xnatx.dqr.dicom.command.cfind.CFindSCU;
import org.nrg.xnatx.dqr.dicom.command.cfind.dcm4che.tool.Dcm4cheToolCFindSCU;
import org.nrg.xnatx.dqr.dicom.command.cmove.CMoveSCU;
import org.nrg.xnatx.dqr.dicom.command.cmove.dcm4che.tool.Dcm4cheToolCMoveSCU;
import org.nrg.xnatx.dqr.dicom.command.cstore.BasicCStoreSCU;
import org.nrg.xnatx.dqr.dicom.command.cstore.CStoreSCU;
import org.nrg.xnatx.dqr.dicom.net.DicomConnectionProperties;
import org.nrg.xnatx.dqr.dicom.strategy.orm.OrmStrategy;
import org.nrg.xnatx.dqr.domain.Patient;
import org.nrg.xnatx.dqr.domain.Series;
import org.nrg.xnatx.dqr.domain.Study;
import org.nrg.xnatx.dqr.domain.entities.Pacs;
import org.nrg.xnatx.dqr.dto.PacsSearchCriteria;
import org.nrg.xnatx.dqr.dto.PacsSearchResults;
import org.nrg.xnatx.dqr.exceptions.DqrRuntimeException;
import org.nrg.xnatx.dqr.exceptions.PacsException;
import org.nrg.xnatx.dqr.exceptions.PacsNotQueryableException;
import org.nrg.xnatx.dqr.preferences.DqrPreferences;
import org.nrg.xnatx.dqr.services.PacsClientService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class DimsePacsClientService implements PacsClientService {
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
        return buildCFindSCU(pacs).cfindSeriesByExample(searchCriteria);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Patient> queryPatients(Pacs pacs, Attributes searchCriteria) throws PacsException {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Study> queryStudies(Pacs pacs, Attributes searchCriteria) throws PacsException {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Series> querySeries(Pacs pacs, Attributes searchCriteria) throws PacsException {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void importSeries(final Pacs pacs, final Study study, final Series series, final String ae) {
        buildCMoveSCU(pacs, ae).cmoveSeries(study, series);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void exportSeries(final Pacs pacs, final XnatImagescandata series) {
        buildCStoreSCU(pacs).cstoreSeries(series);
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

    private DicomConnectionProperties buildDicomConnectionProperties(final Pacs pacs) {
        // At some point in the future caller will probably specify AE as well
        // For now, this is an ugly hack that just uses the first defined AE in the XNAT webapp
        DicomSCPInstance firstXnatScp = dicomSCPManager.getDicomSCPInstances()
                .values().iterator().next();
        final String localAETitle = firstXnatScp.getAeTitle();
        return new DicomConnectionProperties(localAETitle, pacs);
    }

    private DicomConnectionProperties buildDicomConnectionProperties(final Pacs pacs, final String receiverAETitle) {
        if (!StringUtils.isBlank(receiverAETitle)) {
            return new DicomConnectionProperties(receiverAETitle, pacs);
        } else {
            return buildDicomConnectionProperties(pacs);
        }
    }

    private OrmStrategy getOrmStrategy(final Pacs pacs) {
        final String beanId = pacs.getOrmStrategySpringBeanId();
        if (!ormStrategies.containsKey(beanId)) {
            throw new DqrRuntimeException(String.format("Failed to load the ORM strategy defined by bean '%s'", pacs.getOrmStrategySpringBeanId()));
        }
        return ormStrategies.get(beanId);
    }
}

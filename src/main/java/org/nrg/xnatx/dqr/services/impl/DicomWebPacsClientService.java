package org.nrg.xnatx.dqr.services.impl;

import org.dcm4che3.data.Attributes;
import org.nrg.xdat.om.XnatImagescandata;
import org.nrg.xnatx.dqr.domain.Patient;
import org.nrg.xnatx.dqr.domain.Series;
import org.nrg.xnatx.dqr.domain.Study;
import org.nrg.xnatx.dqr.domain.entities.Pacs;
import org.nrg.xnatx.dqr.dto.PacsSearchCriteria;
import org.nrg.xnatx.dqr.dto.PacsSearchResults;
import org.nrg.xnatx.dqr.exceptions.PacsException;
import org.nrg.xnatx.dqr.services.PacsClientService;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

@Service
public class DicomWebPacsClientService implements PacsClientService {
    public DicomWebPacsClientService() {
    }

    @Override
    public boolean canConnect(Pacs pacs) {
        return false;
    }

    @Override
    public Optional<Study> getStudy(Pacs pacs, String studyId) throws PacsException {
        return Optional.empty();
    }

    @Override
    public PacsSearchResults<Patient> queryPatients(Pacs pacs, PacsSearchCriteria searchCriteria) throws PacsException {
        return null;
    }

    @Override
    public PacsSearchResults<Study> queryStudies(Pacs pacs, PacsSearchCriteria searchCriteria) throws PacsException {
        return null;
    }

    @Override
    public PacsSearchResults<Series> querySeries(Pacs pacs, PacsSearchCriteria searchCriteria) throws PacsException {
        return null;
    }

    @Override
    public void queryPatients(Pacs pacs, Map<Integer, String> searchKeys, Consumer<Attributes> callback) throws PacsException {

    }

    @Override
    public void queryStudies(Pacs pacs, Map<Integer, String> searchKeys, Consumer<Attributes> callback) throws PacsException {

    }

    @Override
    public void querySeries(Pacs pacs, Map<Integer, String> searchKeys, Consumer<Attributes> callback) throws PacsException {

    }

    @Override
    public void queryInstance(Pacs pacs, Map<Integer, String> searchKeys, Consumer<Attributes> callback) throws PacsException {

    }

    @Override
    public void query(Pacs pacs, Attributes searchCriteria, final Consumer<Attributes> callback) throws PacsException {

    }

    @Override
    public void importSeries(Pacs pacs, Study study, Series series, String ae) {

    }

    @Override
    public void importInstance(Pacs pacs, String studyInstanceUid, String seriesInstanceUid, String sopInstanceUid, String destinationAe) {

    }

    @Override
    public void exportSeries(Pacs pacs, XnatImagescandata series) {
        throw new RuntimeException("Export over DICOMweb not implemented");
    }
}

package org.nrg.xnatx.dqr.services.impl.hibernate;

import lombok.extern.slf4j.Slf4j;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.nrg.xdat.model.XnatAbstractresourceI;
import org.nrg.xdat.model.XnatImagescandataI;
import org.nrg.xnatx.dqr.domain.daos.ArchivedRequestedSeriesDAO;
import org.nrg.xnatx.dqr.domain.entities.ArchivedRequestedSeries;
import org.nrg.xnatx.dqr.domain.entities.SeriesRetrievalRequest;
import org.nrg.xnatx.dqr.services.ArchivedRequestedSeriesService;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Optional;

@Slf4j
@Service
@Transactional
public class HibernateArchivedRequestedSeriesService
        extends AbstractHibernateEntityService<ArchivedRequestedSeries, ArchivedRequestedSeriesDAO>
        implements ArchivedRequestedSeriesService {
    @Override
    public Optional<ArchivedRequestedSeries> latestMatching(SeriesRetrievalRequest seriesRequest) {
        return getDao().latest(
                seriesRequest.getStudyInstanceUid(),
                seriesRequest.getSeriesInstanceUid(),
                seriesRequest.getDestinationProject()
        );
    }

    @Override
    public Optional<ArchivedRequestedSeries> createIfDicom(final String studyInstanceUid, final XnatImagescandataI scan) {
        boolean isDicom = false;
        int nInstances = 0;
        long nBytes = 0;
        for (final XnatAbstractresourceI resource : scan.getFile()) {
            if ("DICOM".equals(resource.getLabel())) {
                isDicom = true;
                try {
                    nInstances += resource.getFileCount();
                    nBytes += Long.valueOf(resource.getFileSize().toString());
                } catch (Throwable t) {
                    log.error("Unable to extract sizes from DICOM resource", t);
                }
            }
        }
        if (isDicom) {
            final ArchivedRequestedSeries series = ArchivedRequestedSeries.builder()
                    .studyInstanceUid(studyInstanceUid)
                    .seriesInstanceUid(scan.getUid())
                    .xnatProject(scan.getProject())
                    .instancesArchived(nInstances)
                    .bytesArchived(nBytes)
                    .build();
            return Optional.of(create(series));
        } else {
            return Optional.empty();
        }
    }
}

/**
 * Copyright (c) 2023 Flywheel
 */
package org.nrg.xnatx.dqr.services.impl.hibernate;

import org.dcm4che2.data.DicomObject;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.nrg.xft.security.UserI;
import org.nrg.xnatx.dqr.domain.daos.SeriesRetrievalStatusDAO;
import org.nrg.xnatx.dqr.domain.entities.PaginatedPacsRequest;
import org.nrg.xnatx.dqr.domain.entities.SeriesRetrievalStatus;
import org.nrg.xnatx.dqr.services.SeriesRetrievalStatusService;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;

@Transactional
@Service
public class HibernateSeriesRetrievalStatusService
        extends AbstractHibernateEntityService<SeriesRetrievalStatus, SeriesRetrievalStatusDAO>
        implements SeriesRetrievalStatusService {
    public List<SeriesRetrievalStatus> findByStudyProject(String studyInstanceUid, String project) {
        return getDao().findByStudyProject(studyInstanceUid, project);
    }

    public void updateRetrievalStatistics(final SeriesRetrievalStatus status, final int addFiles, final long addBytes) {
        status.setNumberOfDownloadedInstances(addFiles + status.getNumberOfDownloadedInstances());
        status.setBytesDownloaded(addBytes + status.getBytesDownloaded());
        update(status);
    }

    public void createFromCFindResults(final String username, final String project, final Iterable<DicomObject> series) {
        for (final DicomObject o : series) {
            create(SeriesRetrievalStatus.fromCFindResult(username, project, o));
        }
    }

    @Override
    public List<SeriesRetrievalStatus> getAllForUser(UserI user, PaginatedPacsRequest request) {
        return null;
    }
}

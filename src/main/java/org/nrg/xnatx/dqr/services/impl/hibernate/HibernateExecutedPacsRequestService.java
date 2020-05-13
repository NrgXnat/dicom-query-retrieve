package org.nrg.xnatx.dqr.services.impl.hibernate;

import org.nrg.xnatx.dqr.domain.daos.ExecutedPacsRequestDAO;
import org.nrg.xnatx.dqr.domain.entities.ExecutedPacsRequest;
import org.nrg.xnatx.dqr.services.ExecutedPacsRequestService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Created by mike on 1/19/18.
 */
@Service
public class HibernateExecutedPacsRequestService extends AbstractHibernatePacsRequestService<ExecutedPacsRequest, ExecutedPacsRequestDAO> implements ExecutedPacsRequestService {
    @Override
    @Transactional
    public ExecutedPacsRequest getMostRecentForPacs(final long pacsId) {
        return instance(getDao().findByPacsIdOrderedByMostRecent(pacsId));
    }

    @Override
    @Transactional
    public ExecutedPacsRequest getMostRecentForStudyInstanceUid(String studyInstanceUid) {
        return instance(getDao().findByStudyInstanceUidOrderedByMostRecent(studyInstanceUid));
    }
}

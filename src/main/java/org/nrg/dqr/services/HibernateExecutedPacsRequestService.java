package org.nrg.dqr.services;

import lombok.extern.slf4j.Slf4j;
import org.nrg.dqr.daos.ExecutedPacsRequestDAO;
import org.nrg.dqr.domain.entities.ExecutedPacsRequest;
import org.nrg.dqr.domain.entities.PaginatedPacsRequest;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.nrg.xft.security.UserI;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Created by mike on 1/19/18.
 */
@Service
@Transactional
@Slf4j
public class HibernateExecutedPacsRequestService extends AbstractHibernateEntityService<ExecutedPacsRequest, ExecutedPacsRequestDAO> implements ExecutedPacsRequestService {
    @Override
    public List<ExecutedPacsRequest> getAllForUser(final UserI user) {
        return getDao().findAllByUser(user);
    }

    @Override
    public List<ExecutedPacsRequest> getAllForUser(final UserI user, final PaginatedPacsRequest request) {
        return getDao().findAllByUser(user, request);
    }

    @Override
    public ExecutedPacsRequest getByIdAndUser(final long id, final UserI user) {
        return getTopItemSafely(getDao().findAllByIdAndUser(id, user));
    }

    @Override
    public ExecutedPacsRequest getMostRecentForPacs(final long pacsId) {
        return getTopItemSafely(getDao().findByPacsIdOrderedByMostRecent(pacsId));
    }

    @Override
    public ExecutedPacsRequest getMostRecentForStudyInstanceUid(final String studyInstanceUid) {
        return getTopItemSafely(getDao().findByStudyInstanceUidOrderedByMostRecent(studyInstanceUid));
    }

    private static ExecutedPacsRequest getTopItemSafely(final List<ExecutedPacsRequest> items) {
        return items == null || items.isEmpty() ? null : items.get(0);
    }
}

package org.nrg.dqr.services;

import lombok.extern.slf4j.Slf4j;
import org.nrg.dqr.daos.ExecutedPacsRequestDAO;
import org.nrg.dqr.domain.entities.ExecutedPacsRequest;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.nrg.xft.security.UserI;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Created by mike on 1/19/18.
 */
@Service
@Slf4j
public class HibernateExecutedPacsRequestService extends AbstractHibernateEntityService<ExecutedPacsRequest, ExecutedPacsRequestDAO> implements ExecutedPacsRequestService {
    @Override
    @Transactional
    public List<ExecutedPacsRequest> getAllForUser(UserI user) {
        return getDao().findAllForUser(user);
    }

    @Override
    @Transactional
    public ExecutedPacsRequest getByIdForUser(Long id, UserI user) {
        return getTopItemSafely(getDao().findByIdForUser(id, user));
    }

    @Override
    @Transactional
    public ExecutedPacsRequest getMostRecentForPacs(Long pacsId) {
        return getTopItemSafely(getDao().findByPacsidOrderedByMostRecent(pacsId));
    }

    @Override
    @Transactional
    public ExecutedPacsRequest getMostRecentForStudyInstanceUid(String studyInstanceUid) {
        return getTopItemSafely(getDao().findByStudyInstanceUidOrderedByMostRecent(studyInstanceUid));
    }

    private ExecutedPacsRequest getTopItemSafely(final List<ExecutedPacsRequest> items) {
        return items == null || items.isEmpty() ? null : items.get(0);
    }
}

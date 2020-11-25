package org.nrg.dqr.services;

import org.nrg.dqr.domain.entities.ExecutedPacsRequest;
import org.nrg.dqr.domain.entities.PaginatedPacsRequest;
import org.nrg.framework.orm.hibernate.BaseHibernateService;
import org.nrg.xft.security.UserI;

import java.util.List;

/**
 * Created by mike on 1/19/18.
 */
public interface ExecutedPacsRequestService extends BaseHibernateService<ExecutedPacsRequest> {
    List<ExecutedPacsRequest> getAllForUser(UserI user);

    List<ExecutedPacsRequest> getAllForUser(UserI user, PaginatedPacsRequest request);

    ExecutedPacsRequest getByIdAndUser(long id, UserI user);

    ExecutedPacsRequest getMostRecentForPacs(long pacsId);

    ExecutedPacsRequest getMostRecentForStudyInstanceUid(String studyInstanceUid);
}

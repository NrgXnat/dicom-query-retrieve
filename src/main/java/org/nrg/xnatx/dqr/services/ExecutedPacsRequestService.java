package org.nrg.xnatx.dqr.services;

import org.nrg.xnatx.dqr.domain.entities.ExecutedPacsRequest;
import org.nrg.framework.orm.hibernate.BaseHibernateService;
import org.nrg.xft.security.UserI;

import java.util.List;

/**
 * Created by mike on 1/19/18.
 */
public interface ExecutedPacsRequestService extends BaseHibernateService<ExecutedPacsRequest> {
    List<ExecutedPacsRequest> getAllForUser(UserI user);
    ExecutedPacsRequest getByIdForUser(Long id, UserI user);
    ExecutedPacsRequest getMostRecentForPacs(Long pacsId);
    ExecutedPacsRequest getMostRecentForStudyInstanceUid(String studyInstanceUid);
}

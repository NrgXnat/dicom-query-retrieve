package org.nrg.xnatx.dqr.services;

import java.util.List;
import org.nrg.framework.orm.hibernate.BaseHibernateService;
import org.nrg.xnatx.dqr.domain.entities.PacsPing;

/**
 * Created by mike on 1/19/18.
 */
public interface PacsPingService extends BaseHibernateService<PacsPing> {
    PacsPing getLatestPing(final long pacsId);

    List<PacsPing> getPings(final long pacsId);
}

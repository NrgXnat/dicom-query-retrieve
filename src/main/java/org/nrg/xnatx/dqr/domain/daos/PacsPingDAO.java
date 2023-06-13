/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.domain.daos.PacsPingDAO
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.domain.daos;

import org.apache.commons.lang3.tuple.Pair;
import org.nrg.framework.ajax.PaginatedRequest;
import org.nrg.framework.orm.hibernate.AbstractHibernateDAO;
import org.nrg.framework.orm.hibernate.QueryBuilder;
import org.nrg.xnatx.dqr.domain.entities.PacsPing;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created by mike on 1/19/18.
 */
@Repository
public class PacsPingDAO extends AbstractHibernateDAO<PacsPing> {
    public PacsPing getLatestPing(final long pacsId) {
        QueryBuilder<PacsPing> builder = newQueryBuilder();
        builder.where(builder.eq("pacsId", pacsId));
        builder.orderBy(Pair.of(PaginatedRequest.SortDir.DESC, "pingTime"));
        return instance(builder.getResults());
    }

    public List<PacsPing> getPings(final long pacsId) {
        return findByProperty("pacsId", pacsId);
    }
}

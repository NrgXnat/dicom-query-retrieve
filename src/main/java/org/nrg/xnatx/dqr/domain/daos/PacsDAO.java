/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.domain.daos.PacsDAO
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.domain.daos;

import com.google.common.collect.ImmutableMap;
import org.nrg.framework.orm.hibernate.AbstractHibernateDAO;
import org.nrg.framework.orm.hibernate.QueryBuilder;
import org.nrg.xnatx.dqr.domain.entities.Pacs;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static org.nrg.xnatx.dqr.domain.entities.Pacs.PROP_DEFAULT_QUERY_RETRIEVE_PACS;
import static org.nrg.xnatx.dqr.domain.entities.Pacs.PROP_DEFAULT_STORAGE_PACS;
import static org.nrg.xnatx.dqr.domain.entities.Pacs.PROP_QUERYABLE;
import static org.nrg.xnatx.dqr.domain.entities.Pacs.PROP_STORABLE;

@Repository
public class PacsDAO extends AbstractHibernateDAO<Pacs> {
    /**
     * Finds the PACS marked as the default query-retrieve PACS.
     *
     * @return The default query-retrieve PACS if one is marked.
     */
    public Optional<Pacs> findByDefaultQueryRetrievePacs() {
        return Optional.ofNullable(findByUniqueProperty(PROP_DEFAULT_QUERY_RETRIEVE_PACS, true));
    }

    /**
     * Finds the PACS marked as the default storage PACS.
     *
     * @return The default storage PACS if one is marked.
     */
    public Optional<Pacs> findByDefaultStoragePacs() {
        return Optional.ofNullable(findByUniqueProperty(PROP_DEFAULT_STORAGE_PACS, true));
    }

    public List<Pacs> findAllBut(final Pacs entity) {
        QueryBuilder<Pacs> builder = newQueryBuilder();
        builder.where(builder.ne("id", entity.getId()));
        return builder.getResults();
    }

    public List<Pacs> findAllStorable() {
        return findByProperty(PROP_STORABLE, true);
    }

    public List<Pacs> findAllQueryable() {
        return findByProperty(PROP_QUERYABLE, true);
    }

    public List<Pacs> findAllQueryableAndStorable() {
        return findByProperties(ImmutableMap.of(PROP_STORABLE, true, PROP_QUERYABLE, true));
    }
}

/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.domain.daos.PacsDAO
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.domain.daos;

import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.SimpleExpression;
import org.nrg.framework.orm.hibernate.AbstractHibernateDAO;
import org.nrg.xnatx.dqr.domain.entities.Pacs;
import org.nrg.xnatx.dqr.exceptions.PacsNotFoundException;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class PacsDAO extends AbstractHibernateDAO<Pacs> {
    private static final SimpleExpression CRITERIA_ENABLED                     = Restrictions.eq("enabled", true);
    private static final SimpleExpression CRITERIA_DEFAULT_QUERY_RETRIEVE_PACS = Restrictions.eq("defaultQueryRetrievePacs", true);
    private static final SimpleExpression CRITERIA_DEFAULT_STORAGE_PACS        = Restrictions.eq("defaultStoragePacs", true);
    private static final SimpleExpression CRITERIA_STORABLE                    = Restrictions.eq("storable", true);
    private static final SimpleExpression CRITERIA_QUERYABLE                   = Restrictions.eq("queryable", true);

    public Pacs getPacs(final long pacsId) throws PacsNotFoundException {
        final Pacs pacs = retrieve(pacsId);
        if (pacs == null) {
            throw new PacsNotFoundException(pacsId);
        }
        return pacs;
    }

    public void deletePacs(final long pacsId) throws PacsNotFoundException {
        delete(getPacs(pacsId));
    }

    /**
     * Finds the PACS marked as the default query-retrieve PACS.
     *
     * @return The default query-retrieve PACS if one is marked.
     */
    public Optional<Pacs> findByDefaultQueryRetrievePacs() {
        final List<Pacs> defaultDqrPacs = findByCriteria(CRITERIA_DEFAULT_QUERY_RETRIEVE_PACS, CRITERIA_ENABLED);
        return defaultDqrPacs.isEmpty() ? Optional.empty() : Optional.of(defaultDqrPacs.get(0));
    }

    /**
     * Finds the PACS marked as the default storage PACS.
     *
     * @return The default storage PACS if one is marked.
     */
    public Optional<Pacs> findByDefaultStoragePacs() {
        final List<Pacs> defaultDqrPacs = findByCriteria(CRITERIA_DEFAULT_STORAGE_PACS, CRITERIA_ENABLED);
        return defaultDqrPacs.isEmpty() ? Optional.empty() : Optional.of(defaultDqrPacs.get(0));
    }

    public List<Pacs> findAllBut(final Pacs entity) {
        return findByCriteria(Restrictions.ne("id", entity.getId()));
    }

    public List<Pacs> findAllStorable() {
        return findByCriteria(CRITERIA_STORABLE, CRITERIA_ENABLED);
    }

    public List<Pacs> findAllQueryable() {
        return findByCriteria(CRITERIA_QUERYABLE, CRITERIA_ENABLED);
    }

    public List<Pacs> findAllQueryableAndStorable() {
        return findByCriteria(CRITERIA_STORABLE, CRITERIA_QUERYABLE, CRITERIA_ENABLED);
    }
}

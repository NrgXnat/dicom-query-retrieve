/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.services.impl.hibernate.HibernatePacsEntityService
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.services.impl.hibernate;

import java.util.List;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.nrg.xnatx.dqr.domain.daos.PacsDAO;
import org.nrg.xnatx.dqr.domain.entities.Pacs;
import org.nrg.xnatx.dqr.services.PacsEntityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HibernatePacsEntityService extends AbstractHibernateEntityService<Pacs, PacsDAO> implements PacsEntityService {
    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public Pacs create(final Pacs entity) {
        clearDefaultPacsFlagsOnOtherEntitiesIfThisEntityIsTheNewDefault(entity);
        return super.create(entity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void update(final Pacs entity) {
        clearDefaultPacsFlagsOnOtherEntitiesIfThisEntityIsTheNewDefault(entity);
        super.update(entity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public List<Pacs> findAllQueryableAndStorable() {
        return getDao().findAllQueryableAndStorable();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public List<Pacs> findAllStorable() {
        return getDao().findAllStorable();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public List<Pacs> findAllQueryable() {
        return getDao().findAllQueryable();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Pacs> findAll(final boolean storable, final boolean queryable) {
        if (storable && queryable) {
            return findAllQueryableAndStorable();
        }
        if (storable) {
            return findAllStorable();
        }
        return queryable ? findAllQueryable() : getAll();
    }

    private void clearDefaultPacsFlagsOnOtherEntitiesIfThisEntityIsTheNewDefault(final Pacs entity) {
        if (entity.isDefaultStoragePacs() || entity.isDefaultQueryRetrievePacs()) {
            // Hibernate barfs if you load the same entity twice in a session, even if you only update it once.
            // So we have to exclude the entity in question here.
            final List<Pacs> allPacs = getDao().findAllBut(entity);
            for (final Pacs p : allPacs) {
                if (p.getId() != entity.getId()) {
                    if (entity.isDefaultStoragePacs()) {
                        p.setDefaultStoragePacs(false);
                    }
                    if (entity.isDefaultQueryRetrievePacs()) {
                        p.setDefaultQueryRetrievePacs(false);
                    }
                    super.update(p);
                }
            }
        }
    }
}

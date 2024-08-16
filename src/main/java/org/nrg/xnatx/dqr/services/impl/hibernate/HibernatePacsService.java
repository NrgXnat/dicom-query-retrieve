/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.services.impl.hibernate.HibernatePacsService
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.services.impl.hibernate;

import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.nrg.xnatx.dqr.domain.daos.PacsDAO;
import org.nrg.xnatx.dqr.domain.entities.Pacs;
import org.nrg.xnatx.dqr.dto.PacsSettings;
import org.nrg.xnatx.dqr.exceptions.InvalidPacsConfigurationException;
import org.nrg.xnatx.dqr.exceptions.PacsNotFoundException;
import org.nrg.xnatx.dqr.services.PacsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.apache.commons.lang3.StringUtils;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
public class HibernatePacsService extends AbstractHibernateEntityService<Pacs, PacsDAO> implements PacsService {
    /**
     * {@inheritDoc}
     */
    @Override
    public Pacs createPacs(final PacsSettings settings) throws InvalidPacsConfigurationException {
        return createPacs(new Pacs(settings));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Pacs createPacs(final Pacs pacs) throws InvalidPacsConfigurationException {
        return create(pacs);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Nullable
    public Pacs create(final Pacs entity) {
        try {
            validatePacs(entity);
            clearDefaultPacsFlagsOnOtherEntitiesIfThisEntityIsTheNewDefault(entity);
            return super.create(entity);
        } catch (InvalidPacsConfigurationException ipc) {
            log.error("Invalid Configuration: ", ipc);
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Pacs getPacs(final long pacsId) throws PacsNotFoundException {
        return getDao().getPacs(pacsId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Pacs updatePacs(long pacsId, PacsSettings settings) throws PacsNotFoundException, InvalidPacsConfigurationException {
        final Pacs entity = getPacs(pacsId);
        entity.copySettings(settings);
        update(entity);
        return entity;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(final Pacs entity) {
        try {
            validatePacs(entity);
            clearDefaultPacsFlagsOnOtherEntitiesIfThisEntityIsTheNewDefault(entity);
            super.update(entity);
        } catch (InvalidPacsConfigurationException ipc) {
            log.error("Invalid Configuration: ", ipc);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deletePacs(long pacsId) throws PacsNotFoundException {
        getDao().deletePacs(pacsId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Pacs> findDefaultQueryRetrievePacs() {
        return getDao().findByDefaultQueryRetrievePacs();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Pacs> findDefaultStoragePacs() {
        return getDao().findByDefaultStoragePacs();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Pacs> findAllQueryableAndStorable() {
        return getDao().findAllQueryableAndStorable();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Pacs> findAllStorable() {
        return getDao().findAllStorable();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Pacs> findAllQueryable() {
        return getDao().findAllQueryable();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Pacs> findAll(final boolean storable, final boolean queryable) {
        return storable && queryable ? findAllQueryableAndStorable() : (storable ? findAllStorable() : (queryable ? findAllQueryable() : getAll()));
    }

    private void validatePacs(final Pacs entity) throws InvalidPacsConfigurationException {
        final List<String> errors = new ArrayList<>();
        if (!entity.isDicomWebEnabled() &&  StringUtils.isBlank(entity.getHost())) {
            errors.add("DMSE PACS configuration must set the host");
        }
        if (entity.isDefaultStoragePacs() && !entity.isStorable()) {
            errors.add("PACS marked as default storage PACS must be storable");
        }
        if (entity.isDefaultQueryRetrievePacs() && !entity.isQueryable()) {
            errors.add("PACS marked as default query/retrieve PACS must be queryable");
        }
        if (entity.isDicomWebEnabled() && !entity.isQueryable()) {
            errors.add("DICOMweb PACS configurations must be queryable");
        }
        if (entity.isDicomWebEnabled() && entity.isStorable()) {
            errors.add("DICOMweb PACS configurations must not be storable");
        }

        if (!errors.isEmpty()) {
            throw new InvalidPacsConfigurationException(errors);
        }
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

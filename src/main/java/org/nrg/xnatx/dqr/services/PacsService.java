/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.services.PacsService
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.services;

import org.nrg.framework.orm.hibernate.BaseHibernateService;
import org.nrg.xnatx.dqr.domain.entities.Pacs;
import org.nrg.xnatx.dqr.dto.PacsSettings;
import org.nrg.xnatx.dqr.exceptions.InvalidPacsConfigurationException;
import org.nrg.xnatx.dqr.exceptions.PacsNotFoundException;

import java.util.List;
import java.util.Optional;

public interface PacsService extends BaseHibernateService<Pacs> {
    /**
     * Creates a new PACS.
     * @param settings The PACS to create.
     * @throws InvalidPacsConfigurationException If the PACS configuration is invalid.
     */
    Pacs createPacs(final PacsSettings settings) throws InvalidPacsConfigurationException;

    /**
     * Creates a new PACS.
     * @param pacs The PACS to create.
     * @throws InvalidPacsConfigurationException If the PACS configuration is invalid.
     */
    Pacs createPacs(final Pacs pacs) throws InvalidPacsConfigurationException;

    /**
     * Finds the PACS with the specified ID.
     *
     * @param pacsId The ID of the PACS to retrieve.
     *
     * @return The PACS with the specified ID.
     *
     * @throws PacsNotFoundException If no PACS with the specified ID is found.
     */
    Pacs getPacs(final long pacsId) throws PacsNotFoundException;

    /**
     * Updates the PACS with the specified ID.
     *
     * @param pacsId The ID of the PACS to update.
     * @param pacsUpdates The updates to apply to the PACS.
     * @return The updated PACS.
     * @throws PacsNotFoundException If no PACS with the specified ID is found.
     * @throws InvalidPacsConfigurationException If the PACS configuration is invalid.
     */
    Pacs updatePacs(final long pacsId, final PacsSettings pacsUpdates) throws PacsNotFoundException, InvalidPacsConfigurationException;

    /**
     * Deletes the PACS with the specified ID.
     *
     * @param pacsId The ID of the PACS to delete.
     *
     * @throws PacsNotFoundException If no PACS with the specified ID is found.
     */
    void deletePacs(final long pacsId) throws PacsNotFoundException;

    /**
     * Finds the PACS marked as the default query-retrieve PACS.
     *
     * @return The default query-retrieve PACS if one is marked.
     */
    Optional<Pacs> findDefaultQueryRetrievePacs();

    /**
     * Finds the PACS marked as the default storage PACS.
     *
     * @return The default storage PACS if one is marked.
     */
    Optional<Pacs> findDefaultStoragePacs();

    /**
     * Finds all PACS that support both query and store operations.
     *
     * @return A list of all PACS that support both query and store operations.
     */
    List<Pacs> findAllQueryableAndStorable();

    /**
     * Finds all PACS that support store operations.
     *
     * @return A list of all PACS that support store operations.
     */
    List<Pacs> findAllStorable();

    /**
     * Finds all PACS that support query operations.
     *
     * @return A list of all PACS that support query operations.
     */
    List<Pacs> findAllQueryable();

    /**
     * Finds all PACS that support store operations if the <b>storable</b> parameter is
     * <b>true</b> and/or query operations if the <b>queryable</b> parameter is <b>true</b>.
     * Note that one of these parameters being <b>false</b> doesn't search for instances
     * that specifically <i>don't</i> support that operation, just that whether the PACS
     * supports that operation isn't considered at all. This means that setting both parameters
     * to <b>false</b> is the same as calling {@link #getAll()}.
     *
     * @param storable  Indicates the service should search for PACS that support store operations.
     * @param queryable Indicates the service should search for PACS that support query operations.
     *
     * @return A list of all PACS that support the operations as defined by the parameter values.
     */
    List<Pacs> findAll(final boolean storable, final boolean queryable);
}

/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.services.PacsEntityService
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.services;

import java.util.List;
import org.nrg.framework.orm.hibernate.BaseHibernateService;
import org.nrg.xnatx.dqr.domain.entities.Pacs;

// TODO: Combine with PacsService
public interface PacsEntityService extends BaseHibernateService<Pacs> {
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

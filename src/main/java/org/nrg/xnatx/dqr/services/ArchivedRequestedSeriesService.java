/**
 * Copyright (c) 2023 Flywheel
 */
package org.nrg.xnatx.dqr.services;

import org.nrg.framework.orm.hibernate.BaseHibernateService;
import org.nrg.xdat.model.XnatImagescandataI;
import org.nrg.xnatx.dqr.domain.entities.ArchivedRequestedSeries;
import org.nrg.xnatx.dqr.domain.entities.SeriesRetrievalRequest;

import java.util.Optional;

public interface ArchivedRequestedSeriesService extends BaseHibernateService<ArchivedRequestedSeries> {
    /**
     * Retrieve the most recent record of an archived series matching the series request provided
     * @param seriesRequest series request to be matched
     * @return most recent record of the requested series being archived, or Optional::empty if no match
     */
    Optional<ArchivedRequestedSeries> latestMatching(SeriesRetrievalRequest seriesRequest);

    /**
     * Create and store an ArchivedRequestedSeries record for the named archived series, if the series
     * contains DICOM data.
     * @param studyInstanceUid study containing the series
     * @param series XNAT scan data record for the archived series
     * @return newly stored archived series record if the series contains DICOM, or Optional::empty otherwise
     */
    Optional<ArchivedRequestedSeries> createIfDicom(String studyInstanceUid, XnatImagescandataI series);
}

/**
 * Copyright (c) 2023 Flywheel
 */
package org.nrg.xnatx.dqr.services;

import org.dcm4che2.data.DicomObject;
import org.nrg.framework.orm.hibernate.BaseHibernateService;
import org.nrg.xft.security.UserI;
import org.nrg.xnatx.dqr.domain.entities.PaginatedPacsRequest;
import org.nrg.xnatx.dqr.domain.entities.SeriesRetrievalStatus;

import java.util.List;

public interface SeriesRetrievalStatusService extends BaseHibernateService<SeriesRetrievalStatus> {
    /**
     * Get series retrieval status records for the provided Study Instance UID and XNAT project.
     * @param studyInstanceUid
     * @param project
     * @return
     */
    List<SeriesRetrievalStatus> findByStudyProject(String studyInstanceUid, String project);

    /**
     * Update the provided series retrieval status record to incorporate received data.
     * @param series
     * @param addFiles count of added files
     * @param addBytes count of added bytes
     */
    void updateRetrievalStatistics(SeriesRetrievalStatus series, int addFiles, long addBytes);

    /**
     * Register a new series retrieval status record for each series returned from a C-FIND.
     * @param username requesting username
     * @param project project into which series will be downloaded
     * @param series series descriptions as returned from C-FIND
     */
    void createFromCFindResults(String username, String project, Iterable<DicomObject> series);

    /**
     * Retrieve all series retrieval status records with the provided requesting user.
     * @param user
     * @param request
     * @return
     */
    List<SeriesRetrievalStatus> getAllForUser(UserI user, PaginatedPacsRequest request);
}

/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.services.BasePacsRequestService
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.services;

import org.nrg.framework.orm.hibernate.BaseHibernateService;
import org.nrg.xapi.exceptions.NotFoundException;
import org.nrg.xft.security.UserI;
import org.nrg.xnatx.dqr.domain.entities.PacsRequest;
import org.nrg.xnatx.dqr.domain.entities.PaginatedPacsRequest;

import java.util.List;
import java.util.Map;

public interface BasePacsRequestService<R extends PacsRequest> extends BaseHibernateService<R> {
    long getAllForUserCount(final UserI user);

    long getAllForUserCount(final String username);

    List<R> getAllForUser(final UserI user);

    List<R> getAllForUser(final String username);

    List<R> getAllForUser(final UserI user, final PaginatedPacsRequest request);

    List<R> getAllForUser(final String username, final PaginatedPacsRequest request);

    R getByIdForUser(final long id, final UserI user) throws NotFoundException;

    R getByIdForUser(final long id, final String username) throws NotFoundException;

    List<R> getAllOrderedByDate();

    List<R> getAllOrderedByDate(final PaginatedPacsRequest request);

    R getMostRecentForPacs(final long pacsId);

    R getMostRecentForStudyInstanceUid(final String studyInstanceUid);

    // TODO: What is this Object nonsense here?
    List<Map<String, Object>> getAllWithOrder();

    // TODO: What is this Object nonsense here?
    List<Map<String, Object>> getAllWithOrderForUser(final UserI user);

    List<Map<String, Object>> getAllWithOrderForUser(final String username);

    List<R> getAllForPacsOrderedByPriorityAndDate(final long pacsId);

    List<R> getAllForPacsOrderedByPriorityAndDate(final long pacsId, final PaginatedPacsRequest request);

    void deleteAllWithRequestIdAndStatus(String requestId, List<String> statuses);

    void deleteAllWithRequestIdAndStudyInstanceUid(String requestId, String studyInstanceUid);
}

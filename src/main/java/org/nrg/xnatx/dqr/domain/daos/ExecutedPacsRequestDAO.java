/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.domain.daos.ExecutedPacsRequestDAO
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.domain.daos;

import java.util.List;

import org.hibernate.Query;
import org.nrg.framework.generics.GenericUtils;
import org.nrg.xnatx.dqr.domain.entities.ExecutedPacsRequest;
import org.springframework.stereotype.Repository;

/**
 * Created by mike on 1/19/18.
 */
@Repository
public class ExecutedPacsRequestDAO extends AbstractPacsRequestDAO<ExecutedPacsRequest> {
    @Override
    protected String getTimeSortProperty() {
        return "executedTime";
    }

    public ExecutedPacsRequest getByRequestIdAndStudyInstanceUidOrderedByMostRecent(final String requestId, final String studyInstanceUid) {
        final Query query = getSession().getNamedQuery(ExecutedPacsRequest.GET_BY_STUDY_INSTANCE_UID_AND_REQUEST_ID_ORDERED_BY_MOST_RECENT)
                .setParameter("requestId", requestId)
                .setParameter("studyInstanceUid", studyInstanceUid);
        final List<ExecutedPacsRequest> list = GenericUtils.convertToTypedList(query.list(), ExecutedPacsRequest.class);
        return instance(list);
    }
}

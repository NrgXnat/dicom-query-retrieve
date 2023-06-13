/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.domain.daos.StudyIdStudyInstanceUidMappingDAO
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.domain.daos;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.nrg.framework.ajax.PaginatedRequest;
import org.nrg.framework.orm.hibernate.AbstractHibernateDAO;
import org.nrg.framework.orm.hibernate.QueryBuilder;
import org.nrg.xnatx.dqr.domain.entities.StudyIdStudyInstanceUidMapping;
import org.springframework.stereotype.Repository;

/**
 * Created by mike on 1/19/18.
 */
@Repository
public class StudyIdStudyInstanceUidMappingDAO extends AbstractHibernateDAO<StudyIdStudyInstanceUidMapping> {
    public List<StudyIdStudyInstanceUidMapping> findAllForStudyInstanceUid(final String studyInstanceUid) {
        QueryBuilder<StudyIdStudyInstanceUidMapping> builder = newQueryBuilder();
        builder.where(builder.eq("studyInstanceUid", studyInstanceUid));
        builder.orderBy(Pair.of(PaginatedRequest.SortDir.DESC, "created"));
        return builder.getResults();
    }
}

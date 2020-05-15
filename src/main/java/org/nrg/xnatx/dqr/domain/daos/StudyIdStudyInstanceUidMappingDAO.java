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
import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.nrg.framework.orm.hibernate.AbstractHibernateDAO;
import org.nrg.xnatx.dqr.domain.entities.StudyIdStudyInstanceUidMapping;
import org.springframework.stereotype.Repository;

/**
 * Created by mike on 1/19/18.
 */
@Repository
public class StudyIdStudyInstanceUidMappingDAO extends AbstractHibernateDAO<StudyIdStudyInstanceUidMapping> {
    public List<StudyIdStudyInstanceUidMapping> findAllForStudyInstanceUid(final String studyInstanceUid) {
        final Criteria criteria = getCriteriaForType();
        criteria.add(Restrictions.eq("studyInstanceUid", studyInstanceUid));
        criteria.addOrder(Order.desc("created"));
        return checked(criteria.list());
    }
}

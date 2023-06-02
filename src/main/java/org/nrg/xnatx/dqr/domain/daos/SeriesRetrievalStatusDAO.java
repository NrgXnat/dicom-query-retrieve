/**
 * Copyright (c) 2023 Flywheel
 */
package org.nrg.xnatx.dqr.domain.daos;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.nrg.framework.generics.GenericUtils;
import org.nrg.framework.orm.hibernate.AbstractHibernateDAO;
import org.nrg.xnatx.dqr.domain.entities.SeriesRetrievalStatus;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class SeriesRetrievalStatusDAO extends AbstractHibernateDAO<SeriesRetrievalStatus> {
    public List<SeriesRetrievalStatus> findByStudyProject(final String studyInstanceUid,
                                                          final String project) {
        final Criteria criteria = getSession().createCriteria(getParameterizedType());
        criteria.add(Restrictions.eq("studyInstanceUid", studyInstanceUid));
        criteria.add(Restrictions.eq("project", project));
        return GenericUtils.convertToTypedList(criteria.list(), SeriesRetrievalStatus.class);
    }
}

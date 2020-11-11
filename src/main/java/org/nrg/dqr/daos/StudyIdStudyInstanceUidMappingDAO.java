package org.nrg.dqr.daos;

import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.nrg.dqr.domain.entities.StudyIdStudyInstanceUidMapping;
import org.nrg.framework.generics.GenericUtils;
import org.nrg.framework.orm.hibernate.AbstractHibernateDAO;
import org.nrg.xft.security.UserI;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created by mike on 1/19/18.
 */
@Repository
public class StudyIdStudyInstanceUidMappingDAO extends AbstractHibernateDAO<StudyIdStudyInstanceUidMapping> {
    @SuppressWarnings("unused")
    public List<StudyIdStudyInstanceUidMapping> findByIdForUser(final Long id, final UserI user) {
        return findByCriteria(Restrictions.eq("id", id), Restrictions.eq("username", user.getUsername()));
    }

    public List<StudyIdStudyInstanceUidMapping> findAllForStudyInstanceUid(final String studyInstanceUid) {
        final Criteria criteria = getCriteriaForType();
        criteria.add(Restrictions.eq("studyInstanceUid", studyInstanceUid));
        criteria.addOrder(Order.desc("created"));
        return GenericUtils.convertToTypedList(criteria.list(), StudyIdStudyInstanceUidMapping.class);
    }
}

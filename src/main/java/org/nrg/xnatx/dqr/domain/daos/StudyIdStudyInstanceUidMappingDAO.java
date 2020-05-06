package org.nrg.xnatx.dqr.domain.daos;

import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.nrg.xnatx.dqr.domain.entities.StudyIdStudyInstanceUidMapping;
import org.nrg.framework.orm.hibernate.AbstractHibernateDAO;
import org.nrg.xft.security.UserI;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created by mike on 1/19/18.
 */
@Repository
public class StudyIdStudyInstanceUidMappingDAO extends AbstractHibernateDAO<StudyIdStudyInstanceUidMapping> {

    public List<StudyIdStudyInstanceUidMapping> findByIdForUser(Long id, UserI user){
        return findByCriteria(Restrictions.eq("id", id),Restrictions.eq("username", user.getUsername()));
    }
    public List<StudyIdStudyInstanceUidMapping> findAllForStudyInstanceUid(String studyInstanceUid){
        final Criteria criteria = getCriteriaForType();
        criteria.add(Restrictions.eq("studyInstanceUid", studyInstanceUid));
        criteria.addOrder(Order.desc("created"));
        return criteria.list();
    }
}

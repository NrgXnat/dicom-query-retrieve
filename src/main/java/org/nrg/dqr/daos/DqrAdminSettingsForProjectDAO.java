/*
 * web: org.nrg.xnat.node.dao.XnatNodeInfoDAO
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2017, Washington University School of Medicine and Howard Hughes Medical Institute
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.dqr.daos;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.nrg.dqr.domain.entities.DqrAdminSettingsForProject;
import org.nrg.framework.orm.hibernate.AbstractHibernateDAO;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Class DqrAdminSettingsForProjectDAO.
 */
@Repository
public class DqrAdminSettingsForProjectDAO extends AbstractHibernateDAO<DqrAdminSettingsForProject> {
    @Transactional
    public DqrAdminSettingsForProject getDqrAdminSettingsByProjectId(String projectId){
        final Criteria criteria = getSession().createCriteria(getParameterizedType());
        criteria.add(Restrictions.eq("projectId", projectId));
        return (DqrAdminSettingsForProject) criteria.uniqueResult();
    }
}

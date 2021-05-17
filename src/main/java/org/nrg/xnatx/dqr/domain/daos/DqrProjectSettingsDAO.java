/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.domain.daos.DqrProjectSettingsDAO
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.domain.daos;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.nrg.framework.orm.hibernate.AbstractHibernateDAO;
import org.nrg.xnatx.dqr.domain.entities.DqrProjectSettings;
import org.springframework.stereotype.Repository;

/**
 * The Class DqrProjectSettingsDAO.
 */
@Repository
public class DqrProjectSettingsDAO extends AbstractHibernateDAO<DqrProjectSettings> {
    public DqrProjectSettings findByProjectId(final String projectId) {
        final Criteria criteria = getSession().createCriteria(getParameterizedType());
        criteria.add(Restrictions.eq("projectId", projectId));
        return instance(checked(criteria.list()));
    }
}

/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.domain.daos.PacsDAO
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.domain.daos;

import java.util.List;
import org.hibernate.criterion.Restrictions;
import org.nrg.framework.orm.hibernate.AbstractHibernateDAO;
import org.nrg.xnatx.dqr.domain.entities.Pacs;
import org.springframework.stereotype.Repository;

@Repository
public class PacsDAO extends AbstractHibernateDAO<Pacs> {
    public List<Pacs> findAllBut(final Pacs entity) {
        return findByCriteria(Restrictions.ne("id", entity.getId()));
    }

    public List<Pacs> findAllStorable() {
        return findByCriteria(Restrictions.eq("storable", true), Restrictions.eq("enabled", true));
    }

    public List<Pacs> findAllQueryable() {
        return findByCriteria(Restrictions.eq("queryable", true), Restrictions.eq("enabled", true));
    }

    public List<Pacs> findAllQueryableAndStorable() {
        return findByCriteria(Restrictions.eq("storable", true), Restrictions.eq("queryable", true), Restrictions.eq("enabled", true));
    }
}

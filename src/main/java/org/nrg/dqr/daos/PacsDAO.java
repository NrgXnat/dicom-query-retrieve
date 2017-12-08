/*
 * PacsDAO
 * DQR is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.nrg.dqr.daos;

import java.util.List;

import org.hibernate.criterion.Restrictions;
import org.nrg.framework.orm.hibernate.AbstractHibernateDAO;
import org.nrg.dqr.domain.entities.Pacs;
import org.springframework.stereotype.Repository;

@Repository
public class PacsDAO extends AbstractHibernateDAO<Pacs> {

    public List<Pacs> findAllBut(final Pacs entity) {
        return findByCriteria(Restrictions.ne("id", entity.getId()));
    }
}

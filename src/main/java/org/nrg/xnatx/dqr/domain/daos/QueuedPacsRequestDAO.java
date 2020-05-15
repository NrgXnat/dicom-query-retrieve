/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.domain.daos.QueuedPacsRequestDAO
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
import org.nrg.xft.security.UserI;
import org.nrg.xnatx.dqr.domain.entities.PacsRequest;
import org.nrg.xnatx.dqr.domain.entities.QueuedPacsRequest;
import org.springframework.stereotype.Repository;

/**
 * Created by mike on 1/19/18.
 */
@Repository
public class QueuedPacsRequestDAO extends AbstractPacsRequestDAO<QueuedPacsRequest> {
    public List<QueuedPacsRequest> findAllForUser(final UserI user) {
        return findByCriteria(Restrictions.eq("username", user.getUsername()));
    }

    public List<QueuedPacsRequest> findAllForPacsOrderedByPriorityAndDate(final long pacsId) {
        final Criteria criteria = getSession().createCriteria(getParameterizedType());
        criteria.add(Restrictions.eq("pacsId", pacsId));
        criteria.addOrder(Order.asc("priority"));
        criteria.addOrder(Order.asc("queuedTime"));
        return checked(criteria.list());
    }

    public List<QueuedPacsRequest> findQueuedOrFailedForPacsOrderedByPriorityAndDate(final long pacsId) {
        final Criteria criteria = getSession().createCriteria(getParameterizedType());
        criteria.add(Restrictions.eq("pacsId", pacsId));
        criteria.add(Restrictions.in("status", new String[]{PacsRequest.FAILED_STATUS_TEXT, PacsRequest.QUEUED_STATUS_TEXT}));
        criteria.addOrder(Order.asc("priority"));
        criteria.addOrder(Order.asc("queuedTime"));
        return checked(criteria.list());
    }
}

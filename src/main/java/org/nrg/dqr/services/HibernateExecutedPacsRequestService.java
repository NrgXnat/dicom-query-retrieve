package org.nrg.dqr.services;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nrg.dqr.daos.ExecutedPacsRequestDAO;
import org.nrg.dqr.domain.entities.ExecutedPacsRequest;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

/**
 * Created by mike on 1/19/18.
 */
@Service
public class HibernateExecutedPacsRequestService extends AbstractHibernateEntityService<ExecutedPacsRequest, ExecutedPacsRequestDAO> implements ExecutedPacsRequestService {

    private static final Log _log = LogFactory.getLog(HibernateExecutedPacsRequestService.class);

    @Inject
    private ExecutedPacsRequestDAO _dao;

}

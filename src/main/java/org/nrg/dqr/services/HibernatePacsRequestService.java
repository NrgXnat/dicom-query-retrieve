package org.nrg.dqr.services;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nrg.dqr.daos.PacsRequestDAO;
import org.nrg.dqr.domain.entities.PacsRequest;
import org.nrg.dqr.services.PacsRequestService;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.nrg.xdat.daos.XdatUserAuthDAO;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

/**
 * Created by mike on 1/19/18.
 */
@Service
public class HibernatePacsRequestService extends AbstractHibernateEntityService<PacsRequest, PacsRequestDAO> implements PacsRequestService {

    private static final Log _log = LogFactory.getLog(HibernatePacsRequestService.class);

    @Inject
    private XdatUserAuthDAO _dao;

}

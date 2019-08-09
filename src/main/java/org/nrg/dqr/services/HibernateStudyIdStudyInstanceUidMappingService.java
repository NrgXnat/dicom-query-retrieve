package org.nrg.dqr.services;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nrg.dqr.daos.QueuedPacsRequestDAO;
import org.nrg.dqr.daos.StudyIdStudyInstanceUidMappingDAO;
import org.nrg.dqr.domain.entities.QueuedPacsRequest;
import org.nrg.dqr.domain.entities.StudyIdStudyInstanceUidMapping;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.nrg.xft.security.UserI;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by mike on 1/19/18.
 */
@Service
public class HibernateStudyIdStudyInstanceUidMappingService extends AbstractHibernateEntityService<StudyIdStudyInstanceUidMapping, StudyIdStudyInstanceUidMappingDAO> implements StudyIdStudyInstanceUidMappingService {

    private static final Log _log = LogFactory.getLog(HibernateStudyIdStudyInstanceUidMappingService.class);

    @Inject
    private StudyIdStudyInstanceUidMappingDAO _dao;

    @Inject
    private NamedParameterJdbcTemplate _parameterized;

    @Override
    @Transactional
    public List<StudyIdStudyInstanceUidMapping> getAllForStudyInstanceUid(String studyInstanceUid){
        return _dao.findAllForStudyInstanceUid(studyInstanceUid);
    }
}

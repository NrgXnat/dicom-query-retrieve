package org.nrg.dqr.services;

import org.nrg.dqr.daos.StudyIdStudyInstanceUidMappingDAO;
import org.nrg.dqr.domain.entities.StudyIdStudyInstanceUidMapping;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Created by mike on 1/19/18.
 */
@Service
public class HibernateStudyIdStudyInstanceUidMappingService extends AbstractHibernateEntityService<StudyIdStudyInstanceUidMapping, StudyIdStudyInstanceUidMappingDAO> implements StudyIdStudyInstanceUidMappingService {
    @Override
    @Transactional
    public List<StudyIdStudyInstanceUidMapping> getAllForStudyInstanceUid(final String studyInstanceUid) {
        return getDao().findAllForStudyInstanceUid(studyInstanceUid);
    }
}

package org.nrg.xnatx.dqr.services.impl.hibernate;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.nrg.xnatx.dqr.domain.daos.StudyIdStudyInstanceUidMappingDAO;
import org.nrg.xnatx.dqr.domain.entities.StudyIdStudyInstanceUidMapping;
import org.nrg.xnatx.dqr.services.StudyIdStudyInstanceUidMappingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Created by mike on 1/19/18.
 */
@Service
@Slf4j
public class HibernateStudyIdStudyInstanceUidMappingService extends AbstractHibernateEntityService<StudyIdStudyInstanceUidMapping, StudyIdStudyInstanceUidMappingDAO> implements StudyIdStudyInstanceUidMappingService {
    @Override
    @Transactional
    public List<StudyIdStudyInstanceUidMapping> getAllForStudyInstanceUid(final String studyInstanceUid) {
        return getDao().findAllForStudyInstanceUid(studyInstanceUid);
    }
}

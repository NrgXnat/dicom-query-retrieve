package org.nrg.dqr.services;

import org.nrg.dqr.domain.entities.StudyIdStudyInstanceUidMapping;
import org.nrg.framework.orm.hibernate.BaseHibernateService;

import java.util.List;

/**
 * Created by mike on 1/19/18.
 */
public interface StudyIdStudyInstanceUidMappingService extends BaseHibernateService<StudyIdStudyInstanceUidMapping> {

    List<StudyIdStudyInstanceUidMapping> getAllForStudyInstanceUid(String studyInstanceUid);
}

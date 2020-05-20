/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.services.impl.hibernate.HibernateStudyIdStudyInstanceUidMappingService
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.services.impl.hibernate;

import lombok.extern.slf4j.Slf4j;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.nrg.xnatx.dqr.domain.daos.StudyIdStudyInstanceUidMappingDAO;
import org.nrg.xnatx.dqr.domain.entities.StudyIdStudyInstanceUidMapping;
import org.nrg.xnatx.dqr.services.StudyIdStudyInstanceUidMappingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Created by mike on 1/19/18.
 */
@Service
@Slf4j
@Transactional
public class HibernateStudyIdStudyInstanceUidMappingService extends AbstractHibernateEntityService<StudyIdStudyInstanceUidMapping, StudyIdStudyInstanceUidMappingDAO> implements StudyIdStudyInstanceUidMappingService {
    @Override
    public List<StudyIdStudyInstanceUidMapping> getAllForStudyInstanceUid(final String studyInstanceUid) {
        return getDao().findAllForStudyInstanceUid(studyInstanceUid);
    }
}

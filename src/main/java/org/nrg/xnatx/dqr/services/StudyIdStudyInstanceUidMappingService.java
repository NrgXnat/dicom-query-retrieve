/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.services.StudyIdStudyInstanceUidMappingService
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.services;

import java.util.List;
import org.nrg.framework.orm.hibernate.BaseHibernateService;
import org.nrg.xnatx.dqr.domain.entities.StudyIdStudyInstanceUidMapping;

/**
 * Created by mike on 1/19/18.
 */
public interface StudyIdStudyInstanceUidMappingService extends BaseHibernateService<StudyIdStudyInstanceUidMapping> {

    List<StudyIdStudyInstanceUidMapping> getAllForStudyInstanceUid(String studyInstanceUid);
}

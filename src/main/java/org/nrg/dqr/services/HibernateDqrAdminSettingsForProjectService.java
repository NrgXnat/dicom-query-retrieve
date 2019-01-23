/*
 * web: org.nrg.xnat.node.services.impl.HibernateXnatNodeInfoService
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2017, Washington University School of Medicine and Howard Hughes Medical Institute
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.dqr.services;

import org.nrg.dqr.daos.DqrAdminSettingsForProjectDAO;
import org.nrg.dqr.domain.entities.DqrAdminSettingsForProject;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.nrg.xnat.entities.ArchiveProcessorInstance;
import org.nrg.xnat.processor.dao.ArchiveProcessorInstanceDAO;
import org.nrg.xnat.processor.services.ArchiveProcessorInstanceService;
import org.nrg.xnat.processor.services.impl.HibernateArchiveProcessorInstanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.util.List;

@Service
public class HibernateDqrAdminSettingsForProjectService extends AbstractHibernateEntityService<DqrAdminSettingsForProject, DqrAdminSettingsForProjectDAO> implements DqrAdminSettingsForProjectService {

//    @Override
//    @Transactional
//    public List<ArchiveProcessorInstance> getAllSiteProcessors(){
//        return _dao.getSiteArchiveProcessors();
//    }
//
//    @Override
//    @Transactional
//    public List<ArchiveProcessorInstance> getAllEnabledSiteProcessors(){
//        return _dao.getEnabledSiteArchiveProcessors();
//    }
//
//    @Override
//    @Transactional
//    public List<ArchiveProcessorInstance> getAllEnabledSiteProcessorsForAe(String aeAndPort){
//        return _dao.getEnabledSiteArchiveProcessorsForAe(aeAndPort);
//    }
//
//    @Override
//    @Transactional
//    public List<ArchiveProcessorInstance> getAllEnabledSiteProcessorsInOrder(){
//        return _dao.getEnabledSiteArchiveProcessorsInOrder();
//    }
//
//    @Override
//    @Transactional
//    public List<ArchiveProcessorInstance> getAllEnabledSiteProcessorsInOrderForLocation(final int location){
//        return _dao.getEnabledSiteArchiveProcessorsInOrderForLocation(location);
//    }
//
//    @Override
//    @Transactional
//    public ArchiveProcessorInstance findSiteProcessorById(final long processorId){
//        return _dao.getSiteArchiveProcessorInstanceByProcessorId(processorId);
//    }

    @Override
    @Transactional
    public DqrAdminSettingsForProject findSettingsByProject(final String projectId){
        return _dao.getDqrAdminSettingsByProjectId(projectId);
    }

    @Override
    @Transactional
    public boolean isDqrEnabledForProject(String projectId) {
        DqrAdminSettingsForProject settings = findSettingsByProject(projectId);
        if(settings!=null && settings.isEnabled()){
            return true;
        }
        else{
            return false;
        }
    }

    @Inject
    private DqrAdminSettingsForProjectDAO _dao;

    /** The Constant _log. */
    private static final Logger _log = LoggerFactory.getLogger(HibernateDqrAdminSettingsForProjectService.class);

}

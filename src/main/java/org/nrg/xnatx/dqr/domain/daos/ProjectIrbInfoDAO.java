/*
 * PacsDAO
 * DQR is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.nrg.xnatx.dqr.domain.daos;

import org.hibernate.criterion.Restrictions;
import org.nrg.xnatx.dqr.domain.entities.ProjectIrbInfo;
import org.nrg.framework.orm.hibernate.AbstractHibernateDAO;
import org.springframework.stereotype.Repository;
import java.util.ArrayList;

@Repository
public class ProjectIrbInfoDAO extends AbstractHibernateDAO<ProjectIrbInfo> {

    public ProjectIrbInfo findIrbInfoForProject(String projectId){
        ArrayList<ProjectIrbInfo> results = (ArrayList<ProjectIrbInfo>) findByCriteria(Restrictions.eq("projectId", projectId));
        if(results==null || results.size()==0){
            return null;
        }
        else{
            return results.get(0);
        }
    }

    public String findIrbNumberForProject(String projectId){
        ArrayList<ProjectIrbInfo> results = (ArrayList<ProjectIrbInfo>) findByCriteria(Restrictions.eq("projectId", projectId));
        if(results==null || results.size()==0){
            return null;
        }
        else{
            return results.get(0).getIrbNumber();
        }
    }

    public byte[] findIrbFileForProject(String projectId){
        ArrayList<ProjectIrbInfo> results = (ArrayList<ProjectIrbInfo>) findByCriteria(Restrictions.eq("projectId", projectId));
        if(results==null || results.size()==0){
            return null;
        }
        else{
            return results.get(0).getIrbFile();
        }
    }

//    public List<Pacs> findAllBut(final Pacs entity) {
//        return findByCriteria(Restrictions.ne("id", entity.getId()));
//    }
//
//    public List<Pacs> findAllStorable() {
//        return findByCriteria(Restrictions.eq("storable", true), Restrictions.eq("enabled", true));
//    }
//
//    public List<Pacs> findAllQueryable() {
//        return findByCriteria(Restrictions.eq("queryable", true), Restrictions.eq("enabled", true));
//    }
//
//    public List<Pacs> findAllQueryableAndStorable() {
//        return findByCriteria(Restrictions.eq("storable", true), Restrictions.eq("queryable", true), Restrictions.eq("enabled", true));
//    }
}

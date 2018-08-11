/*
 * PacsSessionFinder2
 * DQR is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.apache.turbine.modules.screens;

import org.apache.commons.lang.StringUtils;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.dcm.scp.DicomSCPInstance;
import org.nrg.dcm.scp.DicomSCPManager;
import org.nrg.dqr.domain.entities.Pacs;
import org.nrg.dqr.dto.ApplicationEntity;
import org.nrg.dqr.services.PacsEntityService;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.turbine.modules.screens.SecureScreen;
import org.nrg.xdat.turbine.utils.TurbineUtils;

import java.util.*;

public class PacsSessionFinder2 extends SecureScreen {

    @Override
    protected void doBuildTemplate(RunData data, Context context) throws Exception {
        ArrayList<XnatProjectdata> projects = XnatProjectdata.getAllXnatProjectdatas(TurbineUtils.getUser(data), true);
        Comparator<XnatProjectdata> projRunningTitleComparator
                = new Comparator<XnatProjectdata>() {
            public int compare(XnatProjectdata p1, XnatProjectdata p2) {
                String run1 = p1.getSecondaryId().toUpperCase();
                String run2 = p2.getSecondaryId().toUpperCase();
                return run1.compareTo(run2);
            }

        };
        Collections.sort(projects,projRunningTitleComparator);
        context.put("projects", projects);

//        ArrayList<ApplicationEntity> aes = new ArrayList<>();
////        ArrayList<ApplicationEntity> xnatAes = new ArrayList<>();
////        ArrayList<ApplicationEntity> pacsAes = new ArrayList<>();
//        ArrayList<String> addedAeTitles = new ArrayList<>();
////        String defaultAe = "";
//        //Get all enabled and storable PACS
////        final List<Pacs> allPacs = XDAT.getContextService().getBean(PacsEntityService.class).findAllStorable();
////        for (Pacs pacs : allPacs){
////            //Only add receivers that are set to storable to list of XNAT AEs
////            String aeTitle = pacs.getAeTitle();
////            String label = pacs.getLabel();
////            if (!StringUtils.isBlank(aeTitle)) {
////                if (!addedAeTitles.contains(aeTitle)) {
////                    ApplicationEntity ae = new ApplicationEntity();
////                    ae.setAeTitle(aeTitle);
////                    if(!StringUtils.isBlank(label)){
////                        ae.setLabel(label);
////                    }
////                    if (pacs.isDefaultStoragePacs()) {
////                        ae.setIsDefaultStorageDestination(true);
////                    }
////                    aes.add(ae);
////                    pacsAes.add(ae);
////                }
////            }
////        }
//
//        Collection<DicomSCPInstance> scps = XDAT.getContextService().getBean(DicomSCPManager.class).getDicomSCPInstances().values();
//        for (DicomSCPInstance scp : scps){
//            String aeTitle = scp.getAeTitle();
//            if(!StringUtils.isBlank(aeTitle)){
//                ApplicationEntity ae = new ApplicationEntity();
//                ae.setAeTitle(aeTitle);
////                if(!xnatAes.contains(aeTitle)) {
////                    xnatAes.add(ae);
////                }
//                if(!addedAeTitles.contains(aeTitle)){
//                    aes.add(ae);
//                }
//            }
//        }
//
//        Collections.sort(aes);
//        context.put("aes", aes);
//        context.put("xnatAes", xnatAes);
//        context.put("pacsAes", pacsAes);


        ArrayList<String> aesAndPorts = new ArrayList<>();

        Collection<DicomSCPInstance> scps = XDAT.getContextService().getBean(DicomSCPManager.class).getDicomSCPInstances().values();
        for (DicomSCPInstance scp : scps){
            try {
                String aeTitle = scp.getAeTitle();
                int port = scp.getPort();
                String aeAndPort = aeTitle + ":" + port;
                aesAndPorts.add(aeAndPort);
            }
            catch(Exception e){
                log.error("Exception getting information for one of the SCP receivers",e);
            }
        }
        Collections.sort(aesAndPorts);
//        context.put("aes", aesAndPorts);
        context.put("scps", scps);
    }
}

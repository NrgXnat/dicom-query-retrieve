package org.nrg.xnat.actions.postArchive;

import java.util.Map;

import org.apache.log4j.Logger;
import org.nrg.xdat.om.XnatExperimentdata;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.security.UserI;
import org.nrg.xft.utils.SaveItemHelper;

public class TipStatus implements org.nrg.xnat.archive.PrearcSessionArchiver.PostArchiveAction{
    static Logger logger = Logger.getLogger(TipStatus.class);

    @Override
    public Boolean execute(UserI user, XnatImagesessiondata src, Map<String, Object> params) {
        try {
            XnatExperimentdata copy=src.getLightCopy();
            copy.setProperty(src.getXSIType()+"/fields/field[name=activeintip]/field", "true");

            SaveItemHelper.authorizedSave(copy,user, false, false,EventUtils.newEventInstance(EventUtils.CATEGORY.DATA, EventUtils.TYPE.PROCESS, "Configured initial TIP status"));
            return true;
        } catch (Exception e) {
            logger.error("",e);
            return false;
        }
    }

}

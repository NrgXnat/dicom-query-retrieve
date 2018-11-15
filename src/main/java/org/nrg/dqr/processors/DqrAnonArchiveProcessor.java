package org.nrg.dqr.processors;

import lombok.extern.slf4j.Slf4j;
import org.dcm4che2.data.DicomObject;
import org.nrg.action.ServerException;
import org.nrg.config.entities.Configuration;
import org.nrg.dicom.mizer.service.MizerService;
import org.nrg.dqr.domain.entities.DqrAdminSettingsForProject;
import org.nrg.dqr.services.DqrAdminSettingsForProjectService;
import org.nrg.xnat.entities.ArchiveProcessorInstance;
import org.nrg.xnat.helpers.merge.anonymize.DefaultAnonUtils;
import org.nrg.xnat.helpers.prearchive.SessionData;
import org.restlet.data.Status;
import org.nrg.xdat.XDAT;
import org.springframework.stereotype.Component;
import org.nrg.xnat.processors.AbstractArchiveProcessor;

import java.util.Map;

@Component
@Slf4j
public class DqrAnonArchiveProcessor extends AbstractArchiveProcessor {

    @Override
    public boolean process(final DicomObject dicomData, final SessionData sessionData, final MizerService mizer, ArchiveProcessorInstance instance, Map<String, Object> aeParameters) throws ServerException{
        try {
            String projId = "";
            String subj = "";
            String folder = "";
            if(sessionData!=null){
                projId = sessionData.getProject();
                subj = sessionData.getSubject();
                folder = sessionData.getFolderName();
            }
            DqrAdminSettingsForProject settings = XDAT.getContextService().getBean(DqrAdminSettingsForProjectService.class).findSettingsByProject(projId);
            if(settings!=null && settings.isDqrAnonEnabled()){
                mizer.anonymize(dicomData, projId, subj, folder, settings.getDqrAnonScript());
            }
            else {
                log.debug("DQR Anonymization is not enabled, allowing session {} {} {} to proceed without additional anonymization.", projId, subj, sessionData.getName());
            }

        } catch (Throwable e) {
            log.debug("DQR Dicom anonymization failed: " + dicomData, e);
            throw new ServerException(Status.SERVER_ERROR_INTERNAL,e);
        }
        return true;
    }
}

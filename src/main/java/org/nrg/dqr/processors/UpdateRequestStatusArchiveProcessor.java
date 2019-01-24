package org.nrg.dqr.processors;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.nrg.action.ServerException;
import org.nrg.dicom.mizer.service.MizerService;
import org.nrg.dqr.domain.entities.ExecutedPacsRequest;
import org.nrg.dqr.domain.entities.PacsRequest;
import org.nrg.dqr.services.ExecutedPacsRequestService;
import org.nrg.xdat.XDAT;
import org.nrg.xnat.entities.ArchiveProcessorInstance;
import org.nrg.xnat.helpers.merge.anonymize.DefaultAnonUtils;
import org.nrg.xnat.helpers.prearchive.SessionData;
import org.nrg.xnat.processors.AbstractArchiveProcessor;
import org.restlet.data.Status;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;

@Component
@Slf4j
public class UpdateRequestStatusArchiveProcessor extends AbstractArchiveProcessor {

    @Override
    public boolean process(final DicomObject dicomData, final SessionData sessionData, final MizerService mizer, ArchiveProcessorInstance instance, Map<String, Object> aeParameters) throws ServerException{
        try {
            final String studyInstanceUID = dicomData.getString(Tag.StudyInstanceUID);
            ExecutedPacsRequestService executedService = XDAT.getContextService().getBean(ExecutedPacsRequestService.class);
            ExecutedPacsRequest mostRecentRequest = executedService.getMostRecentForStudyInstanceUid(studyInstanceUID);
            if(mostRecentRequest!=null) {
                Date executedTime = mostRecentRequest.getExecutedTime();
                Date currTime = new Date();
                if (executedTime != null) {
                    if ((currTime.getTime() - executedTime.getTime()) < (3600000)) {
                        //The most recent request for that study UID was in the last hour
                        if (StringUtils.equals(mostRecentRequest.getStatus(), PacsRequest.ISSUED_STATUS_TEXT)) {
                            mostRecentRequest.setStatus(PacsRequest.RECEIVED_STATUS_TEXT);
                            executedService.update(mostRecentRequest);
                        }
                    }
                }
            }
        } catch (Throwable e) {
            log.debug("Error updating request status: " + dicomData, e);
            //throw new ServerException(Status.SERVER_ERROR_INTERNAL,e);
            //Don't throw exception because we should not block import just because request status couldn't be updated.
        }
        return true;
    }
}

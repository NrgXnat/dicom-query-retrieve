/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.processors.UpdateRequestStatusArchiveProcessor
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.processors;

import java.util.Date;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.nrg.dicom.mizer.service.MizerService;
import org.nrg.xnat.entities.ArchiveProcessorInstance;
import org.nrg.xnat.helpers.prearchive.SessionData;
import org.nrg.xnat.processors.AbstractArchiveProcessor;
import org.nrg.xnatx.dqr.domain.entities.ExecutedPacsRequest;
import org.nrg.xnatx.dqr.domain.entities.PacsRequest;
import org.nrg.xnatx.dqr.services.ExecutedPacsRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class UpdateRequestStatusArchiveProcessor extends AbstractArchiveProcessor {
    @Autowired
    public UpdateRequestStatusArchiveProcessor(final ExecutedPacsRequestService service) {
        _service = service;
    }

    @Override
    public boolean process(final DicomObject dicomData, final SessionData sessionData, final MizerService mizer, final ArchiveProcessorInstance instance, final Map<String, Object> aeParameters) {
        try {
            final ExecutedPacsRequest mostRecentRequest = _service.getMostRecentForStudyInstanceUid(dicomData.getString(Tag.StudyInstanceUID));
            if (mostRecentRequest != null) {
                final Date executedTime = mostRecentRequest.getExecutedTime();
                if (executedTime != null) {
                    if ((new Date().getTime() - executedTime.getTime()) < (3600000)) {
                        //The most recent request for that study UID was in the last hour
                        if (StringUtils.equals(mostRecentRequest.getStatus(), PacsRequest.ISSUED_STATUS_TEXT)) {
                            mostRecentRequest.setStatus(PacsRequest.RECEIVED_STATUS_TEXT);
                            _service.update(mostRecentRequest);
                        }
                    }
                }
            }
        } catch (Throwable e) {
            log.debug("Error updating request status: {}", dicomData, e);
            //throw new ServerException(Status.SERVER_ERROR_INTERNAL,e);
            //Don't throw exception because we should not block import just because request status couldn't be updated.
        }
        return true;
    }

    private final ExecutedPacsRequestService _service;
}

/**
 * Copyright (c) 2023 Flywheel
 */
package org.nrg.xnat.actions.postArchive;

import lombok.extern.slf4j.Slf4j;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.model.XnatAbstractresourceI;
import org.nrg.xdat.model.XnatImagescandataI;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.archive.PrearcSessionArchiver;
import org.nrg.xnatx.dqr.domain.entities.SeriesRetrievalStatus;
import org.nrg.xnatx.dqr.services.SeriesRetrievalStatusService;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Slf4j
public class UpdateSeriesStatus implements PrearcSessionArchiver.PostArchiveAction {
    private static final SeriesRetrievalStatusService seriesRetrievalStatusService = XDAT.getContextService()
            .getBeanSafely(SeriesRetrievalStatusService.class);

    /* Don't update in the calling thread, but a new thread per request should be fine */
    private static final Executor executor = new Executor() {
        public void execute(Runnable r) {
            new Thread(r).start();
        }
    };

    @Override
    /**
     * Update any study retrieval statistics associated with the newly archived study.
     */
    public Boolean execute(final UserI user, final XnatImagesessiondata session, final Map<String, Object> params) {
        final String studyInstanceUid = session.getUid();
        final String project = session.getProject();
        log.debug("updating series retrieval records for archived study {}", project, session.getLabel());

        final Runnable update = () -> {
            final Map<String,SeriesRetrievalStatus> seriesStatus = seriesRetrievalStatusService.findByStudyProject(
                    studyInstanceUid, project
            )
                    .stream()
                    .collect(Collectors.toMap(SeriesRetrievalStatus::getSeriesInstanceUid, s -> s));

            if (seriesStatus.isEmpty()) {
                return;
            }

            for (final XnatImagescandataI scan : session.getSortedScans()) {
                final String seriesInstanceUid = scan.getUid();
                int nFiles = 0;
                long dataSize = 0;
                RuntimeException err = null;    // report errors only if they're for a series we're tracking
                for (final XnatAbstractresourceI resource : scan.getFile()) {
                    if ("DICOM".equals(resource.getLabel())) {
                        try {
                            nFiles += resource.getFileCount();
                            dataSize += Long.valueOf(resource.getFileSize().toString());
                        } catch (RuntimeException e) {
                            err = e;
                        }
                    }
                }
                final SeriesRetrievalStatus status = seriesStatus.get(seriesInstanceUid);
                if (null != status) {
                    if (null == err) {
                        try {
                            log.debug("updating series retrieval record for {}: {} files, {} bytes",
                                    status, nFiles, dataSize);
                            seriesRetrievalStatusService.updateRetrievalStatistics(status, nFiles, dataSize);
                        } catch (Throwable t) {
                            log.error("{}:{} series {} retrieval status update failed",
                                    project, session.getLabel(), scan.getId(), t);
                        }
                    } else {
                        log.error("{}:{} series {} retrieval status update failed",
                                project, session.getLabel(), scan.getId(), err);
                    }
                 }
            }
        };
        executor.execute(update);
        return true;
    }
}

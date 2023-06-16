/**
 * Copyright (c) 2023 Flywheel
 */
package org.nrg.xnat.actions.postArchive;

import lombok.extern.slf4j.Slf4j;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.model.XnatImagescandataI;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.archive.PrearcSessionArchiver;
import org.nrg.xnatx.dqr.services.ArchivedRequestedSeriesService;
import org.nrg.xnatx.dqr.services.SeriesRetrievalRequestService;

import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Each time a study is archived, check the series retrieval requests to see if the newly archived
 * study contains series that have been requested. If so, add an archived requested series record
 * describing the current state of each series.
 */
@Slf4j
public class RecordArchivedSeries implements PrearcSessionArchiver.PostArchiveAction {
    private static final ArchivedRequestedSeriesService archivedRequestedSeriesService = XDAT.getContextService()
        .getBeanSafely(ArchivedRequestedSeriesService.class);
    private static final SeriesRetrievalRequestService seriesRetrievalRequestService = XDAT.getContextService()
        .getBeanSafely(SeriesRetrievalRequestService.class);

    /* Don't do heavy lifting in the calling thread, but a new thread per request should be fine */
    private static final Executor executor = r -> new Thread(r).start();

    @Override
    /*
    When a study is archived, if any contained series have been requested from a PACS, add archived series entries.
    */
    public Boolean execute(final UserI user, final XnatImagesessiondata session, final Map<String, Object> params) {
        final String project = session.getProject();
        log.debug("updating series retrieval records for archived study {}:{}", project, session.getLabel());

        final Runnable create = () -> {
            /* FIXME: as implemented, this adds an ArchivedRequestedSeries for all series in every study that
             * has had at least one series requested. This means some of the inserted records are for series
             * that haven't actually been requested. This is probably harmless.
             */
            if (seriesRetrievalRequestService.hasBeenRequested(session, null)) {
                for (final XnatImagescandataI scan : session.getSortedScans()) {
                    archivedRequestedSeriesService.createIfDicom(session.getUid(), scan);
                }
            }
        };
        executor.execute(create);
        return true;
    }
}

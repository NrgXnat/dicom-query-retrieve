package org.nrg.xnatx.dqr.services;

import org.nrg.xnatx.dqr.domain.entities.ExecutedPacsRequest;

/**
 * Created by mike on 1/19/18.
 */
public interface ExecutedPacsRequestService extends BasePacsRequestService<ExecutedPacsRequest> {
    ExecutedPacsRequest getMostRecentForPacs(final long pacsId);

    ExecutedPacsRequest getMostRecentForStudyInstanceUid(final String studyInstanceUid);
}

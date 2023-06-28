package org.nrg.xnatx.dqr.services;

import org.dcm4che2.data.DicomObject;
import org.nrg.framework.orm.hibernate.BaseHibernateService;
import org.nrg.xdat.model.XnatImagesessiondataI;
import org.nrg.xft.security.UserI;
import org.nrg.xnatx.dqr.domain.RequestContext;
import org.nrg.xnatx.dqr.domain.entities.ArchivedRequestedSeries;
import org.nrg.xnatx.dqr.domain.entities.PaginatedPacsRequest;
import org.nrg.xnatx.dqr.domain.entities.SeriesRetrievalRequest;

import javax.annotation.Nullable;
import java.util.List;

public interface SeriesRetrievalRequestService extends BaseHibernateService<SeriesRetrievalRequest> {
    RequestContext makeCMoveContext(UserI user, @Nullable String userDefinedId);

    /**
     * Returns all stored series retrieval requests for the provided series identifiers and requesting user.
     * @param series record of an archived series
     * @param username requesting user; if null, returns requests for all users
     * @return List of series retrieval requests
     */
    @Deprecated
    List<SeriesRetrievalRequest> findForArchivedSeries(ArchivedRequestedSeries series, @Nullable String username);

    /**
     * From the provided C-FIND results, store a series retrieval request for the provided
     * requesting user and into the named project.
     * @param user user making the request
     * @param destinationProject XNAT project to which series should be downloaded
     * @param userDefinedId optional label for later searching
     * @param cfindResult series representation returned from C-FIND
     * @throws IllegalArgumentException if the C-FIND result does not contain the minimal values needed
     * to identify a request: Study and Series Instance UIDs
     */
    SeriesRetrievalRequest createFromCFindResult(
            UserI user, String destinationProject, @Nullable String userDefinedId, DicomObject cfindResult
    );

    /**
     * Has any scan/series in the provided XNAT archived session/study been requested by the named user?
     * @param session XNAT archived session
     * @param user requesting user; if null, ask for any user
     * @return true if the provided session has been requested
     */
    boolean hasBeenRequested(XnatImagesessiondataI session, @Nullable UserI user);

    /**
     * Find series retrieval requests for the named user, most recent first.
     * @param user requesting user; if null, return requests for all users.
     * @param userDefinedId user-defined ID; if provided, return only matching requests
     * @param request pagination parameters
     * @return List of series retrieval requests
     */
    List<SeriesRetrievalRequest> findReverseChronological(@Nullable UserI user,
                                                          @Nullable String userDefinedId,
                                                          PaginatedPacsRequest request);
}

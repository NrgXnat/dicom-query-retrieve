package org.nrg.xnatx.dqr.domain;

import org.dcm4che2.data.DicomObject;
import org.nrg.xft.security.UserI;
import org.nrg.xnatx.dqr.services.SeriesRetrievalRequestService;

import javax.annotation.Nullable;

public class CMoveRequestContext implements DimseRequestContext {
    private final SeriesRetrievalRequestService service;
    private final UserI user;
    private final @Nullable String userDefinedId;

    public CMoveRequestContext(SeriesRetrievalRequestService service, final UserI user, final @Nullable String userDefinedId) {
        this.service = service;
        this.user = user;
        this.userDefinedId = userDefinedId;
    }

    @Override
    public void recordRequest(String xnatProjectName, DicomObject requestParameters) {
        service.createFromCFindResult(user, xnatProjectName, userDefinedId, requestParameters);
    }
}

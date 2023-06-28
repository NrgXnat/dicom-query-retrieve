package org.nrg.xnatx.dqr.domain;

import org.dcm4che2.data.DicomObject;

public interface RequestContext {
    void recordRequest(String xnatProjectName, DicomObject requestParameters);
}

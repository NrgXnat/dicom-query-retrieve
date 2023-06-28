package org.nrg.xnatx.dqr.domain;

import org.dcm4che2.data.DicomObject;

public interface DimseRequestContext {
    void recordRequest(String xnatProjectName, DicomObject requestParameters);
}

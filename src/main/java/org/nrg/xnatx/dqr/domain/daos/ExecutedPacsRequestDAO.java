/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.domain.daos.ExecutedPacsRequestDAO
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.domain.daos;

import java.util.List;
import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.nrg.xnatx.dqr.domain.entities.ExecutedPacsRequest;
import org.springframework.stereotype.Repository;

/**
 * Created by mike on 1/19/18.
 */
@Repository
public class ExecutedPacsRequestDAO extends AbstractPacsRequestDAO<ExecutedPacsRequest> {
    @Override
    protected String getTimeSortProperty() {
        return "executedTime";
    }
}

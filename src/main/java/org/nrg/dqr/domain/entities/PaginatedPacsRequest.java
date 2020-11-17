/*
 * dicom-query-retrieve: org.nrg.dqr.domain.entities.PaginatedPacsRequest
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.dqr.domain.entities;

import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.nrg.framework.ajax.hibernate.HibernatePaginatedRequest;

@NoArgsConstructor
@SuperBuilder(toBuilder = true)
public class PaginatedPacsRequest extends HibernatePaginatedRequest {
    @Override
    protected String getDefaultSortColumn() {
        return "id";
    }
}

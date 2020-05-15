/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.utils.CsvRow
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.utils;

import java.util.Collection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.nrg.xnatx.dqr.domain.Study;
import org.nrg.xnatx.dqr.dto.PacsSearchCriteria;

@Getter
@Accessors(prefix = "_")
@Builder
@AllArgsConstructor
public class CsvRow {
    private final PacsSearchCriteria _criteria;
    private final String             _anonScript;
    private final Collection<Study>  _studies;
}

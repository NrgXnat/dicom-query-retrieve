/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.dicom.converters.DqrDateRangeSerializer
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.dicom.converters;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.nrg.xnatx.dqr.utils.DqrDateRange;

import java.io.IOException;

public class DqrDateRangeSerializer extends AbstractDqrSerializer<DqrDateRange> {
    public DqrDateRangeSerializer() {
        super(DqrDateRange.class);
    }

    @Override
    protected void serializeImpl(final DqrDateRange value, final JsonGenerator generator, final SerializerProvider provider) throws IOException {
        writeNonNullDateField(generator, "start", value.getStart());
        writeNonNullDateField(generator, "end", value.getEnd());
    }
}

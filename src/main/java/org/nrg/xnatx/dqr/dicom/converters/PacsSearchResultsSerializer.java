/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.dicom.converters.PacsSearchResultsSerializer
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.dicom.converters;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.type.MapLikeType;
import com.fasterxml.jackson.databind.type.SimpleType;
import org.nrg.xnatx.dqr.dto.PacsSearchResults;

import java.io.IOException;

public class PacsSearchResultsSerializer extends AbstractDqrSerializer<PacsSearchResults<?>> {
    public PacsSearchResultsSerializer() {
        super(MapLikeType.construct(PacsSearchResults.class, SimpleType.construct(Object.class), SimpleType.construct(Object.class)));
    }

    @Override
    protected void serializeImpl(final PacsSearchResults<?> value, final JsonGenerator generator, final SerializerProvider provider) throws IOException {
        generator.writeBooleanField("hasLimitedResultSetSize", value.isHasLimitedResultSetSize());
        generator.writeObjectField("studyDateRangeLimitResults", value.getStudyDateRangeLimitResults());
        if (value.getResults() != null && !value.getResults().isEmpty()) {
            generator.writeArrayFieldStart("results");
            for (final Object result : value.getResults()) {
                generator.writeObject(result);
            }
            generator.writeEndArray();
        }
    }
}

/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.dicom.converters.DqrDateRangeDeSerializer
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.dicom.converters;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import java.io.IOException;
import java.util.Date;
import org.nrg.xnatx.dqr.utils.DqrDateRange;

public class DqrDateRangeDeSerializer extends StdDeserializer<DqrDateRange> {
    public DqrDateRangeDeSerializer() {
        super(DqrDateRange.class);
    }

    @Override
    public DqrDateRange deserialize(final JsonParser parser, final DeserializationContext context) throws IOException {
        if (parser.getCurrentToken() != JsonToken.START_OBJECT) {
            throw new IOException("invalid start marker");
        }

        final DqrDateRange.DqrDateRangeBuilder builder = DqrDateRange.builder();
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            final String field = parser.getCurrentName();
            parser.nextToken();
            final String text = parser.getText();
            final Date   date = DqrDateRange.parse(text);
            if (date == null) {
                throw new InvalidFormatException("Unable to parse value for \"" + field + "\"", text, Date.class);
            }
            switch (field) {
                case "start":
                    builder.start(date);
                    break;
                case "end":
                    builder.end(date);
                    break;
            }
        }
        return builder.build();
    }
}

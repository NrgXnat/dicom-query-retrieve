/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.dicom.converters.PacsSearchCriteriaDeserializer
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
import org.apache.commons.lang3.ObjectUtils;
import org.nrg.xnatx.dqr.dto.PacsSearchCriteria;
import org.nrg.xnatx.dqr.utils.DqrDateRange;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Date;

public class PacsSearchCriteriaDeserializer extends StdDeserializer<PacsSearchCriteria> {
    private static final long serialVersionUID = 3526449322405862791L;

    public PacsSearchCriteriaDeserializer() {
        super(PacsSearchCriteria.class);
    }

    @Override
    public PacsSearchCriteria deserialize(final JsonParser parser, final DeserializationContext context) throws IOException {
        if (parser.getCurrentToken() != JsonToken.START_OBJECT) {
            throw new IOException("invalid start marker");
        }

        final PacsSearchCriteria.PacsSearchCriteriaBuilder builder    = PacsSearchCriteria.builder();
        LocalDateTime                                      startRange = null;
        LocalDateTime                                      endRange   = null;
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            final String field = parser.getCurrentName();
            parser.nextToken();
            switch (field) {
                case "pacsId":
                    builder.pacsId(Long.parseLong(parser.getText()));
                    break;
                case "patientId":
                    builder.patientId(parser.getText());
                    break;
                case "patientName":
                    builder.patientName(parser.getText());
                    break;
                case "accessionNumber":
                    builder.accessionNumber(parser.getText());
                    break;
                case "studyInstanceUid":
                    builder.studyInstanceUid(parser.getText());
                    break;
                case "studyId":
                    builder.studyId(parser.getText());
                    break;
                case "seriesInstanceUid":
                    builder.seriesInstanceUid(parser.getText());
                    break;
                case "seriesDescription":
                    builder.seriesDescription(parser.getText());
                    break;
                case "seriesNumber":
                    builder.seriesNumber(Integer.parseInt(parser.getText()));
                    break;
                case "modality":
                    builder.modality(parser.getText());
                    break;
                case "dob":
                    builder.dob(parser.getText());
                    break;
                case "studyDateRange":
                    builder.studyDateRange(context.readValue(parser, DqrDateRange.class));
                    break;
                case "studyDateIsToday":
                    if (ObjectUtils.anyNotNull(startRange, endRange)) {
                        throw new InvalidFormatException("Value specified for \"studyDateIsToday\" but either \"studyDateFrom\", \"studyDateTo\", or both were also specified.", "studyDateIsToday", Date.class);
                    }
                    startRange = endRange = LocalDateTime.now();
                    break;
                case "studyDateFrom":
                    final String startText = parser.getText();
                    startRange = DqrDateRange.parse(startText);
                    if (startRange == null) {
                        throw new InvalidFormatException("Unable to parse value for \"studyDateFrom\"", startText, Date.class);
                    }
                    break;
                case "studyDateTo":
                    final String endText = parser.getText();
                    endRange = DqrDateRange.parse(endText);
                    if (endRange == null) {
                        throw new InvalidFormatException("Unable to parse value for \"studyDateTo\"", endText, Date.class);
                    }
                    break;
            }
        }
        if (ObjectUtils.anyNotNull(startRange, endRange)) {
            final DqrDateRange.DqrDateRangeBuilder dateRange = DqrDateRange.builder();
            if (startRange != null) {
                dateRange.start(startRange);
            }
            if (endRange != null) {
                dateRange.end(endRange);
            }
            builder.studyDateRange(dateRange.build());
        }
        return builder.build();
    }
}

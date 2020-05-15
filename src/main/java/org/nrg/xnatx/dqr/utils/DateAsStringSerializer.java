/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.utils.DateAsStringSerializer
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.utils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.ser.std.DateTimeSerializerBase;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;

@JacksonStdImpl
@Slf4j
public class DateAsStringSerializer extends DateTimeSerializerBase<Date> {
    @SuppressWarnings("unused")
    public DateAsStringSerializer() {
        this(null, null);
    }

    public DateAsStringSerializer(final Boolean useTimestamp, final DateFormat customFormat) {
        super(Date.class, useTimestamp, customFormat);
    }

    public DateAsStringSerializer withFormat(final Boolean timestamp, final DateFormat customFormat) {
        return new DateAsStringSerializer(timestamp, customFormat);
    }

    public void serialize(final Date value, final JsonGenerator generator, final SerializerProvider provider) throws IOException {
        generator.writeString(DqrDateRange.format(value));
    }

    @Override
    protected long _timestamp(final Date value) {
        return value == null ? 0L : value.getTime();
    }
}

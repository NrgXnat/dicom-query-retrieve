package org.nrg.xnat.utils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.ser.std.DateTimeSerializerBase;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

@JacksonStdImpl
public class DateAsStringSerializer extends DateTimeSerializerBase<Date> {
    public static final DateAsStringSerializer instance = new DateAsStringSerializer();

    public DateAsStringSerializer() {
        this(null, null);
    }

    public DateAsStringSerializer(Boolean useTimestamp, DateFormat customFormat) {
        super(Date.class, useTimestamp, customFormat);
    }

    public DateAsStringSerializer withFormat(Boolean timestamp, DateFormat customFormat) {
        return new DateAsStringSerializer(timestamp, customFormat);
    }

    protected long _timestamp(Date value) {
        return value == null ? 0L : value.getTime();
    }

    public void serialize(Date value, JsonGenerator gen, SerializerProvider provider) throws IOException {
//        if(this._asTimestamp(provider)) {
//            gen.writeString(""+this._timestamp(value));
//        } else if(myDateFormat != null) {
//            DateFormat var4 = myDateFormat;
        synchronized (DATE_FORMAT) {
            gen.writeString(DATE_FORMAT.format(value));
        }
//        } else {
//            provider.defaultSerializeDateValue(value, gen);
//        }

    }

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");

    static {
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT"));
    }
}
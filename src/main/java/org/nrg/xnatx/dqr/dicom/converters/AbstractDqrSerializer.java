/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.dicom.converters.AbstractDqrSerializer
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.dicom.converters;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.apache.commons.lang3.StringUtils;
import org.nrg.xnatx.dqr.domain.Study;
import org.nrg.xnatx.dqr.utils.DqrDateRange;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Date;

public abstract class AbstractDqrSerializer<T> extends StdSerializer<T> {
    private static final long serialVersionUID = 3634214101922651894L;

    protected AbstractDqrSerializer(final Class<T> typeToSerialize) {
        super(typeToSerialize);
    }

    protected AbstractDqrSerializer(final JavaType typeToSerialize) {
        super(typeToSerialize);
    }

    protected abstract void serializeImpl(final T value, final JsonGenerator generator, final SerializerProvider provider) throws IOException;

    @Override
    public void serialize(final T value, final JsonGenerator generator, final SerializerProvider provider) throws IOException {
        generator.writeStartObject();
        serializeImpl(value, generator, provider);
        generator.writeEndObject();
    }

    protected void writeNonBlankStringField(final JsonGenerator generator, final String name, final String value) throws IOException {
        if (StringUtils.isNotBlank(value)) {
            generator.writeStringField(name, value);
        }
    }

    protected void writeNonNullDateField(final JsonGenerator generator, final String name, final Date value) throws IOException {
        if (value != null) {
            generator.writeStringField(name, DqrDateRange.formatDate(value));
        }
    }

    protected void writeNonNullDateField(final JsonGenerator generator, final String name, final LocalDateTime value) throws IOException {
        if (value != null) {
            generator.writeStringField(name, DqrDateRange.formatDate(value));
        }
    }

    protected void writeNonEmptyListStringField(final JsonGenerator generator, final String name, final Collection<String> value) throws IOException {
        if (value != null && !value.isEmpty()) {
            generator.writeArrayFieldStart(name);
            for (final String item : value) {
                generator.writeString(item);
            }
            generator.writeEndArray();
        }
    }

    protected void writeBaseStudyAttributes(final JsonGenerator generator, final Study study) throws IOException {
        writeNonBlankStringField(generator, "studyInstanceUid", study.getStudyInstanceUid());
        writeNonBlankStringField(generator, "studyId", study.getStudyId());
        writeNonBlankStringField(generator, "studyDescription", study.getStudyDescription());
        writeNonBlankStringField(generator, "accessionNumber", study.getAccessionNumber());
        writeNonNullDateField(generator, "studyDate", study.getStudyDate());
        writeNonEmptyListStringField(generator, "modalitiesInStudy", study.getModalitiesInStudy());
        if (study.getReferringPhysicianName() != null) {
            final String value = study.getReferringPhysicianName().toString();
            if (StringUtils.isNotBlank(value)) {
                generator.writeStringField("referringPhysicianName", value);
            }
        }
    }
}

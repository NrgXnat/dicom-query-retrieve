/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.dicom.converters.PatientSerializer
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.dicom.converters;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.nrg.xnatx.dqr.domain.Patient;
import org.nrg.xnatx.dqr.domain.Study;

import java.io.IOException;

public class PatientSerializer extends AbstractDqrSerializer<Patient> {
    public PatientSerializer() {
        super(Patient.class);
    }

    @Override
    protected void serializeImpl(final Patient value, final JsonGenerator generator, final SerializerProvider provider) throws IOException {
        writeNonBlankStringField(generator, "id", value.getId());
        if (value.getName() != null) {
            generator.writeStringField("name", value.getName().toString());
        }
        writeNonBlankStringField(generator, "sex", value.getSex());
        writeNonNullDateField(generator, "birthDate", value.getBirthDate());
        if (value.getStudies() != null && !value.getStudies().isEmpty()) {
            generator.writeArrayFieldStart("studies");
            for (final Study study : value.getStudies()) {
                generator.writeStartObject();
                writeBaseStudyAttributes(generator, study);
                generator.writeEndObject();
            }
            generator.writeEndArray();
        }
    }
}

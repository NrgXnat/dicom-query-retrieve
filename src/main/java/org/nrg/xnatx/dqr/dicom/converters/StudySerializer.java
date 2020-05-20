/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.dicom.converters.StudySerializer
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
import java.util.stream.Collectors;

public class StudySerializer extends AbstractDqrSerializer<Study> {
    public StudySerializer() {
        super(Study.class);
    }

    @Override
    protected void serializeImpl(final Study value, final JsonGenerator generator, final SerializerProvider provider) throws IOException {
        writeBaseStudyAttributes(generator, value);
        if (value.getPatient() != null) {
            generator.writeObjectFieldStart("patient");
            final Patient patient = value.getPatient();
            writeNonBlankStringField(generator, "id", patient.getId());
            if (patient.getName() != null) {
                generator.writeStringField("name", patient.getName().getLastNameCommaFirstName());
            }
            writeNonBlankStringField(generator, "sex", patient.getSex());
            if (patient.getStudies() != null && !patient.getStudies().isEmpty()) {
                generator.writeArrayFieldStart("studies");
                for (final String studyInstanceUid : patient.getStudies().stream().map(Study::getStudyInstanceUid).collect(Collectors.toList())) {
                    generator.writeString(studyInstanceUid);
                }
                generator.writeEndArray();
            }
            generator.writeEndObject();
        }
    }
}

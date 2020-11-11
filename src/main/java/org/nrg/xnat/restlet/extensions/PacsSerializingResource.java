/*
 * dicom-query-retrieve: org.nrg.xnat.restlet.extensions.PacsSerializingResource
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnat.restlet.extensions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.ser.DefaultSerializerProvider;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.nrg.dqr.restlet.NullValueSerializer;
import org.nrg.xnat.restlet.resources.SecureResource;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;

@Getter(AccessLevel.PROTECTED)
@Accessors(prefix = "_")
public abstract class PacsSerializingResource extends SecureResource {
    protected PacsSerializingResource(final Context context, final Request request, final Response response) {
        super(context, request, response);

        _objectMapper = new ObjectMapper();

        final DefaultSerializerProvider provider = new DefaultSerializerProvider.Impl();
        provider.setNullValueSerializer(new NullValueSerializer());
        _objectMapper.setSerializerProvider(provider);
    }

    protected String writeValue(final Object value) throws JsonProcessingException {
        return writeValue(value, null);
    }

    protected String writeValue(final Object value, final Class<?> serializationView) throws JsonProcessingException {
        final ObjectWriter writer = serializationView == null ? getObjectMapper().writer() : getObjectMapper().writerWithView(serializationView);
        return writer.writeValueAsString(value);
    }

    private final ObjectMapper _objectMapper;
}

/*
 * org.nrg.dqr.restlet.NullValueSerializer
 * DQR is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.nrg.dqr.restlet;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class NullValueSerializer extends JsonSerializer<Object> {

    @Override
    public void serialize(final Object entity, final JsonGenerator generator, final SerializerProvider provider) throws IOException {
        try {
            generator.writeString("");
        } catch (JsonProcessingException e) {
            _log.error("Ran into a problem generating JSON", e);
            throw e;
        } catch (IOException e) {
            _log.error("Got an I/O exception somehow", e);
            throw e;
        }
    }

    private static final Logger _log = LoggerFactory.getLogger(NullValueSerializer.class);
}

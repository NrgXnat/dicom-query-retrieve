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
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class NullValueSerializer extends JsonSerializer<Object> {
    @Override
    public void serialize(final Object entity, final JsonGenerator generator, final SerializerProvider provider) throws IOException {
        try {
            generator.writeString("");
        } catch (JsonProcessingException e) {
            log.error("Ran into a problem generating JSON", e);
            throw e;
        } catch (IOException e) {
            log.error("Got an I/O exception somehow", e);
            throw e;
        }
    }
}

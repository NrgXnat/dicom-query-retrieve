package org.nrg.xnatx.dqr.dicom.json.event;

import jakarta.json.JsonException;
import jakarta.json.stream.JsonLocation;
import jakarta.json.stream.JsonParser;
import java.math.BigDecimal;

/**
 * Replayable event representing an exception from the source parser.
 * It is expected that this would be replayed during the hasNext()
 * but all methods of this event throw the exception.
 */
final class ReplayableJsonException implements ReplayableJsonEvent {
    private final JsonException exception;

    ReplayableJsonException(final JsonException exception) {
        this.exception = exception;
    }

    @Override
    public JsonParser.Event getEvent() {
        throw exception;
    }

    @Override
    public String getString() {
        throw exception;
    }

    @Override
    public boolean isIntegralNumber() {
        throw exception;
    }

    @Override
    public int getInt() {
        throw exception;
    }

    @Override
    public long getLong() {
        throw exception;
    }

    @Override
    public BigDecimal getBigDecimal() {
        throw exception;
    }

    @Override
    public JsonLocation getLocation() {
        throw exception;
    }

    @Override
    public boolean isException() { return true; }
}

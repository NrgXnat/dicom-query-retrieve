package org.nrg.xnatx.dqr.dicom.json.event;

import jakarta.json.JsonException;
import jakarta.json.stream.JsonLocation;
import jakarta.json.stream.JsonParser;
import java.math.BigDecimal;
import java.util.Optional;

/**
 * Record of one JsonParser event and its content, playable to a consumer
 * (by acting as a delegate for the methods the consumer would normally
 * call in JsonParser).
 * All methods match similarly-named methods in JsonParser, except for isException.
 */
public interface ReplayableJsonEvent {
    JsonParser.Event getEvent();

    String getString();

    boolean isIntegralNumber();

    int getInt();

    long getLong();

    BigDecimal getBigDecimal();

    JsonLocation getLocation();

    /**
     * Does this record capture a JsonException?
     * @return true if a JsonException occurred during capture.
     */
    boolean isException();

    /**
     * Construct a replayable event record for the current JsonParser state
     * @param event the current Event
     * @param parser The JSON parser
     * @return event of a suitable type
     */
    static ReplayableJsonEvent forCurrent(final JsonParser.Event event, final JsonParser parser) {
        try {
            switch (event) {
                case START_OBJECT:
                case END_OBJECT:
                case START_ARRAY:
                case END_ARRAY:
                case VALUE_NULL:
                case VALUE_FALSE:
                case VALUE_TRUE:
                    return new NoValueEvent(parser.getLocation(), event);

                case KEY_NAME:
                case VALUE_STRING:
                    return new StringValueEvent(parser.getLocation(), event, parser.getString());

                case VALUE_NUMBER:
                    return new NumberValueEvent(parser);

                default:
                    throw new IllegalArgumentException("unknown JsonParser Event " + event);
            }
        } catch (JsonException e) {
            return new ReplayableJsonException(e);
        }
    }

    /**
     * Get the replayable event record for the next parser state, if any
     * @param parser The JSON parser
     * @return wrapped next event, or empty if there is no next event
     */
    static Optional<ReplayableJsonEvent> next(final JsonParser parser) {
        if (parser.hasNext()) {
            try {
                final JsonParser.Event event = parser.next();
                return Optional.of(forCurrent(event, parser));
            } catch (JsonException e) {
                return Optional.of(new ReplayableJsonException(e));
            }
        } else {
            return Optional.empty();
        }
    }
}

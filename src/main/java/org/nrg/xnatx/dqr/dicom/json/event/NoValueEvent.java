package org.nrg.xnatx.dqr.dicom.json.event;

import jakarta.json.stream.JsonLocation;
import jakarta.json.stream.JsonParser.Event;

/**
 * Replayable event that doesn't carry an associated value. This includes obvious cases
 * (START_OBJECT, END_ARRAY) but also enumerated values: VALUE_TRUE, VALUE_FALSE, VALUE_NULL
 */
public final class NoValueEvent extends AbstractReplayableJsonEvent implements ReplayableJsonEvent {
    private final Event event;

    public NoValueEvent(final JsonLocation location, final Event event) {
        super(location, event.name());
        this.event = event;
    }

    @Override
    public Event getEvent() { return event; }

    @Override
    public String toString() { return event.name() + "@" + getLocation(); }
}

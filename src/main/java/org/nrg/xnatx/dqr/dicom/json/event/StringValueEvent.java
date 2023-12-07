package org.nrg.xnatx.dqr.dicom.json.event;

import javax.json.stream.JsonLocation;
import javax.json.stream.JsonParser.Event;

/**
 * KEY_NAME and VALUE_STRING carry a String value.
 */
public final class StringValueEvent extends AbstractReplayableJsonEvent implements ReplayableJsonEvent {
    private final Event event;
    private final String value;

    public StringValueEvent(final JsonLocation location, final Event event, final String value) {
        super(location, event.name());
        this.event = event;
        this.value = value;
    }

    @Override
    public Event getEvent() { return event; }

    @Override
    public String getString() { return value; }

    @Override
    public String toString() { return event.name() + "=" + value + " @" + getLocation(); }
}

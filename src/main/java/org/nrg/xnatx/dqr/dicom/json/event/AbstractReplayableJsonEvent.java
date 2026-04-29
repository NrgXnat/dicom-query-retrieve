package org.nrg.xnatx.dqr.dicom.json.event;

import jakarta.json.stream.JsonLocation;
import java.math.BigDecimal;

/**
 * Provide common default behaviors for replayable events.
 */
abstract class AbstractReplayableJsonEvent implements ReplayableJsonEvent {
    private final JsonLocation location;
    private final String stateDescription;

    protected AbstractReplayableJsonEvent(final JsonLocation location, final String stateDescription) {
        this.location = location;
        this.stateDescription = stateDescription;
    }

    @Override
    public String getString() {
        throw new IllegalStateException("cannot call getString() on parser in state " + stateDescription);
    }

    @Override
    public boolean isIntegralNumber() {
        throw new IllegalStateException("cannot call isIntegralNumber() on parser in state " + stateDescription);
    }

    @Override
    public int getInt() {
        throw new IllegalStateException("cannot call getInt() on parser in state " + stateDescription);
    }

    @Override
    public long getLong() {
        throw new IllegalStateException("cannot call getLong() on parser in state " + stateDescription);
    }

    @Override
    public BigDecimal getBigDecimal() {
        throw new IllegalStateException("cannot call getBigDecimal() on parser in state " + stateDescription);
    }

    @Override
    public boolean isException() { return false; }

    @Override
    public JsonLocation getLocation() {
        return location;
    }
}

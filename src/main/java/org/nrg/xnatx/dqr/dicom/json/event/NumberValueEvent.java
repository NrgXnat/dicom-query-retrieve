package org.nrg.xnatx.dqr.dicom.json.event;

import javax.json.stream.JsonLocation;
import javax.json.stream.JsonParser;
import java.math.BigDecimal;

/**
 * This class converts numeric values, decoded from InlineBinary, to compliant DICOM JSON
 * plain old numeric Value array entries. There are constructors for each numeric
 * type we might receive so that we can start from the full binary representation when
 * we produce a decimal string representation. (Exception: short, int can use the long
 * constructor because there's no potential for lossy casting.)
 */
public final class NumberValueEvent extends AbstractReplayableJsonEvent implements ReplayableJsonEvent {
    private final String stringValue;
    private final BigDecimal value;
    private final boolean isInt;

    public NumberValueEvent(final JsonParser parser) {
        super(parser.getLocation(), JsonParser.Event.VALUE_NUMBER.name());
        stringValue = parser.getString();
        value = new BigDecimal(stringValue);
        isInt = parser.isIntegralNumber();
    }

    public NumberValueEvent(final JsonLocation location, final long v) {
        super(location, JsonParser.Event.VALUE_NUMBER.name());
        stringValue = String.valueOf(v);
        value = new BigDecimal(v);
        isInt = true;
    }

    public NumberValueEvent(final JsonLocation location, final float v) {
        super(location, JsonParser.Event.VALUE_NUMBER.name());
        stringValue = String.valueOf(v);
        value = new BigDecimal(v);
        isInt = false;
    }

    public NumberValueEvent(final JsonLocation location, final double v) {
        super(location, JsonParser.Event.VALUE_NUMBER.name());
        stringValue = String.valueOf(v);
        value = new BigDecimal(v);
        isInt = false;
    }

    public NumberValueEvent(final JsonLocation location, final BigDecimal v, final boolean isInt) {
        super(location, JsonParser.Event.VALUE_NUMBER.name());
        stringValue = v.toPlainString();
        value = v;
        this.isInt = isInt;
    }

    @Override
    public JsonParser.Event getEvent() { return JsonParser.Event.VALUE_NUMBER; }

    @Override
    public String getString() { return stringValue; }

    @Override
    public boolean isIntegralNumber() { return isInt; }

    @Override
    public int getInt() {
        return value.intValue();
    }

    @Override
    public long getLong() {
        return value.longValue();
    }

    @Override
    public BigDecimal getBigDecimal() { return value; }

    @Override
    public String toString() { return getEvent().name() + "=" + value + " @" + getLocation(); }
}

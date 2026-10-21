package org.nrg.xnatx.dqr.dicom.json;

import org.nrg.xnatx.dqr.dicom.json.event.NoValueEvent;
import org.nrg.xnatx.dqr.dicom.json.event.NumberValueEvent;
import org.nrg.xnatx.dqr.dicom.json.event.ReplayableJsonEvent;
import org.nrg.xnatx.dqr.dicom.json.event.StringValueEvent;
import lombok.extern.slf4j.Slf4j;
import org.dcm4che3.data.UID;
import org.dcm4che3.data.VR;

import jakarta.json.stream.JsonLocation;
import jakarta.json.stream.JsonParser;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.util.Base64;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * @see jakarta.json.stream.JsonParser
 * Filtering JsonParser that captures the token stream from another JsonParser and makes
 * corrections for an error in DICOM JSON content known to be made by some devices.
 * In detail: the content tag "InlineBinary" is allowed only to be used for the VRs listed
 * in inlineBinaryAllowedVRs; but some devices use it for VR SL as well and that causes
 * the dcm4che3 parser to fail.
 * Throughout this code we favor delegating to the source JsonParser whenever possible.
 * If we are inside an element object, we collect and record events/tokens from the source
 * in an ElementState queue, and usually just replay those events to the caller. In the
 * one case of an element with VR SL and InlineBinary content, we replace the InlineBinary
 * tag with Value and the base64-encoded content with an array of (signed) integers.
 */
@Slf4j
final public class DicomCorrectingJsonParser implements JsonParser {
    private static final EnumSet<VR> INLINE_BINARY_ALLOWED_VRS = EnumSet.of(VR.OB, VR.OD, VR.OF, VR.OL, VR.OV, VR.OW, VR.UN);
    private static final Pattern tagPattern = Pattern.compile("[0-9a-fA-f]{8}");
    private static final String TAG_TRANSFER_SYNTAX = "00020010";
    private static final String KEY_INLINE_BINARY = "InlineBinary";
    private static final String KEY_VR = "vr";
    private static final String KEY_VALUE = "Value";
    private final JsonParser sourceParser;
    private ElementState elementState;
    private ByteOrder byteOrder = ByteOrder.LITTLE_ENDIAN;  // default Transfer Syntax is Explicit VR Little Endian

    public DicomCorrectingJsonParser(final JsonParser jsonParser) {
        sourceParser = jsonParser;
        elementState = null;
    }

    @Override
    public boolean hasNext() {
        if (null != elementState && !elementState.queue.isEmpty()) {
            return true;
        } else {
            return sourceParser.hasNext();
        }
    }

    @Override
    public Event next() {
        if (null != elementState) {
            if (elementState.hasNext()) {
                return elementState.next();
             } else {
                elementState = null;
                return next();
            }
        } else {
            final Event e = sourceParser.next();
            if (e == Event.KEY_NAME) {
                // If this token is a tag-valued key, the value is a JSON object representing
                // a DICOM element. Create a new ElementState to parse it.
                if (tagPattern.matcher(sourceParser.getString()).matches()) {
                    // There are other possibilities: VR PN Value is a map, so we'd have hit the
                    // START_OBJECT that cleared the previous state and could now be looking at
                    // components of the PN map. That's why we check for a tag.
                    elementState = new ElementState(e);
                    if (TAG_TRANSFER_SYNTAX.equals(elementState.current.getString())) {
                        // If we see Transfer Syntax, we need to process so we can apply the right byte ordering.
                        elementState.readTransferSyntaxElement();
                    } else {
                        elementState.start();
                    }
                }
            }
            return e;
        }
    }

    @Override
    public String getString() {
        return null == elementState ? sourceParser.getString() : elementState.current.getString();
    }

    @Override
    public boolean isIntegralNumber() {
        return null == elementState ? sourceParser.isIntegralNumber() : elementState.current.isIntegralNumber();
    }

    @Override
    public int getInt() {
        return null == elementState ? sourceParser.getInt() : elementState.current.getInt();
    }

    @Override
    public long getLong() {
        return null == elementState ? sourceParser.getLong() : elementState.current.getLong();
    }

    @Override
    public BigDecimal getBigDecimal() {
        return null == elementState ? sourceParser.getBigDecimal() : elementState.current.getBigDecimal();
    }

    @Override
    public JsonLocation getLocation() {
        return null == elementState ? sourceParser.getLocation() : elementState.current.getLocation();
    }

    @Override
    public void close() {
        sourceParser.close();
    }

    /**
     * State machine that intercepts the token stream from the source JsonParser,
     * determines if we're in an element that needs correction, and makes the
     * correction if so. Along the way it collects a queue of ReplayableJsonEvents
     * to play back to the consumer.
     */
    @SuppressWarnings("UnnecessaryReturnStatement")
    private final class ElementState {
        final LinkedList<ReplayableJsonEvent> queue = new LinkedList<>();
        ReplayableJsonEvent current;
        ReplayableJsonEvent contentKey = null;
        ReplayableJsonEvent contentValue = null;
        VR vr = null;

        /**
         * Initialize with the current event on the source JsonParser but do not start
         * reading more tokens.
         * @param event current event
         */
        ElementState(final Event event) {
            current = ReplayableJsonEvent.forCurrent(event, sourceParser);
            if (Event.KEY_NAME != current.getEvent() || !tagPattern.matcher(current.getString()).matches()) {
                throw new IllegalStateException("ElementState must start at parse of tag key, found " + current);
            }
        }

        /**
         * Start state for transforming an element
         */
        void start() {
            expectStartObject();
        }

        /**
         * Do we have more queued events?
         * @return true if so
         */
        boolean hasNext() {
            return !queue.isEmpty();
        }

        /**
         * Provide the next queued Event, making the next recorded token the current dispatch object.
         * @return the next Event
         * @throws java.util.NoSuchElementException if the queue is empty
         */
        Event next() {
            current = queue.remove();
            return current.getEvent();
        }

        /**
         * Verify that we are at the START_OBJECT (should be after a tag)
         * then move to the next state.
         */
        void expectStartObject() {
            final Optional<ReplayableJsonEvent> oev = nextSourceEvent("waiting for object after tag key");
            if (!oev.isPresent()) {
                return; // invalid state, return to pass through
            }
            final ReplayableJsonEvent ev = oev.get();
            if (ev.getEvent() != Event.START_OBJECT) {
                log.warn("{}: expected START_OBJECT token after tag key", ev.getLocation());
                return;
            }
            readingElement();
        }

        /**
         * We don't know enough about this element yet to determine whether we need to process.
         * Collect and stash tokens until we can move to another state.
         */
        void readingElement() {
            final Optional<ReplayableJsonEvent> oev = nextSourceEvent("reading DICOM element");
            if (!oev.isPresent()) {
                return; // invalid state, return to pass through
            }
            final ReplayableJsonEvent ev = oev.get();
            if (ev.getEvent() == Event.KEY_NAME) {
                final String key = ev.getString();
                if (KEY_VR.equals(key)) {
                    assignVR1();
                } else if (KEY_INLINE_BINARY.equals(key)) {
                    contentKey = ev;
                    expectBinaryContent1();
                } else {
                    // non-InlineBinary content provision; we have no corrections, return to pass through
                    return;
                }
            } else {
                log.warn("{}: unexpected JSON token {} when key expected inside DICOM element", ev.getLocation(), ev.getEvent());
                // unknown state, return to pass through
                return;
            }
        }

        /**
         * We're done with this element, collect up to the END_OBJECT so we can replay for the consumer.
         */
        void endElement() {
            for (;;) {
                final Optional<ReplayableJsonEvent> oev = nextSourceEvent("ending DICOM element");
                if (!oev.isPresent()) {
                    return; // invalid state, return to pass through
                }
                final Event event = oev.get().getEvent();
                // END_OBJECT is straightforward; START_OBJECT is less so. If we're in a SQ element,
                // then there's no conversion necessary, but each nested object starts with a START_OBJECT.
                // We don't have any more processing to do on this element but that nested object will
                // have some we'll need to evaluate.
                if (event == Event.END_OBJECT || event == Event.START_OBJECT) {
                    log.trace("{} element complete: {}", oev.get().getLocation(), queue);
                    return; // element object complete, return
                }
            }
        }

        /**
         * Read and store an InlineBinary content string. We don't know the VR yet
         * so we can't be sure whether we need to convert.
         */
        void expectBinaryContent1() {
            final Optional<ReplayableJsonEvent> oev = nextSourceEvent("reading VR value");
            if (!oev.isPresent()) {
                return; // invalid state, return to pass through
            }
            final ReplayableJsonEvent ev = oev.get();
            if (ev.getEvent() == Event.VALUE_STRING) {
                contentValue = ev;
                expectVR();
            } else {
                log.warn("{}: unexpected JSON token {} when inline binary string value expected", ev.getLocation(), ev.getEvent());
                // unknown state, return to pass through
                return;
            }
        }

        /**
         * Read the value for "vr"; if this VR allows InlineBinary than we don't need any conversion
         * so we can skip to the end of the element. If not, then we need to read the VR as well
         * to see if we need conversion.
         */
        void assignVR1() {
            final Optional<ReplayableJsonEvent> oev = nextSourceEvent("reading VR value");
            if (!oev.isPresent()) {
                return; // invalid state, return to pass through
            }
            final ReplayableJsonEvent ev = oev.get();
            if (ev.getEvent() == Event.VALUE_STRING) {
                final String vrName = ev.getString();
                try {
                    vr = VR.valueOf(vrName);
                } catch (IllegalArgumentException e) {
                    log.warn("{}: expected VR specification, found {}", ev.getLocation(), vrName);
                    // unknown state, return to pass through
                    return;
                }
                if (INLINE_BINARY_ALLOWED_VRS.contains(vr)) {
                    log.trace("InlineBinary content allowed for VR {}, no correction needed", vr);
                    endElement();
                } else {
                    log.trace("InlineBinary not allowed for VR {}, waiting for content type", vr);
                    expectContentKey();
                }
            } else {
                log.warn("{}: unexpected JSON token {} when VR value expected inside DICOM element", ev.getLocation(), ev.getEvent());
                // unknown state, return to pass through
                return;
            }
        }

        /**
         * We are at the key that will tell us the content type, and we know this VR does not support
         * InlineBinary. If the content is InlineBinary, we will need to read the content and then
         * correct. If not, no correction is required and we can close out the element.
         */
        void expectContentKey() {
            final Optional<ReplayableJsonEvent> oev = nextSourceEvent("reading content key");
            if (!oev.isPresent()) {
                return; // invalid state, return to pass through
            }
            final ReplayableJsonEvent ev = oev.get();
            switch (ev.getEvent()) {
                case KEY_NAME:
                    final String contentName = ev.getString();
                    if (KEY_INLINE_BINARY.equals(contentName)) {
                        contentKey = ev;
                        expectConvertibleContent();
                    } else {
                        // content type does not require conversion, finish the object
                        endElement();
                    }
                    break;

                case END_OBJECT:
                    log.debug("no value in element, element state complete");
                    return;

                default:
                    log.warn("{}: unexpected JSON token {} waiting for DICOM element content key", ev.getLocation(), ev.getEvent());
                    // unknown state, return to pass through
                    return;
            }
        }

        /**
         * We know the content is InlineBinary but do not yet know the VR; we should be getting the VR key now.
         */
        void expectVR() {
            final Optional<ReplayableJsonEvent> oev = nextSourceEvent("reading VR key");
            if (!oev.isPresent()) {
                return; // invalid state, return to pass through
            }
            final ReplayableJsonEvent ev = oev.get();
            if (ev.getEvent() == Event.KEY_NAME) {
                final String key = ev.getString();
                if (KEY_VR.equals(key)) {
                    assignVR2();
                } else {
                    log.warn("{}: unexpected JSON key {} waiting for DICOM element key \"vr\"", ev.getLocation(), key);
                    // unknown state, return to pass through
                    return;
                }
            } else {
                log.warn("{}: unexpected JSON token {} waiting for DICOM element VR key", ev.getLocation(), ev.getEvent());
                // unknown state, return to pass through
                return;
            }
        }

        /**
         * We know the content is InlineBinary and are now getting the VR.
         */
        void assignVR2() {
            final Optional<ReplayableJsonEvent> oev = nextSourceEvent("reading VR value");
            if (!oev.isPresent()) {
                return; // invalid state, return to pass through
            }
            final ReplayableJsonEvent ev = oev.get();
            if (ev.getEvent() == Event.VALUE_STRING) {
                final String vrName = ev.getString();
                try {
                    vr = VR.valueOf(vrName);
                } catch (IllegalArgumentException e) {
                    log.warn("{}: expected VR specification, found {}", ev.getLocation(), vrName);
                    // unknown state, return to pass through
                    return;
                }
                if (INLINE_BINARY_ALLOWED_VRS.contains(vr)) {
                    log.trace("InlineBinary content allowed for VR {}, no correction needed", vr);
                    endElement();
                } else {
                    log.trace("InlineBinary not allowed for VR {}, waiting for content type", vr);
                    convertContent();
                }
            }
        }

        /**
         * Read the InlineBinary content that we will convert.
         */
        void expectConvertibleContent() {
            final Optional<ReplayableJsonEvent> oev = nextSourceEvent("reading VR value");
            if (!oev.isPresent()) {
                return; // invalid state, return to pass through
            }
            final ReplayableJsonEvent ev = oev.get();
            if (ev.getEvent() == Event.VALUE_STRING) {
                contentValue = ev;
                convertContent();
                endElement();
            } else {
                log.warn("{}: expected InlineBinary content, found {}", ev.getLocation(), ev.getEvent());
                // unknown state, return to pass through
                return;
            }
        }

        /**
         * We have the content key; replace it in the token stream with "Value".
         * We have the inline binary content; convert it and replace the value token with
         * an array of integer values.
         */
        void convertContent() {
            if (contentKey == null) {
                throw new IllegalStateException("must identify content key token before converting");
            }
            if (contentValue == null) {
                throw new IllegalStateException("must identify content value token before converting");
            }
            final int keyIndex = queue.indexOf(contentKey);
            final int contentIndex = queue.indexOf(contentValue);
            if (keyIndex + 1 != contentIndex) {
                log.warn("key index {} and content index {} are not consecutive", keyIndex, contentIndex);
            }

            final ByteBuffer bytes = ByteBuffer.wrap(Base64.getDecoder().decode(contentValue.getString()));
            bytes.order(byteOrder);

            // Replace the "InlineBinary": "..." token sequence with "Value": [sl0, sl1, ...]
            final List<ReplayableJsonEvent> valueEvents = new LinkedList<>();
            final Runnable convertValue;
            switch (vr) {
                case AT: convertValue = () -> {
                    final IntBuffer ibuf = bytes.asIntBuffer();
                    for (int i = 0; i < ibuf.limit(); i++) {
                        final String hex = String.format("%08X", ibuf.get(i));
                        valueEvents.add(new StringValueEvent(contentValue.getLocation(), Event.VALUE_STRING, hex));
                    }
                };
                break;

                case FL: convertValue = () -> {
                    final FloatBuffer fbuf = bytes.asFloatBuffer();
                    for (int i = 0; i < fbuf.limit(); i++) {
                        valueEvents.add(new NumberValueEvent(contentValue.getLocation(), fbuf.get(i)));
                    }
                };
                break;

                case FD: convertValue = () -> {
                    final DoubleBuffer dbuf = bytes.asDoubleBuffer();
                    for (int i = 0; i < dbuf.limit(); i++) {
                        valueEvents.add(new NumberValueEvent(contentValue.getLocation(), dbuf.get(i)));
                    }
                };
                break;

                case SS: convertValue = () -> {
                    final ShortBuffer sbuf = bytes.asShortBuffer();
                    for (int i = 0; i < sbuf.limit(); i++) {
                        valueEvents.add(new NumberValueEvent(contentValue.getLocation(), sbuf.get(i)));
                    }
                };
                break;

                case US: convertValue = () -> {
                    final ShortBuffer sbuf = bytes.asShortBuffer();
                    for (int i = 0; i < sbuf.limit(); i++) {
                        // Java doesn't have unsigned values so we have to put the bytes in the next width integer type
                        final short sval = sbuf.get(i);
                        final int usval = 0xffff & sval;
                        valueEvents.add(new NumberValueEvent(contentValue.getLocation(), usval));
                    }
                 };
                break;

                case SL: convertValue = () -> {
                    final IntBuffer ibuf = bytes.asIntBuffer();
                    for (int i = 0; i < ibuf.limit(); i++) {
                        valueEvents.add(new NumberValueEvent(contentValue.getLocation(), ibuf.get(i)));
                    }
                };
                break;

                case UL: convertValue = () -> {
                    final IntBuffer ibuf = bytes.asIntBuffer();
                    for (int i = 0; i < ibuf.limit(); i++) {
                        final int ival = ibuf.get(i);
                        final long val = 0xffffffffL & ival;
                        valueEvents.add(new NumberValueEvent(contentValue.getLocation(), val));
                    }
                };
                break;

                case SV: convertValue = () -> {
                    final LongBuffer lbuf = bytes.asLongBuffer();
                    for (int i = 0; i < lbuf.limit(); i++) {
                        valueEvents.add(new NumberValueEvent(contentValue.getLocation(), lbuf.get(i)));
                    }
                };
                break;

                case UV: convertValue = () -> {
                    final LongBuffer lbuf = bytes.asLongBuffer();
                    final BigDecimal two = new BigDecimal(2);
                    for (int i = 0; i < lbuf.limit(); i++) {
                        // build a BigDecimal that is the unsigned long value of these 8 bytes
                        final long lval = lbuf.get(i);
                        final BigDecimal lsb = new BigDecimal(lval & 0x01);
                        final BigDecimal v = new BigDecimal(lval >> 1).multiply(two).add(lsb);
                        valueEvents.add(new NumberValueEvent(contentValue.getLocation(), v, true));
                    }
                };
                break;

                default:
                    throw new UnsupportedOperationException("InlineBinary decoding not implemented for " + vr);
            }
            valueEvents.add(new NoValueEvent(contentValue.getLocation(), Event.START_ARRAY));
            convertValue.run();
            valueEvents.add(new NoValueEvent(contentValue.getLocation(), Event.END_ARRAY));

            log.trace("converting InlineBinary {} -> {}", contentValue, valueEvents);
            queue.remove(contentValue);
            queue.addAll(contentIndex, valueEvents);

            final StringValueEvent fixedKey = new StringValueEvent(contentKey.getLocation(), contentKey.getEvent(), KEY_VALUE);
            queue.set(keyIndex, fixedKey);
        }

        /**
         * The Transfer Syntax element is a special case; it's a simple value assignment so
         * we wrap the whole state machine into this one method; and we need the value to
         * set the byte ordering for this document.
         */
        void readTransferSyntaxElement() {
            // Skip ahead to the first tag name
            ReplayableJsonEvent ev;
            do {
                final Optional<ReplayableJsonEvent> oev = nextSourceEvent("looking for Transfer Syntax Value");
                if (oev.isPresent()) {
                    ev = oev.get();
                } else {
                    return;
                }
            } while (ev.getEvent() != Event.KEY_NAME);
            if (KEY_VALUE.equals(ev.getString())) {
                // skip ahead to the content
                do {
                    final Optional<ReplayableJsonEvent> oev = nextSourceEvent("looking for Transfer Syntax content");
                    if (oev.isPresent()) {
                        ev = oev.get();
                    } else {
                        return;
                    }
                } while (ev.getEvent() != Event.VALUE_STRING);
                final String tsuid = ev.getString();
                byteOrder = UID.ExplicitVRBigEndian.equals(tsuid) ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
                endElement();
            } else {
                // We know the VR, it's UI. Skip over the value to the next tag name
                readTransferSyntaxElement();
            }
        }

        /**
         * Utility for retrieving, checking, and stashing the source event.
         * @param stateDescription description of state of the parser, for error messages
         * @return event if available and valid, empty otherwise
         */
        Optional<ReplayableJsonEvent> nextSourceEvent(final String stateDescription) {
            final Optional<ReplayableJsonEvent> oev = ReplayableJsonEvent.next(sourceParser);
            if (!oev.isPresent()) {
                log.warn("reached unexpected end of JSON token stream {}", stateDescription);
                return Optional.empty();
            }
            final ReplayableJsonEvent ev = oev.get();
            queue.add(ev);
            if (ev.isException()) {
                log.warn("JSON exception encountered {}", stateDescription);
                return Optional.empty();
            }
            return oev;
        }
    }
}

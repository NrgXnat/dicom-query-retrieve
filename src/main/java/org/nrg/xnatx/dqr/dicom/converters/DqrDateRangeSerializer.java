package org.nrg.xnatx.dqr.dicom.converters;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import org.nrg.xnatx.dqr.utils.DqrDateRange;

public class DqrDateRangeSerializer extends StdSerializer<DqrDateRange> {
    public DqrDateRangeSerializer() {
        super(DqrDateRange.class);
    }

    @Override
    public void serialize(final DqrDateRange value, final JsonGenerator generator, final SerializerProvider provider) throws IOException {
        generator.writeStartObject();
        generator.writeStringField("start", DqrDateRange.format(value.getStart()));
        generator.writeObjectField("end", DqrDateRange.format(value.getEnd()));
        generator.writeEndObject();
    }
}

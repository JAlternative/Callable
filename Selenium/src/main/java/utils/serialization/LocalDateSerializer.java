package utils.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.time.LocalDate;

import static utils.tools.Format.API;

public class LocalDateSerializer extends StdSerializer<LocalDate> {
    protected LocalDateSerializer() {
        super((Class<LocalDate>) null);
    }

    @Override
    public void serialize(LocalDate value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        String s = value.format(API.getFormat());
        gen.writeString(s);
    }
}
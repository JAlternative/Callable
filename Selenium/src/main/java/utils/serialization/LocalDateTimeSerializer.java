package utils.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.time.LocalDateTime;

import static utils.tools.Format.API_KPI_VALUE;

public class LocalDateTimeSerializer extends StdSerializer<LocalDateTime> {
    protected LocalDateTimeSerializer() {
        super((Class<LocalDateTime>) null);
    }

    @Override
    public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        String s = value.format(API_KPI_VALUE.getFormat());
        gen.writeString(s);
    }
}
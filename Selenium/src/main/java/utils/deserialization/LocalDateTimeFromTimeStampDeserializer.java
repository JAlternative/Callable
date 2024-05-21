package utils.deserialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.sql.Date;
import java.sql.Time;
import java.time.LocalDateTime;

public class LocalDateTimeFromTimeStampDeserializer extends StdDeserializer {

    protected LocalDateTimeFromTimeStampDeserializer() {
        super((Class<?>) null);
    }
    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        return (LocalDateTime.of(new Date(node.asLong()).toLocalDate(), new Time(node.asLong()).toLocalTime()));
    }
}

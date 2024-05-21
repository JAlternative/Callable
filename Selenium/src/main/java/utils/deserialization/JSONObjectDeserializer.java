package utils.deserialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.json.JSONObject;

import java.io.IOException;


public class JSONObjectDeserializer extends StdDeserializer<JSONObject> {
    protected JSONObjectDeserializer() {
        super((Class<?>) null);
    }

    @Override
    public JSONObject deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode epNode = p.getCodec().readTree(p);
        return new JSONObject(String.valueOf(epNode));
    }
}

package utils.deserialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static utils.Params.HREF;

public class LinksToMapDeserializer extends StdDeserializer {
    protected LinksToMapDeserializer() {
        super((Class<?>) null);
    }

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        HashMap<String, String> links = new HashMap<>();
        Iterator<Map.Entry<String, JsonNode>> it = node.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> next = it.next();
            links.put(next.getKey(), next.getValue().get(HREF).asText());
        }
        return links;
    }
}

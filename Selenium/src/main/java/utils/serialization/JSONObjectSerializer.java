package utils.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.json.JSONObject;

import java.io.IOException;

public class JSONObjectSerializer extends StdSerializer<JSONObject> {

    protected JSONObjectSerializer() {
        super((Class<JSONObject>) null);
    }

    @Override
    public void serialize(JSONObject value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeRaw(':');
        String s = value.toString();
        gen.writeRaw(s);
    }
}

package utils.deserialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.json.JSONObject;
import wfm.models.*;

import java.io.IOException;

import static utils.Params.*;

public class EmployeePositionDeserializer extends StdDeserializer<EmployeePosition> {

    protected EmployeePositionDeserializer() {
        super((Class<?>) null);
    }

    @Override
    public EmployeePosition deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode epNode = p.getCodec().readTree(p);
        EmployeePosition ep = new EmployeePosition();
        ObjectMapper op = new ObjectMapper().findAndRegisterModules();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(JSONObject.class, new JSONObjectDeserializer());
        op.registerModule(module);
        ep.setId(epNode.get(ID).asInt());
        ep.setOrgUnit(op.treeToValue(epNode.get(EMBEDDED).get(POSITION).get(EMBEDDED).get(ORG_UNIT_JSON), OrgUnit.class));
        ep.setEmployee(op.treeToValue(epNode.get(EMBEDDED).get(EMPLOYEE_JSON), Employee.class));
        ep.setPosition(op.treeToValue(epNode.get(EMBEDDED).get(POSITION), Position.class));
        if (epNode.get(HIDDEN) != null) {
            ep.setHidden(epNode.get(HIDDEN).asBoolean());
        }
        if (epNode.get(TEMPORARY) != null) {
            ep.setHidden(epNode.get(TEMPORARY).asBoolean());
        }
        ep.setDateInterval(op.treeToValue(epNode.get(DATE_INTERVAL), DateInterval.class));
        ep.setLinks(op.treeToValue(epNode.get(LINKS), JSONObject.class));
        return ep;
    }
}

package wfm.models;

import org.json.JSONObject;
import utils.Params;

public class AttributeValue {
    private int id;
    private String value;

    public AttributeValue(JSONObject jsonObject) {
        id = jsonObject.getInt(Params.ID);
        value = jsonObject.getString(Params.VALUE);
    }

    public int getId() {
        return id;
    }

    public String getValue() {
        return value;
    }
}

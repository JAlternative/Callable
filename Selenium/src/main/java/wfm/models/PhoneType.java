package wfm.models;

import org.json.JSONObject;
import utils.LinksAnnotation;
import wfm.HasLinks;

import static utils.Params.*;

public class PhoneType implements HasLinks {

    private String code;

    private String name;

    private String relation;
    @LinksAnnotation
    private JSONObject links;

    public PhoneType(JSONObject json) {
        code = json.getString(CODE);
        name = json.getString(NAME);
        relation = json.getString("relation");
        links = json.getJSONObject(LINKS);
    }

    public String getCode() {
        return code;
    }

    public String getRelation() {
        return relation;
    }

    public String getName() {
        return name;
    }
}

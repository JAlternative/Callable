package wfm.models;

import org.json.JSONObject;

import static utils.Params.*;

public class OrgUnitInRole {

    private final int id;
    private final JSONObject links;

    public OrgUnitInRole(JSONObject jsonObject) {
        this.links = jsonObject.getJSONObject(LINKS);
        this.id = jsonObject.getInt(ID);
    }

    public JSONObject getLinks() {
        return links;
    }

    public int getId() {
        return id;
    }
}

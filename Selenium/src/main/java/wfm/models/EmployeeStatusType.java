package wfm.models;

import org.json.JSONObject;

import static utils.Params.*;

public class EmployeeStatusType {
    private final String outerId;
    private final String subType;
    private final String title;
    private final String shortTitle;
    private final JSONObject links;
    private final int id;
    private final boolean newStatus;


    public EmployeeStatusType(JSONObject json) {
        this.outerId = json.getString(OUTER_ID);
        this.subType = json.optString("subType");
        this.title = json.getString(TITLE);
        this.shortTitle = json.optString("shortTitle");
        this.links = json.optJSONObject(LINKS);
        this.id = json.optInt(ID);
        this.newStatus = json.optBoolean("new");
    }

    public String getOuterId() {
        return outerId;
    }

    public String getTitle() {
        return title;
    }

    public int getId() {
        return id;
    }

}

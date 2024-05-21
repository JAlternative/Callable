package wfm.models;

import io.qameta.allure.internal.shadowed.jackson.annotation.JsonIgnore;
import org.json.JSONObject;
import utils.Params;
import utils.tools.CustomTools;

import static utils.Params.HREF;
import static utils.Params.SELF;

public class OutsidePlanResource {
    private final int employeePositionId;
    private final String type;
    private final JSONObject links;
    private final DateTimeInterval dateTimeInterval;
    private final int id;


    public OutsidePlanResource(JSONObject json) {
        this.employeePositionId = json.getInt("employeePositionId");
        String jsonType;
        try {
            jsonType = json.getString("type");
        } catch (org.json.JSONException e){
            jsonType = json.getString("subType");
        }
        this.type = jsonType;
        this.links = json.getJSONObject(Params.LINKS);
        this.dateTimeInterval = CustomTools.getClassObjectFromJson(DateTimeInterval.class, json.getJSONObject("dateTimeInterval"));
        this.id = json.getInt("id");
    }

    public int getEmployeePositionId() {
        return employeePositionId;
    }

    public String getType() {
        return type;
    }

    public JSONObject getLinks() {
        return links;
    }

    public DateTimeInterval getDateTimeInterval() {
        return dateTimeInterval;
    }

    public int getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OutsidePlanResource that = (OutsidePlanResource) o;

        if (employeePositionId != that.employeePositionId) return false;
        if (!type.equals(that.type)) return false;
        return dateTimeInterval.equals(that.dateTimeInterval);
    }

    @JsonIgnore
    public String getSelfLink() {
        return links.getJSONObject(SELF).getString(HREF);
    }
}

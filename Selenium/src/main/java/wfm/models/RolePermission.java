package wfm.models;

import org.json.JSONObject;
import utils.LinksAnnotation;

import java.util.*;

import static utils.Params.*;

public class RolePermission {

    @LinksAnnotation
    private JSONObject links;
    private String scheduleRequestType;
    private String rosterType;

    public RolePermission(JSONObject jsonObject) {
        this.links = jsonObject.getJSONObject(LINKS);
        this.scheduleRequestType = jsonObject.getString("scheduleRequestType");
        this.rosterType = jsonObject.optString("rosterType");
    }

    public RolePermission() {}

    public JSONObject getLinks() {
        return links;
    }

    public String getScheduleRequestType() {
        return scheduleRequestType;
    }

    public String getRosterType() {
        return rosterType;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this){
            return true;
        }
        if (getClass() != o.getClass()) {
            return false;
        }
        RolePermission rolePermission = (RolePermission) o;
        return rolePermission.links.similar(this.links)
                && rolePermission.scheduleRequestType.equals(this.scheduleRequestType)
                && Objects.equals(rolePermission.rosterType, this.rosterType);
    }
}

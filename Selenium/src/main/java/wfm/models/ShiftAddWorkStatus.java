package wfm.models;

import org.json.JSONObject;
import utils.Params;
import wfm.HasLinks;

public class ShiftAddWorkStatus implements HasLinks {
    private final Integer id;
    private final String status;
    private final Integer addWorkId;
    private final JSONObject links;

    public ShiftAddWorkStatus(JSONObject json) {
        this.id = json.getInt(Params.ID);
        this.addWorkId = json.getInt("addWorkId");
        this.status = json.getString(Params.STATUS);
        links = json.getJSONObject(Params.LINKS);
    }

    public Integer getId() {
        return id;
    }

    public Integer getAddWorkId() {
        return addWorkId;
    }

    public String getStatus() {
        return status;
    }

    public JSONObject getLinks() {
        return links;
    }
}

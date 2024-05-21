package wfm.models;

import org.json.JSONObject;
import wfm.HasLinks;

import static utils.Params.*;
/**
 * На UI называется "Тип должности"
 */
public class PositionType implements HasLinks {
    private final String name;
    private final String outerId;
    private final int id;
    private final JSONObject links;

    public int getId() {
        return id;
    }

    public PositionType(JSONObject jsonObject) {
        this.id = jsonObject.optInt(ID);
        this.name = jsonObject.getString(NAME);
        this.outerId = jsonObject.getString(OUTER_ID);
        this.links = jsonObject.optJSONObject(LINKS);
    }

    public String getName() {
        return name;
    }
    public String getOuterId() {
        return outerId;
    }
    public JSONObject getLinks() {
        return links;
    }
}

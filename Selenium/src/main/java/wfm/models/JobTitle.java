package wfm.models;

import org.json.JSONObject;
import wfm.HasLinks;

import static utils.Params.*;

/**
 * На UI называется "Название должности"
 */
public class JobTitle implements HasLinks {
    private final String fullName;
    private final int id;
    private JSONObject links;


    public JobTitle(JSONObject json) {
        this.fullName = json.getString(NAME);
        this.id = json.getInt(ID);
        this.links = json.getJSONObject(LINKS);
    }

    public String getFullName() {
        return fullName;
    }

    public int getId() {
        return id;
    }
    @Override
    public JSONObject getLinks() {
        return links;
    }
}

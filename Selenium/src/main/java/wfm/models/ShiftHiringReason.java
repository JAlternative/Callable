package wfm.models;

import org.json.JSONObject;
import utils.Params;
import utils.Projects;
import wfm.HasLinks;

import java.net.URI;
import java.util.List;

import static utils.Params.*;
import static utils.tools.CustomTools.getListFromJsonObject;
import static utils.tools.RequestFormers.getJsonFromUri;

public class ShiftHiringReason implements HasLinks {

    private final String title;
    private final JSONObject links;
    private final int id;

    public ShiftHiringReason(JSONObject json) {
        this.title = json.optString(Params.TITLE);
        this.links = json.getJSONObject(Params.LINKS);
        this.id = json.getInt(Params.ID);
    }

    public String getTitle() {
        return title;
    }

    public JSONObject getLinks() {
        return links;
    }

    public int getId() {
        return id;
    }

    public List<EntityProperty> getAttachedEntityProperties() {
        URI uri = URI.create(links.getJSONObject(SELF).getString(HREF) + "/entity-properties");
        JSONObject json = getJsonFromUri(Projects.WFM, uri);
        return getListFromJsonObject(json, EntityProperty.class);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (! (o instanceof ShiftHiringReason)) {
            return false;
        }
        ShiftHiringReason other = (ShiftHiringReason) o;
        return other.getId() == id;
    }
}

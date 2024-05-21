package wfm.models;

import org.json.JSONObject;

import java.net.URI;

import static utils.Params.*;

public class ShiftEditReason {

    private final String code;
    private final String title;
    private final JSONObject links;

    public ShiftEditReason(JSONObject object) {
        this.code = object.getString("code");
        this.title = object.getString("title");
        this.links = object.getJSONObject(LINKS);
    }

    public String getCode() {
        return code;
    }

    public String getTitle() {
        return title;
    }

    public URI getSelfLink() {
        return URI.create(links.getJSONObject(SELF).getString(HREF));
    }
}

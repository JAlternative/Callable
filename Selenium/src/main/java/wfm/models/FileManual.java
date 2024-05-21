package wfm.models;

import org.json.JSONObject;
import utils.LinksAnnotation;
import utils.Params;
import wfm.HasLinks;

import java.time.ZonedDateTime;

public class FileManual implements HasLinks {
    String fileName;
    String type;
    ZonedDateTime created;
    ZonedDateTime updated;
    @LinksAnnotation
    JSONObject links;
    public FileManual(JSONObject json) {
        fileName = json.getString("fileName");
        type = json.optString(Params.TYPE);
        created = ZonedDateTime.parse(json.getString("created"));
        updated = ZonedDateTime.parse(json.getString("updated"));
        links = json.getJSONObject(Params.LINKS);
    }

    public String getFileName() {
        return fileName;
    }

    public String getType() {
        return type;
    }
}

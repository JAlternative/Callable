package wfm.models;

import org.json.JSONObject;

import java.net.URI;
import java.time.LocalDateTime;

import static utils.Params.*;

public class BioRecordResourceList {
    private final String dateTime;
    private final String processed;
    private final String title;
    private final String type;
    private final JSONObject links;

    public BioRecordResourceList(JSONObject object) {
        this.title = object.optString("title");
        this.dateTime = object.optString("dateTime");
        this.type = object.optString(TYPE);
        this.links = object.optJSONObject(LINKS);
        this.processed = object.optString("processed");
    }

    public LocalDateTime getDateTime() {
        return !dateTime.isEmpty() ? LocalDateTime.parse(dateTime) : null;
    }

    public String getProcessed() {
        return processed;
    }

    public String getTitle() {
        return title;
    }

    public String getType() {
        return type;
    }

    public URI getLink(String name) {
        return links != null ? URI.create(links.optJSONObject(name).optString(HREF)) : null;
    }

    public int getEmployeeId() {
        String employeeLink = links != null ? links.optJSONObject("bioRecEmployee").optString(HREF) : null;
        return employeeLink != null ? Integer.parseInt(employeeLink.substring(employeeLink.lastIndexOf("/") + 1)) : 0;
    }
}

package bio.models;

import org.json.JSONArray;
import org.json.JSONObject;

public class FaceDescriptors {
    private final int id;
    private final int personId;
    private final boolean confirmed;
    private final long timestamp;
    private final String body;
    private final String similarities;
    private final String srcUrl;
    private final JSONArray detailedLogs;
    private final String warning;


    public FaceDescriptors(JSONObject jsonObject) {
        this.id = jsonObject.getInt("id");
        this.personId = jsonObject.getInt("personId");
        this.confirmed = jsonObject.getBoolean("confirmed");
        this.timestamp = jsonObject.getLong("timestamp");
        this.body = jsonObject.optString("body");
        this.similarities = jsonObject.getString("similarities");
        this.srcUrl = jsonObject.optString("srcUrl", null);
        this.detailedLogs = jsonObject.getJSONArray("detailedLogs");
        this.warning = jsonObject.optString("warning");
    }

    public int getId() {
        return id;
    }

    public int getPersonId() {
        return personId;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getBody() {
        return body;
    }

    public String getSimilarities() {
        return similarities;
    }

    public String getSrcUrl() {
        return srcUrl;
    }

    public JSONArray getDetailedLogs() {
        return detailedLogs;
    }

    public String getWarning() {
        return warning;
    }
}

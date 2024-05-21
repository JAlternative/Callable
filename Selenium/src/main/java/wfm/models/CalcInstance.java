package wfm.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.json.JSONObject;
import utils.LinksAnnotation;
import utils.deserialization.LocalDateTimeFromTimeStampDeserializer;
import wfm.HasLinks;

import java.time.LocalDateTime;

import static utils.Params.LINKS;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CalcInstance implements HasLinks {
    private String name;
    private boolean active;
    private String status;
    @JsonDeserialize(using = LocalDateTimeFromTimeStampDeserializer.class)
    private LocalDateTime lastLiveTime;
    @LinksAnnotation
    private JSONObject links;

    public CalcInstance(JSONObject json) {
        this.active = json.getBoolean("active");
        this.name = json.getString("name");
        this.status = json.getString("status");
        this.links = json.getJSONObject(LINKS);
        this.lastLiveTime = LocalDateTime.parse(json.getString("lastLiveTime"));
    }

    public boolean isActive() {
        return active;
    }

    @Override
    public JSONObject getLinks() {
        return links;
    }

    public String getName() {
        return name;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getLastLiveTime() {
        return lastLiveTime;
    }
}

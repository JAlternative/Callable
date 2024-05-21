package wfm.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.json.JSONObject;
import utils.LinksAnnotation;
import utils.Projects;
import wfm.HasLinks;

import java.net.URI;
import java.util.List;

import static utils.Params.*;
import static utils.tools.CustomTools.getListFromJsonObject;
import static utils.tools.RequestFormers.*;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AdditionalWork implements HasLinks {

    private int id;
    private boolean disabled;
    private boolean hasStatuses;
    private String title;
    private String outerId;
    @LinksAnnotation
    private JSONObject links;

    public AdditionalWork(JSONObject jsonObject) {
        this.links = jsonObject.getJSONObject(LINKS);
        this.id = jsonObject.getInt(ID);
        this.disabled = jsonObject.optBoolean("disabled");
        this.title = jsonObject.optString(TITLE);
        this.hasStatuses = jsonObject.optBoolean("hasStatuses");
        this.outerId = jsonObject.getString(OUTER_ID);
    }

    public AdditionalWork() {
    }

    public String getTitle() {
        return title;
    }

    public String getOuterId() {
        return outerId;
    }

    @Override
    public JSONObject getLinks() {
        return links;
    }

    public int getId() {
        return id;
    }

    public boolean isHasStatuses() {
        return hasStatuses;
    }

    public boolean isDisabled() {
        return disabled;
    }

    @JsonIgnore
    public List<AddWorkRule> getAttachedAddWorkRules() {
        URI uri = URI.create(links.getJSONObject("addWorkRule").getString(HREF));
        JSONObject json = getJsonFromUri(Projects.WFM, uri);
        return getListFromJsonObject(json, AddWorkRule.class);
    }

    public AdditionalWork setHasStatuses(Boolean hasStatuses) {
        this.hasStatuses = hasStatuses;
        return this;
    }

    public AdditionalWork setDisabled(Boolean disabled) {
        this.disabled = disabled;
        return this;
    }

    @JsonIgnore
    public List<ShiftAddWorkStatus> getAddWorkStatuses() {
        String link = links.getJSONObject("statuses").getString(HREF);
        URI uri = URI.create(link.substring(0, link.indexOf('{')));
        JSONObject json = getJsonFromUri(Projects.WFM, uri);
        return getListFromJsonObject(json, ShiftAddWorkStatus.class);
    }

    @JsonIgnore
    public ShiftAddWorkStatus getAddWorkStatus(String name) {
        return getAddWorkStatuses().stream()
                .filter(status -> status.getStatus().equals(name))
                .findAny()
                .orElse(null);
    }

}

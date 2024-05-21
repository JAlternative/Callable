package wfm.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;
import utils.LinksAnnotation;
import wfm.HasLinks;

import static utils.Params.*;

public class ScheduleRequestAlias implements HasLinks {
    private int id;
    private boolean enabled;
    private boolean autoApprove;
    private boolean requireApproval;
    private String outerId;
    private String title;
    private String shortName;
    private String type;

    private boolean moveToExchange;
    private boolean bindToPosition;

    private String intervalType;
    @LinksAnnotation
    private JSONObject links;

    public ScheduleRequestAlias(JSONObject json) {
        this.id = json.getInt(ID);
        this.enabled = json.getBoolean(ENABLED);
        this.autoApprove = json.optBoolean(AUTO_APPROVE);
        this.requireApproval = json.optBoolean(REQUIRE_APPROVAL);
        this.moveToExchange = json.optBoolean(MOVE_TO_EXCHANGE);
        this.bindToPosition = json.optBoolean(BIND_TO_POSITION);
        this.outerId = json.getString(OUTER_ID);
        this.title = json.getString(TITLE);
        this.shortName = json.getString(SHORT_NAME);
        this.type = json.getString(TYPE);
        this.intervalType = json.optString(INTERVAL_TYPE).isEmpty() ? null : json.optString(INTERVAL_TYPE);
        this.links = json.getJSONObject(LINKS);
    }

    private ScheduleRequestAlias() {

    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isAutoApprove() {
        return autoApprove;
    }

    public boolean getRequireApproval() {
        return requireApproval;
    }

    public boolean getMoveToExchange() {
        return moveToExchange;
    }

    public boolean getBindToPosition() {
        return bindToPosition;
    }

    public String getOuterId() {
        return outerId;
    }

    public String getTitle() {
        return title;
    }

    public String getShortName() {
        return shortName;
    }

    public String getType() {
        return type;
    }

    public String getIntervalType() {
        return intervalType;
    }

    @Override
    public JSONObject getLinks() {
        return links;
    }

    @JsonIgnore
    public int getAlias() {
        String selfLink = getLinks().getJSONObject(SELF).getString(HREF);
        selfLink = selfLink.substring(selfLink.lastIndexOf("/") + 1);
        return Integer.parseInt(selfLink);
    }

    public int getId() {
        return id;
    }

    public ScheduleRequestAlias setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public ScheduleRequestAlias setAutoApprove(boolean autoApprove) {
        this.autoApprove = autoApprove;
        return this;
    }

    public ScheduleRequestAlias setRequireApproval(boolean requireApproval) {
        this.requireApproval = requireApproval;
        return this;
    }

    public ScheduleRequestAlias setOuterId(String outerId) {
        this.outerId = outerId;
        return this;
    }

    public ScheduleRequestAlias setTitle(String title) {
        this.title = title;
        return this;
    }

    public ScheduleRequestAlias setShortName(String shortName) {
        this.shortName = shortName;
        return this;
    }

    public ScheduleRequestAlias setType(String type) {
        this.type = type;
        return this;
    }

    public ScheduleRequestAlias setLinks(JSONObject links) {
        this.links = links;
        return this;
    }

    public ScheduleRequestAlias setMoveToExchange(boolean moveToExchange) {
        this.moveToExchange = moveToExchange;
        return this;
    }

    public ScheduleRequestAlias setBindToPosition(boolean bindToPosition) {
        this.bindToPosition = bindToPosition;
        return this;
    }

    public ScheduleRequestAlias copy() {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(objectMapper.writeValueAsString(this), ScheduleRequestAlias.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}

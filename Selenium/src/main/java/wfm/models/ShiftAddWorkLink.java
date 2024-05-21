package wfm.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.json.JSONObject;
import utils.LinksAnnotation;
import utils.Params;
import utils.Projects;
import utils.serialization.LocalDateTimeSerializer;
import utils.tools.CustomTools;
import wfm.HasLinks;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Objects;

import static utils.Params.HREF;
import static utils.tools.RequestFormers.getJsonFromUri;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ShiftAddWorkLink implements HasLinks {
    private Integer shiftId;
    private Integer workTypeId;
    private String workTypeTitle;
    private String shiftAddWorkStatus;
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime from;
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime to;
    @LinksAnnotation
    private JSONObject links;
    private Integer id;

    public ShiftAddWorkLink() {
    }

    public ShiftAddWorkLink(JSONObject json) {
        this.shiftId = json.getInt("shiftId");
        this.workTypeId = json.getInt("workTypeId");
        this.workTypeTitle = json.getString("workTypeTitle");
        shiftAddWorkStatus = json.optString("shiftAddWorkStatus");
        from = LocalDateTime.parse(json.getString("from"));
        to = LocalDateTime.parse(json.getString("to"));
        links = json.getJSONObject(Params.LINKS);
        id = json.getInt(Params.ID);
    }

    public Integer getShiftId() {
        return shiftId;
    }

    public ShiftAddWorkLink setShiftId(Integer shiftId) {
        this.shiftId = shiftId;
        return this;
    }

    public Integer getWorkTypeId() {
        return workTypeId;
    }

    public ShiftAddWorkLink setWorkTypeId(Integer workTypeId) {
        this.workTypeId = workTypeId;
        return this;
    }

    public String getWorkTypeTitle() {
        return workTypeTitle;
    }

    public String getShiftAddWorkStatus() {
        return shiftAddWorkStatus;
    }

    public ShiftAddWorkLink setShiftAddWorkStatus(String shiftAddWorkStatus) {
        this.shiftAddWorkStatus = shiftAddWorkStatus;
        return this;
    }

    public LocalDateTime getFrom() {
        return from;
    }

    public ShiftAddWorkLink setFrom(LocalDateTime from) {
        this.from = from;
        return this;
    }

    public LocalDateTime getTo() {
        return to;
    }

    public ShiftAddWorkLink setTo(LocalDateTime to) {
        this.to = to;
        return this;
    }

    public JSONObject getLinks() {
        return links;
    }

    public ShiftAddWorkLink setLinks(JSONObject links) {
        this.links = links;
        return this;
    }

    public Integer getId() {
        return id;
    }

    public ShiftAddWorkLink setId(Integer id) {
        this.id = id;
        return this;
    }

    @JsonIgnore
    public AdditionalWork getAdditionalWork() {
        URI uri = URI.create(links.getJSONObject("shiftAddWork").getString(HREF));
        JSONObject json = getJsonFromUri(Projects.WFM, uri);
        if (json.length() == 0) {
            return null;
        } else {
            return CustomTools.getClassObjectFromJson(AdditionalWork.class, json);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ShiftAddWorkLink that = (ShiftAddWorkLink) o;
        if (shiftAddWorkStatus != null && !shiftAddWorkStatus.equals("")) {
            return Objects.equals(that.getShiftAddWorkStatus(), this.getShiftAddWorkStatus())
                    && id.equals(that.id);
        } else {
            return id.equals(that.id);
        }
    }

    @Override
    public int hashCode() {
        return id;
    }
}

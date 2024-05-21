package wfm.models;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.json.JSONArray;
import org.json.JSONObject;
import utils.Projects;
import utils.deserialization.JSONObjectDeserializer;
import wfm.HasLinks;
import wfm.repository.CommonRepository;

import javax.annotation.Nonnull;
import java.net.URI;

import static utils.Links.EMPLOYEE;
import static utils.Links.REL_EMPLOYEE_POSITIONS;
import static utils.Params.*;
import static utils.tools.RequestFormers.getJsonFromUri;
import static utils.tools.RequestFormers.makePath;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Position implements EmployeeEssence, Comparable, HasLinks {

    private int positionCategoryId;
    private int id;
    @JsonDeserialize(using = JSONObjectDeserializer.class)
    @JsonAlias(LINKS)
    private JSONObject links;
    private String outerId;
    private String name;
    private DateInterval dateInterval;

    public Position(int positionCategoryId, int id, JSONObject links, String outerId, String name, DateInterval dateInterval) {
        this.positionCategoryId = positionCategoryId;
        this.id = id;
        this.links = links;
        this.outerId = outerId;
        this.name = name;
        this.dateInterval = dateInterval;
    }

    public Position() {
    }

    public Position(JSONObject jsonObject) {
        this.outerId = jsonObject.optString(OUTER_ID);
        this.name = jsonObject.getString(NAME);
        this.positionCategoryId = jsonObject.getInt("positionCategoryId");
        this.dateInterval = new DateInterval(jsonObject.optJSONObject(DATE_INTERVAL));
        this.id = jsonObject.getInt("id");
        this.links = jsonObject.getJSONObject(LINKS);
    }

    public String getLink(String name) {
        JSONObject tempObject = links.optJSONObject(name);
        return tempObject == null ? null : tempObject.optString(HREF);
    }

    public String getOuterId() {
        return outerId;
    }

    public String getName() {
        return name;
    }

    public int getPositionCategoryId() {
        return positionCategoryId;
    }

    public int getPositionGroupId() {
        String position = getLink(POSITION_GROUP);
        return position == null || position.isEmpty() ? 0 : Integer.parseInt(position.substring(position.lastIndexOf('/') + 1));
    }

    public int getPositionTypeId() {
        String position = getLink(POSITION_TYPE);
        return position == null || position.isEmpty() ? 0 : Integer.parseInt(position.substring(position.lastIndexOf('/') + 1));
    }

    public DateInterval getDateInterval() {
        return this.dateInterval;
    }

    public int getId() {
        return id;
    }

    @Override
    public Employee getEmployee() {
        String link = getLink(EMPLOYEE);
        if (link == null) {
            return null;
        }
        try {
            JSONObject json = getJsonFromUri(Projects.WFM, URI.create(link));
            if (json != null) {
                return new Employee(json);
            }
            return null;
        } catch (org.json.JSONException e) {
            return null;
        }
    }

    @Override
    public EmployeePosition getEmployeePosition() {
        String link = this.getLink(REL_EMPLOYEE_POSITIONS);
        if (link == null) return null;

        String url = link.split("\\{")[0];
        JSONObject json = getJsonFromUri(Projects.WFM, url);
        if (json == null) return null;

        JSONObject embedded = json.optJSONObject(EMBEDDED);
        if (embedded == null) return null;

        JSONArray jsonArray = embedded.optJSONArray(REL_EMPLOYEE_POSITIONS);
        if (jsonArray == null) return null;

        for (Object obj : jsonArray) {
            JSONObject position = ((JSONObject) obj).optJSONObject(EMBEDDED).optJSONObject(POSITION);
            if (position != null && position.optInt(ID) == this.id) {
                return new EmployeePosition((JSONObject) obj);
            }
        }
        return null;
    }

    @Override
    public Position getPosition() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Position position = (Position) o;
        return position.getId() == this.getId();
    }

    @Override
    public int compareTo(@Nonnull Object o) {
        if (this == o) {
            return 0;
        }
        if (getClass() != o.getClass()) {
            return -1;
        }
        Position position = (Position) o;
        return this.getDateInterval().startDate.compareTo(position.getDateInterval().startDate);
    }

    /**
     * обновление информации о позиции
     */
    public Position refreshPositions() {
        String urlEnding = makePath(POSITIONS, id);
        return new Position(getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, urlEnding));
    }

    public void setOuterId(String outerId) {
        this.outerId = outerId;
    }
}
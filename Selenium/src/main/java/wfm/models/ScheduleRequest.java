package wfm.models;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.json.JSONObject;
import utils.Links;
import utils.LinksAnnotation;
import utils.Projects;
import utils.serialization.LocalDateSerializer;
import wfm.HasLinks;
import wfm.components.schedule.ScheduleRequestStatus;
import wfm.components.schedule.ScheduleRequestType;
import wfm.repository.ScheduleRequestRepository;

import java.net.URI;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static utils.Links.*;
import static utils.Params.SCHEDULE_REQUEST_RULE;
import static utils.Params.*;
import static utils.tools.RequestFormers.getJsonFromUri;
import static utils.tools.RequestFormers.makePath;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ScheduleRequest implements HasLinks {
    private String type;
    @JsonIgnore
    private String title;
    private DateTimeInterval dateTimeInterval;
    private String status;
    @LinksAnnotation
    private JSONObject links;
    @JsonProperty(EMPLOYEE_ID)
    private int employeeId;
    @JsonProperty(EMPLOYEE_POSITION_ID)
    private int employeePositionId;
    private String url = Links.getTestProperty("release") + "/schedule-board";
    private String comment;
    @JsonIgnore
    private int id;
    private String aliasCode;
    private int positionId;
    private int rosterId;
    private String employeeOuterId;
    private String positionOuterId;
    private String startTime;
    private String endTime;

    public ScheduleRequest(JSONObject object) {
        this.type = object.getString(TYPE);
        this.title = object.getString(TITLE);
        this.dateTimeInterval = new DateTimeInterval((object.optJSONObject(DATE_TIME_INTERVAL)));
        this.status = object.getString(STATUS);
        this.links = object.optJSONObject(LINKS);
        this.employeeId = object.optInt(EMPLOYEE_ID);
        this.comment = object.optString("commentText");
        this.id = getSelfId();
        this.aliasCode = object.optString("aliasCode");
        this.positionId = object.optInt(POSITION_ID);
        this.employeePositionId = object.optInt(EMPLOYEE_POSITION_ID);
    }

    public ScheduleRequest() {
    }

    public ScheduleRequestType getType() {
        return Arrays.stream(ScheduleRequestType.values()).filter(t -> t.name().equals(type))
                .findFirst().orElseThrow(() -> new AssertionError("В енаме не было типа запроса с именем " + title));
    }

    public DateTimeInterval getDateTimeInterval() {
        return this.dateTimeInterval;
    }

    public ScheduleRequestStatus getStatus() {
        return ScheduleRequestStatus.valueOf(status);
    }

    @JsonIgnore
    public Employee getEmployee() {
        if (employeeId != 0) {
            return new Employee(getJsonFromUri(Projects.WFM, url, makePath(EMPLOYEES, employeeId)));
        }
        String link = links != null ? getLink(EMPLOYEE) : null;
        return link != null ? new Employee(getJsonFromUri(Projects.WFM, URI.create(link))) : null;
    }

    @JsonIgnore
    public ScheduleRequest getSelfRequest() {
        int requestId = getSelfId();
        try {
            return requestId != 0 ? new ScheduleRequest(getJsonFromUri(Projects.WFM, url,
                                                                       makePath(SCHEDULE_REQUESTS, requestId))) : null;
        } catch (AssertionError e) {
            return null;
        }
    }

    @JsonIgnore
    public JSONObject getRepeatRule() {
        JSONObject linkObject = getLinkWrappedInJson(SCHEDULE_REQUEST_RULE);
        return linkObject != null ? getJsonFromUri(Projects.WFM, URI.create(linkObject.getString(HREF))) : new JSONObject();
    }

    @JsonIgnore
    public boolean isRepeatable() {
        return !links.isNull(SCHEDULE_REQUEST_RULE);
    }

    public String getTitle() {
        return title;
    }

    public String getComment() {
        return comment;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ScheduleRequest scheduleRequest = (ScheduleRequest) o;
        return scheduleRequest.getDateTimeInterval().equals(this.dateTimeInterval)
                && scheduleRequest.employeeId == (this.employeeId)
                && scheduleRequest.status.equals(this.status)
                && scheduleRequest.title.equals(this.title)
                && scheduleRequest.type.equals(this.type)
                && scheduleRequest.url.equals(this.url);
    }

    /**
     * Обновляет информацию о запросе
     *
     * @param omId подразделение, в котором ищем запрос
     */
    public ScheduleRequest updateScheduleRequest(int omId) {
        ScheduleRequest newRequest = getSelfRequest();
        if (newRequest == null) {
            List<ScheduleRequest> requests = ScheduleRequestRepository.getScheduleRequests(omId, getDateTimeInterval().toDateInterval());
            return !requests.isEmpty() ? requests.get(0) : null;
        }
        return newRequest;
    }

    public ScheduleRequest setTitle(String title) {
        this.title = title;
        return this;
    }

    public ScheduleRequest setDateTimeInterval(DateTimeInterval dateTimeInterval) {
        this.dateTimeInterval = dateTimeInterval;
        return this;
    }

    public ScheduleRequest setStatus(String status) {
        this.status = status;
        return this;
    }

    public ScheduleRequest setLinks(JSONObject links) {
        this.links = links;
        return this;
    }

    public ScheduleRequest setEmployeeId(int employeeId) {
        this.employeeId = employeeId;
        return this;
    }

    public ScheduleRequest setUrl(String url) {
        this.url = url;
        return this;
    }

    public ScheduleRequest setComment(String comment) {
        this.comment = comment;
        return this;
    }

    public ScheduleRequest setType(String type) {
        this.type = type;
        return this;
    }

    @JsonIgnore
    public ScheduleRequest setAlias(ScheduleRequestAlias alias) {
        this.type = alias.getType();
        this.title = alias.getTitle();
        this.aliasCode = alias.getOuterId();
        return this;
    }

    public int getId() {
        return id;
    }

    public ScheduleRequest setId(int id) {
        this.id = id;
        return this;
    }

    public String getAliasCode() {
        return aliasCode;
    }

    public ScheduleRequest setAliasCode(String aliasCode) {
        this.aliasCode = aliasCode;
        return this;
    }

    @JsonProperty(END_DATE)
    @JsonGetter
    @JsonSerialize(using = LocalDateSerializer.class)
    public LocalDate getEndDate() {
        return dateTimeInterval.getEndDateTime().toLocalDate();
    }

    @JsonProperty(START_DATE)
    @JsonGetter
    @JsonSerialize(using = LocalDateSerializer.class)
    public LocalDate getStartDate() {
        return dateTimeInterval.getStartDateTime().toLocalDate();
    }

    public int getPositionId() {
        return this.positionId;
    }

    public ScheduleRequest setPositionId(int positionId) {
        this.positionId = positionId;
        return this;
    }

    @JsonProperty(ROSTER_ID_JSON)
    @JsonGetter
    public int getRosterId() {
        return this.rosterId;
    }

    @Override
    public JSONObject getLinks() {
        return links;
    }

    public ScheduleRequest setRosterId(int id) {
        this.rosterId = id;
        return this;
    }

    public int getEmployeePositionId() {
        return employeePositionId;
    }

    public ScheduleRequest setEmployeePositionId(int employeePositionId) {
        this.employeePositionId = employeePositionId;
        return this;
    }

    public String getEmployeeOuterId() {
        return employeeOuterId;
    }

    public ScheduleRequest setEmployeeOuterId(String employeeOuterId) {
        this.employeeOuterId = employeeOuterId;
        return this;
    }

    public String getPositionOuterId() {
        return positionOuterId;
    }

    public ScheduleRequest setPositionOuterId(String positionOuterId) {
        this.positionOuterId = positionOuterId;
        return this;
    }

    public String getStartTime() {
        return startTime;
    }

    public ScheduleRequest setStartTime(String startTime) {
        this.startTime = startTime;
        return this;
    }

    public String getEndTime() {
        return endTime;
    }

    public ScheduleRequest setEndTime(String endTime) {
        this.endTime = endTime;
        return this;
    }

}

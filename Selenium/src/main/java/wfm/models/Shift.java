package wfm.models;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.json.JSONArray;
import org.json.JSONObject;
import utils.LinksAnnotation;
import utils.Projects;
import utils.deserialization.LocalDateTimeFromTimeStampDeserializer;
import utils.serialization.LocalDateSerializer;
import utils.tools.CustomTools;
import utils.tools.RequestFormers;
import wfm.HasLinks;
import wfm.components.schedule.ScheduleRequestStatus;
import wfm.repository.CommonRepository;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static utils.Links.SHIFTS;
import static utils.Params.*;
import static utils.tools.CustomTools.getClassObjectFromJson;
import static utils.tools.RequestFormers.getJsonFromUri;
import static utils.tools.RequestFormers.makePath;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Shift implements Comparable, HasLinks {
    @JsonAlias("employee_position_id")
    private Integer employeePositionId;
    private DateTimeInterval dateTimeInterval;
    private DateInterval dateInterval;
    private TimeInterval timeInterval;
    @JsonAlias("startdatetime")
    @JsonDeserialize(using = LocalDateTimeFromTimeStampDeserializer.class)
    private LocalDateTime startDateTime;
    @JsonAlias("enddatetime")
    @JsonDeserialize(using = LocalDateTimeFromTimeStampDeserializer.class)
    private LocalDateTime endDateTime;
    private Integer id;
    @JsonProperty(STATUS)
    private String status;
    @JsonProperty(SUBTYPE)
    private String subtype;
    @JsonAlias("pos_cat_roster_id")
    private Integer positionCategoryRosterId;
    private ExchangeStatus exchangeStatus;
    private String hiringReasonText;
    private Integer lunch;
    @JsonProperty(COMMENT_TEXT)
    private String comment;
    private Boolean outstaff;
    @LinksAnnotation
    private JSONObject links;
    private PositionGroup positionGroup;
    private PositionCategory positionCategory;
    private int rosterId;
    private RepeatRule repeatRule;
    private String breaks;
    private String jobTitle;

    public Shift(JSONObject object) {
        this.employeePositionId = object.optInt(EMPLOYEE_POSITION_ID);
        this.dateInterval = object.optJSONObject(DATE_INTERVAL) != null ? new DateInterval(object.getJSONObject(DATE_INTERVAL)) : null;
        this.timeInterval = object.optJSONObject(TIME_INTERVAL) != null ? new TimeInterval(object.getJSONObject(TIME_INTERVAL)) : null;
        this.dateTimeInterval = object.optJSONObject(DATE_TIME_INTERVAL) != null ? new DateTimeInterval(object.getJSONObject(DATE_TIME_INTERVAL)) : null;
        this.id = object.getInt(ID);
        this.positionCategoryRosterId = object.optInt(POSITION_CATEGORY_ROSTER_ID);
        this.exchangeStatus = object.optJSONObject(EXCHANGE_STATUS) != null ? new ExchangeStatus(object.optJSONObject(EXCHANGE_STATUS)) : null;
        this.lunch = object.getInt(LUNCH);
        this.comment = object.optString(COMMENT_TEXT);
        this.links = object.getJSONObject(LINKS);
        this.hiringReasonText = object.optString(HIRING_REASON_TEXT);
        this.status = object.optString(STATUS);
        this.subtype = object.optString(SUBTYPE);
        this.outstaff = object.optBoolean(OUTSTAFF);
        this.positionGroup = object.optJSONObject(POSITION_GROUP) != null ? new PositionGroup(object.optJSONObject(POSITION_GROUP)) : null;
        this.positionCategory = object.optJSONObject(POSITION_CATEGORY) != null ? new PositionCategory(object.optJSONObject(POSITION_CATEGORY)) : null;
        this.breaks = object.optString(BREAKS);
        this.jobTitle = object.optString(JOB_TITLE);
    }

    public Shift() {
    }

    public Integer getEmployeePositionId() {
        return employeePositionId;
    }

    public Integer getId() {
        return id;
    }

    public DateTimeInterval getDateTimeInterval() {
        return this.dateTimeInterval;
    }

    public LocalDateTime getStartDateTime() {
        return startDateTime;
    }

    public LocalDateTime getEndDateTime() {
        return endDateTime;
    }

    public LocalTime getStartTime() {
        return this.dateTimeInterval.toTimeInterval().getStartTime();
    }

    public LocalTime getEndTime() {
        return this.dateTimeInterval.toTimeInterval().getEndTime();
    }

    public Integer getPositionCategoryRosterId() {
        return positionCategoryRosterId;
    }

    public PositionCategory getPositionCategory() {
        return positionCategory;
    }

    public PositionGroup getPositionGroup() {
        return positionGroup;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    @JsonIgnore
    public boolean isNextDayEnd() {
        return dateTimeInterval.getStartDate().plusDays(1).equals(dateTimeInterval.getEndDate());
    }

    public ExchangeStatus getExchangeStatus() {
        return exchangeStatus;
    }

    public Integer getLunch() {
        return lunch;
    }

    public String getComment() {
        return comment;
    }

    public String getHiringReasonText() {
        return hiringReasonText;
    }

    public Boolean getOutstaff() {
        return outstaff;
    }

    @JsonIgnore
    public PositionGroup getPosGroup() {
        JSONObject link = this.refreshShift().links.optJSONObject(POSITION_GROUP);
        if (link == null) {
            return null;
        }
        URI uri = URI.create(link.getString(HREF));
        JSONObject json = getJsonFromUri(Projects.WFM, uri);
        return getClassObjectFromJson(PositionGroup.class, json);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Shift shift = (Shift) o;
        boolean datesMatch = shift.getDateTimeInterval().equals(this.dateTimeInterval);
        boolean epsMatch = this.hasNoEmployee() && shift.hasNoEmployee() || Objects.equals(shift.getEmployeePositionId(), this.employeePositionId);
        boolean bool = datesMatch && epsMatch;
        if (shift.getOutstaff() != null) {
            bool = bool && Objects.equals(shift.getOutstaff(), this.getOutstaff());
        }
        if (shift.getJobTitle() != null && !shift.getJobTitle().equals("")) {
            bool = bool && Objects.equals(shift.getJobTitle(), this.getJobTitle());
        }
        if (shift.getHiringReasonText() != null && !shift.getHiringReasonText().equals("")) {
            bool = bool && Objects.equals(shift.getHiringReasonText(), this.hiringReasonText);
        }
        return bool;
    }

    private boolean hasNoEmployee() {
        return employeePositionId == null || employeePositionId == 0;
    }

    public boolean equalsWithId(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Shift shift = (Shift) o;

        return shift.getDateTimeInterval().equals(this.dateTimeInterval)
                && Objects.equals(shift.getEmployeePositionId(), this.employeePositionId)
                && Objects.equals(shift.getId(), this.id);
    }

    @Override
    public String toString() {
        return dateTimeInterval + ", ID сотрудника: " + employeePositionId;
    }

    /**
     * Проверка того, что смены существуют и имеют одинаковое время.
     * Вызывается после переноса смены от одного сотрудника к другому.
     *
     * @param expected смена после перемещения
     */
    public void assertSameTime(Shift expected) {
        if (expected == null) {
            throw new AssertionError("Перенесённая смена не была найдена в api");
        }
        if (!expected.getDateTimeInterval().toTimeInterval().equals(this.getDateTimeInterval().toTimeInterval())) {
            String assertionText = String.format("Временные промежутки не совпали. Ожидалось: %s. Было на самом деле: %s",
                                                 expected.getDateTimeInterval().toTimeInterval(), this.getDateTimeInterval().toTimeInterval());
            throw new AssertionError(assertionText);
        }
    }

    /**
     * Обновляет информацию о смене
     */
    public Shift refreshShift() {
        Shift newShift;
        try {
            newShift = new Shift(RequestFormers.getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, makePath(SHIFTS, getId())));
            return newShift;
        } catch (AssertionError e) {
            return null;
        }
    }

    @Override
    public int compareTo(Object o) {
        Shift shift = (Shift) o;
        return id - shift.getId();
    }

    @JsonIgnore
    public List<ShiftAddWorkLink> getAdditionalWork() {
        URI uri = URI.create(links.getJSONObject("shiftAddWorkLink").getString(HREF));
        JSONObject json = getJsonFromUri(Projects.WFM, uri);
        if (json.length() == 0) {
            return new ArrayList<>();
        } else {
            json = json.getJSONObject(EMBEDDED);
            JSONArray array = json.getJSONArray("shiftAddWorkLinkResList");
            return CustomTools.getListFromJsonArray(array, ShiftAddWorkLink.class);
        }
    }

    @Override
    public JSONObject getLinks() {
        return links;
    }

    public Shift setEmployeePositionId(Integer employeePositionId) {
        this.employeePositionId = employeePositionId;
        return this;
    }

    public Shift setDateTimeInterval(DateTimeInterval dateTimeInterval) {
        this.dateTimeInterval = dateTimeInterval;
        return this;
    }

    public Shift setId(Integer id) {
        this.id = id;
        return this;
    }

    @JsonIgnore
    public Shift setDate(LocalDate newDate) {
        if (!dateTimeInterval.getStartDate().equals(newDate)) {
            LocalDate endDate;
            if (dateTimeInterval.getEndDate().equals(dateTimeInterval.getStartDate().plusDays(1))) {
                endDate = newDate.plusDays(1);
            } else {
                endDate = newDate;
            }
            LocalDateTime start = LocalDateTime.of(newDate, dateTimeInterval.getStartDateTime().toLocalTime());
            LocalDateTime end = LocalDateTime.of(endDate, dateTimeInterval.getEndDateTime().toLocalTime());
            this.dateTimeInterval = new DateTimeInterval(start, end);
        }
        return this;
    }

    public Shift setLinks(JSONObject links) {
        this.links = links;
        return this;
    }

    public Shift setStatus(ScheduleRequestStatus status) {
        if (status != null) {
            this.status = status.name();
        }
        return this;
    }

    @JsonIgnore
    public Shift setStatusIfNull(ScheduleRequestStatus status) {
        if (status != null && (this.status == null || this.status.equals(""))) {
            this.status = status.name();
        }
        return this;
    }

    public Shift setPositionCategoryRosterId(Integer positionCategoryRosterId) {
        this.positionCategoryRosterId = positionCategoryRosterId;
        return this;
    }

    public String getStatus() {
        return status;
    }

    public String getSubtype() {
        return subtype;
    }

    public Shift setDateTimeInterval() {
        this.dateTimeInterval = new DateTimeInterval(startDateTime, endDateTime);
        return this;
    }

    public Shift setOutstaff(Boolean outstaff) {
        this.outstaff = outstaff;
        return this;
    }

    public Shift setPositionGroup(PositionGroup posGroup) {
        this.positionGroup = posGroup;
        return this;
    }

    public Shift setPositionCategory(PositionCategory posCat) {
        this.positionCategory = posCat;
        return this;
    }

    public Shift setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
        return this;
    }

    public RepeatRule getRepeatRule() {
        return repeatRule;
    }

    public Shift setRepeatRule(RepeatRule repeatRule) {
        this.repeatRule = repeatRule;
        return this;
    }

    public int getRosterId() {
        return this.rosterId;
    }

    public Shift setRosterId(int rosterId) {
        this.rosterId = rosterId;
        return this;
    }

    public Shift setHiringReasonText(String text) {
        this.hiringReasonText = text;
        return this;
    }

    @JsonSerialize(using = LocalDateSerializer.class)
    public LocalDate getStartDate() {
        return dateTimeInterval.getStartDate();
    }

    @JsonSerialize(using = LocalDateSerializer.class)
    public LocalDate getEndDate() {
        return dateTimeInterval.getEndDate();
    }

    @JsonGetter
    public DateInterval getDateInterval() {
        if (isRepeatedFreeShift()) {
            return new DateInterval(getStartDate(), repeatRule.getEndDate());
        } else {
            return null;
        }
    }

    @JsonGetter
    public TimeInterval getTimeInterval() {
        if (isRepeatedFreeShift()) {
            return dateTimeInterval.toTimeInterval();
        } else {
            return null;
        }
    }

    @JsonGetter
    public String getRepeat() {
        if (isRepeatedFreeShift()) {
            return repeatRule.getPeriodicity();
        } else {
            return null;
        }
    }

    private boolean isRepeatedFreeShift() {
        return repeatRule != null || (links != null && links.optJSONObject("exchangeRule") != null);
    }

    /**
     * Получить перерыв в минутах
     */
    public Long getBreaks() {
        if (Objects.isNull(breaks) || breaks.isEmpty() || breaks.equals("[]")) {
            return null;
        }
        JSONArray jsonArray = new JSONArray(breaks);
        JSONObject jsonObject = jsonArray.getJSONObject(0);
        LocalTime startTime = LocalTime.parse(jsonObject.getString("startTime"));
        LocalTime endTime = LocalTime.parse(jsonObject.getString("endTime"));
        return Duration.between(startTime, endTime).toMinutes();
    }

    /**
     * Получить список всех дат в заданном промежутке startDateTime и endDateTime
     */
    public List<LocalDate> getAllDatesInInterval() {
        List<LocalDate> dateList = new ArrayList<>();
        LocalDate currentDate = getStartDate();
        LocalDate endDate = getEndDate();
        while (!currentDate.isAfter(endDate)) {
            dateList.add(currentDate);
            currentDate = currentDate.plusDays(1);
        }
        return dateList;
    }
}

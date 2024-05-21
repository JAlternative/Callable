package wfm.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.json.JSONObject;
import utils.LinksAnnotation;
import utils.serialization.LocalDateSerializer;
import wfm.HasLinks;
import wfm.components.systemlists.LimitType;

import java.time.LocalDate;

import static utils.Params.*;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class Limits implements HasLinks {

    private int limit;
    private int id;
    private int limitDays;
    private int orgUnitId;
    private String orgUnitName;
    private String orgType;
    private String limitType;
    @JsonSerialize(using = LocalDateSerializer.class)
    private LocalDate from;
    @JsonSerialize(using = LocalDateSerializer.class)
    private LocalDate to;
    private String period;
    private int positionGroupId;
    private String positionGroupName;
    private int jobTitleId;
    private String jobTitleName;
    private boolean calculateByRate;
    private boolean hasJobTitle;
    private boolean hasOrgUnit;
    private boolean hasPositionGroup;
    @LinksAnnotation
    private JSONObject links;

    public Limits() {

    }

    public Limits(LimitType type, boolean hasJobTitle, boolean hasOrgUnit, boolean hasPositionGroup, Integer limit, Integer limitDays) {
        this.limitType = type.toString();
        this.hasJobTitle = hasJobTitle;
        this.hasOrgUnit = hasOrgUnit;
        this.hasPositionGroup = hasPositionGroup;
        if (limit != null) {
            this.limit = limit;
        }
        if (limitDays != null) {
            this.limitDays = limitDays;
        }
        this.orgType = "СВП";
    }

    public Limits(JSONObject jsonObject) {
        this.links = jsonObject.getJSONObject(LINKS);
        this.orgUnitId = jsonObject.optInt(ORG_UNIT_ID);
        this.orgType = jsonObject.getString(ORG_TYPE);
        String from = jsonObject.optString(FROM);
        String to = jsonObject.optString(TO);
        this.from = from != "" ? LocalDate.parse(from) : null;
        this.to = to != "" ? LocalDate.parse(to) : null;
        this.period = jsonObject.optString(PERIOD);
        this.limitType = jsonObject.getString(LIMIT_TYPE);
        this.limit = jsonObject.getInt(LIMIT);
        this.positionGroupId = jsonObject.optInt(POSITION_GROUP_ID);
        this.positionGroupName = jsonObject.optString("positionGroupName");
        this.jobTitleId = jsonObject.optInt(JOB_TITLE_ID);
        this.jobTitleName = jsonObject.optString("jobTitleName");
    }

    public String getLimitType() {
        return limitType;
    }

    public int getLimit() {
        return limit;
    }

    public int getPositionGroupId() {
        return positionGroupId;
    }

    public int getOrgUnitId() {
        return orgUnitId;
    }

    public Limits setLimit(int limit) {
        this.limit = limit;
        return this;
    }

    public int getId() {
        return id;
    }

    public Limits setId(Integer id) {
        this.id = id;
        return this;
    }

    public int getLimitDays() {
        return limitDays;
    }

    public Limits setLimitDays(int limitDays) {
        this.limitDays = limitDays;
        return this;
    }

    public Limits setOrgUnitId(int orgUnitId) {
        this.orgUnitId = orgUnitId;
        return this;
    }

    public String getOrgUnitName() {
        return orgUnitName;
    }

    public Limits setOrgUnitName(String orgUnitName) {
        this.orgUnitName = orgUnitName;
        return this;
    }

    public String getOrgType() {
        return orgType;
    }

    public Limits setOrgType(String orgType) {
        this.orgType = orgType;
        return this;
    }

    public Limits setLimitType(String limitType) {
        this.limitType = limitType;
        return this;
    }

    public LocalDate getFrom() {
        return from;
    }

    public Limits setFrom(LocalDate from) {
        this.from = from;
        return this;
    }

    public LocalDate getTo() {
        return to;
    }

    public Limits setTo(LocalDate to) {
        this.to = to;
        return this;
    }

    public String getPeriod() {
        return period;
    }

    public Limits setPeriod(String period) {
        this.period = period;
        return this;
    }

    public Limits setPositionGroupId(int positionGroupId) {
        this.positionGroupId = positionGroupId;
        return this;
    }

    @JsonIgnore
    public Limits setPositionGroup(PositionGroup positionGroup) {
        this.positionGroupId = positionGroup.getId();
        this.positionGroupName = positionGroup.getName();
        return this;
    }

    @JsonIgnore
    public Limits setJobTitle(JobTitle jobTitle) {
        this.jobTitleId = jobTitle.getId();
        this.jobTitleName = jobTitle.getFullName();
        return this;
    }

    @JsonIgnore
    public Limits setOrgUnit(OrgUnit orgUnit) {
        this.orgUnitId = orgUnit.getId();
        this.orgUnitName = orgUnit.getName();
        return this;
    }

    public String getPositionGroupName() {
        return positionGroupName;
    }

    public Limits setPositionGroupName(String positionGroupName) {
        this.positionGroupName = positionGroupName;
        return this;
    }

    public int getJobTitleId() {
        return jobTitleId;
    }

    public Limits setJobTitleId(int jobTitleId) {
        this.jobTitleId = jobTitleId;
        return this;
    }

    public String getJobTitleName() {
        return jobTitleName;
    }

    public Limits setJobTitleName(String jobTitleName) {
        this.jobTitleName = jobTitleName;
        return this;
    }

    public boolean isCalculateByRate() {
        return calculateByRate;
    }

    public Limits setCalculateByRate(boolean calculateByRate) {
        this.calculateByRate = calculateByRate;
        return this;
    }

    @Override
    public JSONObject getLinks() {
        return links;
    }

    public Limits setLinks(JSONObject links) {
        this.links = links;
        return this;
    }

    @JsonIgnore
    public boolean isHasJobTitle() {
        return hasJobTitle;
    }

    @JsonIgnore
    public boolean isHasOrgUnit() {
        return hasOrgUnit;
    }

    @JsonIgnore
    public boolean isHasPositionGroup() {
        return hasPositionGroup;
    }

}

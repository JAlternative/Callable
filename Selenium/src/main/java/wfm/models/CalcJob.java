package wfm.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.json.JSONObject;
import utils.LinksAnnotation;
import wfm.HasLinks;

import static utils.Params.*;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CalcJob implements HasLinks {
    private int id;
    private boolean hasError;
    private String createTime;
    private String startTime;
    private String endTime;
    private int orgUnitId;
    private String type;
    private String status;
    private int jobBatchId;
    private String parameters;
    private String instanceName;
    @LinksAnnotation
    private JSONObject links;

    public CalcJob(JSONObject json) {
        this.id = json.getInt(ID);
        this.hasError = json.getBoolean("hasError");
        this.createTime = json.getString("createTime");
        this.startTime = json.optString(START_TIME);
        this.endTime = json.optString(END_TIME);
        this.orgUnitId = json.getInt(ORG_UNIT_ID);
        this.type = json.getString(TYPE);
        this.status = json.getString(STATUS);
        this.jobBatchId = json.optInt("jobBatchId");
        this.links = json.getJSONObject(LINKS);
        this.parameters = json.optString("parameters");
        this.instanceName = json.optString("instanceName");
    }

    public CalcJob() {

    }

    public int getId() {
        return id;
    }

    public CalcJob setId(int id) {
        this.id = id;
        return this;
    }

    public boolean hasError() {
        return hasError;
    }

    public CalcJob setHasError(boolean hasError) {
        this.hasError = hasError;
        return this;
    }

    public String getCreateTime() {
        return createTime;
    }

    public CalcJob setCreateTime(String createTime) {
        this.createTime = createTime;
        return this;
    }

    public String getStartTime() {
        return startTime;
    }

    public CalcJob setStartTime(String startTime) {
        this.startTime = startTime;
        return this;
    }

    public String getEndTime() {
        return endTime;
    }

    public CalcJob setEndTime(String endTime) {
        this.endTime = endTime;
        return this;
    }

    public int getOrgUnitId() {
        return orgUnitId;
    }

    public CalcJob setOrgUnitId(int orgUnitId) {
        this.orgUnitId = orgUnitId;
        return this;
    }

    public String getType() {
        return type;
    }

    public CalcJob setType(String type) {
        this.type = type;
        return this;
    }

    public String getStatus() {
        return status;
    }

    public CalcJob setStatus(String status) {
        this.status = status;
        return this;
    }

    public int getJobBatchId() {
        return jobBatchId;
    }

    public CalcJob setJobBatchId(int jobBatchId) {
        this.jobBatchId = jobBatchId;
        return this;
    }

    @Override
    public JSONObject getLinks() {
        return links;
    }

    public CalcJob setLinks(JSONObject links) {
        this.links = links;
        return this;
    }

    public String getParameters() {
        return parameters;
    }

    public CalcJob setParameters(String parameters) {
        this.parameters = parameters;
        return this;
    }

    public String getInstanceName() {
        return instanceName;
    }
}

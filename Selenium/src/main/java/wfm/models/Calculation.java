package wfm.models;

import org.json.JSONObject;
import wfm.components.calculation.CalculationStatus;

public class Calculation {
    private final int organizationUnitId;
    private final String organizationUnitName;
    private final boolean executing;
    private final boolean completed;
    private final boolean successful;
    private final JSONObject error;
    private final JSONObject errorStackTrace;

    public Calculation(JSONObject json) {
        this.organizationUnitId = json.getInt("organizationUnitId");
        this.organizationUnitName = json.optString("organizationUnitName");
        this.executing = json.getBoolean("executing");
        this.completed = json.getBoolean("completed");
        this.successful = json.getBoolean("successful");
        this.error = json.optJSONObject("error");
        this.errorStackTrace = json.optJSONObject("errorStackTrace");
    }

    public int getOrganizationUnitId() {
        return organizationUnitId;
    }

    public boolean isExecuting() {
        return executing;
    }

    public boolean isCompleted() {
        return completed;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public JSONObject getError() {
        return error;
    }

    public CalculationStatus getStatus() {
        if (isExecuting()) {
            return CalculationStatus.EXECUTING;
        } else if (isCompleted()) {
            if (isSuccessful()) {
                return CalculationStatus.SUCCESSFUL;
            } else {
                return CalculationStatus.ERROR;
            }
        } else {
            return CalculationStatus.OTHER;
        }
    }

    @Override
    public String toString() {
        return String.format("%s (id %s): %s", organizationUnitName, organizationUnitId, getStatus());
    }
}
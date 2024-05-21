package wfm.models;

import org.json.JSONObject;
import wfm.components.analytics.KpiType;

import static utils.Params.*;

public class KpiList {
    private final int kpiId;
    private final String outerId;
    private final String name;
    private final String timeUnit;
    private final boolean percentage;

    public KpiList(JSONObject jsonObject) {
        this.kpiId = jsonObject.getInt(KPI_ID);
        this.outerId = jsonObject.getString(OUTER_ID);
        this.name = jsonObject.getString(NAME);
        this.timeUnit = jsonObject.getString("timeUnit");
        this.percentage = jsonObject.getBoolean("percentage");
    }

    public int getKpiId() {
        return kpiId;
    }

    public String getOuterId() {
        return outerId;
    }

    public String getName() {
        return name;
    }

    public boolean isPercentage() {
        return percentage;
    }

    public KpiType getKpiType() {
        return KpiType.getValue(name);
    }

    public String getTimeUnit() {
        return timeUnit;
    }
}

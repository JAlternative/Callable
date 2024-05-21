package wfm.models;

import org.json.JSONObject;

import java.time.LocalDateTime;

public class Kpi {
    private final Double value;
    private final Double srcValue;
    private final Double delta;
    private final String datetime;
    private int omId;

    public Kpi(JSONObject object) {
        this.value = object.getDouble("value");
        this.srcValue = object.getDouble("srcValue");
        this.delta = object.getDouble("delta");
        this.datetime = object.getString("datetime");
    }

    public Double getValue() {
        return value;
    }

    public Double getSrcValue() {
        return srcValue;
    }

    public Double getDelta() {
        return delta;
    }

    public LocalDateTime getDateTime() {
        return LocalDateTime.parse(datetime);
    }

    public int getOmId() {
        return omId;
    }

    public void setOmId(int omId) {
        this.omId = omId;
    }

    public String getValuesToString() {
        return "\nЗначение: " + value +
                "\nКоррекция: " + delta +
                "\nРазница: " + srcValue +
                "\nВремя: " + getDateTime();
    }
}

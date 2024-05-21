package wfm.models;

import org.json.JSONObject;
import utils.Params;
import wfm.HasLinks;

import java.util.Random;

import static utils.Params.*;

public class ExchangeRule implements HasLinks {
    private final String employeeValue;
    private final String shiftValue;
    private final int id;
    private final boolean outside;
    private final JSONObject links;

    public ExchangeRule(JSONObject json) {
        id = json.getInt(Params.ID);
        employeeValue = json.getString(EMPLOYEE_VALUE);
        shiftValue = json.getString(SHIFT_VALUE);
        outside = json.getBoolean(OUTSIDE);
        links = json.getJSONObject(LINKS);
    }

    public ExchangeRule(String value, boolean outside) {
        employeeValue = value;
        shiftValue = value;
        this.outside = outside;
        id = new Random().nextInt(6);
        links = null;
    }

    public int getId() {
        return id;
    }

    public String getEmployeeValue() {
        return employeeValue;
    }

    public String getShiftValue() {
        return shiftValue;
    }

    public boolean getBoolean() {
        return outside;
    }

    @Override
    public JSONObject getLinks() {
        return links;
    }
}

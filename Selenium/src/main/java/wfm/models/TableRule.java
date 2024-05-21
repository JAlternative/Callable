package wfm.models;

import org.json.JSONArray;
import org.json.JSONObject;
import utils.Params;
import wfm.HasLinks;

import static utils.Params.*;

public class TableRule implements HasLinks {
    private final int deepEdit;
    private final JSONArray fixedDays;
    private final JSONObject timeEdit;
    private final String value;
    private final int exchangeDaysToEdit;
    private final JSONObject links;
    private final int id;
    private final int orgUnitId;
    private final String orgUnitName;

    public TableRule(JSONObject json) {
        deepEdit = json.optInt(DEEP_EDIT);
        fixedDays = json.optJSONArray(FIXED_FAYS);
        timeEdit = json.optJSONObject(TIME_EDIT);
        value = json.getString(VALUE);
        exchangeDaysToEdit = json.optInt(EXCHANGE_DAYS_TO_EDIT);
        links = json.getJSONObject(LINKS);
        id = json.get(Params.ID).toString().contains("null") ?
                Integer.parseInt(links.toString().substring(links.toString().lastIndexOf('/') + 1, links.toString().lastIndexOf('"'))) : json.getInt(Params.ID);
        orgUnitId = json.get(ORG_UNIT_ID).toString().contains("null") ? 0 : json.getInt(ORG_UNIT_ID);
        orgUnitName = json.get(ORG_UNIT_NAME).toString().contains("null") ? null : json.getString(ORG_UNIT_NAME);
    }

    public int getDeepEdit() {
        return deepEdit;
    }

    public JSONArray getFixedDays() {
        return fixedDays;
    }

    public JSONObject getTimeEdit() {
        return timeEdit;
    }

    public String getValue() {
        return value;
    }

    public int getExchangeDaysToEdit() {
        return exchangeDaysToEdit;
    }

    @Override
    public JSONObject getLinks() {
        return links;
    }

    public int getId() {
        return id;
    }

    public int getOrgUnitId() {
        return orgUnitId;
    }

    public String getOrgUnitName() {
        return orgUnitName;
    }

}

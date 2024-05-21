package wfm.models;

import org.json.JSONObject;
import utils.Params;
import wfm.HasLinks;

/**
 * Атрибут подразделения без привязки к конкретному подразделению
 */
public class EntityPropertiesKey implements HasLinks {

    private final String key;
    private final String title;
    private final String dataType;
    private final String display;
    private final boolean forCalculate;
    private final JSONObject links;

    public EntityPropertiesKey(JSONObject jsonObject) {
        key = jsonObject.getString(Params.KEY);
        title = jsonObject.getString(Params.TITLE);
        dataType = jsonObject.getString("dataType");
        display = jsonObject.optString("display");
        forCalculate = jsonObject.optBoolean("forCalculate");
        links = jsonObject.getJSONObject(Params.LINKS);
    }

    public String getKey() {
        return key;
    }

    public String getTitle() {
        return title;
    }

    public String getDataType() {
        return dataType;
    }

    public String getDisplay() {
        return display;
    }

    public boolean isForCalculate() {
        return forCalculate;
    }

    public JSONObject getLinks() {
        return links;
    }
}

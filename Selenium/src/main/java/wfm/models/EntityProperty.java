package wfm.models;

import org.json.JSONObject;
import wfm.HasLinks;

import static utils.Params.*;

/**
 * Атрибут подразделения в привязке к конкретному подразделению
 */
public class EntityProperty<T> implements HasLinks {
    private final String propKey;
    private final String title;
    private final String type;
    private final String display;
    private T value;
    private final JSONObject links;

    public EntityProperty(JSONObject object) {
        this.propKey = object.getString(PROP_KEY);
        this.title = object.getString(TITLE);
        this.display = object.optString(DISPLAY);
        this.value = (T) object.get(VALUE);
        this.type = object.getString(TYPE);
        this.links = object.getJSONObject(LINKS);
    }

    public String getPropKey() {
        return propKey;
    }

    public String getTitle() {
        return title;
    }

    public String getDisplay() {
        return display;
    }

    public T getValue() {
        return value;
    }

    public String getType() {
        return type;
    }

    public JSONObject getLinks() {
        return links;
    }

    public void setValue(T value) {
        this.value = value;
    }

}

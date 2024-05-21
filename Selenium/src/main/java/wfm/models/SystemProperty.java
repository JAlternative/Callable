package wfm.models;

import org.json.JSONObject;
import wfm.components.schedule.SystemProperties;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Arrays;

import static utils.Params.*;

public class SystemProperty <T> {
    private final boolean enabled;
    private final String key;
    private final String title;
    private final String description;
    private final Class <T> dataType;
    private final JSONObject links;
    private final String type;

    private final T value;
    private final String created;
    private final String updated;

    public SystemProperty(JSONObject jsonObject) {
        this.enabled = jsonObject.getBoolean(ENABLED);
        this.key = jsonObject.getString(KEY);
        this.title = jsonObject.optString(TITLE);
        this.description = jsonObject.optString(DESCRIPTION);
        this.dataType = determineDataType(jsonObject.optString(DATA_TYPE));
        this.links = jsonObject.getJSONObject(LINKS);
        this.type = jsonObject.optString(TYPE);
        this.value = jsonObject.optString(VALUE).equals("") ? null : (T) convertValue(jsonObject.optString(VALUE), this.dataType);
        this.created = jsonObject.optString("created");
        this.updated = jsonObject.optString("updated");
    }

    public <V> V getValue() {
        return (V) value;
    }

    public LocalDateTime getCreated() {
        return LocalDateTime.parse(created);
    }

    public LocalDateTime getUpdated() {
        return LocalDateTime.parse(updated);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getKey() {
        return key;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Class getDataType() {
        return dataType;
    }

    public SystemProperties getSystemProperties() {
        SystemProperties[] values = SystemProperties.values();
        return Arrays.stream(values)
                .filter(systemProperties -> systemProperties.getKey().contains(key))
                .findFirst()
                .orElse(null);
    }

    public JSONObject getLinks() {
        return this.links;
    }

    public URI getSelfLink() {
        return URI.create(links.getJSONObject(SELF).getString(HREF));
    }

    public String getType() {
        return type;
    }

    private Class determineDataType(String type) {
        switch (type) {
            case "BOOL":
                return Boolean.class;
            case "DBL":
                return Double.class;
            case "INT":
                return Integer.class;
            default:
                return String.class;
        }
    }

    private <V> Object convertValue(String value, Class <V> tClass) {
        if (tClass.equals(Boolean.class)) {
            return Boolean.parseBoolean(value);
        } else if (tClass.equals(Double.class)) {
            return Double.parseDouble(value);
        } else if (tClass.equals(Integer.class)) {
            return Integer.parseInt(value);
        }
        return value;
    }
}

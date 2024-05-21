package wfm;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.json.JSONObject;

import java.lang.reflect.Field;

import static utils.Params.HREF;
import static utils.Params.SELF;

public interface HasLinks {
    JSONObject links = new JSONObject();

    @JsonIgnore
    default JSONObject getLinks() {
        Field field;
        try {
            field = this.getClass().getDeclaredField("links");
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        field.setAccessible(true);
        JSONObject value;
        try {
            value = (JSONObject) field.get(this);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return value;
    }

    @JsonIgnore
    default String getSelfLink() {
        return getLinks().getJSONObject(SELF).getString(HREF);
    }

    @JsonIgnore
    default String getSelfPath() {
        return getPath(SELF);
    }

    @JsonIgnore
    default String getLink(String linkName) {
        JSONObject o = getLinks().optJSONObject(linkName);
        return o == null ? null : o.getString(HREF);
    }

    /**
     * Возвращает окончание заданной ссылки (все, что после api/v1/)
     */
    @JsonIgnore
    default String getPath(String linkName) {
        String o = getLink(linkName);
        return o == null ? null: o.replaceFirst("^.*/api/v\\d/", "");
    }

    /**
     * Возвращает айди объекта, полученный из его self-ссылки
     */
    @JsonIgnore
    default int getSelfId() {
        try {
            return Integer.parseInt(getSelfPath().replaceAll("^.*/", ""));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    @JsonIgnore
    default JSONObject getLinkWrappedInJson(String linkName) {
        return new JSONObject().put(HREF, getLink(linkName));
    }
}

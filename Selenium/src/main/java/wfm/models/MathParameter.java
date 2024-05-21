package wfm.models;

import org.json.JSONArray;
import org.json.JSONObject;
import utils.tools.CustomTools;
import wfm.HasLinks;

import javax.annotation.Nonnull;
import java.util.List;

import static utils.Params.*;

public class MathParameter implements HasLinks {
    private final String type;
    private final String outerId;
    private final String name;
    private final String commonName;
    private final String shortName;
    private final String entity;
    private final boolean hidden;
    private final JSONObject links;
    private final List<MathValue> mathValues;
    private final String viewType;

    public MathParameter(JSONObject jsonObject) {
        this.type = jsonObject.getString(TYPE);
        this.outerId = jsonObject.getString(OUTER_ID);
        this.name = jsonObject.getString(NAME);
        this.entity = jsonObject.getString("entity");
        this.commonName = jsonObject.getString("commonName");
        this.shortName = jsonObject.getString("shortName");
        this.hidden = jsonObject.getBoolean("hidden");
        this.links = jsonObject.getJSONObject(LINKS);
        this.mathValues = getMathValues(jsonObject.optJSONArray("values"));
        this.viewType = jsonObject.optString("viewType");
    }

    public String getType() {
        return type;
    }

    public String getOuterId() {
        return outerId;
    }

    public String getName() {
        return name;
    }

    public String getCommonName() {
        return commonName;
    }

    public int getMathParameterId() {
        String link = links.getJSONObject(SELF).getString(HREF);
        return Integer.parseInt(link.substring(link.lastIndexOf("/") + 1));
    }

    public String getShortName() {
        return shortName;
    }

    public boolean isHidden() {
        return hidden;
    }

    public String getEntity() {
        return entity;
    }

    public List<MathValue> getMathValues() {
        return mathValues;
    }

    @Override
    public JSONObject getLinks() {
        return links;
    }

    private List<MathValue> getMathValues(JSONArray jsonArray) {
        return CustomTools.getListFromJsonArray(jsonArray, MathValue.class);
    }

    public String getViewType() {
        return viewType;
    }

    public static class MathValue {

        private final String value;
        private final String name;

        public MathValue(@Nonnull JSONObject jsonObject) {
            this.value = jsonObject.optString("value");
            this.name = jsonObject.optString("name");
        }

        public String getValue() {
            return value;
        }

        public String getName() {
            return name;
        }
    }
}

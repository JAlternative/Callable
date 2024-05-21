package wfm.models;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.json.JSONObject;
import utils.LinksAnnotation;
import wfm.HasLinks;

import static utils.Params.*;

/**
 * На UI называется "Категория должности"
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PositionCategory implements HasLinks {

    private final int categoryId;
    private final String name;
    private final String outerId;
    private final String calculationMode;
    @LinksAnnotation
    private JSONObject links;

    public PositionCategory(JSONObject object) {
        this.categoryId = object.getInt(CATEGORY_ID);
        this.name = object.getString(NAME);
        this.outerId = object.optString(OUTER_ID);
        this.calculationMode = object.getString(CALCULATION_MODE);
        this.links = object.getJSONObject(LINKS);
    }

    public int getCategoryId() {
        return categoryId;
    }

    public String getName() {
        return name;
    }

    public String getOuterId() {
        return outerId;
    }

    public String getCalculationMode() {
        return calculationMode;
    }

    public JSONObject getLinks() {
        return links;
    }
    public PositionCategory setLinks(JSONObject links) {
        this.links = links;
        return this;
    }

    @JsonGetter()
    private Object getForExchange() {
        return null;
    }
}

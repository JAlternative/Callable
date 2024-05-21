package wfm.models;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.json.JSONObject;
import utils.Params;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ExchangeStatus {
    private int id;
    private String outerId;
    private String title;
    private String shortName;
    private boolean allowClose;
    private boolean allowDelete;
    @JsonAlias("new")
    private boolean newField;

    public ExchangeStatus(JSONObject jsonObject) {
        id = jsonObject.getInt(Params.ID);
        outerId = jsonObject.getString(Params.OUTER_ID);
        title = jsonObject.getString(Params.TITLE);
        shortName = jsonObject.optString(Params.SHORT_NAME);
        allowClose = jsonObject.optBoolean("allowClose");
        allowDelete = jsonObject.optBoolean("allowDelete");
        newField = jsonObject.optBoolean("new");
    }

    public ExchangeStatus() {
    }

    public int getId() {
        return id;
    }

    public String getOuterId() {
        return outerId;
    }

    public String getTitle() {
        return outerId;
    }

}

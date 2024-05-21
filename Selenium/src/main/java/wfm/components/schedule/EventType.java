package wfm.components.schedule;

import org.json.JSONObject;

import static utils.Params.*;

public class EventType {
    private final String name;
    private final String shortName;
    private final String outerId;
    private final int id;
    private final JSONObject links;

    public EventType (JSONObject json) {
        this.name = json.getString(NAME);
        this.shortName = json.getString(SHORT_NAME);
        this.outerId = json.getString(OUTER_ID);
        this.links = json.getJSONObject(LINKS);
        this.id = json.getInt(ID);
    }

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }

    public String toString() {
        return this.name;
    }

}

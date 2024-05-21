package wfm.models;

import org.json.JSONObject;

import static utils.Params.*;

public class BusinessDays {
    private String dayOfWeek;
    private TimeInterval timeInterval;
    private int isoWeekday;
    private JSONObject links;

    public BusinessDays(JSONObject jsonObject) {
        this.timeInterval = new TimeInterval(jsonObject.getJSONObject(TIME_INTERVAL));
        this.isoWeekday = jsonObject.getInt(ISO_WEEK_DAY);
        this.dayOfWeek = jsonObject.getString("dayOfWeek");
        this.links = jsonObject.getJSONObject(LINKS);
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public int getIsoWeekday() {
        return isoWeekday;
    }

    public TimeInterval getTimeInterval() {
        return timeInterval;
    }

    public String getLink() {
        String link = links.optJSONObject(SELF).getString(HREF);
        return link.substring(0,link.lastIndexOf("/"));
    }

}

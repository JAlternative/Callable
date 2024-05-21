package wfm.models;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.Duration;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static utils.Params.*;
import static utils.Params.HREF;

public class LunchRule {
    private final String type;
    private final String commonName;
    private final String outerId;
    private final String entity;
    private final String value;
    private final JSONObject links;

    public LunchRule(JSONObject json) {
        type = json.getString(TYPE);
        commonName = json.getString(COMMON_NAME);
        outerId = json.getString(OUTER_ID);
        entity = json.getString("entity");
        value = json.getString(VALUE);
        links = json.getJSONObject(LINKS);
    }

    public String getCommonName() {
        return commonName;
    }

    public String getOuterId() {
        return outerId;
    }

    public String getValue() {
        return value;
    }

    public List<LunchTime> getLunchList() {
        JSONArray jsonArray = new JSONArray(value.replace("/", ""));
        List<LunchTime> lunchTimes = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject temp = jsonArray.getJSONObject(i);
            lunchTimes.add(new LunchTime(temp));
        }
        return lunchTimes;
    }

    public int getLunchTime(LocalTime from, LocalTime to) {
        List<LunchTime> lunchTimeList = getLunchList();
        for (LunchTime lunchTime : lunchTimeList) {
            long shiftFrom = ChronoUnit.HOURS.between(LocalTime.parse("00:00"), LocalTime.parse(lunchTime.from));
            long shiftTo = ChronoUnit.HOURS.between(LocalTime.parse("00:00"), LocalTime.parse(lunchTime.to));
            long duration = Duration.between(from, to).toHours() - lunchTime.length / 60;
            if (duration > shiftFrom && duration < shiftTo) {
                return lunchTime.length;
            }
        }
        return -1;
    }

    public JSONObject getLinks() {
        return links;
    }

    public String getSelfLink() {
        return links.getJSONObject(SELF).getString(HREF);
    }

    private class LunchTime {
        private final String from;
        private final String to;
        private final int length;

        private LunchTime(JSONObject json) {
            from = json.getString(FROM);
            to = json.getString(TO);
            length = json.getInt("length");
        }

        protected int getLength() {
            return length;
        }

    }
}

package wfm.models;

import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.time.LocalTime;

import static utils.Params.*;

public class TimeInterval {

    @Nonnull
    private final LocalTime startTime;

    private final LocalTime endTime;

    public TimeInterval(@Nonnull LocalTime startTime, LocalTime endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public TimeInterval(JSONObject timeInterval) {
        String start = timeInterval.optString(START_TIME);
        String end = timeInterval.optString(END_TIME);
        this.startTime = LocalTime.parse(start);
        this.endTime = LocalTime.parse(end);
    }

    @Nonnull
    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TimeInterval timeInterval = (TimeInterval) o;

        return this.endTime.equals(timeInterval.endTime) && this.startTime.equals(timeInterval.startTime);
    }

    @Override
    public String toString() {
        return this.startTime + " " + (this.endTime != null ? endTime.toString() : "");
    }
}
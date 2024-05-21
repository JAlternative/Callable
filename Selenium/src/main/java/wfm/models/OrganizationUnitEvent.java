package wfm.models;

import org.json.JSONObject;
import wfm.components.schedule.MonthlyRepeatType;
import wfm.components.schedule.Periodicity;
import wfm.components.schedule.RepeatType;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static utils.Links.REL_ORGANIZATION_UNIT_EVENT_TYPE;
import static utils.Params.*;

public class OrganizationUnitEvent {
    private final Double value;
    private final String outerId;
    private final LocalDateTime time;
    private final LocalDateTime modificationTime;
    private final JSONObject dateTimeInterval;
    private final LocalDate date;
    private final Integer id;
    private final JSONObject repeatRule;
    private final JSONObject links;

    public OrganizationUnitEvent(JSONObject jsonObject) {
        this.value = jsonObject.getDouble(VALUE);
        this.outerId = jsonObject.optString(OUTER_ID);
        String jsonTime = jsonObject.optString("time");
        this.time = jsonTime.isEmpty() ? null : LocalDateTime.parse(jsonTime);
        String jsonModificationTime = jsonObject.optString("modificationTime");
        this.modificationTime = jsonModificationTime.isEmpty() ? null : LocalDateTime.parse(jsonModificationTime);
        this.dateTimeInterval = jsonObject.getJSONObject(DATE_TIME_INTERVAL);
        String jsonDate = jsonObject.optString(DATE);
        this.date = jsonDate.isEmpty() ? null : LocalDate.parse(jsonDate);
        this.id = jsonObject.optInt(ID);
        this.repeatRule = jsonObject.optJSONObject(REPEAT_RULE);
        this.links = jsonObject.getJSONObject(LINKS);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrganizationUnitEvent that = (OrganizationUnitEvent) o;
        return value.equals(that.value)
                && outerId.equals(that.outerId)
                && Objects.equals(time, that.time)
                && Objects.equals(modificationTime, that.modificationTime)
                && dateTimeInterval.similar(that.dateTimeInterval)
                && date.equals(that.date)
                && Objects.equals(id, that.id)
                && repeatRule.similar(that.repeatRule)
                && links.similar(that.links);
    }

    public URI getLink(String linkName) {
        String link = links.getJSONObject(linkName).getString(HREF);
        String splitLink = link.contains("{") ? link.split("\\{")[0] : link;
        return URI.create(splitLink);
    }

    public Integer getFalseId() {
        //какая то затычка, а не айди. всегда нулл
        return this.id;
    }

    public Integer getId() {
        String link = getLink(SELF).toString();
        return Integer.parseInt(link.substring(link.lastIndexOf("/") + 1));
    }

    public DateTimeInterval getDateTimeInterval() {
        return new DateTimeInterval(dateTimeInterval);
    }

    public RepeatRule getRepeatRule() {
        return this.repeatRule != null ? new RepeatRule(repeatRule) : null;
    }

    public int getEventTypeId() {
        String link = getLink(REL_ORGANIZATION_UNIT_EVENT_TYPE).toString();
        return Integer.parseInt(link.substring(link.lastIndexOf("/") + 1));
    }

    public LocalDate getDate() {
        return date;
    }

    public Double getValue() {
        return value;
    }

    public String getOuterId() {
        return outerId;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public LocalDateTime getModificationTime() {
        return modificationTime;
    }


    public static class RepeatRule {
        private final String periodicity;
        private final String monthlyRepeatType;
        private final boolean custom;
        private final int frequency;
        private final int repeatCount;
        private final int day;
        //здесь должен быть java.time.DayOfWeek
        private final String dayOfWeek;
        private final int weekOffset;
        private final boolean lastDayInMonth;
        private final boolean lastWeekDayInMonth;
        //здесь должен быть список java.time.DayOfWeek
        private final List<String> daysOfWeek;
        //здесь должен быть список LocalDate
        private final List<String> excludedDates;
        private final LocalDate startDate;
        private final LocalDate endDate;
        private final Long remainingTimeBeforeStart;
        private final String repeatType;

        private RepeatRule(JSONObject jsonObject) {
            this.periodicity = jsonObject.optString("periodicity");
            this.repeatType = jsonObject.optString("repeatType");
            this.monthlyRepeatType = jsonObject.optString("monthlyRepeatType");
            this.custom = jsonObject.optBoolean("custom");
            this.frequency = jsonObject.optInt("frequency");
            this.repeatCount = jsonObject.optInt("repeatCount");
            this.day = jsonObject.optInt("day");
            this.dayOfWeek = jsonObject.optString("dayOfWeek");
            this.weekOffset = jsonObject.optInt("weekOffset");
            this.lastDayInMonth = jsonObject.optBoolean("lastDayInMonth");
            this.lastWeekDayInMonth = jsonObject.optBoolean("lastWeekDayInMonth");
            this.daysOfWeek = Arrays.asList(jsonObject.optString("daysOfWeek").split("\\s*,\\s*"));
            this.excludedDates = Arrays.asList(jsonObject.optString("excludedDates").split("\\s*,\\s*"));
            String jsonStartDate = jsonObject.optString(START_DATE);
            this.startDate = jsonStartDate.isEmpty() ? null : LocalDate.parse(jsonStartDate);
            String jsonEndDate = jsonObject.optString(END_DATE);
            this.endDate = jsonEndDate.isEmpty() ? null : LocalDate.parse(jsonEndDate);
            this.remainingTimeBeforeStart = jsonObject.optLong("remainingTimeBeforeStart");
        }

        public Periodicity getPeriodicity() {
            return this.periodicity == null ? null : Periodicity.valueOf(this.periodicity);
        }

        public RepeatType getRepeatType() {
            return this.repeatType == null ? null : RepeatType.valueOf(this.repeatType);
        }

        public MonthlyRepeatType getMonthlyRepeatType() {
            return this.monthlyRepeatType == null ? null : MonthlyRepeatType.valueOf(this.monthlyRepeatType);
        }

        public boolean isCustom() {
            return custom;
        }

        public int getFrequency() {
            return frequency;
        }

        public int getRepeatCount() {
            return repeatCount;
        }

        public int getDay() {
            return day;
        }

        public String getDayOfWeek() {
            return dayOfWeek;
        }

        public int getWeekOffset() {
            return weekOffset;
        }

        public boolean isLastDayInMonth() {
            return lastDayInMonth;
        }

        public boolean isLastWeekDayInMonth() {
            return lastWeekDayInMonth;
        }

        public List<String> getDaysOfWeek() {
            return daysOfWeek;
        }

        public List<String> getExcludedDates() {
            return excludedDates;
        }

        public LocalDate getStartDate() {
            return startDate;
        }

        public LocalDate getEndDate() {
            return endDate;
        }

        public Long getRemainingTimeBeforeStart() {
            return remainingTimeBeforeStart;
        }


    }

}

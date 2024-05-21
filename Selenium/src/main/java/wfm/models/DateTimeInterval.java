package wfm.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.commons.collections4.CollectionUtils;
import org.json.JSONObject;
import utils.serialization.LocalDateTimeSerializer;

import javax.annotation.Nonnull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static utils.Params.END_DATE_TIME;
import static utils.Params.START_DATE_TIME;

public class DateTimeInterval {

    @Nonnull
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime startDateTime;
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime endDateTime;
    @JsonIgnore
    private long lengthInSeconds;
    @JsonIgnore
    private int lengthInHours;

    public DateTimeInterval(@Nonnull LocalDateTime startDateTime, LocalDateTime endDateTime) {
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
    }
    public DateTimeInterval() {}

    public DateTimeInterval(JSONObject dateInterval) {
        String start = dateInterval.optString(START_DATE_TIME);
        String end = dateInterval.optString(END_DATE_TIME);
        this.startDateTime = LocalDateTime.parse(start);
        this.endDateTime = end.isEmpty() ? null : LocalDateTime.parse(end);
    }

    public boolean includesDate(LocalDate date) {
        return !date.isBefore(startDateTime.toLocalDate()) && (endDateTime == null || !date.isAfter(endDateTime.toLocalDate()));
    }

    public boolean includesDate(LocalDateTime date) {
        return !date.isBefore(startDateTime) && (endDateTime == null || !date.isAfter(endDateTime));
    }

    @Nonnull
    public LocalDateTime getStartDateTime() {
        return this.startDateTime;
    }

    public LocalDateTime getEndDateTime() {
        return this.endDateTime;
    }

    @JsonIgnore
    public LocalDate getStartDate() {
        return this.startDateTime.toLocalDate();
    }

    @JsonIgnore
    public LocalDate getEndDate() {
        return this.endDateTime.toLocalDate();
    }

    public long getLengthInHours() {
        return ChronoUnit.HOURS.between(startDateTime, endDateTime);
    }

    @JsonIgnore
    public long getLengthInMinutes() {
        return ChronoUnit.MINUTES.between(startDateTime, endDateTime);
    }

    public TimeInterval toTimeInterval() {
        return new TimeInterval(startDateTime.toLocalTime(), endDateTime.toLocalTime());
    }

    public DateInterval toDateInterval() {
        return new DateInterval(startDateTime.toLocalDate(), endDateTime.toLocalDate());
    }

    @Override
    public String toString() {
        return this.startDateTime + " " + (this.endDateTime != null ? this.endDateTime : "");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DateTimeInterval dateTimeInterval = (DateTimeInterval) o;
        return dateTimeInterval.getStartDateTime().truncatedTo(ChronoUnit.SECONDS)
                .equals(this.getStartDateTime().truncatedTo(ChronoUnit.SECONDS))
                && dateTimeInterval.getEndDateTime().truncatedTo(ChronoUnit.SECONDS)
                .equals(this.getEndDateTime().truncatedTo(ChronoUnit.SECONDS));
    }

    public List<LocalDateTime> betweenHours() {
        return Stream.iterate(this.startDateTime, t -> t.plusHours(1))
                .limit(ChronoUnit.HOURS.between(this.startDateTime, this.endDateTime) + 1)
                .collect(Collectors.toList());
    }

    public int lengthOfIntersectionWith(DateTimeInterval time) {
        return CollectionUtils.intersection(this.betweenHours(), time.betweenHours()).size();
    }

    public void setLengthInSeconds(long lengthInSeconds) {
        this.lengthInSeconds = lengthInSeconds;
    }

    /**
     * Возвращает новый интервал со смещением относительно текущего на заданное количество минут.
     * Для перемещения интервала на более ранее время нужно передать отрицательное число.
     */
    public DateTimeInterval offsetByMinutes(int minutes) {
        return new DateTimeInterval(startDateTime.plusMinutes(minutes), endDateTime.plusMinutes(minutes));
    }

    /**
     * Получить список всех дат в заданном промежутке startDateTime и endDateTime
     * */
    public List<LocalDate> getAllDatesInInterval() {
        List<LocalDate> dateList = new ArrayList<>();
        LocalDate currentDate = getStartDate();
        LocalDate endDate = getEndDate();
        while (!currentDate.isAfter(endDate)) {
            dateList.add(currentDate);
            currentDate = currentDate.plusDays(1);
        }
        return dateList;
    }
}
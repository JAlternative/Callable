package wfm.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.json.JSONObject;
import utils.serialization.LocalDateSerializer;

import javax.annotation.Nonnull;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static utils.Params.END_DATE;
import static utils.Params.START_DATE;

public class DateInterval {
    @JsonSerialize(using = LocalDateSerializer.class)
    public LocalDate startDate;
    @JsonSerialize(using = LocalDateSerializer.class)
    public LocalDate endDate;
    private static final Random RANDOM = new Random();

    public DateInterval(@Nonnull LocalDate startDate, LocalDate endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public DateInterval() {
        this.startDate = LocalDate.now().with(TemporalAdjusters.firstDayOfMonth());
        this.endDate = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());
    }

    public DateInterval(@Nonnull LocalDate date) {
        this.startDate = date;
        this.endDate = date;
    }

    public DateInterval(JSONObject dateInterval) {
        //в позишене бывает что нет дата интервала
        if (dateInterval != null) {
            String start = dateInterval.optString(START_DATE);
            String end = dateInterval.optString(END_DATE);
            this.startDate = start.isEmpty() ? LocalDate.now() :LocalDate.parse(start);
            this.endDate = end.isEmpty() ? null : LocalDate.parse(end);
        } else {
            this.startDate = LocalDate.now().plusYears(1000);
            this.endDate = null;
        }
    }

    public boolean includesDate(LocalDate date) {
        return (!date.isBefore(startDate) || date.isEqual(startDate))
                && (endDate == null || (!date.isAfter(endDate) || date.isEqual(endDate)));
    }

    @Nonnull
    public LocalDate getStartDate() {
        return this.startDate;
    }

    public LocalDate getEndDate() {
        return this.endDate;
    }

    @JsonIgnore
    public LocalDate getRandomDateBetween() {
        int diff = difference();
        if (diff == 0) {
            return startDate;
        }
        return this.startDate.plusDays(RANDOM.nextInt(diff));
    }

    public int difference() {
        return Math.toIntExact(this.startDate.until(this.endDate, ChronoUnit.DAYS));
    }

    //Возвращает список дат из всего интервала, исключая даты в списке, который принимает в параметрах
    public List<LocalDate> subtract(List<LocalDate> dates) {
        List<LocalDate> betweenDatesList = getBetweenDatesList();
        betweenDatesList.removeAll(dates);
        return betweenDatesList;
    }

    @JsonIgnore
    public List<LocalDate> getBetweenDatesList() {
        return Stream.iterate(this.startDate, date -> date.plusDays(1))
                .limit(ChronoUnit.DAYS.between(this.startDate, this.endDate) + 1)
                .collect(Collectors.toList());
    }

    /**
     * Check if the intersection between the intervals
     *
     * @param interval1 first dateInterval
     * @param interval2 second dateInterval
     * @return true if both intervals is null or both intervals has null bounds or one interval include any boundary of other interval
     */
    public static boolean hasIntersection(DateInterval interval1, DateInterval interval2) {
        if (interval1 == null || interval2 == null) {
            return true;
        }
        boolean int2IncludesInt1StartDate = interval2.includesDate(interval1.getStartDate());
        boolean int2IncludesInt1EndDate = interval1.getEndDate() != null && interval2.includesDate(interval1.getEndDate());
        boolean int1IncludesInt2StartDate = interval1.getEndDate() != null && interval2.includesDate(interval1.getEndDate());
        boolean int1IncludesInt2EndDate = interval2.getEndDate() != null && interval1.includesDate(interval2.getEndDate());
        boolean bothEndDatesAreNull = interval1.getEndDate() == null && interval2.getEndDate() == null;
        return int2IncludesInt1StartDate ||
                int2IncludesInt1EndDate ||
                int1IncludesInt2StartDate ||
                int1IncludesInt2EndDate ||
                bothEndDatesAreNull;
    }

    public boolean hasIntersection(DateInterval dateInterval) {
        return hasIntersection(this, dateInterval);
    }

    @Override
    public String toString() {
        return this.startDate.toString() + " " + (this.endDate != null ? this.endDate.toString() : "");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DateInterval dateInterval = (DateInterval) o;

        return dateInterval.toString().equals(this.toString());
    }
}
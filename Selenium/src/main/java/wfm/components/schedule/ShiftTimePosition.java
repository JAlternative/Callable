package wfm.components.schedule;

import wfm.models.DateInterval;

import java.time.LocalDate;

import static utils.tools.LocalDateTools.*;

public enum ShiftTimePosition {
    FUTURE,
    PAST,
    PAST_MONTH,
    DEFAULT,
    ALLMONTH,
    PREVIOUS_MONTH,
    NEXT_MONTH,
    FUTURE_WITHOUT_LAST_DAY,
    ALL_MONTH_WITHOUT_FIRST_AND_LAST,
    PAST_THIS_WEEK,
    FUTURE_THIS_WEEK,
    ALL_THIS_WEEK,
    FUTURE_WITH_NEXT_MONTH;

    /**
     * В зависимости от того за какой период будут нужны смены формируется дата для дальнейшего
     * использования в формировании URI.
     *
     * @return дата с учетом условий алгоритма
     */
    public DateInterval getShiftsDateInterval() {
        LocalDate firstDay = getFirstDate();
        LocalDate lastDay = getLastDate();
        LocalDate now = LocalDate.now();
        LocalDate monday = getMonday();
        LocalDate sunday = getSunday();
        int currentDay = now.getDayOfMonth();
        switch (this) {
            case PAST:
                return new DateInterval(firstDay, now.minusDays(1));
            case PAST_MONTH:
                return new DateInterval(firstDay.minusMonths(1), now.minusDays(1));
            case FUTURE:
                return new DateInterval(now, lastDay);
            case FUTURE_WITHOUT_LAST_DAY:
                return new DateInterval(now, lastDay.minusDays(1));
            case ALLMONTH:
                return new DateInterval(firstDay, lastDay);
            case PREVIOUS_MONTH:
                return new DateInterval(now.minusMonths(1).withDayOfMonth(1),
                                        now.minusMonths(1).withDayOfMonth(now.minusMonths(1).lengthOfMonth()));
            case NEXT_MONTH:
                return new DateInterval(now.plusMonths(1).withDayOfMonth(1),
                                        now.plusMonths(1).withDayOfMonth(now.plusMonths(1).lengthOfMonth()));
            case ALL_MONTH_WITHOUT_FIRST_AND_LAST:
                return new DateInterval(firstDay.plusDays(1), lastDay.minusDays(1));
            case PAST_THIS_WEEK:
                return new DateInterval(monday, now.minusDays(1));
            case FUTURE_THIS_WEEK:
                return new DateInterval(now, sunday);
            case ALL_THIS_WEEK:
                return new DateInterval(monday, sunday);
            case FUTURE_WITH_NEXT_MONTH:
                return new DateInterval(now, lastDay.plusMonths(1));
            default:
                return currentDay < 15 ? new DateInterval(now, lastDay) : new DateInterval(firstDay, now.minusDays(1));
        }
    }
}

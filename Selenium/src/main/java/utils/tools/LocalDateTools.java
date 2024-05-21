package utils.tools;

import utils.TimeType;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Random;

/**
 * @author Evgeny Gurkin 21.08.2020
 */
public class LocalDateTools {
    public static final int THAT = 32;
    public static final int FIRST = 33;
    public static final int LAST = 34;
    public static final int RANDOM_PAST = 35;
    public static final int RANDOM = 0;
    private static final long CURRENT_TIME_MILLIS = System.currentTimeMillis();
    private static final LocalDate NOW = LocalDate.now();

    /**
     * Метод создает случайное число с учетом переданного начального значения
     *
     * @param count - задает диапазон максимального значения
     */
    private static Integer seedGenerator(int count) {
        return new Random(CURRENT_TIME_MILLIS).nextInt(count);
    }

    /**
     * Метод возвращает сгенерированную дату соответствующую указанным промежуткам:
     * (см. табл. https://wiki.abcconsulting.ru/pages/viewpage.action?pageId=178985586)
     * пример использования метода в степе:
     * LocalDate a = randomSeedDate(6, 36, ChronoUnit.MONTHS, TimeType.RANDOM);
     * текущая дата сместится на 36 месяцев назад, далее случайным образом прибавится значение из промежутка [0, 42],
     * затем из полученного значения даты выберется случайное число на выпавший месяц
     *
     * @param forFuture указать числовое значение конца промежутка, например: полгода вперед => 6
     * @param toLast    указать числовое значение начала промежутка, например: 3 года назад => 36
     * @param unit      указать масштаб(месяц, год и т.д),  который будет меняться в соответствии с промежутком
     * @param timeType  указать какой день в дате нужно получить (FIRST, LAST, RANDOM)
     * @return дата на основе сида текущего времени
     */
    public static LocalDate randomSeedDate(int forFuture, int toLast, ChronoUnit unit, TimeType timeType) {
        //Дата смещена в начало диапазона, далее сдвинута на случайное значение в масштабе unit
        LocalDate localDate = LocalDate.now().minus(toLast, unit)
                .plus(seedGenerator(forFuture + toLast + 1), unit);
        switch (timeType) {
            case RANDOM:
                localDate = localDate.withDayOfMonth(seedGenerator(localDate.getDayOfMonth()) + 1);
                break;
            case FIRST:
                localDate = localDate.with(TemporalAdjusters.firstDayOfMonth());
                break;
            case LAST:
                localDate = localDate.with(TemporalAdjusters.lastDayOfMonth());
                break;
        }
        return localDate;
    }

    public static LocalDate getDate(int year, int month, int day) {
        int localYear = getYear(year);
        int localMonth = getMonth(month);
        int localDay = getDay(localYear, localMonth, day);
        return LocalDate.of(localYear, localMonth, localDay);
    }

    public static LocalDate getDate(int year, int month) {
        int localYear = getYear(year);
        int localMonth = getMonth(month);
        int localDay = getDay(localYear, localMonth, RANDOM);
        return LocalDate.of(localYear, localMonth, localDay);
    }

    private static int getDay(int year, int month, int day) {
        LocalDate newNow = NOW.withMonth(month).withYear(year);
        int lastDayMonth = newNow.with(TemporalAdjusters.lastDayOfMonth()).getDayOfMonth();
        switch (day) {
            case RANDOM:
                return new Random().nextInt(lastDayMonth) + 1;
            case FIRST:
                return 1;
            case LAST:
                return lastDayMonth;
            case THAT:
            default:
                if (day <= lastDayMonth) {
                    return day;
                } else {
                    return newNow.getDayOfMonth();
                }
        }
    }

    private static int getMonth(int month) {
        switch (month) {
            case THAT:
                return NOW.getMonthValue();
            case RANDOM:
                return NOW.plusMonths(new Random().nextInt(12)).getMonthValue();
            case LAST:
                return NOW.with(TemporalAdjusters.lastDayOfYear()).getMonthValue();
            case FIRST:
                return NOW.with(TemporalAdjusters.firstDayOfYear()).getMonthValue();
            case RANDOM_PAST:
                return NOW.minusMonths(new Random().nextInt(NOW.getMonthValue())).getMonthValue();
            default:
                return month;
        }
    }

    private static int getYear(int year) {
        switch (year) {
            case LAST:
            case FIRST:
            case THAT:
                return NOW.getYear();
            case RANDOM:
                return NOW.getYear() + new Random().nextInt(2) + 1;
            default:
                return year;
        }
    }

    public static LocalDate getFirstDate() {
        return getDate(THAT, THAT, FIRST);
    }

    public static LocalDate getLastDate() {
        return getDate(THAT, THAT, LAST);
    }

    public static LocalDate getMonday() {
        return now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    public static LocalDate getSunday() {
        return now().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
    }

    public static LocalDate now() {
        return LocalDate.now();
    }
}

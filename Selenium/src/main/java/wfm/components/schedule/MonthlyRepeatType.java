package wfm.components.schedule;

/**
 * @author Vasily Nazarenko.
 */
public enum MonthlyRepeatType {
    NOT_USED("не применимо"),
    DAY("в один и тот же день месяца"),
    WEEKDAY("в такой же день недели с начала месяца");


    private final String name;

    MonthlyRepeatType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}

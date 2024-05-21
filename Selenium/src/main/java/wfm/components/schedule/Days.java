package wfm.components.schedule;

import java.util.Arrays;

/**
 * @author Evgeny Gurkin 29.07.2020
 */
public enum Days {
    DAY_OFF("Выходной", "DAY_OFF"),
    DAY("Рабочий", "WORK_DAY"),
    WITHOUT("Без указания поведения", "INHERIT");
    private final String nameOfDay;
    private final String kpiBehavior;

    Days(String nameOfDay, String kpiBehavior) {
        this.nameOfDay = nameOfDay;
        this.kpiBehavior = kpiBehavior;
    }

    public static Days getByName(String name) {
        return Arrays.stream(Days.values()).filter(days -> days.getNameOfDay().equals(name)).findFirst()
                .orElseThrow(() -> new AssertionError("Не был найден тип дня с названием: " + name));
    }

    public String getNameOfDay() {
        return nameOfDay;
    }

    public String getKpiBehavior() {
        return kpiBehavior;
    }
}

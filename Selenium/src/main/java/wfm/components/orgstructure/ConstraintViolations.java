package wfm.components.orgstructure;

import java.util.Random;

public enum ConstraintViolations {
    SHIFT_CROSS_NEXT_MONTH("Смена пересекает следующий месяц"),
    VIOLATION_HOUR_NORMS("Нарушение нормы часов"),
    FORBIDDEN_OVERTIME("Сверхурочная работа запрещена для сотрудника"),
    EXCEEDING_HOURS_PER_WEEK("Превышение нормы часов"),
    SHORTAGE_HOURS_PER_WEEK("Нехватка нормы часов"),
    NO_LONG_REST_PER_WEEK("Нарушение непрерывного отдыха в неделю (42 часа)"),
    SHORT_REST_BETWEEN_SHIFTS("Нарушение продолжительности междусменного отдыха"),
    A_LOT_OF_OVERTIME("У сотрудника превышен лимит сверхурочной работы"),
    REST_AFTER_SHIFT_LESS_THEN_TWO_SHIFTS("Отдых после смены меньше, чем две рабочие длины самой смены");


    private final String name;

    ConstraintViolations(String name) {
        this.name = name;
    }

    public static ConstraintViolations getRandomConstraintViolation() {
        return ConstraintViolations.values()[new Random().nextInt(MathParameters.values().length)];
    }

    public String getName() {
        return name;
    }

}

package wfm.components.utils;

public enum DateTypeField {
    START_DATE("Дата начала"),
    END_DATE("Дата окончания"),
    OPEN_DATE("Дата открытия"),
    CLOSE_DATE("Дата закрытия"),
    START_DEPUTY_DATE("Дата начала замещения"),
    END_DEPUTY_DATE("Дата окончания замещения"),
    WORKING("Рабочий"),
    DAY_OFF("Выходной"),
    START_JOB("Начало работы"),
    END_JOB("Окончание работы"),
    POSITION_START_DATE("Дата начала должности"),
    DEPUTY_START_DATE("Дата начала замещения"),
    DEPUTY_END_DATE("Дата окончания замещения"),
    POSITION_END_DATE("Дата окончания должности"),
    START_CYCLE("Начало цикла"),
    END_CYCLE("Окончание цикла");

    private final String name;

    DateTypeField(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public int getValue() {
        return this.ordinal() + 1;
    }
}

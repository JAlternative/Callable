package wfm.components.schedule;

public enum InEmployeeWorkingHours {
    HOURS_AMOUNT_MONTH("Кол-во часов (месяц)"),
    HOURS_AMOUNT_NORM_MONTH("Кол-во часов - норматив (месяц)"),
    PROCESSING_FLAWS_QUARTER("Переработки/недоработки (квартал)"),
    PROCESSING_FLAWS_YEAR("Переработки/недоработки (год)"),
    HOURS_AMOUNT_YEAR("Кол-во часов (год)"),
    HOURS_AMOUNT_NORM_YEAR("Кол-во часов - норматив (год)"),
    HOURS_AMOUNT_PLAN("Кол-во часов план"),
    HOURS_AMOUNT_FIRST_HALF("Кол-во часов первая пол."),
    HOURS_AMOUNT_SECOND_HALF("Кол-во часов вторая пол."),
    SHIFTS_AMOUNT("Кол-во смен"),
    DAY_OFF_AMOUNT("Кол-во выходных"),
    OUTER_ID("OuterId"),
    NIGHT_HOURS_AMOUNT("Кол-во ноч. часов"),
    NIGHT_HOURS_AMOUNT_FACT("Кол-во ноч. часов факт"),
    RATE("Ставка");

    private final String lineName;

    InEmployeeWorkingHours(String lineName) {
        this.lineName = lineName;
    }

    public String getLineName() {
        return lineName;
    }
}

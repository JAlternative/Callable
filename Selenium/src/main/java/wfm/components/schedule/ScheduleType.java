package wfm.components.schedule;

/**
 * @author Evgeny Gurkin 29.07.2020
 */
public enum ScheduleType {
    SALE("Продажи"),
    SERVICE("Сервис"),
    SALE_AND_SERVICE(""),
    ANY_TYPE("");

    private final String nameOfType;

    ScheduleType(String nameOfType) {
        this.nameOfType = nameOfType;
    }

    public String getNameOfType() {
        return nameOfType;
    }
}

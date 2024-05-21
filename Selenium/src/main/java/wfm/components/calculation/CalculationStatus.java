package wfm.components.calculation;

public enum CalculationStatus {
    ERROR("Ошибка"),
    SUCCESSFUL("Расчет завершен"),
    OTHER("Другой"),
    EXECUTING("Выполняется");
    private final String statusName;

    CalculationStatus(String statusName) {
        this.statusName = statusName;
    }

    public String getStatusName() {
        return statusName;
    }
}

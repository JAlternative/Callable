package wfm.components.schedule;

public enum AddWorkStatus {
    PLANNED("Запланировано"),
    DONE("Проведено"),
    CANCELLED("Отменено");

    private final String status;

    AddWorkStatus(String status) {
        this.status = status;
    }

    public String getStatusName() {
        return status;
    }

}

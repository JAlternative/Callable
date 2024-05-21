package bio.components.client;

public enum EmployeeStatus {
    INCLUDE("Опознается по исключению", "Опознавать"),
    ADMIN("Администратор", "Сделать администратором"),
    EXCLUDE("Не опознается по исключению", "Блокировать"),
    REMOVED("Не присоединен к терминалу", "Убрать исключение");
    private final String status;
    private final String action;

    EmployeeStatus(String status, String action) {
        this.status = status;
        this.action = action;
    }

    public String getStatus() {
        return status;
    }

    public String getAction() {
        return action;
    }
}

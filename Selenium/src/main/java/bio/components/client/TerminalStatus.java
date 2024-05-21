package bio.components.client;

public enum TerminalStatus {
    ACTIVE("Активен"),
    BLOCKED("Неактивен");
    private final String status;

    TerminalStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}

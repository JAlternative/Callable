package wfm.components.schedule;

public enum ChevronStatus {
    OPEN("mdi-chevron-up"),
    CLOSE("mdi-chevron-down");

    private final String status;

    ChevronStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}

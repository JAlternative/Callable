package wfm.components.schedule;

public enum GraphStatus {
    PUBLISH("Плановый график опубликован", "published"),
    NOT_PUBLISH("Плановый график не опубликован", "not-published"),
    ON_APPROVAL("Плановый график на утверждении", "default");
    private final String statusName;
    private final String className;

    GraphStatus(String statusName, String className) {
        this.statusName = statusName;
        this.className = className;
    }

    public String getStatusName() {
        return statusName;
    }

    public String getClassName() {
        return className;
    }
}

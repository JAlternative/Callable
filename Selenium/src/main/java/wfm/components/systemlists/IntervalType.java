package wfm.components.systemlists;

public enum IntervalType {
    ONE_DAY("Один день"),
    DATE("Дата"),
    DATETIME("Дата и время");

    private final String name;

    IntervalType(String name) {
        this.name = name;
    }

    public String getNameOfType() {
        return name;
    }
}

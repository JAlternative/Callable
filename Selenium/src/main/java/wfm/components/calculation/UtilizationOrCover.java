package wfm.components.calculation;

public enum UtilizationOrCover {
    UTILIZATION("Утилизация", "staff-number-dialog-utilization"),
    COVER("Покрытие", "staff-number-dialog-coverage");

    private final String name;
    private final String id;

    UtilizationOrCover(String name, String id) {
        this.name = name;
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}

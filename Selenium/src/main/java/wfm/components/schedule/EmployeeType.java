package wfm.components.schedule;

public enum EmployeeType {
    OWN_PERSONNEL("Собственный персонал"),
    INTERNAL_PART_TIMER("Внутренние совместители"),
    OUT_STAFF("Аутстафф");
    private final String name;

    EmployeeType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}

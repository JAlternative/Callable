package wfm.components.utils;

public enum TimeTypeField {
    START_TIME("Время начала"),
    END_TIME("Время окончания");
    private final String name;

    TimeTypeField(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public int getValue() {
        return this.ordinal() + 1;
    }
}

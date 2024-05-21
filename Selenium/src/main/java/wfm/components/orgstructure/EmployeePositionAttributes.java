package wfm.components.orgstructure;

public enum EmployeePositionAttributes implements Attributes {
    SCHEDULE_BOARD("test_key1", "Расписание", "STRING"),
    POP_UP("test_key2", "Всплывающее окно", "STRING");
    private final String key;
    private final String title;
    private final String dataType;

    EmployeePositionAttributes(String key, String title, String dataType) {
        this.key = key;
        this.title = title;
        this.dataType = dataType;
    }

    public String getKey() {
        return key;
    }

    public String getTitle() {
        return title;
    }

    public String getDataType() {
        return dataType;
    }

    public String getDisplay() {
        return this.toString();
    }
}

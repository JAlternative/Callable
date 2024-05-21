package wfm.components.orgstructure;

public enum EmployeeAttributes implements Attributes {
    CHILD_CARE_VACATION("childCareVacation", "Отпуск по уходу за ребенком", "STRING"),
    DISABILITY("disability", "инвалидность", "STRING"),
    OUTSTAFF("Outstaff", "АутСТАФФ", "BOOLEAN");
    private final String key;
    private final String title;
    private final String dataType;

    EmployeeAttributes(String key, String title, String dataType) {
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
}

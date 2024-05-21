package wfm.components.orgstructure;

public enum OrgUnitAttributes implements Attributes{
    ORG_UNIT_FORMAT("org_unit_format", "Принадлежность подразделения", "STRING"),
    TEST_ATTRIBUTE("test_attribute", "Тестовый атрибут", "STRING");

    private final String key;
    private final String title;
    private final String dataType;

    OrgUnitAttributes(String key, String title, String dataType) {
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

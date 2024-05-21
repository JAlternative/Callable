package wfm.components.calculation;

public enum FilterType {
    EVENT_TYPE("Типы событий"),
    KPI("KPI"),
    FUNCTIONAL_ROLE("Функциональные роли"),
    EVENT_AND_KPI("KPI и Типы событий");

    private final String typeName;

    FilterType(String typeName) {
        this.typeName = typeName;
    }

    public String getTypeName() {
        return typeName;
    }

    public int getTypeNumber() {
        return FilterType.this.ordinal() + 1;
    }
}

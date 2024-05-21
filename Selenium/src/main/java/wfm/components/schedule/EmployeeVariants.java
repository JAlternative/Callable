package wfm.components.schedule;

public enum EmployeeVariants {
    EMPLOYEE_POSITION_ATTRIBUTES("dialogs.orgUnit.groups.attributesEmployeePosition", "Атрибуты назначения"),
    POSITION_ATTRIBUTES("dialogs.orgUnit.groups.attributesPosition", "Атрибуты позиции"),
    EDIT("common.actions.edit", "Редактировать");

    private final String function;
    private final String name;

    EmployeeVariants(String function, String name) {
        this.function = function;
        this.name = name;
    }

    public String getVariant() {
        return function;
    }

    public String getName() {
        return name;
    }
}

package wfm.components.orgstructure;

public enum OrgUnitInputs {
    OM_NAME("org-unit-name"),
    TYPE("org-unit-type"),
    DEPUTY("org-unit-deputy-employee"),
    PARENT_OM("org-unit-parent"),
    OUTER_ID("org-unit-outerId"),
    ;

    private final String fieldType;

    OrgUnitInputs(String fieldType) {
        this.fieldType = fieldType;
    }

    public String getFieldType() {
        return fieldType;
    }
}

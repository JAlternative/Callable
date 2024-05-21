package wfm.components.orgstructure;

import static utils.Links.*;

public enum MathParameterEntities {
    ORGANIZATION_UNIT(ORG_UNITS, "org-unit-keys"),
    POSITION_GROUP(POSITION_GROUPS, "position-group-keys"),
    EMPLOYEE(EMPLOYEES, "employee-keys"),
    POSITION(POSITIONS,"position-keys"),
    EMPLOYEE_POSITION(EMPLOYEE_POSITIONS, "employee-position-keys");
    private final String link;
    private final String keys;

    MathParameterEntities(String link, String keys) {
        this.link = link;
        this.keys = keys;
    }

    public String getLink() {
        return link;
    }

    public String getKeys() {
        return keys;
    }


}

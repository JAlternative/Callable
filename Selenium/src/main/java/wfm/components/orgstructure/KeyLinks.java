package wfm.components.orgstructure;

public enum KeyLinks {
    ORG_UNIT_KEYS("org-unit-keys"),
    EMPLOYEE_KEYS("employee-keys"),
    EMPLOYEE_POSITION_KEYS("employee-position-keys");
    private final String link;

    KeyLinks(String link) {
        this.link = link;
    }

    public String getLink() {
        return link;
    }

}

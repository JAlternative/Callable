package wfm.components.orgstructure;

public enum EmployeeSkills {
    TUTOR("наставничество"),
    RESPONSIBLE("ответственный"),
    MASTER("Административная должность");

    private final String name;

    EmployeeSkills(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}

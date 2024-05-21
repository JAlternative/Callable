package stresbd.models;

public class Positions {

    private String action;
    private String employeeOuterId;
    private String name;
    private String organizationUnitOuterId;
    private String outerId;
    private String positionCategoryOuterId;
    private String positionGroupName;
    private String positionTypeOuterId;
    private String startDate;
    private final String stringFromFile;

    public Positions(String stringFromFile) {
        this.stringFromFile = stringFromFile;
    }

    public String getAction() {
        return lineSeparator()[0];
    }

    public String getEmployeeOuterId() {
        return lineSeparator()[1];
    }

    public String getName() {
        return lineSeparator()[2];
    }

    public String getOrganizationUnitOuterId() {
        return lineSeparator()[3];
    }

    public String getOuterId() {
        return lineSeparator()[4];
    }

    public String getPositionCategoryOuterId() {
        return lineSeparator()[5];
    }

    public String getPositionGroupName() {
        return lineSeparator()[6];
    }

    public String getPositionTypeOuterId() {
        return lineSeparator()[7];
    }

    public String getStartDate() {
        return lineSeparator()[8];
    }

    private String getStringFromFile() {
        return stringFromFile;
    }

    private String[] lineSeparator() {
        return getStringFromFile().split(",");
    }

}

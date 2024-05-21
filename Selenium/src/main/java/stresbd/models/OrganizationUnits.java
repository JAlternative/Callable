package stresbd.models;

public class OrganizationUnits {

    private String action;
    private String active;
    private String availableForCalculation;
    private String chiefPositionOuterId;
    private String dateFrom;
    private String dateTo;
    private String email;
    private String fax;
    private String name;
    private String organizationUnitTypeOuterId;
    private String outerId;
    private String parentOuterId;
    private final String stringFromFile;

    public OrganizationUnits(String stringFromFile) {
        this.stringFromFile = stringFromFile;
    }

    public String isAction() {
        return lineSeparator()[0];
    }

    public String isActive() {
        return lineSeparator()[1];
    }

    public String isAvailableForCalculation() {
        return lineSeparator()[2];
    }

    public String getChiefPositionOuterId() {
        return lineSeparator()[3];
    }

    public String getDateFrom() {
        return lineSeparator()[4];
    }

    public String getDateTo() {
        return lineSeparator()[5];
    }

    public String getEmail() {
        return lineSeparator()[6];
    }

    public String getFax() {
        return lineSeparator()[7];
    }

    public String getName() {
        return lineSeparator()[8];
    }

    public String getOrganizationUnitTypeOuterId() {
        return lineSeparator()[9];
    }

    public String getOuterId() {
        return lineSeparator()[10];
    }

    public String getParentOuterId() {
        return lineSeparator()[11];
    }

    private String getStringFromFile() {
        return stringFromFile;
    }

    private String[] lineSeparator() {
        return getStringFromFile().split(",");
    }

}

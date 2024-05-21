package stresbd.models;

public class BusinessHours {

    private String action;
    private String endDate;
    private String startDate;
    private String day1TimeIntervalEndTime;
    private String day1TimeIntervalStartTime;
    private String day2TimeIntervalEndTime;
    private String day2TimeIntervalStartTime;
    private String day3TimeIntervalEndTime;
    private String day3TimeIntervalStartTime;
    private String day4TimeIntervalEndTime;
    private String day4TimeIntervalStartTime;
    private String day5TimeIntervalEndTime;
    private String day5TimeIntervalStartTime;
    private String day6TimeIntervalEndTime;
    private String day6TimeIntervalStartTime;
    private String day7TimeIntervalEndTime;
    private String day7TimeIntervalStartTime;
    private String organizationUnitOuterId;
    private final String stringFromFile;

    public BusinessHours(String stringFromFile) {
        this.stringFromFile = stringFromFile;
    }

    public String getAction() {
        return lineSeparator()[0];
    }

    public String getEndDate() {
        return lineSeparator()[1];
    }

    public String getStartDate() {
        return lineSeparator()[2];
    }

    public String getDay1TimeIntervalEndTime() {
        return lineSeparator()[3];
    }

    public String getDay1TimeIntervalStartTime() {
        return lineSeparator()[4];
    }

    public String getDay2TimeIntervalEndTime() {
        return lineSeparator()[5];
    }

    public String getDay2TimeIntervalStartTime() {
        return lineSeparator()[6];
    }

    public String getDay3TimeIntervalEndTime() {
        return lineSeparator()[7];
    }

    public String getDay3TimeIntervalStartTime() {
        return lineSeparator()[8];
    }

    public String getDay4TimeIntervalEndTime() {
        return lineSeparator()[9];
    }

    public String getDay4TimeIntervalStartTime() {
        return lineSeparator()[10];
    }

    public String getDay5TimeIntervalEndTime() {
        return lineSeparator()[11];
    }

    public String getDay5TimeIntervalStartTime() {
        return lineSeparator()[12];
    }

    public String getDay6TimeIntervalEndTime() {
        return lineSeparator()[13];
    }

    public String getDay6TimeIntervalStartTime() {
        return lineSeparator()[14];
    }

    public String getDay7TimeIntervalEndTime() {
        return lineSeparator()[15];
    }

    public String getDay7TimeIntervalStartTime() {
        return lineSeparator()[16];
    }

    public String getOrganizationUnitOuterId() {
        return lineSeparator()[17];
    }

    private String[] lineSeparator() {
        return getStringFromFile().split(",");
    }

    private String getStringFromFile() {
        return stringFromFile;
    }

}

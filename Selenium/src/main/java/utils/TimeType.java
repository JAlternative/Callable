package utils;

public enum TimeType {
    RANDOM("randomDay"),
    FIRST("firstDay"),
    LAST("lastDay");
    private final String day;

    TimeType(String day) {
        this.day = day;
    }

    public String getDay() {
        return day;
    }
}

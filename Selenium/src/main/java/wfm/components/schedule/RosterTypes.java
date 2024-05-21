package wfm.components.schedule;

import java.util.stream.Stream;

import static utils.tools.CustomTools.randomItem;

public enum RosterTypes {
    //TODO обращаться в системный список типы ростеров /api/v1/search/schedule-request-alias-roster-types
    ANY("Любой", null),
    PLANNED("Плановый график", "PLANNED"),
    TABLE("Табель", "TABLE");

    private final String name;
    private final String value;

    RosterTypes(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public static RosterTypes getRandomRosterType() {
        return Stream.of(RosterTypes.ANY,
                RosterTypes.PLANNED,
                RosterTypes.TABLE).collect(randomItem());
    }

}

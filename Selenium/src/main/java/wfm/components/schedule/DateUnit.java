package wfm.components.schedule;

/**
 * @author Evgeny Gurkin 29.07.2020
 */
public enum DateUnit {
    YEAR(2),
    DAY(2),
    MONTH(1),
    HOUR(1);
    private final int level;

    DateUnit(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }
}

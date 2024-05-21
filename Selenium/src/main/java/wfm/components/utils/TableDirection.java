package wfm.components.utils;

public enum TableDirection {
    LEFT("left", -1),
    RIGHT("right", 1);
    private final String nameOfDirection;
    private final int changeValue;

    TableDirection(String nameOfDirection, int changeValue) {
        this.nameOfDirection = nameOfDirection;
        this.changeValue = changeValue;
    }

    public String getNameOfDirection() {
        return nameOfDirection;
    }

    public int getChangeValue() {
        return changeValue;
    }
}

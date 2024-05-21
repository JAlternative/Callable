package wfm.components.systemlists;

public enum LimitType {
    GENERAL("Общий"),
    ADD_WORK("На подработку"),
    POSITION("По назначению");

    private final String name;

    LimitType(String name) {
        this.name = name;
    }

    public String getNameOfType() {
        return name;
    }
}

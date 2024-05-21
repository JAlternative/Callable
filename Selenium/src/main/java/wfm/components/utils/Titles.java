package wfm.components.utils;

public enum Titles {
    ROLE("Роль"),
    ORG_UNIT("Подразделение");

    private final String title;

    Titles(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}

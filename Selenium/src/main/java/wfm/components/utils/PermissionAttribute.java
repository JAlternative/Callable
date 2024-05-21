package wfm.components.utils;

public enum PermissionAttribute {

    STANDARD("Стандартное"),

    SPECIAL("Специальное");

    private final String name;

    PermissionAttribute(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}

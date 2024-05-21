package bio.components.client;

/**
 * Выбор варианта создания нового пользователя, привязать и создать нового
 */
public enum VariantsCreate {
    BIND_USER("assigntoexistedpersan",
            "Привязать пользователя к уже существующей персоне (поступающей из внешних источников, либо ранее созданной локально)"),
    CREATE_NEW("assigntonewlocal",
            "Создать новую локальную персону, и наделить её возможностями пользователя");

    private final String variant;
    private final String forAllure;

    VariantsCreate(String operation, String allure) {
        this.variant = operation;
        this.forAllure = allure;
    }

    public String getVariant() {
        return variant;
    }

    public String getForAllure() {
        return forAllure;
    }
}

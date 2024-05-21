package bio.components.client;

/**
 * Выбор операции с пользователем
 */
public enum VariantsOperation {
    ADD_USER("Создать нового пользователя"),
    EDIT("Редактировать пользователя"),
    DELETE("Удалить (отвязать) пользователя");

    private final String variant;

    VariantsOperation(String operation) {
        this.variant = operation;
    }

    public String getVariant() {
        return variant;
    }
}

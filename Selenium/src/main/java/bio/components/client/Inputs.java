package bio.components.client;

/**
 * Выбора варианта поля ввода при добавлении нового пользователя
 */
public enum Inputs {
    LOGIN("username", "Логин"),
    PASS("password", "Пароль"),
    CONFIRM_PASS("confirmpassword", "Подтверждение пароля"),
    //здесь поля ввода для имени, фамилии, отчества
    FAMILY("lastName", "Фамилия"),
    NAME("firstName", "Имя"),
    PATRONYMIC("patronymicName", "Отчество");

    private final String inputs;
    private final String allureName;

    Inputs(String input, String allureName) {
        this.inputs = input;
        this.allureName = allureName;
    }

    public String getInputs() {
        return inputs;
    }

    public String getAllureName() {
        return allureName;
    }
}

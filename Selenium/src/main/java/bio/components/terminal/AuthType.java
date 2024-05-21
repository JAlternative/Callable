package bio.components.terminal;

public enum AuthType {
    ONE_EVENT("Авторизация без определённого события", 1, "Одна кнопка (идентификация)"),
    TWO_EVENT("Авторизация с началом и концом смены", 2, "Две кнопки (начало и окончание смены)"),
    FOUR_EVENT("Авторизация с началом, концом и перерывами смены", 4, "Четыре кнопки (начало и окончание смены и перерыва)");
    private final String type;
    private final int value;
    private final String settingsName;

    AuthType(String type, int value, String settingsName) {
        this.type = type;
        this.value = value;
        this.settingsName = settingsName;
    }

    public String getType() {
        return type;
    }

    public int getValue() {
        return value;
    }

    public String getSettingsName() {
        return settingsName;
    }
}

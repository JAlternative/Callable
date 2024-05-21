package bio.components.terminal;

public enum CheckInType {
    OPEN_BREAK("В блоке \"Перерыв\" нажать на кнопку \"Начать\"", "Начинаем перерыв"),
    CLOSE_BREAK("В блоке \"Перерыв\" нажать на кнопку \"Закончить\"", "Завершаем перерыв"),
    OPEN_SHIFT("\"Начать работу\"", "Начинаем рабочий день"),
    RECORD("\"Отметиться\"", "Отмечаемся"),
    CLOSE_SHIFT("\"Закончить работу\"", "Завершаем рабочий день");
    private final String checkInWay;
    private final String textOnPanel;

    CheckInType(String checkInWay, String textOnPanel) {
        this.checkInWay = checkInWay;
        this.textOnPanel = textOnPanel;

    }

    public String getCheckInWayName() {
        return checkInWay;
    }

    public String getTextOnPanel() {
        return textOnPanel;
    }
}

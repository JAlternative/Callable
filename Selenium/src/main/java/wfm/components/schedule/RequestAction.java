package wfm.components.schedule;

public enum RequestAction {
    REJECT("Отклонить"),
    ACCEPT("Подтвердить"),
    EDIT("Изменить"),
    DELETE("Удалить"),
    MOVE_TO_EXCHANGE("Отдать на биржу");
    private final String action;


    RequestAction(String action) {
        this.action = action;
    }

    public String getAction() {
        return action;
    }
}

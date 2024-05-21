package wfm.components.utils;

public enum PopUpText {
    MESSAGE_SEND("Сообщение отправлено! Вашу заявку рассмотрят в ближайшее время."),
    MUST_SPECIFY_TITLE_AND_BODY("Нужно указать тему и текст сообщения");

    private final String text;

    PopUpText(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }
}

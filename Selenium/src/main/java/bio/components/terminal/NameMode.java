package bio.components.terminal;

/**
 * @author Evgeny Gurkin 13.08.2020
 */
public enum NameMode {
    FULL("Иванов Иван Иванович"),
    SHORT_FAMILY("И. Иван Иванович"),
    NOT_DISPLAYED("Не отображать ФИО (и не показывать кнопку \"это не я\")");

    private final String mode;

    NameMode (String mode){
        this.mode =mode;
    }

    public String getMode() {
        return mode;
    }
}

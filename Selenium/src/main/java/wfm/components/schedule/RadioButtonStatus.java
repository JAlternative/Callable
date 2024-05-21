package wfm.components.schedule;

/**
 * @author Evgeny Gurkin 29.07.2020
 */
public enum RadioButtonStatus {
    SELECT("and (contains(@class, 'is-checked'))]"),
    NO_SELECT("and not (contains(@class, 'is-checked'))]");

    private final String status;

    RadioButtonStatus(String status) {
        this.status = status;
    }

    public String getNameOfType() {
        return status;
    }

}

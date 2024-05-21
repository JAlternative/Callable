package wfm.components.analytics;

/**
 * @author Evgeny Gurkin 29.07.2020
 */
public
enum Column {
    DATE,
    DIAGNOSTICS,
    CORR_3,
    PROGNOSIS,
    CORR_5;

    public int getColumnNumber() {
        return this.ordinal() + 1;
    }
}

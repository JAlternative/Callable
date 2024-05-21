package wfm.components.utils;


public enum ScopeReportTab {
    DAY(24, "ДЕНЬ"),
    MONTH( 28, "МЕСЯЦ"),
    YEAR( 12, "ГОД");

    private final int numberOfElements;
    private final String scopeName;

    ScopeReportTab(int numberOfElements, String scopeName) {
        this.numberOfElements = numberOfElements;
        this.scopeName = scopeName;
    }

    public int getNumberOfElements() {
        return numberOfElements;
    }

    public String getScopeName() {
        return scopeName;
    }
}

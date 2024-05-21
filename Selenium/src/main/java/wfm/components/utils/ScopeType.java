package wfm.components.utils;

public enum ScopeType {
    DAY("День", "дневном"),
    WEEK("Неделя", "недельном"),
    MONTH("Месяц", "месячном");

    private final String scopeName;

    private final String scopeAdjective;

    ScopeType(String scopeName, String scopeAdjective) {
        this.scopeName = scopeName;
        this.scopeAdjective = scopeAdjective;
    }

    public String getScopeName() {
        return scopeName;
    }

    public String getScopeAdjective() {
        return scopeAdjective;
    }
}

package wfm.components.systemlists;

public enum TableRuleStrategy {
    UP_TO_DATE("Предыдущие дни"),
    PREVIOUS_MONTH("Предыдущий месяц");

    public String getString() {
        return string;
    }

    final String string;

    TableRuleStrategy(String name) {
        this.string = name;
    }
}

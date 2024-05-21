package wfm.components.analytics;

public enum ParamName {
    LENGTH_SHIFT_EXCLUDING_LUNCH("Длина смены по нормативу без учета обеда"),
    PLANNED_NUMBER_OF_EMPLOYEES("Планируемая численность сотрудников"),
    ABSENCE_RATIO("Коэффициент отсутствия"),
    NUMBER_EMPLOYEES_WORKING_2X2("Кол-во сотрудников, работающих по графику 2x2"),
    MAXIMUM_NUMBER_CLOSING_AT_THE_LAST_HOUR
            ("Максимальное количество закрывающих в последний час по дням недели"),
    CONSIDER_THE_PREVIOUS_MONTH("Учитывать предыдущий месяц"),
    PROHIBIT_WITHDRAWAL_EMPLOYEES_OVER_RZ("Запрет вывода сотрудников свыше РЗ на открытие/закрытие");

    private final String name;

    ParamName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}

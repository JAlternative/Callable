package wfm.components.schedule;

/**
 * @author Evgeny Gurkin 29.07.2020
 */
public
enum ParamName {
    LENGTH_SHIFT_EXCLUDING_LUNCH("Длина смены по нормативу без учета обеда"),
    NUMBER_OF_EMPLOYEES("Планируемая численность сотрудников"),
    DEVIATION_FROM_STANDARD_MODE("Режим учета переработок сотрудников"),
    STANDARD_HOURS_IN_MONTH("Количество рабочих часов на месяц по нормативу"),
    TOTAL_LIMIT_EMPLOYEE("Максимальный лимит часов на сотрудника"),
    MIN_SHIFT_EXCHANGE_RULE_INTERSECTION("Минимальное время пересменки"),
    CALC_OFF_ROUND("Округление штатной численности"),
    ;

    private final String name;

    ParamName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}

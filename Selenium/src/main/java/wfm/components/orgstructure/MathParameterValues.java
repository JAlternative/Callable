package wfm.components.orgstructure;

public enum MathParameterValues {
    /**
    EMPLOYEE
     */
    MIN_LENGTH_OF_SHIFT(MathParameterEntities.EMPLOYEE, "minLengthOfShift", "Минимальная продолжительность смены без учета обеда", "DOUBLE"),
    MAX_LENGTH_OF_SHIFT(MathParameterEntities.EMPLOYEE, "maxLengthOfShift", "Максимальная продолжительность смены без учета обеда", "DOUBLE"),
    STAFF_WITH_SHIFT_EXCHANGE_RULES_GROUPS(MathParameterEntities.EMPLOYEE_POSITION, "staffWithShiftExchangeRulesGroups", "Постоянное участие в пересменке", "STRING"),
    /**
     * ORGANIZATION_UNIT
     */
    SUM_ACCOUNTING_NORM(MathParameterEntities.ORGANIZATION_UNIT, "summarizedAccountingNorm", "Суммированный учёт", "BOOLEAN"),
    ACCESS_TO_OVERWORK(MathParameterEntities.ORGANIZATION_UNIT, "accessToOverwork", "Доступ к переработкам", "BOOLEAN"),
    CHECK_VIOLATIONS(MathParameterEntities.ORGANIZATION_UNIT, "checkViolations", "Рассчитывать конфликты", "BOOLEAN"),
    REST_BETWEEN_SHIFTS(MathParameterEntities.ORGANIZATION_UNIT, "restBetweenShifts", "Отдых между сменами", "INTEGER"),
    DEPERSONALISATION_MODE(MathParameterEntities.ORGANIZATION_UNIT, "modeConfirm", "Режим деперсонализации", "STRING"),
    EXCHANGE_PROVIDER(MathParameterEntities.ORGANIZATION_UNIT, "exchangeProvider", "Провайдер биржи смен", "STRING"),

    /**
     * POSITION_GROUP
     */
    EXCLUDE_FROM_CALCULATION(MathParameterEntities.POSITION_GROUP, "excludeFromCalculation", "Исключать из расчета", "BOOLEAN"),
    LUNCH_RULES(MathParameterEntities.POSITION_GROUP, "lunchRules", "Правила формирования обеденного перерыва", "BOOLEAN");
    private final MathParameterEntities entity;

    private final String outerId;
    private final String name;
    private final String type;

    MathParameterValues(MathParameterEntities entity, String outerId, String name, String type) {
        this.entity = entity;
        this.outerId = outerId;
        this.name = name;
        this.type = type;
    }

    public MathParameterEntities getEntity() {
        return entity;
    }

    public String getOuterId() {
        return outerId;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

}

package wfm.components.orgstructure;

import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public enum MathParameters {
    DO_NOT_EXCEED_HOURS_BALANCE(287, "EMPLOYEE", "Не превышать баланс часов"),
    IGNORE_RESTRICTIONS_BETWEEN_BREAKS(278, "EMPLOYEE", "Игнорировать ограничения на перерыв между сменами"),
    IGNORE_THE_MONTHLY_STANDARD_FOR_THE_PRODUCTION_CALENDAR(195, "EMPLOYEE", "Игнорировать норматив на месяц по производственному календарю"),
    NIGHT_EMPLOYEE(310, "EMPLOYEE", "Ночной сотрудник"),
    //мат параметры группы EMPLOYEE_POSITION
    NIGHT_EMPLOYEE_POSITION(318, "EMPLOYEE_POSITION", "Ночной сотрудник"),
    AVAILABILITY_INTERVALS(319, "EMPLOYEE_POSITION", "Время доступности для сотрудника"),
    STAFF_WITH_SHIFT_EXCHANGE_RULES_GROUPS(320, "EMPLOYEE_POSITION", "Постоянное участие в пересменке"),
    MIN_LENGTH_OF_SHIFT(312, "EMPLOYEE_POSITION", "Мин. продолжительность смены без учета обеда"),
    MAX_LENGTH_OF_SHIFT(313, "EMPLOYEE_POSITION", "Макс. продолжительность смены без учета обеда"),
    MIN_COUNT_OF_CONTINUOUS_FREE_DAYS(315, "EMPLOYEE_POSITION", "Мин. количество последовательных выходных"),
    MAX_COUNT_OF_CONTINUOUS_FREE_DAYS(316, "EMPLOYEE_POSITION", "Макс. количество последовательных выходных"),
    MIN_COUNT_OF_CONTINUOUS_WORK_DAYS(317, "EMPLOYEE_POSITION", "Мин. количество последовательных рабочих дней"),
    MAX_COUNT_OF_CONTINUOUS_WORK_DAYS(314, "EMPLOYEE_POSITION", "Макс. количество последовательных рабочих дней"),
    STANDARD_HOURS_IN_WEEK(370, "EMPLOYEE_POSITION", "Количество рабочих часов в неделю по нормативу"),
    SEQUENCE_START_DATE(389, "EMPLOYEE_POSITION", "Дата начала ротационности"),
    FIXED_DAYS(321, "EMPLOYEE_POSITION", "Выберите рабочие дни сотрудника");

    private final int mathParamId;
    private final String nameParam;
    private final String entityParam;

    MathParameters(int mathParamId, String entityParam, String nameParam) {
        this.mathParamId = mathParamId;
        this.nameParam = nameParam;
        this.entityParam = entityParam;
    }

    public static MathParameters getRandomMathParameter() {
        return MathParameters.values()[new Random().nextInt(MathParameters.values().length)];
    }

    public static List<MathParameters> getMathParamsListByEntity(String entity) {
        Set<MathParameters> set = EnumSet.allOf(MathParameters.class);
        return set.stream()
                .filter(param -> param.getEntityParam().equals(entity))
                .collect(Collectors.toList());
    }

    public int getMathParamId() {
        return mathParamId;
    }

    public String getNameParam() {
        return nameParam;
    }

    public String getEntityParam() {
        return entityParam;
    }
}

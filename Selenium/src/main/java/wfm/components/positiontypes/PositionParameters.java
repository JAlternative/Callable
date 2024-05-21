package wfm.components.positiontypes;

public enum PositionParameters {
    REMOVAL_UNNECESSARY_SHIFTS("Разрешить удаление лишних смен"),
    GROUP_RESPONSIBLE("Группа ответственных"),
    MIN_CONSECUTIVE_WEEKENDS("Минимальное количество последовательных выходных"),
    MAX_CONSECUTIVE_WEEKENDS("Максимальное количество последовательных выходных"),
    MIN_CONSECUTIVE_WORKING_DAYS("Минимальное количество последовательных рабочих дней"),
    MAX_CONSECUTIVE_WORKING_DAYS("Максимальное количество последовательных рабочих дней"),
    MINIMUM_WEEKENDS_WEEK("Минимальное количество выходных в неделю"),
    MIN_SHIFT_WITHOUT_LUNCH("Минимальная продолжительность смены без учета обеда"),
    MAX_SHIFT_WITHOUT_LUNCH("Максимальная продолжительность смены без учета обеда");

    private final String parameterName;

    PositionParameters(String parameterName) {
        this.parameterName = parameterName;
    }

    public String getParameterName() {
        return parameterName;
    }
}

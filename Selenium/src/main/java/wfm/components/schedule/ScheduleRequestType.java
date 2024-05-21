package wfm.components.schedule;

import wfm.models.ScheduleRequestAlias;
import wfm.repository.ScheduleRequestAliasRepository;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static utils.tools.CustomTools.randomItem;

public enum ScheduleRequestType {
    //что то поменялось
    //TODO обращаться в системный список типы запросов расписания /api/v1/search/schedule-request-types или /api/v1/schedule-requests/alias
    SHIFT_REQUEST("Запрос смены", "???"),
    SHIFT("Смена", "Смена"),
    DAY_OFF("Отгул", "Отг."),
    PARTIAL_ABSENCE("Частичное отсутствие", "Отс."),
    VACATION("Отпуск", "Отп."),
    SICK_LEAVE("Больничный", "Бол."),
    NON_APPEARANCE("Неявка", "Неяв."),
    NON_APPEARANCE_DATE("Неявка (день)", "НеявД."),
    NON_APPEARANCE_DATETIME("Неявка (время)", "НеявВ."),
    OFF_TIME("Выходной", "Вых."),
    OFF_TIMES("Выходные дни", "В"), //на стенде OFF_TIME
    TRAINING("Обучение", "Обуч."),
    BUSINESS_TRIP("Командировка", "К"),
    SHIFT_OTHER("Смена в др. подразделении", "См. в др"),
    PLANNED_ABSENCE("Плановое отсутствие", "Пл.отс."),
    OVERTIME("Сверхурочная работа", "СверхР"),
    OVERTIME_WORK("Работа сверхурочно", ""),
    ON_DUTY("Дежурство", ""),
    FREE_SHIFT("Свободные смены", "");

    private final String name;
    private final String shortName;

    ScheduleRequestType(String name, String shortName) {
        this.name = name;
        this.shortName = shortName;
    }

    public static ScheduleRequestType checkAgainstActiveAliases(Stream<ScheduleRequestType> stream) {
        List<String> aliases = ScheduleRequestAliasRepository.getAllAliases()
                .stream()
                .filter(ScheduleRequestAlias::isEnabled)
                .map(ScheduleRequestAlias::getType)
                .collect(Collectors.toList());
        return stream.filter(t -> aliases.contains(t.name())).collect(randomItem());
    }

    public static ScheduleRequestType getRandomNotShift() {
        return checkAgainstActiveAliases(Arrays.stream(ScheduleRequestType.values())
                .filter(scheduleRequestType -> scheduleRequestType != ScheduleRequestType.SHIFT
                        && scheduleRequestType != ScheduleRequestType.SHIFT_OTHER
                        && scheduleRequestType != ScheduleRequestType.SHIFT_REQUEST));
    }

    public static ScheduleRequestType getRandomAbsenceRequest() {
        return checkAgainstActiveAliases(Stream.of(ScheduleRequestType.SICK_LEAVE,
                ScheduleRequestType.VACATION,
                ScheduleRequestType.OFF_TIME,
                ScheduleRequestType.PLANNED_ABSENCE,
                ScheduleRequestType.TRAINING,
                ScheduleRequestType.DAY_OFF,
                ScheduleRequestType.PARTIAL_ABSENCE,
                ScheduleRequestType.BUSINESS_TRIP,
                ScheduleRequestType.SHIFT_OTHER));
    }

    public static ScheduleRequestType getRandomDayTypeRequest() {
        return checkAgainstActiveAliases(Stream.of(ScheduleRequestType.SICK_LEAVE,
                         ScheduleRequestType.VACATION,
                         ScheduleRequestType.OFF_TIME,
                         ScheduleRequestType.PLANNED_ABSENCE)
                         //ScheduleRequestType.NON_APPEARANCE)
                );
    }

    public static ScheduleRequestType getRandomScheduleRequestType() {
        return checkAgainstActiveAliases(Stream.of(ScheduleRequestType.SHIFT,
                ScheduleRequestType.DAY_OFF,
                ScheduleRequestType.PARTIAL_ABSENCE,
                ScheduleRequestType.VACATION,
                ScheduleRequestType.SICK_LEAVE,
                ScheduleRequestType.NON_APPEARANCE,
                ScheduleRequestType.OFF_TIME,
                ScheduleRequestType.TRAINING,
                ScheduleRequestType.BUSINESS_TRIP,
                ScheduleRequestType.SHIFT_OTHER,
                ScheduleRequestType.PLANNED_ABSENCE));
    }

    public String getName() {
        return name;
    }

    public String getShortName() {
        return shortName;
    }

    public static ScheduleRequestType getType(String type) {
        return Arrays.stream(ScheduleRequestType.values())
                .filter(e -> e.toString().equals(type))
                .findFirst()
                .orElseThrow(NoSuchElementException::new);
    }
}

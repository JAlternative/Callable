package wfm.repository;

import org.apache.http.NameValuePair;
import org.json.JSONObject;
import utils.Projects;
import utils.tools.LocalDateTools;
import utils.tools.Pairs;
import utils.tools.RequestFormers;
import wfm.PresetClass;
import wfm.components.schedule.ShiftTimePosition;
import wfm.models.*;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static utils.Links.*;
import static utils.tools.CustomTools.getClassObjectFromJson;
import static utils.tools.CustomTools.getListFromJsonObject;
import static utils.tools.RequestFormers.getJsonFromUri;
import static utils.tools.RequestFormers.makePath;

/**
 * @author Evgeny Gurkin 19.08.2020
 */
public class ShiftRepository {

    private ShiftRepository() {
    }

    /**
     * Для позиции сотрудника берет первую смену в месяце, если ее нет то создает ее и возвращает ее значение
     *
     * @param employeePosition - позиция сотрудника
     */
    public static Shift getFirstDayMonthShift(EmployeePosition employeePosition) {
        LocalDate firstDate = LocalDateTools.getFirstDate();
        Shift shift = getShift(employeePosition, firstDate, null);
        if (shift == null) {
            ShiftTimePosition timePosition = ShiftTimePosition.PAST;
            List<Shift> shifts = getShifts(employeePosition, timePosition);
            return PresetClass.presetForMakeShiftDate(employeePosition, firstDate, false, timePosition, shifts);
        }
        return shift;
    }

    /**
     * Для позиции сотрудника берет последнюю смену в месяце, если ее нет то создает ее и возвращает ее значение
     *
     * @param employeePosition - позиция сотрудника
     */
    public static Shift getLastDayMonthShift(EmployeePosition employeePosition) {
        LocalDate lastDate = LocalDateTools.getLastDate();
        Shift shift = getShift(employeePosition, lastDate, null);
        if (shift == null) {
            ShiftTimePosition timePosition = ShiftTimePosition.FUTURE;
            List<Shift> shifts = getShifts(employeePosition, timePosition);
            return PresetClass.presetForMakeShiftDate(employeePosition, lastDate, false, timePosition, shifts);
        }
        return shift;
    }

    /**
     * Возвращает смену для позиции сотрудника за указанную дату
     *
     * @param employeePosition позиция сотрудника
     * @param date             дата смены
     * @param position         временной отрезок для выбора ростера. Nullable
     */
    public static Shift getShift(EmployeePosition employeePosition, LocalDate date, ShiftTimePosition position) {
        int empPosId = employeePosition.getId();
        if (position == null) {
            if (LocalDate.now().equals(LocalDateTools.getFirstDate())) {
                position = ShiftTimePosition.FUTURE;
            } else {
                position = date.isAfter(LocalDate.now().minusDays(1)) ? ShiftTimePosition.FUTURE : ShiftTimePosition.PAST;
            }
        }
        Roster activeRoster = RosterRepository.getNeededRosterId(position, new DateInterval(date.with(TemporalAdjusters.firstDayOfMonth()),
                                                                                            date.with(TemporalAdjusters.lastDayOfMonth())),
                                                                 employeePosition.getOrgUnit().getId());

        String urlEnding = makePath(ROSTERS, activeRoster.getId(), SHIFTS);
        List<NameValuePair> pairs = Pairs.newBuilder().from(date).to(date).build();
        JSONObject futureObject = RequestFormers.getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, urlEnding, pairs);
        return getListFromJsonObject(futureObject, Shift.class).stream()
                .filter(shift -> shift.getEmployeePositionId() == empPosId
                        && shift.getDateTimeInterval().getStartDate().equals(date))
                .min(Comparator.comparing(c -> c.getComment().isEmpty()))
                .orElse(null);
    }

    /**
     * Для отображения id смен у конкретного сотрудника за все числа в указанный период
     *
     * @param timePosition     период времени в будущем или настоящем от текущей даты
     * @param employeePosition позиция сотрудника
     * @return список смен в указанном периоде для позиции сотрудника
     */
    public static List<Shift> getShifts(EmployeePosition employeePosition, ShiftTimePosition timePosition) {
        Roster activeRoster = RosterRepository.getNeededRosterId(timePosition, new DateInterval(), employeePosition.getOrgUnit().getId());
        String urlEnding = makePath(ROSTERS, activeRoster.getId(), SHIFTS);
        DateInterval neededDateInterval = timePosition.getShiftsDateInterval();
        List<NameValuePair> pairs = Pairs.newBuilder().from(neededDateInterval.startDate).to(neededDateInterval.endDate).build();
        JSONObject futureObject = RequestFormers.getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, urlEnding, pairs);
        return getListFromJsonObject(futureObject, Shift.class).stream()
                .filter(shift -> shift.getEmployeePositionId() == employeePosition.getId())
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Возвращает список смен для всех позиций сотрудника в данном временном промежутке
     *
     * @param eps          позиция сотрудника
     * @param timePosition период времени в будущем или настоящем от текущей даты
     * @return список смен в указанном периоде для всех позиций сотрудника
     */
    public static List<Shift> getShifts(List<EmployeePosition> eps, ShiftTimePosition timePosition) {
        List<Shift> shifts = new ArrayList<>();
        for (EmployeePosition ep : eps) {
            shifts.addAll(getShifts(ep, timePosition));
        }
        return shifts;
    }

    /**
     * Достает все смены из ростера за заданный промежуток времени
     *
     * @param rosterId         id нужного ростера
     * @param dateTimeInterval период, за который нужны смены
     * @return список смен
     */
    public static List<Shift> getShiftsForRoster(int rosterId, DateInterval dateTimeInterval) {
        List<NameValuePair> pairs = Pairs.newBuilder()
                .from(dateTimeInterval.getStartDate())
                .to(dateTimeInterval.getEndDate())
                .build();
        String urlEnding = makePath(ROSTERS, rosterId, SHIFTS);
        JSONObject shiftsListJson = RequestFormers.getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, urlEnding, pairs);
        return getListFromJsonObject(shiftsListJson, Shift.class);
    }

    /**
     * Возвращает смены сотрудника в текущем месяце до указанной даты
     */
    public static List<Shift> getShiftsBeforeDate(EmployeePosition ep, LocalDate date) {
        return getShifts(ep, ShiftTimePosition.ALLMONTH)
                .stream()
                .filter(s -> s.getDateTimeInterval().getStartDate().isBefore(date))
                .sorted()
                .collect(Collectors.toList());
    }

    public static Shift getShiftById(int id) {
        String urlEnding = makePath(SHIFTS, id);
        JSONObject futureObject = RequestFormers.getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, urlEnding);
        return getClassObjectFromJson(Shift.class, futureObject);
    }

    /**
     * Возвращает свободные смены на заданную дату
     *
     * @param omId айди подразделения
     * @param date дата, за которую нужно получить свободные смены
     */
    public static List<Shift> getFreeShifts(int omId, LocalDate date) {
        return getFreeShifts(omId, new DateInterval(date));
    }

    /**
     * Возвращает свободные смены на заданный период
     *
     * @param omId  айди подразделения
     * @param dates даты, за которые нужно получить свободные смены
     */
    public static List<Shift> getFreeShifts(int omId, DateInterval dates) {
        return ShiftRepository.getShiftsForRoster(RosterRepository.getActiveRosterThisMonth(omId).getId(), dates)
                .stream()
                .filter(s -> s.getEmployeePositionId() == 0 || s.getEmployeePositionId() == null)
                .collect(Collectors.toList());
    }

    /**
     * Возвращает смены для сотрудника в указанном диапазоне
     *
     * @param employeeId   айди сотрудника
     * @param dateInterval временной интервал для поиска
     */
    public static List<Shift> getEmployeeSelfShifts(int employeeId, DateInterval dateInterval) {
        List<NameValuePair> nameValuePairs = Pairs.newBuilder()
                .from(dateInterval.startDate)
                .to(dateInterval.endDate)
                .excludeOther(true)
                .build();
        JSONObject someObject = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, makePath(EMPLOYEES, employeeId, SHIFTS), nameValuePairs);
        return getListFromJsonObject(someObject, Shift.class);
    }

    /**
     * Вернуть свободные ячейки сотрудника в расписании
     */
    public static List<LocalDate> emptySells(EmployeePosition employeePosition) {
        List<Shift> shifts = ShiftRepository.getEmployeeSelfShifts(employeePosition.getEmployee().getId(),
                ShiftTimePosition.FUTURE.getShiftsDateInterval());
        LocalDate today = LocalDate.now();
        return IntStream.range(0, today.lengthOfMonth() - today.getDayOfMonth() + 1)
                .mapToObj(today::plusDays)
                .filter(date -> shifts.stream()
                        .map(Shift::getDateTimeInterval)
                        .map(DateTimeInterval::getStartDate)
                        .noneMatch(date::isEqual)).collect(Collectors.toList());
    }
    /**
     * Ищет свободную смену в подразделении по времени, jobTitle и признаку outstaff
     */
    public static Shift getFreeShift(int omId, DateTimeInterval interval, JobTitle jobTitle, Boolean outstaff){
        List<Shift> shifts = getFreeShifts(omId,interval.toDateInterval());
        return shifts.stream()
                            .filter(s -> Objects.equals(s.getJobTitle(), jobTitle.getFullName()) && s.getOutstaff() == outstaff)
                            .findAny()
                            .orElse(null);
    }

    public static List<Shift> getShiftsByEmployeePositionAndRoster(EmployeePosition employeePosition, DateInterval dateInterval, Roster roster){
        return ShiftRepository.getShiftsForRoster(roster.getId(), dateInterval)
                .stream().filter(e -> e.getEmployeePositionId().equals(employeePosition.getId()))
                .collect(Collectors.toList());
    }
}

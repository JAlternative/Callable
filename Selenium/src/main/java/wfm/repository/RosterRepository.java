package wfm.repository;

import org.apache.http.NameValuePair;
import org.json.JSONObject;
import utils.Projects;
import utils.tools.LocalDateTools;
import utils.tools.Pairs;
import wfm.PresetClass;
import wfm.components.schedule.ShiftTimePosition;
import wfm.models.DateInterval;
import wfm.models.OrgUnit;
import wfm.models.Roster;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import static utils.ErrorMessagesForReport.NO_TEST_DATA;
import static utils.Links.ORGANIZATION_UNITS;
import static utils.Links.ROSTERS;
import static utils.tools.CustomTools.*;
import static utils.tools.RequestFormers.getJsonFromUri;
import static utils.tools.RequestFormers.makePath;

/**
 * @author Evgeny Gurkin 19.08.2020
 */
public class RosterRepository {

    private RosterRepository() {
    }

    /**
     * Берет объект активного ростера
     *
     * @param omNumber - номер оргюнита
     */
    public static Roster getActiveRoster(int omNumber, DateInterval dateInterval) {
        List<Roster> rosters = getRosters(omNumber, dateInterval, true);
        if (rosters.isEmpty()) {
            throw new AssertionError(NO_TEST_DATA + "У подразделения нет ростеров");
        }
        return getRosters(omNumber, dateInterval, true).get(0);
    }

    /**
     * Возвращает активный ростер на следующий месяц для выбранного ОМ
     *
     * @param omId - id текущего ОМ
     * @return объект Roster
     */
    public static Roster getActiveRosterNextMonth(int omId) {
        return getActiveRoster(omId, new DateInterval(LocalDateTools.getFirstDate().plusMonths(1),
                                                      LocalDateTools.getLastDate().plusMonths(1)));
    }

    /**
     * Берет объект ростера c отработанным временем
     *
     * @param omNumber - номер оргюнита
     */
    private static Roster getWorkedRoster(int omNumber, DateInterval dateInterval) {
        List<Roster> rosters = getRosters(omNumber, dateInterval, false);
        for (Roster roster : rosters) {
            if (roster.isWorked()) {
                return roster;
            }
        }
        return getZeroRoster(omNumber, dateInterval);
    }

    public static Roster getWorkedRosterPrevMonth(int omId) {
        return getWorkedRoster(omId, new DateInterval(LocalDateTools.getFirstDate().minusMonths(1),
                                                      LocalDateTools.getFirstDate().minusDays(1)));
    }

    public static Roster getZeroRosterOrReturnNull(int omId, DateInterval interval) {
        return RosterRepository.getRosters(omId, interval, false)
                .stream()
                .filter(r -> r.getVersion() == 0)
                .findFirst()
                .orElse(null);
    }

    /**
     * Берет объект нулевого ростера
     *
     * @param omNumber - номер оргюнита
     */
    public static Roster getZeroRoster(int omNumber, DateInterval dateInterval) {
        final List<Roster> roster = getRosters(omNumber, dateInterval, false);
        Roster rosterZero = roster.stream().filter(r -> r.getVersion() == 0).collect(randomItem());
        if (rosterZero == null) {
            return getActiveRoster(omNumber, dateInterval);
        }
        return rosterZero;
    }

    /**
     * Возвращает список активных или не активных ростеров для оргюнита, за указанный временной интервал
     *
     * @param omNumber     - айди оргюнита
     * @param dateInterval - временной интервал для поиска ростеров
     * @param isActive     - ищем активный ростер или все
     */
    public static List<Roster> getRosters(int omNumber, DateInterval dateInterval, boolean isActive) {
        String urlEnding = makePath(ORGANIZATION_UNITS, omNumber, ROSTERS);
        List<NameValuePair> nameValuePairs = Pairs.newBuilder().from(dateInterval.getStartDate())
                .to(dateInterval.getEndDate()).onlyActive(isActive).build();
        JSONObject published = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, urlEnding, nameValuePairs);
        return getListFromJsonObject(published, Roster.class);
    }

    /**
     * Берет список всех ростеров за текущий месяц для указанного айди оргюнита
     */
    public static List<Roster> getRosters(int omId) {
        return getRosters(omId, new DateInterval(LocalDate.now(), LocalDateTools.getLastDate()), false);
    }

    /**
     * Возвращает ростеры на текущий месяц для выбранного ОМ
     *
     * @param omId - id текущего ОМ
     * @return джисон массив ростеров
     */
    public static Roster getActiveRosterThisMonth(int omId) {
        return getActiveRoster(omId, new DateInterval());
    }

    /**
     * Возвращает ростеры на текущий месяц для выбранного ОМ
     *
     * @param omId - id текущего ОМ
     * @return джисон массив ростеров
     */
    public static Roster getWorkedRosterThisMonth(int omId) {
        return getWorkedRoster(omId, new DateInterval());
    }

    /**
     * Собирает айди ростеров и затем возвращает случайный
     */
    public static Roster getRandomRoster(OrgUnit orgUnit) {
        List<Roster> rosters = getRosters(orgUnit.getId());
        return getRandomFromList(rosters);
    }

    /**
     * Выбираем ростер исхода из того где будет больше смен
     *
     * @param timePosition - от текущей даты (true) или от начала месяца
     * @param omId         - id текущего ОМ
     * @return id ростера в виде строки
     */
    public static Roster getRosterId(ShiftTimePosition timePosition, int omId) {
        if (timePosition == ShiftTimePosition.DEFAULT && LocalDate.now().getDayOfMonth() > 15 || timePosition == ShiftTimePosition.PAST) {
            return getZeroRoster(omId, new DateInterval());
        } else {
            return getActiveRoster(omId, new DateInterval());
        }
    }

    /**
     * Возвращает нулевой или активный ростер в зависимости от типа выбранного интервала, заданных дат и айди оргюнита.
     * В прошлых датах выбирается нулевой ростер, а в будущих активный ростер
     *
     * @param timePosition - в какоих датах нужен ростер
     * @param dateInterval - интервал дат для поиска ростеров
     * @param omId         - айди оргюнита
     * @return - айди ростера в виде строки
     */
    public static Roster getNeededRosterId(ShiftTimePosition timePosition, DateInterval dateInterval, int omId) {
        int currentDayIndex = LocalDate.now().getDayOfMonth();
        if (ShiftTimePosition.DEFAULT == timePosition && currentDayIndex >= 15) {
            return getZeroRoster(omId, dateInterval);
        } else if (ShiftTimePosition.PAST == timePosition || ShiftTimePosition.PAST_MONTH == timePosition) {
            return getZeroRoster(omId, dateInterval);
        } else if (ShiftTimePosition.PREVIOUS_MONTH == timePosition) {
            return getZeroRoster(omId, timePosition.getShiftsDateInterval());
        } else if (ShiftTimePosition.NEXT_MONTH == timePosition) {
            try {
                getActiveRoster(omId, timePosition.getShiftsDateInterval());
            } catch (AssertionError assertionError) {
                PresetClass.createEmptyPlannedRoster(omId, timePosition.getShiftsDateInterval().getStartDate());
            }
            return getActiveRoster(omId, timePosition.getShiftsDateInterval());
        } else {
            return getActiveRoster(omId, dateInterval);
        }
    }
}

package wfm.repository;

import com.google.common.base.Joiner;
import io.qameta.allure.Allure;
import io.qameta.allure.Step;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.http.NameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import utils.Links;
import utils.Projects;
import utils.db.DBUtils;
import utils.tools.ExcludeOmList;
import utils.tools.LocalDateTools;
import utils.tools.Pairs;
import wfm.PresetClass;
import wfm.components.calculation.FilterType;
import wfm.components.orgstructure.OrgUnitOptions;
import wfm.components.orgstructure.OrganizationUnitTypeId;
import wfm.components.schedule.DateUnit;
import wfm.components.schedule.KPIOrFTE;
import wfm.components.schedule.ScheduleType;
import wfm.components.utils.OmDbType;
import wfm.models.*;

import java.net.URI;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

import static utils.ErrorMessagesForReport.NO_TEST_DATA;
import static utils.Links.*;
import static utils.Params.*;
import static utils.db.DBUtils.hasLimitsInCurrentMonth;
import static utils.db.DBUtils.hasRosterNotLocked;
import static utils.tools.CustomTools.*;
import static utils.tools.RequestFormers.*;
import static wfm.repository.CommonRepository.URL_BASE;
import static wfm.repository.CommonRepository.getAllOrgUnitTypes;

/**
 * @author Evgeny Gurkin 19.08.2020
 */
public class OrgUnitRepository {
    private static final String ATTACHMENT_TITLE = "Выбор оргюнита";
    private static final String SELECTED_STRING_PART = "Был выбран оргюнит ";
    private static final String SELECTED_STRING_LOGGER = "Был выбран оргюнит {}";
    private static final String SELECTED_MONTH_ALLURE_CONTENTS = "Был выбран %s месяц оргюнита %s";
    private static final String SELECTED_FITTING_STATE = "В оргЮните {} в {} месяц благоприятная ситуация для теста ";

    private static final String ORG_UNIT_NOT_FOUND_ERROR_MESSAGE = NO_TEST_DATA + "Не найдено оргюнитов, соответствующих заданным условиям";
    private static final String NUMBER_OF_ORG_UNITS = "Количество оргюнитов: {}";
    private static final Logger LOG = LoggerFactory.getLogger(OrgUnitRepository.class);

    private OrgUnitRepository() {
    }

    /**
     * Взять оргюнит по айди
     */
    public static OrgUnit getOrgUnit(int id) {
        String urlEnding = makePath(ORGANIZATION_UNITS, id);
        JSONObject someObject = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, urlEnding);
        return new OrgUnit(someObject);
    }

    /**
     * Берет все незакрытые оргюниты
     *
     * @param onlyStore - только магазины или все оргюниты
     * @return лист оргюнитов
     */
    public static List<OrgUnit> getAllOrgUnits(boolean onlyStore) {
        JSONObject orgNameArray = getObjectsOrgUnits(CommonRepository.URL_BASE, true, onlyStore);
        return getListFromJsonObject(orgNameArray, OrgUnit.class);
    }

    /**
     * Берет все незакрытые оргюниты, доступные для расчета
     *
     * @param onlyStore - только магазины или все оргюниты
     * @return лист оргюнитов
     */
    public static List<OrgUnit> getAllAvailableOrgUnits(boolean onlyStore) {
        JSONObject orgNameArray = getObjectsOrgUnits(CommonRepository.URL_BASE, true, onlyStore);
        return getListFromJsonObject(orgNameArray, OrgUnit.class).stream()
                .filter(OrgUnit::isAvailableForCalculation)
                .collect(Collectors.toList());
    }

    public static List<OrgUnit> getAllLowLevelChildrenOrgUnits(int parentId) {
        JSONObject jsonObject = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, makePath(ORGANIZATION_UNITS, parentId, ALL_LOW_LEVEL_CHILDREN));
        return getListFromJsonObject(jsonObject, OrgUnit.class);
    }

    /**
     * Выбирает случайный оргЮнит из списка незакрытых магазинов доступных для рассчета. метод для аналитики и расписания
     *
     * @return - класс оргЮнит
     */
    public static OrgUnit getRandomAvailableOrgUnit() {
        List<OrgUnit> allStore = getAllAvailableOrgUnits(true);
        OrgUnit unit = getRandomFromList(allStore);
        Allure.addAttachment(ATTACHMENT_TITLE, SELECTED_STRING_PART + unit.getName()
                + ", не противоречащий набору оргюнитов для остальных тестов.");
        return unit;
    }

    /**
     * Выюирает случайный оргюнит в зависимости от того какой тип расписания нам нужен
     *
     * @param scheduleType ANY_TYPE - любой оргюнит с графиком
     *                     остальные варианты ищут хотя бы 2 графика, где есть название выбранного типа графика
     */
    public static OrgUnit getRandomStoreWithBusinessHours(ScheduleType scheduleType) {
        List<OrgUnit> orgUnits = getAllOrgUnits(true);
        Collections.shuffle(orgUnits);
        for (OrgUnit orgUnit : orgUnits) {
            if (PresetClass.checkOrgUnitBusinessHours(scheduleType, orgUnit)) {
                return orgUnit;
            }
        }
        throw new AssertionError(ORG_UNIT_NOT_FOUND_ERROR_MESSAGE);
    }

    /**
     * Выбирает оргюнит, который не находится в списках исключения по заданным условиям
     *
     * @param options выбираем условие для оргюнита
     *                WITH_FREE_POSITIONS - с свободными должностями
     *                EMPLOYEES_WITH_POSITION - сотрудники с должностями
     *                EMPLOYEES_WITHOUT_POSITION - соттрудники без должностей
     */
    public static OrgUnit getRandomOmWithEmployeeOptions(OrgUnitOptions options) {
        List<OrgUnit> allStore = getAllAvailableOrgUnits(true);
        Collections.shuffle(allStore);
        for (OrgUnit orgUnit : allStore) {
            if (!PositionRepository.getPositionsArray(orgUnit.getId()).isEmpty() && checkOrgUnitForOptions(orgUnit, options)) {
                return orgUnit;
            }
        }
        LOG.info("Найти подразделение не удалось, по условию выбора: {}", options);
        throw new AssertionError(ORG_UNIT_NOT_FOUND_ERROR_MESSAGE);
    }

    private static boolean checkOrgUnitForOptions(OrgUnit orgUnit, OrgUnitOptions options) {
        switch (options) {
            case WITH_FREE_POSITIONS:
                /* Смотрим все эмплоепозишены и позишены оргюнита. Потом убираем из позишенов все что было
                найдено в эмплоепозишене, если в позишенах что-то осталось, то берем оргюнит. Проверка на более,
                чем 1, потому что там иногда может оставаться 1 позишен, мб шеф, хотя он должен вычитаться из списка. */
                return checkOrgUnitForFreePositions(orgUnit);
            case EMPLOYEES_WITH_POSITION:
                return checkOrgUnitForEmployeesWithPositions(orgUnit);
            case WITH_POSITION_GROUPS:
                return checkOrgUnitForPositionGroups(orgUnit);
            default:
                throw new AssertionError("Данная опция не поддерживается методом " + options);
        }
    }

    private static boolean checkOrgUnitForFreePositions(OrgUnit orgUnit) {
        List<Integer> allPositionsWithEmp = EmployeePositionRepository.getEmployeePositions(orgUnit.getId()).stream()
                .map(EmployeePosition::getPosition)
                .map(Position::getId)
                .collect(Collectors.toList());
        List<Integer> positions = PositionRepository.getPositionsArray(orgUnit.getId()).stream()
                .map(Position::getId)
                .collect(Collectors.toList());
        positions.removeAll(allPositionsWithEmp);
        if (positions.size() > 1) {
            Allure.addAttachment(ATTACHMENT_TITLE, SELECTED_STRING_PART + orgUnit.getName()
                    + ", у которого имеются пустые должности");
            LOG.info(SELECTED_STRING_LOGGER, orgUnit.getName());
            return true;
        }
        return false;
    }

    private static boolean checkOrgUnitForEmployeesWithPositions(OrgUnit orgUnit) {
        if (EmployeePositionRepository.getEmployeePositionsWithoutFiredEmployees(orgUnit.getId(), LocalDateTools.getLastDate(), false).size() > 1) {
            Allure.addAttachment(ATTACHMENT_TITLE, SELECTED_STRING_PART + orgUnit.getName()
                    + ", у которого есть сотрудники с должностью");
            LOG.info(SELECTED_STRING_LOGGER, orgUnit.getName());
            return true;
        }
        return false;
    }

    private static boolean checkOrgUnitForPositionGroups(OrgUnit orgUnit) {
        if (EmployeePositionRepository.getEmployeePositionsWithoutFiredEmployees(orgUnit.getId(), LocalDateTools.getLastDate(), false).size() > 1) {
            List<EmployeePosition> employeePositions = EmployeePositionRepository.getEmployeePositions(orgUnit.getId());
            Set<Integer> actualEmployeeGroups = employeePositions.stream()
                    .map(EmployeePosition::getPosition)
                    .map(Position::getPositionGroupId)
                    .collect(Collectors.toSet());
            if (actualEmployeeGroups.size() > 2) {
                Allure.addAttachment(ATTACHMENT_TITLE, SELECTED_STRING_PART + orgUnit.getName()
                        + ", у которого есть сотрудники с должностью и более двух групп позиций");
                LOG.info(SELECTED_STRING_LOGGER, orgUnit.getName());
                return true;
            }
        }
        return false;
    }

    /**
     * Выбирает из списка магазинов случайный оргюнит, у которого есть ростер со сменами и больше 1 сотрудника с должностью,
     * затем проверяет KPI и FTE; если надо, делает запрос.
     */
    public static OrgUnit getRandomOrgUnitsForShifts() {
        return getRandomOrgUnitsForShifts(true, false, false, false,
                                          false, new DateInterval(LocalDate.now(), LocalDateTools.getLastDate()));
    }

    /**
     * @param checkByPositionCategoryId Чтобы при выборе оргюнита
     *                                  нам попадались те, у кого хотя бы два сотрудника с одинаковым positionCategoryId, ставим true
     * @param checkContainingLimits     Проверить оргюниты на содержание лимитов часов
     */
    public static OrgUnit getRandomOrgUnitsForShifts(boolean onlyStore, boolean checkByPositionCategoryId, boolean checkContainingLimits,
                                                     boolean checkRosterLocked, boolean checkEmployeeByApi) {
        return getRandomOrgUnitsForShifts(onlyStore, checkByPositionCategoryId, checkContainingLimits, checkRosterLocked, checkEmployeeByApi,
                                          new DateInterval(LocalDate.now(), LocalDateTools.getLastDate()));
    }

    /**
     * Находит оргюнит, у которого есть расписание на ещё один месяц, помимо текущего
     *
     * @param checkByPositionCategoryId Чтобы при выборе оргюнита
     *                                  нам попадались те, у кого хотя бы два сотрудника с одинаковым positionCategoryId, ставим true
     * @param checkContainingLimits     Проверить оргюниты на содержание лимитов часов
     * @param dateInterval              интервал месяца, у которого должно быть расписание
     */
    public static OrgUnit getRandomOrgUnitsForShifts(boolean onlyStore, boolean checkByPositionCategoryId, boolean checkContainingLimits,
                                                     boolean checkRosterLocked, boolean checkEmployeeByApi, DateInterval dateInterval) {
        List<OrgUnit> allStore = getAllAvailableOrgUnits(onlyStore);
        Collections.shuffle(allStore);
        for (OrgUnit orgUnit : allStore) {
            int id = orgUnit.getId();
            boolean hasFutureRosters = !RosterRepository.getRosters(id, new DateInterval(LocalDate.now(), LocalDateTools.getLastDate()), true).isEmpty();
            boolean hasFutureRostersOtherMons = !RosterRepository.getRosters(id, dateInterval, true).isEmpty();
            boolean hasActiveEmployees = EmployeePositionRepository.getEmployeePositionsWithoutFiredEmployees(id, dateInterval.getEndDate(), false).size() > 1;
            boolean hasOnPositionCategoryId = orgUnit.checkEmployeesForMatchOnPositionCategoryId(checkByPositionCategoryId);
            boolean hasLimitsInCurrentMonth = hasLimitsInCurrentMonth(orgUnit, checkContainingLimits);
            boolean hasRosterNotLocked = hasRosterNotLocked(orgUnit, checkRosterLocked);
            boolean hasCheckEmployeeByApi = hasCheckEmployeeByApi(id, checkEmployeeByApi);
            boolean hasShiftsInActiveRoster;
            try {
                int activeRosterId = RosterRepository.getActiveRosterThisMonth(id).getId();
                hasShiftsInActiveRoster = !ShiftRepository.getShiftsForRoster(activeRosterId, new DateInterval()).isEmpty();
            } catch (AssertionError e) {
                continue;
            }
            if (hasFutureRosters && hasFutureRostersOtherMons && hasActiveEmployees && hasShiftsInActiveRoster &&
                    hasOnPositionCategoryId && hasLimitsInCurrentMonth && hasRosterNotLocked && hasCheckEmployeeByApi) {
                PresetClass.kpiAndFteChecker(id);
                Allure.addAttachment(ATTACHMENT_TITLE, SELECTED_STRING_PART + orgUnit.getName()
                        + ", у которого есть сотрудники с должностью, рассчитанное расписание" + (checkByPositionCategoryId ? " и минимум два сотрудника с одинаковым positionCategoryId" : ""));
                LOG.info(SELECTED_STRING_LOGGER, orgUnit.getName());
                List<BusinessHours> businessDaysList = BusinessHoursRepository.scheduleType(orgUnit.getId());
                if (businessDaysList.isEmpty() && URL_BASE.contains("pochta")) {
                    DateInterval interval = new DateInterval(LocalDate.now().minusMonths(2), LocalDate.now().plusYears(1));
                    PresetClass.createBusinessHours(orgUnit.getId(), interval, "SALE");
                    PresetClass.createBusinessHours(orgUnit.getId(), interval, "SERVICE");
                }
                return orgUnit;
            }
        }
        throw new AssertionError(NO_TEST_DATA + "Не удалось найти подразделение, у которого есть " +
                                         "сотрудники с назначенными должностями, ростер со сменами, KPI и FTE");

    }

    /**
     * Возвращает подразделение без дочерних подразделений с первичным неопубликованным графиком и руководителем
     */
    public static OrgUnit getRandomLowLevelOrgUnitWithUnpublishedInitialRosterAndChief() {
        List<OrgUnit> allStore = getAllAvailableOrgUnits(true);
        Collections.shuffle(allStore);
        for (OrgUnit unit : allStore) {
            int omId = unit.getId();
            Roster roster;
            try {
                roster = RosterRepository.getActiveRosterThisMonth(omId);
            } catch (AssertionError e) {
                continue;
            }
            Roster rosterPublished = RosterRepository.getRosters(omId).stream().filter(Roster::isPublished).findAny().orElse(null);
            Position chiefPosition = PositionRepository.getChief(omId);
            if (rosterPublished == null && roster.getVersion() == 1 &&
                    chiefPosition != null && chiefPosition.getEmployee() != null) {
                Allure.addAttachment(ATTACHMENT_TITLE, SELECTED_STRING_PART + unit.getName()
                        + ", у которого есть неопубликованный первичный ростер");
                LOG.info(SELECTED_STRING_LOGGER, unit.getName());
                return unit;
            }
        }
        throw new AssertionError(NO_TEST_DATA + "Не удалось найти подразделение, у которого есть " +
                                         "неопубликованный первичный ростер");
    }

    /**
     * Возвращает подразделение, у которого есть опубликованный график
     */
    public static OrgUnit getRandomOrgUnitWithPublishedRoster() {
        OrgUnit unit = getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        if (RosterRepository.getRosters(omId).stream().filter(Roster::isPublished).findAny().orElse(null) == null) {
            PresetClass.makeRosterPublication(RosterRepository.getWorkedRosterThisMonth(omId).getId());
        }
        return unit;
    }

    /**
     * Выбирает случайный оргюнит из списка магазинов, у которого есть ростер со сменами и табель с отработанным временем
     */
    public static OrgUnit getRandomOrgUnitWithWorkedRosters(boolean onlyStore, boolean ifCheckFirstDay) {
        List<OrgUnit> allStore = getAllAvailableOrgUnits(onlyStore);
        Collections.shuffle(allStore);
        for (OrgUnit orgUnit : allStore) {
            int id = orgUnit.getId();
            try {
                RosterRepository.getActiveRosterThisMonth(id).getId();
            } catch (AssertionError e) {
                continue;
            }
            if ((!LocalDate.now().equals(LocalDateTools.getFirstDate()) && hasNonEmptyWorkedRoster(id))
                    || (LocalDate.now().equals(LocalDateTools.getFirstDate()) && hasNonEmptyWorkedRosterPrevMonth(id) && ifCheckFirstDay)) {
                Allure.addAttachment(ATTACHMENT_TITLE, SELECTED_STRING_PART + orgUnit.getName()
                        + ", у которого есть табель с отработанным временем");
                LOG.info(SELECTED_STRING_LOGGER, orgUnit.getName());
                return orgUnit;
            }
        }
        throw new AssertionError(NO_TEST_DATA + "Не было найдено оргюнитов c табелем отработанного времени");
    }

    public static OrgUnit getRandomOrgUnitWithWorkedRosters(boolean onlyStore) {
        List<OrgUnit> allStore = getAllAvailableOrgUnits(onlyStore);
        Collections.shuffle(allStore);
        for (OrgUnit orgUnit : allStore) {
            int id = orgUnit.getId();
            int rosterId;
            try {
                rosterId = RosterRepository.getActiveRosterThisMonth(id).getId();
            } catch (AssertionError e) {
                continue;
            }
            List<Shift> shifts = ShiftRepository.getShiftsForRoster(rosterId, new DateInterval());
            if (!shifts.isEmpty() && hasNonEmptyWorkedRoster(id)) {
                Allure.addAttachment(ATTACHMENT_TITLE, SELECTED_STRING_PART + orgUnit.getName()
                        + ", у которого есть табель с отработанным временем");
                LOG.info(SELECTED_STRING_LOGGER, orgUnit.getName());
                return orgUnit;
            }
        }
        throw new AssertionError(NO_TEST_DATA + "Не было найдено оргюнитов c табелем отработанного времени");
    }

    /**
     * Выбирает случайный оргюнит из списка магазинов, у которого есть ростер со сменами и неутвержденный табель с отработанным временем
     */
    public static OrgUnit getRandomOrgUnitWithNotApprovedWorkedRoster() {
        List<OrgUnit> allStore = getAllAvailableOrgUnits(true);
        Collections.shuffle(allStore);
        for (OrgUnit orgUnit : allStore) {
            int id = orgUnit.getId();
            int rosterId;
            try {
                rosterId = RosterRepository.getActiveRosterThisMonth(id).getId();
            } catch (AssertionError e) {
                continue;
            }
            List<Shift> shifts = ShiftRepository.getShiftsForRoster(rosterId, new DateInterval());
            if (!shifts.isEmpty() && hasNonEmptyWorkedRoster(id) && hasNotApprovedWorkedRoster(id)) {
                Allure.addAttachment(ATTACHMENT_TITLE, String.format("%s %s, у которого есть неутвержденный табель с отработанным временем", SELECTED_STRING_PART, orgUnit.getName()));
                LOG.info(SELECTED_STRING_LOGGER, orgUnit.getName());
                return orgUnit;
            }
        }
        throw new AssertionError(NO_TEST_DATA + "Не было найдено оргюнитов c неутвержденным табелем отработанного времени");
    }

    private static boolean hasNonEmptyWorkedRoster(int omId) {
        Roster workedRoster = RosterRepository.getWorkedRosterThisMonth(omId);
        if (workedRoster.getVersion() == 0) {
            List<Shift> workedShifts = ShiftRepository.getShiftsForRoster(workedRoster.getId(), new DateInterval());
            return !workedShifts.isEmpty();
        }
        return false;
    }

    private static boolean hasNonEmptyWorkedRosterPrevMonth(int omId) {
        Roster workedRoster = RosterRepository.getWorkedRosterPrevMonth(omId);
        if (workedRoster.getVersion() == 0) {
            List<Shift> workedShifts = ShiftRepository.getShiftsForRoster(workedRoster.getId(), new DateInterval(LocalDateTools.getFirstDate().minusMonths(1),
                                                                                                                 LocalDateTools.getFirstDate().minusDays(1)));
            return !workedShifts.isEmpty();
        }
        return false;
    }

    private static boolean hasNotApprovedWorkedRoster(int omId) {
        String workedApprove = RosterRepository.getWorkedRosterThisMonth(omId).getWorkedApprove();
        if (workedApprove.equals("") || LocalDate.parse(workedApprove).isBefore(LocalDate.now().minusDays(1))) {
            return true;
        } else {
            return LocalDate.parse(workedApprove).isBefore(LocalDate.now().minusDays(1));
        }
    }

    private static boolean hasCheckEmployeeByApi(int omId, boolean checkEmployeeByApi) {
        if (!checkEmployeeByApi) {
            return true;
        } else {
            return !EmployeePositionRepository.getAllEmployeesWithCheckByApi(omId, null, true).isEmpty();
        }
    }

    private static boolean hasCheckFtePositionGroupId(boolean checkFtePositionGroupId, Position position) {
        return !checkFtePositionGroupId || !Objects.isNull(DBUtils.getFtePositionGroupId(position));
    }

    /**
     * Выбирает случайный оргюнит из списка, у которого есть рассчитанное расписание
     *
     * @return оргюнит
     */
    public static ImmutablePair<OrgUnit, Roster> getRandomOrgUnitWithActiveRoster() {
        List<OrgUnit> allStore = getAllAvailableOrgUnits(true);
        Collections.shuffle(allStore);
        Roster roster;
        for (OrgUnit orgUnit : allStore) {
            int id = orgUnit.getId();
            try {
                roster = RosterRepository.getActiveRosterThisMonth(id);
            } catch (AssertionError e) {
                continue;
            }
            Allure.addAttachment(ATTACHMENT_TITLE, SELECTED_STRING_PART + orgUnit.getName()
                    + ", у которого есть активный ростер с id " + roster.getId());
            return new ImmutablePair<>(orgUnit, roster);
        }
        throw new AssertionError(NO_TEST_DATA + "Не было найдено оргюнитов c рассчитанным расписанием");
    }

    /**
     * Случайный оргюнит для аналитики с FTE
     */
    public static OrgUnit getRandomFteUnit() {
        List<OrgUnit> orgUnits = getAllAvailableOrgUnits(true);
        List<NameValuePair> pairs = Pairs.newBuilder()
                .from(LocalDate.now())
                .level(1)
                .to(LocalDate.now().with(TemporalAdjusters.lastDayOfYear()))
                .build();
        for (OrgUnit orgUnit : orgUnits) {
            JSONArray array = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, makePath(ORGANIZATION_UNITS, orgUnit.getId(), FTE_GROUPS), pairs)
                    .getJSONObject(EMBEDDED).getJSONArray("fteList");
            for (int i = 0; i < array.length(); i++) {
                JSONObject temp = array.getJSONObject(i);
                if (temp.getJSONObject("values").getDouble("FULL") > 0.0) {
                    return orgUnit;
                }
            }
        }
        throw new AssertionError(ORG_UNIT_NOT_FOUND_ERROR_MESSAGE);
    }

    /**
     * Возвращает оргюнит без значий коррекций KPI или FTE
     *
     * @param kpiOrFTE - смотрит по KPI/FTE или по обоим
     */
    public static OrgUnit getUnitWithNotZeroCorrectionNumber(KPIOrFTE kpiOrFTE) {
        List<OrgUnit> allOrgUnits = getAllAvailableOrgUnits(true);
        Collections.shuffle(allOrgUnits);
        String path = KPIOrFTE.KPI_FORECAST == kpiOrFTE ? KPI_FORECAST_CORRECTION_SESSION : KPI_CORRECTION_SESSION;
        for (OrgUnit unit : allOrgUnits) {
            try {
                int number = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, makePath(ORGANIZATION_UNITS, unit.getId(), KPI,
                                                                                              1, path)).getInt("number");
                if (number > 1) {
                    return unit;
                }
            } catch (AssertionError e) {
                LOG.info("Коррекции у оргюнита {} не было", unit.getId());
            }
        }
        throw new AssertionError(ORG_UNIT_NOT_FOUND_ERROR_MESSAGE);
    }

    /**
     * Берет случайный оргюнит у котрого есть мат параметры с данными
     */
    public static OrgUnit getOrgUnitsWithRightMatchParameters() {
        List<OrgUnit> units = getAllNotClosedOrgUnits().stream()
                .filter(OrgUnit::isAvailableForCalculation)
                .collect(Collectors.toList());
        Collections.shuffle(units);
        for (OrgUnit unit : units) {
            int id = unit.getId();
            String urlEnding = makePath(ORGANIZATION_UNITS, id, MATH_PARAMETER_VALUES);
            JSONObject objectConversion;
            JSONObject objectOperations;
            JSONObject objectTraffic;
            try {
                objectConversion = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, makePath(urlEnding, 2));
            } catch (AssertionError e) {
                continue;
            }
            try {
                objectOperations = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, makePath(urlEnding, 3));
            } catch (AssertionError e) {
                continue;
            }
            try {
                objectTraffic = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, makePath(urlEnding, 170));
            } catch (AssertionError e) {
                continue;
            }
            double valueConversion = objectConversion.getDouble(VALUE);
            int valueOperations = objectOperations.getInt(VALUE);
            int valueTraffic = objectTraffic.getInt(VALUE);
            if (valueConversion < 0 || valueConversion > 1) {
                continue;
            }
            if (valueOperations <= 10 || valueOperations >= 50) {
                continue;
            }
            if (valueTraffic <= 10 || valueTraffic >= 50) {
                continue;
            }
            return unit;
        }
        throw new AssertionError(ORG_UNIT_NOT_FOUND_ERROR_MESSAGE);
    }

    @Step("Поиск ОМ с рассчитанным/нерасчитанным {kpiOrFTE} на месяц")
    public static ImmutablePair<OrgUnit, LocalDate> getOrgUnitOptions(boolean exist, KPIOrFTE kpiOrFTE) {
        List<OrgUnit> allOrgUnits = getAllAvailableOrgUnits(true);
        List<Integer> months = new ArrayList<>();
        OrgUnit orgUnitResult = null;
        for (OrgUnit tempUnit : allOrgUnits) {
            int i = tempUnit.getId();
            if (kpiOrFTE == KPIOrFTE.KPI_HISTORY) {
                months = CommonRepository.getOrgUnitsKpiMonth(i, exist, false);
            } else if (kpiOrFTE == KPIOrFTE.FTE) {
                months = CommonRepository.getOrgUnitsFteMonth(i, exist, false);
                List<Integer> publicationMonths = CommonRepository.checkMonthsPublication(i, KPIOrFTE.KPI_FORECAST).stream()
                        .map(LocalDate::getMonthValue).collect(Collectors.toList());
                months.removeIf(publicationMonths::contains);
            } else if (CommonRepository.checkKpiValuesNotBroken(i) && kpiOrFTE == KPIOrFTE.KPI_OR_FTE) {
                months = CommonRepository.getOrgUnitsKpiMonth(i, exist, false);
                months.addAll(CommonRepository.getOrgUnitsFteMonth(i, exist, false));
            }
            if (!months.isEmpty()) {
                orgUnitResult = tempUnit;
                break;
            }
        }
        if (orgUnitResult == null) {
            throw new AssertionError(ORG_UNIT_NOT_FOUND_ERROR_MESSAGE);
        }
        int randomMonth = getRandomFromList(months);
        String status;
        if (exist) {
            status = "рассчитан";
        } else {
            status = "не рассчитан";
        }
        LOG.info(SELECTED_FITTING_STATE, orgUnitResult.getName(), randomMonth);
        Allure.addAttachment(ATTACHMENT_TITLE,
                             String.format(SELECTED_MONTH_ALLURE_CONTENTS, randomMonth, orgUnitResult.getName()) +
                                     "\n" + "Статус этого месяца " + status);
        return new ImmutablePair<>(orgUnitResult, LocalDate.now().withMonth(randomMonth));
    }

    /**
     * Выбирает оргюнит, в котором есть рассчитанный KPI без публикации на даты расчета
     */
    public static ImmutablePair<OrgUnit, LocalDate> findMonthWithOutPublication(KPIOrFTE kpiOrFTE, boolean exist) {
        List<OrgUnit> allOrgUnits = getAllAvailableOrgUnits(true);
        List<Integer> months = new ArrayList<>();
        OrgUnit orgUnitResult = null;
        for (OrgUnit tempUnit : allOrgUnits) {
            int i = tempUnit.getId();
            if (kpiOrFTE == KPIOrFTE.KPI_HISTORY) {
                months = CommonRepository.getOrgUnitsKpiMonth(i, exist, false);
            } else if (kpiOrFTE == KPIOrFTE.FTE) {
                months = CommonRepository.getOrgUnitsFteMonth(i, exist, false);
            } else {
                months = CommonRepository.getOrgUnitsKpiMonth(i, exist, false);
                months.addAll(CommonRepository.getOrgUnitsFteMonth(i, exist, false));
            }
            if (!months.isEmpty()) {
                List<LocalDate> publicationMonths = CommonRepository.checkMonthsPublication(i, kpiOrFTE);
                if (publicationMonths.isEmpty()) {
                    orgUnitResult = tempUnit;
                    break;
                } else {
                    List<Integer> monthWithPublication = publicationMonths.stream()
                            .filter(localDate -> localDate.getYear() == LocalDate.now().getYear())
                            .map(LocalDate::getMonthValue).collect(Collectors.toList());
                    months.removeAll(monthWithPublication);
                    if (!months.isEmpty()) {
                        orgUnitResult = tempUnit;
                        break;
                    }
                }
            }
        }
        if (orgUnitResult == null) {
            throw new AssertionError(ORG_UNIT_NOT_FOUND_ERROR_MESSAGE);
        }
        int randomMonth = !months.isEmpty() ? getRandomFromList(months) : 1;
        LOG.info(SELECTED_FITTING_STATE, orgUnitResult.getName(), randomMonth);
        Allure.addAttachment(ATTACHMENT_TITLE,
                             String.format(SELECTED_MONTH_ALLURE_CONTENTS, randomMonth, orgUnitResult.getName()));
        return new ImmutablePair<>(orgUnitResult, LocalDate.now().withMonth(randomMonth));
    }

    @Step("Поиск ОМ с рассчитанным/не рассчитанным KPI и FTE на месяц")
    public static ImmutablePair<OrgUnit, LocalDate> getOrgUnitOptionsSpecial(boolean previousTrueJanuaryFalse) {
        List<OrgUnit> idList = getAllAvailableOrgUnits(true);
        List<Integer> months = null;
        OrgUnit unit = null;
        for (OrgUnit orgUnit : idList) {
            int orgId = orgUnit.getId();
            months = CommonRepository.getOrgUnitsKpiMonth(orgId, true, previousTrueJanuaryFalse);
            months.addAll(CommonRepository.getOrgUnitsFteMonth(orgId, true, previousTrueJanuaryFalse));
            if (!previousTrueJanuaryFalse && months.contains(1)) {
                unit = orgUnit;
                break;
            } else if (previousTrueJanuaryFalse && !months.isEmpty()) {
                unit = orgUnit;
            }
            break;
        }
        if (unit == null) {
            throw new AssertionError(ORG_UNIT_NOT_FOUND_ERROR_MESSAGE);
        }
        int randomMonth = getRandomFromList(months);
        LOG.info(SELECTED_FITTING_STATE, unit.getName(), randomMonth);
        Allure.addAttachment(ATTACHMENT_TITLE,
                             String.format(SELECTED_MONTH_ALLURE_CONTENTS, randomMonth, unit.getName()));
        return new ImmutablePair<>(unit, LocalDate.now().withMonth(randomMonth));
    }

    /**
     * берет случайный не сломанный оргюнит, сломанный он или нет проверяется по наличию kpi значений нулевого уровня
     */
    public static OrgUnit getRandomNotBrokenStore() {
        List<OrgUnit> tempList = getAllAvailableOrgUnits(true);
        Collections.shuffle(tempList);
        for (OrgUnit orgUnit : tempList) {
            if (CommonRepository.checkKpiValuesNotBroken(orgUnit.getId())) {
                Allure.addAttachment(ATTACHMENT_TITLE, SELECTED_STRING_PART + orgUnit.getName() +
                        "типа \"Торговая точка\", с валидными KPI данными нулевого уровня");
                return orgUnit;
            }
        }
        throw new AssertionError(ORG_UNIT_NOT_FOUND_ERROR_MESSAGE);
    }

    /**
     * берет оргюнит с наличием рабочего графика
     */
    public static OrgUnit selectUnitWithBusinessHours() {
        OrgUnit unit = new OrgUnit(new JSONObject());
        List<OrgUnit> orgUnits = getAllOrgUnits(true);
        for (OrgUnit orgUnit : orgUnits) {
            if (CommonRepository.checkKpiValuesNotBroken(orgUnit.getId())
                    && PresetClass.checkOrgUnitBusinessHours(ScheduleType.ANY_TYPE, orgUnit)) {
                unit = orgUnit;
                break;
            }
        }
        Allure.addAttachment(ATTACHMENT_TITLE, SELECTED_STRING_PART + unit.getName() +
                "типа \"Торговая точка\", с валидными KPI данными нулевого уровня и графиком работы");
        return unit;
    }

    /**
     * Берет случайный оргюнит с указанным типом айди
     */
    public static OrgUnit getRandomOrgUnitByTypeId(OrganizationUnitTypeId id) {
        JSONObject embedded = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, makePath(ORGANIZATION_UNIT_TYPES, id.getId(), ORGANIZATION_UNITS));
        OrgUnit unit = getListFromJsonObject(embedded, OrgUnit.class)
                .stream()
                .collect(randomItem());
        if (unit == null) {
            throw new AssertionError("Не найден оргюнит с типом " + id.getName());
        }
        Allure.addAttachment(ATTACHMENT_TITLE, "Был выбран случайный оргЮнит, тип: " + id.getName());
        return unit;
    }

    /**
     * Берет два случайных оргюнита с указанным типом айди
     */
    public static List<OrgUnit> getTwoRandomOrgId(OrganizationUnitTypeId id) {
        List<NameValuePair> pairs = Pairs.newBuilder().withChildren(false).page(0).size(10000).build();
        JSONObject someEmployeePositions = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, ORGANIZATION_UNITS, pairs);
        List<OrgUnit> orgUnitList = getListFromJsonObject(someEmployeePositions, OrgUnit.class).stream()
                .filter(orgUnit -> orgUnit.getOrganizationUnitTypeId() == id.getId())
                .collect(Collectors.toList());
        OrgUnit orgUnit1 = getRandomFromList(orgUnitList);
        orgUnitList.remove(orgUnit1);
        OrgUnit orgUnit2 = getRandomFromList(orgUnitList);
        Allure.addAttachment("Выбор оргЮнитов", "Были выбраны два случайных оргЮнита, тип: " + id.getName());
        List<OrgUnit> twoUnits = new ArrayList<>();
        twoUnits.add(orgUnit1);
        twoUnits.add(orgUnit2);
        return twoUnits;
    }

    /**
     * Берет всех потомков оргюнита
     */
    public static List<OrgUnit> getAllChildrenOrgUnits(OrgUnit orgUnit) {
        List<NameValuePair> pairs = Pairs.newBuilder().onlyActive(true).build();
        String path = makePath(ORGANIZATION_UNITS, orgUnit.getId(), CHILDREN);
        JSONObject children = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, path, pairs);
        return getListFromJsonObject(children, OrgUnit.class);
    }

    /**
     * Берет всех потомков оргюнита за указанную дату
     */
    public static List<OrgUnit> getAllChildrenOrgUnits(OrgUnit orgUnit, LocalDate date) {
        List<NameValuePair> pairs = Pairs.newBuilder().includeDate(date).build();
        String path = makePath(ORGANIZATION_UNITS, orgUnit.getId(), CHILDREN);
        JSONObject children = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, path, pairs);
        return getListFromJsonObject(children, OrgUnit.class);
    }

    /**
     * Возвращает оргюнит и FteOperationValues в зависимости от выбранного типа фильтра
     */
    public static ImmutablePair<OrgUnit, FteOperationValuesModel> getRandomOrgUnitWithFteGroup(FilterType filterType) {
        List<OrgUnit> allStore = getAllOrgUnits(true);
        List<String> eventTypeNames = new ArrayList<>(CommonRepository.getEventTypes().values());
        List<KpiList> kpiValues = KpiListRepository.getKpiTypes();
        Collections.shuffle(allStore);
        for (OrgUnit orgUnit : allStore) {
            if (CommonRepository.checkFteGroupStatus(orgUnit)) {
                FteOperationValuesModel groupFteLastYear = FteOperationValuesRepository.setEventNameAndKpi(FteOperationValuesRepository.checkGroupFteLastYear(orgUnit), eventTypeNames, kpiValues);
                switch (filterType) {
                    case EVENT_TYPE:
                        if (!groupFteLastYear.getEventNames().isEmpty()) {
                            return new ImmutablePair<>(orgUnit, groupFteLastYear);
                        }
                        break;
                    case KPI:
                        if (!groupFteLastYear.getKpiList().isEmpty()) {
                            return new ImmutablePair<>(orgUnit, groupFteLastYear);
                        }
                        break;
                    case EVENT_AND_KPI:
                        if (!groupFteLastYear.getEventNames().isEmpty() && !groupFteLastYear.getKpiList().isEmpty()) {
                            return new ImmutablePair<>(orgUnit, groupFteLastYear);
                        }
                        break;
                    default:
                        if (!groupFteLastYear.getEventNames().isEmpty() || !groupFteLastYear.getKpiList().isEmpty()) {
                            return new ImmutablePair<>(orgUnit, groupFteLastYear);
                        }
                        break;
                }
            }
        }
        Assert.fail("Не нашли оргюнит с данными");
        return new ImmutablePair<>(null, null);
    }

    /**
     * Возвращает случайный незакрытый оргюнит (с датой закрытия позже сегодняшнего дня)
     *
     * @return оргюнит
     */
    public static OrgUnit getRandomNotClosedOrgName() {
        List<OrgUnit> orgUnits = getAllNotClosedOrgUnits();
        return getRandomFromList(orgUnits);
    }

    /**
     * Просто случайный магазин
     */
    public static OrgUnit getRandomStore() {
        List<OrgUnit> orgUnits = getAllOrgUnits();
        OrgUnit orgUnit = getRandomFromList(orgUnits);
        Allure.addAttachment(ATTACHMENT_TITLE,
                             "Была выбрана случайная торговая точка с названием " + orgUnit.getName());
        return orgUnit;
    }

    /**
     * Случайный оргюнит с рабочими днями или с выходными
     */
    public static OrgUnit getRandomOrgUnitWithWorkOrWeekend(boolean workingDayNotWeekend) {
        List<OrgUnit> orgUnits = getAllOrgUnits();
        OrgUnit returnUnit = null;
        orgUnitsCycle:
        while (!orgUnits.isEmpty()) {
            OrgUnit unit = getRandomFromList(orgUnits);
            String urlEnding = makePath(ORGANIZATION_UNITS, unit.getId(), BUSINESS_HOURS);
            JSONObject empObject = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, urlEnding);
            List<BusinessHours> businessHoursList = getListFromJsonObject(empObject, BusinessHours.class);
            if (!businessHoursList.isEmpty()) {
                for (BusinessHours businessHours : businessHoursList) {
                    URI uri = URI.create(businessHours.getLink(DAYS));
                    JSONObject days = getJsonFromUri(Projects.WFM, uri);
                    int keysSize = days.keySet().size();
                    //если размер нулевой значит там все выходные
                    if (!workingDayNotWeekend && keysSize == 0) {
                        returnUnit = unit;
                        break orgUnitsCycle;
                    } else if (workingDayNotWeekend && keysSize == 0) {
                        continue;
                    } else if (workingDayNotWeekend) {
                        returnUnit = unit;
                        break orgUnitsCycle;
                    }
                    //если количество дней 7 то значит все дни в графике рабочие
                    JSONArray daysArray = days.getJSONObject(EMBEDDED).optJSONArray(DAYS);
                    int daysSize = daysArray.length();
                    if (daysSize < 7) {
                        returnUnit = unit;
                        break orgUnitsCycle;
                    }
                }
            }
            orgUnits.remove(unit);
        }
        String dayType;
        if (workingDayNotWeekend) {
            dayType = " рабочих дней ";
        } else {
            dayType = " выходных дней ";
        }
        if (returnUnit == null) {
            throw new AssertionError(ORG_UNIT_NOT_FOUND_ERROR_MESSAGE);
        }
        Allure.addAttachment(ATTACHMENT_TITLE,
                             "Был выбран оргЮнит с " + dayType + " в графике работы: " + returnUnit.getName());
        return returnUnit;
    }

    /**
     * Из всех типов оргюнитов выбирает либо самый нижний typeId (у него нет детей), либо второй снизу
     *
     * @param hasChildren false, если нужен оргюнит нижнего уровня
     * @return число, обозначающее тип желаемого оргюнита
     */

    public static int getLowestOrgTypes(boolean hasChildren) {
        List<Integer> allOrgUnitTypes = new ArrayList<>(getAllOrgUnitTypes().keySet());
        if (hasChildren) {
            return allOrgUnitTypes.get(allOrgUnitTypes.size() - 2);
        } else {
            return allOrgUnitTypes.get(allOrgUnitTypes.size() - 1);
        }
    }

    /**
     * Возвращает список оргюнитов указанного типа
     */
    public static List<OrgUnit> getOrgUnitsByTypeId(int id) {
        JSONObject someEmployeePositions = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, makePath(ORGANIZATION_UNIT_TYPES, id, ORG_UNITS));
        JSONObject orgUnits = someEmployeePositions.getJSONObject(EMBEDDED);
        return getListFromJsonObject(orgUnits, OrgUnit.class)
                .stream()
                .filter(orgUnit -> orgUnit.getOrganizationUnitTypeId() == id)
                .collect(Collectors.toList());
    }

    /**
     * Берет все орюниты и возвращает их объекты
     *
     * @return список оргюнитов
     */
    private static List<OrgUnit> getAllNotClosedOrgUnits() {
        JSONObject orgNameArray = getObjectsOrgUnits(CommonRepository.URL_BASE, true, false);
        List<OrgUnit> orgUnits = getListFromJsonObject(orgNameArray, OrgUnit.class);
        LOG.info(NUMBER_OF_ORG_UNITS, orgUnits.size());
        List<String> excludedOms = ExcludeOmList.getExcludeOMs();
        orgUnits = orgUnits.stream()
                .filter(OrgUnit::isAvailableForCalculation)
                .filter(om -> !excludedOms.contains(om.getName()))
                .collect(Collectors.toList());
        return orgUnits;
    }

    /**
     * Берет список всех торговых точек
     */
    private static List<OrgUnit> getAllOrgUnits() {
        JSONObject orgNameArray = getObjectsOrgUnits(CommonRepository.URL_BASE, true, true);
        List<OrgUnit> orgUnits = getListFromJsonObject(orgNameArray, OrgUnit.class);
        LOG.info(NUMBER_OF_ORG_UNITS, orgUnits.size());
        List<String> excludedOms = ExcludeOmList.getExcludeOMs();
        orgUnits = orgUnits.stream().filter(om -> !excludedOms.contains(om.getName())).collect(Collectors.toList());
        return orgUnits;
    }

    /**
     * Выбирает случайный оргЮнит с учетом вычета, тех которые не следует использовать
     *
     * @return - класс оргЮнит
     */
    public static OrgUnit getRandomOrgUnit() {
        List<OrgUnit> allStore = getAllOrgUnits();
        List<String> excludedOms = ExcludeOmList.getExcludeOMs();
        allStore = allStore.stream().filter(om -> !excludedOms.contains(om.getName())).collect(Collectors.toList());
        OrgUnit unit = getRandomFromList(allStore);
        Allure.addAttachment(ATTACHMENT_TITLE, SELECTED_STRING_PART + unit.getName()
                + ", не противоречащий набору оргюнитов для остальных тестов.");
        return unit;
    }

    /**
     * Выбирается оргюнит у которого в названии есть фрагмент строки
     *
     * @param nameToCompare - фрагмент строки который должен быть в названии оргюнита
     */
    public static OrgUnit getRandomOrgUnitByMatchName(String nameToCompare) {
        List<String> excludedOms = ExcludeOmList.getExcludeOMs();
        List<OrgUnit> withOutFilter = getAllOrgUnits(true);
        List<OrgUnit> orgUnits = withOutFilter.stream()
                .filter(om -> !excludedOms.contains(om.getName()))
                .filter(orgUnit -> orgUnit.getName().contains(nameToCompare)).collect(Collectors.toList());
        OrgUnit unit = !orgUnits.isEmpty() ? getRandomFromList(orgUnits)
                : getRandomFromList(withOutFilter);
        Allure.addAttachment(ATTACHMENT_TITLE, SELECTED_STRING_PART + unit.getName()
                + "Содержащий в себе " + "\"" + nameToCompare + "\"");
        return unit;
    }

    /**
     * Ищет подразделение с заданным OuterId
     */
    public static OrgUnit getOrgUnitByOuterId(String outerId) {
        List<String> excludedOms = ExcludeOmList.getExcludeOMs();
        List<OrgUnit> withOutFilter = getAllOrgUnits(false);
        List<OrgUnit> orgUnits = withOutFilter.stream()
                .filter(om -> !excludedOms.contains(om.getName()))
                .filter(orgUnit -> orgUnit.getOuterId().equals(outerId)).collect(Collectors.toList());
        OrgUnit unit = !orgUnits.isEmpty() ? getRandomFromList(orgUnits)
                : getRandomFromList(withOutFilter);
        Allure.addAttachment(ATTACHMENT_TITLE, SELECTED_STRING_PART + unit.getName()
                + "С outerId " + "\"" + outerId + "\"");
        return unit;
    }

    /**
     * Выбирает оргюнит, который не находится в списках исключения по заданным условиям
     *
     * @param options выбираем условие для оргюнита
     *                WITH_FREE_POSITIONS - с свободными должностями
     *                EMPLOYEES_WITH_POSITION - сотрудники с должностями
     *                EMPLOYEES_WITHOUT_POSITION - соттрудники без должностей
     */
    public static ImmutablePair<OrgUnit, EmployeeEssence> getRandomOmWithEmployeeByOptions(OrgUnitOptions options) {
        List<OrgUnit> allStore = getAllOrgUnits();
        List<String> excludedOms = ExcludeOmList.getExcludeOMs();
        allStore = allStore.stream().filter(om -> !excludedOms.contains(om.getName())).collect(Collectors.toList());
        while (!allStore.isEmpty()) {
            OrgUnit orgUnit = getRandomFromList(allStore);
            List<Position> positions = PositionRepository.getPositionsArray(orgUnit.getId());
            String allureAttachment = "У оргюнита %s было %d %s";
            String attachment;
            if (!positions.isEmpty()) {
                switch (options) {
                    case WITH_FREE_POSITIONS:
                        List<Position> free = PositionRepository.getFreePositions(positions);
                        if (free.isEmpty()) {
                            Position newPosition = PresetClass.createFreePosition(new DateInterval(LocalDate.now().plusDays(1)), orgUnit.getId());
                            free.add(newPosition);
                        }
                        attachment = String.format(allureAttachment, orgUnit.getName(), free.size(), "пустых должностей");
                        Allure.addAttachment(ATTACHMENT_TITLE, attachment);
                        LOG.info(attachment);
                        return new ImmutablePair<>(orgUnit, getRandomFromList(free));
                    case EMPLOYEES_WITH_POSITION:
                        List<EmployeePosition> employeePositions = EmployeePositionRepository.getEmployeePositions(positions);
                        if (!employeePositions.isEmpty()) {
                            attachment = String.format(allureAttachment, orgUnit.getName(), employeePositions.size(), "назначенных должностей");
                            Allure.addAttachment(ATTACHMENT_TITLE, attachment);
                            LOG.info(attachment);
                            return new ImmutablePair<>(orgUnit, getRandomFromList(employeePositions));
                        }
                        break;
                    case EMPLOYEES_WITHOUT_POSITION:
                        List<Employee> unattachedEmployees = EmployeeRepository.getUnattachedEmployee(orgUnit.getId());
                        if (!unattachedEmployees.isEmpty()) {
                            attachment = String.format(allureAttachment, orgUnit.getName(), unattachedEmployees.size(), "неназначенных сотрудников");
                            Allure.addAttachment(ATTACHMENT_TITLE, attachment);
                            LOG.info(attachment);
                            return new ImmutablePair<>(orgUnit, getRandomFromList(unattachedEmployees));
                        }
                        break;
                    case WITH_EMPLOYEES:
                        List<Employee> employees = EmployeeRepository.getEmployeesFromOM(orgUnit.getId());
                        if (!employees.isEmpty()) {
                            attachment = String.format(allureAttachment, orgUnit.getName(), employees.size(), "сотрудников");
                            Allure.addAttachment(ATTACHMENT_TITLE, attachment);
                            LOG.info(attachment);
                            return new ImmutablePair<>(orgUnit, getRandomFromList(employees));
                        }
                        break;
                }
            }
            allStore.remove(orgUnit);
        }
        LOG.info("Найти оргюнит не удалось, по условию выбора: {}", options);
        return ImmutablePair.nullPair();
    }

    /**
     * Выбирает случайный оргюнит, в котором поле availableForCalculation равно false (не включен в расчет).
     * Оргюнит при этом не закрытый (endDate == null)
     * Если такого оргюнита не существует, берет случайный, снимает с него галочку availableForCalculation и возвращает его
     *
     * @return оргюнит
     */
    public static OrgUnit getRndOrgUnitCalculatedFalse() {
        List<OrgUnit> orgUnitList = getAllOrgUnits();
        OrgUnit orgunit = getRandomFromList(orgUnitList);
        orgUnitList = orgUnitList.stream()
                .filter(orgUnit -> !orgUnit.isAvailableForCalculation() &&
                        orgUnit.getDateInterval().getEndDate() == null)
                .collect(Collectors.toList());
        if (orgUnitList.isEmpty()) {
            PresetClass.changeAvailabilityForCalculation(orgunit, false);
            orgunit = getOrgUnit(orgunit.getId());
            return orgunit;
        }
        return getRandomFromList(orgUnitList);
    }

    /**
     * Выбирает два случайных оргЮнита с учетом вычета, тех которые не следует использовать
     *
     * @return - лист оргЮнитов
     */
    public static List<OrgUnit> getTwoRandomOrgUnit() {
        List<OrgUnit> allStore = getAllOrgUnits(true);
        OrgUnit unit1 = getRandomFromList(allStore);
        allStore.remove(unit1);
        OrgUnit unit2 = getRandomFromList(allStore);
        List<OrgUnit> twoRandomOrgUnits = new ArrayList<>();
        twoRandomOrgUnits.add(unit1);
        twoRandomOrgUnits.add(unit2);
        Allure.addAttachment(ATTACHMENT_TITLE, "Были выбраны случайные оргюниты с именами: "
                + unit1.getName() + " и " + unit2.getName() + ", " +
                "не противоречащие набору оргюнитов для остальных тестов.");
        LOG.info("Выбраны ом: {} и {}", unit1.getName(), unit2.getName());
        return twoRandomOrgUnits;
    }

    /**
     * Возвращает два разных подразделения для манипуляций со сменами
     */
    public static ImmutablePair<OrgUnit, OrgUnit> getTwoOrgUnitsForShifts() {
        OrgUnit first = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int firstId = first.getId();
        OrgUnit second;
        do {
            second = OrgUnitRepository.getRandomOrgUnitsForShifts();
        }
        while (second.getId() == firstId);
        return new ImmutablePair<>(first, second);
    }

    /**
     * Метод возвращает список всех ОМ , отсортированных сначал по английски , а потом по русски
     *
     * @param uri - текущий URI для доступа в API
     * @return - список имен ОМ отсортированных по алфавиту
     */
    public static List<String> getOMByUriSortedByAlphabet(URI uri) {
        JSONObject temp = new JSONObject(setUrlAndInitiateForApi(uri, Projects.WFM));
        JSONArray emp = temp.getJSONObject(EMBEDDED).getJSONArray("orgUnits");
        List<String> units = new ArrayList<>();
        for (int i = 0; i <= emp.length() - 1; i++) {
            units.add(i, emp.getJSONObject(i).get(NAME).toString());
        }
        Collections.sort(units);
        return units;
    }

    /**
     * Метод берет список оргюнитов в виде массива JSON объектов
     *
     * @param url - текущий юрл стенда
     */
    public static JSONObject getObjectsOrgUnits(String url, boolean notClosed, boolean onlyStore) {
        Pairs.Builder pairs = Pairs.newBuilder().withChildren(false).page(0);
        if (url.contains("magnit")) {
            pairs.size(1000);
        } else {
            pairs.size(10000);
        }
        if (notClosed) {
            pairs.includeDate(LocalDate.now());
        }
        if (onlyStore) {
            int intTypeOm;
            if (URL_BASE.contains("efes")) {
                intTypeOm = OrganizationUnitTypeId.getOrgUnitTypeByLevel(OrganizationUnitTypeId.getLowest().getLevel() - 1).getId();
            } else if (URL_BASE.contains("magnit-master")) {
                intTypeOm = OmDbType.MAGNIT.getOrganizationUnitTypeId();
            } else {
                OmDbType type = CommonRepository.getTypeDb(url);
                switch (type) {
                    case SHELL:
                        intTypeOm = OmDbType.SHELL.getOrganizationUnitTypeId();
                        break;
                    case POCHTA:
                        intTypeOm = OmDbType.POCHTA.getOrganizationUnitTypeId();
                        break;
                    case INVENTIV:
                        intTypeOm = OmDbType.INVENTIV.getOrganizationUnitTypeId();
                        break;
                    case MAGNIT:
                        intTypeOm = OmDbType.MAGNIT.getOrganizationUnitTypeId();
                        break;
                    default:
                        intTypeOm = 5;
                        break;
                }
            }
            pairs.orgUnitTypeIds(intTypeOm);
        }
        return getJsonFromUri(Projects.WFM, url, ORGANIZATION_UNITS, pairs.build());
    }

    /**
     * Метод берет список оргюнитов и складывает их ID номера и название в мапу
     */
    public static List<OrgUnit> getOrgUnitsNotClosedAndAllType() {
        JSONObject someOm = getObjectsOrgUnits(CommonRepository.URL_BASE, false, false);
        return getListFromJsonObject(someOm, OrgUnit.class);
    }

    /**
     * Случайный оргюнит выбранного типа
     */
    public static OrgUnit getRandomOrgUnit(OrganizationUnitTypeId id) {
        List<OrgUnit> unitList = getOrgUnits(Pairs.newBuilder()
                                                     .includeDate(LocalDateTools.now())
                                                     .withChildren(false)
                                                     .page(0)
                                                     .size(10000).build());
        return unitList.stream()
                .filter(unit -> unit.getOrganizationUnitTypeId() == id.getId())
                .collect(randomItem());
    }

    /**
     * Определенное количество оргюнитов указанного типа
     *
     * @param id      - тип оргюнита
     * @param howMuch - сколько оргюнитов нужно
     */
    public static List<OrgUnit> getRandomOrgUnits(OrganizationUnitTypeId id, int howMuch) {
        List<OrgUnit> units = getOrgUnits(Pairs.newBuilder()
                                                  .includeDate(LocalDateTools.now())
                                                  .withChildren(false)
                                                  .page(0)
                                                  .size(10000).build()).stream()
                .filter(unit -> unit.getOrganizationUnitTypeId() == id.getId())
                .filter(OrgUnit::isAvailableForCalculation)
                .collect(Collectors.toList());
        List<OrgUnit> randomUnits = new ArrayList<>();
        for (int i = 0; i < howMuch; i++) {
            OrgUnit randomFromList = getRandomFromList(units);
            randomUnits.add(randomFromList);
            units.remove(randomFromList);
        }
        return randomUnits;
    }

    /**
     * Случацный оргюнит который не закрыт
     */
    public static OrgUnit getRandomActualOrgUnit(OrganizationUnitTypeId id) {
        List<OrgUnit> units = getOrgUnits(pairsActualOgrUnits());
        return getRandomOrgUnitFromListByTypeID(units, id);
    }

    /**
     * Берет случайную торговую точку которая попадает под набор тегов
     *
     * @param tags - список тегов
     * @return оргюнит
     */
    public static OrgUnit getRandomStoreUnitByTags(List<String> tags) {
        List<NameValuePair> tagPairs = Pairs.newBuilder().size(1000000).tagIds(Joiner.on(',').join(tags)).build();
        List<OrgUnit> units = getOrgUnits(tagPairs);
        return getRandomOrgUnitFromListByTypeID(units, OrganizationUnitTypeId.getLowest());
    }

    /**
     * Берет все подразделения с заданным тегом
     *
     * @param tag тег
     */
    public static List<OrgUnit> getAllOrgUnitsByTag(String tag) {
        List<NameValuePair> tagPairs = Pairs.newBuilder().size(1000000).tagIds(tag).build();
        return getOrgUnits(tagPairs);
    }

    /**
     * из списка оргюнитов выбирает оргюниты только с указанным типом айди
     */
    private static OrgUnit getRandomOrgUnitFromListByTypeID(List<OrgUnit> units, OrganizationUnitTypeId id) {
        OrgUnit unit = units.stream().filter(orgUnit -> orgUnit.getOrganizationUnitTypeId() == id.getId())
                .collect(randomItem());
        Allure.addAttachment(ATTACHMENT_TITLE, "Был выбран случайный оргЮнит, тип: " + id.getName());
        return unit;
    }

    /**
     * Параметры для взятия оргюнитов из апи
     */
    private static List<NameValuePair> pairsActualOgrUnits() {
        LocalDate now = LocalDate.now();
        return Pairs.newBuilder()
                .withChildren(false)
                .name("")
                .includeDate(now)
                .sort("name,asc")
                .page(0)
                .size(10000)
                .build();
    }

    /**
     * Берет оргюниты из апи по заданным параметрам
     */
    private static List<OrgUnit> getOrgUnits(List<NameValuePair> pairs) {
        JSONObject jsonObject = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, ORGANIZATION_UNITS, pairs);
        return getListFromJsonObject(jsonObject, OrgUnit.class);
    }

    public static List<OrgUnit> getAllOrgUnitsPublicationStatus(OrgUnit orgUnit, LocalDate date) {
        Pairs.Builder pairs = Pairs.newBuilder()
                .year(date.getYear())
                .month(date.getMonthValue())
                .size(1000000);
        if (orgUnit != null) {
            List<OrgUnit> children = getAllChildrenOrgUnits(orgUnit);
            final List<String> collect = children.stream().map(OrgUnit::getId).map(String::valueOf).collect(Collectors.toList());
            String stringChildren = String.join(", ", collect);
            pairs.orgUnitIdsSelf(orgUnit.getId())
                    .orgUnitIdsChildren(stringChildren);
        }
        JSONObject someObject = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, makePath(ROSTERS, PUBLICATION_STATUS), pairs.build());
        List<OrgUnit> listFromJsonObject = getListFromJsonObject(someObject, OrgUnit.class);
        LOG.info(NUMBER_OF_ORG_UNITS, listFromJsonObject.size());
        return listFromJsonObject;
    }

    /**
     * Случайный опубликованный оргюнит, к оторого есть публикация за месяц указанный в дате
     */
    public static OrgUnit getRandomOrgUnitPublicationStatus(OrgUnit orgUnit, LocalDate localDate) {
        List<OrgUnit> allOrgUnitsPublicationStatus = getAllOrgUnitsPublicationStatus(orgUnit, localDate);
        return getRandomFromList(allOrgUnitsPublicationStatus);
    }

    /**
     * Берет случайный оргюнит для сравнения графиков
     *
     * @param date - дата для сравнения
     */
    public static OrgUnit getRandomOrgUnitForComparisonGraph(LocalDate date) {
        return getAllOrgUnitsPublicationStatus(null, date)
                .stream()
                .filter(orgUnit1 -> orgUnit1.getPublishedRoster() != null)
                .filter(orgUnit1 -> orgUnit1.getActiveRoster() != null)
                .filter(orgUnit1 -> orgUnit1.getOnApprovalRoster() != null)
                .collect(randomItem());
    }

    /**
     * Возвращает оргюнит с ростером и его родительское подразделение с начальником
     */

    public static ImmutablePair<OrgUnit, OrgUnit> getOrgUnitsAndItsChildrenOrgUnits() {

        List<OrgUnit> orgUnits = getAllOrgUnits(true);
        OrgUnit orgUnitChildren = new OrgUnit(new JSONObject());
        OrgUnit orgUnitParent = new OrgUnit(new JSONObject());
        for (OrgUnit unit : orgUnits) {
            if (CommonRepository.checkKpiValuesNotBroken(unit.getId())
                    && PresetClass.checkOrgUnitBusinessHours(ScheduleType.ANY_TYPE, unit)) {
                orgUnitParent = unit.getParentOrgUnit();
                int omTd = orgUnitParent.getId();
                Position chiefPosition = PositionRepository.getChief(omTd);
                if (!EmployeePositionRepository.getEmployeePositions(omTd).isEmpty()) {
                    if (chiefPosition == null || chiefPosition.getEmployee() == null) {
                        PresetClass.addChief(EmployeePositionRepository.getRandomEmployeePosition(orgUnitParent).getId());
                    }
                    orgUnitChildren = unit;
                    break;
                }
            }
        }
        return new ImmutablePair<>(orgUnitParent, orgUnitChildren);
    }

    /**
     * Список оргюнитов который взяты по параметрам сортера
     */
    public static List<OrgUnit> getOrgUnitsBySorter(Pairs.Builder otherSorters) {
        JSONObject object = getJsonFromUri(Projects.WFM, Links.getTestProperty("release"), ORGANIZATION_UNITS, otherSorters.build());
        return getListFromJsonObject(object, OrgUnit.class);
    }

    /**
     * Получить значения "Принадлежности подразделения"
     */
    public static JSONObject getValueInEntityPropertiesInOrgUnit(int id) {
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(ORG_UNITS, id, ENTITY_PROPERTIES, ORG_UNIT_FORMAT));
        return getJsonFromUri(Projects.WFM, uri);
    }

    /**
     * Получить подразделение по тегу со сменами
     */
    public static OrgUnit getOrgUnitByTag(String tagName) {
        Map<String, String> tags = CommonRepository.getTags();

        List<OrgUnit> matchingOrgUnits = tags.entrySet()
                .stream()
                .filter(entry -> entry.getKey().contains(tagName))
                .map(entry -> new ImmutablePair<>(entry.getKey(), entry.getValue()))
                .map(tagValue -> getAllOrgUnitsByTag(tagValue.getRight()))
                .flatMap(List::stream)
                .collect(Collectors.toList());

        for (OrgUnit orgUnit : matchingOrgUnits) {
            try {
                int activeRosterId = RosterRepository.getActiveRosterThisMonth(orgUnit.getId()).getId();
                boolean hasShiftsInActiveRoster = !ShiftRepository.getShiftsForRoster(activeRosterId, new DateInterval()).isEmpty();
                if (hasShiftsInActiveRoster) {
                    return orgUnit;
                }
            } catch (AssertionError e) {
                continue;
            }
        }
        throw new AssertionError(NO_TEST_DATA + "Не удалось найти подразделение со сменами по тегу: " + tagName);
    }

    /**
     * Получить случайный оргюнит из стратегического планирования
     */
    public static OrgUnit getRandomChildOrgUnitsInStrategicPlanning() {
        return getRandomFromList(getAllChildOrgUnitsInStrategicPlanning());
    }

    /**
     * Получить все дочерние оргюниты из стратегического планирования
     */
    public static List<OrgUnit> getAllChildOrgUnitsInStrategicPlanning() {
        JSONArray jsonArray = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, makePath(MAIN_ORGANIZATION_UNIT))
                .getJSONObject(EMBEDDED).getJSONArray(REL_ORG_UNITS);
        List<OrgUnit> mainOrgUnits = getListFromJsonArray(jsonArray, OrgUnit.class);
        List<OrgUnit> childOrgUnits = new ArrayList<>();
        for (OrgUnit orgUnit : mainOrgUnits) {
            childOrgUnits.addAll(
                    getAllLastChildrenOrgUnits(orgUnit, new ArrayList<>())
            );
        }
        return childOrgUnits;
    }

    /**
     * Рекурсивно получить все дочерние оргюниты, находящиеся в самом нижнем уровне иерархии
     */
    private static List<OrgUnit> getAllLastChildrenOrgUnits(OrgUnit orgUnit, List<OrgUnit> allLastChildren) {
        List<OrgUnit> children = getAllChildrenOrgUnits(orgUnit, LocalDate.now());
        if (children.isEmpty()) {
            allLastChildren.add(orgUnit);
        } else {
            for (OrgUnit child : children) {
                getAllLastChildrenOrgUnits(child, allLastChildren);
            }
        }
        return allLastChildren;
    }

    /**
     * Возвращает список оргюнитов, в которых сотрудник назначен на должность в заданный период времени
     *
     * @param employee сотрудник, для которого ищем орг юниты
     * @param interval интервал дат
     * @return список орг юнитов
     */
    public static List<OrgUnit> getOrgUnitList(Employee employee, DateInterval interval) {
        boolean isMagnit = URL_BASE.contains("magnit");
        return EmployeePositionRepository.getEmployeePositionsFromEmployee(employee, interval)
                .stream()
                .filter(employeePosition -> {
                    if (!isMagnit) {
                        return true;
                    } else {
                        return !employeePosition.isTemporary();
                    }
                })
                .map(EmployeePosition::getOrgUnit)
                .distinct()
                .collect(Collectors.toList());
    }

    private static boolean hasShiftsInActiveRoster(int omId) {
        try {
            int activeRosterId = RosterRepository.getActiveRosterThisMonth(omId).getId();
            return !ShiftRepository.getShiftsForRoster(activeRosterId, new DateInterval()).isEmpty();
        } catch (AssertionError e) {
            return false;
        }
    }

    public static ImmutablePair<OrgUnit, OrgUnit> getTwoOrgUnitsWithSameParent() {
        OrgUnit firstChild = getRandomOrgUnitsForShifts();
        OrgUnit parent = firstChild.getParentOrgUnit();
        List<OrgUnit> units = getAllChildrenOrgUnits(parent);
        for (OrgUnit unit : units) {
            boolean hasFutureRosters = !RosterRepository.getRosters(unit.getId(), new DateInterval(LocalDate.now(), LocalDateTools.getLastDate()), true).isEmpty();
            if (hasFutureRosters && !unit.equals(firstChild)) {
                return new ImmutablePair<>(firstChild, unit);
            }
        }
        throw new AssertionError("Не нашлось двух подходящих оргюнитов с одним родителем");
    }

    /**
     * @param checkFtePositionGroupId - в мастере планирования не работает кнопка "Сформировать отчёт" если ftePositionGroupId = null
     */
    public static ImmutablePair<OrgUnit, EmployeePosition> getRandomOrgUnitWithRandomEmployeeForShiftPair(boolean checkFtePositionGroupId) {
        List<OrgUnit> allStore = getAllAvailableOrgUnits(true);
        Collections.shuffle(allStore);
        if (allStore.isEmpty()) throw new AssertionError("Нет доступных подразделений");

        for (OrgUnit orgUnit : allStore) {
            int id = orgUnit.getId();
            if (!hasShiftsInActiveRoster(id)) continue;

            ImmutablePair<OrgUnit, EmployeePosition> unitAndEmp = findPairByFtePositionGroupId(orgUnit, checkFtePositionGroupId);
            if (unitAndEmp != null) return unitAndEmp;
        }
        throw new AssertionError(NO_TEST_DATA + "Не удалось найти подразделение, у которого есть " +
                "сотрудники и ростер со сменами");
    }

    /**
     * Возвращает подразделение с двумя активными сотрудниками
     */
    public static ImmutableTriple<OrgUnit, EmployeePosition, EmployeePosition> getRandomOrgUnitWithRandomEmployeeForShiftTriple() {
        List<OrgUnit> allStore = getAllAvailableOrgUnits(true);
        int minSizeList = 2;
        Collections.shuffle(allStore);
        if (allStore.isEmpty()) throw new AssertionError("Нет доступных подразделений");

        for (OrgUnit orgUnit : allStore) {
            int id = orgUnit.getId();
            if (!hasShiftsInActiveRoster(id)) continue;

            List<EmployeePosition> employees = EmployeePositionRepository.getAllEmployeesWithCheckByApi(orgUnit.getId(), null, true);
            if (employees.size() < minSizeList) continue;
            List<EmployeePosition> employeesRandomList = getRandomFromList(employees, minSizeList);
            return new ImmutableTriple<>(orgUnit, employeesRandomList.get(0), employeesRandomList.get(1));
        }
        throw new AssertionError(NO_TEST_DATA + "Не удалось найти подразделение, у которого есть " +
                "сотрудники и ростер со сменами");
    }

    public static Map<OrgUnit, EmployeePosition> getOrgUnitEmployeePairFromEachUnitPair(boolean checkFtePositionGroupId) {
        Map<OrgUnit, EmployeePosition> orgUnitEmployeePairMap = new HashMap<>();
        List<OrgUnit> allStore = getAllAvailableOrgUnits(true);
        Collections.shuffle(allStore);
        if (allStore.isEmpty()) {
            throw new AssertionError("Нет доступных подразделений");
        }

        for (OrgUnit orgUnit : allStore) {
            if (!hasShiftsInActiveRoster(orgUnit.getId())) continue;

            ImmutablePair<OrgUnit, EmployeePosition> unitAndEmp = findPairByFtePositionGroupId(orgUnit, checkFtePositionGroupId);
            if (unitAndEmp != null) {
                orgUnitEmployeePairMap.put(unitAndEmp.getLeft(), unitAndEmp.getRight());
                if (orgUnitEmployeePairMap.size() == 2) return orgUnitEmployeePairMap;
            }
        }
        throw new AssertionError(NO_TEST_DATA + "Не удалось найти два подразделения, у которых есть сотрудники и ростер со сменами");
    }

    private static ImmutablePair<OrgUnit, EmployeePosition> findPairByFtePositionGroupId(OrgUnit orgUnit, boolean checkFtePositionGroupId) {
        List<EmployeePosition> employees = EmployeePositionRepository.getAllEmployeesWithCheckByApi(orgUnit.getId(), null, true);

        for (EmployeePosition employeePosition : employees) {
            if (!checkFtePositionGroupId || hasCheckFtePositionGroupId(checkFtePositionGroupId, employeePosition.getPosition())) {
                LOG.info("Выбранное подразделение: {} с ID: {}", orgUnit.getName(), orgUnit.getId());
                LOG.info("Выбранный сотрудник: {} на позиции: {}", employeePosition.getEmployee().getFullName(), employeePosition.getPosition().getName());
                return new ImmutablePair<>(orgUnit, employeePosition);
            }
        }
        return null;
    }

    public static OrgUnit getRandomOrgUnitWithHistoricalData(List<KpiList> kpiList, LocalDate from, LocalDate to, int months) {
        KpiRepository kpiRepository = new KpiRepository(DateUnit.MONTH);
        List<OrgUnit> orgUnits = getAllNotClosedOrgUnits();
        Collections.shuffle(orgUnits);
        for (OrgUnit orgUnit : orgUnits) {
            kpiRepository.setOrgUnit(orgUnit);
            int i = 0;
            while (i < kpiList.size()) {
                List<Kpi> kpis = kpiRepository.getValuesForKpi(from, to, true, kpiList.get(i).getKpiId()).stream()
                        .filter(kpi1 -> kpi1.getValue() != 0.0).collect(Collectors.toList());
                if (kpis.size() < months) {
                    break;
                }
                i++;
            }
            if (i == kpiList.size()) {
                return orgUnit;
            }
        }
        throw new AssertionError("OrgUnit with historical data for kpi is not found");
    }

    /**
     * Возвращает список потомков в дереве для оргюнита с указанным id
     */
    public static List<OrgUnit> getOrgUnitsInTree(int id) {
        JSONObject tree = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, makePath("report-org-unit-tree", id, "tree"));
        JSONArray orgUnits = tree.getJSONObject(EMBEDDED).getJSONArray(REL_ORG_UNITS);
        return getListFromJsonArray(orgUnits, OrgUnit.class);
    }

    /**
     * Возвращает главный орг юнит
     */
    public static OrgUnit getMainOrgUnit() {
        JSONObject unit = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE,
                                         makePath(ORG_UNITS, getOrgUnitsByTypeId(OrganizationUnitTypeId.getHighest().getId()).get(0).getId()));
        return new OrgUnit(unit);
    }

    /**
     * Получает случайное подразделение и сотрудника, для которых ростер не опубликован или отсутствует вообще
     */
    public static ImmutablePair<OrgUnit, EmployeePosition> getRandomOrgUnitWithRandomEmployeeForNotPublishRoster() {
        List<OrgUnit> orgUnits = getAllAvailableOrgUnits(true);
        Collections.shuffle(orgUnits);
        for (OrgUnit orgUnit : orgUnits) {
            List<Roster> rosters = RosterRepository.getRosters(orgUnit.getId());
            if (rosters.stream().anyMatch(Roster::isPublished)) {
                continue;
            }
            List<EmployeePosition> employees = EmployeePositionRepository.getAllEmployeesWithCheckByApi(orgUnit.getId(), null, true);
            if (!employees.isEmpty()) {
                return new ImmutablePair<>(orgUnit, getRandomFromList(employees));
            }
        }
        throw new AssertionError("Не удалось найти подразделение и сотрудника, удовлетворяющие заданным условиям");
    }

    /**
     * Получает случайное подразделение с активным ростером и количеством сотрудников не более 5
     */
    public static OrgUnit getOrgUnitForShiftsCalculation() {
        List<OrgUnit> orgUnits = getAllAvailableOrgUnits(true);
        Collections.shuffle(orgUnits);
        for (OrgUnit orgUnit : orgUnits) {
            try {
                int orgUnitId = orgUnit.getId();
                RosterRepository.getActiveRosterThisMonth(orgUnitId);
                int amountOfEmployeePositions = EmployeePositionRepository.getEmployeePositionsWithoutFiredEmployees(orgUnitId, LocalDate.now(), true).size();
                if (amountOfEmployeePositions <= 5 && amountOfEmployeePositions > 0) {
                    return orgUnit;
                }
            } catch (AssertionError ignored) {
            }
        }
        throw new AssertionError("Не удалось найти подразделение с активным ростером и количеством сотрудников не более 5");
    }
}

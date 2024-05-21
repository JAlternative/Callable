package pagemodel;

import com.google.common.base.Joiner;
import com.google.inject.Inject;
import common.DataProviders;
import guice.TestModule;
import io.qameta.allure.*;
import io.qameta.allure.testng.Tag;
import io.qameta.atlas.core.Atlas;
import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.WebDriverConfiguration;
import io.qameta.atlas.webdriver.exception.WaitUntilException;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.hamcrest.Matchers;
import org.json.JSONArray;
import org.json.JSONObject;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.Reporter;
import org.testng.annotations.*;
import org.testng.asserts.SoftAssert;
import org.testng.internal.collections.Ints;
import pages.MessagesPage;
import pages.ScheduleBoardPage;
import reporting.TestListener;
import ru.yandex.qatools.matchers.webdriver.DisplayedMatcher;
import ru.yandex.qatools.matchers.webdriver.TextMatcher;
import testutils.*;
import utils.Links;
import utils.Projects;
import utils.TimeType;
import utils.db.DBUtils;
import utils.downloading.FileDownloadCheckerForScheduleBoard;
import utils.downloading.TypeOfAcceptContent;
import utils.downloading.TypeOfFiles;
import utils.downloading.TypeOfReports;
import utils.tools.*;
import wfm.ApiRequest;
import wfm.PresetClass;
import wfm.components.orgstructure.MathParameters;
import wfm.components.orgstructure.*;
import wfm.components.schedule.PositionTypes;
import wfm.components.schedule.*;
import wfm.components.systemlists.IntervalType;
import wfm.components.systemlists.LimitType;
import wfm.components.systemlists.TableRuleShiftType;
import wfm.components.systemlists.TableRuleStrategy;
import wfm.components.utils.*;
import wfm.models.*;
import wfm.repository.*;

import java.net.URI;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAdjusters;
import java.util.NoSuchElementException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static apitest.HelperMethods.assertNotChanged;
import static common.ErrorMessagesForRegExp.*;
import static common.Groups.*;
import static org.testng.Assert.*;
import static utils.ErrorMessagesForReport.NO_TEST_DATA;
import static utils.ErrorMessagesForReport.NO_VALID_DATE;
import static utils.Links.*;
import static utils.Params.COMMENT;
import static utils.Params.POSITIONS;
import static utils.Params.*;
import static utils.authorization.CookieRW.getCookieWithCheck;
import static utils.downloading.FileDownloadChecker.getFileNameExtensionFromResponse;
import static utils.tools.CustomTools.*;
import static utils.tools.Format.API;
import static utils.tools.Format.*;
import static utils.tools.RequestFormers.*;
import static wfm.PresetClass.defaultShiftPreset;
import static wfm.PresetClass.getFreeDateForEmployeeShiftPreset;
import static wfm.repository.CommonRepository.*;
import static wfm.repository.JobTitleRepository.randomJobTitle;

@Guice(modules = {TestModule.class})
@Listeners({TestListener.class})
public class ScheduleBoard extends BaseTest {

    private static final String RELEASE_URL = Links.getTestProperty("release");
    private static final Section SECTION = Section.SCHEDULE_BOARD;
    private static final String URL_SB = RELEASE_URL + SECTION.getUrlEnding();
    private static final Logger LOG = LoggerFactory.getLogger(ScheduleBoard.class);
    private static final String PROPERTY_MAP = "property";
    private static final String UI_VALUES_MAP = "strings";
    private static final String SETTING_VALUE_MAP = "settingValue";

    @Inject
    private ScheduleBoardPage sb;

    @DataProvider(name = "CommentProperty")
    private static Object[][] commentProperty() {
        Object[][] array = new Object[3][];
        array[0] = new Object[]{false, false};
        array[1] = new Object[]{false, true};
        array[2] = new Object[]{true, false};
        return array;
    }

    @DataProvider(name = "employeeHours")
    private static Object[][] employeeHours(ITestContext c) {
        changeProperty(SystemProperties.SHIFT_CHECK_IN_WORKED_ROSTER_BEFORE_CREATING, false, c);
        Map<SystemProperties, List<InEmployeeWorkingHours>> initialPairs = new HashMap<>();
        initialPairs.put(SystemProperties.SCHEDULE_BOARD_SHOW_SHIFT_HOURS, Collections.singletonList(InEmployeeWorkingHours.PROCESSING_FLAWS_YEAR));
        initialPairs.put(SystemProperties.SCHEDULE_BOARD_DEVIATION_FROM_STANDARD_PLAN, Collections.singletonList(InEmployeeWorkingHours.HOURS_AMOUNT_PLAN));
        initialPairs.put(SystemProperties.SCHEDULE_BOARD_CRESCENT_STANDARD, Arrays.asList(InEmployeeWorkingHours.HOURS_AMOUNT_FIRST_HALF, InEmployeeWorkingHours.HOURS_AMOUNT_SECOND_HALF));
        initialPairs.put(SystemProperties.SCHEDULE_BOARD_SHOW_DAYS_OFF_COUNT, Collections.singletonList(InEmployeeWorkingHours.DAY_OFF_AMOUNT));
        initialPairs.put(SystemProperties.SCHEDULE_BOARD_SHOW_OUTER_ID, Collections.singletonList(InEmployeeWorkingHours.OUTER_ID));
        initialPairs.put(SystemProperties.SCHEDULE_BOARD_NIGHT_HOURS_INDICATOR, Arrays.asList(InEmployeeWorkingHours.NIGHT_HOURS_AMOUNT, InEmployeeWorkingHours.NIGHT_HOURS_AMOUNT_FACT));
        initialPairs.put(SystemProperties.SCHEDULE_BOARD_SHOW_RATE, Collections.singletonList(InEmployeeWorkingHours.RATE));
        List<Map<String, Object>> result = new ArrayList<>();
        Set<Map.Entry<SystemProperties, List<InEmployeeWorkingHours>>> entrySet = initialPairs.entrySet();
        for (Map.Entry entry : entrySet) {
            Map<String, Object> trueMap = new HashMap<>();
            trueMap.put(PROPERTY_MAP, entry.getKey());
            trueMap.put(UI_VALUES_MAP, entry.getValue());
            trueMap.put(SETTING_VALUE_MAP, true);
            result.add(trueMap);
            Map<String, Object> falseMap = new HashMap<>();
            falseMap.put(PROPERTY_MAP, entry.getKey());
            falseMap.put(UI_VALUES_MAP, entry.getValue());
            falseMap.put(SETTING_VALUE_MAP, false);
            result.add(falseMap);
        }
        int size = result.size();
        Object[][] array = new Object[size][];
        for (int i = 0; i < size; i++) {
            Map<String, Object> item = result.get(i);
            array[i] = new Object[]{item.get(PROPERTY_MAP), item.get(UI_VALUES_MAP), item.get(SETTING_VALUE_MAP)};
        }
        return array;
    }

    @DataProvider(name = "eventTypes")
    private static Object[][] eventTypeProvider() {
        List<EventType> eventTypes = EventTypeRepository.getAllEventTypes();
        Object[][] array = new Object[eventTypes.size()][];
        for (int i = 0; i < eventTypes.size(); i++) {
            array[i] = new Object[]{eventTypes.get(i)};
        }
        return array;
    }

    @DataProvider(name = "locales")
    private static Object[][] locales() {
        Object[][] array = new Object[1][];
        array[0] = new Object[]{AppDefaultLocale.RU.getLocale()};
        if (URL_BASE.contains("pochta") || URL_BASE.contains("94.139.247.111")) {
            array = Arrays.copyOf(array, array.length + 1);
            array[1] = new Object[]{AppDefaultLocale.RU_RUSSIANPOST.getLocale()};
        }
        return array;
    }

    @DataProvider(name = "Non-appearance types")
    private Object[][] enableDateNonAppearance() {
        Object[][] array = new Object[2][];
        array[1] = new Object[]{IntervalType.DATE};
        array[0] = new Object[]{IntervalType.DATETIME};
        return array;
    }

    @DataProvider(name = "requestStatus")
    private static Object[][] requestStatus() {
        Object[][] array = new Object[2][];
        array[0] = new Object[]{ScheduleRequestStatus.APPROVED};
        array[1] = new Object[]{ScheduleRequestStatus.NOT_APPROVED};
        return array;
    }

    @DataProvider(name = "employees for free shifts in same orgUnit")
    private Object[][] employeesForFreeShiftsInSameOrgUnit() {
        Object[][] array = new Object[3][];
        array[0] = new Object[]{false, false, true};
        array[1] = new Object[]{true, false, false};
        array[2] = new Object[]{false, true, false};
        return array;
    }

    @DataProvider(name = "employees for free shifts in different orgUnit")
    private Object[][] employeesForFreeShiftsInDifferentOrgUnit() {
        Object[][] array = new Object[2][];
        array[0] = new Object[]{false, true, true};
        array[1] = new Object[]{true, false, false};
        return array;
    }

    @DataProvider(name = "Locked Shifts from Exchange")
    private Object[][] lockedShiftFromExchange() {
        Object[][] array = new Object[4][];
        array[0] = new Object[]{0, 1};
        array[3] = new Object[]{0, -1};
        array[2] = new Object[]{-1, 1};
        array[1] = new Object[]{null, 1};
        return array;
    }

    @DataProvider(name = "Unlocked Shifts from Exchange")
    private Object[][] unlockedShiftFromExchange() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitWithWorkedRosters(true);
        int omId = unit.getId();
        int rosterId = RosterRepository.getWorkedRosterThisMonth(omId).getId();
        List<Shift> shifts = ShiftRepository.getShiftsForRoster(rosterId, new DateInterval(LocalDateTools.getFirstDate().plusDays(1),
                                                                                           LocalDate.now().minusDays(1)));
        Shift shift;
        EmployeePosition ep;
        if (shifts.isEmpty()) {
            ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(unit.getId(), null, true);
            shift = PresetClass.defaultShiftPreset(ep, ShiftTimePosition.PAST);
        } else {
            shift = getRandomFromList(shifts);
            ep = EmployeePositionRepository.getEmployeePositionById(shift.getEmployeePositionId());
        }
        DBUtils.makeShiftFromExchange(shift);
        LocalDate date = shift.getDateTimeInterval().getStartDate();
        int difference = LocalDate.now().compareTo(date);
        int day = date.getDayOfMonth();
        Object[][] array = new Object[2][];
        array[0] = new Object[]{ep, date, shift, difference + 1, Collections.singletonList(day - 1)};
        array[1] = new Object[]{ep, date, shift, -1, Collections.singletonList(day - 1)};
        return array;
    }

    @DataProvider(name = "Employee Types")
    private Object[][] employeeTypes() {
        List<EmployeeType> types = new ArrayList<>(Arrays.asList(EmployeeType.INTERNAL_PART_TIMER, EmployeeType.OWN_PERSONNEL));
        boolean addOutStaff = PositionGroupRepository.getAllPositionGroups().stream().anyMatch(e -> e.getName().equals("Аутстафф"))
                && JobTitleRepository.getAllJobTitles().stream().anyMatch(e -> e.getFullName().contains("АУТ"));
        if (addOutStaff) {
            types.add(EmployeeType.OUT_STAFF);
        }
        Object[][] array = new Object[types.size()][];
        Predicate<EmployeePosition> pertTimerPredicate = e -> e.isTemporary() && !e.getPosition().getName().contains("АУТ");
        array[0] = new Object[]{EmployeeType.INTERNAL_PART_TIMER, pertTimerPredicate};
        Predicate<EmployeePosition> ownPersonnelPredicate = e -> !e.isTemporary() && !e.getPosition().getName().contains("АУТ");
        array[1] = new Object[]{EmployeeType.OWN_PERSONNEL, ownPersonnelPredicate};
        if (addOutStaff) {
            Predicate<EmployeePosition> outStaffPredicate = e -> e.getPosition().getName().contains("АУТ");
            array[2] = new Object[]{EmployeeType.OUT_STAFF, outStaffPredicate};
        }
        return array;
    }

    @DataProvider(name = "Employee attributes")
    private Object[][] employeeAttributes() {
        Object[][] array = new Object[2][];
        array[0] = new Object[]{EmployeeAttributes.DISABILITY, "GROUP_1"};
        array[1] = new Object[]{EmployeeAttributes.CHILD_CARE_VACATION, "true"};
        return array;
    }

    @DataProvider(name = "System property for norms display")
    private Object[][] displayNormsForTemporaryEmployees(ITestContext c) {
        Object[][] array = new Object[3][];
        array[0] = new Object[]{"ALL", "ABCHR5817-2"};
        array[1] = new Object[]{"TEMPORARY", "ABCHR5817-1"};
        array[2] = new Object[]{"", "ABCHR5817-3"};
        return array;
    }

    @DataProvider(name = "0, 1, 2")
    private Object[][] zeroToTwo() {
        Object[][] array = new Object[3][];
        array[0] = new Object[]{0};
        array[1] = new Object[]{1};
        array[2] = new Object[]{2};
        return array;
    }

    @DataProvider(name = "Additional work time outside of shift")
    private Object[][] additionalWorkTimeOutsideOfShift(ITestContext c) {
        changeProperty(SystemProperties.SHIFT_CHECK_IN_WORKED_ROSTER_BEFORE_CREATING, false, c);
        Object[][] array = new Object[8][];

        array[0] = new Object[]{-60, -60, false, 0};
        array[1] = new Object[]{60, 60, false, 1};
        array[2] = new Object[]{-60, 60, false, 0};
        array[3] = new Object[]{120, -420, false, 2};
        array[4] = new Object[]{120, -360, false, 2};
        array[5] = new Object[]{1, -1, true, -1};
        array[6] = new Object[]{0, 0, true, -1};
        array[7] = new Object[]{1, -478, true, -1};
        return array;
    }

    @DataProvider(name = "Scope types")
    private Object[][] scopeTypes() {
        Object[][] array = new Object[3][];
        array[0] = new Object[]{ScopeType.MONTH};
        array[1] = new Object[]{ScopeType.WEEK};
        array[2] = new Object[]{ScopeType.DAY};
        return array;
    }

    @DataProvider(name = "Data for limit tests")
    private Object[][] limitTypes() {
        Object[][] array = new Object[6][];
        String disableNotification = "Отключение вывода уведомления о создании Лимита с типом \"%s\" по сотрудникам Аутстафф";
        String disableLimitCheck = "Отключение проверки на Лимит с типом \"%s\" по сотрудникам Аутстафф при публикации расписания";
        array[0] = new Object[]{"ABCHR7123-1", disableNotification,
                LimitType.GENERAL, GraphStatus.PUBLISH};
        array[1] = new Object[]{"ABCHR7123-2", disableNotification,
                LimitType.ADD_WORK, GraphStatus.PUBLISH};
        array[2] = new Object[]{"ABCHR7123-3", disableNotification,
                LimitType.POSITION, GraphStatus.PUBLISH};
        array[3] = new Object[]{"ABCHR7123-4", disableLimitCheck,
                LimitType.GENERAL, GraphStatus.NOT_PUBLISH};
        array[4] = new Object[]{"ABCHR7123-5", disableLimitCheck,
                LimitType.ADD_WORK, GraphStatus.NOT_PUBLISH};
        array[5] = new Object[]{"ABCHR7123-6", disableLimitCheck,
                LimitType.POSITION, GraphStatus.NOT_PUBLISH};
        return array;
    }

    @DataProvider(name = "add work status")
    private static Object[][] addWorkStatus() {
        Object[][] array = new Object[3][];
        array[0] = new Object[]{AddWorkStatus.PLANNED.getStatusName()};
        array[1] = new Object[]{AddWorkStatus.DONE.getStatusName()};
        array[2] = new Object[]{AddWorkStatus.CANCELLED.getStatusName()};
        return array;
    }

    @DataProvider(name = "Shift comments system properties")
    private Object[][] shiftCommentsSystemProperties() {
        Object[][] array = new Object[4][];
        array[0] = new Object[]{true, true};
        array[1] = new Object[]{false, false};
        array[2] = new Object[]{false, true};
        array[3] = new Object[]{true, false};
        return array;
    }

    /**
     * Выбирает случайный день для блокировки табеля, чтобы часть табеля была открыта, а часть - нет
     */
    private LocalDate selectRandomTableLockDate() {
        List<LocalDate> dates = ShiftTimePosition.PAST.getShiftsDateInterval().getBetweenDatesList();
        dates.remove(LocalDateTools.getFirstDate());
        dates.remove(LocalDate.now());
        if (dates.isEmpty()) {
            throw new AssertionError(NO_VALID_DATE + "В табеле должно быть как минимум три дня");
        }
        LocalDate lockDate = getRandomFromList(dates);
        int deepEdit = LocalDate.now().compareTo(lockDate);
        LOG.info("{} последних дней табеля будет доступно для редактирования", deepEdit);
        Allure.addAttachment("Блокировка табеля по правилу", "Количество дней табеля, доступных для редактирования - " + deepEdit);
        return lockDate;
    }

    @AfterTest(alwaysRun = true, description = "Закрытие драйвера")
    private void closeDriver() {
        closeDriver(sb.getWrappedDriver());
    }

    @AfterMethod(alwaysRun = true, description = "Очистить куки и подготовить новую вкладку")
    private void cleanCookies() {
        cleanCookiesAndOpenNewTab(sb.getWrappedDriver());
    }

    @AfterMethod(onlyForGroups = {"@After remove test shift hiring reasons"})
    private void removeHiringReasons() {
        ShiftHiringReasonRepository.getShiftHiringReasons()
                .stream()
                .filter(r -> r.getTitle().startsWith("testReason_"))
                .forEach(PresetClass::deleteRequest);
    }

    @AfterMethod(alwaysRun = true, onlyForGroups = {"@After remove calculation hint"})
    private void deleteCalculationHint() {
        CommonRepository.getFileManuals()
                .stream()
                .filter(m -> m.getType().equals("CALCULATION_HINT"))
                .findFirst()
                .ifPresent(PresetClass::deleteRequest);
    }

    @AfterMethod(alwaysRun = true, onlyForGroups = {"@After remove an employee from the current schedule"})
    public void removeAddedOrgUnitType(ITestContext c) {
        List<String> attributes = c.getAttributeNames().stream()
                .filter(a -> a.startsWith("employeePosition")).collect(Collectors.toList());
        if (!attributes.isEmpty()) {
            EmployeePosition emp = (EmployeePosition) c.getAttribute("employeePosition");
            LocalDate date = emp.getPosition().getDateInterval().getStartDate();
            LocalDate dateOfTwoYearsAgo = LocalDate.now()
                    .minusYears(2).withDayOfMonth(date.getDayOfMonth()).withMonth(date.getMonthValue());
            PresetClass.updateEmployeeStartDateInSchedule(emp.getPosition(), emp.getEmployee(), emp.getOrgUnit(),
                                                          date, dateOfTwoYearsAgo, dateOfTwoYearsAgo);
        }
    }

    @AfterMethod(alwaysRun = true, onlyForGroups = {"@After remove requestType"})
    public void removeScheduleRequestType(ITestContext c) {
        List<String> attributes = c.getAttributeNames().stream()
                .filter(a -> a.startsWith("scheduleRequestAlias")).collect(Collectors.toList());
        if (!attributes.isEmpty()) {
            ScheduleRequestAlias scheduleRequestAlias = (ScheduleRequestAlias) c.getAttribute("scheduleRequestAlias");
            PresetClass.deleteScheduleRequestType(scheduleRequestAlias);
        }
    }

    @BeforeMethod(alwaysRun = true, description = "Установка методики ожидания для драйвера")
    private void driverConfig() {
        setBrowserTimeout(sb.getWrappedDriver(), 30);
    }

    @BeforeMethod(alwaysRun = true,
            description = "Отключить двухфакторную аутентификацию на время теста")
    public void disableTwoFactorAuth() {
        changeProperty(SystemProperties.TWO_FACTOR_AUTH, false);
    }

    @BeforeMethod(alwaysRun = true,
            description = "Отключить настройку \"Несколько смен в одном дне в расписании\"")
    public void disableMultipleShiftsInDay() {
        changeProperty(SystemProperties.MULTIPLE_SHIFTS_IN_DAY, false);
    }

    @BeforeMethod(alwaysRun = true,
            description = "Отключить запрос подтверждения выхода из раздела расписания")
    private void disableQuitConfirmationRequest() {
        changeProperty(SystemProperties.ROSTER_QUIT_TAB_NOTICE, false);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = "@Before show overnight shift in both months")
    public void displayOverNightShiftInBothMonths() {
        changeProperty(SystemProperties.WORKED_OR_PUBLISHED_ROSTER, true);
    }

    @BeforeMethod(alwaysRun = true,
            description = "Отключить визуальное отделение временных сотрудников в расписании")
    private void disableScheduleBoardTemporarySeparate() {
        changeProperty(SystemProperties.SCHEDULE_BOARD_TEMPORARY_SEPARATE, false);
    }

    @BeforeMethod(alwaysRun = true,
            description = "Отключить отображение фильтра сотрудников по ролям")
    public void disableDisplayEmployeesFilterByRole() {
        changeProperty(SystemProperties.DISPLAY_EMPLOYEES_FILTER_BY_ROLE, false);
    }

    /**
     * Перейти в раздел расписания сразу на конкретный оргюнит под суперюзером. Если до этого были созданы куки обычных пользователей, они затираются.
     *
     * @param orgUnit - оргюнит на страницу которого осуществляется переход
     */
    private void goToSchedule(OrgUnit orgUnit) {
        new GoToPageSection(sb).goToOmWithoutUI(orgUnit, SECTION);
        sb.spinnerLoader().loadingSpinnerPage().waitUntil("Страница не загрузилась", Matchers.not(DisplayedMatcher.displayed()), 20);

    }

    private void goToScheduleWithCheck(OrgUnit orgUnit) {
        goToSchedule(orgUnit);
        if (getServerDate().equals(LocalDateTools.getFirstDate().plusDays(1))) {
            clickBack();
        }
    }

    @Step("Произвести синхронизацию отметки, перейти в ОМ {omName}")
    private void goToOmAndSyncRecord(int omId, String omName) {
        Cookie cookie = getCookieWithCheck(Projects.WFM);
        //Работает только таким образом (для принудительной синхронизации)
        sb.open(makePath(RELEASE_URL, "integration", "record"));
        systemSleep(1); //метод используется в неактуальных тестах
        sb.open(URL_SB + "/" + omId);
        sb.getWrappedDriver().manage().addCookie(cookie);
        AtlasWebElement firstEmployee = sb.formLayout().employeeNameButtons()
                .waitUntil(Matchers.hasSize(Matchers.greaterThan(0))).get(0);
        waitForClickable(firstEmployee, sb, 30);
        systemSleep(8); //метод используется в неактуальных тестах
    }

    @Step("Пресет. Добавить тестовый комментарий")
    private void createTestShiftComment(String name) {
        PresetClass.createShiftEditReason(name);
    }

    @Step("Пресет. Изменить название тестового комментария c {previousName} на {name}")
    private void updateShiftComment(String previousName, String name) {
        PresetClass.updateShiftEditReason(ShiftEditReasonRepository.getShiftEditReasonByName(previousName).getSelfLink(), name);
    }

    private int getOrgIdFromUrl() {
        String urlAddress = (sb.getWrappedDriver().getCurrentUrl());
        return Integer.parseInt(urlAddress.substring(urlAddress.lastIndexOf("/") + 1));
    }

    private void nonPublishCheck(int omId) {
        PresetClass.nonPublishChecker(omId);
    }

    @Step("Нажать на значок \"Троеточие\"")
    private void threeDotsMenuClick() {
        waitForClickable(sb.formTopBar().buttonOrgUnitMenu(), sb, 15);
        sb.formTopBar().buttonOrgUnitMenu().click();
    }

    @Step("Нажать на кнопку фильтрации по сотрудникам")
    private void employeeFilterButtonClick() {
        sb.formTopBar().employeeFilterButton().waitUntil("Страница ещё загружается", DisplayedMatcher.displayed());
        sb.formTopBar().employeeFilterButton().click();
        LOG.info("Нажимаем на кнопку фильтрации по сотрудникам");
    }

    @Step("Нажать на кнопку редактирования \"Карандаш\"")
    private void schedulePencilClick() {
        sb.subdivisionProperties().editingButton().click();
    }

    @Step("Закрыть окно редактирования свойств подразделения")
    private void orgUnitPropCloseClick() {
        sb.subdivisionProperties().buttonClose().click();
        LOG.info("Закрываем окно редактирования свойств подразделения");
    }

    @Step("Кликнуть на поле тэга")
    private void tagSpaceClick() {
        sb.omEditingForm().tagSpace().click();
    }

    @Step("Ввести тэг: {rnd}")
    private void tagSpaceAdd(String rnd) {
        sb.omEditingForm().tagSpace().sendKeys(rnd);
    }

    @Step("Нажать на кнопку \"Добавить\"")
    private void tagAddOneClick() {
        sb.omEditingForm().tagAddOne().click();
    }

    @Step("Очистить поле названия подразделения")
    private void makeClearOrgUnitNameField() {
        sb.omEditingForm().omFieldInput(OrgUnitInputs.OM_NAME.getFieldType()).click();
        sb.omEditingForm().omFieldInput(OrgUnitInputs.OM_NAME.getFieldType()).clear();
    }

    @Step("Ввести значение в поле названия подразделения: {rndName}")
    private void spaceOrgUnitNameSend(String rndName) {
        sb.omEditingForm().omFieldInput(OrgUnitInputs.OM_NAME.getFieldType()).sendKeys(rndName);
    }

    @Step("Нажать на кнопку \"Изменить\"")
    private void spaceOrgChangeClick() {
        sb.omEditingForm().changeButton().click();
        sb.spinnerLoader().grayLoadingBackground().waitUntil("Спиннер все еще на месте", Matchers.not(DisplayedMatcher.displayed()), 30);
    }

    @Step("Проверить изменение названия ОргЮнита ,предыдущие название: {previousName}, новое название:{newName}")
    private void assertOrgNameChange(String previousName, String newName) {
        sb.subdivisionProperties().editingButton()
                .waitUntil("Кнопка карандаш не была отображена", DisplayedMatcher.displayed(), 10);
        String orgUnitCardName = sb.subdivisionProperties().omName().getText();
        String name = OrgUnitRepository.getOrgUnit(getOrgIdFromUrl()).getName();
        Allure.addAttachment("Название", "Название оргЮнита было успешно сменено с: " + previousName
                + ", на: "
                + name
                + ", текущее имя оргЮнита в карточке: "
                + orgUnitCardName);
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(orgUnitCardName, newName, "Имя ОМ на UI не изменилось");
        softAssert.assertEquals(name, newName, "Имя ОМ в API не изменилось");
        softAssert.assertAll();
    }

    @Step("Проверить изменение тэга. Тэги до: {tagsBefore}. Тэг добавленный: {rndTag}.")
    private void assertTagsChange(OrgUnit orgUnit, List<String> tagsBefore, String rndTag, TagValue tagValue) {
        String tagsCardOrgUnit;
        try {
            tagsCardOrgUnit = sb.omInfoForm().tagsFieldOrgUnitCard().getText();
        } catch (org.openqa.selenium.NoSuchElementException e) {
            tagsCardOrgUnit = "";
        }
        List<String> tagApi = orgUnit.refreshOrgUnit().getTags();
        SoftAssert softAssert = new SoftAssert();
        switch (tagValue) {
            case NO_ONE:
                softAssert.assertTrue(tagApi.contains(rndTag));
                softAssert.assertEquals(tagsCardOrgUnit, rndTag);
                break;
            case SEVERAl:
                softAssert.assertTrue(tagApi.containsAll(tagsBefore));
                softAssert.assertTrue(tagApi.contains(rndTag));
                softAssert.assertTrue(tagApi.containsAll(Arrays.asList(tagsCardOrgUnit.split(", "))));
            case ONE:
                softAssert.assertTrue(tagApi.containsAll(tagsBefore));
                softAssert.assertTrue(tagApi.contains(rndTag));
                softAssert.assertTrue(tagApi.containsAll(Arrays.asList(tagsCardOrgUnit.split(", "))));
                break;
        }
        Allure.addAttachment("Тэги", "У оргЮнита уже были тэги: "
                + tagsBefore.toString() + ". Был добавлен тэг: "
                + rndTag);
        softAssert.assertAll();
    }

    @Step("Проверить удаление тэга. Тэги до удаления: {tagsBefore}, удаленный тэг{deletedTag}")
    private void checkTagRemoval(List<String> tagsBefore, String deletedTag, TagValue tagValue) {
        List<String> tagApi = OrgUnitRepository.getOrgUnit(getOrgIdFromUrl()).getTags();
        SoftAssert softAssert = new SoftAssert();
        switch (tagValue) {
            case SEVERAl:
                tagsBefore.remove(deletedTag);
                String tagsCardOrgUnit = sb.omInfoForm().tagsFieldOrgUnitCard().getText();
                List<String> tagListFromUi = Arrays.asList(tagsCardOrgUnit.split(", "));
                Allure.addAttachment("Удаление", "Был удален тэг: "
                        + deletedTag + ", оставшийся тэг: " + tagsBefore);
                softAssert.assertEquals(tagsBefore, tagListFromUi);
                softAssert.assertEquals(tagApi, tagsBefore);
                break;
            case ONE:
                sb.omInfoForm().tagsFieldOrgUnitCard().should(Matchers.not(DisplayedMatcher.displayed()));
                Allure.addAttachment("Удаление", "До удаления был тэг: " + tagsBefore
                        + ", был удален тэг: " + deletedTag);
                softAssert.assertTrue(tagApi.size() == 0);
        }
        softAssert.assertAll();
    }

    private String getTagToDelete(List<String> tagName, TagValue tagValue) {
        switch (tagValue) {
            case ONE:
                return tagName.get(0);
            case SEVERAl:
                return getRandomFromList(tagName);
        }
        return null;
    }

    @Step("Нажать на кнопку удаления у тэга \"{tagName}\"")
    private void clickOnTagDeleteButton(String tagName) {
        sb.subdivisionProperties().tagNameDeleteButton(tagName)
                .waitUntil("Нужный тэг не появился", DisplayedMatcher.displayed(), 10);
        sb.subdivisionProperties().tagNameDeleteButton(tagName).click();
    }

    @Step("Выбрать дату {date} , на которую не выполнен расчет смен")
    private void pickDateInCalendar(LocalDate date) {
        DatePicker dp = new DatePicker(sb.datePickerForm());
        dp.pickMonth(date);
        dp.okButtonClickWithoutStep();
    }

    @Step("Нажать на кнопку \"Публикация\"")
    private void publishButtonClick() {
        sb.formPublishForm().buttonPublish().click();
    }

    @Step("Нажать на кнопку \"Отклонить\"")
    private void publishRejectButtonClick() {
        sb.formPublishForm().buttonRejectPublish().click();
    }

    @Step("Кликнуть на \"Крестик\" в правом верхнем углу")
    private void closePublicationForm() {
        sb.formPublishForm().waitUntil("Форма не была отображена", DisplayedMatcher.displayed(), 5);
        try {
            sb.formPublishForm().closeButton().click();
        } catch (org.openqa.selenium.WebDriverException e) {
            throw new AssertionError(sb.formErrorForm().getText());
        }
        sb.formPublishForm().waitUntil("Форма все ещё открыта", Matchers.not(DisplayedMatcher.displayed()), 5);
        Allure.addAttachment("Закрытие формы",
                             "После нажатия на \"Крестик\", форма перестала отображаться");
    }

    @Step("Проверить, что публикация графика не прошла")
    private void assertNotPublish() {
        sb.formPublishForm().elementSnackbar()
                .should("Снэкбар не был отображен", DisplayedMatcher.displayed(), 5);
        String message = sb.formPublishForm().elementSnackbar().getText();
        Assert.assertEquals(message, "Невозможно опубликовать смены");
        Allure.addAttachment("Проверка",
                             "После нажатия на кнопку «Опубликовать», появилось следующее сообщение: " + message);
    }

    @Step("Проверить, что первичное расписание не было опубликовано")
    private void assertRosterNotPublished(OrgUnit unit, String errorMessage) {
        sb.errorMessage(errorMessage).should("Сообщение об ошибке не отображается", DisplayedMatcher.displayed());
        Roster roster = RosterRepository.getActiveRosterThisMonth(unit.getId());
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertFalse(roster.isPublished(), "Ростер был опубликован");
        softAssert.assertEquals(roster.getVersion(), 1, "Активная версия ростера изменилась");
        softAssert.assertAll();
    }

    @Step("Кликнуть на кнопку \"Троеточие\"")
    private void shiftThreeDotsClick() {
        LOG.info("Кликаем на кнопку \"Троеточие\"");
        sb.formEditForm().waitUntil("Форма не открылась", DisplayedMatcher.displayed(), 5);
        sb.formEditForm().buttonDotsMenu().click();
        sb.formEditForm().menu()
                .waitUntil("Меню с действиями не отображено", DisplayedMatcher.displayed(), 5);
        sb.formListOfRequest().typeButtonsForm()
                .waitUntil("Кнопка действия с запросом не отображена", DisplayedMatcher.displayed(), 10);
    }

    @Step("Кликнуть на кнопку \"Удалить в меню троеточия\"")
    private void shiftThreeDotsClickDelete() {
        LOG.info("Кликаем на кнопку \"Удалить\"");
        sb.formEditForm().waitUntil("Форма не открылась", DisplayedMatcher.displayed(), 5);
        sb.formEditForm().buttonDelete().click();
        try {
            sb.formEditForm().confirmCorrection().waitUntil("Окно с подтверждением корректировки не найдено", DisplayedMatcher.displayed(), 5);
            sb.formEditForm().saveChangesCorrection().click();
            LOG.info("Подтверждение корректировки \"Нажать кнопку сохранить\"");
        } catch (WaitUntilException ex) {
            LOG.info("Окно с подтверждением корректировки не найдено");
        }
    }

    @Step("Нажать на кнопку \"Создать\"")
    private void createShiftButtonClick() {
        AtlasWebElement createShiftButton = sb.formEditForm().buttonCreateShift();
        createShiftButton.click();
        try {
            sb.correctionConfirmationDialog().confirmationButton().click();
            sb.formEditForm().commentInputField().sendKeys(RandomStringUtils.randomAlphabetic(5));
            createShiftButton.click();
            sb.correctionConfirmationDialog().confirmationButton().click();
        } catch (org.openqa.selenium.NoSuchElementException e) {
            LOG.info("Система не запросила подтверждения корректировки");
        }
        sb.spinnerLoader().loadingSpinnerPage().waitUntil("Спиннер все еще крутится", Matchers.not(DisplayedMatcher.displayed()), 20);
    }

    @Step("Нажать на кнопку \"Изменить\"")
    private void clickEditShiftButton() {
        AtlasWebElement editShiftButton = sb.formEditForm().buttonChangeShift();
        editShiftButton.click();
        LOG.info("Нажимаем на кнопку \"Изменить\"");
        try {
            sb.correctionConfirmationDialog().confirmationButton().click();
        } catch (org.openqa.selenium.NoSuchElementException e) {
            LOG.info("Система не запросила подтверждения корректировки");
        }
        editShiftButton.waitUntil("Форма редактирования не закрылась",
                                  Matchers.not(DisplayedMatcher.displayed()), 10);
        sb.spinnerLoader().loadingSpinnerPage().waitUntil("Загрузка не завершилась",
                                                          Matchers.not(DisplayedMatcher.displayed()), 10);
    }

    @Step("Проверить появление информации об ошибке")
    private void assertErrorMessageDisplayed() {
        sb.formEditForm().errorTextField().should(TextMatcher.text(Matchers.containsString("Поле не может быть пустым")));
        Allure.addAttachment("Проверка", "Форма редактирования не закрывается, поле \"Комментарий\" подсвечивается красным, " +
                "появляется предупреждение \"Поле не может быть пустым\"");
    }

    @Step("Убедиться, что после изменения ячейки фактической смены она окрасилась в красный, а плановая осталась без изменений")
    public void assertMergedUiForShiftsChangesColors(String oldPlanClass, ScheduleWorker scheduleWorker,
                                                     EmployeePosition ep, int omId, DateTimeInterval newInterval) {
        Roster roster = RosterRepository.getWorkedRosterThisMonth(omId);
        LocalDate date = newInterval.getStartDate();
        Shift shift = ShiftRepository.getShiftsForRoster(roster.getId(), new DateInterval(date, date))
                .stream()
                .filter(s -> s.getEmployeePositionId() == ep.getId() &&
                        s.getDateTimeInterval().getStartDate().equals(date))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Смена не найдена"));
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(shift.getDateTimeInterval(), newInterval, "Внесенные изменения не сохранились");

        AtlasWebElement actualShiftElement = scheduleWorker.getPlanOrFactShiftElement(ep, date, true);
        String newActualClass = scheduleWorker.getInternalClass(actualShiftElement);
        softAssert.assertTrue(newActualClass.contains("red") || newActualClass.contains("gantt__plan-mismatch"),
                              "Цвет ячейки фактической смены не стал красным");
        AtlasWebElement planShiftElement = scheduleWorker.getPlanOrFactShiftElement(ep, date, false);
        String newPlanClass = scheduleWorker.getInternalClass(planShiftElement);
        softAssert.assertEquals(newPlanClass, oldPlanClass, "Цвет ячейки плановой смены изменился");
        softAssert.assertAll();
    }

    @Step("Убедиться, что после изменения ячейки фактической смены она окрасилась в красный, а плановая не отображается")
    public void assertUnmergedUIForShiftsChangesColors(ScheduleWorker scheduleWorker, EmployeePosition ep,
                                                       int omId, DateTimeInterval newInterval) {
        Roster roster = RosterRepository.getWorkedRosterThisMonth(omId);
        LocalDate date = newInterval.getStartDate();
        Shift shift = ShiftRepository.getShiftsForRoster(roster.getId(), new DateInterval(date, date))
                .stream()
                .filter(s -> s.getEmployeePositionId() == ep.getId() &&
                        s.getDateTimeInterval().getStartDate().equals(date))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Смена не найдена"));
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(shift.getDateTimeInterval(), newInterval, "Внесенные изменения не сохранились");

        AtlasWebElement planShiftElement = scheduleWorker.getPlanOrFactShiftElement(ep, date, false);
        softAssert.assertNull(planShiftElement, "Ячейка плановой смены отображается отдельно от фактической");

        AtlasWebElement actualShiftElement = scheduleWorker.getPlanOrFactShiftElement(ep, date, true);
        String newActualClass = scheduleWorker.getInternalClass(actualShiftElement);
        softAssert.assertTrue(newActualClass.contains("red") || newActualClass.contains("gantt__plan-mismatch"),
                              "Цвет ячейки фактической смены не стал красным");
        softAssert.assertAll();
    }

    @Step("Выбрать комментарий с названием \"{name}\"")
    private void clickOnComment(String name) {
        sb.formEditForm().commentByNameButton(name).should(COMMENT_NOT_DISPLAYED,
                                                           DisplayedMatcher.displayed());
        sb.formEditForm().commentByNameButton(name).click();
        LOG.info("В выпадающем списке выбираем комментарий \"{}\"", name);
    }

    @Step("Ввести комментарий \"{name}\"")
    private void inputComment(String name) {
        sb.formEditForm().commentInputField().waitUntil("Поле ввода для комментария не прогрузилось", DisplayedMatcher.displayed(), 5);
        new Actions(sb.getWrappedDriver()).moveToElement(sb.formEditForm()).perform();
        sb.formEditForm().commentInputField().sendKeys(Keys.DELETE);
        sb.formEditForm().commentInputField().sendKeys(name);
        LOG.info("Вводим комментарий \"{}\"", name);
        String text = sb.formEditForm().commentInputField().getAttribute(VALUE);
        Assert.assertEquals(text, name, COMMENT_NOT_INPUTTED);
    }

    @Step("Раскрыть список в поле \"Комментарий\"")
    private void clickOnCommentMenu() {
        LOG.info("Кликаем на поле \"Комментарий\"");
        sb.formEditForm().commentInputField().should(NO_COMMENT_FIELD, DisplayedMatcher.displayed());
        sb.formEditForm().commentInputField().click();
    }

    @Step("Нажать на значок времени со стрелкой (рядом с тремя точками)")
    private void listOfSchedulesClick() {
        sb.formTopBar().buttonListOfTimetables().click();
    }

    @Step("В развернувшемся списке кликнуть на версию графика под номером {version}")
    private void clickOnRoster(int version) {
        sb.formTopBar().nonActiveRostersList().get(version - 1).click();
        sb.spinnerLoader().loadingSpinnerPage().waitUntil("Спиннер все еще крутится", Matchers.not(DisplayedMatcher.displayed()), 10);
    }

    private int selectAnotherVersion() {
        WebElement element = sb.formTopBar().nonActiveRostersList()
                .waitUntil(Matchers.hasSize(Matchers.greaterThan(1)))
                .stream()
                .findAny()
                .orElseThrow(() -> new AssertionError("schedule message. Нет подходящего элемента в списке"));
        String text = element.getText();
        int version = Integer.parseInt(text.substring(text.lastIndexOf(" ") + 1));
        Allure.addAttachment("Выбор отличной от активной версии", "Была выбрана " + version + " графика");
        return version;
    }

    @Step("Выбрать \"Режим сравнения табеля\"")
    private void timeSheetCompareButtonClick() {
        sb.formTopBar().timeSheetCompareButton()
                .waitUntil("Кнопка перехода в режим сравнения не отобразилась", DisplayedMatcher.displayed(), 5);
        sb.formTopBar().timeSheetCompareButton().click();
    }

    @Step("Выбрать \"Режим сравнения графиков\"")
    private void scheduleCompareButtonClick() {
        sb.formTopBar().scheduleCompareButton()
                .waitUntil("Кнопка перехода в режим сравнения не отобразилась", DisplayedMatcher.displayed(), 5);
        sb.formTopBar().scheduleCompareButton().click();
        sb.spinnerLoader().loadingSpinnerPage().waitUntil("Спиннер все еще не пропал", Matchers.not(DisplayedMatcher.displayed()), 10);
    }

    @Step("Проверить, что табель учёта или фактическое посещение отображается")
    private boolean isTimeSheetDisplayed() {
        return sb.formLayout().timesheetIndicator().size() >= 2;
    }

    @Step("Нажать на \"Табель учёта\"")
    private void isTimeSheetButtonClick() {
        sb.formTopBar().isTimeSheetButton()
                .waitUntil("Кнопка \"Табель учёта\" не отобразилась", DisplayedMatcher.displayed(), 5);
        sb.formTopBar().isTimeSheetButton().click();
        sb.spinnerLoader().loadingSpinnerPage().waitUntil("Спиннер все еще не пропал", Matchers.not(DisplayedMatcher.displayed()), 10);
    }

    @Step("Кликнуть на смену сотрудника {employee} за {date}")
    private void clickShiftElement(AtlasWebElement shift, LocalDate date, EmployeePosition employee) {
        //Performing the mouse hover action on the target element.
        new Actions(sb.getWrappedDriver()).moveToElement(shift).perform();
        shift.click();
        LOG.info("Кликаем на смену сотрудника {} за {}", employee, date);
    }

    @Step("Проверить, что сотруднику {employeePosition} был добавлен запрос \"{type.name}\"")
    private void assertRequestAdding(EmployeePosition employeePosition, LocalDate date,
                                     ScheduleRequestType type, ScheduleWorker scheduleWorker, OrgUnit unit, ScheduleRequestStatus status, LocalTime... time) {
        LOG.info("Проверяем, что сотруднику {} был добавлен запрос {}", employeePosition, type.getName());
        List<ScheduleRequest> employeeRequest = ScheduleRequestRepository.getEmployeeScheduleRequests(employeePosition.getEmployee().getId(),
                                                                                                      new DateInterval(date), unit.getId());
        ScheduleRequest request = employeeRequest.size() != 0 ? employeeRequest.get(0) : null;
        Assert.assertNotNull(request, "Добавленный ранее запрос не был отображен в API");
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertNotNull(scheduleWorker.getScheduleRequestElement(employeePosition, date),
                                 "Запрос не был добавлен на UI");
        if (time.length == 0 || time[0] == null) {
            softAssert.assertEquals(request.getDateTimeInterval().getStartDate(), date, "Дата начала запроса не совпала");
        } else {
            DateTimeInterval interval = new DateTimeInterval(date.atStartOfDay(), LocalDateTime.of(date, time[0]));
            softAssert.assertEquals(interval, request.getDateTimeInterval(), "Дата и время начала запроса не соответствуют заданным в ходе теста");
        }
        softAssert.assertEquals(type, request.getType(), "Тип запроса указанный при добавлении не совпал с текущим");
        if (status != null) {
            softAssert.assertEquals(status, request.getStatus(),
                                    String.format("Статус запроса не соответствует ожидаемому: %s", status));
        }
        softAssert.assertAll();
    }

    /**
     * Проверяет, не был ли добавлен запрос в расписание сотрудника
     *
     * @param date - дата, за которую проверяем запрос
     * @param type - тип запроса
     */
    private boolean ifRequestNotAdded(EmployeePosition employeePosition, LocalDate date,
                                      ScheduleRequestType type, OrgUnit unit, LocalTime... time) {
        List<ScheduleRequest> employeeRequest = ScheduleRequestRepository.getEmployeeScheduleRequests(employeePosition.getEmployee().getId(),
                                                                                                      new DateInterval(date), unit.getId());
        ScheduleRequest request = employeeRequest.size() != 0 ? employeeRequest.get(0) : null;
        if (request != null && request.getType().equals(type)) {
            if (time.length == 0 || time[0] == null) {
                return !request.getDateTimeInterval().getStartDate().equals(date);
            } else {
                DateTimeInterval interval = new DateTimeInterval(date.atStartOfDay(), LocalDateTime.of(date, time[0]));
                return !interval.equals(request.getDateTimeInterval());
            }
        }
        return true;
    }

    @Step("Проверить, что c {startDate} по {endRepeatDate} с периодичностью \"{periodicity.repeatType}\" были добавлены запросы расписания")
    private void assertPeriodicalScheduleRequestsAdded(Periodicity periodicity, LocalDate startDate, LocalDate endRepeatDate,
                                                       EmployeePosition ep, ScheduleRequestType type, ScheduleWorker sw,
                                                       OrgUnit unit, ScheduleRequestStatus status, LocalTime time) {
        SoftAssert softAssert = new SoftAssert();
        List<LocalDate> dates = new DateInterval(startDate, endRepeatDate).getBetweenDatesList();
        LocalDate date;
        Iterator<LocalDate> it = dates.iterator();
        if (periodicity.equals(Periodicity.DAILY)) {
            while (it.hasNext()) {
                date = it.next();
                assertRequestAdding(ep, date, type, sw, unit, status, time);
            }
        } else {
            while (it.hasNext()) {
                date = it.next();
                if (date.getDayOfWeek().equals(startDate.getDayOfWeek())) {
                    assertRequestAdding(ep, date, type, sw, unit, status);
                } else {
                    softAssert.assertTrue(ifRequestNotAdded(ep, date, type, unit, time),
                                          String.format("Запрос был добавлен %s", date));
                }
            }
        }
        if (!endRepeatDate.isAfter(LocalDateTools.getLastDate().minusDays(1))) {
            softAssert.assertTrue(ifRequestNotAdded(ep, endRepeatDate.plusDays(1), type, unit, time),
                                  "Запрос был добавлен после даты окончания периода");
        }
        softAssert.assertAll();
    }

    @Step("Проверить, что был создан запрос сотруднику с именем {employeePosition}")
    private void assertRepeatRequestAdding(EmployeePosition employeePosition, ScheduleRequestType type, Periodicity repeat,
                                           LocalDate startDate, LocalDate endDate, ScheduleWorker scheduleWorker, OrgUnit unit, List<ScheduleRequest> requestsBefore) {
        List<ScheduleRequest> requestsAfter = ScheduleRequestRepository.getEmployeeScheduleRequests(employeePosition.getEmployee().getId(),
                                                                                                    new DateInterval(startDate, endDate), unit.getId());
        requestsAfter.removeAll(requestsBefore);
        assertFalse(requestsAfter.isEmpty(), "Новый запрос не отобразился в API");
        SoftAssert softAssert = new SoftAssert();
        ScheduleRequest request = requestsAfter.iterator().next();
        JSONObject repeatRule = request.getRepeatRule();
        softAssert.assertTrue(repeatRule.length() > 0, "Созданный ранее запрос не повторяющийся");
        softAssert.assertNotNull(scheduleWorker.getScheduleRequestElement(employeePosition, startDate),
                                 "Запрос не был добавлен на UI");
        softAssert.assertEquals(repeat.toString(), repeatRule.getString("repeat"));
        softAssert.assertEquals(type.toString(), repeatRule.getString("type"));
        DateInterval dateInterval = new DateInterval(repeatRule.getJSONObject("dateInterval"));
        softAssert.assertEquals(dateInterval.getStartDate(), startDate, "Дата начала запроса не совпала");
        softAssert.assertEquals(dateInterval.getEndDate(), endDate, "Дата окончания повторения не совпала");
        softAssert.assertAll();
    }

    @Step("Проверить переход в \"Режим сравнения табеля\"")
    private void goToTimeSheetAndTimeTableCompareCheck(int employeeNumber) {
        sb.compareScheduleMode().waitUntil("Переход не был осуществлен", DisplayedMatcher.displayed(), 15);
        sb.compareScheduleMode().comparisonModeSnackBar()
                .should("Снэкбар не был отображен", DisplayedMatcher.displayed(), 10);
        List<String> rosters = sb.compareScheduleMode().rosterVersionsIconsList().stream().map(WebElement::getText)
                .filter(s -> s.equals("Версия")).collect(Collectors.toList());
        List<String> timeTables = sb.compareScheduleMode().timeTableIconsList().stream().map(WebElement::getText)
                .filter(s -> s.equals("Табель учета")).collect(Collectors.toList());
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(rosters.size(), employeeNumber);
        softAssert.assertEquals(timeTables.size(), employeeNumber);
        softAssert.assertAll();
        String content = "Был успешно выполнен переход в режим сравнения, были отображены иконки табеля учета и версии у каждого сотрудника";
        Allure.addAttachment("Переход в режим сравнения", content);
    }

    @Step("Проверить переход в \"Режим сравнения табеля\"")
    private void goToTimeSheetCompareCheck(int employeeNumber, int version, int addedVersion) {
        sb.compareScheduleMode().waitUntil("Переход не был осуществлен", DisplayedMatcher.displayed(), 15);
        sb.compareScheduleMode().comparisonModeSnackBar()
                .should("Снэкбар не был отображен", DisplayedMatcher.displayed(), 10);
        List<String> rostersActive;
        List<String> rostersAdded;
        if (addedVersion > version) {
            rostersActive = sb.compareScheduleMode().oddIcons().stream().map(WebElement::getText)
                    .filter(s -> s.equals("Версия " + version)).collect(Collectors.toList());
            rostersAdded = sb.compareScheduleMode().evenIcons().stream().map(WebElement::getText)
                    .filter(s -> s.equals("Версия " + addedVersion)).collect(Collectors.toList());
        } else {
            rostersActive = sb.compareScheduleMode().evenIcons().stream().map(WebElement::getText)
                    .filter(s -> s.equals("Версия " + version)).collect(Collectors.toList());
            rostersAdded = sb.compareScheduleMode().oddIcons().stream().map(WebElement::getText)
                    .filter(s -> s.equals("Версия " + addedVersion)).collect(Collectors.toList());

        }
        Allure.addAttachment("Переход в режим сравнения",
                             "Был успешно выполнен переход в режим сравнения были отображены иконки табеля учета и версии " +
                                     "у каждого сотрудника, текст на отображенных иконках "
                                     + rostersActive.get(0) + " " + rostersAdded.get(0));
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(rostersActive.size(), employeeNumber,
                                "У активного ростера количество иконок не совпало с количеством сотрудников");
        softAssert.assertEquals(rostersAdded.size(), employeeNumber,
                                "У добавленного ростера количество иконок не совпало с количеством сотрудников");
        softAssert.assertAll();
    }

    @Step("Проверить, что у сотрудника {ep} удалились смены после {date} включительно и сохранились до этой даты")
    private void assertNoShiftsAfterCertainDate(EmployeePosition ep, LocalDate date, List<Shift> shiftsBefore) {
        List<Shift> shiftsAfter = ShiftRepository.getShiftsBeforeDate(ep, date);
        SoftAssert softAssert = new SoftAssert();
        if (!shiftsBefore.isEmpty()) {
            softAssert.assertFalse(shiftsAfter.isEmpty(), "Смены до даты выхода в декрет пропали");
        }
        Allure.addAttachment("Смены до даты выхода в декрет до перерасчета", shiftsBefore.stream()
                .map(Shift::toString)
                .collect(Collectors.joining(",\n")));
        Allure.addAttachment("Смены до даты выхода в декрет после перерасчета", shiftsAfter.stream()
                .map(Shift::toString)
                .collect(Collectors.joining(",\n")));
        List<Shift> shiftsAfterLeaveStart = ShiftRepository.getShifts(ep, ShiftTimePosition.ALLMONTH)
                .stream()
                // Сама дата выхода также должна очиститься, поэтому -1 день
                .filter(s -> s.getDateTimeInterval().getStartDate().isAfter(date.minusDays(1)))
                .collect(Collectors.toList());
        softAssert.assertTrue(shiftsAfterLeaveStart.isEmpty(), "Смены, начиная с даты выхода в декрет, не исчезли");
        softAssert.assertAll();
    }

    @Step("Проверить правильность расчета")
    private int employeesSize() {
        sb.formLayout().employeeNameButtons().waitUntil(Matchers.hasSize(Matchers.greaterThan(1)));
        return sb.formLayout().employeeNameButtons().size();
    }

    private List<String> getEmployeesNamesOnUi() {
        sb.subdivisionProperties().employeesNames().waitUntil(Matchers.hasSize(Matchers.greaterThan(1)));
        return sb.formLayout().employeeNameButtons().extract(WebElement::getText);
    }

    private String getRandomEmployeeFromUI() {
        List<AtlasWebElement> tempEmployees = sb.formLayout().allEmployeeNameButtons();
        int random = new Random().nextInt(tempEmployees.size());
        String empName = tempEmployees.get(random).getText();
        LOG.info("Выбран сотрудник: {}", empName);
        Allure.addAttachment("Сотрудник", "Выбран сотрудник с именем: " + empName);
        return empName;
    }

    @Step("Кликнуть на сотрудника с именем {employeePosition}")
    private void clickOnEmployeeNameButton(EmployeePosition employeePosition) {
        int emId = employeePosition.getId();
        new Actions(sb.getWrappedDriver()).moveToElement(sb.formLayout().nameButton(emId));
        sb.formLayout().nameButton(emId).click();
        Allure.addAttachment("Имя сотрудника", "text/plain", "Выбран сотрудник с именем: " + employeePosition);
    }

    @Step("Проверить, что был осуществлен переход в карточку сотрудника с именем {name}")
    private void goToEmployeeCardCheck(String name) {
        sb.employeeDataMenu().employeeDataList().waitUntil(Matchers.hasSize(Matchers.greaterThan(3)));
        String cardEmployeeNameFull = sb.employeeDataMenu().employeeNameField().getText().trim();
        String[] separatedName = cardEmployeeNameFull.split(" ");
        if (separatedName.length == 3) {
            String nameAndLastName = separatedName[0] + " " + separatedName[1];
            Allure.addAttachment("Проверка",
                                 "Был выбран сотрудник с именем: " + name + ", в карточке указано имя: " + nameAndLastName);
            Assert.assertEquals(nameAndLastName, name, "Имя в карточке и имя выбранного сотрудника не совпадает");
        } else {
            Assert.fail("schedule message. Подумать что делать со сложными именами");
        }
    }

    @Step("Проверить, что количество свободных смен на UI больше {countShift}")
    private void assertCountFreeShift(int countShift) {
        Assert.assertTrue(sb.freeShiftList().startDates().size() > countShift, "Количество свободных смен на UI меньше " + countShift);
    }

    @Step("Проверить, что появился поп-ап \"Сумма часов за дату {date} у сотрудника {employeeFullName} более 24 часов.\"")
    public void assertForHourLimitExceeded(LocalDate date, String employeeFullName) {
        sb.formLayout().popUpForHourLimitExceeded(date, employeeFullName).waitUntil("Поп-ап не отобразился", DisplayedMatcher.displayed(), 10);
    }

    @Step("В карточке сотрудника в правом верхнем углу нажать на кнопку \"Карандаш\"")
    private void clickOnMainPencilButton() {
        sb.employeeDataMenu().mainPencilButton()
                .waitUntil("Кнопка \"Карандаш\" не отображена ", DisplayedMatcher.displayed(), 5);
        sb.employeeDataMenu().mainPencilButton().click();
        sb.employeeDataMenu().avatarIcon()
                .waitUntil("Аватарка сотрудника не загрузилась", DisplayedMatcher.displayed(), 10);
    }

    @Step("Ввести дату окончания работ дата: {date}")
    private void enterEndWorkDate(LocalDate date) {
        sb.employeeDataMenu().endWorkDateInput().clear();
        sb.employeeDataMenu().endWorkDateInput().sendKeys(date.format(UI_DOTS.getFormat()));
    }

    @Step("Нажать на кнопку \"Изменить\"")
    private void clickOnEmployeeDataSaveButton() {
        sb.employeeDataMenu().mainSaveButton().click();
        sb.spinnerLoader().grayLoadingBackground().waitUntil("Спинер загрузки ещё отображается",
                                                             Matchers.not(DisplayedMatcher.displayed()), 10);
    }

    @Step("Проверить добавление даты окончания работы")
    private void checkEndWorkDateAdding(LocalDate date, Employee employee) {
        LocalDate employeeEndWork = employee.refreshEmployee().getEndWorkDate();
        Assert.assertEquals(employeeEndWork, date,
                            "Дата окончания работы сотрудника: " + employee + " не совпадает");
    }

    /**
     * Метод кликает на шеврон пока у него не изменится статус
     *
     * @param employeeInfoName - определенный блок в карточке сотрудника
     */
    @Step("Нажать на кнопку раскрытия {employeeInfoName.nameOfInformation}")
    private void clickOnShowButton(EmployeeInfoName employeeInfoName) {
        sb.employeeDataMenu().listOfEmpFields()
                .waitUntil(Matchers.hasSize(Matchers.equalTo(EmployeeInfoName.values().length)));
        ChevronStatus currentChevron = getChevronStatus(employeeInfoName);
        if (ChevronStatus.CLOSE == currentChevron) {
            while (currentChevron != ChevronStatus.OPEN) {
                sb.employeeDataMenu().showButton(employeeInfoName.getNameOfInformation()).click();
                currentChevron = getChevronStatus(employeeInfoName);
            }
        } else {
            LOG.info("У шеврона не тот статус {}", ChevronStatus.CLOSE);
        }
    }

    /**
     * Определяет текущий статус шеврона при просмотре карточки сотрудника
     *
     * @param infoName - определенной блок в карточке
     * @return - обновленный статус исходя из класса элемента
     */
    private ChevronStatus getChevronStatus(EmployeeInfoName infoName) {
        String[] classSeparatedString = sb.employeeDataMenu()
                .chevronButton(infoName.getNameOfInformation())
                .getAttribute("class")
                .split(" ");
        Assert.assertEquals(classSeparatedString.length, 3);
        final String currentStatusString = classSeparatedString[2];
        return Arrays.stream(ChevronStatus.values())
                .filter(chevronStatus -> chevronStatus.getStatus().contains(currentStatusString))
                .findAny()
                .orElseThrow(() -> new AssertionError("schedule message. Такого элемента нет в списке"));
    }

    @Step("Нажать на значок карандаша параметра {infoName.nameOfInformation}")
    private void clickOnPencilButton(EmployeeInfoName infoName) {
        sb.employeeDataMenu().pencilButton(infoName.getNameOfInformation()).click();
    }

    @Step("Нажать на значок карандаша параметра {infoName.nameOfInformation}")
    private void clickOnParametersPencilButton(EmployeeInfoName infoName) {
        sb.employeeDataMenu()
                .waitUntil("Форма данных о сотруднике не отобразилась.", DisplayedMatcher.displayed(), 10);
        new Actions(sb.getWrappedDriver()).moveToElement(sb.employeeDataMenu().parametersPencilButton(infoName.getNameOfInformation()));
        sb.employeeDataMenu().parametersPencilButton(infoName.getNameOfInformation()).waitUntil("Карандаш редактирования мат. параметров не отображается",
                                                                                                DisplayedMatcher.displayed(), 2);
        sb.employeeDataMenu().parametersPencilButton(infoName.getNameOfInformation()).click();
        sb.employeeDataMenu().employeeParametersMenu().waitUntil("Форма редактирования мат. параметров не открылась", DisplayedMatcher.displayed(), 2);
    }

    @Step("Ввести значение {value} в пустое поле параметра {params.name}")
    private void enterParamValue(EmployeeParams params, String value) {
        new Actions(sb.getWrappedDriver()).moveToElement(sb.subdivisionProperties().paramInputField(params.getName()));
        sb.subdivisionProperties().paramInputField(params.getName()).sendKeys(value);
    }

    @Step("Активировать чекбокс возле поля \"{skills.name}\"")
    private void clickOnSkillsCheckBox(EmployeeSkills skills) {
        sb.employeeDataMenu().skillCheckBox(skills.getName()).click();
    }

    @Step("Нажать на кнопку коррекции {employeeInfoName.nameOfInformation} с ожиданием её доступности")
    private void clickOnProblemPencilButton(EmployeeInfoName employeeInfoName) {
        while (!sb.parameterForm().isDisplayed()) {
            sb.employeeDataMenu().pencilButton(employeeInfoName.getNameOfInformation()).click();
        }
    }

    @Step("Активировать чекбокс возле произвольно выбранного поля навыка")
    private void clickOnAnySkillCheckBox(List<String> list) {
        List<String> allSkills = new LinkedList<>();
        for (EmployeeSkills skills : EmployeeSkills.values()) {
            allSkills.add(skills.getName());
        }
        allSkills.removeAll(list);
        String freeSkill = allSkills.stream().findAny().orElse(null);
        Assert.assertNotNull(freeSkill, "schedule message. Не смогли выбрать случаный навык");
        sb.employeeDataMenu().skillCheckBox(freeSkill).click();
        Allure.addAttachment("Навык", "Был активирован навык: " + freeSkill);
    }

    @Step("Деактивировать отмеченные чекбоксы")
    private void clickOnAllActiveCheckBoxes(List<String> currentSkills) {
        for (String skill : currentSkills) {
            sb.employeeDataMenu().skillCheckBox(skill).click();
        }
    }

    private List<String> getCurrentSkills() {
        if (sb.employeeDataMenu().skillsNamesField().size() > 0) {
            return sb.employeeDataMenu().skillsNamesField().stream().map(WebElement::getText).collect(Collectors.toList());
        } else {
            return new ArrayList<>();
        }
    }

    @Step("Ввести E-mail: {email} в контактах сотрудника")
    private void typeEmailInContacts(String email) {
        sb.employeeDataMenu().addressTypeInput()
                .waitUntil("Лист с информацией не отобразился", DisplayedMatcher.displayed(), 5);
        sb.employeeDataMenu().emailFieldInput().clear();
        sb.employeeDataMenu().emailFieldInput().sendKeys(email);
    }

    @Step("Очистить окно ввода E-mail")
    private void cleanEmailInput() {
        sb.employeeDataMenu().addressTypeInput()
                .waitUntil("Лист с информацией не отобразился", DisplayedMatcher.displayed(), 5);
        sb.employeeDataMenu().emailFieldInput().clear();
    }

    @Step("Проверить, что график был опубликован")
    private void publicationAssert(ZonedDateTime localDateTimeServer, OrgUnit unit) {
        SoftAssert softAssert = new SoftAssert();
        Roster activeRoster = RosterRepository.getActiveRosterThisMonth(unit.getId());
        softAssert.assertTrue(activeRoster.isPublished(), "Статус графика в API не изменился на \"Опубликован\"");
        LocalDateTime fullDateTimeApi = activeRoster.getPublicationTime();
        if (fullDateTimeApi == null) {
            throw new AssertionError("График не опубликован - у ростера отсутствует время публикации");
        }
        ZonedDateTime apiZonedTime;
        try {
            apiZonedTime = fullDateTimeApi.atZone(unit.getTimeZone());
        } catch (NullPointerException e) {
            apiZonedTime = fullDateTimeApi.atZone(ZoneId.of("UTC"));
        }
        softAssert.assertTrue(localDateTimeServer.until(apiZonedTime, ChronoUnit.SECONDS) < 30,
                              String.format("Время в api - %s текущее время - %s", apiZonedTime, localDateTimeServer));
        String indicator = waitForRosterStatusToChange(GraphStatus.PUBLISH, true);
        if (!indicator.contains(".")) {
            softAssert.assertEquals(indicator, "Плановый график опубликован",
                                    "Статус графика на UI не изменился на \"Опубликован\"");
        }
        softAssert.assertAll();
        Allure.addAttachment("Публикация расписания", String.format("Для расчета был выбран текущий месяц.\n" +
                                                                            "Локальное время публикации - %s, время публикации в API - %s.\n" +
                                                                            "График отображается как опубликованный в API и на UI.",
                                                                    localDateTimeServer, apiZonedTime));
    }

    @Step("Нажать на кнопку \"Изменить\"")
    private void clickOnChangeButton(EmployeeInfoName infoName) {
        sb.employeeDataMenu().changeButton(infoName.getNameOfInformation()).click();
        sb.employeeDataMenu().pencilButton(infoName.getNameOfInformation())
                .waitUntil("Изменения не были приняты", DisplayedMatcher.displayed(), 5);
        sb.spinnerLoader().grayLoadingBackground().waitUntil("Спиннер все еще крутится", Matchers.not(DisplayedMatcher.displayed()), 5);
    }

    @Step("Проверить добавление параметра")
    private void assertParamAdding(String value, EmployeeParams params, int employeeId) {
        String path = makePath(EMPLOYEES, employeeId, MATH_PARAMETER_VALUES, params.getId());
        try {
            JSONObject someObject = getJsonFromUri(Projects.WFM, URL_SB, path);
            String apiValue = someObject.getString(VALUE);
            Allure.addAttachment("Проверка",
                                 "В ходе проверки были сравнены значения параметра в апи:  " + apiValue + " и введенего в ходе теста: " + value);
            Assert.assertEquals(apiValue, value, "Текст параметра не совпадает");
        } catch (AssertionError e) {
            Assert.fail("Параметр : " + params + " не был добавлен сотруднику: " + employeeId);
        }
    }

    @Step("Проверить выбор варианта: {variant.name} в матпараметре: {matchParameter.nameParam} у сотрудника: {name.lastName}")
    private void assertParamChanging(VariantsInMathParameters variant, MathParameters matchParameter, Employee employee) {
        int employeeId = employee.getId();
        String name = employee.getShortName();
        String path = makePath(EMPLOYEES, employeeId, MATH_PARAMETER_VALUES, matchParameter.getMathParamId());
        JSONObject someObject;
        boolean matcher;
        String e = null;
        switch (variant) {
            case OFF:
                someObject = getJsonFromUri(Projects.WFM, URL_SB, path);
                matcher = someObject.getBoolean(VALUE);
                String content = "В матпараметре: " + matchParameter + " у сотрудника: " + name
                        + " был выбран вариант: " + variant;
                Allure.addAttachment("Описание действий", "text/plain", content);
                assertFalse(matcher, "Поле " + matchParameter + " не изменилось на " + variant);
                break;
            case ON:
                someObject = getJsonFromUri(Projects.WFM, URL_SB, path);
                matcher = someObject.getBoolean(VALUE);
                Allure.addAttachment("Описание действий", "text/plain",
                                     "В матпарметре: " + matchParameter + " у сотрудника: " + name + " был выбран вариант: " + variant);
                assertTrue(matcher, "Поле " + matchParameter + " не изменилось на " + variant);
                break;
            case INHERITED_VALUE:
                try {
                    someObject = getJsonFromUri(Projects.WFM, URL_SB, path);
                } catch (AssertionError error) {
                    e = error.getMessage();
                }
                Allure.addAttachment("Описание действий", "text/plain",
                                     "В матпарметре: " + matchParameter + " у сотрудника: " + name + " был выбран вариант: " + variant);
                Assert.assertEquals(e, "expected [200] but found [404]", "Поле " + matchParameter + " не изменилось на " + variant);
                break;
        }
    }

    @Step("Проверить добавление всех трёх навыков.До добавления было {previouslySize} навыков")
    private void checkAllSkillsAdding(int previouslySize, int id) {
        sb.employeeDataMenu().changeButton(EmployeeInfoName.SKILLS.getNameOfInformation())
                .waitUntil("Кнопка все еще отображена", Matchers.not(DisplayedMatcher.displayed()), 5);
        List<String> uiSkills = sb.employeeDataMenu().skillsNamesField().stream()
                .map(WebElement::getText).collect(Collectors.toList());
        List<String> neededSkills = new ArrayList<>();
        for (EmployeeSkills skills : EmployeeSkills.values()) {
            neededSkills.add(skills.getName());
        }
        int newSize = checkValueOfSkill(id);
        Allure.addAttachment("Проверка в API",
                             String.format("До добавления у сотрудника было %d навыков, после добавления стало %d навыков",
                                           previouslySize, newSize));
        Allure.addAttachment("Проверка на UI",
                             String.format("Отображены следующие навыки у сотрудника: %s. Должны быть: %s.",
                                           uiSkills, neededSkills));
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(newSize - previouslySize, 3, "Количество навыков не совпадает");
        softAssert.assertEquals(neededSkills, uiSkills, "Навыки не совпадают");
        softAssert.assertAll();
    }

    @Step("Проверить добавление одного навыка")
    private void checkOneSkillAdding(int previouslySize, int id) {
        sb.employeeDataMenu().changeButton(EmployeeInfoName.SKILLS.getNameOfInformation())
                .waitUntil("Кнопка все еще отображена", Matchers.not(DisplayedMatcher.displayed()), 5);
        int newSizeUi = sb.employeeDataMenu().skillsNamesField().size();
        int newSizeApi = checkValueOfSkill(id);
        Allure.addAttachment("Проверка", "Был добавлен один навык выбранному сотруднику, до добавления навыков было "
                + previouslySize + ", после добавления стало " + newSizeApi);
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(newSizeApi - previouslySize, 1, "Навык не добавлен");
        softAssert.assertEquals(newSizeUi - previouslySize, 1, "Навык не добавлен");
        softAssert.assertAll();
    }

    @Step("Проверить удаление навыков")
    private void checkSkillsDeletion(int previouslySize, int id) {
        sb.employeeDataMenu().changeButton(EmployeeInfoName.SKILLS.getNameOfInformation())
                .waitUntil("Кнопка все еще отображена", Matchers.not(DisplayedMatcher.displayed()), 5);
        sb.employeeDataMenu().skillsNamesField()
                .should("Поле с названием навыков все еще отображено", Matchers.not(DisplayedMatcher.displayed()));
        int newSizeApi = checkValueOfSkill(id);
        Allure.addAttachment("Проверка", "Был добавлен один навык выбранному сотруднику, до добавления навыков было "
                + previouslySize + ", после добавления стало " + newSizeApi);
        Assert.assertEquals(newSizeApi, 0, "Навыки не удалены");
    }

    @Step("Проверить, что e-mail был изменен")
    private void assertEmail(Employee employee, String email) {
        String apiEmail = employee.refreshEmployee().getEmail();
        Assert.assertEquals(apiEmail, email, "E-mail не изменился");
    }

    @Step("Кликнуть на чекбокс \"требуется наставник\"")
    private void internCheckBoxClick() {
        sb.employeeDataMenu().internCheckBox().click();
        sb.employeeDataMenu().mentorsListOpenButton()
                .waitUntil("Ожидание появления кнопки календаря", DisplayedMatcher.displayed(), 5);
    }

    @Step("Деактивировать один или несколько чекбоксов отображения сотрудника напротив сотрудников из списка. " +
            "В данном тесте значение деактивированных чекбоксов равно {value}")
    private void deactivateCheckBoxes(int value) {
        sb.employeesFilterMode()
                .waitUntil("Меню фильтра сотрудников не было отображено", DisplayedMatcher.displayed(), 10);
        //зафиксировать уже снятые чекбоксы до того, как тест начнет снимать еще, для правильного отражения работы теста в логах
        List<String> nonActiveEmployeesPre = sb.employeesFilterMode().employeesWithNoActiveCheckBoxes()
                .stream().map(WebElement::getText).collect(Collectors.toList());
        ElementsCollection<AtlasWebElement> checkBoxes = sb.employeesFilterMode().checkBoxesList();
        for (int i = 0; i < value; i++) {
            checkBoxes.get(i).click();
        }
        List<String> nonActiveEmployees = sb.employeesFilterMode().employeesWithNoActiveCheckBoxes()
                .stream().map(WebElement::getText).collect(Collectors.toList());
        nonActiveEmployees.removeAll(nonActiveEmployeesPre);
        LOG.info("Были деактивированы чекбоксы у {}", nonActiveEmployees);
        Allure.addAttachment("Деактивированные чекбоксы",
                             "Были деактивированы чекбоксы у следующих сотрудников: " + nonActiveEmployees);
    }

    @Step("Нажать на чекбокс \"Выбрать все\"")
    private void clickOnSelectAllCheckBox() {
        sb.employeesFilterMode().checkBoxesSelectAll().click();
    }

    private List<String> activeEmployees() {
        return sb.employeesFilterMode().employeesWithActiveCheckBoxes()
                .stream()
                .map(WebElement::getText)
                .map(e -> e.replaceAll("\\s+", " ").trim())
                .collect(Collectors.toList());
    }

    @Step("Выбрать дату окончания стажерской программы")
    private void selectTheDate(LocalDate date) {
        sb.employeeDataMenu().calendarButton().click();
        DatePicker datePicker = new DatePicker(sb.datePickerForm());
        datePicker.pickDate(date);
        datePicker.okButtonClick();
    }

    @Step("*особые условия(заходим в меню), Проверка того что тип поменялся")
    private void typeChangeCheck(BusinessHours scheduleId) {
        int orderNumber = determineActiveScheduleNumber();
        openScheduleSelectionMenu();
        String attributeText = sb.selectScheduleForm().typeFieldActiveSchedule().getAttribute(VALUE);
        Allure.addAttachment("Тип текущего графика",
                             String.format("В данный момент активирован график с типом \"%s\"", attributeText));
        Assert.assertEquals(determineActiveScheduleId(orderNumber), scheduleId.getDateInterval().toString());
    }

    @Step("Проверить, что был добавлен новый график")
    private void scheduleCheckAdding(List<BusinessHours> before, List<BusinessHours> after) {
        sb.selectScheduleForm()
                .waitUntil("Форма выбора графика отображена", Matchers.not(DisplayedMatcher.displayed()), 5);
        after.removeAll(before);
        Allure.addAttachment("График", "График до " + before + ", график после " + after +
                "Было добавлено графиков " + after.size());
        Assert.assertEquals(after.size(), 1, "Проблемы с добавлением графика");

    }

    @Step("Нажать на значок \"Троеточие\"")
    private void clickOnThreeDotsButton() {
        sb.subdivisionProperties().threeDotsButton()
                .waitUntil("Кнопка троеточия не отображается", DisplayedMatcher.displayed(), 10);
        sb.subdivisionProperties().threeDotsButton().click();
    }

    @Step("Нажать на день с выбранным типом, который по счету {dayNumber}")
    private void clickOnDayTypeChangeButton(int dayNumber) {
        sb.subdivisionProperties().daysTypes().waitUntil(Matchers.hasSize(Matchers.greaterThan(1)));
        sb.subdivisionProperties().dayType(dayNumber).click();
    }

    @Step("Нажать на кнопку дня с типом {days.nameOfDay}")
    private void switchDayTypeTo(Days days) {
        sb.subdivisionProperties().dayTypeButton(days.getNameOfDay()).waitUntil(DisplayedMatcher.displayed());
        sb.subdivisionProperties().dayTypeButton(days.getNameOfDay()).click();
    }

    @Step("Нажать на кнопку \"Изменить\"")
    private void clickOnEditionScheduleChangeButton() {
        sb.subdivisionProperties().editionScheduleSaveButton().click();
        sb.spinnerLoader().grayLoadingBackground().waitUntil("Спинер ещё отображается",
                                                             Matchers.not(DisplayedMatcher.displayed()), 5);
    }

    @Step("У дня c номером {dayNumber} изменить \"Время начала\" на {time}")
    private void changeDayStartTime(String time, int dayNumber) {
        sb.subdivisionProperties().dayStartTimeField(dayNumber)
                .waitUntil("Поле с временем не отобразилось", DisplayedMatcher.displayed(), 5);
        sb.subdivisionProperties().dayStartTimeField(dayNumber).click();
        sb.subdivisionProperties().dayStartTimeField(dayNumber).clear();
        sb.subdivisionProperties().dayStartTimeField(dayNumber).sendKeys(time);
    }

    @Step("У дня c номером {dayNumber} изменить \"Время окончания\" на {time}")
    private void changeDayEndTime(String time, int dayNumber) {
        sb.subdivisionProperties().dayEndTimeField(dayNumber).click();
        sb.subdivisionProperties().dayEndTimeField(dayNumber).clear();
        sb.subdivisionProperties().dayEndTimeField(dayNumber).sendKeys(time);
    }

    @Step("Проверить, что время начала {startTime} и окончания {endTime} у дня с номером {dayNumber}")
    private void switchDayTimeCheck(int dayNumber, String scheduleId, String startTime, String endTime) {
        sb.subdivisionProperties().threeDotsButton().waitUntil("Кнопка троеточия не появилась",
                                                               DisplayedMatcher.displayed(), 15);
        Map<String, String> temp = getWorkingDaysTime(scheduleId, dayNumber);
        String startTimeApi = temp.get(START_TIME);
        String endTimeApi = temp.get(END_TIME);
        Allure.addAttachment("Проверка", "Время начала и окончания введенные: " + startTime + " "
                + endTime + " Время начала и окончания отобразившиеся в апи после сохранения изменений: "
                + startTimeApi + " " + endTimeApi);
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(startTime, startTimeApi, "Время начала в api и введенное не совпали");
        softAssert.assertEquals(endTime, endTimeApi, "Время окончания в api и введенное не совпали");
        softAssert.assertAll();
    }

    @Step("Проверить, что тип дня поменялся. Номер дня: {dayNumber}, айди расписания: {scheduleId}, тип дня: {days.nameOfDay}")
    private void switchDayCheck(int dayNumber, String scheduleId, Days days) {
        sb.subdivisionProperties().threeDotsButton().waitUntil("Кнопка троеточия не появилась",
                                                               DisplayedMatcher.displayed(), 15);
        Map<Integer, String> temp = getWorkingDays(scheduleId);
        switch (days) {
            case DAY:
                Assert.assertNotNull(temp.get(dayNumber), "День все еще числиться как выходной");
                break;
            case DAY_OFF:
                Assert.assertNull(temp.get(dayNumber), "День все ещё числиться как рабочий");
        }
        Allure.addAttachment("Проверка", "Тип дня был успешно сменен на: " + days.getNameOfDay());
    }

    @Step("Нажать на кнопку \"Редактировать график работы\"")
    private void clickOnEditScheduleButton() {
        sb.subdivisionProperties().changeScheduleButton().click();
    }

    @Step("Выбрать дату исключения год: {year}, месяц: {monthEnum}, день: {day}")
    private void selectDateOfSpecialDay(int index, LocalDate date) {
        sb.subdivisionProperties().specialDaysCalendarButton(index).click();
        DatePicker datePicker = new DatePicker(sb.datePickerForm());
        datePicker.pickDate(date);
        datePicker.okButtonClick();
    }

    /**
     * @return Возвращает вариант выбора в матпараметре, отличный от текущего
     */
    private VariantsInMathParameters returnVariant(MathParameters matchParameter) {
        String currentStatus = sb.employeeDataMenu().matchParameters(matchParameter.getNameParam()).getAttribute(VALUE);
        VariantsInMathParameters variant;
        do {
            variant = VariantsInMathParameters.getRandomVariant();
        } while (variant.getName().equals(currentStatus));
        return variant;
    }

    @Step("Из выпадающего списка выбрать вариант: {variant.name}")
    private void chooseVariant(VariantsInMathParameters variant) {
        sb.employeeDataMenu().employeeParametersMenu().variantsInMathParam(variant.getName())
                .waitUntil("Блок вариантов матпараметров не отображен", DisplayedMatcher.displayed(), 5);
        LOG.info("Выбран статус: {}", variant.getName());
        sb.employeeDataMenu().employeeParametersMenu().variantsInMathParam(variant.getName()).click();
    }

    @Step("Выбрать время начала исключения, часы: {hour}, минуты {minute}")
    private void selectSpecialDayStartTime(int index, LocalTime time) {
        sb.subdivisionProperties().specialDaysTimeOpenButton(index).click();
        DatePicker datePicker = new DatePicker(sb.timePickerForm());
        datePicker.pickTime(time);
    }

    @Step("Выбрать время конца исключения, часы: {hour}, минуты {minute}")
    private void selectSpecialDayCloseTime(int index, LocalTime time) {
        sb.subdivisionProperties().specialDaysTimeCloseButton(index).click();
        DatePicker datePicker = new DatePicker(sb.timePickerForm());
        datePicker.pickTime(time);
    }

    @Step("Раскрыть список тип дня")
    private void clickOnSelectSpecialDayTypeField(int index) {
        sb.subdivisionProperties().specialDaysSelectTypeField(index).click();
    }

    @Step("Проверить, что было добавлено исключенин с типом {days.nameOfDay} за дату {date}")
    private void checkExceptionAdding(Days days, LocalDate date, String scheduleId) {
        sb.subdivisionProperties().threeDotsButton()
                .waitUntil("кнопка не отобразилась", DisplayedMatcher.displayed(), 15);
        Map<LocalDate, String> temp = getSpecialDays(new DateInterval(date), scheduleId);
        Assert.assertEquals(temp.get(date), days.getKpiBehavior());
    }

    @Step("Проверить, что был добавлен выходной день с датой {date}")
    private void dayOffAddingCheck(String scheduleId, String date) {
        sb.subdivisionProperties().threeDotsButton()
                .waitUntil("Троеточие", DisplayedMatcher.displayed(), 10);
        List<String> temp = getBusinessHoursDaysOff(scheduleId);
        String assertionStr = null;
        try {
            assertionStr = temp.stream().filter(s -> s.equals(date)).findFirst().orElse(null);
            Assert.assertNotNull(assertionStr, "В списке " + temp + " размером " + temp.size() + " нет подходящих значений");
        } catch (NoSuchElementException e) {
            Assert.fail("Выходной не был добавлен");
        }
        Assert.assertNotNull(assertionStr);
    }

    @Step("Выбрать наставника")
    private void mentorSelect() {
        sb.employeeDataMenu().mentorsListOpenButton().click();
        Random random = new Random();
        int size = sb.employeeDataMenu().mentorsList().size();
        int empRnd = random.nextInt(size);
        LOG.info("Выбран наставник с именем: {}", empRnd);
        sb.employeeDataMenu().mentorsList().get(empRnd).click();
    }

    @Step("Проверить добавление стажерской программы для сотрудника {employee}")
    private void addInternProgramCheck(Employee employee) {
        sb.employeeDataMenu().mentorsField()
                .should("Поле для заполнения ментора не отображено", DisplayedMatcher.displayed(), 5);
        boolean needMentor = employee.refreshEmployee().isNeedMentor();
        //Проверка в том что бы взять значение по имени и проверить его если оно true
        // тогда у человека имеется стажерская программа если false то стажерской программы нет
        assertTrue(needMentor,
                   "Стажерская программа сотруднику " + employee.getShortName() + " не добавлена");
    }

    @Step("В раскрывшемся меню подразделения выбрать \"{variantsOfFunctions.backup}\"")
    private void chooseFunction(VariantsOfFunctions variantsOfFunctions) {
        sb.formOrgUnitMenu().waitUntil("Не отобразились сотрудники", DisplayedMatcher.displayed(), 40);
        LOG.info("Перешли на вкладку \"{}\"", variantsOfFunctions.getBackup());
        if (variantsOfFunctions.equals(VariantsOfFunctions.DOWNLOAD_PLANNED_SCHEDULE)) {
            //до тех пор, пока на фронте не унифицируют элементы пунктов в меню троеточия
            sb.formOrgUnitMenu().plannedSchedule()
                    .should("В меню нет опции с плановым графиком", DisplayedMatcher.displayed());
            sb.formOrgUnitMenu().plannedSchedule().click();
        } else {
            sb.formOrgUnitMenu().variantsOfFunctions(variantsOfFunctions.getVariant())
                    .should(OPTION_NOT_DISPLAYED + variantsOfFunctions.getName(), DisplayedMatcher.displayed());
            systemSleep(1); // без этой паузы могут быть выбраны нормы часов
            sb.formOrgUnitMenu().variantsOfFunctions(variantsOfFunctions.getVariant()).click();
        }
    }

    @Step("В раскрывшемся меню проверить, что функции \"{variantsOfFunctions.name}\" в меню нет")
    private void checkFunctionInMenu(VariantsOfFunctions variantsOfFunctions) {
        sb.formOrgUnitMenu().waitUntil("Не отобразились сотрудники", DisplayedMatcher.displayed(), 40);
        String nameFunctions = variantsOfFunctions.getBackup();
        String functions = sb.formOrgUnitMenu().allVariantsOfFunctions()
                .stream()
                .map(f -> f.getText())
                .filter(t -> t.equals(nameFunctions))
                .findFirst().orElse(null);
        if (Objects.isNull(functions)) {
            LOG.info("В меню не отобразилась функция \"{}\"", nameFunctions);
            Allure.addAttachment("Проверка", "В меню не отобразилась функция \"{}\"", nameFunctions);
        } else {
            LOG.info("В меню отобразилась функция \"{}\"", nameFunctions);
            Allure.addAttachment("Проверка", "В меню отобразилась функция \"{}\"", nameFunctions);
            throw new AssertionError("В меню отобразилась функция " + functions);
        }
    }

    @Step("Кликнуть на кнопку меню \"Выбор месяца\" при публикации смен")
    private void clickPublicationShiftsCalendarButton() {
        sb.formPublishForm().waitUntil("Форма не загрузилась", DisplayedMatcher.displayed(), 10);
        sb.formPublishForm().buttonPublishCalendar().click();
    }

    @Step("Проверить что в поле даты остался прошлый период")
    private void assertDeselectionPublicationPeriod(String periodBefore, String periodAfter) {
        Assert.assertEquals(periodBefore, periodAfter, "Периоды не совпали");
        Allure.addAttachment("Проверка",
                             "До и после выбора месяца и нажатия на кнопку \"Отмена\" были периоды " + periodBefore + " " + periodAfter);
    }

    @Step("Ввести произвольное(ые) значение в текстовое поле {value} дня(ей) месяца")
    private void enterCommentValue(int value) {
        sb.formCommentsForm()
                .waitUntil("Форма комментариев не отобразилась", DisplayedMatcher.displayed(), 10);
        List<AtlasWebElement> freeCommentsFields = sb.formCommentsForm().inputFieldCommentsDay();
        List<String> comments = new ArrayList<>();
        for (int i = 0; i < value; i++) {
            AtlasWebElement temp = getRandomFromList(freeCommentsFields);
            String randomText = RandomStringUtils.randomAlphabetic(15);
            temp.click();
            temp.sendKeys(randomText);
            freeCommentsFields.remove(temp);
            comments.add(randomText);
        }
        LOG.info("Ввели комментарии: {}", comments);
        Allure.addAttachment("Введенные комментарии", "Были введены комментарии: " + comments);
    }

    @Step("Ввести произвольное произвольный текст в текстовое поле {number} версии(й)")
    private void enterRosterComments(int number) {
        sb.formCommentsForm()
                .waitUntil("Форма комментариев не отобразилась", DisplayedMatcher.displayed(), 10);
        List<String> temp = new ArrayList<>();
        for (int i = 0; i < number; i++) {
            String randomText = RandomStringUtils.randomAlphabetic(10);
            sb.formCommentsForm().inputFieldCommentsRosters().get(1).sendKeys(randomText);
            temp.add(randomText);
        }
        if (temp.size() > 1) {
            Allure.addAttachment("Введеннные коментарии", "Был введено " + temp.size() + " комментария: " + temp);
        } else if (temp.size() == 1) {
            Allure.addAttachment("Введеннный коментарий", "Был введен " + temp.size() + " комментарий: " + temp);
        }
    }

    @Step("Нажать на иконку кнопки \"Очистить\" рядом с заполненным текстовым полем c номером {number}")
    private void deleteComment(int number, boolean cleanDaysComment) {
        sb.formCommentsForm()
                .waitUntil("Форма комментариев не отобразилась", DisplayedMatcher.displayed(), 10);
        if (cleanDaysComment) {
            sb.formCommentsForm().inputFieldCommentDay(number).click();
            sb.formCommentsForm().cleanCommentDayButton(number).click();
        } else {
            sb.formCommentsForm().cleanVersionButton(number + 1).click();
        }
    }

    @Step("Нажать на текстовое поле комментария с порядковым номером с индексом в списке: {dayNumber}")
    private void clickCommentFieldDay(int dayNumber) {
        sb.formCommentsForm()
                .waitUntil("Форма комментариев не отобразилась", DisplayedMatcher.displayed(), 10);
        sb.formCommentsForm().inputFieldCommentDay(dayNumber).click();
    }

    @Step("Очистить текстовое поле комментария с порядковым номером в списке: {dayNumber}")
    private void cleanCommentFieldDay(int dayNumber) {
        sb.formCommentsForm().inputFieldCommentDay(dayNumber).clear();
    }

    @Step("Ввести значение: {text} в текстовое поле комментария с порядковым номером в списке: {dayNumber}")
    private void editCommentDay(String text, int dayNumber) {
        sb.formCommentsForm().inputFieldCommentDay(dayNumber).sendKeys(text);
    }

    @Step("Нажать на текстовое поле комментария у версии ростера: {version}")
    private void clickCommentFieldRoster(int version) {
        sb.formCommentsForm()
                .waitUntil("Форма комментариев не отобразилась", DisplayedMatcher.displayed(), 10);
        sb.formCommentsForm().inputFieldCommentRoster(version).click();
    }

    private int getIndexRosterComment(String version) {
        List<AtlasWebElement> temp = sb.formCommentsForm().rosterVersionsList();
        int index = 0;
        for (int i = 0; i < temp.size(); i++) {
            if (temp.get(i).getAttribute(VALUE).equals(version)) {
                index = i;
            }
        }
        return index + 1;
    }

    @Step("Очистить текстовое поле комментария у версии ростера: {version}")
    private void cleanCommentRoster(int version) {
        sb.formCommentsForm().inputFieldCommentRoster(version).clear();
    }

    @Step("Ввести значение: {text} в текстовое поле комментария у версии ростера: {version}")
    private void editCommentRoster(String text, int version) {
        sb.formCommentsForm().inputFieldCommentRoster(version).sendKeys(text);
    }

    @Step("Проверить изменение комментария, предыдущий комментарий: {lastComment}, новый комментарий: {newComment}, дата дня: {date}")
    private void commentEditionCheck(OrgUnit orgUnit, String lastComment, String newComment, LocalDate date) {
        Map<String, String> temp = PresetClass.getDayCommentPreset(new DateInterval(date), orgUnit);
        Allure.addAttachment("комментарий к дню", "Был успешно изменен комментарий с датой: "
                + date + ", c: " + lastComment + ", на: " + newComment);
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(temp.get("text"), newComment, "Комментарий не совпал");
        softAssert.assertEquals(temp.get(DATE), date.toString(), "Дата комментария не совпала");
        softAssert.assertNotEquals(lastComment, temp.get("text"), "Последний комментарий такой же как был до этого");
        softAssert.assertAll();
    }

    @Step("Проверить добавление {addSize} комментария(-ев)")
    private void checkCommentsCreation(int addSize, List<String> previouslyList, LocalDate dateFrom, LocalDate dateTo) {
        int omNumber = getOrgIdFromUrl();
        String urlEnding = makePath(ORGANIZATION_UNITS, omNumber, WORKING_SCHEDULE_DAYS, COMMENTS);
        List<NameValuePair> pairs = Pairs.newBuilder()
                .from(dateFrom)
                .to(dateTo)
                .size(LocalDateTools.getLastDate().getDayOfMonth()).build();
        JSONObject someObject = getJsonFromUri(Projects.WFM, URL_SB, urlEnding, pairs);
        JSONArray eventsList = someObject.getJSONObject(EMBEDDED).getJSONArray(COMMENTS);
        int currentSize = eventsList.length();
        List<String> newComments = new ArrayList<>();
        for (int i = 0; i < currentSize; i++) {
            JSONObject temp = eventsList.getJSONObject(i);
            newComments.add(temp.getString("text"));
        }
        Allure.addAttachment("Проверка", "В оргЮните до добавления были комментарии:"
                + previouslyList + " После добавления стали: " + newComments);
        newComments.removeAll(previouslyList);
        Allure.addAttachment("Добавленные", "были добавлены" + newComments);
        Assert.assertEquals(currentSize, previouslyList.size() + addSize);
    }

    @Step("Проверить удаление комментария, комментарий: {lastComment}, дата дня: {date}")
    private void commentDeletionCheck(OrgUnit orgUnit, String lastComment, LocalDate date) {
        Allure.addAttachment("комментарий", "Был успешно удален комментарий с датой: "
                + date + ", комментарий: " + lastComment);
        assertTrue(getEmptyDayCommentNotExistStatus(orgUnit, date));
    }

    @Step("Нажать кнопку \"Применить\"")
    private void filterModeApplyButtonClick() {
        sb.employeesFilterMode().applyButton().click();
        sb.employeesFilterMode()
                .waitUntil("Форма комментариев все еще отображена",
                           Matchers.not(DisplayedMatcher.displayed()), 5);
        LOG.info("Нажимаем кнопку \"Применить\"");
    }

    @Step("Нажать кнопку закрытия формы фильтров по сотрудникам")
    private void closeFilterModeButtonClick() {
        sb.employeesFilterMode().closeFilterModeButton().click();
        sb.employeesFilterMode()
                .waitUntil("Форма комментариев все еще отображена",
                           Matchers.not(DisplayedMatcher.displayed()), 5);
    }

    @Step("Выбрать тип персонала позиций {personnelType} из списка фильтров сотрудников")
    private void pickPersonnelTypeFromEmployeeFilter(String personnelType) {
        sb.employeesFilterMode().personnelTypeChevron().click();
        sb.employeesFilterMode().personnelTypeButton(personnelType).click();
        LOG.info("Выбираем тип персонала позиций \"{}\" из списка фильтров сотрудников", personnelType);
    }

    @Step("Проверить использование фильтра сотрудников.")
    private void checkFilterMode(List<String> activeList, int omId) {
        List<String> employeesLayOut = sb.formLayout().allEmployeeNameButtons()
                .stream().map(WebElement::getText).collect(Collectors.toList());
        if (!URL_BASE.contains("magnit")) {
            // Проверить, есть ли в списке в расписании совместители, записанные в формате "Фамилия И.О.". Если есть, заменить их на "Фамилия Имя"
            for (String emp : employeesLayOut) {
                if (emp.contains(".")) {
                    String replacement = EmployeeRepository.getEmployeesFromOM(omId).stream()
                            .filter(e -> e.getLastNameInitials().equals(emp)).map(Employee::getShortName)
                            .collect(Collectors.toList()).get(0);
                    Collections.replaceAll(employeesLayOut, emp, replacement);
                }
            }
        }
        Allure.addAttachment("Проверка", "Сотрудники, оставшиеся после фильтрации, и на главной странице совпали");
        Collections.sort(employeesLayOut);
        Collections.sort(activeList);
        Assert.assertEquals(employeesLayOut, activeList, "Сотрудники не совпали после фильтрации");
    }

    @Step("Проверить применение фильтра сотрудников и сравнить UI с API")
    private void checkFilterModeAndCompareWithApi(List<String> activeList, int omId, List<String> apiPositions, boolean checkOutStaff) {
        checkFilterMode(activeList, omId);
        SoftAssert softAssert = new SoftAssert();
        if (checkOutStaff) {
            List<String> outStaffPositions = sb.formLayout().outStaffPositions()
                    .stream().map(WebElement::getText).collect(Collectors.toList());
            int rosterId = RosterRepository.getActiveRosterThisMonth(omId).getId();
            List<Shift> outStaffPositionFromApi = ShiftRepository.getShiftsForRoster(rosterId, new DateInterval(LocalDateTools.getFirstDate(), LocalDateTools.getLastDate()))
                    .stream()
                    .filter(s -> s.getExchangeStatus().equals(BID_CREATED))
                    .collect(Collectors.toList());
            //у Пятерочки все аутстафовые смены записываются в одну строку ("Продавец"), поэтому, если такие смены есть, то список аутстафовых строк должен иметь один элемент.
            if (!outStaffPositionFromApi.isEmpty()) {
                softAssert.assertEquals(outStaffPositions.size(), 1, "Размеры списков смен для аутстафф из API и UI не совпали");
            }
        }
        Collections.sort(activeList);
        Collections.sort(apiPositions);
        softAssert.assertEquals(activeList, apiPositions, "Список сотрудников на UI не совпадает со списком в API");
        softAssert.assertAll();
        Allure.addAttachment("Проверка", "Сотрудники, оставшиеся после фильтрации в меню фильтрации, на главной странице и в API, совпали");
    }

    @Step("Проверить изменение комментария c {oldComment} на {newComment} у {version} версии ростера")
    private void rosterCommentEditionCheck(String oldComment, String newComment, String rosterId, String version) {
        Map<String, String> temp = PresetClass.rosterCommentCheck(Integer.parseInt(rosterId), CommentValue.LOOK_AT);
        Allure.addAttachment("Изменение комментария", "У версии " + version
                + " графика был изменен комментарий с " + oldComment + " на " + newComment);
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(temp.get(COMMENT), newComment, "Комментарий не изменился");
        softAssert.assertNotEquals(oldComment, temp.get(COMMENT), "Комментарий остался старым");
        softAssert.assertAll();
    }

    @Step("Проверить удаление комментария \"{oldComment}\" у {version} версии ростера")
    private void rosterCommentDeletionCheck(String oldComment, String rosterId, String version) {
        Map<String, String> temp = PresetClass.rosterCommentCheck(Integer.parseInt(rosterId), CommentValue.LOOK_AT);
        Allure.addAttachment("Удаление комментария", "У версии " + version
                + " графика был удален комментарий " + oldComment);
        Assert.assertEquals(temp.get(COMMENT), "");
    }

    @Step("Нажать на шеврон раскрытия \"{infoNames.nameOfInformation}\"")
    private void clickOnChevronButton(OmInfoName infoNames) {
        //без этого ожидания не срабатывает клик на шеврон
        systemSleep(1);
        sb.subdivisionProperties().showButton(infoNames.getNamesOfInformation())
                .waitUntil("", DisplayedMatcher.displayed(), 10);
        sb.subdivisionProperties().showButton(infoNames.getNamesOfInformation()).click();
    }

    @Step("Ввести {value} в поле \"Количество человек\"")
    private void enterValueOfPeople(int value) {
        sb.eventForm().peopleValueField()
                .waitUntil("Поле ввода количества человек не отобразилось", DisplayedMatcher.displayed(), 10);
        sb.eventForm().peopleValueField().sendKeys(String.valueOf(value));
    }

    @Step("Очистить поле \"Количество человек\"")
    private void clearValueOfPeople() {
        sb.eventForm().peopleValueField()
                .waitUntil("Поле ввода количества человек не отобразилось", DisplayedMatcher.displayed(), 10);
        sb.eventForm().peopleValueField().clear();
    }

    @Step("В поле {dateType.name} выбрать дату: {date}")
    private void enterEventDateEndOrStart(LocalDate date, DateTypeField dateType) {
        sb.eventForm().dateStartOrEndField(dateType.getName()).clear();
        sb.eventForm().dateStartOrEndField(dateType.getName()).sendKeys(date.format(UI_DOTS.getFormat()));
        LOG.info("В поле \"{}\" ввели дату {}", dateType.getName(), date);
    }

    @Step("В поле \"{dateType.name}\" выбрать время {time}")
    private void enterEventTimeEndOrStart(String time, DateTypeField dateType) {
        sb.eventForm().timeEndOrStartField(dateType.getName()).clear();
        sb.eventForm().timeEndOrStartField(dateType.getName()).sendKeys(time);
    }

    @Step("Проверить добавление {value} комментария(-ев)")
    private void checkRosterCommentsAdding(OrgUnit orgUnit, int value, int previouslySize) {
        int omNumber = orgUnit.getId();
        List<Roster> rostersWithComments = RosterRepository.getRosters(omNumber).stream()
                .filter(roster ->
                        {
                            String description = roster.getDescription();
                            return description != null && !description.equals("");
                        }
                )
                .collect(Collectors.toList());
        Allure.addAttachment("После добавления", "Комментарии у ростеров " + rostersWithComments.stream()
                .map(Roster::getDescription).collect(Collectors.joining(", ")));
        Assert.assertEquals(rostersWithComments.size(), previouslySize + value);
    }

    @Step("Из раскрывающегося списка \"Тип\" выбрать {type.name}")
    private void selectEventType(EventType type) {
        sb.eventForm().waitUntil("Форма создания события не отображена", DisplayedMatcher.displayed(), 10);
        sb.eventForm().selectEventTypeButton().click();
        sb.eventForm().eventTypeButton(type.getName())
                .waitUntil("Кнопка выбранного типа не отображена", DisplayedMatcher.displayed(), 5);
        sb.eventForm().eventTypeButton(type.getName()).click();
    }

    @Step("Из раскрывающегося списка \"Периодичность\" выбрать {repeatType.repeatType}")
    private void selectEventRepeatType(Periodicity repeatType) {
        sb.eventForm().selectEventRepeatButton().click();
        sb.eventForm().eventRepeatButton(repeatType.getRepeatType())
                .waitUntil("Кнопка периодичности события не отображена", DisplayedMatcher.displayed(), 10);
        sb.eventForm().eventRepeatButton(repeatType.getRepeatType()).click();
    }

    @Step("В поле \"Дата окончания повтора\" выбрать дату: {date}")
    private void sendDateEndRepeat(LocalDate date) {
        sb.eventForm().dateRepeatEndField().waitUntil("Поле ввода даты окончания повтора не отобразилось",
                                                      DisplayedMatcher.displayed(), 5);
        sb.eventForm().dateRepeatEndField().clear();
        sb.eventForm().dateRepeatEndField().sendKeys(date.format(UI_DOTS.getFormat()));
    }

    @Step("Нажать на кнопку \"Создать\"")
    private void clickCreateEventButton() {
        sb.eventForm().createButton().click();
        sb.formLayout().popUpForCreateEvent()
                .waitUntil("Поп-ап \"Событие создано\" не отобразился", DisplayedMatcher.displayed(), 15);
        sb.formLayout().popUpForCreateEvent()
                .waitUntil("Поп-ап \"Событие создано\" все еще отображается",
                           Matchers.not(DisplayedMatcher.displayed()), 15);
        sb.spinnerLoader().loadingSpinnerPage().waitUntil("Спиннер все еще отображается",
                                                          Matchers.not(DisplayedMatcher.displayed()), 10);
    }

    @Step("Нажать на зеленую точку эвента в {eventDate}")
    private void clickOnRandomEventPoint(LocalDate eventDate) {
        sb.formLayout().allEventElements().waitUntil(
                Matchers.hasSize(Matchers.greaterThanOrEqualTo(eventDate.with(TemporalAdjusters.lastDayOfMonth()).getDayOfMonth() - 1)));
        sb.formLayout().allEventElements().get(eventDate.getDayOfMonth() - 1).click();
    }

    @Step("Нажать на кнопку \"Изменить\" нужного эвента")
    private void clickOnChangeEvent(LocalDate eventDate, boolean repeatableEvent) {
        sb.formLayout().eventChangeButton().waitUntil(Matchers.hasSize(Matchers.greaterThan(0)));
        if (sb.formLayout().eventChangeButton().size() > 1) {
            for (int i = 0; i < sb.formLayout().eventChangeButton().size(); i++) {
                sb.formLayout().eventChangeButton().get(i).click();
                if (!repeatableEvent && !sb.eventForm().dateRepeatEndField().isDisplayed()) {
                    break;
                } else if (repeatableEvent && sb.eventForm().dateRepeatEndField().isDisplayed()) {
                    break;
                } else {
                    sb.eventForm().closeEventForm().click();
                    clickOnRandomEventPoint(eventDate);
                    sb.formLayout().eventChangeButton().waitUntil(Matchers.hasSize(Matchers.greaterThan(0)));
                }
            }
        } else {
            sb.formLayout().eventChangeButton().get(0).click();
        }
    }

    @Step("Нажать на кнопку \"Изменить\" в форме эвента")
    private void clickOnChangeEventPoint() {
        sb.eventForm().changeButton().click();
    }

    @Step("Выбрать радиобатон \"Изменить серию\" в форме изменения эвента")
    private void clickOnRadioButtonInChangeForm() {
        sb.eventForm().radioButtonChange("Изменить серию")
                .waitUntil("Радиобаттон не отобразился", DisplayedMatcher.displayed(), 10);
        sb.eventForm().radioButtonChange("Изменить серию").click();
    }

    @Step("Нажать на кнопку \"Изменить\" в форме изменения эвента")
    private void clickOnChangeInChangeForm() {
        sb.eventForm().changeButtonInChange()
                .waitUntil("Кнопка\"Изменить\" не отобразилась", DisplayedMatcher.displayed(), 10);
        sb.eventForm().changeButtonInChange().click();
    }

    private Employee getEmployeeWithoutEndWorkDate() {
        List<EmployeePosition> employeePositions = EmployeePositionRepository.getEmployeePositions(getOrgIdFromUrl());
        sb.formLayout().employeeNameButtons().waitUntil(Matchers.hasSize(Matchers.greaterThan(1)));
        List<String> nameButtons = sb.formLayout().allEmployeeNameButtons()
                .stream().map(WebElement::getText).collect(Collectors.toList());
        List<String> employees = new ArrayList<>();
        for (String value : nameButtons) {
            if (value.contains(".")) {
                employees.add(value.substring(0, value.indexOf(".")));
            } else {
                employees.add(value);
            }
        }
        return employeePositions.stream()
                .filter(k ->
                        {
                            String resultOne = k.getEmployee().getShortName()
                                    .substring(0, k.getEmployee().getShortName().indexOf(" ") + 2);
                            boolean flagOne = employees.contains(resultOne);
                            String resultSecond = k.getEmployee().getShortName();
                            boolean flagSecond = employees.contains(resultSecond);
                            return flagOne || flagSecond;
                        }
                )
                .filter(k -> k.getDateInterval().endDate == null)
                .findAny()
                .orElseThrow(() -> new AssertionError("schedule message. Нет сотрудника без даты окончания работы"))
                .getEmployee();
    }

    @Step("Проверить, что было добавлено событие с датой {date} и временем {startTime} - {endTime}, количеством человек {value}, типом {type}")
    private void checkEventAdding(List<OrganizationUnitEvent> before, List<OrganizationUnitEvent> after, LocalDate date, int value, EventType type, LocalTime startTime, LocalTime endTime) {
        after.removeAll(before);
        OrganizationUnitEvent addedEvent = after.get(0);
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(addedEvent.getDateTimeInterval().getStartDateTime().toLocalTime(), startTime);
        softAssert.assertEquals(addedEvent.getDateTimeInterval().getEndDateTime().toLocalTime(), endTime);
        softAssert.assertEquals(addedEvent.getEventTypeId(), type.getId());
        softAssert.assertEquals(addedEvent.getDate().toString(), date.toString());
        softAssert.assertEquals(addedEvent.getValue(), (double) value);
        softAssert.assertAll();
        Allure.addAttachment("Добавление событие",
                             "Событие успешно добавлено с датой: " + date +
                                     ", временем: " + startTime + " - " + endTime +
                                     ", количеством человек: " + value + ", типом: " + type);
    }

    @Step("Проверить, что было добавлено событие с датой {date}, количеством человек {value} и типом: {type}")
    private void checkRepeatEventAdding
            (List<OrganizationUnitEvent> before, List<OrganizationUnitEvent> after, LocalDate date, LocalDate endRepeat,
             int value, Periodicity type) {
        after.removeAll(before);
        assertTrue(after.size() > 0, "Добавленное событие не отображается в апи");
        OrganizationUnitEvent unitEvent = after.get(0);
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(unitEvent.getDate(), date, "Дата события не соответствует введенной");
        softAssert.assertEquals(unitEvent.getValue(), (double) value,
                                "Количество человек в событии не соответствует введенному");
        softAssert.assertEquals(unitEvent.getRepeatRule().getPeriodicity(), type, "Тип периодичности не соответствует заданному");
        softAssert.assertEquals(unitEvent.getRepeatRule().getEndDate(), endRepeat, "Дата окончания повтора не соответствует введенной");
        softAssert.assertAll();
        Allure.addAttachment("Добавление повторяющегося события",
                             "Повторяющееся событие успешно добавлено с датой: " + date.toString() +
                                     ", количеством человек: " + value + ",  с типом повтора: " + type +
                                     " , датой окончания повтора " + endRepeat);
    }

    @Step("Проверить изменение события: дата({date}), количество человек ({value})")
    private void checkNonRepeatEventChanging(List<OrganizationUnitEvent> before, LocalDate date, int value, int orgUnitId) {
        sb.formLayout().popUpForEditEvent()
                .should("Поп-ап \"Событие изменено\" не отобразился", DisplayedMatcher.displayed(), 10);
        sb.formLayout().popUpForEditEvent()
                .should("Поп-ап \"Событие изменено\" все еще отображается",
                        Matchers.not(DisplayedMatcher.displayed()), 10);
        sb.spinnerLoader().loadingSpinnerPage().should("Спиннер загрузки все еще отображается", Matchers.not(DisplayedMatcher.displayed()), 10);
        List<OrganizationUnitEvent> after = OrganizationUnitEventRepository.getOrganizationUnitEvents(date, date, orgUnitId);
        after.removeIf(e -> e.getRepeatRule() != null);
        after.removeAll(before);
        OrganizationUnitEvent addedEvent = after.get(0);
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(after.size(), 1, "Количество измененных эвентов не совпадет");
        softAssert.assertEquals(addedEvent.getDate(), date,
                                "Дата события не совпала с датой, на которую мы изменили");
        softAssert.assertEquals(addedEvent.getValue(), (double) value,
                                "Количество людей в событии не совпало с тем, на которое мы изменяли");
        softAssert.assertAll();
        Allure.addAttachment("Изменения события",
                             "Событие успешно изменено с датой: " + date.toString() +
                                     ", количеством человек: " + value);
    }

    @Step("Кликнуть на блок запроса типа \"{request.title}\" за {request.dateTimeInterval.startDateTime.date}")
    private void clickOnRequestBlock(ScheduleRequest request, ScheduleWorker scheduleWorker, OrgUnit unit) {
        String name = request.getEmployee().getShortName();
        LocalDate date = request.getDateTimeInterval().getStartDate();
        LOG.info("Кликаем на запрос для {} за {}", name, date);
        Allure.addAttachment("Запрос",
                             String.format("Был произведен клик на запрос за %s у сотрудника с именем %s",
                                           request.getDateTimeInterval().getStartDate(), request.getEmployee()));
        EmployeePosition ep = EmployeePositionRepository.getEmployeeByScheduleRequest(request, unit);
        scheduleWorker.getScheduleRequestElement(ep, date).waitUntil("Элемент блока запроса не прогрузился", DisplayedMatcher.displayed(), 15);
        AtlasWebElement element = scheduleWorker.getScheduleRequestElement(ep, date);
        Assert.assertNotNull(element, String.format("Не найден элемент запроса для сотрудника %s за %s", ep, date));
        element.click();
    }

    @Step("В раскрывшемся меню выбрать \"{action.action}\"")
    private void selectAction(RequestAction action, boolean forSeries) {
        sb.formListOfRequest().typeButtons(action.getAction())
                .waitUntil(SHIFT_ACTION_OPTION_NOT_DISPLAYED, DisplayedMatcher.displayed(), 10);
        LOG.info("В раскрывшемся меню выбран вариант: {}", sb.formListOfRequest().typeButtons(action.getAction()).getText());
        sb.formListOfRequest().typeButtons(action.getAction()).click();
        try {
            sb.correctionConfirmationDialog().confirmationButton().click();
        } catch (org.openqa.selenium.NoSuchElementException ex) {
            LOG.info("Система не запросила подтверждения корректировки");
        }
        if (!forSeries) {
            sb.formListOfRequest().waitUntil("Форма редактирования запроса не исчезла",
                                             Matchers.not(DisplayedMatcher.displayed()), 15);
        }
    }

    @Step("Активировать радио-кнопку \"Удалить запрос\"")
    private void clickDeleteRequestRadioButton() {
        sb.formListOfRequest().deleteRequestRadioButton().click();
    }

    @Step("Активировать радио-кнопку \"Удалить серию\"")
    private void clickRepeatDeleteRequestRadioButton() {
        sb.formListOfRequest().deleteAllRequestRadioButton()
                .waitUntil("Радио-кнопка удалить серию не загрузилась", DisplayedMatcher.displayed(), 10);
        sb.formListOfRequest().deleteAllRequestRadioButton().click();
    }

    @Step("Нажать на кнопку \"Удалить\"")
    private void requestConfirmDeleteButtonClick() {
        sb.formListOfRequest().deleteButton().click();
    }

    @Step("Нажать на значок троеточия")
    private void listOfRequestsThreeDotsButtonClick() {
        sb.formListOfRequest().threeDotsButton().waitUntil(THREE_DOTS_BUTTON_NOT_DISPLAYED_ON_SHIFT_EDIT_SCREEN, DisplayedMatcher.displayed());
        sb.formListOfRequest().threeDotsButton().click();
        LOG.info("Нажимаем на значок троеточия");
    }

    @Step("Нажать на кнопку меню выбора разделов (три параллельные линии)")
    private void clickSectionSelectionMenuOnPageHeader() {
        sb.commonHeader().
                waitUntil("Страница не прогрузилась", DisplayedMatcher.displayed(), 10);
        sb.commonHeader().sectionSelectionMenu().
                waitUntil("Кнопка выбора разделов не найдена", DisplayedMatcher.displayed(), 10);
        new Actions(sb.getWrappedDriver()).moveToElement(sb.commonHeader().sectionSelectionMenu());
        sb.commonHeader().sectionSelectionMenu().click();
    }

    @Step("Нажать на \"Расписание\"")
    private void clickOnScheduleSectionButton() {
        sb.moduleButton("schedule-board").click();
    }

    @Step("Проверить, что был осуществлен переход на вкладку \"Расписание\", отображается иконка \"Подразделение\"")
    private void checkGoToSchedulePage() {
        sb.mainHeader().headerText().waitUntil("Название разделе в шапке не совпало с ожидаемым", TextMatcher.text(Matchers.containsString("Расписание")));
        assertTrue(sb.getWrappedDriver().getCurrentUrl().endsWith("/schedule-board"),
                   "Окончание url адреса не совпало с \"schedule-board\"");
        sb.formTopBar().storeSelectButton().should("Иконка \"Подразделение\" не отобразилась", DisplayedMatcher.displayed());
    }

    @Step("Нажать на иконку \"Подразделение\"")
    private void clickOnSelectStoreButton() {
        sb.formTopBar().storeSelectButton().
                waitUntil("Иконка \"Подразделение\" не найдена", DisplayedMatcher.displayed(), 10);
        new Actions(sb.getWrappedDriver()).moveToElement(sb.formTopBar().storeSelectButton()).perform();
        sb.formTopBar().storeSelectButton().click();
    }

    @Step("Вписать название поздразделения \"{omName}\"")
    private void enterOmName(String omName) {
        sb.formTopBar().searchOrgUnitInput().click();
        slowSendKeys(sb.formTopBar().searchOrgUnitInput(), omName);
    }

    @Step("Выбрать поздразделение \"{omName}\" из списка")
    private void selectStoreFromList(String omName) {
        sb.formTopBar().dropDownList().stream().filter(extendedWebElement -> (extendedWebElement.getText().contains(omName)))
                .findFirst().orElseThrow(() -> new AssertionError("Не удалось найти ом в списки с именем " + omName)).click();
    }

    @Step("Проверить, что был осуществлен переход  к расписанию подразделения \"{orgUnitName}\"")
    private void checkTransitionToOrgUnit(String orgUnitName) {
        sb.formTopBar().buttonOrgUnitMenu().waitUntil("Кнопка меню не отобразилась", DisplayedMatcher.displayed(), 15);
        sb.mainHeader().headerText().should("Раздел расписания выбранного ОМ не загрузился",
                                            TextMatcher.text(Matchers.containsString("Расписание: " + orgUnitName)));
        Allure.addAttachment("Осуществлен переход к расписанию ОЮ:" + orgUnitName, orgUnitName);
    }

    @Step("Получить имя рандомного подразделения из списка")
    private String getRandomOrgUnitFromList() {
        List<AtlasWebElement> stores = sb.formTopBar().storesList()
                .waitUntil(Matchers.hasSize(Matchers.greaterThan(0)));
        return getRandomFromList(stores).getText();
    }

    @Step("Проверить изменения в серии событий: дата ({eventStart}) и количество человек ({value})")
    private void checkRepeatEventChanging(List<OrganizationUnitEvent> before, LocalDate eventStart,
                                          LocalDate endRepeat, int value, int orgUnitId) {
        sb.formLayout().popUpForEditEvent()
                .should("Поп-ап \"Событие изменено\" не отобразился", DisplayedMatcher.displayed(), 15);
        sb.formLayout().popUpForEditEvent()
                .waitUntil("Поп-ап \"Событие изменено\" не исчезло", Matchers.not(DisplayedMatcher.displayed()), 5);
        sb.spinnerLoader().loadingSpinnerPage().waitUntil("Спиннер все еще на месте", Matchers.not(DisplayedMatcher.displayed()), 10);
        List<OrganizationUnitEvent> after = OrganizationUnitEventRepository.getOrganizationUnitEvents(eventStart, endRepeat, orgUnitId);
        after.removeAll(before);
        List<OrganizationUnitEvent> changedEvents = after.stream().filter(oue -> oue.getRepeatRule() != null
                        && oue.getValue() == value
                        && oue.getRepeatRule().getEndDate().equals(endRepeat))
                .collect(Collectors.toList());
        Periodicity periodicity = changedEvents.get(0).getRepeatRule().getPeriodicity();
        int expectedEvents = (Math.toIntExact(eventStart.until(endRepeat, ChronoUnit.DAYS)) / periodicity.getRepeatEveryValues()) + 1;
        Assert.assertEquals(changedEvents.size(), expectedEvents, "В апи событие не повторяется два раза");
        Allure.addAttachment("Изменение повторяющегося события",
                             "Повторяющееся событие успешно изменено с датой окончания эвента: " + endRepeat +
                                     " c количеством человек: " + value);
    }

    @Step("Проверить действие с запросом {newStatus}")
    private void assertRequestActionChange(ScheduleRequest request, ScheduleRequestStatus newStatus, int omId) {
        Assert.assertEquals(newStatus, request.updateScheduleRequest(omId).getStatus());
        Allure.addAttachment("Запрос", "Запрос был успешно " + newStatus);
    }

    @Step("Проверить удаление запроса")
    private void assertRequestDeleting(ScheduleRequest request, ScheduleWorker scheduleWorker, OrgUnit unit, LocalDate... dates) {
        SoftAssert softAssert = new SoftAssert();
        EmployeePosition ep = EmployeePositionRepository.getEmployeeByScheduleRequest(request, unit);
        if (dates.length > 1) {
            changeStepName("Проверить удаление серии запросов у сотрудника");
            for (LocalDate date : dates) {
                ElementsCollection<AtlasWebElement> elements = scheduleWorker.getScheduleRequestElements(ep, date);
                softAssert.assertTrue(elements.isEmpty(), "Запрос не был удален на UI");
            }
        } else {
            ElementsCollection<AtlasWebElement> elements = scheduleWorker.getScheduleRequestElements(ep, request.getDateTimeInterval().getStartDate());
            softAssert.assertTrue(elements.isEmpty(), "Запрос не был удален на UI");
        }
        List<ScheduleRequest> requestAfter = ScheduleRequestRepository.getEmployeeScheduleRequests(request.getEmployee().getId(),
                                                                                                   request.getDateTimeInterval().toDateInterval(), unit.getId()).stream()
                .filter(req -> req.getDateTimeInterval().equals(request.getDateTimeInterval()))
                .collect(Collectors.toList());
        softAssert.assertEquals(requestAfter.size(), 0, "Запрос не был удален");
        softAssert.assertAll();
    }

    @Step("Проверить изменение запроса")
    private void assertRequestChange(ScheduleRequest request, LocalDate newDate, ScheduleWorker scheduleWorker, OrgUnit unit, ScheduleRequestStatus status) {
        sb.formEditForm().waitUntil("Форма редактирования все еще отображается",
                                    Matchers.not(DisplayedMatcher.displayed()), 10);
        ScheduleRequest newRequest = request.updateScheduleRequest(unit.getId());
        Assert.assertNotNull(newRequest, "Обновленный запрос не найден.");
        SoftAssert softAssert = new SoftAssert();
        LocalDate startDate = newRequest.getDateTimeInterval().getStartDate();
        softAssert.assertEquals(startDate, newDate);
        EmployeePosition ep = EmployeePositionRepository.getEmployeeByScheduleRequest(request, unit);
        if (request.getDateTimeInterval().getStartDate().getMonthValue() == newDate.getMonthValue()) {
            softAssert.assertNotNull(scheduleWorker.getScheduleRequestElement(ep, startDate),
                                     "Запрос не был добавлен на UI");
        }
        softAssert.assertAll();
    }

    @Step("В поле \"{dateType.name}\" ввести дату {date}")
    private void enterCreateScheduleDateEndOrStart(LocalDate date, DateTypeField dateType) {
        LOG.info("Вводим дату {} В поле \"{}\"", date, dateType);
        sb.formSetResetTimetable()
                .waitUntil("Форма расчета/перерасчета не была отображена", DisplayedMatcher.displayed(), 5);
        sb.formSetResetTimetable().dateStartOrEndInput(dateType.getName()).clear();
        sb.formSetResetTimetable().dateStartOrEndInput(dateType.getName())
                .sendKeys(date.format(Format.UI_DOTS.getFormat()));
    }

    private LocalDate findStartDate() {
        List<AtlasWebElement> endDates = sb.selectScheduleForm().allEndDates();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        List<LocalDate> dates = endDates.stream().map(e -> e.getAttribute(VALUE))
                .map(ds -> LocalDate.parse(ds, formatter))
                .sorted(LocalDate::compareTo)
                .collect(Collectors.toCollection(ArrayList::new));
        return dates.get(dates.size() - 1).plusMonths(2);
    }

    @Step("Активировать чекбокс \"С минимальным отклонением\"")
    private void activateElementCheckbox() {
        sb.formSetResetTimetable().elementCheckbox().click();
    }

    private void calculateButtonClick() {
        clickCalculationButton();
        confirmShiftCalculation();
        sb.spinnerLoader().loadingSpinnerPage().waitUntil("Расчет не завершился", Matchers.not(DisplayedMatcher.displayed()), 100);
        sb.formPostSRsDialog().resultMessage().should("В процессе расчета возникла ошибка: " + sb.formPostSRsDialog().getText(),
                                                      TextMatcher.text("Расчёт выполнен успешно"), 10);
    }

    private void calculateButtonClickWithoutWait() {
        clickCalculationButton();
        confirmShiftCalculation();
    }

    @Step("Нажать на кнопку \"Рассчитать\"")
    private void clickCalculationButton() {
        LOG.info("Кликаем на кнопку \"Рассчитать\"");
        sb.formSetResetTimetable().buttonSet().click();
    }

    @Step("Проверить, что подсказка перед запуском расчета отображается")
    private void assertCalculationHintIsDisplayed(FileManual hint) {
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertTrue(sb.calculationConfirmationWindow().isDisplayed(), "Подсказка не отображается");
        String link = sb.calculationConfirmationWindow().calculationHint().getAttribute("src");
        softAssert.assertTrue(link.startsWith(hint.getSelfLink()), String.format("Подсказка на UI ссылается не на тот файл: ожидали ссылку %s,\n" +
                                                                                         "фактическая ссылка - %s", hint.getSelfLink(), link));
        softAssert.assertAll();
        Allure.attachment("Проверка", String.format("Подсказка перед запуском расчета отображается на UI.\n" +
                                                            "Содержимое подсказки можно найти в разделе \"Справка\" в файле %s", hint.getFileName()));
    }

    private void confirmShiftCalculation() {
        try {
            sb.calculationConfirmationWindow().continueButton().click();
            Allure.step("Кликнуть на кнопку подтверждения начала расчета");
            LOG.info("Кликаем на кнопку подтверждения начала расчета");
        } catch (ElementNotInteractableException e) {
            LOG.info("Окно с запросом подтверждения начала расчета не появилось");
        }
    }

    @Step("Проверить, что новые ростеры не создались")
    private void assertRostersNotCreated(List<Roster> before, List<Roster> after) {
        List<Integer> previousIds = before.stream().map(Roster::getId).collect(Collectors.toList());
        List<Integer> afterIds = after.stream().map(Roster::getId).collect(Collectors.toList());
        afterIds.removeAll(previousIds);
        Assert.assertTrue(afterIds.isEmpty(), "Создался новый ростер");
    }

    @Step("Проверить, что расчет для подразделения \"{unit.name}\" запустился")
    private void assertCalculationStarted(OrgUnit unit) {
        CalcJob calcJob = CalcJobRepository.getLatestCalculation(unit.getId());
        Assert.assertNotNull(calcJob, "Расчет не появился в API");
        if (calcJob.hasError()) {
            sb.formPostSRsDialog().waitUntil("Окно с уведомлением не отобразилось", DisplayedMatcher.displayed(), 10);
            String text = sb.formPostSRsDialog().getText();
            Allure.addAttachment("Ошибка расчета", text);
        }
    }

    @Step("Проверить, что расчет для подразделения \"{unit.name}\" запустился")
    private void assertCalculationStartedWithoutErrorCheck(OrgUnit unit) {
        CalcJob calcJob = CalcJobRepository.getLatestCalculation(unit.getId());
        Assert.assertNotNull(calcJob, "Расчет не появился в API");
    }

    @Step("Проверить, что расчет закончился с ошибкой")
    private void assertErrorMessageDisplayed(String errorMessage) {
        sb.formPostSRsDialog().calcErrDescrMessage(errorMessage).should("Сообщение об ошибке не отобразилось", DisplayedMatcher.displayed(), 120);
    }

    @Step("Нажать на кнопку \"{action}\"")
    private void clickContinueOrCancelCalculation(String errorMessage, String action) {
        sb.formPostSRsDialog().calcErrDescrMessage(errorMessage)
                .should(String.format("Сообщение \"%s\" не отобразилось", errorMessage),
                        DisplayedMatcher.displayed(), 200);
        Allure.addAttachment("Появилось окно с сообщением", "Его текст: " + errorMessage);
        sb.formPostSRsDialog().calcResumeOrCancelButton(action).click();
        sb.formPostSRsDialog().waitUntil("Окно с выбором действий не исчезло", Matchers.not(DisplayedMatcher.displayed()), 5);
        confirmShiftCalculation();
        if (action.contains("Продолжить")) {
            sb.formPostSRsDialog().waitUntil("Расчет не завершился", DisplayedMatcher.displayed(), 300);
        }
    }

    @Step("Закрыть форму редактирования смены")
    private void clickCloseButton() {
        LOG.info("Закрываем форму редактирования смены");
        sb.formSetResetTimetable().waitUntil("Форма не открылась", DisplayedMatcher.displayed(), 5);
        sb.formSetResetTimetable().closeFormButton().click();
        sb.formPostSRsDialog().should("Форма не была закрыта", Matchers.not(DisplayedMatcher.displayed()), 4);
        Allure.addAttachment("Закрытие формы", "Форма успешно закрылась");
    }

    @Step("Проверить отображение окна с ошибкой расчета")
    private void checkErrorAvailability() {
        sb.formErrorForm().layoutErrorForm()
                .should("Окно с ошибкой не было отображено", DisplayedMatcher.displayed(), 30);
        String errorMessage = sb.formErrorForm().errorTextField().getText();
        Allure.addAttachment("Было отображено сообщение об ошибке", "Его текст: " + errorMessage);
    }

    @Step("Проверить, что расчет выполнен успешно. Нажать на кнопку \"Закрыть\"")
    private void clickOnCloseButton() {
        LOG.info("Нажимаем на кнопку \"Закрыть\"");
        sb.formPostSRsDialog().resultMessage().should(DisplayedMatcher.displayed());
        sb.formPostSRsDialog().srsDialogCloseButton().click();
    }

    @Step("Закрыть окно с результатом расчета")
    private void clickCloseCalcResult() {
        LOG.info("Нажимаем на кнопку \"Закрыть\"");
        sb.formPostSRsDialog().srsDialogCloseButton().click();
    }

    @Step("Проверить, что был осуществлен переход в карточку подразделения")
    private void goToOrgUnitCardCheck() {
        sb.omInfoForm().omName()
                .waitUntil("Название ОМ не отобразилось", DisplayedMatcher.displayed(), 20);
        String orgUnitCardName = sb.omInfoForm().omName().getText();
        String orgUnitHeaderName = sb.mainHeader().headerText().getText().split(":")[1].trim();
        sb.omInfoForm().omName().should("Названия ОМ не совпадают", TextMatcher.text(Matchers.containsString(orgUnitHeaderName)), 5);
        Allure.addAttachment("Название", "Название оргЮнита в карточке: " + orgUnitCardName +
                ", Название оргЮнита в шапке: " + orgUnitHeaderName);
    }

    private Map<String, String> emptyPositionsMap() {
        Map<String, String> emptyPositionsMap = new HashMap<>();
        List<AtlasWebElement> temp = sb.subdivisionProperties().emptyPositionsNamesList();
        int positionNumber = new Random().nextInt(temp.size());
        emptyPositionsMap.put("index", String.valueOf(positionNumber));
        emptyPositionsMap.put(NAME, temp.get(positionNumber).getText());
        return emptyPositionsMap;
    }

    @Step("В поле \"Сотрудники\" выбрать должность без сотрудника, нажать на кнопку \"Троеточие\"")
    private void clickOnEmptyPositionThreeDotsButton(String index, String name) {
        List<AtlasWebElement> temp = sb.subdivisionProperties().emptyPositionsThreeDotsList();
        temp.get(Integer.parseInt(index)).click();
        LOG.info("Выбрана пустая должность с названием {}, индекс должности {}", name, index);
        Allure.addAttachment("Дожность", "была выбрана пустая должность с названием " + name);
    }

    @Step("Нажать на значок \"Троеточие\" возле сотрудника с именем : {name}")
    private void clickOnEmployeeThreeDots(String name) {
        sb.subdivisionProperties().allThreeDots().
                forEach(extendedWebElement -> extendedWebElement
                        .waitUntil("Не все троеточия загрузились", DisplayedMatcher.displayed(), 5));
        new Actions(sb.getWrappedDriver()).moveToElement(sb.subdivisionProperties().threeDotsByNameOfEmployee(name));
        sb.subdivisionProperties().threeDotsByNameOfEmployee(name).click();
    }

    @Step("В раскрывшемся меню выбрать \"Редактировать\"")
    private void editButtonClick() {
        sb.subdivisionProperties().employeeEditButton()
                .waitUntil("Ожидание появления кнопки редактирования", DisplayedMatcher.displayed(), 5);
        sb.subdivisionProperties().employeeEditButton().click();
        sb.addNewEmployeeForm()
                .waitUntil("Ожидание формы редактирования сотрудника", DisplayedMatcher.displayed(), 5);
    }

    @Step("Нажать на стрелочку вниз в поле \"Сотрудник\"")
    private void clickOnSelectEmployeeChevron() {
        sb.addNewEmployeeForm().selectEmployeeButton().click();
    }

    /**
     * проверяет что у сотрудника есть дата начала работы, если ее нет, то добавляет
     *
     * @return - дата, которая есть сейчас, или которую мы ввели
     */
    private LocalDate checkDataStartPosition() {
        String startDate = sb.addNewEmployeeForm()
                .inputVariantDate(DateTypeField.START_JOB.getName()).getAttribute(VALUE);
        LocalDate start = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.THAT, LocalDateTools.RANDOM);
        if (startDate.equals("")) {
            start = start.plusMonths(new Random().nextInt(6) + 1);
            chooseDatePositionForm(start, DateTypeField.START_JOB);
        } else {
            start = dateFromTextConverter(startDate);
        }
        if (start.isAfter(LocalDateTools.now())) {
            start = start.minusMonths(new Random().nextInt(6) + 1);
            chooseDatePositionForm(start, DateTypeField.START_JOB);
        }
        return start;
    }

    @Step("Выбрать сотрудника c именем {name} из списка")
    private void selectAnyEmployee(String name) {
        LOG.info("Имя выбранного сотрудника : {}", name);
        sb.addNewEmployeeForm().nameField().sendKeys(name);
        if (sb.addNewEmployeeForm().listOfFreeEmployees().size() == 0) {
            sb.addNewEmployeeForm().nameField().sendKeys(Keys.BACK_SPACE);
        }
        sb.addNewEmployeeForm().employeeButton(name)
                .waitUntil("Имя отсутствует в результатах поиска", DisplayedMatcher.displayed(), 30);
        sb.addNewEmployeeForm().employeeButton(name).click();
    }

    @Step("Нажать на стрелку вниз в поле \"Функциональная роль\"")
    private void clickOnFunctionalRolesSelectButton() {
        sb.addNewEmployeeForm().functionalRoleButton().click();
    }

    @Step("Выбрать функциональную роль : \"{positionGroup.name}\"")
    private void selectFuncRole(PositionGroup positionGroup) {
        sb.addNewEmployeeForm().variantsOfJobsItemGroup(positionGroup.getName())
                .waitUntil("Ожидание появления кнопок", DisplayedMatcher.displayed(), 15);
        sb.addNewEmployeeForm().variantsOfJobsItemGroup(positionGroup.getName()).click();
    }

    @Step("Проверить добавление функциональной роли : \"{positionGroup.name}\" выбранному сотруднику {employee}")
    private void addFuncRoleCheck(Employee employee, PositionGroup positionGroup) {
        int selectedEmployeeId = employee.getId();
        String name = employee.getShortName();
        List<EmployeePosition> employeePositions = EmployeePositionRepository.getEmployeePositionsWithoutFiredEmployees(getOrgIdFromUrl(), LocalDateTools.getLastDate(), false);
        String actualPositionHref = employeePositions.stream()
                .filter(ep -> ep.getEmployee().getId() == (selectedEmployeeId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Сотрудник не был найден по айди"))
                .getPosition().getLink("positionGroup");
        String expected = String.valueOf(positionGroup.getId());
        actualPositionHref = actualPositionHref.substring(actualPositionHref.lastIndexOf("/")).substring(1);
        Assert.assertEquals(actualPositionHref, expected, "Заданная функциональная роль :" + expected + " и функциональная роль" +
                " в АПИ " + actualPositionHref + " не совпадают");
        Allure.addAttachment("Позиция", "Сотруднику с именем : "
                + name + " была выбрана функциональная роль : " + positionGroup.getName());
    }

    @Step("Проверить добавление должности : {variantsOfJobs} выбранному сотруднику ")
    private void addJobCheck(Employee employee, String positionName) {
        SoftAssert softAssert = new SoftAssert();
        String text = sb.subdivisionProperties().popUpPositionChanged().getText();
        softAssert.assertEquals(text, "Должность изменена");
        int selectedEmployeeId = employee.getId();
        String name = employee.getShortName();
        List<EmployeePosition> employeePositions = EmployeePositionRepository.getEmployeePositionsWithoutFiredEmployees(getOrgIdFromUrl(), LocalDateTools.getLastDate(), false);
        String nameJob = employeePositions.stream()
                .filter(ep -> ep.getEmployee().getId() == (selectedEmployeeId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Сотрудник не был найден по айди"))
                .getPosition().getName();
        softAssert.assertEquals(nameJob, positionName, "Заданная группа позиций и группа позиции в АПИ не совпадают");
        softAssert.assertAll();
        Allure.addAttachment("Позиция", "Сотрудник с именем : "
                + name + " был назначен на должность : " + positionName);
    }

    @Step("!Особые условия")
    private void specialConditionsToChangeSchedule(BusinessHours scheduleId) {
        openScheduleSelectionMenu();
    }

    @Step("Нажать на кнопку \"Выбрать график работы\"")
    private void clickOnSelectScheduleButton() {
        sb.subdivisionProperties().selectScheduleButton()
                .waitUntil("Кнопка выбора графика не отображается", DisplayedMatcher.displayed(), 10);
        sb.subdivisionProperties().selectScheduleButton()
                .waitUntil("Выбрать график работы не отобразился", TextMatcher.text(Matchers.containsString("Управление списком графиков работы")), 5);
        sb.subdivisionProperties().selectScheduleButton().click();
        sb.selectScheduleForm().cancelButton().waitUntil("Кнопка отменить", DisplayedMatcher.displayed(), 10);
    }

    //todo следующие несколько методов повторяют код из MixInt. Скорее всего, нужен рефактор
    @Step("*особые действия: зайти в меню выбора графика при открытом меню троеточия")
    private void openScheduleSelectionMenu() {
        sb.subdivisionProperties().selectScheduleButton()
                .waitUntil("Кнопка выбора графика не отображается", DisplayedMatcher.displayed(), 10);
        sb.subdivisionProperties().selectScheduleButton()
                .waitUntil("Выбрать график работы не отобразился", DisplayedMatcher.displayed(), 5);
        sb.subdivisionProperties().selectScheduleButton().click();
        sb.selectScheduleForm().waitUntil("Кнопка отменить", DisplayedMatcher.displayed(), 15);
    }

    private String determineActiveScheduleId(int orderNumber) {
        ActWithSchedule actWithSchedule = new ActWithSchedule(sb.selectScheduleForm());
        return actWithSchedule.getActiveScheduleId(orderNumber);
    }

    @Step("*особые действия: определить порядковый номер активного графика при открытом меню троеточия")
    private int determineActiveScheduleNumber() {
        String activeSchedule = sb.subdivisionProperties().activeSchedule().getText();
        List<String> schedules = sb.subdivisionProperties().allAvailableSchedules().stream().map(WebElement::getText).collect(Collectors.toList());
        return schedules.indexOf(activeSchedule);
    }

    @Step("*особые действия: выйти из меню выбора графика")
    private void exitScheduleSelectionMenu() {
        sb.selectScheduleForm().cancelButton().click();
    }

    @Step("*особые действия: обновить UI, чтобы на нем отобразились изменения, которые могли быть внесены пресетом")
    private void refreshScheduleUI() {
        clickOnThreeDotsButton();
        sb.subdivisionProperties().activeSchedule().click();
    }

    @Step("Кликнуть на значок \"Плюс\"")
    private void clickOnPlusButtonEmployee() {
        sb.subdivisionProperties().plusButtonEmployee().
                waitUntil("plus button was not displayed", DisplayedMatcher.displayed(), 10);
        sb.subdivisionProperties().plusButtonEmployee().click();
    }

    @Step("Проверить, что после сохранения список позиций с " +
            "вариантом должности {variantsOfJobs} c датой начала {startDate} увеличился на один")
    private void assertionCompareMaps(List<Position> before, JobTitle jobTitle, int id, LocalDate startDate) {
        List<Position> after = PositionRepository.emptyPositionReturner(jobTitle, startDate, id);
        if (after.size() == 0) {
            Assert.fail("Список пустой, после теста нет изменений в системе");
        }
        Allure.addAttachment("Сравнение позиций", "У оргюнита с id : " + id +
                " c вариантом должности " + jobTitle + " c датой начала " + startDate + "Позиции до : "
                + before + " позиции после : " + after + "Разница в их количестве : " + (after.size() - before.size()));
        Assert.assertEquals(after.size() - before.size(), 1, "В апи нет изменений");
    }

    @Step("Выбрать работу при создании новой должности {variantsOfJobs}")
    private void chooseJob(JobTitle jobTitle) {
        sb.spinnerLoader().loadingSpinnerInForm().waitUntil("Спиннер загрузки все еще отображается", Matchers.not(DisplayedMatcher.displayed()), 15);
        sb.addNewEmployeeForm().inputJobCategory().click();
        sb.addNewEmployeeForm().jobTitle(jobTitle.getFullName()).click();
        LOG.info("Был выбран вариант: {}", jobTitle);
    }

    @Step("Выбрать дату {date} в форме должности в поле \"{inputVariants.name}\"")
    private void chooseDatePositionForm(LocalDate date, DateTypeField inputVariants) {
        String inputVar = inputVariants.getName();
        sb.addNewEmployeeForm().inputVariantDate(inputVar).click();
        sb.addNewEmployeeForm().inputVariantDate(inputVar).clear();
        sb.addNewEmployeeForm().inputVariantDate(inputVar).sendKeys(date.format(UI.getFormat()));
        LOG.info("В поле {} ввели дату {} ", inputVar, date);
    }

    @Step("Проверить, что поле \"Табельный номер\" не отображается")
    private void assertNotDisplayedCardNumber() {
        sb.addNewEmployeeForm().cardNumberField()
                .waitUntil("Поле \"Табельный номер\" отображается", Matchers.not(DisplayedMatcher.displayed()), 3);
        Allure.attachment("Проверка", "Поле \"Табельный номер\" не отображается");
    }

    @Step("Проверить, что поле \"Ставка\" не отображается")
    private void assertNotDisplayedRate() {
        sb.addNewEmployeeForm().rateField()
                .waitUntil("Поле \"Табельный номер\" отображается", Matchers.not(DisplayedMatcher.displayed()), 3);
        Allure.attachment("Проверка", "Поле \"Ставка\" не отображается");
    }

    @Step("В поле \"Табельный номер\" ввести значение \"{cardNumber}\"")
    private void changeCardNumber(String cardNumber) {
        LOG.info("В поле \"Табельный номер\" ввести \"{}\"", cardNumber);
        sb.addNewEmployeeForm().cardNumberField().click();
        sb.addNewEmployeeForm().cardNumberField().clear();
        sb.addNewEmployeeForm().cardNumberField().sendKeys(String.valueOf(cardNumber));
    }

    /**
     * @param rate значение double/int от 0.1 до 1 (знаков после запятой меньше 3)
     */
    @Step("В поле \"Ставка\" ввести значение \"{rate}\"")
    private void changeRate(String rate) {
        LOG.info("В поле \"Ставка\" ввести \"{}\"", rate);
        sb.addNewEmployeeForm().rateField().click();
        sb.addNewEmployeeForm().rateField().clear();
        sb.addNewEmployeeForm().rateField().sendKeys(rate);
    }

    @Step("Проверить, что в поле \"Табельный номер\" отображается значение \"{cardNumber}\"")
    private void assertCardNumberValue(String cardNumber, EmployeePosition ep) {
        String cardNumberIn = sb.addNewEmployeeForm().cardNumberField().getAttribute("value");
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(cardNumber, cardNumberIn, "Табельный номер сотрудника на UI не совпадает со значением, заданным в ходе теста");
        softAssert.assertEquals(ep.getCardNumber(), cardNumber, "Табельный номер сотрудника в API не совпадает со значением, заданным в ходе теста");
        softAssert.assertAll();
        Allure.attachment("Проверка", String.format("Значение табельного номера изменено на %s", cardNumber));
    }

    @Step("Проверить, что в поле \"Ставка\" отображается значение \"{rate}\"")
    private void assertRateValue(String rate, EmployeePosition ep) {
        String rateEp = String.format(Locale.US, "%.1f", ep.getRate());
        String rateIn = sb.addNewEmployeeForm().rateField().getAttribute("value");
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(rate, rateIn, "Ставка сотрудника на UI не совпадает со значением, заданным в ходе теста");
        softAssert.assertEquals(rate, rateEp, "Ставка сотрудника в API не совпадает со значением, заданным в ходе теста");
        softAssert.assertAll();
        Allure.attachment("Проверка", "Значение ставки изменено на " + rate);
    }

    @Step("Нажать на кнопку \"Сохранить\"")
    private void saveButtonClick() {
        sb.addNewEmployeeForm().saveButton()
                .waitUntil("Кнопка \"Сохранить\" не загрузилась", DisplayedMatcher.displayed(), 10);
        LOG.info("Кликаем \"Сохранить\" на форме должности");
        sb.addNewEmployeeForm().saveButton().sendKeys(Keys.TAB);
        sb.addNewEmployeeForm().saveButton().sendKeys(Keys.ENTER);
        sb.addNewEmployeeForm().addPositionSnackBar()
                .should("Снэкбар с добавлением должности не отобразился", DisplayedMatcher.displayed(), 25);
        sb.addNewEmployeeForm().waitUntil("Форма редактирования сотрудника не закрылась",
                                          Matchers.not(DisplayedMatcher.displayed()), 30);
        sb.spinnerLoader().grayLoadingBackground().waitUntil("Спиннер все еще отображается", Matchers.not(DisplayedMatcher.displayed()), 10);
        sb.subdivisionProperties().popUpPositionChanged()
                .should("Поп-ап панелька удачного изменения должности не открылась",
                        Matchers.not(DisplayedMatcher.displayed()));
    }

    @Step("Проверить добавление должности \"Виртуальный сотрудник\" человеку с именем {name}")
    private void assertVirtualEmployeeAdding(OrgUnit orgUnit, String name) {
        sb.spinnerLoader().loadingSpinnerPage().waitUntil("Спиннер все еще отображается",
                                                          Matchers.not(DisplayedMatcher.displayed()), 10);
        sb.formLayout().popUpForApproval().waitUntil("Поп-ап все еще отображается",
                                                     Matchers.not(DisplayedMatcher.displayed()), 20);
        String posName = EmployeePositionRepository.getEmployeePosition(orgUnit, name).getPosition().getName();
        Assert.assertEquals(posName, "Виртуальный сотрудник",
                            "У человека нет такой должности в данном оргЮните с \"Виртуальный сотрудник\"");
        Allure.addAttachment("Добавление должности",
                             "Сотрудника с именем " + name + " Была добалена должность\"Виртуальный сотрудник\"");
    }

    private LocalDate dateFromTextConverter(String date) {
        String[] dates = date.split(" ");
        String monthNameRus = dates[1];
        Map<String, Integer> months = new HashMap<>();
        for (MonthsEnum monthsEnum : MonthsEnum.values()) {
            String monthName = monthsEnum.getMonthName();
            months.put(monthName.substring(0, monthName.length() - 1), monthsEnum.ordinal() + 1);
        }
        String month = months.keySet().stream()
                .filter(s -> monthNameRus.contains(s.toLowerCase())).findAny()
                .orElseThrow(() -> new AssertionError("schedule message. Не нашли название месяца"));
        return LocalDate.of(Integer.parseInt(dates[2]), months.get(month), Integer.parseInt(dates[0]));
    }

    @Step("Проверить добавление {name}  на  должность '{positionName}'")
    private void assertForStateEmployeeOnFreePosition(String startDate, String dateEnd, String name, String positionName) {
        sb.addNewEmployeeForm().addPositionSnackBar()
                .should("Снэкбар с добавлением должности не отобразился", DisplayedMatcher.displayed(), 10);
        systemSleep(5); //метод используется в неактуальных тестах
        EmployeePosition employeePosition = EmployeePositionRepository.getEmployeePositions(getOrgIdFromUrl()).stream()
                .filter(ep -> ep.toString().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Сотрудник не был добавлен на свободную должность"));
        String startDateApi = employeePosition.getDateInterval().startDate.toString();
        String endDateApi = employeePosition.getDateInterval().endDate.toString();
        String posNameApi = employeePosition.toString();
        Allure.addAttachment("Проверка ", "Был добавлен сотрудник на пустую должность даты, название позиции и имя совпадают в апи с выбранными в" +
                " ходе теста");
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(positionName, posNameApi, "Должность не совпадает");
        softAssert.assertEquals(dateEnd, endDateApi, "Дата окончания должности не совпадает");
        softAssert.assertEquals(startDate, startDateApi, "Дата начала должности не совпадает");
        softAssert.assertAll();
    }

    @Step("Проверить добавление даты окончания")
    private void assertDateEndAvailability(LocalDate date, Employee employee) {
        int id = employee.getId();
        String name = employee.getFullName();
        List<EmployeePosition> employeePositions = EmployeePositionRepository.getEmployeePositionsWithoutFiredEmployees(getOrgIdFromUrl(), LocalDateTools.getLastDate(), false);
        LocalDate apiDateEnd = employeePositions.stream()
                .filter(ep -> ep.getEmployee().getId() == id)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Сотрудник не был найден по айди"))
                .getDateInterval().endDate;
        assertTrue(apiDateEnd.isEqual(date), "Дата не была высталена");
        Allure.addAttachment("Дата окончания", "В ходе теста была высталвена дата окончания "
                + date + " сотруднику с именем " + name + " в api была найдена дата " + apiDateEnd);
    }

    @Step("Ввести текст в окно ввода {infoName.nameOfInformation} ")
    private void sendInTargetParamInput(String paramName, int textToSend) {
        sb.parameterForm().paramNameInput(paramName).clear();
        sb.parameterForm().paramNameInput(paramName).sendKeys(String.valueOf(textToSend));
    }

    @Step("Сохранить изменение параметров")
    private void saveParameterChanges() {
        sb.parameterForm().saveParamButton().click();
    }

    @Step("Проверить, что сотрудник по имени {chosenName} стал руководителем отделения")
    private void assertForMakingLeader(Employee employee) {
        String chosenName = employee.getFullName();
        int chosenId = employee.getId();
        sb.subdivisionProperties().popUpPositionChanged().waitUntil("Was displayed",
                                                                    Matchers.not(DisplayedMatcher.displayed()), 15);
        //обращаемся к оргюниту и находим ссылку на руководителя, оканчивающуюся id сотрудника
        int currentOM = getOrgIdFromUrl();
        String urlEnding = makePath(ORGANIZATION_UNITS, currentOM, ORGANIZATION_UNIT_CHIEFS_CHAIN);
        JSONObject temp = getJsonFromUri(Projects.WFM, URL_SB, urlEnding);
        JSONArray omList = temp.getJSONObject(EMBEDDED).getJSONArray(POSITIONS);
        URI chiefRequest = URI.create(omList.getJSONObject(0)
                                              .getJSONObject(LINKS).getJSONObject(EMPLOYEE_JSON).getString(HREF));
        //находим фио текущего руководителя
        JSONObject chiefEmp = new JSONObject(setUrlAndInitiateForApi(chiefRequest, Projects.WFM));
        int idEmp = chiefEmp.getInt("id");
        //Имя человека, который является на данный момент руководителем
        String headEmp = new Employee(chiefEmp).getFullName();
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(idEmp, chosenId, "Id выбранного сотрудника не совпало с " +
                "id сотрудника , который сейчас в api руководитель");
        softAssert.assertEquals(headEmp, chosenName, "Руководитель " +
                "подразделения не поменялся");
        softAssert.assertAll();
        Allure.addAttachment("Имя руководителя", "text/plain",
                             "Теперь рководителем стал: " + headEmp);
    }

    @Step("Нажать на чекбокс \"Руководитель\"")
    private void leaderCheckBoxClick() {
        sb.addNewEmployeeForm().leaderCheckBox().click();
    }

    private String getParamName(boolean withValue, OrgUnit orgUnit) {
        //todo объединить с таким же методом из оргструктуры. Реализовать добавление параметра через api, а не ui
        Map<String, String> tempMap = getMathParameterValues(orgUnit.getId());
        ArrayList<String> paramNameEqualsEnum = new ArrayList<>();
        for (int i = 0; i < (ParamName.values().length); i++) {
            String name = (ParamName.values()[i]).getName();
            if (tempMap.containsKey(name)) {
                paramNameEqualsEnum.add(name);
            }
        }
        ArrayList<String> paramNameWithout = new ArrayList<>();
        String targetParamName = "";
        if (withValue) {
            if (paramNameEqualsEnum.size() == 0) {
                targetParamName = (ParamName.values()[new Random().nextInt(ParamName.values().length)]).getName();
            } else if (paramNameEqualsEnum.size() < ParamName.values().length) {
                for (int i = 0; i < ParamName.values().length; i++) {
                    if (!paramNameEqualsEnum.contains((ParamName.values()[i]).getName())) {
                        paramNameWithout.add((ParamName.values()[i]).getName());
                    }
                }
                targetParamName = String.valueOf(getRandomFromList(paramNameWithout));
            } else {
                targetParamName = PresetClass.makeClearParam(orgUnit);
            }
        } else {
            if (paramNameEqualsEnum.size() > 0) {
                targetParamName = String.valueOf(getRandomFromList(paramNameEqualsEnum));
            } else {
                String paramName = getParamName(false, orgUnit);
                int rndNumber = new Random().nextInt(1000);
                sendInTargetParamInput(paramName, rndNumber);
                saveParameterChanges();
                assertParamChanges(paramName, rndNumber);
                targetParamName = paramName;
            }
        }
        LOG.info("Выбран параметр: {}", targetParamName);
        return targetParamName;
    }

    @Step("Проверить, что был изменен параметр {name} под номером {number}")
    private void assertParamChanges(String name, int number) {
        String infoName = EmployeeInfoName.OPTIONS.getNameOfInformation();
        sb.parameterForm().waitUntil(Matchers.not(DisplayedMatcher.displayed()));
        while (!sb.parameterForm().isDisplayed()) {
            sb.employeeDataMenu().pencilButton(infoName).click();
        }
        MathParameter param = MathParameterRepository.getMathParameters().stream().filter(p -> p.getShortName().equals(name)).findFirst().orElse(null);
        MathParameterValue value = MathParameterValueRepository.getMathParameterValueForEntity(MathParameterEntities.ORGANIZATION_UNIT, getOrgIdFromUrl(), param);
        String check = StringUtils.substringBefore(String.valueOf(value.getValue()), ".");
        if (!check.equals(String.valueOf(number))) {
            Assert.fail("Параметры не изменились");
        }
    }

    @Step("Кликнуть на кнопку меню пустой должности (три точки)")
    private void threeDotsEmptyPositions() {
        Random random = new Random();
        sb.subdivisionProperties().threeDotEmptyPositionButtons().get(1)
                .waitUntil("Должности не отобразились", DisplayedMatcher.displayed(), 30);
        int size = random.nextInt(sb.subdivisionProperties().threeDotEmptyPositionButtons().size());
        sb.subdivisionProperties().threeDotEmptyPositionButtons().get(size).click();
        AtlasWebElement positionElement;
        String positionName;
        try {
            positionElement = sb.subdivisionProperties().emptyPositionsNamesRelease().get(size);
            positionName = positionElement.getText().replaceAll(" ", "");
        } catch (IndexOutOfBoundsException e) {
            positionElement = sb.subdivisionProperties().emptyPositionsNamesMaster().get(size);
            positionName = positionElement.getText().substring("Ставка: / ".length() + 1);
        }
        Allure.addAttachment("Название должности", "text/plain",
                             "Выбрана должность: " + positionName);
    }

    @Step("Нажать на кнопку удаления должности")
    private void deletePositions() {
        sb.subdivisionProperties().deletePositionButton().click();
    }

    @Step("Нажать на кнопку \"Печать\"")
    private void pushPrintButton() {
        sb.printForm().printButton().click();
        sb.printForm().waitUntil("Форма печати еще не закрылась",
                                 Matchers.not(DisplayedMatcher.displayed()), 10);
    }

    @Step("Нажать на радиобатон \"Только график\"")
    private void pushRadioButtonOnlySchedule() {
        sb.printForm().radioButtonOnlySchedule().click();
    }

    @Step("Проверить, что должность была удалена")
    private void assertForDeleteEmptyPosition(List<Position> before) {
        sb.subdivisionProperties().positionDeleted()
                .should("Всплывающее окно \"Должность удалена\" не был отображен",
                        DisplayedMatcher.displayed(), 20);
        sb.subdivisionProperties().positionDeleted()
                .waitUntil("Всплывающее окно \"Должность удалена\" не исчезла",
                           Matchers.not(DisplayedMatcher.displayed()), 10);
        List<Position> after = PositionRepository.checkApiPositionsDate(getOrgIdFromUrl());
        String namePosition = "";
        if (after.size() == before.size() + 1) {
            after.removeAll(before);
            namePosition = after.get(0).getName();
        } else {
            Assert.fail("Пустая должность не была удалена");
        }
        Allure.addAttachment("Название должности", "text/plain",
                             "Была удалена должность: " + namePosition);
    }

    /**
     * Метод проверки скачивания отчетов для контента за выбранные даты/месяц
     *
     * @param content - тип контента
     * @param checker - инициализированный чекер
     */
    @Step("Здесь проверяется что даты в названии файла дейсвительно совпадют с датами в приложении")
    private void assetForRightDownloading(TypeOfAcceptContent content, FileDownloadCheckerForScheduleBoard checker, Role role) {
        HttpResponse httpResponse = checker.downloadResponse(role, content);
        assertStatusCode(httpResponse, 200, checker.getDownloadLink().toString());
        ImmutablePair<String, String> fileNameExtensionFromResponse = getFileNameExtensionFromResponse(httpResponse);
        String expectedFormat = checker.getTypeOfFiles().getFileExtension();
        String expectedFilename = checker.getFileName();
        String responseFilename = fileNameExtensionFromResponse.left;
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(fileNameExtensionFromResponse.right, expectedFormat, "Расширение файла не совпадает с ожидаемым");
        softAssert.assertEquals(responseFilename, expectedFilename, "Имя файла не совпадает с ожидаемым");
        softAssert.assertTrue(responseFilename.contains(LocalDateTools.getFirstDate().toString()), "Даты начала не было в названии файла");
        softAssert.assertTrue(responseFilename.contains(LocalDateTools.getLastDate().toString()), "Даты окончания не было в названии файла");
        softAssert.assertAll();
        Allure.addAttachment("Скачанный файл",
                             "text/plain",
                             "Скачан файл: " + responseFilename + "." + fileNameExtensionFromResponse.right);
    }

    @Step("Проверить документ после нажатия на печать")
    private void assertForRightDownloadingPDF(FileDownloadCheckerForScheduleBoard checker) {
        systemSleep(10); //метод используется в неактуальных тестах
        SoftAssert softAssert = new SoftAssert();
        HttpResponse httpResponse = checker.downloadResponse(Role.ADMIN, TypeOfAcceptContent.BASIC);
        assertStatusCode(httpResponse, 200, checker.getDownloadLink().toString());
        //Определение типа контента
        List<String> attachmentContents = Arrays.stream(httpResponse.getAllHeaders())
                .filter(header -> header.getName().contains("Content-Type"))
                .map(Header::getValue)
                .collect(Collectors.toList());
        String content = "application/pdf";
        softAssert.assertTrue(attachmentContents.contains(content), "Контент ответа не соотвествует PDF");
        CustomTools.removeFirstWindowHandler(sb);
        URI urlAddress = URI.create(sb.getWrappedDriver().getCurrentUrl());
        URI checkerUri = checker.getDownloadLink();
        Allure.addAttachment("Скачивание PDF", "Ожидаемая ссылка: " + checkerUri
                + ", ссылка, которая сформировалась в новой странице после нажатия на печать: " + urlAddress);
        softAssert.assertTrue(EqualUri.areEquals(urlAddress, checkerUri), "Ссылки для скачивания файла не совпали," +
                " адрес страницы: " + urlAddress + ", а ожидалось: " + checkerUri);
        softAssert.assertAll();
    }

    @Step("Убирать галочки до тех пор, пока не останется одна")
    private void switchOffPositionsCheckBoxes() {
        sb.printPDFForm().waitUntil("Форма печати не была отображена", DisplayedMatcher.displayed(), 5);
        List<AtlasWebElement> temp = sb.printPDFForm().positionsTypesCheckBoxesList();
        int previouslySize = temp.size();
        for (int i = 0; i < temp.size(); i++) {
            if (temp.size() > 1) {
                temp.stream().findAny().ifPresent(WebElement::click);
            }
        }
        int afterSize = temp.size();
        Allure.addAttachment("Типы позиций",
                             "До снятия было " + previouslySize + " после снятия стало " + afterSize + "выделенных групп позиций");
    }

    @Step("Убрать одну галочку с произвольно выбранного типа сотрудника")
    private void switchOffOnePositionCheckBox() {
        sb.printPDFForm().waitUntil("Форма печати не была отображена", DisplayedMatcher.displayed(), 5);
        List<AtlasWebElement> temp = sb.printPDFForm().positionsTypesCheckBoxesList();
        int previouslySize = temp.size();
        temp.stream().findAny().ifPresent(WebElement::click);
        int afterSize = temp.size();
        Allure.addAttachment("Типы позиций",
                             "До снятия было " + previouslySize + " после снятия стало " + afterSize + "выделенных групп позиций");
    }

    private String getJobTypeAndConvertToId() {
        ElementsCollection<AtlasWebElement> temp = sb.printPDFForm().beforePrintJobsList();
        ArrayList<String> tempJobs = new ArrayList<>();
        temp.stream().map(WebElement::getText).forEach(tempJobs::add);
        Map<String, String> jobsEnum = new HashMap<>();
        for (PositionTypes type : PositionTypes.values()) {
            jobsEnum.put(type.getName(), type.getId());
        }
        StringBuilder fullJobsIds = new StringBuilder();
        for (int i = 0; i < tempJobs.size(); i++) {
            if (i == 0) {
                fullJobsIds.append(jobsEnum.get(tempJobs.get(i)));
            } else {
                fullJobsIds.append(",").append(jobsEnum.get(tempJobs.get(i)));
            }
        }
        Allure.addAttachment("Типы должностей. ", "В окне печати было "
                + tempJobs.size() + " позиции и их айди: " + fullJobsIds);
        return fullJobsIds.toString();
    }

    /**
     * Возвращает список из трех дат: даты начала и конца заданного интервала и одну случайную дату между ними
     */
    private List<LocalDate> getDatesForCheckingUi(DateInterval interval) {
        List<LocalDate> dates = interval.getBetweenDatesList();
        if (dates.size() == 1) {
            return Collections.singletonList(dates.get(0));
        } else if (dates.size() == 2) {
            return dates;
        }
        LocalDate start = interval.getStartDate();
        LocalDate end = interval.getEndDate();
        dates.removeAll(Arrays.asList(start, end));
        LocalDate randomBetween = getRandomFromList(dates);
        return new LinkedList<>(Arrays.asList(start, randomBetween, end));
    }

    private void prepareShiftsForCellAccessibilityCheck(List<LocalDate> datesToCheck, EmployeePosition ep, LocalDate lockDate) {
        List<LocalDate> datesToCheckPastMonth = datesToCheck.stream()
                .filter(date -> date.getMonth() != LocalDate.now().getMonth())
                .collect(Collectors.toList());
        if (!datesToCheckPastMonth.isEmpty()) {
            for (LocalDate localDate : datesToCheckPastMonth) {
                PresetClass.presetForMakeShiftDate(ep, localDate, false, ShiftTimePosition.PAST_MONTH);
            }
        }
        datesToCheck.removeAll(datesToCheckPastMonth);
        for (LocalDate localDate : datesToCheck) {
            PresetClass.presetForMakeShiftDate(ep, localDate, false, ShiftTimePosition.PAST);
        }
        PresetClass.presetForMakeShiftDate(ep, lockDate, false, ShiftTimePosition.PAST);
    }

    /**
     * Возвращает три смены из прошлого месяца для последующей проверки того, активен табель или нет.
     * Одна смена в первый день периода, одна - в последний, и одна - в случайны день между ними.
     * Если в ростере нет ни одной смены на заданные даты, возвращает смены-болванки, в которых заполнена только дата.
     */
    private List<Shift> getShiftsForCheckingLastMonth(int omId, EmployeePosition ep) {
        LocalDate monthAgo = LocalDate.now().minusMonths(1).withDayOfMonth(1);
        DateInterval interval = new DateInterval(monthAgo, monthAgo.withDayOfMonth(monthAgo.getMonth().length(monthAgo.isLeapYear())));
        Roster workedRoster = RosterRepository.getZeroRosterOrReturnNull(omId, interval);
        if (workedRoster == null) {
            throw new AssertionError("Не найден табель за прошлый месяц");
        }
        List<Integer> clickableEmployees = EmployeePositionRepository.getAllEmployeesWithCheckByApi(omId, null, true)
                .stream()
                .map(EmployeePosition::getId)
                .collect(Collectors.toList());
        List<Shift> shifts = ShiftRepository.getShiftsForRoster(workedRoster.getId(), interval)
                .stream()
                .filter(s -> clickableEmployees.contains(s.getEmployeePositionId()))
                .collect(Collectors.toList());
        LocalDate start = interval.getStartDate();
        LocalDate end = interval.getEndDate();
        Shift firstDay = shifts.stream()
                .filter(s -> s.getDateTimeInterval().getStartDate().equals(start))
                .findAny()
                .orElse(getDummyShiftFromPeriod(start, ep));
        Shift lastDay = shifts.stream()
                .filter(s -> s.getDateTimeInterval().getEndDate().equals(end))
                .findAny()
                .orElse(getDummyShiftFromPeriod(end, ep));
        Shift randomDay = shifts.stream()
                .filter(s -> !s.getDateTimeInterval().getEndDate().equals(end)
                        && !s.getDateTimeInterval().getStartDate().equals(start))
                .findFirst().orElse(null);
        if (randomDay == null) {
            randomDay = getDummyShiftFromPeriod(interval.getRandomDateBetween(), ep);
        } else {
            PresetClass.deleteRequest(randomDay);
            randomDay = getDummyShiftFromPeriod(randomDay.getStartDate(), ep);
        }
        return Arrays.asList(firstDay, randomDay, lastDay);
    }

    /**
     * Формирует смену-болванку для использования в pagemodel.ScheduleBoard#getShiftsForCheckingLastMonth
     */
    private Shift getDummyShiftFromPeriod(LocalDate date, EmployeePosition ep) {
        PresetClass.makeClearDate(ep, date);
        return new Shift().setDateTimeInterval(new DateTimeInterval(date.atStartOfDay(),
                                                                    date.atTime(8, 0)));
    }

    @Step("Проверить в апи что у подразделения есть комментарии с {dateInterval.startDate} до {dateInterval.endDate}, т.к." +
            " документ для печати формируется из api")
    private void checkCommentAvailable(OrgUnit orgUnit, DateInterval dateInterval) {
        Map<String, String> temp = PresetClass.getDayCommentPreset(dateInterval, orgUnit);
        Assert.assertNotNull(temp);
    }

    @Step("Проверить, что график был отправлен на утверждение")
    private void assertPopUpForApproval(ZonedDateTime localDateTimeServer, OrgUnit unit) {
        sb.formLayout().popUpForApproval().waitUntil("Поп-ап не отобразился", DisplayedMatcher.displayed(), 10);
        sb.formLayout().popUpForApproval().should("График не отправлен на утверждение", TextMatcher.text(Matchers.containsString("График отправлен на утверждение")), 10);
        Roster activeRoster = RosterRepository.getActiveRosterThisMonth(unit.getId());
        LocalDateTime dateTimeApi = activeRoster.getOnApprovalTime();
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertTrue(activeRoster.isOnApproval(), "Статус графика в API не изменился на \"На утверждении\"");
        String indicator = waitForRosterStatusToChange(GraphStatus.ON_APPROVAL, true);
        if (!indicator.contains(".")) {
            softAssert.assertEquals(indicator, "Плановый график на утверждении",
                                    "Статус графика на UI не изменился на \"На утверждении\"");
        }
        ZonedDateTime zonedApiTime;
        try {
            zonedApiTime = dateTimeApi.atZone(unit.getTimeZone());
        } catch (NullPointerException e) {
            zonedApiTime = dateTimeApi.atZone(ZoneId.of("UTC"));
        }
        long differentTimes = Math.abs(ChronoUnit.MINUTES.between(zonedApiTime, localDateTimeServer));
        softAssert.assertTrue(differentTimes < 3, "Время не совпадает больше, чем на 2 минуты, " +
                "время отправки графика на утверждение: " + localDateTimeServer + ", время графика на сервере: " + zonedApiTime);
        softAssert.assertAll();
        Allure.addAttachment("Отправка графика на утверждение", String.format("Время отправки на утверждение в API - %s, текущее время - %s\n" +
                                                                                      "График отображается как отправленный на утверждение в API и на UI",
                                                                              dateTimeApi, localDateTimeServer.toString()));
    }

    @Step("Проверить, что была создана смена")
    private void assertCreateShift(EmployeePosition emp, DateTimeInterval dateTimeInterval, ScheduleWorker scheduleWorker, boolean endInTheNextDay) {
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertNotNull(
                scheduleWorker.getScheduleShiftElement(emp, dateTimeInterval.getStartDate()),
                "Смена не была отображена на сайте"
        );
        Shift shift = ShiftRepository.getShift(emp, dateTimeInterval.getStartDate(), null);
        Assert.assertNotNull(shift, "Смена не была создана");
        softAssert.assertEquals(shift.getDateTimeInterval().toString(), dateTimeInterval.toString(),
                                "Время начала и конца смены не соответствуют заданным значениям");
        if (endInTheNextDay) {
            softAssert.assertTrue(shift.isNextDayEnd(), "Смена не заканчивается следующим днем");
        }
        Allure.addAttachment("Смена", shift.toString());
        softAssert.assertAll();
    }

    @Step("Проверить, что была создана смена c обедом в соответствии с правилами обеда")
    private void assertCreateShiftWithLunchRule(EmployeePosition emp, DateTimeInterval dateTimeInterval, ScheduleWorker sw, int lunchTime) {
        Shift shift = ShiftRepository.getShift(emp, dateTimeInterval.getStartDate(), null);
        Assert.assertNotNull(shift, "Смена не была создана");
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertNotNull(sw.getScheduleShiftElement(emp, dateTimeInterval.getStartDate()), "Смена не была отображена на сайте");
        softAssert.assertEquals(shift.getDateTimeInterval().toString(), dateTimeInterval.toString(),
                                "Время начала и конца смены не соответствуют заданным значениям");
        softAssert.assertEquals(shift.getLunch().intValue(), lunchTime, "Время обеда не соответствует правилу обеда");
        Allure.addAttachment("Фактическое время обеда в смене", String.format("%d минут", shift.getLunch()));
        softAssert.assertAll();
    }

    @Step("Проверить, что была создана смена с перерывом")
    private void assertCreateShift(EmployeePosition emp, DateTimeInterval dateTimeInterval, ScheduleWorker scheduleWorker, Long breakInMinutes) {
        SoftAssert softAssert = new SoftAssert();
        LocalDate startDate = dateTimeInterval.getStartDate();
        LOG.info("Проверяем, что была создана смена с перерывом на {}", startDate);
        if (!sb.formTopBar().monthSelected().getText().equals(startDate.getMonth().getDisplayName(TextStyle.FULL_STANDALONE, new Locale("ru")))) {
            clickForward();
            scheduleWorker = new ScheduleWorker(sb);
        }
        softAssert.assertNotNull(
                scheduleWorker.getScheduleShiftElement(emp, startDate),
                "Смена не была отображена на сайте"
        );
        Shift shift = ShiftRepository.getShift(emp, dateTimeInterval.getStartDate(), ShiftTimePosition.FUTURE_WITH_NEXT_MONTH);
        Assert.assertNotNull(shift, "Смена не была создана");
        softAssert.assertEquals(shift.getDateTimeInterval().toString(), dateTimeInterval.toString(),
                                "Время начала и конца смены не соответствуют заданным значениям");
        softAssert.assertEquals(shift.getBreaks(), breakInMinutes, "Время перерыва не соответствует заданному времени перерыва");
        Allure.addAttachment("Смена", shift.toString());
        softAssert.assertAll();
    }

    @Step("Проверить, что была создана смена, заканчивающаяся в следующем месяце")
    private void assertCreateShiftEndInNextMonth(EmployeePosition emp, LocalDateTime startDateTime,
                                                 LocalDateTime endDateTime, ScheduleWorker scheduleWorker) {
        SoftAssert softAssert = new SoftAssert();
        Shift shift = ShiftRepository.getShift(emp, startDateTime.toLocalDate(), null);
        softAssert.assertNotNull(
                scheduleWorker.getScheduleShiftElement(emp, startDateTime.toLocalDate()),
                "Смена не была отображена на сайте");
        softAssert.assertNotNull(shift, "Смена не была создана");
        softAssert.assertEquals(shift.getDateTimeInterval().getStartDateTime(), startDateTime, "Дата начала смены не совпала с введенной");
        softAssert.assertEquals(shift.getDateTimeInterval().getEndDateTime(), endDateTime, "Дата окончания смены не совпала с введенной");
        Allure.addAttachment("Смена", shift.toString());
        softAssert.assertAll();
    }

    @Step("Проверить, что смена была удалена")
    private void assertDeleteShift(EmployeePosition emp, Shift shift, ScheduleWorker scheduleWorker) {
        sb.spinnerLoader().loadingSpinnerPage().waitUntil("Страница не загрузилась", Matchers.not(DisplayedMatcher.displayed()), 10);
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertNull(
                scheduleWorker.getScheduleShiftElement(emp, shift.getDateTimeInterval().getStartDate()),
                "Смена все еще отображается");
        List<Shift> actualShifts = ShiftRepository.getShifts(emp, ShiftTimePosition.DEFAULT)
                .stream()
                .filter(e -> Objects.equals(e.getId(), shift.getId()))
                .collect(Collectors.toList());
        softAssert.assertTrue(actualShifts.size() == 0, "Смена не была удалена");
        softAssert.assertAll();
        Allure.addAttachment("Удаление смены",
                             String.format("Смена сотрудника %s за %s успешно удалена",
                                           emp, shift.getDateTimeInterval().getStartDate()));
    }

    @Step("Проверить, что смена была отредактирована")
    private void assertEditShift(EmployeePosition emp,
                                 DateTimeInterval dateTimeInterval, ScheduleWorker scheduleWorker) {
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertNotNull(
                scheduleWorker.getScheduleShiftElement(emp, dateTimeInterval.getStartDate()),
                "Смена не появилась на заданной дате");
        Shift shift = ShiftRepository.getShift(emp, dateTimeInterval.getStartDate(), null);
        softAssert.assertEquals(
                shift.getDateTimeInterval().toString(),
                dateTimeInterval.toString(),
                "Дата начала смены не совпадает"
        );
        Allure.addAttachment("Смена", shift.toString());
        softAssert.assertAll();
    }

    @Step("Проверить, что дата окончания смены была изменена")
    private void assertEditEndDateShift(EmployeePosition emp, LocalDate date,
                                        LocalDateTime changeDateTime, ScheduleWorker scheduleWorker) {
        sb.formEditForm().waitUntil(
                "Форма редактирования все еще отображается",
                Matchers.not(DisplayedMatcher.displayed()), 10
        );
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertNotNull(
                scheduleWorker.getScheduleShiftElement(emp, date),
                "Смена отображается на первоначальной дате"
        );
        Shift shift = ShiftRepository.getShift(emp, date, null);
        softAssert.assertEquals(shift.getDateTimeInterval().getEndDateTime(), changeDateTime, "Дата не совпадает");
        Allure.addAttachment("Смена", shift.toString());
        softAssert.assertAll();
    }

    @Step("Кликнуть на свободную ячейку графика сотрудника с именем {employeePosition} за {date}")
    private void clickOnEmptyCell(EmployeePosition employeePosition, LocalDate date, ScheduleWorker scheduleWorker) {
        LOG.info("Кликаем на свободную ячейку графика сотрудника с именем {} за {}", employeePosition, date);
        scheduleWorker.onEmptyCellClicker(employeePosition, date.getDayOfMonth(), false);
    }

    @Step("Кликнуть повторно на \"+\" ячейку графика")
    private void clickOnPlusCellOnGraph() {
        LOG.info("Кликаем повторно на \"+\" ячейку графика");
        sb.formLayout().greyAddShiftButton()
                .waitUntil(NO_PlUS_BUTTON, DisplayedMatcher.displayed(), 5);
        sb.formLayout().greyAddShiftButton().click();
        sb.formEditForm().waitUntil("Форма не была отображена", DisplayedMatcher.displayed(), 5);
        sb.spinnerLoader().loadingSpinnerInForm().waitUntil("Форма ещё не загрузилась", Matchers.not(DisplayedMatcher.displayed()), 5);
    }

    @Step("Кликнуть на смену сотрудника {emp.employee} за {date}")
    private void clickOnTargetShift(EmployeePosition emp, LocalDate date, ScheduleWorker scheduleWorker) {
        LOG.info("Кликаем на смену у {} за {}", emp, date.toString());
        AtlasWebElement targetShift = scheduleWorker.getScheduleShiftElement(emp, date);
        Assert.assertNotNull(targetShift, String.format("Элемент смены сотрудника %s за %s не найден", emp, date));
        targetShift.waitUntil("Смена не отобразилась", DisplayedMatcher.displayed(), 5);
        try {
            targetShift.click();
        } catch (WebDriverException e) {
            targetShift = scheduleWorker.getScheduleShiftElement(emp, date);
            scheduleWorker.makeShiftClickable(emp);
            targetShift.click();
        }
        sb.formEditForm().waitUntil("Панель редактирования смены не отобразилась",
                                    DisplayedMatcher.displayed(), 15);
    }

    @Step("Кликнуть на смену сотрудника {emp.employee} за {date}")
    private void clickOnTargetShiftPlanOrFact(EmployeePosition emp, LocalDate date, ScheduleWorker scheduleWorker, boolean workedShift) {
        LOG.info("Кликаем на смену у {} за {}", emp, date.toString());
        scheduleWorker.clickOnTargetShiftPlanOrFact(emp, date, workedShift);
        sb.formEditForm().waitUntil("Панель редактирования смены не отобразилась",
                                    DisplayedMatcher.displayed(), 5);
    }

    @Step("Проверить, что к смене был добавлен комментарий \"{name}\"")
    private void assertShiftCommentAdding(String name, Shift shift) {
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(sb.formEditForm().commentInputField().getAttribute(VALUE),
                                name, "Комментарий не появился на UI");
        softAssert.assertEquals(shift.refreshShift().getComment(), name, "Комментарий не появился в API");
        softAssert.assertAll();
    }

    @Step("Проверить, что к запросу был добавлен комментарий \"{name}\"")
    private void assertRequestCommentAdding(String name, ScheduleRequest request, int omId) {
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(sb.formEditForm().commentInputField().getAttribute(VALUE),
                                name, "Комментарий не появился на UI");
        softAssert.assertEquals(request.updateScheduleRequest(omId).getComment(), name, "Комментарий не появился в API");
        softAssert.assertAll();
    }

    @Step("В поле \"{timeType.name}\" смены выбрать время {time}")
    private void enterShiftTimeStartOrEnd(LocalTime time, TimeTypeField timeType) {
        String timeString = "Время";
        AtlasWebElement inputTimeShift;
        if (timeType == TimeTypeField.START_TIME) {
            inputTimeShift = sb.formEditForm().inputStartTimeShift(timeString);
        } else {
            inputTimeShift = sb.formEditForm().inputEndTimeShift(timeString);
        }
        sb.formEditForm().spinner().waitUntil("Спиннер всё ещё отображается", Matchers.not(DisplayedMatcher.displayed()), 10);
        inputTimeShift.click();
        inputTimeShift.clear();
        inputTimeShift.sendKeys(time.format(DateTimeFormatter.ofPattern("HH:mm")));
        LOG.info("В поле \"{}\" введено время {}", timeString, time);
    }

    @Step("В поле \"{dateType.name}\" доп. работы выбрать время {time}")
    private void enterAdditionalWorkTimeStartOrEnd(LocalTime time, DateTypeField dateType) {
        String dateTypeString = dateType.getName();
        sb.formEditForm().spinner().waitUntil("Спиннер всё ещё отображается", Matchers.not(DisplayedMatcher.displayed()), 10);
        AtlasWebElement input = sb.formEditForm().inputStartOrEndTimeAdditionalWork(dateTypeString);
        input.click();
        input.clear();
        input.sendKeys(time.format(DateTimeFormatter.ofPattern("HH:mm")));
        LOG.info("В поле \"{}\" введено время {}", dateTypeString, time);
    }

    @Step("Кликнуть на раскрывающийся список \"Тип\"")
    private void clickOnSelectRequestTypeButton() {
        sb.formEditForm().selectTypeButton().waitUntil("Кнопка не появилась", DisplayedMatcher.displayed(), 10);
        sb.formEditForm().selectTypeButton().click();
        LOG.info("Кликаем на раскрывающийся список \"Тип\"");
    }

    private void selectRequestType(ScheduleRequestType requestType) {
        selectRequestType(requestType, null);
    }

    @Step("В раскрывшемся списке выбрать \"{requestType.name}\"")
    private void selectRequestType(ScheduleRequestType requestType, IntervalType intervalType) {
        LOG.info("В раскрывшемся списке выбираем \"{}\"", requestType.getName());
        if (requestType == ScheduleRequestType.SHIFT) {
            sb.formEditForm().scheduleRequestButton().click();

        } else {
            ElementsCollection<AtlasWebElement> requestTypeList = sb.formEditForm().typeButtons();
            requestTypeList.waitUntil("Список типов запросов не загрузился",
                                      Matchers.not(requestTypeList.isEmpty()), 5);
            systemSleep(5);//без этого ожидания может быть выбран не тот тип запроса
            requestTypeList.stream()
                    .filter(extendedWebElement -> {
                        String s = extendedWebElement.getText().trim();
                        if (s.contains("\n")) {
                            s = s.substring(0, s.indexOf("\n"));
                        }
                        if (intervalType != null) {
                            return s.contains(requestType.getName() + "_" + intervalType.toString());
                        }
                        return s.contains(requestType.getName());
                    })
                    .findFirst()
                    .orElseThrow(() -> new AssertionError(REQUEST_OPTION_NOT_DISPLAYED))
                    .click();
        }
    }

    @Step("Навести курсор на смену сотрудника {employeePosition} за {date}, дождаться появления кнопки \"+\", нажать на нее")
    private void addRequestForShift(EmployeePosition employeePosition, LocalDate date, ScheduleWorker scheduleWorker) {
        AtlasWebElement targetShift = scheduleWorker.getScheduleShiftElement(employeePosition, date);
        Assert.assertNotNull(targetShift, String.format("Не найдено элемента расписания для сотрудника %s на дату %s", employeePosition, date));
        new Actions(sb.getWrappedDriver()).moveToElement(targetShift).perform();
        sb.formLayout().addRequestButton().waitUntil("Кнопка добавления запроса не отобразилась", DisplayedMatcher.displayed(), 5);
        sb.formLayout().addRequestButton().click();

        LOG.info("Кликнули на кнопку \"+\" над сменой у {} за {}", employeePosition, date);
        sb.formEditForm().waitUntil("Панель создания запроса не отобразилась",
                                    DisplayedMatcher.displayed(), 5);
    }

    @Step("Кликнуть на раскрывающийся список \"Периодичность\"")
    private void clickOnPeriodicitySelectButton() {
        sb.formListOfRequest().periodicitySelectButton().click();
    }

    @Step("В раскрывшемся списке выбрать \"{value.repeatType}\"")
    private void selectPeriodicity(Periodicity value) {
        sb.formListOfRequest().periodicityPanel()
                .waitUntil("Панель выбора переодичности не отобразилась", DisplayedMatcher.displayed(), 5);
        sb.formListOfRequest().periodicityTypeButtons()
                .waitUntil(Matchers.hasSize(Matchers.greaterThan(4)))
                .stream().filter(element -> element.getText().equals(value.getRepeatType()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("schedule message. Кнопки с выбранным типом не было в списке"))
                .click();
    }

    @Step("В поле \"Дата окончания повтора\" выбрать дату {date}")
    private void enterPeriodicityEndDate(LocalDate date) {
        LOG.info("Дата окончания повтора: {}", date);
        sb.formListOfRequest().dateEndInput().clear();
        sb.formListOfRequest().dateEndInput().sendKeys(date.format(UI_DOTS.getFormat()));
    }

    @Step("В поле \"{dateType.name}\" выбрать дату {date}")
    private void enterShiftDateStartOrEnd(LocalDate date, DateTypeField dateType) {
        LOG.info("В поле \"{}\" вводим дату {}", dateType.getName(), date);
        sb.formEditForm().spinner().waitUntil("Спиннер всё ещё отображается", Matchers.not(DisplayedMatcher.displayed()), 10);
        sb.formEditForm().dateStartOrEndInput(dateType.getName()).click();
        sb.formEditForm().dateStartOrEndInput(dateType.getName()).clear();
        sb.formEditForm().dateStartOrEndInput(dateType.getName()).sendKeys(date.format(UI_DOTS.getFormat()));
    }

    @Step("Проверить, что был сделан переход на страницу в масштабе {waitingScope.scopeName}")
    private void scopeChecker(ScopeType waitingScope) {
        ArrayList<AtlasWebElement> scopeElementsList = new ArrayList<>();
        scopeElementsList.add(sb.mainHeader().dayScope());
        scopeElementsList.add(sb.mainHeader().weekScope());
        scopeElementsList.add(sb.mainHeader().monthScope());

        ScopeType[] listOfScope = ScopeType.values();

        AtlasWebElement currentActiveElement = scopeElementsList.stream()
                .filter(element -> element.getAttribute("class").contains(ACTIVE))
                .findFirst()
                .orElseThrow(() -> new AssertionError("schedule message. Такого индекса нет"));
        String attribute = currentActiveElement.getAttribute("data-index");
        ScopeType currentScope = Arrays.stream(listOfScope)
                .filter(scope -> String.valueOf(scope.ordinal()).equals(attribute))
                .findFirst()
                .orElseThrow(() -> new AssertionError("schedule message. Такого элемента нет"));
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(currentScope, waitingScope,
                                "Отображение текущего масштаба не совпадает с тем на который мы перешли");
        //Для проверок количества временных промежутков
        LocalDate localDate = LocalDate.now();
        int numberOfDataIntervals = 0;
        int numberOfDataElements = 0;
        switch (currentScope) {
            case MONTH:
                numberOfDataElements = sb.formTopBar().dateAboveGraph().size();
                numberOfDataIntervals = localDate.lengthOfMonth();
                break;
            case WEEK:
                numberOfDataElements = sb.formTopBar().dateAboveGraph().size();
                numberOfDataIntervals = 7;
                break;
            case DAY:
                numberOfDataElements = sb.formTopBar().hourAboveGraph().size();
                numberOfDataIntervals = 24;
                break;
        }
        softAssert.assertEquals(numberOfDataElements, numberOfDataIntervals,
                                "Количество элементов дат не совпадает с предполагаемым на выбранном масштабе");
        //для проверки отображемой даты на UI
        LocalDate forAssertionDate = null;
        String dateFromUi = sb.formTopBar().currentTime().getText().replaceAll(":", "").trim();
        if (currentScope == ScopeType.MONTH || currentScope == ScopeType.WEEK) {
            Locale locale = new Locale("ru");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("LLLL").withLocale(locale);
            TemporalAccessor accessor = formatter.parse(dateFromUi);
            forAssertionDate = localDate.withMonth(accessor.get(ChronoField.MONTH_OF_YEAR));
        } else if (currentScope == ScopeType.DAY) {
            String monthValue = StringUtils
                    .substringBefore(StringUtils.substringAfter(dateFromUi, " "), " ");
            MonthsEnum month = Arrays.stream(MonthsEnum.values())
                    .filter(monthsEnum -> monthsEnum.getShortName().toLowerCase().contains(monthValue))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("schedule message. Такого элемента нет"));
            int day = Integer.parseInt(dateFromUi.replaceAll("\\D+", ""));
            String dayOfTheWeek = dateFromUi.substring(0, dateFromUi.indexOf(","));
            DayOfWeek dayWeek = Arrays.stream(DayOfWeek.values())
                    .filter(dayOfWeek -> dayOfWeek.getDisplayName(TextStyle.SHORT, new Locale("ru"))
                            .contains(dayOfTheWeek))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("schedule message. Такого элемента нет"));
            softAssert.assertEquals(dayWeek.ordinal() + 1, localDate.getDayOfWeek().getValue(),
                                    "Отображаемый день не совпадает с текущим");
            forAssertionDate = localDate.withDayOfMonth(day).withMonth(month.ordinal() + 1);
        }
        softAssert.assertEquals(forAssertionDate, localDate, "Отображаемая дата и текущая дата не совпадают");
        softAssert.assertAll();
    }

    @Step("Смена масштаба расписания на {scope.scopeName}")
    private void switchScope(ScopeType scope) {
        sb.mainHeader().dayScope(scope.ordinal()).click();
    }

    @Step("Навести курсор на последнюю отметку у \"{employeeName}\"")
    private void hoverRecord(String employeeName, int recordIndex) {
        for (int i = 0; i < sb.formLayout().employeeNameButtons().size(); i++) {
            if (sb.formLayout().employeeNameButtons().get(i).getText().equals(employeeName)) {
                new Actions(sb.getWrappedDriver()).moveToElement(sb.formLayout().record(i, recordIndex)).perform();
                break;
            }
        }
    }

    @Step("Проверить, что на всплывающем окне отображается текст \"Отметка: {time}\"")
    private void checkRecordTime(LocalTime time) {
        List<String> records = sb.formLayout().recordHints().waitUntil(Matchers.hasSize(Matchers.greaterThan(0)))
                .stream().map(webElement -> webElement.getText().trim()).collect(Collectors.toList());
        assertTrue(records.contains("Отметка: " + time.toString()), "Отметка со временем "
                + time + "не отобразилась на вспылывающем окне.\n" + "Отобразившиеся отметки: " + String.join(", ", records));
    }

    @Step("Нажать кнопку \"Изменить\"")
    private void commentsChangeButtonClick() {
        sb.formCommentsForm().changeButton().click();
        sb.formCommentsForm()
                .waitUntil("Форма комментариев все еще отображена",
                           Matchers.not(DisplayedMatcher.displayed()), 35);
        sb.spinnerLoader().loadingSpinnerPage().waitUntil("Страница не загрузилась", Matchers.not(DisplayedMatcher.displayed()), 30);
    }

    @Step("Ввести в поле логина сотрудника: {login}")
    private void sendEmployeeLogin(String login) {
        systemSleep(5); //иначе в поле пароля вводит логин, ожидание иконок и полей ввода ничего не дает
        sb.employeeDataMenu().employeeLoginField().click();
        sb.employeeDataMenu().employeeLoginField().clear();
        sb.employeeDataMenu().employeeLoginField().sendKeys(login);
    }

    @Step("Ввести в поле пароля сотрудника: {password}")
    private void sendEmployeePass(String password) {
        sb.employeeDataMenu().employeePassField().click();
        sb.employeeDataMenu().employeePassField().sendKeys(password);
    }

    @Step("Ввести в поле подтверждения пароля сотрудника: {password}")
    private void sendEmployeeConformPass(String password) {
        sb.employeeDataMenu().employeeConformPassField().click();
        sb.employeeDataMenu().employeeConformPassField().sendKeys(password);
    }

    @Step("Проверить, что у сотрудника {empName} логин был изменен на {login}")
    private void assertEmployeeLogin(String empName, String login) {
        int omNumber = getOrgIdFromUrl();
        String tempId = EmployeePositionRepository.getEmployeePositions(omNumber).stream()
                .filter(position -> position.getEmployee().getShortName().equals(empName))
                .findFirst()
                .orElseThrow(() -> new AssertionError("schedule message. Подходящий сотрудник в апи не найден"))
                .getEmployee().getLink(REL_ACCOUNT).toString().split("/")[6];
        JSONObject user = getJsonFromUri(Projects.WFM, URL_SB, makePath(USERS, tempId));
        String loginApi = (String) user.get("username");
        Assert.assertEquals(loginApi, login);
    }

    @Step("Проверить, что появилась новая версия графика после редактирования смены")
    private void assertCreateNewVersion(int numberOfRostersBefore, int omId) {
        SoftAssert softAssert = new SoftAssert();
        int numberOfRostersAfter = RosterRepository.getRosters(omId).size();
        softAssert.assertEquals(numberOfRostersBefore, numberOfRostersAfter - 1, "Ростер не добавился");
        int numberOfVersionOnUI = sb.formTopBar().listOfTimeTablesVersions().size();
        softAssert.assertEquals(numberOfRostersAfter, numberOfVersionOnUI + 1, "Версия на сайте не добавилась");
        softAssert.assertAll();
    }

    @Step("Создать дополнительную смену сотруднику с именем {employeePosition}")
    private void changeRandomShift(EmployeePosition employeePosition) {
        PresetClass.presetForMakeShift(employeePosition, false, ShiftTimePosition.FUTURE);
    }

    @Step("Нажать на кнопку перемещения смены")
    private void clickReplaceShiftButton() {
        sb.formLayout().replaceShift().click();
        sb.spinnerLoader().loadingSpinnerPage().waitUntil("Спиннер загрузки все еще отображается",
                                                          Matchers.not(DisplayedMatcher.displayed()), 10);
    }

    @Step("Нажать иконку кнопки \"Дублировать\"")
    private void clickDuplicateShiftButton() {
        sb.formLayout().duplicateShift().waitUntil("Кнопка \"Дублировать\" не отобразилась", DisplayedMatcher.displayed(), 10);
        sb.formLayout().duplicateShift().click();
        sb.spinnerLoader().loadingSpinnerPage().waitUntil("Спиннер загрузки все еще отображается",
                                                          Matchers.not(DisplayedMatcher.displayed()), 30);
    }

    @Step("Нажать на кнопку удаления смен")
    private void deleteMassShift() {
        LOG.info("Нажимаем на кнопку удаления смен");
        sb.formLayout().deleteMassShift().click();
        sb.formLayout().popUpForMassDeleteShift()
                .waitUntil("Поп-ап с уведомлением о удалении смен не был отображен",
                           DisplayedMatcher.displayed(), 10);
        sb.spinnerLoader().loadingSpinnerPage().waitUntil("Спиннер загрузки все еще отображается",
                                                          Matchers.not(DisplayedMatcher.displayed()), 10);
    }

    @Step("Нажать на кнопку дублирования смен и продублировать смены на сотрудника с именем {employeePosition}")
    private void batchCopyShift(EmployeePosition employeePosition, ScheduleWorker scheduleWorker, LocalDate start, boolean expectError) {
        String logText = "Дублируем смены за {} и {} числа на сотрудника {}";
        scheduleWorker.manipulationShiftDuplicate(employeePosition, start);
        batchActionShift(employeePosition, start, expectError, logText);
    }

    @Step("Нажать на кнопку перемещения смен и перенести смены на сотрудника с именем {employeePosition.employee}")
    private void batchTransferShift(EmployeePosition employeePosition, ScheduleWorker scheduleWorker, LocalDate start, boolean expectError) {
        String logText = "Перемещаем смены за {} и {} числа на сотрудника {}";
        scheduleWorker.manipulationShiftTransfer(employeePosition, start);
        batchActionShift(employeePosition, start, expectError, logText);
    }

    /**
     * Метод содержит общий код для массового дублирования и перемещения смен, вызывается соответствующими методами
     *
     * @param employeePosition позиция сотрудника, на которого копируем/перемещаем
     * @param start            первая из дат, с которыми совершаем действие
     * @param expectError      ожидается ли ошибка при выполнении действия
     * @param logText          текст, выводящийся в лог
     */
    private void batchActionShift(EmployeePosition employeePosition, LocalDate start, boolean expectError, String logText) {
        LOG.info(logText, start, start.plusDays(1), employeePosition);
        if (expectError) {
            sb.formLayout().popUpShiftAndRequestOverlap().should("Сообщение об ошибке не отображается", DisplayedMatcher.displayed(), 5);
        }
        sb.spinnerLoader().loadingSpinnerPage().waitUntil("Спиннер загрузки все еще отображается",
                                                          Matchers.not(DisplayedMatcher.displayed()), 20);
    }

    @Step("Переместить одну смену от {employeePositionFrom} за {dateFrom} к сотруднику с именем {employeePositionTo} за {dateTo}")
    private void transferOneShift(EmployeePosition employeePositionFrom, EmployeePosition employeePositionTo,
                                  LocalDateTime dateFrom, LocalDateTime dateTo, ScheduleWorker worker) {
        String nameFrom = employeePositionFrom.getEmployee().getShortName();
        String nameTo = employeePositionTo.getEmployee().getShortName();
        LOG.info("Перемещаем смены: {} за {} число на {} число сотрудника: {}", nameFrom, dateFrom, dateTo, nameTo);
        if (nameFrom.equals(nameTo) && dateFrom.toLocalDate().isEqual(dateTo.toLocalDate())) {
            int days = LocalDateTools.getLastDate().getDayOfMonth();
            int today = LocalDate.now().getDayOfMonth();
            if (days == today) {
                Assert.fail(NO_VALID_DATE + "Операция не может быть совершена, потому что сегодня последний день месяца");
            } else {
                Assert.fail(NO_TEST_DATA + "Ошибка выбора дней для операции, выбраны одинаковые дни у одного и того же сотрудника");
            }
        }
        worker.exchangeShift(employeePositionFrom, employeePositionTo, dateFrom.toLocalDate(), dateTo.toLocalDate().getDayOfMonth());
    }

    @Step("Выделить две смены через Shift у сотрудника с именем {employeePosition}")
    private void selectTwoShift(EmployeePosition employeePosition, Shift[] shifts, ScheduleWorker scheduleWorker) {
        LocalDate firstDate = shifts[0].getDateTimeInterval().getStartDate();
        LocalDate secondDate = shifts[1].getDateTimeInterval().getStartDate();
        LOG.info("Выделяем смены у: {} за {} и {} числа", employeePosition, firstDate, secondDate);
        scheduleWorker.selectTwoShifts(employeePosition, new LocalDate[]{firstDate, secondDate});
    }

    @Step("Выбрать случайную должность из выпадающего списка")
    private void selectRandomJobTitle() {
        sb.formEditForm().jobTitleOpen().click();
        AtlasWebElement jobTitle = sb.formEditForm().jobTitleList().stream()
                .findAny()
                .orElseThrow(() -> new AssertionError("Список должностей пустой"));
        jobTitle.click();
    }

    @Step("Выбрать случайную причину привлечения из выпадающего списка")
    private void selectRandomEmployeeAttractionReason() {
        sb.formEditForm().hiringReasonInput().click();
        AtlasWebElement hiringReasonOptions = sb.formEditForm().hiringReasonOptions().stream()
                .findAny()
                .orElseThrow(() -> new AssertionError("Список причин привлечения сотрудника пустой"));
        hiringReasonOptions.click();
    }

    @Step("Проверить перемещение смен")
    private void assertTransferShift(EmployeePosition firstEmp, EmployeePosition secondEmp, Shift firstShift,
                                     Shift secondShift, ScheduleWorker scheduleWorker) {
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertNotNull(
                scheduleWorker.getScheduleShiftElement(firstEmp, firstShift.getDateTimeInterval().getStartDate()),
                "Смена не отображается на первоначальной дате"
        );
        softAssert.assertNotNull(
                scheduleWorker.getScheduleShiftElement(secondEmp, secondShift.getDateTimeInterval().getStartDate()),
                "Смена не отображается на дате смены для обмена"
        );
        Shift newFirstShift = ShiftRepository.getShift(firstEmp, firstShift.getDateTimeInterval().getStartDate(), null);
        Shift newSecondShift = ShiftRepository.getShift(secondEmp, secondShift.getDateTimeInterval().getStartDate(), null);
        secondShift.assertSameTime(newFirstShift);
        firstShift.assertSameTime(newSecondShift);
        softAssert.assertAll();
    }

    @Step("Проверить, что смены сотрудника с именем {firstEmp} были продублированы сотруднику с именем {secondEmp}")
    private void assertDuplicateShift(EmployeePosition firstEmp, Shift shiftFrom, EmployeePosition secondEmp,
                                      LocalDate dayTo, ScheduleWorker scheduleWorker) {
        SoftAssert softAssert = new SoftAssert();
        String textAssertion1 = String.join(
                " ",
                "Смена не была скопирована сотруднику с именем:",
                secondEmp.toString(),
                "на",
                dayTo.toString()
        );
        String textAssertion2 = String.join(
                " ",
                "Скопированная смена исчезла с прежнего места",
                firstEmp.toString(),
                "за",
                shiftFrom.getDateTimeInterval().getStartDate().toString()
        );
        softAssert.assertNotNull(scheduleWorker.getScheduleShiftElement(secondEmp, dayTo), textAssertion1);
        softAssert.assertNotNull(
                scheduleWorker.getScheduleShiftElement(firstEmp, shiftFrom.getDateTimeInterval().getStartDate()),
                textAssertion2
        );
        Shift duplicateShift = ShiftRepository.getShift(secondEmp, dayTo, null);
        shiftFrom.assertSameTime(duplicateShift);
        softAssert.assertAll();
        Allure.addAttachment("Смена", duplicateShift.toString());
    }

    @Step("Проверить, что смена была перенесена от сотрудника с именем {firstEmp} сотруднику с именем {secondEmp}")
    private void assertTransferShiftToSameEmployee(Shift firstShift, EmployeePosition firstEmp, EmployeePosition secondEmp,
                                                   LocalDate dateTo, ScheduleWorker scheduleWorker) {
        SoftAssert softAssert = new SoftAssert();
        String textAssertion1 = String.join(
                " ",
                "Не появилась ожидаемая смена после переноса за",
                dateTo.toString(),
                "у",
                secondEmp.toString()
        );
        String textAssertion2 = String.join(
                " ",
                "Перенесенная смена всё еще находится на прежнем месте у",
                firstEmp.toString(),
                "за",
                firstShift.getDateTimeInterval().getStartDate().toString()
        );
        softAssert.assertNotNull(
                scheduleWorker.getScheduleShiftElement(secondEmp, dateTo),
                textAssertion1
        );
        softAssert.assertNull(
                scheduleWorker.getScheduleShiftElement(firstEmp, firstShift.getDateTimeInterval().getStartDate()),
                textAssertion2
        );
        Shift newShift = ShiftRepository.getShift(secondEmp, dateTo, null);
        softAssert.assertNotNull(newShift, "Смена не была перенесена");
        softAssert.assertEquals(firstShift.getDateTimeInterval().toTimeInterval().toString(),
                                newShift.getDateTimeInterval().toTimeInterval().toString(), "Время начала смены не совпало с временем исходной");
        softAssert.assertAll();
        Allure.addAttachment("Смена", newShift.toString());
    }

    @Step("Проверить массовое удаление смен у сотрудника с именем {employeePosition}")
    private void assertMassDeleteShift(EmployeePosition employeePosition, Shift[] shifts, ScheduleWorker scheduleWorker) {
        SoftAssert softAssert = new SoftAssert();
        LocalDate firstShiftDate = shifts[0].getDateTimeInterval().getStartDate();
        LocalDate secondShiftDate = shifts[1].getDateTimeInterval().getStartDate();
        softAssert.assertNull(scheduleWorker.getScheduleShiftElement(employeePosition, firstShiftDate),
                              "Первая удаленная смена все еще отображается");
        softAssert.assertNull(scheduleWorker.getScheduleShiftElement(employeePosition, secondShiftDate),
                              "Вторая удаленная смена все еще отображается");
        softAssert.assertNull(ShiftRepository.getShift(employeePosition, firstShiftDate, null),
                              "Первая удаленная смена не исчезла из API");
        softAssert.assertNull(ShiftRepository.getShift(employeePosition, secondShiftDate, null),
                              "Вторая удаленная смена не исчезла из API");
        softAssert.assertAll();
    }

    @Step("Проверить, что у сотрудника с именем {employeePosition} нет перемещенных смен ")
    private void assertMassTransferEmptyShifts(EmployeePosition employeePosition, Shift[] shifts, ScheduleWorker scheduleWorker) {
        LocalDate firstShiftDate = shifts[0].getDateTimeInterval().getStartDate();
        LocalDate secondShiftDate = shifts[1].getDateTimeInterval().getStartDate();
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertNull(scheduleWorker.getScheduleShiftElement(employeePosition, firstShiftDate),
                              "Первая удаленная смена все отображается");
        softAssert.assertNull(scheduleWorker.getScheduleShiftElement(employeePosition, secondShiftDate),
                              "Вторая удаленная смена все отображается");
        Shift firstShiftOld = ShiftRepository.getShift(employeePosition, firstShiftDate, null);
        Shift secondShiftOld = ShiftRepository.getShift(employeePosition, secondShiftDate, null);
        softAssert.assertNull(firstShiftOld, "Первая смена осталась у сотрудника");
        softAssert.assertNull(secondShiftOld, "Вторая смена осталась у сотрудника");
        softAssert.assertAll();
    }

    //параметр в методе используется для отчета
    @Step("Проверить массовые манипуляции со сменами сотрудника {firstEmp} к сотруднику с именем {secondEmp}")
    private void assertMassManipulateShift(EmployeePosition firstEmp, EmployeePosition secondEmp, Shift[] shifts,
                                           LocalDate[] dates) {
        SoftAssert softAssert = new SoftAssert();
        Shift newFirstShift = ShiftRepository.getShift(secondEmp, dates[0], null);
        Shift newSecondShift = ShiftRepository.getShift(secondEmp, dates[1], null);
        if (newFirstShift == null || newSecondShift == null) {
            throw new AssertionError("Новые смены не появились в API");
        }
        Allure.addAttachment("Смена", newFirstShift.toString());
        Allure.addAttachment("Смена", newSecondShift.toString());
        shifts[0].assertSameTime(newFirstShift);
        shifts[1].assertSameTime(newSecondShift);
        softAssert.assertAll();
    }

    @Step("Проверить массовое копирование смен от сотрудника с именем {firstEmp} сотруднику с именем {secondEmp}")
    private void assertMassDuplicateShifts(EmployeePosition firstEmp, Shift[] shifts,
                                           EmployeePosition secondEmp, ImmutablePair<LocalDate, LocalDate> dates,
                                           ScheduleWorker worker) {
        assertDuplicateShift(firstEmp, shifts[0], secondEmp, dates.getLeft(), worker);
        assertDuplicateShift(firstEmp, shifts[1], secondEmp, dates.getRight(), worker);
    }

    @Step("Нажать на стрелку в параметре из списка : {matchParameter.nameParam}")
    private void chooseMatchParam(MathParameters matchParameter) {
        sb.employeeDataMenu().matchParameters(matchParameter.getNameParam()).waitUntil(DisplayedMatcher.displayed());
        sb.employeeDataMenu().matchParameters(matchParameter.getNameParam()).click();
    }

    @Step("Проверить, что в поле \"Дата начала\" дата меняется на первое число текущего месяца.")
    private void assertCantSendWrongValue(LocalDate dateExpected) {
        String actual = sb.formEditForm().dateStartOrEndInput(DateTypeField.START_DATE.getName()).getAttribute(VALUE);
        String expected = dateExpected.format(UI_DOTS.getFormat());
        Assert.assertEquals(actual, expected, "Дата не изменилась на первое число текущего месяца");
    }

    @Step("Проверить, что изменения не сохраняются и появляется поп-ап с предупреждением об ошибке")
    private void assertErrorEditShift(Shift shiftBefore) {
        sb.spinnerLoader().popUp()
                .should("Поп-ап с сообщением об ошибке не отобразился", DisplayedMatcher.displayed(), 10);
        SoftAssert softAssert = new SoftAssert();
        String popUpText = sb.spinnerLoader().popUp().getText();
        softAssert.assertTrue(popUpText.contains("Неизвестная ошибка"));
        Shift shiftAfter = shiftBefore.refreshShift();
        //нельзя использовать assertEquals напрямую, так как метод не использует метод сравнения класса Shift
        softAssert.assertTrue(shiftAfter.equals(shiftBefore), "Смена изменилась, хотя изменения должны быть отклонены");
        softAssert.assertAll();
    }

    /**
     * Проверяет есть ли поле ввода комментария при изменении смены и если оно есть то вставляет комментарий
     */
    private void checkingForComments(String comment) {
        systemSleep(0.5); //метод используется в неактуальных тестах
        boolean commentsExist = false;
        try {
            commentsExist = sb.formEditForm().commentInputField().isDisplayed();
        } catch (org.openqa.selenium.NoSuchElementException ignored) {
        }
        if (commentsExist) {
            clickOnCommentMenu();
            clickOnComment(comment);
        }
    }

    @Step("Проверить комментарий при отклонении графика")
    private void assertAddRejectComment(Roster rosterBefore, String comment) {
        Roster rosterAfter = rosterBefore.refreshRoster();
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(rosterAfter.getDescription(), comment,
                                "Введенный комментарий не совпадает с комментарием в API");
        ElementsCollection<AtlasWebElement> extendedWebElements = sb.formCommentsForm().rosterVersionsList();
        int order;
        for (order = 0; order < extendedWebElements.size(); order++) {
            String value = extendedWebElements.get(order).getAttribute(VALUE);
            if (value.equals(String.valueOf(rosterAfter.getVersion()))) {
                break;
            }
        }
        String commentText = sb.formCommentsForm().inputsCommentaryRoster().get(order).getAttribute(VALUE);
        softAssert.assertEquals(commentText, comment, "Введенный комментарий не совпадает с комментарием на UI");
        softAssert.assertAll();
    }

    @Step("Проверить, что поле \"Причина отклонения\" подсвечивается красным, появляется предупреждение о том, что поле не может быть пустым")
    private void assertErrorInInputField(Roster rosterBefore) {
        sb.rejectPublishingRosterDialog().errorInput()
                .should("Поле ввода не подсветилось красным с сообщением об ошибке",
                        TextMatcher.text(Matchers.containsString("не может быть пустым")), 5);
        assertRostersDescriptionNotChanged(rosterBefore);
    }

    @Step("Проверить, что окно \"Причина отклонения\" закрывается. Изменения не сохраняются")
    private void assertCancelRejectComment(Roster rosterBefore) {
        sb.rejectPublishingRosterDialog()
                .should("Окно \"Причина отклонения\" не закрылось",
                        Matchers.not(DisplayedMatcher.displayed()), 5);
        assertRostersDescriptionNotChanged(rosterBefore);
    }

    /**
     * Проверка того что у ростеров оодинаковые комментарии и статус "утверждения"
     */
    private void assertRostersDescriptionNotChanged(Roster rosterBefore) {
        Roster rosterAfter = rosterBefore.refreshRoster();
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(rosterAfter.getDescription(), rosterBefore.getDescription(), "Добавился комментарий");
        softAssert.assertEquals(rosterAfter.isOnApproval(), rosterAfter.isOnApproval(),
                                "График потерял статус \"На утверждении\"");
        softAssert.assertAll();
    }

    @Step("Ввести комментарий в окне отклонения графика работы")
    private void sendCommentaryInRejectRoster(String comment) {
        sb.rejectPublishingRosterDialog().commentaryInput().sendKeys(comment);
    }

    @Step("Нажать кнопку \"Сохранить\" в форме отклонения графика работы")
    private void saveCommentaryInRejectRoster() {
        sb.rejectPublishingRosterDialog().saveButton().click();
    }

    @Step("Нажать кнопку \"Отменить\" в форме отклонения графика работы")
    private void cancelCommentaryInRejectRoster() {
        sb.rejectPublishingRosterDialog().cancelButton().click();
    }

    @Step("Навести курсор на количество часов у сотрудника: {employeePosition}")
    private void hoverOnEmployeeHours(EmployeePosition employeePosition) {
        LOG.info("Наводим курсор на часы сотрудника {}", employeePosition);
        AtlasWebElement empPlanWorkingHours = sb.formLayout().employeePlanWorkingHours(employeePosition.getId());
        AtlasWebElement employeeWorkingHoursPopUp = sb.formLayout().employeeWorkingHoursPopUp();
        empPlanWorkingHours.waitUntil("Часы сотрудника не отображаются", DisplayedMatcher.displayed(), 5);
        try {
            new Actions(sb.getWrappedDriver()).moveToElement(empPlanWorkingHours).perform();
            employeeWorkingHoursPopUp.waitUntil("Всплывающий элемент с количеством часов у сотрудника не отобразился",
                                                DisplayedMatcher.displayed(), 5);
        } catch (WaitUntilException ex) {
            new Actions(sb.getWrappedDriver()).moveToElement(empPlanWorkingHours).perform();
            employeeWorkingHoursPopUp.waitUntil("Всплывающий элемент с количеством часов у сотрудника не отобразился",
                                                DisplayedMatcher.displayed(), 5);
        }
        ElementsCollection<AtlasWebElement> list = sb.formLayout().linesInEmployeeWorkingHours();
        new Actions(sb.getWrappedDriver()).moveToElement(list.get(list.size() - 1));
        AtlasWebElement employeeFactWorkingHours = sb.formLayout().employeeFactWorkingHours(employeePosition.getId());
        try {
            new Actions(sb.getWrappedDriver()).moveToElement(employeeFactWorkingHours).perform();
            employeeWorkingHoursPopUp.waitUntil("Всплывающий элемент с количеством часов у сотрудника не отобразился",
                                                DisplayedMatcher.displayed(), 5);
        } catch (WaitUntilException ex) {
            new Actions(sb.getWrappedDriver()).moveToElement(employeeFactWorkingHours).perform();
            employeeWorkingHoursPopUp.waitUntil("Всплывающий элемент с количеством часов у сотрудника не отобразился",
                                                DisplayedMatcher.displayed(), 5);
        }
        Allure.addAttachment("Имя сотрудника", "text/plain", "Выбран сотрудник с именем: " + employeePosition);
    }

    @Step("Кликнуть шеврон напротив статуса доп. работы")
    private void clickChevronAddWorkStatus() {
        sb.formEditForm().additionalWorkStatusChevron().click();
    }

    @Step("Выбрать статус доп. работы \"{status}\"")
    private void selectAddWorkStatus(String status) {
        sb.formEditForm().statusName(status).should(String.format("Статус \"%s\" отсутствует в списке", status),
                                                    DisplayedMatcher.displayed(), 5);
        sb.formEditForm().statusName(status).click();
    }

    /**
     * Проверяет, что во всплывающей подсказке при наведении на часы сотрудника в строках о ночном количестве часов
     * отображается число >= продолжительности ночной смены
     *
     * @param shift ночная смена, созданная в пресете к тесту
     * @return пара значений: пройдена ли проверка и строка для ассерта
     */
    private ImmutablePair<Boolean, String> checkHoursForNightShift(Shift shift, int omId) {
        String line;
        DateTimeInterval shiftInterval = shift.getDateTimeInterval();
        LocalDate date = shiftInterval.getStartDate();
        Roster workedRoster = RosterRepository.getWorkedRosterThisMonth(omId);
        if (workedRoster != null && date.isBefore(LocalDate.now())) {
            line = InEmployeeWorkingHours.NIGHT_HOURS_AMOUNT_FACT.getLineName();
        } else {
            line = InEmployeeWorkingHours.NIGHT_HOURS_AMOUNT.getLineName();
        }
        AtlasWebElement hoursElement = sb.formLayout().numberOfHoursInEmployeeToolTip(line);
        assert hoursElement != null;
        String hours = hoursElement.getText();
        int hoursUI = Integer.parseInt(hours.substring(hours.lastIndexOf(" ") + 1));

        String string = SystemPropertyRepository.getSystemProperty(SystemProperties.SCHEDULE_BOARD_NIGHT_HOURS_LIMITS).getValue().toString();
        string = string.substring(1, string.indexOf("]"));
        int time1 = Integer.parseInt(string.substring(0, string.indexOf(",")));
        int time2 = Integer.parseInt(string.substring(string.indexOf(" ") + 1));
        LocalDateTime timeStart = LocalDateTime.of(shiftInterval.getStartDate(), LocalTime.of(time1, 0));
        LocalDateTime timeEnd = LocalDateTime.of(shiftInterval.getEndDate(), LocalTime.of(time2, 0));
        DateTimeInterval interval = new DateTimeInterval(timeStart, timeEnd);
        int apiNightHours = shiftInterval.lengthOfIntersectionWith(interval);
        Allure.addAttachment("Проверка количества ночных часов на UI",
                             String.format("В пресете была создана смена продолжительностью %d часов. На UI отображается %d часов. \n" +
                                                   "Внимание: если на UI часов больше, чем в созданной смене, проверка считается пройденной.", apiNightHours, hoursUI));
        return new ImmutablePair<>(hoursUI >= apiNightHours,
                                   String.format("Количество часов в строке \"%s\" (%s ч) меньше продолжительности добавленной в пресете смены (%s ч)",
                                                 line, hoursUI, apiNightHours));
    }

    @Step("Проверить, что информация сотрудника отображается во всплывающей подсказке при системной настройке {property} со значением {settingValue}")
    private void assertDisplayInfoInEmployeeTooltip(SystemProperties property, boolean settingValue, int omId,
                                                    List<InEmployeeWorkingHours> uiLines, EmployeePosition employeePosition, Shift shift) {
        List<String> expectedLines = uiLines.stream().map(InEmployeeWorkingHours::getLineName).sorted().collect(Collectors.toList());
        Allure.description(String.format("Системная настройка: %s\n\nЗначение системной настройки: %s\n\nПроверяемые строки на UI: %s",
                                         property.getKey(), settingValue, expectedLines));
        SoftAssert softAssert = new SoftAssert();
        if (property.equals(SystemProperties.SCHEDULE_BOARD_SHOW_RATE)) {
            boolean containsTag = checkTag(employeePosition.getId(), "Ставка", false);
            if (settingValue) {
                softAssert.assertTrue(containsTag, "Под именем сотрудника нет бейджа со ставкой");
            } else {
                softAssert.assertFalse(containsTag, "Под именем сотрудника есть бейдж со ставкой");
            }
        }
        if (property.equals(SystemProperties.SCHEDULE_BOARD_NIGHT_HOURS_INDICATOR) && settingValue) {
            ImmutablePair<Boolean, String> hoursCheck = checkHoursForNightShift(shift, omId);
            softAssert.assertTrue(hoursCheck.left, hoursCheck.right);
        }
        List<String> linesOnUI = sb.formLayout().linesInEmployeeWorkingHours().stream()
                .map(AtlasWebElement::getText)
                .sorted()
                .collect(Collectors.toList());
        if (settingValue) {
            for (String line : expectedLines) {
                softAssert.assertTrue(linesOnUI.contains(line),
                                      String.format("Всплывающее окно с информацией о часах сотрудника не содержит строку \"%s\"", line));
            }
        } else {
            for (String line : expectedLines) {
                softAssert.assertFalse(linesOnUI.contains(line),
                                       String.format("Всплывающее окно с информацией о часах сотрудника содержит строку \"%s\"", line));
            }
        }
        Allure.addAttachment("Проверяемые строки", expectedLines.toString());
        Allure.addAttachment("Строки на UI", linesOnUI.toString());
        softAssert.assertAll();
    }

    @Step("Проверить, что отображается статус графика \"{graphStatus.statusName}\"")
    private void assertViewPublishStatus(GraphStatus graphStatus) {
        String planTitle = getPlanTitle().right;
        ElementsCollection<AtlasWebElement> colorLinesOnTopSchedule = sb.formLayout().colorLinesOnTopSchedule();
        AtlasWebElement colorElement = null;
        int size = colorLinesOnTopSchedule.size();
        if (size == 1) {
            colorElement = colorLinesOnTopSchedule.get(0);
        } else if (size == 2) {
            colorElement = colorLinesOnTopSchedule.get(1);
        } else {
            Assert.fail("Количество элементов статуса публикации графиков = " + size + ", а должно быть 1 или 2");
        }
        String expectedColorStatus = "";
        switch (graphStatus) {
            case PUBLISH:
                expectedColorStatus = "default";
                break;
            case NOT_PUBLISH:
            case ON_APPROVAL:
                expectedColorStatus = "not-published";
        }
        String colorStatus = colorElement.getAttribute("class");
        String statusText = planTitle.replaceAll("\\.", "").trim();
        SoftAssert softAssert = new SoftAssert();
        String statusName = graphStatus.getStatusName();
        softAssert.assertTrue(statusName.contains(statusText), "Ожидали статус графика \"" + statusName
                + "\", отображается: " + statusText);
        softAssert.assertTrue(colorStatus.contains(expectedColorStatus), "Цвет строки статуса не совпадает с ожидаемым");
        softAssert.assertAll();
        Allure.addAttachment("Статус публикации", "Ожидали статус графика \"" + statusName
                + "\", отображается: " + statusText + ". \nТекст статуса может быть не полным при приближении к концу месяца.");
    }

    /**
     * Метод выбирает случайную группу позиций и возвращает список активных сотрудников которые есть под этой группой
     *
     * @return слева название группы позиции, справа список имён сотрудников
     */
    private ImmutablePair<String, List<String>> getRandomPositionGroupEmployees(int orgUnitId) {
        List<EmployeePosition> employeePositions = EmployeePositionRepository.getEmployeePositions(orgUnitId);
        Set<Integer> actualEmployeeGroups = employeePositions.stream()
                .map(EmployeePosition::getPosition)
                .map(Position::getPositionGroupId)
                .collect(Collectors.toSet());
        actualEmployeeGroups.remove(0);
        Integer groupId = actualEmployeeGroups.stream()
                .skip(new Random().nextInt(actualEmployeeGroups.size()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Не были найдены группы позиций в оргюните № " + orgUnitId));
        String groupName = getPositionGroups().get(groupId);
        List<String> activeEmployees = employeePositions.stream()
                .filter(em -> em.getPosition().getPositionGroupId() == groupId)
                .map(EmployeePosition::getEmployee)
                .map(Employee::getShortName).collect(Collectors.toList());
        Allure.addAttachment("Выбор группы позиции", "Была выбрана случайная группа позиций: " + groupName);
        return new ImmutablePair<>(groupName, activeEmployees);
    }

    /**
     * Метод возвращает мэп с id группы и списком сотрудников в данной группе
     */
    private Map<Integer, List<String>> getGroupEmployeesMap(List<EmployeePosition> employeePositions) {
        Map<Integer, List<String>> groupsWithEmpl = new HashMap<>();
        Set<Integer> actualEmployeeGroups = employeePositions.stream()
                .map(EmployeePosition::getPosition)
                .map(Position::getPositionGroupId)
                .collect(Collectors.toSet());
        actualEmployeeGroups.remove(0);
        for (Integer groupId : actualEmployeeGroups) {
            List<String> activeEmployees = employeePositions.stream()
                    .filter(em -> em.getPosition().getPositionGroupId() == groupId)
                    .map(EmployeePosition::getEmployee)
                    .map(Employee::getShortName).collect(Collectors.toList());
            groupsWithEmpl.put(groupId, activeEmployees);
        }
        return groupsWithEmpl;
    }

    /**
     * Метод возвращает список активных сотрудников которые есть под указанной группой
     *
     * @return слева название группы позиции, справа список имён сотрудников
     */
    private ImmutablePair<String, List<String>> getPositionGroupAndEmployeeNames(int groupId, List<EmployeePosition> employeePositions) {
        String groupName = getPositionGroups().get(groupId);
        List<String> activeEmployees = employeePositions.stream()
                .filter(em -> em.getPosition().getPositionGroupId() == groupId)
                .map(EmployeePosition::getEmployee)
                .map(Employee::getShortName).collect(Collectors.toList());
        Allure.addAttachment("Выбор группы позиции", "Была выбрана случайная группа позиций: " + groupName);
        return new ImmutablePair<>(groupName, activeEmployees);
    }

    @Step("Проверить документ после нажатия на \"Скачать\"")
    private void assertForDownloadingPath(FileDownloadCheckerForScheduleBoard checker, Role role) {
        systemSleep(10); //метод используется в неактуальных тестах
        SoftAssert softAssert = new SoftAssert();
        HttpResponse httpResponse = checker.downloadResponse(role, TypeOfAcceptContent.BASIC);
        assertStatusCode(httpResponse, 200, checker.getDownloadLink().toString());
        //Определение типа контента
        CustomTools.removeFirstWindowHandler(sb);
        URI urlAddress = URI.create(sb.getWrappedDriver().getCurrentUrl());
        URI checkerUri = checker.getDownloadLink();
        Allure.addAttachment("Скачивание PDF", "Ожидаемая ссылка: " + checkerUri
                + ", ссылка, которая сформировалась в новой странице после нажатия на печать: " + urlAddress);
        softAssert.assertTrue(EqualUri.areEquals(urlAddress, checkerUri), "Ссылки для скачивания файла не совпали," +
                " адрес страницы: " + urlAddress + ", а ожидалось: " + checkerUri);
        softAssert.assertAll();
    }

    @Step("Проверить документ после нажатия на \"Скачать\"")
    private void assertForDownloadingPath(FileDownloadCheckerForScheduleBoard checker, URI urlAddress, Role role) {
        SoftAssert softAssert = new SoftAssert();
        HttpResponse httpResponse = checker.downloadResponse(role, TypeOfAcceptContent.BASIC);
        assertStatusCode(httpResponse, checker.getDownloadLink().toString());
        URI checkerUri = checker.getDownloadLink();

        Allure.addAttachment("Скачивание PDF", "Ожидаемая ссылка: " + checkerUri.getHost() + checkerUri.getPath() + "?" + checkerUri.getQuery()
                + ", ссылка, которая сформировалась в новой странице после нажатия на печать: " + urlAddress);
        softAssert.assertTrue(EqualUri.areEquals(urlAddress, checkerUri), "Ссылки для скачивания файла не совпали," +
                " адрес страницы: " + urlAddress + ", а ожидалось: " + checkerUri);
        softAssert.assertAll();
    }

    /**
     * Обновляет текущее окно. При SystemProperties.ROSTER_QUIT_TAB_NOTICE=true
     * если появляется всплывающее alert окно, то подтверждает изменения, и обновляет страницу.
     * Если не появляется, просто обновляет страницу.
     */
    private void refreshPageAndAcceptAlertWindow() {
        LOG.info("Обновляем текущее окно");
        sb.getWrappedDriver().navigate().refresh();
        try {
            sb.getWrappedDriver().switchTo().alert().accept();
        } catch (NoAlertPresentException e) {
            LOG.info("Всплывающее окно не появилось");
        }
        sb.spinnerLoader().loadingSpinnerPage().waitUntil("Страница не загрузилась", Matchers.not(DisplayedMatcher.displayed()), 30);
    }

    @Step("Выбрать формат \"{typeOfFiles.fileExtension}\" для скачивания файла")
    private void chooseDownloadFileFormat(TypeOfFiles typeOfFiles) {
        sb.downloadForm().formatInput().click();
        systemSleep(0.5); //метод используется в неактуальных тестах
        sb.downloadForm().reportTypeButton(typeOfFiles.getFileExtension()).click();
    }

    @Step("Изменить локаль")
    public void changeLocale(String locale) {
        CustomTools.changeProperty(SystemProperties.APP_DEFAULT_LOCALE, locale);
        Allure.addAttachment("Изменение локали", "Локаль была изменена на " + locale);
    }

    @Step("Нажать кнопку \"Скачать\"")
    private void pressDownloadButton() {
        sb.downloadForm().downloadButton().waitUntil("Кнопка \"Скачать\" не отображается", DisplayedMatcher.displayed(), 5);
        sb.downloadForm().downloadButton().click();
    }

    @Step("Проверить, что в поле \"Формат\" по умолчанию стоит {typeOfFiles.fileFormat}.")
    private void assertJasperDefaultReportFormat(TypeOfFiles typeOfFiles) {
        String value = sb.downloadForm().formatInputT13().getAttribute(VALUE);
        Assert.assertEquals(value, typeOfFiles.getFileFormat(), "Отображемый формат не совпал с системной настройкой");
    }

    /**
     * Берет ссылку на скачивания из атрибута элемента
     */
    private URI getUriFromButtonAttribute(VariantsOfFunctions variants) {
        String href = sb.formOrgUnitMenu().variantsOfFunctions(variants.getName()).getAttribute("href");
        return URI.create(href);
    }

    @Step("Проверить расчёт расписания")
    private void assertScheduleCalculation(List<Integer> previousIds, int omId) {
        Roster newRoster = RosterRepository.getRosters(omId)
                .stream()
                .filter(r -> !previousIds.contains(r.getId()))
                .findFirst()
                .orElse(null);
        assertNotNull(newRoster, "Расчет не найден в API");
        Allure.addAttachment("Проверка",
                             String.format("Был создан новый график с параметрами, версия графика на сайте: %d", newRoster.getVersion()));
        OrgUnit unit = OrgUnitRepository.getOrgUnit(omId);
        LocalDateTime newRosterCreationTime = newRoster.getCreationTime();
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime apiZonedTime;
        try {
            apiZonedTime = newRosterCreationTime.atZone(unit.getTimeZone());
        } catch (NullPointerException e) {
            apiZonedTime = newRosterCreationTime.atZone(ZoneId.of("UTC"));
        }
        assertTrue(apiZonedTime.until(now, ChronoUnit.SECONDS) < 210,
                   String.format("Текущее время: %s время в api: %s", LocalDateTime.now(), newRoster.getCreationTime()));
    }

    @Step("Проверить скачивание документа Выгрузки в 1С")
    private void assertForDownloadingPath(FileDownloadCheckerForScheduleBoard checker, URI fromWebElement) {
        SoftAssert softAssert = new SoftAssert();
        HttpResponse httpResponse = checker.downloadResponse(Role.ADMIN, TypeOfAcceptContent.BASIC);
        assertStatusCode(httpResponse, 200, checker.getDownloadLink().toString());
        //Определение типа контента
        URI checkerUri = checker.getDownloadLink();
        Allure.addAttachment("Скачивание документа", "Ожидаемая ссылка: " + checkerUri
                + ", ссылка, которая была в локаторе элемента: " + fromWebElement);
        softAssert.assertTrue(EqualUri.areEquals(fromWebElement, checkerUri), "Ссылки для скачивания файла не совпали," +
                " адрес страницы: " + fromWebElement + ", а ожидалось: " + checkerUri);
        softAssert.assertAll();
    }

    /**
     * Возвращает самое левое название из подписей (название табеля). Первый элемент в списке взять нельзя, т.к. план может быть впереди
     */
    private ImmutablePair<String, String> getTimesheetTitle() {
        String status = sb.formLayout().timesheetIndicatorBackground().stream().min(ScheduleBoard::compareXAttribute)
                .get().getAttribute("class").replaceAll(".*gantt__", "");
        String text = sb.formLayout().timesheetIndicator().stream().min(ScheduleBoard::compareXAttribute)
                .get().getText();
        return new ImmutablePair<String, String>(status.trim(), text.trim());
    }

    /**
     * Возвращает самое правое название из подписей (название плановых смен)
     */
    private ImmutablePair<String, String> getPlanTitle() {
        String status = sb.formLayout().timesheetIndicatorBackground().stream().max(ScheduleBoard::compareXAttribute)
                .get().getAttribute("class").replaceAll(".*gantt__", "");
        String text = sb.formLayout().timesheetIndicator().stream().max(ScheduleBoard::compareXAttribute)
                .get().getText();
        return new ImmutablePair<>(status.trim(), text.trim());
    }

    private String waitForRosterStatusToChange(GraphStatus status, boolean planRoster) {
        String indicator;
        int counter = -1;
        do {
            try {
                indicator = getTimeSheetIndicatorData(planRoster).left;
            } catch (
                    StaleElementReferenceException e) {  // Если выпал этот exception, то UI обновился в процессе перебора, чего мы и ждем.
                indicator = getTimeSheetIndicatorData(planRoster).left;
            }
            systemSleep(1); //цикл
            counter++;
        } while (!indicator.equals(status.getClassName()) && counter < 5);
        Assert.assertEquals(indicator, status.getClassName(), "Подпись графика на UI не изменилась");
        return getTimeSheetIndicatorData(planRoster).right;
    }

    private ImmutablePair<String, String> getTimeSheetIndicatorData(boolean plan) {
        if (plan) {
            return getPlanTitle();
        } else {
            return getTimesheetTitle();
        }
    }

    @Step("Проверить наличие надписи над табелем учета рабочего времени в зависимости от системной настройки appDefaultLocale")
    private void checkCaptionTimesheet() {
        SystemProperty prop = SystemPropertyRepository.getSystemProperty(SystemProperties.APP_DEFAULT_LOCALE);
        String expected = AppDefaultLocale.findTextByLocale(prop.getValue().toString());
        String actual = getTimesheetTitle().right.replace(".", "");
        boolean check;
        if (actual.length() < expected.length()) {
            check = expected.startsWith(actual);
        } else {
            check = actual.startsWith(expected);
        }
        assertTrue(check, String.format("Надпись над табелем с отработанным временем не соответствует системной настройке appDefaultLocale: " +
                                                "ожидали \"%s\", а на самом деле \"%s\"", expected, actual));
        Allure.addAttachment("Проверка",
                             String.format("Название табеля зависит от установленной настройки appDefaultLocale: " +
                                                   "при значении настройки \"%s\" название табеля равно \"%s\"", prop.getValue(), actual));
    }

    private static int compareXAttribute(AtlasWebElement e1, AtlasWebElement e2) {
        float value1 = Float.parseFloat(e1.getAttribute("x"));
        float value2 = Float.parseFloat(e2.getAttribute("x"));
        return (int) (value1 - value2);
    }

    @Step("Проверить, что плановые и фактические смены отображаются в одной ячейке")
    private void assertMergedUiForShiftsIsActive(int omId) {
        int rowHeight = determineRowHeight(omId);
        int cellHeight = determineCellHeight();
        assertNotEquals(rowHeight / cellHeight, 2, "Плановые и фактические смены не отображаются в одной ячейке");
    }

    @Step("Проверить, что отображаются только фактические смены")
    private void assertUnmergedUiForShiftsIsActive(int omId) {
        int rowHeight = determineRowHeight(omId);
        int cellHeight = determineCellHeight();
        assertNotEquals(rowHeight / cellHeight, 2, "Плановые смены отображаются вместе с фактическими");
    }

    @Step("Проверить появление информации об ошибке при попытке создания запроса смены в другом подразделении без указания даты")
    private void assertErrorSaveWithoutDate(LocalDate localDate) {
        sb.formEditForm().waitUntil("Форма редактирования больше не отображается",
                                    DisplayedMatcher.displayed(), 3);
        sb.formEditForm().errorTextField().should("Красная подсветка строки ввода даты",
                                                  TextMatcher.text(Matchers.containsString("Должна быть не позднее, чем " + localDate + " 24:00:00")), 5);
        Allure.addAttachment("Проверка", "Форма редактирования не закрывается, поле \"Дата окончания\"" +
                " подсвечивается красным, появляется предупреждение");
    }

    @Step("Проверить, что сотруднику {ep} был добавлен запрос на сверхурочную работу")
    private void assertAddOvertime(EmployeePosition ep, ScheduleWorker scheduleWorker,
                                   List<OutsidePlanResource> overtimeBefore, int omId, DateTimeInterval interval) {
        sb.formEditForm().waitUntil("Форма редактирования все еще отображается",
                                    Matchers.not(DisplayedMatcher.displayed()), 10);
        LocalDateTime startTime = interval.getStartDateTime();
        AtlasWebElement overlayElement = scheduleWorker.getOutsidePlanElement(ep, startTime.toLocalDate());
        Assert.assertNotNull(overlayElement, "Элемент сверхурочной работы не отображается");
        String overlayText = overlayElement.getText();
        LocalDateTime endTime = interval.getEndDateTime();
        int duration = (int) Duration.between(startTime, endTime).toHours();
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(overlayText, "С " + duration);
        int rosterId = RosterRepository.getActiveRosterThisMonth(omId).getId();
        List<OutsidePlanResource> overtimeAfter = OutsidePlanResourceRepository.getAllOutsideResources(rosterId, ShiftTimePosition.ALLMONTH);
        overtimeAfter.removeAll(overtimeBefore);
        OutsidePlanResource newOvertime = overtimeAfter.iterator().next();
        DateTimeInterval overtimeInterval = newOvertime.getDateTimeInterval();
        softAssert.assertEquals(overtimeInterval.getStartDateTime(), startTime);
        softAssert.assertEquals(overtimeInterval.getEndDateTime(), endTime);
        softAssert.assertEquals(newOvertime.getEmployeePositionId(), ep.getId());
        softAssert.assertAll();
        Allure.addAttachment("Добавление запроса на сверхурочную работу",
                             String.format("Для сотрудника %s добавлен запрос на сверхурочную работу за %s",
                                           ep, overtimeInterval));
    }

    @Step("Проверить, что сотруднику {ep} был добавлен запрос на дежурство на {newInterval.startDateTime.date}")
    private void assertAddOnDuty(EmployeePosition ep, ScheduleWorker scheduleWorker,
                                 List<OutsidePlanResource> overtimeBefore, int omId,
                                 DateTimeInterval newInterval) {
        sb.formEditForm().waitUntil("Форма редактирования все еще отображается",
                                    Matchers.not(DisplayedMatcher.displayed()), 10);
        Roster roster = RosterRepository.getActiveRosterThisMonth(omId);
        int rosterId = roster.getId();
        List<OutsidePlanResource> onDutyAfter = OutsidePlanResourceRepository.getAllOutsideResources(rosterId, ShiftTimePosition.ALLMONTH);
        onDutyAfter.removeAll(overtimeBefore);
        Assert.assertFalse(onDutyAfter.isEmpty(), "Новый запрос не был добавлен в API");
        OutsidePlanResource newOnDuty = onDutyAfter.iterator().next();

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(newOnDuty.getDateTimeInterval(), newInterval,
                                "Время запроса в API не совпадает со временем, введенным в ходе теста");
        softAssert.assertEquals(newOnDuty.getEmployeePositionId(), ep.getId(),
                                "ID сотрудника в запросе в API не совпадает с ID, выбранным в ходе теста");

        AtlasWebElement onDutyElement = scheduleWorker.getOutsidePlanElement(ep, newInterval.getStartDate());
        softAssert.assertNotNull(onDutyElement, "Элемент запроса на найден на UI");
        softAssert.assertAll();
        Allure.addAttachment("Добавление запроса на дежурство",
                             String.format("Для сотрудника %s добавлен запрос на дежурство за %s",
                                           ep, newOnDuty.getDateTimeInterval().getStartDate()));
    }

    @Step("Кликнуть на элемент запроса типа \"{requestType.name}\" у работника {employeePosition} за {date}")
    private void clickOutsidePlanResourceElement(ScheduleRequestType requestType, ScheduleWorker scheduleWorker,
                                                 EmployeePosition employeePosition, LocalDate date) {
        AtlasWebElement requestElement = scheduleWorker.getOutsidePlanElement(employeePosition, date);
        String requestName = requestType.getName();
        Assert.assertNotNull(requestElement, String.format("Элемент запроса типа \"%s\" у сотрудника %s за %s не найден",
                                                           requestName, employeePosition, date));
        requestElement.click();
        LOG.info("Кликаем на элемент запроса типа \"{}\" у сотрудника {} за {}", requestName, employeePosition, date);
    }

    @Step("Проверить удаление запроса типа \"{requestType.name}\" у сотрудника {emp} за {date}")
    private void assertDeleteOutsidePlanResource(EmployeePosition emp, ScheduleRequestType requestType,
                                                 OutsidePlanResource scheduleRequest, ScheduleWorker scheduleWorker,
                                                 int rosterId, LocalDate date) {
        SoftAssert softAssert = new SoftAssert();
        ElementsCollection<AtlasWebElement> requestElements = scheduleWorker.getOutsidePlanElements(emp, scheduleRequest.getDateTimeInterval().getStartDate());
        softAssert.assertTrue(requestElements.isEmpty(), "Запрос все еще отображается");
        List<OutsidePlanResource> requestAfter = OutsidePlanResourceRepository.getAllOutsideResources(rosterId, ShiftTimePosition.ALLMONTH);
        softAssert.assertFalse(requestAfter.contains(scheduleRequest), "Запрос не был удален");
        softAssert.assertAll();
        String requestName = requestType.getName();
        Allure.addAttachment(String.format("Удаление запроса типа \"%s\"", requestName),
                             String.format("Запрос типа \"%s\" у сотрудника %s за %s успешно удален", requestName, emp, date));
    }

    @Step("Проверить, что для запроса сверхурочной работы сотрудника {ep} {overtimeRequest.dateTimeInterval} время окончания было изменено на {endTime}")
    private void asserEditOvertime(OutsidePlanResource overtimeRequest, List<OutsidePlanResource> overtimeBefore,
                                   LocalDateTime endTime, int rosterId, EmployeePosition ep) {
        List<OutsidePlanResource> overtimeAfter = OutsidePlanResourceRepository.getAllOutsideResources(rosterId, ShiftTimePosition.ALLMONTH);
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(overtimeBefore.size(), overtimeAfter.size(), "Количество запросов на сверхурочную работу изменилось");
        softAssert.assertFalse(overtimeAfter.contains(overtimeRequest));
        overtimeAfter.removeAll(overtimeBefore);
        OutsidePlanResource editedOvertime = overtimeAfter.iterator().next();
        softAssert.assertEquals(editedOvertime.getDateTimeInterval().getEndDateTime(), endTime, "Время окончания запроса не изменилось");
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        String overlayText = scheduleWorker.getOutsidePlanElement(ep, editedOvertime.getDateTimeInterval().getStartDate()).getText();
        int lengthInMinutes = (int) editedOvertime.getDateTimeInterval().getLengthInMinutes();
        int hours = lengthInMinutes / 60;
        int minutes = lengthInMinutes % 60;
        String expectedText;
        if (minutes == 0) {
            expectedText = String.valueOf(hours);
        } else if (hours == 0) {
            expectedText = String.valueOf(hours);
        } else {
            expectedText = hours + ":" + minutes;
        }
        softAssert.assertEquals(overlayText, "С " + expectedText);
        softAssert.assertAll();
        Allure.addAttachment("Редактирование запроса на сверхурочную работу",
                             String.format("У запроса сотрудника %s  на сверхурочную работу за %s было изменено время окончания работы. Новое время окончания: %s",
                                           ep, overtimeRequest.getDateTimeInterval().getStartDate(), endTime));
    }

    @Step("Определить расстояние между двумя соседними горизонтальными линиями")
    private int determineRowHeight(int omId) {
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        Shift shift = PresetClass.defaultShiftPreset(ep);
        LocalDate date = shift.getDateTimeInterval().getStartDate();
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        AtlasWebElement shiftElement = scheduleWorker.getScheduleShiftElement(ep, date);
        return shiftElement.getSize().getHeight();
    }

    @Step("Определить высоту одной ячейки")
    private int determineCellHeight() {
        ElementsCollection<AtlasWebElement> horizontalLines = sb.formLayout().horizontalLine();
        return Integer.parseInt(horizontalLines.get(1).getAttribute("y1")) -
                Integer.parseInt(horizontalLines.get(0).getAttribute("y1"));
    }

    @Step("Включить значение системной настройки \"Индикатор дополнительной информации\"")
    private void enableScheduleBoardHelpIndicator() {
        changeProperty(SystemProperties.SCHEDULE_BOARD_HELP_INDICATOR, true);
    }

    @Step("Нажать кнопку меню (i)")
    private void pressMenuI() {
        sb.formTopBar().menuIButton().
                waitUntil("Иконка меню (i) не найдена", DisplayedMatcher.displayed(), 10);
        new Actions(sb.getWrappedDriver()).moveToElement(sb.formTopBar().menuIButton()).perform();
        sb.formTopBar().menuIButton().click();
    }

    @Step("Кликнуть на \"{item.itemTitle}\" из выпадающего списка кнопки (i)")
    private void selectItemFromDropDownMenuI(ButtonIDropDownMenu item) {
        String itemTitle = item.getItemTitle();
        sb.formTopBar().iButtonAdditionalInformationItem(itemTitle).
                waitUntil("Пункт " + itemTitle + " в выпадающем списке не найден",
                          DisplayedMatcher.displayed(), 10);
        new Actions(sb.getWrappedDriver()).moveToElement(sb.formTopBar().iButtonAdditionalInformationItem(itemTitle)).perform();
        sb.formTopBar().iButtonAdditionalInformationItem(itemTitle).click();

    }

    @Step("Кликнуть на кнопку с корзиной в блоке доп. работ")
    private void clickDeleteAdditionalWorkButton() {
        LOG.info("Кликаем на кнопку с корзиной в блоке доп. работ");
        sb.formEditForm().deleteAdditionalWorkButton().waitUntil("Кнопка удаления доп. работ не появилась", DisplayedMatcher.displayed(), 3);
        sb.formEditForm().deleteAdditionalWorkButton().click();
    }

    @Step("Выбрать доп. работу для удаления")
    private void selectAdditionalWorkToDelete() {
        LOG.info("Выбираем доп. работу для удаления");
        sb.formEditForm().lineInDeleteAdditionalWorkButtonMenu().waitUntil("Список доп. работ на удаление не появился", DisplayedMatcher.displayed(), 3);
        sb.formEditForm().lineInDeleteAdditionalWorkButtonMenu().click();
    }

    @Step("Проверить, что пункта \"{item}\" в меню нет")
    private void checkItemIsNotInTheMenu(String item) {
        clickSectionSelectionMenuOnPageHeader();
        String name = sb.commonHeader().sectionSelectionMenuListItem()
                .stream()
                .map(f -> f.getText())
                .filter(t -> t.equals(item))
                .findFirst().orElse(null);
        if (Objects.nonNull(name)) {
            Allure.addAttachment("Проверка меню", "Пункт " + name + "отобразился в меню");
            throw new AssertionError("Пункт " + name + "отобразился в меню");
        }
    }

    @Step("Обновить текущую страницу")
    private void refreshPage() {
        sb.getWrappedDriver().navigate().refresh();
        try {
            sb.getWrappedDriver().switchTo().alert().accept();
            Allure.addAttachment("Закрываем всплывающее окно alert", "");
        } catch (NoAlertPresentException e) {
            System.out.println(e.getMessage());
        }
        sb.spinnerLoader().loadingSpinnerPage().waitUntil("Страница не загрузилась", Matchers.not(DisplayedMatcher.displayed()), 30);
    }

    private boolean isSelected(ButtonIDropDownMenu item) {
        String itemTitle = item.getItemTitle();
        String attributeName = "au-target-id";
        List<Integer> selectedElementId = sb.formTopBar().selectedElementList()
                .stream().map((e) -> Integer.parseInt(e.getAttribute(attributeName)))
                .collect(Collectors.toList());
        int idThisTitle = Integer.parseInt(sb.formTopBar().iButtonAdditionalInformationItem(itemTitle).getAttribute(attributeName));
        boolean result = selectedElementId.contains(idThisTitle + 1);
        String allureTitle = String.format("Проверка того, что индикатор \"%s\" включен", itemTitle);
        if (result) {
            Allure.addAttachment(allureTitle, String.format("Индикатор \"%s\" уже был включен", itemTitle));
        } else {
            Allure.addAttachment(allureTitle, String.format("Индикатор \"%s\" не был включен", itemTitle));
        }
        return result;
    }

    @Step("Перейти в раздел \"Расписание\" подразделения \"{orgUnit.name}\" с ролью \"{role.name}\"")
    private void goToScheduleAsUser(Role role, OrgUnit orgUnit) {
        new RoleWithCookies(sb.getWrappedDriver(), role, orgUnit).getSectionPageForSpecificOrgUnit(orgUnit.getId(), Section.SCHEDULE_BOARD);
        sb.spinnerLoader().loadingSpinnerPage().waitUntil("Страница не загрузилась", Matchers.not(DisplayedMatcher.displayed()), 60);
    }

    @Step("Перейти в раздел \"Расписание\" подразделения \"{unit.name}\" с ролью \"{role.name}\"")
    private void goToScheduleAsUser(Role role, OrgUnit unit, User user) {
        new RoleWithCookies(sb.getWrappedDriver(), role, unit, user).getSectionPageForSpecificOrgUnit(unit.getId(), Section.SCHEDULE_BOARD);
        sb.spinnerLoader().loadingSpinnerPage().waitUntil("Страница не загрузилась", Matchers.not(DisplayedMatcher.displayed()), 10);
    }

    /**
     * Значение SystemProperties.SCHEDULE_BOARD_HELP_INDICATOR должно быть true.
     * Метод проверяет работу пункта "Дополнительная информация", меню кнопки со значком (i).
     *
     * @param isItemSelected - true пункт отмечен
     *                       isItemSelected - false пункт не отмечен.
     *                       При загрузке страницы пункт "Дополнительная информация" отмечен по умолчанию.
     */
    @Step("Проверить наличие/отсутствие панели \"Дополнительная информация\"")
    private void isAdditionalInformationDisplayed(boolean isItemSelected) {
        if (isItemSelected) {
            sb.formLayout().timesheetIndicator().waitUntil("Подписи \"Табель\" и \"Плановый график\" в расписании не добавились",
                                                           Matchers.not(Matchers.empty()), 5);
            Allure.addAttachment("Подписи \"Табель\" и \"Плановый график\" в расписании добавились", "");
            LOG.info("Подписи \"Табель\" и \"Плановый график\" в расписании добавились");
        } else {
            sb.formLayout().timesheetIndicator().waitUntil("\"Подписи \"Табель\" и \"Плановый график\" в расписании не удалились",
                                                           Matchers.empty(), 5);
            Allure.addAttachment("Подписи \"Табель\" и \"Плановый график\" в расписании не отображаются", "");
            LOG.info("\"Подписи \"Табель\" и \"Плановый график\" в расписании не отображаются\"");
        }
    }

    @Step("Проверить отображение тега под должностью сотрудника на UI")
    private boolean checkTag(int empId, String tagName, boolean exactMatch) {
        ElementsCollection<AtlasWebElement> tags = sb.formLayout().employeeTags(empId);
        for (AtlasWebElement tag : tags) {
            boolean result;
            if (exactMatch) {
                result = tag.getText().equals(tagName);
            } else {
                result = tag.getText().contains(tagName);
            }
            if (result) {
                Allure.addAttachment("Проверка",
                                     String.format("Под должностью сотрудника появился индикатор \"%s\"", tagName));
                return true;
            }
        }
        Allure.addAttachment("Проверка",
                             String.format("Под должностью сотрудника нет индикатора \"%s\"", tagName));
        return false;
    }

    @Step("Нажать кнопку всплывающего окна \"Всё равно уйти\"")
    private void pressButtonQuit() {
        sb.confirmationWindow().
                waitUntil("Окно подтверждения перехода не появилось", DisplayedMatcher.displayed(), 10);
        new Actions(sb.getWrappedDriver()).moveToElement(sb.confirmationWindow()).perform();
        sb.confirmationWindow().buttonQuit().
                waitUntil("Кнопка всплывающего окна  \"Всё равно уйти\" не найдена", DisplayedMatcher.displayed(), 5);
        new Actions(sb.getWrappedDriver()).moveToElement(sb.confirmationWindow().buttonQuit()).perform();
        sb.confirmationWindow().buttonQuit().click();
    }

    /**
     * Метод выбирает случайный элемент главного меню выбора разделов,
     * Если раздел не описан в Section enum, то он выбран не будет.
     *
     * @param wasteElementsList Список элементов меню, которые надо
     *                          исключить из выбора. Если wasteElementsList=null,
     *                          то в выборе участвуют все элементы.
     * @return AtlasWebElement, соответствующий выбранному разделу.
     */

    private AtlasWebElement takeRandomWebElementFromSectionSelectionMenu(List<String> wasteElementsList) {
        Section section = null;
        String sectionName;
        String sectionUrl;
        int numberAttempts = 0;
        int index = 0;
        ElementsCollection<AtlasWebElement> unSortedItemList = sb.commonHeader().sectionSelectionMenuListItem();
        List<AtlasWebElement> itemList = unSortedItemList;
        if (wasteElementsList != null) {
            itemList = unSortedItemList.stream()
                    .filter(i -> !wasteElementsList.contains(i.getText()))
                    .collect(Collectors.toList());
        }
        while (section == null && numberAttempts < 20) {
            numberAttempts++;
            int min = 0;
            int max = itemList.size() - 1;
            index = (int) (Math.random() * (max - min) + min);
            sectionUrl = itemList.get(index).getAttribute("href");
            sectionName = itemList.get(index).getText();
            String finalSectionUrl = sectionUrl;
            section = Arrays.stream(Section.values()).
                    filter(s ->
                                   finalSectionUrl.contains(s.getUrlEnding())).
                    findFirst().orElse(null);
            if (section == null) {
                LOG.info("Добавьте в класс Section Enum для секции {}, c URL: {}", sectionName, sectionUrl);
            }
        }
        return itemList.get(index);
    }

    @Step("Нажать пункт \"{sectionName}\", из меню разделов (три горизонтальные линии)")
    private void clickItemFromSectionsMenu(AtlasWebElement element, String sectionName) {
        element.waitUntil(String.format("Элемент меню %s не найден", sectionName), DisplayedMatcher.displayed(), 5);
        new Actions(sb.getWrappedDriver()).moveToElement(element).perform();
        element.click();
        Allure.addAttachment(String.format("Выбран пункт меню \"%s\" c  URL:%s", sectionName, element.getAttribute("href")), "");
        LOG.info("Выбран пункт меню \"{}\", c URL: \"{}\"", sectionName, element.getAttribute("href"));
    }

    @Step("Проверить, что в главном меню присутствует раздел \"{sectionName}\"")
    private void assertItemExistsInSectionsMenu(String sectionName) {
        sb.commonHeader().section(sectionName).should(String.format("Раздел \"%s\" не отобразился", sectionName),
                                                      DisplayedMatcher.displayed());
        Assert.assertTrue(sb.commonHeader().section(sectionName).isEnabled());
        LOG.info("В главном меню присутствует раздел \"{}\"", sectionName);
    }

    /**
     * Метод выбирает случайную группу позиций и возвращает список активных сотрудников которые есть под этой группой
     *
     * @param groupsWithEmployees с группами и сотрудниками в каждой группе
     * @return случайную группу и список сотрудников
     */
    private ImmutablePair<Integer, List<String>> getEmployeesListFromRandomGroup(Map<Integer, List<String>> groupsWithEmployees) {
        Integer randomGroup = groupsWithEmployees.keySet().stream().collect(randomItem());
        return new ImmutablePair<>(randomGroup, groupsWithEmployees.get(randomGroup));
    }

    @Step("Выбор элемента {positionName} в фильтре сотрудников в расписании ДЛЯ ПОЧТЫ")
    private void pickFunctionalRoleFromEmployeeFilter(String positionName) {
        sb.employeesFilterMode().positionGroupChevron().click();
        sb.employeesFilterMode().groupItemButton(positionName).click();
    }

    @Step("Нажать шеврон мат параметра")
    private void clickOnMathParameterChevronButton(MathParameters param) {
        sb.employeeDataMenu().employeeParametersMenu().mathParameterDropdown(param.getNameParam())
                .waitUntil("Шеврон у параметра " + param.getNameParam() + " не отображен.", DisplayedMatcher.displayed(), 5);
        new Actions(sb.getWrappedDriver()).moveToElement(sb.employeeDataMenu().employeeParametersMenu().mathParameterDropdown(param.getNameParam())).perform();
        sb.employeeDataMenu().employeeParametersMenu().mathParameterDropdown(param.getNameParam()).click();
    }

    @Step("Сохранить изменение параметров")
    private void saveDataInParamsForm() {
        sb.employeeDataMenu().employeeParametersMenu().saveParamButton().click();
    }

    @Step("Проверить тега под должностью сотрудника и мат параметр в API")
    private void assertEmployeeTag(EmployeePosition employeePosition, MathParameters param) {
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertTrue(checkTag(employeePosition.getId(), param.getNameParam(), true),
                              String.format("Не найден тег \"%s\" под сотрудником %s", param.getNameParam(), employeePosition));
        softAssert.assertTrue(Boolean.parseBoolean(MathParameterRepository.getValueFromMathParam(employeePosition.getEmployee(), param).getValue()),
                              String.format("В API у сотрудника %s нет параметра \"%s\"", employeePosition, param));
        softAssert.assertAll();
        Allure.addAttachment("Проверка",
                             String.format("Был добавлен параметр \"%s\" у сотрудника %s", param.getNameParam(), employeePosition));
    }

    @Step("Проверить, что был осуществлен переход на страницу  \"{sectionName}\"")
    private void assertGoToDesiredSection(String sectionName) {
        sb.mainHeader().headerText().waitUntil("Кнопка меню не отобразилась", DisplayedMatcher.displayed(), 15);
        sb.mainHeader().headerText().should("Раздел " + sectionName + " не загрузился",
                                            TextMatcher.text(Matchers.containsString(sectionName)));
    }

    @Step("Нажать любой другой ОМ \"{name}\"")
    private void clickOrgUnit(AtlasWebElement webElement, String name) {
        webElement.
                waitUntil("Не удалось найти " + name + " в падающем списке.", DisplayedMatcher.displayed(), 25);
        new Actions(sb.getWrappedDriver()).moveToElement(webElement).perform();
        webElement.click();
    }

    @Step("Проверить, отражено ли отклонение от стандартного времени на UI (фактическое время должно быть выделено красным цветом) и в API")
    private void assertDeviationExcess(OrgUnit unit, EmployeePosition position) {
        SoftAssert softAssert = new SoftAssert();
        Employee employee = position.getEmployee();
        AtlasWebElement factTimeElement = sb.formLayout().employeeFactWorkingHours(position.getId());
        AtlasWebElement standardTimeElement = sb.formLayout().employeePlanWorkingHours(position.getId());
        String factTime = factTimeElement.getText();
        String standardTime = standardTimeElement.getText();
        DeviationFromStandard deviation = DeviationFromStandardRepository.getDeviation(RosterRepository.getActiveRosterThisMonth(unit.getId()),
                                                                                       RosterRepository.getWorkedRosterThisMonth(unit.getId()), position);
        if (Double.parseDouble(factTime) > Double.parseDouble(standardTime)) {
            LOG.info("Количество часов у сотрудника {} превысило норму", employee);
            softAssert.assertTrue(factTimeElement.getAttribute("class").contains("red"), "Индикатор с количеством часов при превышении нормы не окрасился в красный цвет");
            softAssert.assertTrue(deviation.getFact() > deviation.getStandard(), "В API превышение нормы не отражено");
            softAssert.assertAll();
            Allure.addAttachment("Проверка отражения фактического времени на UI",
                                 String.format("На UI у сотрудника %s фактические часы %s окрасились в красный цвет, так как превысили норму %s.",
                                               employee, factTime, standardTime));
        } else {
            Assert.fail("На UI превышение нормы у сотрудника " + employee + " не отражено.");
        }
    }

    @Step("Проверить, отражено ли отклонение от стандартного времени на UI (фактическое время должно быть выделено желтым цветом) и в API")
    private void assertDeviationLack(OrgUnit unit, EmployeePosition position) {
        SoftAssert softAssert = new SoftAssert();
        Employee employee = position.getEmployee();
        AtlasWebElement factTimeElement = sb.formLayout().employeeFactWorkingHours(position.getId());
        AtlasWebElement standardTimeElement = sb.formLayout().employeePlanWorkingHours(position.getId());
        String factTime = factTimeElement.getText();
        String standardTime = standardTimeElement.getText();
        DeviationFromStandard deviation = DeviationFromStandardRepository.getDeviation(RosterRepository.getActiveRosterThisMonth(unit.getId()), RosterRepository.getWorkedRosterThisMonth(unit.getId()), position);
        if (Double.parseDouble(factTime) < Double.parseDouble(standardTime)) {
            LOG.info("Количество часов у сотрудника {} меньше нормы", employee);
            softAssert.assertTrue(factTimeElement.getAttribute("class").contains("yellow"), "Индикатор с количеством часов при превышении нормы не окрасился в желтый цвет");
            softAssert.assertTrue(deviation.getFact() < deviation.getStandard(), "В API нехватка нормы не отражена");
            softAssert.assertAll();
            Allure.addAttachment("Проверка отражения фактического времени на UI",
                                 String.format("На UI у сотрудника %s фактические часы %s окрасились в желтый цвет, так как не достигли нормы %s.",
                                               employee, factTime, standardTime));
        } else {
            Assert.fail("На UI нехватка нормы у сотрудника " + employee + " не отражена.");
        }
    }

    private <T> void assertNoChanges(ScheduleWorker scheduleWorker, LocalDate date, EmployeePosition ep,
                                     List<T> before, List<T> after, List<String> errorMessage,
                                     String expectedShiftElementText, String expectedErrorMessage) {
        if (expectedErrorMessage != null) {
            sb.errorMessage(expectedErrorMessage)
                    .should(String.format("Сообщение об ошибке с текстом \"%s\" не отображается",
                                          expectedErrorMessage.substring(0, expectedErrorMessage.lastIndexOf(" "))),
                            DisplayedMatcher.displayed());
            sb.errorMessageClose(expectedErrorMessage).click();
        }
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(before, after,
                                String.format("%s сотрудника %s изменились: \nБыло: %s\nСтало: %s",
                                              errorMessage.get(0), ep, before, after));
        AtlasWebElement actualShiftElement = scheduleWorker.getScheduleShiftElement(ep, date);
        if (expectedShiftElementText == null) {
            softAssert.assertNull(actualShiftElement, String.format("%s отображается на UI", errorMessage.get(1)));
            Allure.addAttachment(String.format("Проверка изменений в %s сотрудника на UI", errorMessage.get(2)),
                                 String.format("На UI отобразилось уведомление о невозможности создания/переноса %s для сотрудника %s на %s. " +
                                                       "Изменения не сохранены", errorMessage.get(3), ep, date));
        } else {
            softAssert.assertEquals(expectedShiftElementText, actualShiftElement.getText(),
                                    String.format("Элемент %s на UI изменился", errorMessage.get(3)));
            Allure.addAttachment(String.format("Проверка изменений в %s сотрудника на UI", errorMessage.get(2)),
                                 String.format("На UI отобразилось уведомление о невозможности создания/переноса %s для сотрудника %s на %s. " +
                                                       "Изменения не сохранены", errorMessage.get(3), ep, date));
        }
        softAssert.assertAll();
        Allure.addAttachment(String.format("Проверка изменений в %s сотрудника в API", errorMessage.get(2)),
                             String.format("%s в API для сотрудника %s остались без изменений.", errorMessage.get(0), ep));
    }

    @Step("Проверить, что смены сотрудника {ep} остались без изменений")
    private void assertNoChangesToShifts(ScheduleWorker scheduleWorker, LocalDate date, EmployeePosition ep, List<Shift> before,
                                         String expectedShiftElementText, String expectedErrorMessage, ShiftTimePosition position) {
        List<Shift> after = ShiftRepository.getShifts(ep, position);
        List<String> errorMessage = Arrays.asList("Смены", "Смена", "сменах", "смены");
        assertNoChanges(scheduleWorker, date, ep, before, after, errorMessage, expectedShiftElementText, expectedErrorMessage);
    }

    @Step("Проверить, что запросы сотрудника {ep} остались без изменений")
    private void assertNoChangesToRequests(ScheduleWorker scheduleWorker, LocalDate date, EmployeePosition ep, List<ScheduleRequest> before,
                                           String expectedShiftElementText, String expectedErrorMessage) {
        List<ScheduleRequest> after = ScheduleRequestRepository.getEmployeeScheduleRequests(
                ep.getEmployee().getId(), new DateInterval(date), ep.getOrgUnit().getId());
        List<String> errorMessage = Arrays.asList("Запросы", "Запрос", "запросах", "запроса");
        assertNoChanges(scheduleWorker, date, ep, before, after, errorMessage, expectedShiftElementText, expectedErrorMessage);
    }

    @Step("Кликнуть на индикатор количества свободных смен над столбцом за {date}")
    private void clickFreeShift(LocalDate date) {
        int day = date.getDayOfMonth();
        ElementsCollection<AtlasWebElement> indicators = sb.formLayout().freeShiftIndicators();
        LOG.info("Кликаем на индикатор свободных смен над столбцом {}", date);
        indicators.waitUntil("Индикаторы количества свободных смен не отображаются",
                             Matchers.not(indicators.isEmpty()), 5)
                .get(day - 1)
                .click();
    }

    /**
     * Находит порядковый номер свободной смены на UI
     *
     * @param interval время и начала и конца смены
     * @param title    название позиции, для которой создана смена
     */
    private int findShiftOrderNumber(DateTimeInterval interval, String title) {
        List<AtlasWebElement> startDates = sb.freeShiftList().startDates();
        List<AtlasWebElement> endDates = sb.freeShiftList().endDates();
        List<AtlasWebElement> positions = sb.freeShiftList().positions();
        int i;
        for (i = 0; i < startDates.size(); i++) {
            String uiStartTime = startDates.get(i).getAttribute(VALUE).trim();
            String uiEndTime = endDates.get(i).getAttribute(VALUE).trim();
            String uiPosition = positions.get(i).getText().trim();
            String expectedStartTime = interval.getStartDateTime().toLocalTime().toString().substring(0, 5).trim();
            String expectedEndTime = interval.getEndDateTime().toLocalTime().toString().substring(0, 5).trim();

            if (uiStartTime.equals(expectedStartTime)
                    && uiEndTime.equals(expectedEndTime)
                    && uiPosition.equals(title)) {
                LOG.info("На UI найдена свободная смена со временем {} для позиции \"{}\". Порядковый номер в списке - {}",
                         interval, title, i + 1);
                Allure.addAttachment("Поиск строки заданной свободной смены на UI",
                                     String.format("Найдена свободная смена со временем %s для позиции \"%s\". Порядковый номер в списке - %s",
                                                   interval, title, i + 1));
                return i;
            }
        }
        throw new AssertionError(String.format("На UI не найдено свободных смен со временем %s для позиции \"%s\"",
                                               interval, title));
    }

    /**
     * Необходимо передавать order + 1, чтобы в отчете отображался порядковый номер, начиная с 1
     */
    @Step("Раскрыть список сотрудников для свободной смены с порядковым номером {order}")
    private void clickFreeShiftEmployeeList(int order) {
        LOG.info("Раскрываем список сотрудников для свободной смены с порядковым номером {}", order);
        sb.freeShiftList().employee(order - 1).click();
    }

    @Step("Проверить отображение сотрудника {employee} в списке свободной смены. Сотрудник отображается: {displayed}")
    private void freeShiftEmployeesCheck(EmployeePosition employee, boolean displayed) {
        changeStepNameDependingOnParameter(displayed, String.format("Проверить, что сотрудник %s отображается на UI", employee),
                                           String.format("Проверить, что сотрудник %s не отображается на UI", employee));
        sb.freeShiftList().loading().waitUntil("Список сотрудников всё ещё загружается",
                                               Matchers.not(DisplayedMatcher.displayed()), 120);
        ElementsCollection<AtlasWebElement> listItems = sb.freeShiftList().availableEmployees();
        String employeeNotDisplayed = "Сотрудник не появился в списке созданной свободной смены";
        if (!listItems.isEmpty()) { //на ui иногда отображаются названия подразделений, которым принадлежат сотрудники, а иногда нет, поэтому проверяется только ФИО
            List<String> uiEmployees = listItems.stream()
                    .map(AtlasWebElement::getText)
                    .map(e -> {
                        if (e.contains("\n")) {
                            e = e.substring(0, e.indexOf("\n"));
                        }
                        if (e.contains("/")) {
                            e = e.substring(0, e.indexOf(" /"));
                        }
                        return e;
                    }).collect(Collectors.toList());
            String employeeName = employee.getEmployee().getFullName();
            Allure.addAttachment("Список сотрудников на UI", uiEmployees.toString());
            LOG.info("Список сотрудников на UI: {}", uiEmployees);
            if (displayed) {
                Assert.assertTrue(uiEmployees.contains(employeeName), employeeNotDisplayed);
            } else {
                Assert.assertFalse(uiEmployees.contains(employeeName), "Сотрудник отображается в списке");
            }
        } else if (displayed) {
            Assert.fail(employeeNotDisplayed);
        }
        //При (!displayed && listItems.isEmpty()) ассерт будет зеленым.
        //Прямой ассерт listItems.isEmpty() при !displayed поставить нельзя, т.к. в списке могут быть другие подходящие сотрудники
    }

    @Step("Прокликать до месяца, где у подразделения есть РЗ")
    private void clickBackToFTE() {
        LocalDate date = LocalDate.of(2020, 12, 1);
        Period period = Period.between(date, LocalDate.now());
        int clicks = period.getYears() * 12 + period.getMonths();
        LOG.info("Кликаем по кнопке \"Назад\" {} раз до {}", clicks, date);
        for (int i = 0; i < clicks; i++) {
            clickBack();
        }
        Allure.addAttachment(String.format("Навигация до %s", date.format(DateTimeFormatter.ofPattern("MMM yyyy").withLocale(new Locale("ru")))),
                             String.format("Сделано %s кликов по кнопке \"Назад\"", clicks));
    }

    @Step("Кликнуть кнопку \"Назад\" в расписании")
    private void clickBack() {
        LOG.info("Кликаем кнопку \"Назад\" в расписании");
        waitForClickable(sb.formLayout().navigateBackButton(), sb, 5);
        sb.formLayout().navigateBackButton().click();
        sb.spinnerLoader().loadingSpinnerPage().waitUntil("Страница не загрузилась", Matchers.not(DisplayedMatcher.displayed()), 10);
    }

    @Step("Кликнуть кнопку \"Вперед\" в расписании")
    private void clickForward() {
        LOG.info("Кликаем кнопку \"Вперед\" в расписании");
        waitForClickable(sb.formLayout().navigateForwardButton(), sb, 5);
        sb.formLayout().navigateForwardButton().click();
        sb.spinnerLoader().loadingSpinnerPage().waitUntil("Страница не загрузилась", Matchers.not(DisplayedMatcher.displayed()), 30);
    }

    @Step("Включить индикаторы {items}, если они выключены")
    private void enableIndicatorsIfDisabled(List<ButtonIDropDownMenu> items) {
        for (ButtonIDropDownMenu item : items) {
            if (!isSelected(item)) {
                pressMenuI();
                selectItemFromDropDownMenuI(item);
            }
        }
    }

    @Step("Убедиться, что индикаторы \"{indicators}\" отображаются над расписанием")
    private void assertDisplayedIndicator(List<ButtonIDropDownMenu> indicators) {
        systemSleep(3); //не все индикаторы успевают сразу прогрузиться
        List<String> items = sb.formLayout().indicators().stream().map(AtlasWebElement::getText).collect(Collectors.toList());
        List<String> indicatorNames = indicators
                .stream()
                .map(ButtonIDropDownMenu::getItemTitle)
                .collect(Collectors.toList());
        Assert.assertTrue(items.containsAll(indicatorNames),
                          String.format("Отображаются не все нужные индикаторы. На UI отображены индикаторы: %s. Искомые индикаторы: %s ",
                                        items, indicatorNames));
    }

    @Step("Проверить, что ячейки смены с {interval.startDate} по {interval.endDate} недоступны для редактирования")
    private void assertInactiveCells(EmployeePosition ep, DateInterval interval, List<LocalDate> dates, ScheduleWorker scheduleWorker) {
        for (LocalDate date : dates) {
            assertInactiveCell(ep, date, scheduleWorker);
        }
    }

    private void assertInactiveCell(EmployeePosition ep, LocalDate date, ScheduleWorker scheduleWorker) {
        clickOnTargetShift(ep, date, scheduleWorker);
        tryToDeleteShiftWithoutPermission();
        clickCloseButton();
        Allure.addAttachment("Проверка недоступности смены для редактирования",
                             String.format("Смена за %s у сотрудника %s недоступна для редактирования, кнопка удаления не найдена",
                                           date, ep.getEmployee().getFullName()));
    }

    private void assertActiveCell(EmployeePosition ep, LocalDate date, ScheduleWorker scheduleWorker) {
        clickOnTargetShift(ep, date, scheduleWorker);
        sb.formEditForm().dateStartOrEndInput(DateTypeField.END_DATE.getName())
                .should("Смена открылась в режиме просмотра", DisplayedMatcher.displayed(), 5);
        Allure.addAttachment("Проверка доступности смены для редактирования",
                             String.format("Смена за %s доступна для редактирования", date));
        clickCloseButton();
    }

    private void assertFailedShiftEdit(EmployeePosition ep, LocalDate date, ScheduleWorker scheduleWorker) {
        Random random = new Random();
        clickOnTargetShift(ep, date, scheduleWorker);
        sb.formEditForm().dateStartOrEndInput(DateTypeField.END_DATE.getName())
                .should("Смена открылась в режиме просмотра", DisplayedMatcher.displayed(), 30);
        enterShiftTimeStartOrEnd(LocalTime.of(9, random.nextInt(60)), TimeTypeField.START_TIME);
        enterShiftTimeStartOrEnd(LocalTime.of(17, random.nextInt(60)), TimeTypeField.END_TIME);
        clickEditShiftButton();
        sb.formLayout().warning().should("Окно с ошибкой об отсутствии пермишена не отобразилось", DisplayedMatcher.displayed(), 5);
        String actualWarningMessage = sb.formLayout().warning().getText();
        Assert.assertTrue(actualWarningMessage.contains("Редактирование плана в прошлом"),
                          String.format("Текст ошибки [%s] не соответствует ожидаемому", actualWarningMessage));
        Allure.addAttachment("Проверка невозможности редактирования смены",
                             String.format("Смену за %s нельзя отредактировать ввиду отсутствия пермишена", date));
        new Actions(sb.getWrappedDriver()).sendKeys(Keys.ESCAPE).perform();
    }

    private void assertFailedShiftDelete(EmployeePosition ep, LocalDate date, ScheduleWorker scheduleWorker) {
        clickOnTargetShift(ep, date, scheduleWorker);
        shiftThreeDotsClick();
        sb.formListOfRequest().typeButtons(RequestAction.DELETE.getAction()).click();
        sb.formLayout().warning().should("Окно с ошибкой об отсутствии пермишена не отобразилось", DisplayedMatcher.displayed(), 5);
        String actualWarningMessage = sb.formLayout().warning().getText();
        Assert.assertTrue(actualWarningMessage.contains("Редактирование плана в прошлом"),
                          String.format("Текст ошибки [%s] не соответствует ожидаемому", actualWarningMessage));
        new Actions(sb.getWrappedDriver()).sendKeys(Keys.ESCAPE).perform();
        Allure.addAttachment("Проверка невозможности удаления смены",
                             String.format("Смену за %s нельзя удалить ввиду отсутствия пермишена", date));
    }

    @Step("Проверить, что ячейки табеля с {interval.startDate} по {interval.endDate} доступны для редактирования")
    private void assertActiveCells(EmployeePosition ep, DateInterval interval, List<LocalDate> dates, ScheduleWorker scheduleWorker) {
        for (LocalDate date : dates) {
            assertActiveCell(ep, date, scheduleWorker);
        }
    }

    @Step("Проверить, что ячейки за прошлый месяц недоступны для редактирования")
    private void assertInactiveCellsLastMonth(List<Shift> shifts, EmployeePosition ep, ScheduleWorker scheduleWorker) {
        for (Shift shift : shifts) {
            if (shift.getId() != null && shift.getId() != 0) {
                LocalDate date = shift.getStartDate();
                assertInactiveCell(EmployeePositionRepository.getEmployeePositionById(shift.getEmployeePositionId()), date, scheduleWorker);
            } else {
                clickOnEmptyCell(ep, shift.getStartDate(), scheduleWorker);
                Assert.assertThrows(WaitUntilException.class, this::clickOnPlusCellOnGraph);
            }
        }
    }

    @Step("Проверить, что ячейки за прошлый месяц доступны для редактирования")
    private void assertActiveCellsLastMonth(List<Shift> shifts, EmployeePosition ep, ScheduleWorker scheduleWorker) {
        for (Shift shift : shifts) {
            if (shift.getId() != null && shift.getId() != 0) {
                LocalDate date = shift.getStartDate();
                assertActiveCell(EmployeePositionRepository.getEmployeePositionById(shift.getEmployeePositionId()), date, scheduleWorker);
            } else {
                clickOnEmptyCell(ep, shift.getStartDate(), scheduleWorker);
                clickOnPlusCellOnGraph();
                clickCloseButton();
            }
        }
    }

    /**
     * Проверяет активные/неактивные ячейки со сменами в табеле с учетом прошлого месяца
     *
     * @param ep                    позиция сотрудника
     * @param interval              интервал проверки
     * @param datesToCheckLastMonth список дат в прошлом месяце
     * @param date                  дата, на которую табель должен быть заблокирован
     * @param sw                    объект ScheduleWorker
     */
    private void assertLockedShifts(EmployeePosition ep, DateInterval interval, List<LocalDate> datesToCheckLastMonth, LocalDate date, ScheduleWorker sw) {
        if (!datesToCheckLastMonth.isEmpty()) {
            clickBack();
            assertActiveCells(ep, interval, datesToCheckLastMonth, sw);
        } else {
            if (!date.getMonth().equals(LocalDate.now().getMonth())) {
                clickBack();
            }
        }
        assertInactiveCell(ep, date, sw);
    }

    @Step("Выбрать в выпадающем списке тип доп. работы \"{additionalWork}\"")
    private void selectAdditionalWorkType(String additionalWork) {
        LOG.info("Выбираем в выпадающем списке тип \"{}\"", additionalWork);
        sb.formEditForm().additionalWorkTypeInput().click();
        sb.formEditForm().additionalWorkTypeListItem(additionalWork).waitUntil("В списке нет элемента \"" + additionalWork + "\"",
                                                                               DisplayedMatcher.displayed(), 5);
        systemSleep(1); //без ожидания выбирается режим сравнения графиков
        sb.formEditForm().additionalWorkTypeListItem(additionalWork).click();
    }

    @Step("Проверить добавление доп. работы к смене сотрудника {ep} за {date}")
    private void assertAddAdditionalWork(ScheduleWorker scheduleWorker, EmployeePosition ep, LocalDate date,
                                         LocalTime startTime, LocalTime endTime, Shift shift, AdditionalWork additionalWork) {
        AtlasWebElement addWorkElement = scheduleWorker.getAdditionalWorkElement(ep, date);
        SoftAssert softAssert = new SoftAssert();
        Roster roster = RosterRepository.getActiveRosterThisMonth(ep.getOrgUnit().getId());
        softAssert.assertTrue(roster.isPublished(), "Статус ростера изменился на \"Не опубликован\"");
        softAssert.assertNotNull(addWorkElement, "Элемент доп. работы не отображается на UI");
        List<ShiftAddWorkLink> additionalWorkAfter = shift.refreshShift().getAdditionalWork();
        Assert.assertFalse(additionalWorkAfter.isEmpty(), "У смены нет доп. работ в API");
        ShiftAddWorkLink addedWork = additionalWorkAfter.iterator().next();
        softAssert.assertEquals(addedWork.getFrom(), LocalDateTime.of(date, startTime), "Время начала не совпадает");
        softAssert.assertEquals(addedWork.getTo(), LocalDateTime.of(date, endTime), "Время окончания не совпадает");
        softAssert.assertEquals(addedWork.getAdditionalWork().getTitle(), additionalWork.getTitle(), "Тип доп. работы не совпадает");
        softAssert.assertAll();
    }

    @Step("Проверить добавление статуса \"{statusName}\" к доп. работе в смене сотрудника {ep} за {date}")
    private void assertAddStatusToAdditionalWork(ScheduleWorker scheduleWorker, EmployeePosition ep, LocalDate date, String statusName) {
        AtlasWebElement addWorkElement = scheduleWorker.getAdditionalWorkElement(ep, date);
        SoftAssert softAssert = new SoftAssert();
        Roster roster = RosterRepository.getActiveRosterThisMonth(ep.getOrgUnit().getId());
        softAssert.assertTrue(roster.isPublished(), "Статус ростера изменился на \"Не опубликован\"");
        String attachment = String.format("Доп. работа со статусом \"%s\"", statusName);
        if (!Objects.equals(statusName, AddWorkStatus.CANCELLED.getStatusName())) {
            softAssert.assertNotNull(addWorkElement, String.format("Элемент доп. работы со статусом \"%s\" не отображается на UI", statusName));
            Allure.addAttachment("Доп. работа на UI", String.format("%s отобразилась", attachment));
        } else {
            softAssert.assertNull(addWorkElement, String.format("Элемент доп. работы со статусом \"%s\" отображается на UI", statusName));
            Allure.addAttachment("Доп. работа на UI", String.format("%s не отобразилась", attachment));
        }
        Shift shift = ShiftRepository.getShift(ep, date, ShiftTimePosition.ALLMONTH);
        List<ShiftAddWorkLink> additionalWorkAfter = shift.getAdditionalWork();
        Assert.assertFalse(additionalWorkAfter.isEmpty(), "У смены нет доп. работ в API");
        ShiftAddWorkLink addedWork = additionalWorkAfter.iterator().next();
        softAssert.assertEquals(addedWork.getShiftAddWorkStatus(), statusName);
        softAssert.assertAll();
    }

    @Step("Проверить удаление доп. работы к смене сотрудника {ep} за {date}")
    private void assertDeleteAdditionalWork(ScheduleWorker scheduleWorker, EmployeePosition ep, LocalDate date, List<ShiftAddWorkLink> additionalWorkBefore) {
        AtlasWebElement addWorkElement = scheduleWorker.getAdditionalWorkElement(ep, date);
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertNull(addWorkElement, "Элемент доп. работы отображается на UI");
        Shift shift = ShiftRepository.getShift(ep, date, ShiftTimePosition.DEFAULT);
        List<ShiftAddWorkLink> additionalWorkAfter = shift.getAdditionalWork();
        additionalWorkAfter.removeAll(additionalWorkBefore);
        softAssert.assertTrue(additionalWorkAfter.isEmpty(), "Доп. работа не была удалена из API");
        softAssert.assertAll();
    }

    @Step("Проверить, что отображается сообщение об ошибке {expectedText} и доп. работы к смене {shift} не изменились")
    private void assertErrorInAdditionalWorkInputField(String expectedText, Shift shift) {
        sb.formEditForm().errorTextField().waitUntil("Текст ошибки не отобразился", DisplayedMatcher.displayed(), 5);
        String errorText = sb.formEditForm().errorTextField().getText();
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(errorText, expectedText, "Текст на UI не соответствует ожидаемому");
        List<ShiftAddWorkLink> additionalWorkAfter = shift.refreshShift().getAdditionalWork();
        softAssert.assertTrue(additionalWorkAfter.isEmpty(), "Доп. работы смены изменились");
        softAssert.assertAll();
    }

    @Step("Поставить галочку напротив сотрудника с именем {employee}")
    private void clickEmployeeCheckbox(Employee employee) {
        sb.scheduleWizardForm().waitUntil("Мастер планирования не загрузился", DisplayedMatcher.displayed(), 10); //NoSuchElementException
        try {
            sb.scheduleWizardForm().employeeCheckbox(employee.getShortName()).isDisplayed();
            new Actions(sb.getWrappedDriver()).moveToElement(sb.scheduleWizardForm().employeeCheckbox(employee.getShortName())).perform();
            sb.scheduleWizardForm().employeeCheckbox(employee.getShortName()).click();
        } catch (org.openqa.selenium.NoSuchElementException e) {
            new Actions(sb.getWrappedDriver()).moveToElement(sb.scheduleWizardForm().employeeCheckbox(employee.getLastNameInitials())).perform();
            sb.scheduleWizardForm().employeeCheckbox(employee.getLastNameInitials()).click();
        }
    }

    @Step("В поле \"{dateType.name}\" выбрать дату: {date}")
    private void enterStartOrEndCycleDate(LocalDate date, DateTypeField dateType) {
        sb.scheduleWizardForm().dateStartOrEndInput(dateType.getName()).clear();
        sb.scheduleWizardForm().dateStartOrEndInput(dateType.getName()).sendKeys(date.format(UI_DOTS.getFormat()));
        LOG.info("В поле \"{}\" ввели дату {}", dateType.getName(), date);
    }

    @Step("Ввести в поле {name} время: {startTime} - {endTime}")
    private void enterShiftTime(String name, LocalTime startTime, LocalTime endTime) {
        sb.scheduleWizardForm().timeInput(name).sendKeys(startTime + "-" + endTime);
        sb.scheduleWizardForm().timeInput(name).sendKeys(Keys.ENTER);
    }

    @Step("Кликнуть на кнопку \"добавить +\" перерыв")
    private void clickAddBreakButton() {
        try {
            sb.scheduleWizardForm().addBreakButton().click();
        } catch (org.openqa.selenium.NoSuchElementException ex) {
            sb.scheduleWizardForm().addBreakButtonPlus().click();
        }
    }

    @Step("Кликнуть на кнопку \"добавить +\" вид работы")
    private void clickAddAdditionalWorkButtonInWizard() {
        sb.scheduleWizardForm().addAdditionalWorkButton().waitUntil("Кнопка \"добавить +\" вид работы не активна", DisplayedMatcher.displayed(), 10);
        sb.scheduleWizardForm().addAdditionalWorkButton().click();
    }

    @Step("Кликнуть на кнопку \"добавить +\" вид работы")
    private void clickAddAdditionalWorkButtonInShift() {
        sb.formEditForm().addAdditionalWorkButton().waitUntil("Кнопка \"добавить +\" вид работы не активна", DisplayedMatcher.displayed(), 10);
        sb.formEditForm().addAdditionalWorkButton().click();
    }

    @Step("Нажать на шеврон для выбора доп. работы")
    private void clickAddWorkTypeChevron() {
        sb.scheduleWizardForm().addWorkTypeChevron().waitUntil("Шеврон для выбора доп работы не появился", DisplayedMatcher.displayed(), 5);
        sb.scheduleWizardForm().addWorkTypeChevron().click();
    }

    private void chooseAddWorkType(String addWorkTypeName) {
        sb.scheduleWizardForm().addWorkTypeInList(addWorkTypeName)
                .should(String.format("Тип доп. работы %s отсутствует в списке", addWorkTypeName), DisplayedMatcher.displayed(), 10);
        sb.scheduleWizardForm().addWorkTypeInList(addWorkTypeName).click();
        LOG.info("Выбрали доп. работу с названием \"{}\"", addWorkTypeName);
        sb.scheduleWizardForm().addWorkTypeInList(addWorkTypeName)
                .waitUntil(String.format("Список не закрылся", addWorkTypeName), Matchers.not(DisplayedMatcher.displayed()), 10);
    }

    @Step("Ввести время доп. работы: {startTime} - {endTime}")
    private void enterAddWorkTime(LocalTime startTime, LocalTime endTime) {
        String time = startTime + "-" + endTime;
        sb.scheduleWizardForm().addWorkTypeTime().clear();
        sb.scheduleWizardForm().addWorkTypeTime().sendKeys(time);
        sb.scheduleWizardForm().addWorkTypeTime().sendKeys(Keys.ENTER);
    }

    @Step("Включить переключатель \"Задать цикличность\"")
    private void setCycleArm() {
        sb.scheduleWizardForm().setCycleArm().click();
    }

    @Step("Ввести количество рабочих дней в цикле: {workDaysInCycle}")
    private void enterWorkDaysInCycle(int workDaysInCycle) {
        sb.scheduleWizardForm().workDaysInCycle()
                .should("Поле для ввода рабочих дней в цикле не отобразилось", DisplayedMatcher.displayed(), 10);
        sb.scheduleWizardForm().workDaysInCycle().clear();
        sb.scheduleWizardForm().workDaysInCycle().sendKeys(Integer.toString(workDaysInCycle));
        LOG.info("Ввели количество рабочих дней в цикле \"{}\"", workDaysInCycle);
    }

    @Step("Ввести количество выходных дней в цикле: {freeDaysInCycle}")
    private void enterFreeDaysInCycle(int freeDaysInCycle) {
        sb.scheduleWizardForm().freeDaysInCycle()
                .should("Поле для ввода выходных дней в цикле не отобразилось", DisplayedMatcher.displayed(), 10);
        sb.scheduleWizardForm().freeDaysInCycle().clear();
        sb.scheduleWizardForm().freeDaysInCycle().sendKeys(Integer.toString(freeDaysInCycle));
        LOG.info("Ввели количество выходных дней в цикле \"{}\"", freeDaysInCycle);
    }

    @Step("Нажать на кнопку \"Сформировать\"")
    private void pressFormButton(boolean closePlanningWizardForm) {
        sb.scheduleWizardForm().formButton().click();
        if (closePlanningWizardForm) {
            sb.scheduleWizardForm().waitUntil("Форма мастера планирования не закрылась",
                                              Matchers.not(DisplayedMatcher.displayed()), 60);
            sb.formLayout().popUpForScheduleWizard()
                    .waitUntil("Поп-ап с уведомлением о создании смен не был отображен",
                               DisplayedMatcher.displayed(), 10);
            sb.spinnerLoader().loadingSpinnerPage().waitUntil("Спиннер загрузки все еще отображается",
                                                              Matchers.not(DisplayedMatcher.displayed()), 40);
        }
    }

    @Step("В раскрывшемся списке выбрать \"{requestType}\"")
    private void selectRequestTypeInScheduleWizard(String requestType) {
        ElementsCollection<AtlasWebElement> requestTypeList = sb.scheduleWizardForm().typeButtons();
        requestTypeList.waitUntil("Список типов запросов не загрузился",
                                  Matchers.not(requestTypeList.isEmpty()), 5);
        systemSleep(5); //без этого ожидания может быть выбран не тот тип запроса
        requestTypeList.stream()
                .filter(extendedWebElement -> extendedWebElement.getText().trim().equals(requestType))
                .findFirst()
                .orElseThrow(() -> new AssertionError(String.format("Schedule message. Кнопки с выбранным типом %s не было в списке", requestType)))
                .click();

        LOG.info("В раскрывшемся списке выбираем \"{}\"", requestType);
    }

    @Step("Проверить, что в промежутке {interval.startDate} по {interval.endDate} были добавлены смены и доп. работы")
    private void assertShiftsAndAddWorksCreated(EmployeePosition ep, DateInterval interval, ScheduleWorker sw, AdditionalWork addWork,
                                                LocalTime startShift, LocalTime endShift, LocalTime startAddWork, LocalTime endAddWork, Long lunchInMinutes) {
        List<LocalDate> dates = interval.getBetweenDatesList();
        Iterator<LocalDate> it = dates.iterator();
        LocalDate date;
        while (it.hasNext()) {
            date = it.next();
            assertCreateShift(ep, new DateTimeInterval(
                    LocalDateTime.of(date, startShift), LocalDateTime.of(date, endShift)), sw, lunchInMinutes);
            assertAddWorksAdded(sw, ep, new DateTimeInterval(LocalDateTime.of(date, startAddWork), LocalDateTime.of(date, endAddWork)), addWork);
        }
    }

    @Step("Проверить добавление доп. работы к смене сотрудника {ep}")
    private void assertAddWorksAdded(ScheduleWorker scheduleWorker, EmployeePosition ep, DateTimeInterval interval, AdditionalWork additionalWork) {
        LocalDate date = interval.getStartDate();
        LOG.info("Проверяем, добавление доп. работы к смене сотрудника {} за {}", ep, date);
        if (!sb.formTopBar().monthSelected().getText().equals(date.getMonth().getDisplayName(TextStyle.FULL_STANDALONE, new Locale("ru")))) {
            clickForward();
            scheduleWorker = new ScheduleWorker(sb);
        }
        AtlasWebElement addWorkElement = scheduleWorker.getAdditionalWorkElement(ep, date);
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertNotNull(addWorkElement, "Элемент доп. работы не отображается на UI");
        Shift shift = ShiftRepository.getShift(ep, date, ShiftTimePosition.FUTURE_WITH_NEXT_MONTH);
        List<ShiftAddWorkLink> additionalWorks = shift.getAdditionalWork();
        softAssert.assertTrue(additionalWorks.size() == 1, "Количество доп. работ не равно 1");
        Assert.assertTrue(additionalWorks.size() > 0, "У смены нет доп. работы");
        ShiftAddWorkLink addedWork = additionalWorks.get(0);
        softAssert.assertEquals(addedWork.getFrom(), interval.getStartDateTime(), "Время начала не совпадает");
        softAssert.assertEquals(addedWork.getTo(), interval.getEndDateTime(), "Время окончания не совпадает");
        softAssert.assertEquals(addedWork.getAdditionalWork().getTitle(), additionalWork.getTitle(), "Тип доп. работы не совпадает");
        softAssert.assertAll();
    }

    @Step("Проверить, что в промежутке {interval.startDate} по {interval.endDate} были добавлены запросы расписания")
    private void assertScheduleRequestsAdded(EmployeePosition ep, DateInterval interval, ScheduleWorker sw,
                                             ScheduleRequestType type, OrgUnit unit, ScheduleRequestStatus status) {
        List<LocalDate> dates = interval.getBetweenDatesList();
        Iterator<LocalDate> it = dates.iterator();
        LocalDate date;
        while (it.hasNext()) {
            date = it.next();
            if (!sb.formTopBar().monthSelected().getText().equals(date.getMonth().getDisplayName(TextStyle.FULL_STANDALONE, new Locale("ru")))) {
                clickForward();
                sw = new ScheduleWorker(sb);
            }
            assertRequestAdding(ep, date, type, sw, unit, status);
        }
    }

    @Step("Нажать на шеврон для выбора запроса отсутствия")
    private void clickRequestChevron() {
        sb.scheduleWizardForm().addRequestTypeChevron().click();
    }

    /**
     * Необходимо передавать order + 1, чтобы в отчете отображался порядковый номер, начиная с 1
     */
    @Step("Кликнуть на кнопку \"Многоточие\" у свободной смены № {order}")
    private void clickThreeDotsForFreeShift(int order) {
        LOG.info("Кликаем на кнопку \"Многоточие\" у свободной смены №{}", order);
        sb.freeShiftList().threeDotsMenu(order - 1).waitUntil("Кнопка \"Многоточие\" не отображается", DisplayedMatcher.displayed(), 5);
        sb.freeShiftList().threeDotsMenu(order - 1).click();
    }

    @Step("Выбрать \"{option}\" в выпадающем меню")
    private void clickEditButtonForFreeShift(String option) {
        LOG.info("Выбираем в выпадающем списке вариант \"{}\"", option);
        sb.freeShiftList().threeDotsMenuItem(option)
                .waitUntil("schedule message. Кнопки с выбранным типом не было в списке", DisplayedMatcher.displayed(), 5);
        sb.freeShiftList().threeDotsMenuItem(option).click();
    }

    @Step("Указать причину привлечения \"{option.title}\"")
    private void selectHiringReasonForFreeShift(ShiftHiringReason option) {
        String name = option.getTitle();
        LOG.info("Указываем причину привлечения \"{}\"", name);
        sb.formEditForm().hiringReasonInput().should("Поле ввода причины привлечения не отображается", DisplayedMatcher.displayed(), 5);
        new Actions(sb.getWrappedDriver()).moveToElement(sb.formEditForm().hiringReasonInput()).perform();
        sb.formEditForm().hiringReasonInput().click();
        List<String> availableReasons = sb.formEditForm().hiringReasonOptions().stream().map(AtlasWebElement::getText).collect(Collectors.toList());
        LOG.info("Доступные причины привлечения: {}", availableReasons);
        Allure.addAttachment("Доступные причины привлечения", availableReasons.toString());
        sb.formEditForm().hiringReasonOption(name)
                .waitUntil("schedule message. Кнопки с выбранным типом не было в списке", DisplayedMatcher.displayed(), 5);
        sb.formEditForm().hiringReasonOption(name).click();
    }

    @Step("Указать категорию позиции \"{option.name}\"")
    private void selectPositionCategoryForFreeShift(PositionCategory option) {
        String name = option.getName();
        LOG.info("Указываем категорию позиции \"{}\"", name);
        sb.formEditForm().positionCategoryInput().waitUntil("Поле ввода категории позиции не отображается", DisplayedMatcher.displayed(), 5);
        sb.formEditForm().positionCategoryInput().click();
        systemSleep(1); //без этого ожидание может быть выбран не тот пункт
        sb.formEditForm().positionCategoryOption(name)
                .waitUntil("schedule message. Кнопки с выбранным типом не было в списке", DisplayedMatcher.displayed(), 5);
        sb.formEditForm().positionCategoryOption(name).click();
    }

    @Step("Навести на сотрудника")
    private void hoverEmployee(EmployeePosition employeePosition) {
        new Actions(sb.getWrappedDriver()).moveToElement(sb.formLayout().nameButton(employeePosition.getId())).perform();
        sb.formLayout().fullNameHint().waitUntil("Всплывающий элемент с ФИО сотрудника не отобразился",
                                                 DisplayedMatcher.displayed(), 30);
    }

    @Step("Проверить, что на всплывающей подсказке отображаются ФИО сотрудника")
    private void checkHint(Employee employee) {
        String employeeNameFromHint = sb.formLayout().fullNameHint().getText();
        if (employeeNameFromHint.contains("\n")) {
            employeeNameFromHint = employeeNameFromHint.replace("\n", " ");
        }
        assertEquals(employee.getFullName(), employeeNameFromHint,
                     "Имя сотрудника сотрудника на подсказке не совпало с именем в списке");
        Allure.addAttachment("Проверка", "Имя сотрудника в списке и на подсказке совпало");
    }

    @Step("Проверить, что открывающие и закрывающие смены отобразились")
    private void checkShiftCounter() {
        sb.formLayout().shiftCounterDesignation(TypeShift.OPENING_SHIFTS.getTypeShift())
                .waitUntil("Открывающие смены не отобразились",
                           DisplayedMatcher.displayed(), 5);
        sb.formLayout().shiftCounterDesignation(TypeShift.CLOSING_SHIFTS.getTypeShift())
                .waitUntil("Закрывающие смены не отобразились",
                           DisplayedMatcher.displayed(), 5);
    }

    @Step("Закрыть боковую панель планового графика")
    private void closePlannedScheduleSidebar() {
        sb.downloadForm().closeSidebar().click();
        sb.downloadForm()
                .should("Форма выгрузки графика не закрылась",
                        Matchers.not(DisplayedMatcher.displayed()), 5);
    }

    @Step("Открыть форму должности у сотрудника {employeePosition}")
    private void openEmployeesPositionForm(EmployeePosition employeePosition) {
        LOG.info("Открываем форму должности у {}", employeePosition);
        sb.formLayout().employeePositionsButtons(employeePosition.getId()).click();
        sb.addNewEmployeeForm()
                .waitUntil("Форма должности не открылась",
                           DisplayedMatcher.displayed(), 5);
    }

    @Step("Проверить, что ячейки смен сотрудника {emp.employee} не активны на следующих датах: {dates}")//
    private void assertInactiveEmptyCells(EmployeePosition emp, ScheduleWorker scheduleWorker, List<LocalDate> dates) {
        for (LocalDate date : dates) {
            assertInactiveEmptyCell(emp, scheduleWorker, date);
        }
    }

    private void assertInactiveEmptyCell(EmployeePosition emp, ScheduleWorker scheduleWorker, LocalDate date) {
        Assert.assertNull(scheduleWorker.getScheduleShiftElement(emp, date),
                          String.format("Ячейка смены сотрудника %s за %s не свободна", emp.getEmployee().getFullName(), date));
        clickOnEmptyCell(emp, date, scheduleWorker);
        Assert.assertThrows(WaitUntilException.class, this::clickOnPlusCellOnGraph);
    }

    @Step("Указать функциональную роль \"{option.name}\"")
    private void selectPositionGroupForFreeShift(PositionGroup option) {
        String name = option.getName();
        LOG.info("Указываем группу позиции \"{}\"", name);
        sb.formEditForm().positionGroupInput().waitUntil("Поле ввода функциональной роли не отображается", DisplayedMatcher.displayed(), 5);
        sb.formEditForm().positionGroupInput().click();
        sb.formEditForm().positionGroupOption(name)
                .waitUntil("schedule message. Кнопки с выбранным типом не было в списке", DisplayedMatcher.displayed(), 5);
        systemSleep(1); //без этого ожидание может быть выбран не тот пункт
        sb.formEditForm().positionGroupOption(name).click();
    }

    @Step("Проверить, что появилось окно об ошибке с сообщениями о конфликтах")
    private void assertConstraintViolationDialogAppeared(EmployeePosition ep, LocalDateTime localDateTimeNow, int omId, List<String> messages, VariantsOfFunctions function) {
        sb.constraintViolationDialog().waitUntil("Окно c конфликтами не отобразилось", DisplayedMatcher.displayed(), 10);
        List<String> constrViolationMessages = sb.constrViolationMessages()
                .stream().map(AtlasWebElement::getText)
                .collect(Collectors.toList());
        Allure.addAttachment("Сообщения о конфликтах", String.format("Сообщение содержит следующие конфликты: %s", constrViolationMessages));
        LOG.info("Сообщение содержит следующие конфликты: {}", constrViolationMessages);
        SoftAssert softAssert = new SoftAssert();
        messages.removeAll(constrViolationMessages);
        softAssert.assertTrue(messages.isEmpty(), String.format("Не отображены следующие сообщения: %s", messages));
        if (constrViolationMessages.contains(ConstraintViolations.VIOLATION_HOUR_NORMS.getName())) {
            List<String> names = sb.constrList(ConstraintViolations.VIOLATION_HOUR_NORMS.getName())
                    .stream().map(e -> e.getAttribute("textContent"))
                    .collect(Collectors.toList());
            LOG.info("Перечислены следующие сотрудники: {}", names);
            softAssert.assertTrue(names.contains(ep.toString()), String.format("В списке отсутствует сотрудник %s", ep));
            Allure.addAttachment("Сотрудники", String.format("Нарушены нормы часов у следующих сотрудников: %s", names));
        }
        LocalDateTime dateTimeApi;
        if (function == VariantsOfFunctions.PUBLICATION) {
            dateTimeApi = RosterRepository.getActiveRosterThisMonth(omId).getPublicationTime();
        } else {
            dateTimeApi = RosterRepository.getActiveRosterThisMonth(omId).getOnApprovalTime();
        }
        if (dateTimeApi != null) {
            long minutesDiff = ChronoUnit.MINUTES.between(dateTimeApi, localDateTimeNow);
            softAssert.assertTrue(minutesDiff > 3, "Несмотря на конфликты, график был отправлен на утверждение или опубликован");
        }
        softAssert.assertAll();
    }

    @Step("Проверить, что имена сотрудников скрыты")
    private void assertPersonalDataVisibility(boolean hasPermissions) {
        changeStepNameIfTrue(hasPermissions, "Проверить, что имена сотрудников отображаются");
        sb.formLayout().employeeNameButtons().forEach(e -> e.should(getMatcherDependingOnPermissions(hasPermissions)));
    }

    @Step("Проверить добавление конфликта для сотрудника {ep} за дату {date}")
    private void checkConstraintViolation(int omId, EmployeePosition ep, LocalDate date, ScheduleWorker sw, String text, boolean isDateIncluded) {
        String name = ep.getEmployee().getFullName();
        SoftAssert softAssert = new SoftAssert();
        //временно отключена проверка конфликтов на UI, так как конфликты не отображаются при удаленном запуске
        //sb.formLayout().conflictCircles().should("Красного кружочка в строке \"Конфликты\" над расписанием не обнаружено",
        //                                         Matchers.hasSize(Matchers.greaterThan(0)));
        //sw.onConflictCircleClicker(date.getDayOfMonth());
        //softAssert.assertFalse(sb.formLayout().textInConflictCircle(text, name).isEmpty(), String.format("На UI нет текста \"%s\"", text));
        //Allure.addAttachment("Конфликт на UI", String.format("В диалоговом окне, появившемся после нажатия на красный кружок за дату %s, содержатся сообщения %s",
        //                                                     date, sb.formLayout().textInConflictCircle(text, name).stream().map(WebElement::getText).collect(Collectors.toList())));
        ShiftTimePosition shiftTimePosition = LocalDate.now().getMonthValue() != date.getMonthValue() ? ShiftTimePosition.NEXT_MONTH : ShiftTimePosition.ALLMONTH;
        List<String> textConstrViolations = CommonRepository.getConstraintViolations(omId, ep, shiftTimePosition);
        Assert.assertNotNull(textConstrViolations);
        softAssert.assertTrue(textConstrViolations.stream().anyMatch(t -> t.contains(String.format("%s %s", text, name)) && t.contains(date.toString()) == isDateIncluded),
                              String.format("Конфликт для сотрудника %s не отразился в API", ep));
        Allure.addAttachment("Конфликт в API", String.format("В API содержатся следующие сообщения о конфликтах %s", textConstrViolations));
        softAssert.assertAll();
    }

    @Step("Выбрать периодичность \"{periodicity.repeatType}\"")
    private void selectPeriodicityForRequest(Periodicity periodicity) {
        sb.formEditForm().periodicityInput().click();
        systemSleep(2); //без этого ожидания может что-нибудь другое выбраться
        sb.formEditForm().requestPeriod(periodicity.getRepeatType()).click();
        LOG.info("Выбрали периодичность: {}", periodicity.getRepeatType());
    }

    @Step("Ввести дату окончания повтора {endRepeatDate}")
    private void enterEndRepeatDate(LocalDate endRepeatDate) {
        sb.formEditForm().endRepeatDate().clear();
        sb.formEditForm().endRepeatDate().sendKeys(endRepeatDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
        LOG.info("Ввели дату окончания повтора: {}", endRepeatDate);
    }

    @Step("Проверить наличие бэйджей с функциональными ролями у сотрудников {eps}")
    private void assertBadgesWithPositionGroups(List<EmployeePosition> eps, Set<Integer> posGroupIds) {
        SoftAssert softAssert = new SoftAssert();
        List<PositionGroup> posGroups = PositionGroupRepository.getAllPositionGroups();
        for (EmployeePosition ep : eps) {
            int id = ep.getPosition().getPositionGroupId();
            if (posGroupIds.contains(id)) {
                String posGroupName = posGroups.stream()
                        .filter(e -> e.getId() == id)
                        .findFirst()
                        .orElseThrow(NoSuchElementException::new).getName();
                softAssert.assertTrue(checkTag(ep.getId(), posGroupName, true),
                                      String.format("Под сотрудником %s отсутствует бэйдж с функциональной ролью %s",
                                                    ep, posGroupName));
            }
        }
        softAssert.assertAll();
        Allure.addAttachment("Проверка наличия бэйджей",
                             String.format("Под сотрудниками %s присутствуют бэйджи с функциональными ролями", eps));
    }

    @Step("Проверить, что у свободной смены за {shift.dateTimeInterval.startDateTime.date} причина привлечения изменилась на {reason.title}")
    private void assertEditFreeShift(OrgUnit unit, Roster activeRoster, ShiftHiringReason reason, Shift shift) {
        Shift newShift = shift.refreshShift();
        Assert.assertNotNull(newShift, "Смена не найдена в api");
        SoftAssert softAssert = new SoftAssert();
        Roster rosterAfter = RosterRepository.getActiveRosterThisMonth(unit.getId());
        softAssert.assertEquals(rosterAfter.getVersion(), activeRoster.getVersion(), "Версия графика изменилась");
        softAssert.assertEquals(newShift.getHiringReasonText(), reason.getTitle(), "Причина привлечения не изменилась");
        softAssert.assertEquals(newShift.getDateTimeInterval().toString(), shift.getDateTimeInterval().toString(), "Время смены изменилось");
        softAssert.assertAll();
    }

    @Step("Проверить, что была добавлена свободная смена за {interval.startDateTime.date} с причиной привлечения \"{reason.title}\"")
    private void assertAddFreeShift(OrgUnit unit, DateTimeInterval interval, PositionGroup posGroup, ShiftHiringReason reason) {
        List<Shift> freeShiftsAfter = ShiftRepository.getShiftsForRoster(RosterRepository.getActiveRosterThisMonth(unit.getId()).getId(),
                                                                         interval.toDateInterval())
                .stream()
                .filter(s -> s.getEmployeePositionId() == 0)
                .collect(Collectors.toList());
        Assert.assertFalse(freeShiftsAfter.isEmpty(), "Свободные смены не появились");
        Shift shift = freeShiftsAfter.iterator().next();
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(shift.getDateTimeInterval(), interval, "Время смены не совпало");
        softAssert.assertEquals(shift.getPosGroup().getId(), posGroup.getId(), "ID функциональной роли не совпал");
        if (reason == null) {
            softAssert.assertEquals(shift.getHiringReasonText(), "", "У смены есть причина привлечения");
            changeStepName(String.format("Проверить, что была добавлена свободная смена за %s без причины привлечения",
                                         interval.getStartDateTime().toLocalDate()));
        } else {
            softAssert.assertEquals(shift.getHiringReasonText(), reason.getTitle(), "Причина привлечения не совпала");
        }
        softAssert.assertAll();
    }

    @Step("В раскрывшемся меню выбрать \"{employeeVariant.name}\"")
    private void chooseEmployeeFunction(String name, EmployeeVariants employeeVariant) {
        sb.subdivisionProperties().employeeButton(name, employeeVariant.getVariant())
                .waitUntil(String.format("Кнопка %s  отсутсвует", employeeVariant.getVariant()), DisplayedMatcher.displayed(), 5);
        sb.subdivisionProperties().employeeButton(name, employeeVariant.getVariant()).click();
        LOG.info("В раскрывшемся меню сотрудника {} выбираем \"{}\"", name, employeeVariant.getName());
    }

    @Step("Ввести значение \"{value}\" для атрибута \"{title}\"")
    private void enterAttributeValue(String title, String value) {
        sb.subdivisionProperties().attributeForm()
                .waitUntil("Форма для ввода значений атрибутов не открылась", DisplayedMatcher.displayed(), 5);
        sb.subdivisionProperties().attributeForm().attributeValueInput(title).click();
        sb.subdivisionProperties().attributeForm().attributeValueInput(title).sendKeys(value);
        LOG.info("Вводим значение \"{}\" для атрибута \"{}\"", value, title);
    }

    @Step("Сохранить значение атрибута")
    private void saveAttributeValueButtonClick() {
        sb.subdivisionProperties().attributeForm().saveButton().click();
        LOG.info("Нажимаем кнопку \"Сохранить\"");
    }

    @Step("Проверить, что сотрудник {ep} выделен жирным цветом и значение атрибута \"{value}\" добавлено в API")
    private void assertEmployeeMarkedByBoldFont(EmployeePosition ep, EntityPropertiesKey propertiesKey, String value) {
        sb.formLayout().employeeButtonFontBold(ep.getId())
                .should(String.format("Имя сотрудника %s не выделено жирным шрифтом", ep), DisplayedMatcher.displayed(), 5);
        EntityProperty property = EntityPropertyRepository.getEntityPropertyByKey(MathParameterEntities.EMPLOYEE_POSITION,
                                                                                  ep.getEmployeePosition().getId(), propertiesKey.getKey());
        Assert.assertNotNull(property);
        Assert.assertEquals(property.getValue(), value);
    }

    @Step("Сравнить норму часов временного сотрудника {temp} и постоянного сотрудника {perm} при настройке \"norm.fullIntervalNorms\" = \"{propertyValue}\"")
    private void assertEmployeeHoursNorm(EmployeePosition temp, EmployeePosition perm,
                                         double tempNormBefore, double permNormBefore, String propertyValue,
                                         int rosterId, LocalDate fromDate, LocalDate toDate) {
        LocalDate start = LocalDateTools.getFirstDate();
        double tempNormAfter = DeviationFromStandardRepository.getRoundedStandardDeviation(rosterId, start, toDate, temp);
        double permNormAfter = DeviationFromStandardRepository.getRoundedStandardDeviation(rosterId, start, toDate, perm);
        double tempPlanHours = Double.parseDouble(sb.formLayout().employeePlanWorkingHours(temp.getId()).getText());
        double permPlanHours = Double.parseDouble(sb.formLayout().employeePlanWorkingHours(perm.getId()).getText());
        String tempName = temp.toString();
        String permName = perm.toString();
        LOG.info("Сравнение норм часов временного сотрудника {} и постоянного сотрудника {} c периодом работы с {} по {}", tempName, permName, fromDate, toDate);
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertTrue(Math.abs(tempPlanHours - tempNormAfter) <= 0.1,
                              String.format("Разница между значением норматива для временного сотрудника на UI %f и в API %f составила %f",
                                            tempPlanHours, tempNormAfter, tempPlanHours - tempNormAfter));
        softAssert.assertTrue(Math.abs(permPlanHours - permNormAfter) <= 0.1,
                              String.format("Разница между значением норматива для постоянного сотрудника на UI %f и в API %f составила %f",
                                            permPlanHours, permNormAfter, permPlanHours - permNormAfter));
        String logChanged = "%s сотрудник %s: норма часов изменилась: было %f, стало %f";
        String logNotChanged = "%s сотрудник %s: норма часов не изменилась: было %f, стало %f";
        String permanentNotChanged = String.format(logNotChanged, "Постоянный", permName, permNormBefore, permNormAfter);
        String temporaryChanged = String.format(logChanged, "Временный", tempName, tempNormBefore, tempNormAfter);
        if (propertyValue.equals("TEMPORARY")) {
            softAssert.assertEquals(tempNormBefore, tempNormAfter, temporaryChanged);
            softAssert.assertTrue(permNormBefore > permNormAfter, permanentNotChanged);
        } else if (propertyValue.equals("ALL")) {
            softAssert.assertEquals(tempNormBefore, tempNormAfter, temporaryChanged);
            softAssert.assertEquals(permNormBefore, permNormAfter,
                                    String.format(logChanged, "Постоянный", permName, permNormBefore, permNormAfter));
        } else {
            softAssert.assertTrue(tempNormBefore > tempNormAfter,
                                  String.format(logNotChanged, "Временный", tempName, tempNormBefore, tempNormAfter));
            softAssert.assertTrue(permNormBefore > permNormAfter, permanentNotChanged);
        }
        softAssert.assertAll();
    }

    @Step("Проверить, что во всплывающем окне отобразилась строка \"{expectedAttribute}\"")
    private void assertAttributeDisplayedInPopup(EmployeePosition employeePosition, String expectedAttribute) {
        List<String> attributesInPopup = sb.formLayout().attributesInPopup(employeePosition.getId()).stream()
                .map(e -> e.getAttribute("textContent").replaceAll("\n", "").trim())
                .collect(Collectors.toList());
        Assert.assertTrue(attributesInPopup.size() != 0, String.format("У сотрудника %s нет атрибутов во всплывающем окне", employeePosition));
        Assert.assertTrue(attributesInPopup.contains(expectedAttribute),
                          String.format("Всплывающее окно с информацией о часах сотрудника не содержит атрибут \"%s\"", expectedAttribute));
        Allure.addAttachment("Проверка", String.format("Во всплывающем окне, отображенном при наведении на норму часов у сотрудника %s, появился атрибут \"%s\"",
                                                       employeePosition, expectedAttribute));
    }

    @Step("Проверить отсутствие предупреждения о том, что произошло изменение лимита часов на подразделение")
    private void assertLimitWarningNotDisplayed() {
        Assert.assertTrue(sb.formLayout().limitWarning().isEmpty(),
                          "Отобразилось предупреждение об изменении лимита часов не подразделение");
    }

    @Step("Проверить наличие отметок на сменах")
    private void assertMarksOnShiftsCreated(int omId, List<EmployeePosition> eps) {
        ScheduleWorker sw = new ScheduleWorker(sb);
        for (EmployeePosition ep : eps) {
            Assert.assertNotNull(sw.getPresenceMarkElement(ep));
            return;
        }
    }

    @Step("Нажать на кнопку \"Утвердить\"")
    private void clickApproveButton() {
        sb.formApprovalForm().waitUntil("Форма утверждения табеля не загрузилась", DisplayedMatcher.displayed());
        sb.formApprovalForm().approveButton().click();
        LOG.info("Нажали \"Утвердить\"");
    }

    @Step("Проверить, что табель утвержден")
    private void assertTableConfirmed(int omId, String confirmMessage) {
        sb.spinnerLoader().loadingSpinnerPage().waitUntil("Спиннер все еще крутится", Matchers.not(DisplayedMatcher.displayed()), 5);
        SoftAssert softAssert = new SoftAssert();
        waitForRosterStatusToChange(GraphStatus.ON_APPROVAL, false);
        softAssert.assertTrue(sb.formLayout().greyTimesheetIndicator().size() > 0,
                              "Прямоугольник над табелем не окрасился в серый цвет");
        Roster workedRoster = RosterRepository.getZeroRosterOrReturnNull(omId, new DateInterval());
        softAssert.assertEquals(workedRoster.getWorkedApprove(), LocalDate.now().minusDays(1).format(API.getFormat()), "Дата утверждения ростера не совпадает с сегодняшним днем");
        LOG.info(String.format("Дата утверждения ростера в API: %s", workedRoster.getWorkedApprove()));
        softAssert.assertAll();
        Allure.addAttachment("Проверка", String.format("Надпись над табелем окрасилась в серый цвет, дата утверждения ростера %s", workedRoster.getWorkedApprove()));
    }

    @Step("Проверить даты утверждения табеля")
    private void assertDatesForTableConfirm(int omId) {
        SoftAssert softAssert = new SoftAssert();
        Roster workedRoster = RosterRepository.getZeroRosterOrReturnNull(omId, new DateInterval());
        LocalDate startDate = !workedRoster.getWorkedApprove().equals("") ? (LocalDate.parse(workedRoster.getWorkedApprove()).plusDays(1)) : LocalDateTools.getFirstDate();
        sb.formApprovalForm()
                .waitUntil("Форма утверждения не отобразилась", DisplayedMatcher.displayed(), 10);
        String startConfirmDate = sb.formApprovalForm().disabledDate("Дата начала").get(1).getAttribute("value");
        String endConfirmDate = sb.formApprovalForm().disabledDate("Дата окончания").get(1).getAttribute("value");
        LOG.info("Дата начала табеля для утверждения {}", startConfirmDate);
        LOG.info("Дата окончания табеля для утверждения {}", endConfirmDate);
        softAssert.assertEquals(startConfirmDate, startDate.format(UI_DOTS.getFormat()));
        softAssert.assertAll();
        // обработка ситуации, когда LocalDate.now это уже вчерашний день (актуально для мастера почты)
        try {
            Assert.assertEquals(endConfirmDate, LocalDate.now().minusDays(1).format(UI_DOTS.getFormat()));
        } catch (AssertionError assertionError) {
            Assert.assertEquals(endConfirmDate, LocalDate.now().format(UI_DOTS.getFormat()));
        }
        Allure.attachment("Проверка дат", String.format("Табель будет утвержден с %s по %s", startConfirmDate, endConfirmDate));
    }

    @Step("Проверить, что фактическая смена подсвечена красным")
    private void assertShiftCellDiffersFromPlan(EmployeePosition ep, LocalDate date, boolean differ) {
        ScheduleWorker sw = new ScheduleWorker(sb);
        ElementsCollection<AtlasWebElement> elements = sw.getShiftPlanMismatch(ep, date);
        if (differ) {
            elements.should(String.format("Ячейка со сменой за %s сотрудника %s не подсвечена красным", date, ep), Matchers.hasSize(Matchers.greaterThan(0)));
        } else {
            changeStepName("Проверить, что фактическая смена не подсвечена красным");
            elements.should(String.format("Ячейка со сменой за %s сотрудника %s подсвечена красным", date, ep), Matchers.hasSize(Matchers.equalTo(0)));
        }
    }

    @Step("Ввести свободный комментарий при удалении смены")
    private void enterComment(String comment) {
        sb.shiftDeletionDialog().should("Окно для ввода комментария не появилось", DisplayedMatcher.displayed(), 5);
        LOG.info("Вводим свободный комментарий");
        sb.shiftDeletionDialog().commentText().sendKeys(comment);
    }

    @Step("Нажать \"Удалить\"")
    private void pressDeleteButton() {
        LOG.info("Нажимаем \"Удалить\"");
        sb.shiftDeletionDialog().deleteButton().click();
    }

    @Step("Проверить, что смена не удалилась")
    private void assertShiftNotDeleted(EmployeePosition ep, Shift shift, ScheduleWorker sw) {
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertTrue(sb.shiftDeletionDialog().isDisplayed(), "Окно ввода комментария все еще отображается");
        softAssert.assertNotNull(
                sw.getScheduleShiftElement(ep, shift.getDateTimeInterval().getStartDate()), "Смена удалилась на UI");
        List<Shift> shifts = ShiftRepository.getShifts(ep, ShiftTimePosition.PAST);
        softAssert.assertTrue(shifts.contains(shift),
                              String.format("Смена сотрудника %s за %s удалилась", ep, shift.getDateTimeInterval().getStartDate()));
    }

    @Step("Кликнуть на иконку конверта")
    private void clickEnvelope() {
        sb.mainHeader().envelope().should("Иконка конверта не отобразилась",
                                          DisplayedMatcher.displayed(), 5);
        sb.mainHeader().envelope().click();
        LOG.info("Кликнули на иконку конверта");

    }

    @Step("Проверить, что открылся раздел \"Уведомления\" со списком уведомлений")
    private void assertNotificationsOpened() {
        MessagesPage msp = new Atlas(new WebDriverConfiguration(sb.getWrappedDriver())).create(sb.getWrappedDriver(), MessagesPage.class);
        msp.messageListPanel().allMessages().should("Иконка конверта не отобразилась",
                                                    Matchers.hasSize(Matchers.greaterThan(0)));
        LOG.info("Открылся раздел \"Уведомления\", в списке {} сообщений", msp.messageListPanel().allMessages().size());
        Allure.addAttachment("Раздел \"Уведомления\"", String.format("В списке %d сообщений", msp.messageListPanel().allMessages().size()));
    }

    @Step("Проверить отображение переходящей смены сотрудника {ep} между месяцами в графике обоих месяцев")
    private void assertDisplayOvernightShift(EmployeePosition ep, ScheduleWorker sw, boolean pastMidnight) {
        LocalDate lastDate = LocalDateTools.getLastDate();
        LocalDate nextDay = lastDate.plusDays(1);
        if (pastMidnight) {
            AtlasWebElement shift = sw.getPlanOrFactShiftElement(ep, lastDate, false);
            Assert.assertNotNull(shift);
            Allure.addAttachment("Проверка отображения переходящей смены",
                                 String.format("Переходящая смена сотрудника %s отображается в следующем месяце", ep));
            String shiftHours = sw.getShiftHours(ep, lastDate, false);
            Assert.assertEquals(shiftHours, "0-6", "Часы, отображаемые в ячейке смены,не соответствуют ожидаемым");
        } else {
            Assert.assertNull(sw.getPlanOrFactShiftElement(ep, lastDate, true));
            Allure.addAttachment("Проверка отображения переходящей смены",
                                 String.format("Переходящая смена сотрудника %s не отображается в следующем месяце", ep));

        }

    }

    /**
     * Возвращает ссылку на pdf документ с плановым графиком
     */
    private String getLinkOnPlan() {
        return sb.downloadForm().downloadButton().getAttribute(HREF);
    }

    /**
     * Возвращает текст ошибки, который будет отображаться на UI при пересечении смен
     */
    private String shiftOverlapErrorMessageGenerator(OrgUnit unit, EmployeePosition ep, Shift shift) {
        return String.format("Пересечение со сменой в подразделении %s по сотруднику %s c %s по %s", unit.getName(), ep.getEmployee().getFullName(),
                             shift.getDateTimeInterval().getStartDateTime().format(UI_DATETIME_WITH_SPACE.getFormat()),
                             shift.getDateTimeInterval().getEndDateTime().format(UI_DATETIME_WITH_SPACE.getFormat()));
    }

    @Step("Нажать на ячейку в строке \"Свободные смены\" за дату {date}")
    private void clickCellInFreeShiftsRowOnSelectedDate(LocalDate date) {
        sb.freeShiftCellForSpecificDayOfMonth(date.getDayOfMonth())
                .waitUntil("Свободная смена не появилась", DisplayedMatcher.displayed(), 30);
        LOG.info("Нажать на ячейку в строке \"Свободные смены\" за дату " + date);
        sb.freeShiftCellForSpecificDayOfMonth(date.getDayOfMonth()).click();
    }

    @Step("Раскрыть последний список в поле \"Сотрудник\"")
    private void expandLastEmployeeList() {
        sb.freeShiftList().waitUntil("Окно со свободными сменами не открылось", DisplayedMatcher.displayed(), 30);
        LOG.info("Раскрыть последний список в поле \"Сотрудник\"");
        List<AtlasWebElement> employees = sb.freeShiftList().employees();
        employees.get(employees.size() - 1).click();
    }

    @Step("Выбрать сотрудника мобильной группы")
    private void selectMobileTeamEmployee(Employee employee) {
        sb.freeShiftList().employeeListLoader().waitUntil("Список доступных сотрудников не прогрузился", Matchers.not(DisplayedMatcher.displayed()), 360);
        LOG.info("Выбрать в списке сотрудника под именем " + employee.getEmployee());
        AtlasWebElement el = sb.freeShiftList().getAllEmployeesElements().stream().filter(e -> e.getText().contains(employee.getFullName()))
                .findFirst().orElseThrow(() -> new AssertionError("Cотрудник под именем " + employee.getFullName() + " не найден в списке"));
        el.click();
    }

    @Step("Нажать \"Сохранить\"")
    private void clickOnButtonSaved() {
        sb.freeShiftList().save().waitUntil("Кнопка \"Сохранить\" не отображается", DisplayedMatcher.displayed(), 30);
        sb.freeShiftList().save().click();
        sb.spinnerLoader().loadingSpinnerPage().waitUntil("Страница не загрузилась", Matchers.not(DisplayedMatcher.displayed()), 30);
    }

    @Step("Нажать на название должности созданного назначения")
    private void clickOnCreatedAppointmentPositionTitle(EmployeePosition employeePosition) {
        sb.spinnerLoader().loadingSpinnerPage().waitUntil("Страница не загрузилась", Matchers.not(DisplayedMatcher.displayed()), 30);
        AtlasWebElement el = sb.formLayout().employeePositionsByName(employeePosition.getEmployee().toString(), employeePosition.getPosition().getName())
                .stream().reduce((e1, e2) -> e2).orElseThrow(() -> new AssertionError("Cотрудник под именем " + employeePosition.getEmployee().toString() +
                                                                                              " и должностью " + employeePosition.getPosition().getName() + " не найден в списке"));
        el.click();
        sb.addNewEmployeeForm().waitUntil("Форма должности не загрузилась", DisplayedMatcher.displayed(), 20);
    }

    @Step("Проверить, что название должности, ставка, табельный номер совпадают с данными из назначения в стороннем подразделении")
    private void assertOnCreatedAppointmentPositionTitle(EmployeePosition employeePosition, OrgUnit unit) {
        EmployeePosition mobileEmployeePosition = EmployeePositionRepository.getEmployeePositions(unit.getId())
                .stream().filter(e -> e.getEmployee().getFullName().contains(employeePosition.getEmployee().getFullName()))
                .reduce((f, s) -> s).orElseThrow(() -> new AssertionError("Сотрудника мобильной смены " +
                                                                                  employeePosition.getEmployee().getFullName() + " не найдено на странице"));
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(employeePosition.getPosition().getName(), mobileEmployeePosition.getPosition().getName(), "Должности не совпадают");
        softAssert.assertEquals(employeePosition.getRate(), mobileEmployeePosition.getRate(), "Ставка не совпадает");
        softAssert.assertEquals(employeePosition.getCardNumber(), mobileEmployeePosition.getCardNumber(), "Табельные номера не совпадают");
        softAssert.assertAll();
    }

    @Step("Проверить совпадение количества смен и сумму их часов")
    private void assertTimeEquality(EmployeePosition ep, List<Shift> shifts, boolean period) {
        int shiftCountUI = Integer.parseInt(sb.formLayout().numberOfHoursInEmployeeToolTip(InEmployeeWorkingHours.SHIFTS_AMOUNT.getLineName())
                                                    .getText().replaceAll("\\D+", ""));
        int hoursCountUI = Integer.parseInt(sb.formLayout().numberOfHoursInEmployeeToolTip(InEmployeeWorkingHours.HOURS_AMOUNT_MONTH.getLineName())
                                                    .getText().replaceAll("\\D+", ""));
        Double hoursCountOnPopUp = Math.floor(Double.parseDouble(sb.formLayout().employeeHoursWorked(ep.getId()).getText()));

        Duration totalDuration = shifts.stream()
                .map(shift -> {
                    LocalDateTime shiftStart = shift.getDateTimeInterval().getStartDateTime();
                    LocalDateTime shiftEnd = shift.getDateTimeInterval().getEndDateTime();
                    boolean isAfterDate = shiftEnd.toLocalDate().isAfter(shiftStart.toLocalDate());
                    boolean lastDay = period ? shiftStart.toLocalDate().isEqual(LocalDateTools.getLastDate())
                            : shiftEnd.toLocalDate().isEqual(LocalDateTools.getSunday());

                    if (lastDay && isAfterDate) {
                        shiftEnd = shiftStart.withHour(23).withMinute(59).withSecond(59);
                    }

                    return Duration.between(shiftStart, shiftEnd).minusMinutes(shift.getLunch());
                }).reduce(Duration.ZERO, Duration::plus);

        long totalHours = totalDuration.toHours();
        Allure.addAttachment("Сравнение количества часов",
                             String.format("Количество часов в попапе: %d\nКоличество часов на UI: %d\nКоличество часов в API: %d",
                                           hoursCountOnPopUp.intValue(), hoursCountUI, totalHours));

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertTrue(shifts.size() == shiftCountUI,
                              String.format("Количество смен в попапе %d не совпадает с фактическим количеством смен %d", shiftCountUI, shifts.size()));
        softAssert.assertTrue(hoursCountUI == totalHours,
                              String.format("Количество часов (месяц) на в попапе %d не совпадает с суммой часов на API %d", hoursCountUI, totalHours));
        softAssert.assertTrue(hoursCountOnPopUp.intValue() == totalHours,
                              String.format("Количество часов отображаемое в поле сотрудника на UI %d не совпадает с суммой часов на API %d", hoursCountOnPopUp.intValue(), totalHours));
        softAssert.assertAll();
    }

    @Step("Проверить, что поле {fieldName} содержит ошибку {errorMessage}")
    private void assertErrorInField(String fieldName, String errorMessage) {
        sb.formEditForm().waitUntil("Форма редактирования больше не отображается",
                                    DisplayedMatcher.displayed(), 3);
        sb.formEditForm().errorMessage(fieldName).should("Красная подсветка строки",
                                                         TextMatcher.text(Matchers.containsString(errorMessage)), 5);
        Allure.addAttachment("Проверка", "Форма редактирования не закрывается, " +
                "ожидается сообщение об ошибке: " + errorMessage);
    }

    @Step("Проверить, что нет свободных смен за {date}")
    private void assertNotActiveFreeShifts(int omId, LocalDate date) {
        systemSleep(5);//похоже, что ростер не успевает прогрузиться и бывает, что свободная смена не успевает удалиться
        List<Shift> freeShifts = ShiftRepository.getFreeShifts(omId, date);
        assertEquals(freeShifts.size(), 0, String.format("Найдено %d свободных смен за %s",
                                                         freeShifts.size(), date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))));
    }

    private boolean clickBackAndTurnOnPlan(int days, boolean clickback) {
        if (LocalDate.now().minusDays(days).isBefore(LocalDateTools.getFirstDate()) && !clickback) {
            clickBack();
            listOfSchedulesClick();
            isTimeSheetButtonClick();
            clickback = true;
        }
        return clickback;
    }

    private void tryToDeleteShiftWithoutPermission() {
        try {
            shiftThreeDotsClick();
            sb.formListOfRequest().typeButtons(RequestAction.DELETE.getAction())
                    .should("Смена открылась в режиме редактирования", Matchers.not(DisplayedMatcher.displayed()));
        } catch (org.openqa.selenium.ElementNotInteractableException e) {
            // Если кнопки  "..." нет, то смена совсем никак не редактируется, все ок.
        }
    }

    @Test(groups = {"TK2688-3", G1, SCHED2},
            description = "Удаление/добавление подписей \"Табель\" и \"плановый график\" в расписании")
    @Link(name = "Ссылка на тест-кейс", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=197460864")
    @TmsLink("60334")
    @Tag("TK2688-3")
    @Tag(SCHED2)
    public void removeAndAddLabels() {
        List<PermissionType> permissionCustomGeneratedTypes = new ArrayList<>(Arrays.asList(
                PermissionType.SCHEDULE_VIEW,
                PermissionType.SCHEDULE_EDIT,
                PermissionType.SCHEDULE_CUSTOM_INDICATORS
        ));
        Role role = PresetClass.createCustomPermissionRole(permissionCustomGeneratedTypes);
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitWithWorkedRosters(true);
        goToScheduleAsUser(role, orgUnit);
        enableScheduleBoardHelpIndicator();
        refreshPage();
        if (!isSelected(ButtonIDropDownMenu.ADDITIONAL_INFORMATION)) {
            pressMenuI();
            selectItemFromDropDownMenuI(ButtonIDropDownMenu.ADDITIONAL_INFORMATION);
        }
        isAdditionalInformationDisplayed(true);
        pressMenuI();
        selectItemFromDropDownMenuI(ButtonIDropDownMenu.ADDITIONAL_INFORMATION);
        isAdditionalInformationDisplayed(false);
        pressMenuI();
        selectItemFromDropDownMenuI(ButtonIDropDownMenu.ADDITIONAL_INFORMATION);
    }

    @Test(groups = {"TEST-81"}, description = "Изменить название ОМ")
    public void changeName() {
        OrgUnit orgUnit = OrgUnitRepository.getAllOrgUnits(true).stream().filter(orgUnit1 -> orgUnit1.getId() == 500).collect(randomItem());
        goToSchedule(orgUnit);
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.SUBDIVISION_PROPERTIES);
        schedulePencilClick();
        String randomName = RandomStringUtils.randomAlphabetic(10);
        makeClearOrgUnitNameField();
        spaceOrgUnitNameSend(randomName);
        spaceOrgChangeClick();
        assertOrgNameChange(orgUnit.getName(), randomName);
    }

    @Test(groups = {"TEST-82.1"},
            description = "Добавление тега к подразделению с уже имеющимся тегом")
    public void addTagWithTag() {
        OrgUnit unit = OrgUnitRepository.getRandomAvailableOrgUnit();
        goToSchedule(unit);
        List<String> tags = PresetClass.tagPreset(unit, TagValue.ONE);
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.SUBDIVISION_PROPERTIES);
        schedulePencilClick();
        tagSpaceClick();
        String randomAlphabetic = RandomStringUtils.randomAlphabetic(10);
        tagSpaceAdd(randomAlphabetic);
        tagAddOneClick();
        spaceOrgChangeClick();
        assertTagsChange(unit, tags, randomAlphabetic, TagValue.ONE);
    }

    @Test(groups = {"TEST-82.2"},
            description = "Добавление тега к подразделению с уже имеющимися тегами")
    public void addTagWithTwoTag() {
        OrgUnit unit = OrgUnitRepository.getRandomAvailableOrgUnit();
        goToSchedule(unit);
        List<String> tags = PresetClass.tagPreset(unit, TagValue.SEVERAl);
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.SUBDIVISION_PROPERTIES);
        schedulePencilClick();
        tagSpaceClick();
        String randomAlphabetic = RandomStringUtils.randomAlphabetic(10);
        tagSpaceAdd(randomAlphabetic);
        tagAddOneClick();
        spaceOrgChangeClick();
        assertTagsChange(unit, tags, randomAlphabetic, TagValue.SEVERAl);
    }

    @Test(groups = {"TEST-82.3"}, description = "Добавление тега к подразделению без тегов")
    public void addTagWithOutTag() {
        OrgUnit unit = OrgUnitRepository.getRandomAvailableOrgUnit();
        goToSchedule(unit);
        List<String> tags = PresetClass.tagPreset(unit, TagValue.NO_ONE);
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.SUBDIVISION_PROPERTIES);
        schedulePencilClick();
        tagSpaceClick();
        String randomAlphabetic = RandomStringUtils.randomAlphabetic(10);
        tagSpaceAdd(randomAlphabetic);
        tagAddOneClick();
        spaceOrgChangeClick();
        assertTagsChange(unit, tags, randomAlphabetic, TagValue.NO_ONE);
    }

    @Test(groups = {"TEST-96"}, description = "Внести параметр по ОМ (сохранить)")
    public void enterParameterOnOmAndSave() {
        OrgUnit unit = OrgUnitRepository.getRandomAvailableOrgUnit();
        goToSchedule(unit);
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.SUBDIVISION_PROPERTIES);
        clickOnProblemPencilButton(EmployeeInfoName.OPTIONS);
        String paramName = getParamName(false, unit);
        int rndNumber = new Random().nextInt(1000);
        sendInTargetParamInput(paramName, rndNumber);
        saveParameterChanges();
        assertParamChanges(paramName, rndNumber);
    }

    @Test(groups = {"TEST-97"}, description = "Скорректировать параметр по ОМ")
    public void correctParameterOnOm() {
        OrgUnit unit = OrgUnitRepository.getRandomAvailableOrgUnit();
        goToSchedule(unit);
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.SUBDIVISION_PROPERTIES);
        clickOnProblemPencilButton(EmployeeInfoName.OPTIONS);
        String paramName = getParamName(true, unit);
        int rndNumber = new Random().nextInt(1000);
        sendInTargetParamInput(paramName, rndNumber);
        saveParameterChanges();
        assertParamChanges(paramName, rndNumber);
    }

    @Test(groups = {"TEST-100", "not actual"}, description = "Добавить e-mail")
    public void addEmployeeEmail() {
        OrgUnit unit = OrgUnitRepository.getRandomAvailableOrgUnit();
        goToSchedule(unit);
        Employee employee = EmployeeRepository.getRandomEmployeeNameEmailStatus(unit, getEmployeesNamesOnUi(), false);
        clickOnEmployeeNameButton(EmployeePositionRepository.getEmployeePosition(unit, employee.getFullName()));
        clickOnShowButton(EmployeeInfoName.CONTACTS);
        clickOnPencilButton(EmployeeInfoName.CONTACTS);
        String email = generateRandomEmail();
        typeEmailInContacts(email);
        clickOnChangeButton(EmployeeInfoName.CONTACTS);
        assertEmail(employee, email);
    }

    @Test(groups = {"TEST-101", "not actual"}, description = "Скорректировать e-mail")
    public void correctEmployeeEmail() {
        OrgUnit unit = OrgUnitRepository.getRandomAvailableOrgUnit();
        goToSchedule(unit);
        Employee employee = EmployeeRepository.getRandomEmployeeNameEmailStatus(unit, getEmployeesNamesOnUi(), true);
        clickOnEmployeeNameButton(EmployeePositionRepository.getEmployeePosition(unit, employee.getFullName()));
        clickOnShowButton(EmployeeInfoName.CONTACTS);
        clickOnPencilButton(EmployeeInfoName.CONTACTS);
        cleanEmailInput();
        String email = generateRandomEmail();
        typeEmailInContacts(email);
        clickOnChangeButton(EmployeeInfoName.CONTACTS);
        assertEmail(employee, email);
    }

    @Test(groups = {"TEST-102", "not actual"}, description = "Добавить стажерскую программу")
    public void addInternProgram() {
        OrgUnit unit = OrgUnitRepository.getRandomAvailableOrgUnit();
        goToSchedule(unit);
        Employee employee = EmployeeRepository.randomEmployeeWithoutMentorSkill(getEmployeesNamesOnUi(), unit);
        clickOnEmployeeNameButton(EmployeePositionRepository.getEmployeePosition(unit, employee.getFullName()));
        clickOnShowButton(EmployeeInfoName.INTERNSHIP_PROGRAM);
        clickOnPencilButton(EmployeeInfoName.INTERNSHIP_PROGRAM);
        internCheckBoxClick();
        mentorSelect();
        selectTheDate(LocalDateTools.getDate(2021, LocalDateTools.RANDOM, LocalDateTools.RANDOM));
        clickOnChangeButton(EmployeeInfoName.INTERNSHIP_PROGRAM);
        addInternProgramCheck(employee);
    }

    @Test(groups = {"TEST-84"}, description = "Создать пустую должность ")
    public void createEmptyPost() {
        OrgUnit unit = OrgUnitRepository.getRandomAvailableOrgUnit();
        goToSchedule(unit);
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.SUBDIVISION_PROPERTIES);
        clickOnChevronButton(OmInfoName.EMPLOYEES);
        clickOnPlusButtonEmployee();
        JobTitle jobTitle = randomJobTitle();
        chooseJob(jobTitle);
        PositionGroup role = PositionGroupRepository.randomPositionGroup();
        clickOnFunctionalRolesSelectButton();
        selectFuncRole(role);
        LocalDate startDate = LocalDateTools.now();
        chooseDatePositionForm(startDate, DateTypeField.POSITION_START_DATE);
        List<Position> arrayBefore = PositionRepository.emptyPositionReturner(jobTitle, startDate, getOrgIdFromUrl());
        saveButtonClick();
        assertionCompareMaps(arrayBefore, jobTitle, getOrgIdFromUrl(), startDate);
    }

    @Test(groups = {"TEST-85"}, description = "Удалить пустую должность")
    public void deleteEmptyPosition() {
        OrgUnit unit = OrgUnitRepository.getRandomAvailableOrgUnit();
        goToSchedule(unit);
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.SUBDIVISION_PROPERTIES);
        clickOnChevronButton(OmInfoName.EMPLOYEES);
        threeDotsEmptyPositions();
        List<Position> beforeDelete = PositionRepository.checkApiPositionsDate(getOrgIdFromUrl());
        deletePositions();
        assertForDeleteEmptyPosition(beforeDelete);
    }

    @Test(groups = {"TEST-87"}, description = "Назначить сотрудника на свободную должность")
    public void assignEmployeeFreePosition() {
        OrgUnit unit = OrgUnitRepository.getRandomAvailableOrgUnit();
        goToSchedule(unit);
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.SUBDIVISION_PROPERTIES);
        clickOnChevronButton(OmInfoName.EMPLOYEES);
        List<String> temp = getEmployeesNamesOnUi();
        Map<String, String> tempPosition = emptyPositionsMap();
        String index = tempPosition.get("index");
        String positionName = tempPosition.get(NAME);
        clickOnEmptyPositionThreeDotsButton(index, positionName);
        editButtonClick();
        clickOnSelectEmployeeChevron();
        Employee employee = EmployeeRepository.getRandomPersonNotWorking(temp);
        String name = employee.getFullName();
        selectAnyEmployee(name);
        chooseDatePositionForm(checkDataStartPosition(), DateTypeField.START_JOB);
        clickOnFunctionalRolesSelectButton();
        saveButtonClick();
        addJobCheck(employee, positionName);
    }

    @Test(groups = {"TEST-91"}, description = "Добавить график работы Service")
    public void addTimeTable() {
        OrgUnit unit = OrgUnitRepository.getRandomAvailableOrgUnit();
        goToSchedule(unit);
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.SUBDIVISION_PROPERTIES);
        clickOnChevronButton(OmInfoName.SCHEDULE);
        clickOnThreeDotsButton();
        clickOnSelectScheduleButton();
        ActWithSchedule actWithSchedule = new ActWithSchedule(sb.selectScheduleForm());
        actWithSchedule.clickOnPlusButton();
        LocalDate dateOpen = findStartDate();
        LocalDate dateClose = dateOpen.plusYears(2);
        actWithSchedule.dateOpenSelect(dateOpen);
        actWithSchedule.dateCloseSelect(dateClose);
        actWithSchedule.selectScheduleType(ScheduleType.SERVICE);
        int id = getOrgIdFromUrl();
        List<BusinessHours> checkListBefore = BusinessHoursRepository.checkForAvailability(id);
        actWithSchedule.clickOnSaveButton();
        List<BusinessHours> checkListAfter = BusinessHoursRepository.checkForAvailability(id);
        scheduleCheckAdding(checkListBefore, checkListAfter);
    }

    @Test(groups = {"TEST-90"}, description = "Назначить должности функциональную роль")
    public void assignItemGroup() {
        OrgUnit unit = OrgUnitRepository.getRandomAvailableOrgUnit();
        goToSchedule(unit);
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.SUBDIVISION_PROPERTIES);
        clickOnChevronButton(OmInfoName.EMPLOYEES);
        Employee employee = EmployeeRepository.chooseRandomEmployee(unit);
        String employeeName = employee.getFullName();
        clickOnEmployeeThreeDots(employeeName);
        editButtonClick();
        clickOnFunctionalRolesSelectButton();
        PositionGroup positionGroup = PositionGroupRepository.randomPositionGroup();
        selectFuncRole(positionGroup);
        saveButtonClick();
        addFuncRoleCheck(employee, positionGroup);
    }

    @Test(groups = {"TEST-92.1"}, description = "Переключение графика работы на SALE")
    public void changeSchedulesTypeToSale() {
        ActWithSchedule actWithSchedule = new ActWithSchedule(sb.selectScheduleForm());
        OrgUnit unit = OrgUnitRepository.getRandomAvailableOrgUnit();
        goToSchedule(unit);
        BusinessHours scheduleNameService = BusinessHoursRepository.getAnyScheduleWithTypeWithPreset(ScheduleType.SERVICE, unit.getId());
        BusinessHours scheduleNameSale = BusinessHoursRepository.getAnyScheduleWithTypeWithPreset(ScheduleType.SALE, unit.getId());
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.SUBDIVISION_PROPERTIES);
        clickOnChevronButton(OmInfoName.SCHEDULE);
        specialConditionsToChangeSchedule(scheduleNameService);
        clickOnThreeDotsButton();
        clickOnSelectScheduleButton();
        actWithSchedule.selectActiveSchedule(scheduleNameSale);
        actWithSchedule.clickOnSaveButton();
        typeChangeCheck(scheduleNameSale);
    }

    @Test(groups = {"TEST-92.2"}, description = "Переключение графика работы на SERVICE")
    public void changeSchedulesTypeToService() {
        ActWithSchedule actWithSchedule = new ActWithSchedule(sb.selectScheduleForm());
        OrgUnit unit = OrgUnitRepository.getRandomAvailableOrgUnit();
        goToSchedule(unit);
        BusinessHours scheduleNameService = BusinessHoursRepository.getAnyScheduleWithTypeWithPreset(ScheduleType.SERVICE, unit.getId());
        BusinessHours scheduleNameSale = BusinessHoursRepository.getAnyScheduleWithTypeWithPreset(ScheduleType.SALE, unit.getId());
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.SUBDIVISION_PROPERTIES);
        clickOnChevronButton(OmInfoName.SCHEDULE);
        specialConditionsToChangeSchedule(scheduleNameSale);
        clickOnThreeDotsButton();
        clickOnSelectScheduleButton();
        actWithSchedule.selectActiveSchedule(scheduleNameService);
        actWithSchedule.clickOnSaveButton();
        typeChangeCheck(scheduleNameService);
    }

    @Test(groups = {"TEST-94.1"}, description = "Скорректировать тип дня на выходной")
    public void adjustDayToDayOff() {
        OrgUnit unit = OrgUnitRepository.getRandomAvailableOrgUnit();
        goToSchedule(unit);
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.SUBDIVISION_PROPERTIES);
        clickOnChevronButton(OmInfoName.SCHEDULE);
        clickOnThreeDotsButton();
        int orderNumber = determineActiveScheduleNumber();
        openScheduleSelectionMenu();
        String scheduleId = determineActiveScheduleId(orderNumber);
        int dayId = PresetClass.getAnyDayWithType(scheduleId, Days.DAY);
        exitScheduleSelectionMenu();
        refreshScheduleUI();
        clickOnThreeDotsButton();
        clickOnEditScheduleButton();
        clickOnDayTypeChangeButton(dayId);
        switchDayTypeTo(Days.DAY_OFF);
        clickOnEditionScheduleChangeButton();
        switchDayCheck(dayId, scheduleId, Days.DAY_OFF);
    }

    @Test(groups = {"TEST-89"}, description = "Сделать должность руководителем подразделения")
    public void makeThePositionHeadOfTheUnit() {
        OrgUnit unit = OrgUnitRepository.getRandomAvailableOrgUnit();
        goToSchedule(unit);
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.SUBDIVISION_PROPERTIES);
        clickOnChevronButton(OmInfoName.EMPLOYEES);
        Employee employee = EmployeeRepository.chooseRandomEmployee(unit);
        clickOnEmployeeThreeDots(employee.getFullName());
        editButtonClick();
        leaderCheckBoxClick();
        saveButtonClick();
        assertForMakingLeader(employee);
    }

    @Test(groups = {"TEST-94.2"}, description = "Скорректировать тип дня на рабочий")
    public void adjustDayOffTypeToDay() {
        OrgUnit unit = OrgUnitRepository.getRandomAvailableOrgUnit();
        goToSchedule(unit);
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.SUBDIVISION_PROPERTIES);
        clickOnChevronButton(OmInfoName.SCHEDULE);
        clickOnThreeDotsButton();
        int orderNumber = determineActiveScheduleNumber();
        openScheduleSelectionMenu();
        String scheduleId = determineActiveScheduleId(orderNumber);
        int dayId = PresetClass.getAnyDayWithType(scheduleId, Days.DAY_OFF);
        exitScheduleSelectionMenu();
        refreshScheduleUI();
        clickOnThreeDotsButton();
        clickOnEditScheduleButton();
        clickOnDayTypeChangeButton(dayId);
        switchDayTypeTo(Days.DAY);
        clickOnEditionScheduleChangeButton();
        switchDayCheck(dayId, scheduleId, Days.DAY);
    }

    @Test(groups = {"TEST-95.1", BROKEN, "not actual"}, description = "Добавление исключения без выбора поведения KPI")
    public void addSpecialDaysToScheduleWithOutBehavior() {
        OrgUnit unit = OrgUnitRepository.getRandomAvailableOrgUnit();
        goToSchedule(unit);
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.SUBDIVISION_PROPERTIES);
        clickOnChevronButton(OmInfoName.SCHEDULE);
        clickOnThreeDotsButton();
        int orderNumber = determineActiveScheduleNumber();
        openScheduleSelectionMenu();
        String scheduleId = determineActiveScheduleId(orderNumber);
        exitScheduleSelectionMenu();
        refreshScheduleUI();
        clickOnThreeDotsButton();
        clickOnEditScheduleButton();
        int index = sb.subdivisionProperties().specialDaysList().waitUntil(Matchers.hasSize(Matchers.greaterThan(0))).size() + 1;
        LocalDate date = LocalDateTools.getDate(2023, LocalDateTools.RANDOM, LocalDateTools.RANDOM);
        selectDateOfSpecialDay(index, date);
        selectSpecialDayStartTime(index, LocalTime.of(12, 15));
        selectSpecialDayCloseTime(index, LocalTime.of(20, 20));
        clickOnSelectSpecialDayTypeField(index);
        switchDayTypeTo(Days.DAY);
        clickOnEditionScheduleChangeButton();
        checkExceptionAdding(Days.WITHOUT, date, scheduleId);
    }

    @Test(groups = {"TEST-95.2", BROKEN, "not actual"}, description = "Добавление выходного в расписание")
    public void addDayOffToSchedule() {
        OrgUnit unit = OrgUnitRepository.getRandomAvailableOrgUnit();
        goToSchedule(unit);
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.SUBDIVISION_PROPERTIES);
        clickOnChevronButton(OmInfoName.SCHEDULE);
        clickOnThreeDotsButton();
        int orderNumber = determineActiveScheduleNumber();
        openScheduleSelectionMenu();
        String scheduleId = determineActiveScheduleId(orderNumber);
        exitScheduleSelectionMenu();
        refreshScheduleUI();
        clickOnThreeDotsButton();
        clickOnEditScheduleButton();
        int index = sb.subdivisionProperties().specialDaysList().waitUntil(Matchers.hasSize(Matchers.greaterThan(0))).size() + 1;
        LocalDate date = LocalDateTools.getDate(2022, LocalDateTools.RANDOM, LocalDateTools.RANDOM);
        selectDateOfSpecialDay(index, date);
        selectSpecialDayStartTime(index, LocalTime.of(12, 15));
        selectSpecialDayCloseTime(index, LocalTime.of(20, 20));
        clickOnSelectSpecialDayTypeField(index);
        switchDayTypeTo(Days.DAY_OFF);
        clickOnEditionScheduleChangeButton();
        dayOffAddingCheck(scheduleId, date.format(API.getFormat()));
    }

    @Test(groups = {"TEST-86"},
            description = "Поставить дату окончания работы сотрудника на должности")
    public void addEndWorkDate() {
        OrgUnit unit = OrgUnitRepository.getRandomAvailableOrgUnit();
        goToSchedule(unit);
        PresetClass.deleteEmployeeDismissalDate(unit);
        Employee employee = getEmployeeWithoutEndWorkDate();
        clickOnEmployeeNameButton(EmployeePositionRepository.getEmployeePosition(unit, employee.getFullName()));
        clickOnMainPencilButton();
        LocalDate endWorkDate = LocalDateTools.randomSeedDate(12, -5, ChronoUnit.MONTHS, TimeType.RANDOM);
        enterEndWorkDate(endWorkDate);
        clickOnEmployeeDataSaveButton();
        checkEndWorkDateAdding(endWorkDate, employee);
    }

    @Ignore("Заменен на api-тест")
    @Test(groups = {"TEST-166", G0, SCHED12,
            "@Before disable pre-publication checks"},
            description = "Публикация графика")
    @Severity(SeverityLevel.CRITICAL)
    @Link(name = "Статья: \"Расписание\"", url = "https://wiki.goodt.me/x/RAOrCg")
    @TmsLink("61616")
    @Tag("TEST-166")
    @Tag(SCHED12)
    public void schedulePublication() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        disablePublishSystemPropertiesIfNoLimitIsSet(unit);
        nonPublishCheck(omId);
        PresetClass.kpiAndFteChecker(omId);
        goToSchedule(unit);
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.PUBLICATION);
        publishButtonClick();
        closePublicationForm();
        refreshPage();
        listOfSchedulesClick();
        ZonedDateTime dateTime = ZonedDateTime.now();
        publicationAssert(dateTime, unit);
    }

    @Test(groups = {"TK2872-1", "TEST-1176"}, description = "Просмотр опубликованного графика")
    public void viewPublishedChart() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        GraphStatus graphStatus = GraphStatus.PUBLISH;
        PresetClass.publishGraphPreset(graphStatus, unit);
        goToSchedule(unit);
        assertViewPublishStatus(graphStatus);
    }

    @Test(groups = {"TK2872-2", "TEST-1176"}, description = "Просмотр неопубликованного графика")
    public void viewNonPublishedChart() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        GraphStatus graphStatus = GraphStatus.NOT_PUBLISH;
        PresetClass.publishGraphPreset(graphStatus, unit);
        goToSchedule(unit);
        assertViewPublishStatus(graphStatus);
    }

    @Test(groups = {"TK2872-3", "TEST-1176"}, description = "Просмотр графика на утверждении")
    public void viewOnApprovalChart() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        GraphStatus graphStatus = GraphStatus.ON_APPROVAL;
        PresetClass.publishGraphPreset(graphStatus, unit);
        goToSchedule(unit);
        assertViewPublishStatus(graphStatus);
    }

    @Test(groups = {"TEST-168", "TP-3"},
            description = "Создание новой версии графика при редактировании смен")
    public void createNewVersionWhenEditingShift() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        PresetClass.kpiAndFteChecker(omId);
        PresetClass.checkAndMakePublicationRoster(omId);
        int numberOfRostersBefore = RosterRepository.getRosters(omId).size();
        EmployeePosition emp = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, false);
        changeRandomShift(emp);
        goToSchedule(unit);
        listOfSchedulesClick();
        assertCreateNewVersion(numberOfRostersBefore, omId);
    }

    @Test(groups = {"TEST-163.1"}, description = "Печать графика и табеля")
    @Severity(SeverityLevel.NORMAL)
    public void printGraphicsAndTable() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        goToSchedule(unit);
        List<EmployeePosition> employeePositions = EmployeePositionRepository.getEmployeePositions(PositionRepository.getPositionsArray(omId));
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.PRINT);
        FileDownloadCheckerForScheduleBoard checker = new FileDownloadCheckerForScheduleBoard(Role.ADMIN,
                                                                                              TypeOfFiles.PDF, null, omId, employeePositions.stream().map(EmployeePosition::getId)
                                                                                                      .sorted().map(String::valueOf).collect(Collectors.joining(",")));
        assertForRightDownloadingPDF(checker);
    }

    @Test(groups = {"TEST-163.2", "not actual"}, description = "Печать только графика")
    public void printGraphics() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        goToSchedule(unit);
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.PRINT);
        String positionIds = getJobTypeAndConvertToId();
        int omNumber = getOrgIdFromUrl();
        pushRadioButtonOnlySchedule();
        pushPrintButton();
        FileDownloadCheckerForScheduleBoard checker = new FileDownloadCheckerForScheduleBoard(Role.ADMIN,
                                                                                              TypeOfFiles.PDF_ONLY_SCHEDULE, null, omNumber, positionIds);
        assertForRightDownloadingPDF(checker);
    }

    @Ignore("Заменен на api-тест")
    @Test(groups = {"TEST-165", G0, SCHED21,
            "@Before disable pre-publication checks"},
            description = "Отправка графика на утверждение")
    @Severity(SeverityLevel.CRITICAL)
    @TmsLink("61617")
    @Tag("TEST-165")
    @Tag(SCHED21)
    @Link(name = "Статья: \"Расписание\"", url = "https://wiki.goodt.me/x/RAOrCg")
    public void sendingScheduleForApproval() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        disablePublishSystemPropertiesIfNoLimitIsSet(unit);
        nonPublishCheck(omId);
        goToSchedule(unit);
        threeDotsMenuClick();
        ZonedDateTime localDateTimeServer = ZonedDateTime.now();
        chooseFunction(VariantsOfFunctions.FOR_APPROVAL);
        assertPopUpForApproval(localDateTimeServer, unit);
    }

    @Test(groups = {"TEST-171"}, description = "Выгрузка графика в excel")
    @Severity(SeverityLevel.NORMAL)
    public void uploadGraphicsToExcel() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        PresetClass.checkAndMakePublicationRoster(omId);
        goToSchedule(unit);
        threeDotsMenuClick();
        String rosterId = String.valueOf(RosterRepository.getActiveRosterThisMonth(omId).getId());
        chooseFunction(VariantsOfFunctions.DOWNLOAD_XLSX);
        FileDownloadCheckerForScheduleBoard checker = new FileDownloadCheckerForScheduleBoard(Role.ADMIN,
                                                                                              TypeOfFiles.XLSX, rosterId);
        assetForRightDownloading(TypeOfAcceptContent.PDF_XLSX, checker, Role.ADMIN);
    }

    @Test(groups = {"TEST-116"}, description = "Переход в карточку сотрудника")
    @Tag("TEST-116")
    public void goToEmployeeCard() {
        OrgUnit unit = OrgUnitRepository.getRandomOmWithEmployeeOptions(OrgUnitOptions.EMPLOYEES_WITH_POSITION);
        goToSchedule(unit);
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(unit.getId(), null, false);
        clickOnEmployeeNameButton(ep);
        goToEmployeeCardCheck(ep.getEmployee().getShortName());
    }

    @Test(groups = {"TEST-115"}, description = "Переход в карточку ОМ")
    @Tag("TEST-115")
    public void gotToOrgUnitCard() {
        OrgUnit unit = OrgUnitRepository.getRandomAvailableOrgUnit();
        goToSchedule(unit);
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.SUBDIVISION_PROPERTIES);
        goToOrgUnitCardCheck();
    }

    @Test(groups = {"TEST-172", G1, SCHED17},
            description = "Создание события заданного типа",
            dataProvider = "eventTypes")
    @Link(name = "Статья: \"Расписание\"", url = "https://wiki.goodt.me/x/RAOrCg")
    @Tag("TEST-172")
    @TmsLink("61610")
    @Tag(SCHED17)
    public void addEventOfCertainType(EventType eventType) {
        changeTestName(String.format("Создание события типа \"%s\"", eventType.getName()));
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        goToSchedule(orgUnit);
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.CREATE_EVENT);
        selectEventType(eventType);
        int randomValue = new Random().nextInt(100) + 1;
        enterValueOfPeople(randomValue);
        LocalDate date = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.THAT, LocalDateTools.RANDOM).plusMonths(new Random().nextInt(5) + 1);
        enterEventDateEndOrStart(date, DateTypeField.START_DATE);
        enterEventDateEndOrStart(date, DateTypeField.END_DATE);
        LocalTime startTime = LocalTime.of(15, 0);
        LocalTime endTime = LocalTime.of(16, 30);
        enterEventTimeEndOrStart(startTime.toString(), DateTypeField.START_DATE);
        enterEventTimeEndOrStart(endTime.toString(), DateTypeField.END_DATE);
        List<OrganizationUnitEvent> beforeEventAdd = OrganizationUnitEventRepository.getOrganizationUnitEvents(date, date, orgUnit.getId());
        clickCreateEventButton();
        List<OrganizationUnitEvent> afterEventAdd = OrganizationUnitEventRepository.getOrganizationUnitEvents(date, date, orgUnit.getId());
        checkEventAdding(beforeEventAdd, afterEventAdd, date, randomValue, eventType, startTime, endTime);
    }

    @Test(groups = {"TEST-167"},
            description = "Создание новой версии графика при расчете/перерасчете")
    @Severity(SeverityLevel.CRITICAL)
    public void createNewScheduleVersion() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        goToSchedule(unit);
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.CALCULATION_RECALCULATE_SHIFTS);
        LocalDate dateStart = LocalDateTools.getFirstDate();
        LocalDate dateEnd = LocalDateTools.getLastDate();
        enterCreateScheduleDateEndOrStart(dateStart, DateTypeField.START_DATE);
        enterCreateScheduleDateEndOrStart(dateEnd, DateTypeField.END_DATE);
        RosterRepository.getRosters(omId).stream().findAny().orElseThrow(() -> new AssertionError("У подразделения нет ростеров"));
        List<Integer> previousIds = RosterRepository.getRosters(omId).stream().map(Roster::getId).collect(Collectors.toList());
        calculateButtonClick();
        clickOnCloseButton();
        listOfSchedulesClick();
        assertScheduleCalculation(previousIds, omId);
    }

    @Test(groups = {"ABCHR2878", "TEST-1086"},
            description = "Создание графика при расчете/перерасчете с минимальным отклонением")
    public void createNewScheduleVersionWithDeviation() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = orgUnit.getId();
        goToSchedule(orgUnit);
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.CALCULATION_RECALCULATE_SHIFTS);
        LocalDate dateStart = LocalDateTools.getFirstDate();
        enterCreateScheduleDateEndOrStart(dateStart, DateTypeField.START_DATE);
        RosterRepository.getRosters(omId).stream().findAny().orElseThrow(() -> new AssertionError("У подразделения нет ростеров"));
        List<Integer> previousIds = RosterRepository.getRosters(omId).stream().map(Roster::getId).collect(Collectors.toList());
        activateElementCheckbox();
        calculateButtonClick();
        assertScheduleCalculation(previousIds, omId);
    }

    @Test(groups = {"TEST-83.1"}, description = "Удаление одного тега с имеющимся одним тегом")
    public void deleteOneTagWithOneTag() {
        OrgUnit unit = OrgUnitRepository.getRandomAvailableOrgUnit();
        goToSchedule(unit);
        List<String> tagsBefore = PresetClass.tagPreset(unit, TagValue.ONE);
        String tagToDelete = getTagToDelete(tagsBefore, TagValue.ONE);
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.SUBDIVISION_PROPERTIES);
        schedulePencilClick();
        clickOnTagDeleteButton(tagToDelete);
        spaceOrgChangeClick();
        checkTagRemoval(tagsBefore, tagToDelete, TagValue.ONE);
    }

    @Test(groups = {"TEST-83.2"},
            description = "Удаление одного тега с имеющимися несколькими тегами")
    public void deleteOneTagWithSeveralTag() {
        OrgUnit unit = OrgUnitRepository.getRandomAvailableOrgUnit();
        goToSchedule(unit);
        List<String> tagsBefore = PresetClass.tagPreset(unit, TagValue.SEVERAl);
        String tagToDelete = getTagToDelete(tagsBefore, TagValue.SEVERAl);
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.SUBDIVISION_PROPERTIES);
        schedulePencilClick();
        clickOnTagDeleteButton(tagToDelete);
        spaceOrgChangeClick();
        checkTagRemoval(tagsBefore, tagToDelete, TagValue.SEVERAl);
    }

    @Test(groups = {"TEST-157"}, description = "Редактирование комментария к дням")
    public void editDayComment() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        goToSchedule(orgUnit);
        Map<String, String> temp = PresetClass.getDayCommentPreset(new DateInterval(), orgUnit);
        LocalDate date = LocalDate.parse(temp.get(DATE));
        String commentTextOld = temp.get("text");
        String newComment = RandomStringUtils.randomAlphabetic(10);
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.COMMENTS_ON_DAYS);
        int dayNumber = date.getDayOfMonth();
        clickCommentFieldDay(dayNumber);
        cleanCommentFieldDay(dayNumber);
        editCommentDay(newComment, dayNumber);
        commentsChangeButtonClick();
        commentEditionCheck(orgUnit, commentTextOld, newComment, date);
    }

    @Test(groups = {"TEST-158"}, description = "Удаление комментария к дням")
    public void deleteDayComment() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        goToSchedule(orgUnit);
        Map<String, String> temp = PresetClass.getDayCommentPreset(new DateInterval(), orgUnit);
        LocalDate date = LocalDate.parse(temp.get(DATE));
        String commentTextOld = temp.get("text");
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.COMMENTS_ON_DAYS);
        int dayNumber = date.getDayOfMonth();
        deleteComment(dayNumber, true);
        commentsChangeButtonClick();
        commentDeletionCheck(orgUnit, commentTextOld, date);
    }

    @Test(groups = {"TEST-160"},
            description = "Редактирование комментария к версии графика")
    public void changeRosterComment() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        Map<String, String> roster = PresetClass.rosterCommentCheck(RosterRepository.getRandomRoster(orgUnit).getId(), CommentValue.EXIST);
        goToSchedule(orgUnit);
        String version = roster.get(VERSION);
        String oldComment = roster.get(COMMENT);
        String rosterId = roster.get("id");
        String newComment = RandomStringUtils.randomAlphabetic(10);
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.COMMENTS_TO_THE_VERSIONS_OF_THE_CALCULATION);
        int index = getIndexRosterComment(version);
        clickCommentFieldRoster(index);
        cleanCommentRoster(index);
        editCommentRoster(newComment, index);
        commentsChangeButtonClick();
        rosterCommentEditionCheck(oldComment, newComment, rosterId, version);
    }

    @Test(groups = {"TEST-161"}, description = "Удаление комментария к версии графика")
    public void deleteRosterComment() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        Map<String, String> roster = PresetClass.rosterCommentCheck(RosterRepository.getRandomRoster(orgUnit).getId(), CommentValue.EXIST);
        goToSchedule(orgUnit);
        String version = roster.get(VERSION);
        String rosterId = roster.get("id");
        String oldComment = roster.get(COMMENT);
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.COMMENTS_TO_THE_VERSIONS_OF_THE_CALCULATION);
        deleteComment(Integer.parseInt(version), false);
        commentsChangeButtonClick();
        rosterCommentDeletionCheck(oldComment, rosterId, version);
    }

    @Test(groups = {"TEST-170"}, description = "Сравнение графика и табеля")
    public void scheduleAndTimeSheetCompare() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        goToSchedule(unit);
        PresetClass.rosterPublishCheck(RosterRepository.getActiveRosterThisMonth(omId).getId());
        int employeeValue = employeesSize();
        listOfSchedulesClick();
        timeSheetCompareButtonClick();
        goToTimeSheetAndTimeTableCompareCheck(employeeValue);
    }

    @Test(groups = {"TEST-105.1"}, description = "Добавление всех навыков сотруднику")
    public void addAllSkills() {
        OrgUnit unit = OrgUnitRepository.getRandomAvailableOrgUnit();
        goToSchedule(unit);
        EmployeePosition empPos = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(unit.getId(), null, false);
        int id = empPos.getId();
        int size = PresetClass.addOrDeleteSkills(id, false);
        clickOnEmployeeNameButton(empPos);
        clickOnShowButton(EmployeeInfoName.SKILLS);
        clickOnPencilButton(EmployeeInfoName.SKILLS);
        clickOnSkillsCheckBox(EmployeeSkills.TUTOR);
        clickOnSkillsCheckBox(EmployeeSkills.RESPONSIBLE);
        clickOnSkillsCheckBox(EmployeeSkills.MASTER);
        clickOnChangeButton(EmployeeInfoName.SKILLS);
        checkAllSkillsAdding(size, id);
    }

    @Test(groups = {"TEST-105.2"},
            description = "Добавление одного навыка при отсутствии других навыков")
    public void addSkillWithNoSkill() {
        OrgUnit unit = OrgUnitRepository.getRandomAvailableOrgUnit();
        goToSchedule(unit);
        EmployeePosition empPos = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(unit.getId(), null, false);
        int id = empPos.getId();
        int size = PresetClass.addOrDeleteSkills(id, false);
        clickOnEmployeeNameButton(empPos);
        clickOnShowButton(EmployeeInfoName.SKILLS);
        List<String> tempSkills = getCurrentSkills();
        clickOnPencilButton(EmployeeInfoName.SKILLS);
        clickOnAnySkillCheckBox(tempSkills);
        clickOnChangeButton(EmployeeInfoName.SKILLS);
        checkOneSkillAdding(size, id);
    }

    @Test(groups = {"TEST-105.3"}, description = "Добавление навыка при уже имеющихся навыках")
    public void addSkillWithAnySkills() {
        OrgUnit unit = OrgUnitRepository.getRandomAvailableOrgUnit();
        goToSchedule(unit);
        EmployeePosition employeeIdName = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(unit.getId(), null, false);
        int id = employeeIdName.getId();
        int size = PresetClass.addOrDeleteSkills(id, true);
        clickOnEmployeeNameButton(employeeIdName);
        clickOnShowButton(EmployeeInfoName.SKILLS);
        List<String> tempSkills = getCurrentSkills();
        clickOnPencilButton(EmployeeInfoName.SKILLS);
        clickOnAnySkillCheckBox(tempSkills);
        clickOnChangeButton(EmployeeInfoName.SKILLS);
        checkOneSkillAdding(size, id);
    }

    @Test(groups = {"TEST-162"},
            description = "Отображение комментария к дням в печатной версии")
    private void printScheduleWithComment() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        goToSchedule(unit);
        DateInterval dateInterval = new DateInterval();
        PresetClass.getDayCommentPreset(dateInterval, unit);
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.PRINT);
        List<Integer> positionIds = EmployeePositionRepository.getActualEmployeePositionsWithChief(omId).stream()
                .map(EmployeePosition::getId).collect(Collectors.toList());
        FileDownloadCheckerForScheduleBoard checker = new FileDownloadCheckerForScheduleBoard(Role.ADMIN,
                                                                                              TypeOfFiles.PDF, TypeOfReports.PRINT_SCHEDULE_WITH_COMMENT, omId, Joiner.on(',').join(positionIds));
        checkCommentAvailable(unit, dateInterval);
        assertForRightDownloadingPDF(checker);
    }

    @Test(groups = {"TEST-173", G1, SCHED17},
            description = "Создать повторяющееся событие")
    @Link(name = "Статья: \"Расписание\"", url = "https://wiki.goodt.me/x/RAOrCg")
    @Tag(SCHED17)
    @TmsLink("61609")
    @Tag("TEST-173")
    public void createRepeatingEvent() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        goToSchedule(unit);
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.CREATE_EVENT);
        int randomValue = new Random().nextInt(100) + 1;
        enterValueOfPeople(randomValue);
        LocalDate date = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.THAT, LocalDateTools.RANDOM).plusMonths(new Random().nextInt(5) + 1);
        enterEventDateEndOrStart(date, DateTypeField.START_DATE);
        enterEventDateEndOrStart(date, DateTypeField.END_DATE);
        enterEventTimeEndOrStart("12:00", DateTypeField.START_DATE);
        enterEventTimeEndOrStart("18:00", DateTypeField.END_DATE);
        selectEventRepeatType(Periodicity.WEEKLY);
        LocalDate endRepeat = date.plusDays(new Random().nextInt(100) + 7);
        sendDateEndRepeat(endRepeat);
        List<OrganizationUnitEvent> beforeEventAdd = OrganizationUnitEventRepository.getOrganizationUnitEvents(date, date, unit.getId());
        clickCreateEventButton();
        List<OrganizationUnitEvent> afterEventAdd = OrganizationUnitEventRepository.getOrganizationUnitEvents(date, date, unit.getId());
        checkRepeatEventAdding(beforeEventAdd, afterEventAdd, date, endRepeat, randomValue, Periodicity.WEEKLY);
    }

    @Test(groups = {"TEST-174", G1, SCHED17},
            description = "Отредактировать событие")
    @Link(name = "Статья: \"Расписание\"", url = "https://wiki.goodt.me/x/RAOrCg")
    @Tag(SCHED17)
    @TmsLink("61608")
    @Tag("TEST-174")
    public void editNonRepeatEvent() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        List<OrganizationUnitEvent> nonRepeatEventsBefore = OrganizationUnitEventRepository.eventsChoice(false, omId);
        LocalDate eventDate = getRandomFromList(nonRepeatEventsBefore).getDate();
        goToSchedule(unit);
        enableIndicatorsIfDisabled(Collections.singletonList(ButtonIDropDownMenu.EVENTS));
        clickOnRandomEventPoint(eventDate);
        clickOnChangeEvent(eventDate, false);
        int randomValue = new Random().nextInt(100) + 1;
        clearValueOfPeople();
        enterValueOfPeople(randomValue);
        LocalDate date = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.THAT, LocalDateTools.RANDOM).plusMonths(new Random().nextInt(2) + 1);
        enterEventDateEndOrStart(date, DateTypeField.START_DATE);
        enterEventDateEndOrStart(date, DateTypeField.END_DATE);
        enterEventTimeEndOrStart("12:00", DateTypeField.START_DATE);
        enterEventTimeEndOrStart("18:00", DateTypeField.END_DATE);
        clickOnChangeEventPoint();
        checkNonRepeatEventChanging(nonRepeatEventsBefore, date, randomValue, omId);
    }

    @Test(groups = {"TEST-175", G1, SCHED17, POCHTA},
            description = "Редактирование серии событий")
    @Link(name = "Статья: \"Расписание\"", url = "https://wiki.goodt.me/x/RAOrCg")
    @Tag(SCHED17)
    @TmsLink("61607")
    @Tag("TEST-175")
    public void editRepeatEvent() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        List<OrganizationUnitEvent> repeatEventsBefore = OrganizationUnitEventRepository.eventsChoice(true, omId);
        LocalDate eventDate = getRandomFromList(repeatEventsBefore).getDate();
        goToSchedule(unit);
        enableIndicatorsIfDisabled(Collections.singletonList(ButtonIDropDownMenu.EVENTS));
        clickOnRandomEventPoint(eventDate);
        clickOnChangeEvent(eventDate, true);
        int randomValue = new Random().nextInt(100) + 1;
        clearValueOfPeople();
        enterValueOfPeople(randomValue);
        enterEventTimeEndOrStart("12:00", DateTypeField.START_DATE);
        enterEventTimeEndOrStart("18:00", DateTypeField.END_DATE);
        LocalDate endRepeat = eventDate.plusDays(new Random().nextInt(40) + 8);
        sendDateEndRepeat(endRepeat);
        clickOnChangeEventPoint();
        clickOnRadioButtonInChangeForm();
        clickOnChangeInChangeForm();
        checkRepeatEventChanging(repeatEventsBefore, eventDate, endRepeat, randomValue, omId);
    }

    @Test(groups = {"TEST-106"}, description = "Снять навык")
    public void deleteAllActiveSkills() {
        OrgUnit unit = OrgUnitRepository.getRandomAvailableOrgUnit();
        goToSchedule(unit);
        EmployeePosition employeeIdName = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(unit.getId(), null, false);
        int id = employeeIdName.getId();
        int size = PresetClass.addOrDeleteSkills(id, true);
        clickOnEmployeeNameButton(employeeIdName);
        clickOnShowButton(EmployeeInfoName.SKILLS);
        List<String> currentSkills = getCurrentSkills();
        clickOnPencilButton(EmployeeInfoName.SKILLS);
        clickOnAllActiveCheckBoxes(currentSkills);
        clickOnChangeButton(EmployeeInfoName.SKILLS);
        checkSkillsDeletion(size, id);
    }

    @Test(groups = {"TEST-164.2", "not actual"},
            description = "Печать графика и табеля одного типа сотрудников")
    public void printGraphicForOnePosition() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        goToSchedule(unit);
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.PRINT);
        switchOffPositionsCheckBoxes();
        String positionIds = getJobTypeAndConvertToId();
        pushPrintButton();
        FileDownloadCheckerForScheduleBoard checker = new FileDownloadCheckerForScheduleBoard(Role.ADMIN,
                                                                                              TypeOfFiles.PDF, null, omId, positionIds);
        assertForRightDownloadingPDF(checker);
    }

    @Test(groups = {"TEST-164.3", "not actual"},
            description = "Печать только графика нескольких типов сотрудников")
    public void printOnlyGraphicsOfSeveralTypesOfEmployees() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        goToSchedule(unit);
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.PRINT);
        switchOffOnePositionCheckBox();
        String positionIds = getJobTypeAndConvertToId();
        pushRadioButtonOnlySchedule();
        pushPrintButton();
        FileDownloadCheckerForScheduleBoard checker = new FileDownloadCheckerForScheduleBoard(Role.ADMIN,
                                                                                              TypeOfFiles.PDF_ONLY_SCHEDULE, null, omId, positionIds);
        assertForRightDownloadingPDF(checker);
    }

    @Test(groups = {"TEST-164.1", "not actual"},
            description = "Печать графика и табеля нескольких типов сотрудников")
    public void printGraphicWithoutOnePosition() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        goToSchedule(unit);
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.PRINT);
        switchOffOnePositionCheckBox();
        String positionIds = getJobTypeAndConvertToId();
        pushPrintButton();
        FileDownloadCheckerForScheduleBoard checker = new FileDownloadCheckerForScheduleBoard(Role.ADMIN,
                                                                                              TypeOfFiles.PDF, null, omId, positionIds);
        assertForRightDownloadingPDF(checker);
    }

    @Test(groups = {"TEST-164.4", "not actual"}, description = "Печать только графика одного типа сотрудников")
    public void PrintOnlyGraphicsOfOneTypeOfEmployee() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        goToSchedule(unit);
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.PRINT);
        switchOffPositionsCheckBoxes();
        String positionIds = getJobTypeAndConvertToId();
        pushRadioButtonOnlySchedule();
        pushPrintButton();
        FileDownloadCheckerForScheduleBoard checker = new FileDownloadCheckerForScheduleBoard(Role.ADMIN,
                                                                                              TypeOfFiles.PDF_ONLY_SCHEDULE, null, omId, positionIds);
        assertForRightDownloadingPDF(checker);
    }

    @Test(groups = {"TEST-99"}, description = "Добавить дату увольнения")
    public void addEndDateToEmployee() {
        OrgUnit unit = OrgUnitRepository.getRandomAvailableOrgUnit();
        goToSchedule(unit);
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.SUBDIVISION_PROPERTIES);
        clickOnChevronButton(OmInfoName.EMPLOYEES);
        Employee employee = EmployeeRepository.chooseRandomEmployee(unit);
        clickOnEmployeeThreeDots(employee.getFullName());
        editButtonClick();
        LocalDate date = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.THAT, LocalDateTools.RANDOM).plusMonths(new Random().nextInt(5) + 1);
        chooseDatePositionForm(date, DateTypeField.END_JOB);
        saveButtonClick();
        assertDateEndAvailability(date, employee);
    }

    @Test(groups = {"TEST-88"}, description = "Назначить сотрудника на создаваемую должность")
    public void addEmployeeOnFreePosition() {
        OrgUnit unit = OrgUnitRepository.getRandomAvailableOrgUnit();
        goToSchedule(unit);
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.SUBDIVISION_PROPERTIES);
        clickOnChevronButton(OmInfoName.EMPLOYEES);
        List<String> temp = getEmployeesNamesOnUi();
        Map<String, String> tempPosition = emptyPositionsMap();
        String index = tempPosition.get("index");
        String positionName = tempPosition.get(NAME);
        clickOnEmptyPositionThreeDotsButton(index, positionName);
        editButtonClick();
        DateFormat yearMonthDay = new SimpleDateFormat("yyyy-MM-dd");
        clickOnSelectEmployeeChevron();
        Employee employee = EmployeeRepository.getRandomPersonNotWorking(temp);
        String name = employee.getFullName();
        selectAnyEmployee(name);
        LocalDate startDate = checkDataStartPosition();
        chooseDatePositionForm(startDate, DateTypeField.START_JOB);
        LocalDate endDate = LocalDateTools.randomSeedDate(5, -2, ChronoUnit.MONTHS, TimeType.RANDOM);
        chooseDatePositionForm(endDate, DateTypeField.END_JOB);
        saveButtonClick();
        assertForStateEmployeeOnFreePosition(yearMonthDay.format(startDate),
                                             yearMonthDay.format(endDate), name, positionName);
    }

    @Test(groups = {"TEST-169"}, description = "Сравнение версий графиков")
    public void scheduleCompare() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        goToSchedule(unit);
        int version = RosterRepository.getActiveRosterThisMonth(omId).getVersion();
        PresetClass.rosterPublishCheck(RosterRepository.getActiveRosterThisMonth(omId).getId());
        int employeeValue = employeesSize();
        listOfSchedulesClick();
        scheduleCompareButtonClick();
        listOfSchedulesClick();
        int addedVersion = selectAnotherVersion();
        clickOnRoster(addedVersion);
        goToTimeSheetCompareCheck(employeeValue, version, addedVersion);
    }

    @Test(groups = {"TEST-156.1"},
            description = "Добавление одного комментария к дням")
    public void createOneDayComment() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        int addSize = 1;
        LocalDate startDate = LocalDateTools.getFirstDate();
        LocalDate endDate = LocalDateTools.getLastDate();
        List<String> temp = PresetClass.checkDayComment(startDate, endDate, addSize, omId);
        goToSchedule(unit);
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.COMMENTS_ON_DAYS);
        enterCommentValue(addSize);
        commentsChangeButtonClick();
        checkCommentsCreation(addSize, temp, startDate, endDate);
    }

    @Test(groups = {"TEST-156.2"},
            description = "Добавление нескольких комментариев к дням")
    public void createAnyDayComment() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        LocalDate startDate = LocalDateTools.getFirstDate();
        LocalDate endDate = LocalDateTools.getLastDate();
        int addSize = new Random().nextInt(4) + 2;
        List<String> temp = PresetClass.checkDayComment(startDate, endDate, addSize, omId);
        goToSchedule(unit);
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.COMMENTS_ON_DAYS);
        enterCommentValue(addSize);
        commentsChangeButtonClick();
        checkCommentsCreation(addSize, temp, startDate, endDate);
    }

    @Ignore("Переведён на api")
    @Test(groups = {"TEST-132.1", SHIFTS, G0, SCHED9,
            "@Before set default shift duration",
            "@Before disable start time check for worked shifts"},
            description = "Создание смен")
    @Link(name = "Статья: \"Расписание\"", url = "https://wiki.goodt.me/x/RAOrCg")
    @Severity(SeverityLevel.CRITICAL)
    @TmsLink("61650")
    @Tag("TEST-132.1")
    @Tag(SCHED9)
    public void createShift() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        PresetClass.changeOrSetMathParamValue(omId, MathParameterValues.ACCESS_TO_OVERWORK, true, true);
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        PresetClass.kpiAndFteChecker(omId);
        EmployeePosition employeePosition = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, false);
        LocalDate date = PresetClass.getFreeDateFromNow(employeePosition);
        goToSchedule(unit);
        clickOnEmptyCell(employeePosition, date, scheduleWorker);
        clickOnPlusCellOnGraph();
        LocalTime start = LocalTime.of(10, 0, 0);
        LocalTime end = LocalTime.of(19, 0, 0);
        enterShiftTimeStartOrEnd(start, TimeTypeField.START_TIME);
        enterShiftTimeStartOrEnd(end, TimeTypeField.END_TIME);
        createShiftButtonClick();
        assertCreateShift(employeePosition, new DateTimeInterval(date.atTime(start),
                                                                 date.atTime(end)), scheduleWorker, false);
    }

    @Test(groups = {"TEST-132.2",
            "@Before set default shift duration"}, description = "Создание смены для виртуального сотрудника")
    public void createShiftForVirtualEmployee() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        PresetClass.kpiAndFteChecker(omId);
        EmployeePosition employeePosition = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, false);
        LocalDate date = PresetClass.getFreeDateForEmployeeShiftPreset(employeePosition);
        goToSchedule(unit);
        clickOnEmptyCell(employeePosition, date, scheduleWorker);
        clickOnPlusCellOnGraph();
        LocalTime start = LocalTime.of(10, 0, 0);
        LocalTime end = LocalTime.of(19, 0, 0);
        enterShiftTimeStartOrEnd(start, TimeTypeField.START_TIME);
        enterShiftTimeStartOrEnd(end, TimeTypeField.END_TIME);
        createShiftButtonClick();
        assertCreateShift(employeePosition, new DateTimeInterval(date.atTime(start),
                                                                 date.atTime(end)), scheduleWorker, false);
    }

    @Ignore("Заменен на api-тест")
    @Test(groups = {"TEST-133", SHIFTS, G0, SCHED9,
            "@Before set default shift duration",
            "@Before disable mandatory comments when deleting worked shift"}, description = "Удаление смен")
    @Link(name = "Статья: \"Расписание\"", url = "https://wiki.goodt.me/x/RAOrCg")
    @Severity(SeverityLevel.CRITICAL)
    @TmsLink("61649")
    @Tag(SCHED9)
    @Tag("TEST-133")
    public void deleteShift() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        PresetClass.kpiAndFteChecker(omId);
        EmployeePosition employeePosition = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, false);
        Shift shift = PresetClass.defaultShiftPreset(employeePosition);
        goToSchedule(unit);
        clickOnTargetShift(employeePosition, shift.getDateTimeInterval().getStartDate(), scheduleWorker);
        shiftThreeDotsClick();
        selectAction(RequestAction.DELETE, false);
        assertDeleteShift(employeePosition, shift, scheduleWorker);
    }

    @Ignore("Заменен на api-тест")
    @Test(groups = {"TEST-134", SHIFTS, G0, SCHED9,
            "@Before set default shift duration"},
            description = "Редактирование смен")
    @Link(name = "Статья: \"Расписание\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=178979652")
    @TmsLink("61648")
    @Severity(SeverityLevel.CRITICAL)
    @Tag("TEST-134")
    @Tag(SCHED9)
    public void editShift() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        PresetClass.kpiAndFteChecker(omId);
        EmployeePosition employeePosition = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, false);
        Shift firstShift = PresetClass.defaultShiftPreset(employeePosition, ShiftTimePosition.FUTURE);
        goToSchedule(unit);
        clickOnTargetShift(employeePosition, firstShift.getDateTimeInterval().getStartDate(), scheduleWorker);
        LocalTime start = firstShift.getDateTimeInterval().toTimeInterval().getStartTime().plusHours(1);
        LocalTime end = firstShift.getDateTimeInterval().toTimeInterval().getEndTime().plusHours(1);
        enterShiftTimeStartOrEnd(start, TimeTypeField.START_TIME);
        enterShiftTimeStartOrEnd(end, TimeTypeField.END_TIME);
        createShiftButtonClick();
        LocalDate date = firstShift.getDateTimeInterval().getStartDate();
        assertEditShift(employeePosition, new DateTimeInterval(date.atTime(start),
                                                               date.atTime(end)), scheduleWorker);
    }

    @Test(groups = {"TEST-159.1"},
            description = "Добавление одного комментария к версии графика")
    public void createOneRosterComment() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int previouslySize = PresetClass.checkFreeRostersComment(1, unit.getId());
        goToSchedule(unit);
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.COMMENTS_TO_THE_VERSIONS_OF_THE_CALCULATION);
        enterRosterComments(1);
        commentsChangeButtonClick();
        checkRosterCommentsAdding(unit, 1, previouslySize);
    }

    @Test(groups = {"TEST-159.2"},
            description = "Добавление нескольких комментариев к версиям графика")
    public void createTwoRosterComment() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int previouslySize = PresetClass.checkFreeRostersComment(2, orgUnit.getId());
        goToSchedule(orgUnit);
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.COMMENTS_TO_THE_VERSIONS_OF_THE_CALCULATION);
        enterRosterComments(2);
        commentsChangeButtonClick();
        checkRosterCommentsAdding(orgUnit, 2, previouslySize);
    }

    @Test(groups = {"TEST-117.1", G1, SCHED7}, priority = -2,
            description = "Массовая деактивация отображения сотрудников")
    @Link(name = "Статья: \"Расписание\"", url = "https://wiki.goodt.me/x/RAOrCg")
    @Tag("TEST-117.1")
    @Tag(SCHED7)
    public void employeesFilterRemoveAllTick() {
        OrgUnit unit = OrgUnitRepository.getRandomOmWithEmployeeOptions(OrgUnitOptions.EMPLOYEES_WITH_POSITION);
        goToSchedule(unit);
        employeeFilterButtonClick();
        clickOnSelectAllCheckBox();
        List<String> activeEmployees = activeEmployees();
        filterModeApplyButtonClick();
        checkFilterMode(activeEmployees, unit.getId());
    }

    @Test(groups = {"TEST-117.2", G1, SCHED7},
            description = "Деактивация отображения одного сотрудника")
    @Link(name = "Статья: \"Расписание\"", url = "https://wiki.goodt.me/x/RAOrCg")
    @Tag("TEST-117.2")
    @Tag(SCHED7)
    public void employeesFilterRemoveOneTick() {
        OrgUnit unit = OrgUnitRepository.getRandomOmWithEmployeeOptions(OrgUnitOptions.EMPLOYEES_WITH_POSITION);
        goToSchedule(unit);
        employeeFilterButtonClick();
        int noActiveNeed = 1;
        deactivateCheckBoxes(noActiveNeed);
        List<String> activeEmployees = activeEmployees();
        filterModeApplyButtonClick();
        checkFilterMode(activeEmployees, unit.getId());
    }

    @Test(groups = {"TEST-117.3"},
            description = "Деактивация отображения нескольких сотрудников")
    @Tag("TEST-117.3")
    public void employeesFilterRemoveAnyTick() {
        OrgUnit unit = OrgUnitRepository.getRandomOmWithEmployeeOptions(OrgUnitOptions.EMPLOYEES_WITH_POSITION);
        goToSchedule(unit);
        employeeFilterButtonClick();
        int noActiveNeed = new Random().nextInt(employeesSize() / 2);
        deactivateCheckBoxes(noActiveNeed);
        List<String> activeEmployees = activeEmployees();
        filterModeApplyButtonClick();
        checkFilterMode(activeEmployees, unit.getId());
    }

    @Test(groups = {"TEST-117.4", "TEST-1192"}, priority = -1,
            dependsOnMethods = {"employeesFilterRemoveAllTick"},
            description = "Массовая активация отображения сотрудников")
    @Tag("TEST-117.4")
    public void employeesFilterMassActivate() {
        Allure.addAttachment("Список сотрудников после деактивации в тесте \"Массовая деактивация отображения сотрудников\"",
                             "Сотрудники в расписании: " + sb.formLayout().employeeNameButtons()
                                     .stream().map(WebElement::getText).collect(Collectors.toList()));
        employeeFilterButtonClick();
        clickOnSelectAllCheckBox();
        List<String> activeEmployees = sb.employeesFilterMode().allEmployees().stream().map(AtlasWebElement::getText)
                .collect(Collectors.toList());
        filterModeApplyButtonClick();
        //checkFilterMode(activeEmployees, ); //todo
    }

    @Test(groups = {"TEST-117.5", "TEST-1192"},
            description = "Фильтр сотрудников по группе позиций")
    public void employeesFilterByPositionGroup() {
        OrgUnit unit = OrgUnitRepository.getRandomOmWithEmployeeOptions(OrgUnitOptions.WITH_POSITION_GROUPS);
        goToSchedule(unit);
        employeeFilterButtonClick();
        ImmutablePair<String, List<String>> randomPositionGroupEmployees = getRandomPositionGroupEmployees(unit.getId());
        pickFunctionalRoleFromEmployeeFilter(randomPositionGroupEmployees.left);
        filterModeApplyButtonClick();
        checkFilterMode(randomPositionGroupEmployees.right, unit.getId());
    }

    @Test(groups = {"TEST-117.6", "TEST-1192"},
            description = "Отмена применения фильтров")
    @Tag("TEST-117.6")
    public void cancelEmployeesFilter() {
        OrgUnit unit = OrgUnitRepository.getRandomOmWithEmployeeOptions(OrgUnitOptions.EMPLOYEES_WITH_POSITION);
        List<String> allEmployeeNames = EmployeePositionRepository.getEmployeePositions(unit.getId()).stream()
                .map(EmployeePosition::getEmployee)
                .map(Employee::getShortName)
                .collect(Collectors.toList());
        goToSchedule(unit);
        employeeFilterButtonClick();
        clickOnSelectAllCheckBox();
        closeFilterModeButtonClick();
        checkFilterMode(allEmployeeNames, unit.getId());
    }

    @Test(groups = {"ABCHR5211", G2, SCHED8, MAGNIT,
            "@Before change setting with name hint"},
            description = "Отображение подсказки при наведении на фамилию сотрудника")
    @Link(name = "Статья: \"5211_Доработка по отображению ФИО",
            url = "https://wiki.goodt.me/pages/viewpage.action?pageId=238978859")
    @Owner(MATSKEVICH)
    @TmsLink("60270")
    @Tag("ABCHR5211")
    @Tag(SCHED8)
    public void hintWithEmployeesName() {
        OrgUnit unit = OrgUnitRepository.getRandomOmWithEmployeeOptions(OrgUnitOptions.EMPLOYEES_WITH_POSITION);
        goToSchedule(unit);
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(unit.getId(), null, false);
        hoverEmployee(ep);
        checkHint(ep.getEmployee());
    }

    @Test(groups = {"ABCHR4075-1", G2, SCHED24},
            description = "Закрытие боковой формы планового графика")
    @Link(name = "Статья: \"4075_[Расписание] Закрытие боковой формы планового графика",
            url = "https://wiki.goodt.me/pages/viewpage.action?pageId=217714007")
    @Owner(MATSKEVICH)
    @TmsLink("60269")
    @Tag("ABCHR4075-1")
    @Tag(SCHED24)
    public void closingPlannedSchedule() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        goToSchedule(unit);
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.DOWNLOAD_PLANNED_SCHEDULE);
        closePlannedScheduleSidebar();
    }

    @Test(groups = {"ABCHR5706-1", G1, SCHED8},
            description = "Неактивность ячеек после увольнения")
    @Link(name = "Статья: \"5706_[Расписание] Ячейки после увольнения остаются активными",
            url = "https://wiki.goodt.me/pages/viewpage.action?pageId=234105629")
    @Owner(MATSKEVICH)
    @TmsLink("60268")
    @Tag("ABCHR5706-1")
    @Tag(SCHED8)
    public void cellsInactiveAfterDismissal() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        EmployeePosition ep = EmployeePositionRepository
                .getRandomEmployeeWithCheckByApi(unit.getId(), null, false);
        PresetClass.defaultShiftPreset(ep, ShiftTimePosition.FUTURE);
        LocalDate date = LocalDate.now().equals(LocalDateTools.getLastDate()) ? LocalDate.now().minusDays(1) : LocalDate.now();
        goToSchedule(unit);
        openEmployeesPositionForm(ep);
        chooseDatePositionForm(date, DateTypeField.END_JOB);
        chooseDatePositionForm(date, DateTypeField.POSITION_END_DATE);
        saveButtonClick();
        refreshPageAndAcceptAlertWindow();
        List<LocalDate> datesToCheck = getDatesForCheckingUi(new DateInterval(date.plusDays(1),
                                                                              LocalDateTools.getLastDate()));
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        assertInactiveEmptyCells(ep, scheduleWorker, datesToCheck);
    }

    @Test(groups = {"ABCHR2974", G1, SCHED2, POCHTA},
            dataProvider = "true/false", dataProviderClass = DataProviders.class,
            description = "Отображение индикатора по количеству смен")
    @Link(name = "Статья: \"2974_Сделать отображение индикатора по количеству смен " +
            "(открывающие/закрывающие) доступным по настройке\"",
            url = "https://wiki.goodt.me/pages/viewpage.action?pageId=204277215")
    @Owner(MATSKEVICH)
    @TmsLink("60255")
    @Tag("ABCHR2974")
    @Tag(SCHED2)
    public void displayIndicatorNumberShifts(boolean displayingShifts) {
        changeProperty(SystemProperties.SCHEDULE_BOARD_SHIFTS_INDICATOR, displayingShifts);
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        List<Shift> shifts = ShiftRepository.getShiftsForRoster(RosterRepository.getActiveRosterThisMonth(unit.getId()).getId(), new DateInterval());
        if (shifts.isEmpty()) {
            EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(unit.getId(), null, true);
            PresetClass.defaultShiftPreset(ep);
        }
        goToSchedule(unit);
        if (displayingShifts) {
            if (!isSelected(ButtonIDropDownMenu.NUMBER_SHIFTS)) {
                pressMenuI();
                selectItemFromDropDownMenuI(ButtonIDropDownMenu.NUMBER_SHIFTS);
            }
            checkShiftCounter();
        } else {
            Assert.assertThrows(WaitUntilException.class, this::checkShiftCounter);
        }
    }

    @Test(groups = {"ABCHR4232", G2, SCHED10,
            "@Before show the rate in the schedule"},
            dataProvider = "true/false", dataProviderClass = DataProviders.class,
            description = "Отображение полей \"Ставка\" и \"Табельный номер\" в карточке назначения")
    @Link(name = "Статья: \"4232_Добавить права на просмотр ставки и табельного номера\"",
            url = "https://wiki.goodt.me/pages/viewpage.action?pageId=217714370")
    @Owner(MATSKEVICH)
    @TmsLink("60252")
    @Tag(SCHED10)
    public void displayRateAndServiceNumber(boolean displayed) {
        changeTestIDDependingOnParameter(displayed, "ABCHR4232-1", "ABCHR4232-2",
                                         "Отсутствие полей \"Ставка\" и \"Табельный номер\" в карточке назначения без прав");
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(unit.getId(), null, false);
        List<PermissionType> permissions = new ArrayList<>(Arrays.asList(
                PermissionType.ORGANIZATION_UNIT_EDIT,
                PermissionType.SCHEDULE_VIEW,
                PermissionType.SCHEDULE_EDIT,
                PermissionType.ORG_UNIT_EMPLOYEE_PERSONALISATION));
        if (displayed) {
            permissions.add(PermissionType.EMPLOYEE_TABLE_RATE);
            changeProperty(SystemProperties.EMPLOYEE_POSITION_JOB_TITLE_DISABLED, false);
        }
        Role role = PresetClass.createCustomPermissionRole(permissions);
        goToScheduleAsUser(role, unit);
        openEmployeesPositionForm(ep);
        if (displayed) {
            String cardNumber = RandomStringUtils.randomNumeric(6);
            double rate = new Random().nextDouble();
            String rateText = String.format(Locale.US, "%.1f", rate);
            if (rateText.equals("1.0") || rateText.equals("0.0")) {
                rateText = "1";
            }
            changeRate(rateText);
            changeCardNumber(cardNumber);
            saveButtonClick();
            openEmployeesPositionForm(ep);
            ep = EmployeePositionRepository.getEmployeePositionById(ep.getId());
            assertRateValue(rateText, ep);
            assertCardNumberValue(cardNumber, ep);
        } else {
            assertNotDisplayedRate();
            assertNotDisplayedCardNumber();
        }
    }

    @Test(groups = {"ABCHR3972-1", G2, SCHED10},
            description = "Открытие карточки редактирования должности при нажатии на должность и сохранение изменений")
    @Link(name = "Статья: \"3972_Открывать карточку назначения сотрудника на должность при нажатии на должность\"",
            url = "https://wiki.goodt.me/pages/viewpage.action?pageId=214762619")
    @Owner(MATSKEVICH)
    @TmsLink("60251")
    @Tag("ABCHR3972-1")
    @Tag(SCHED10)
    public void editEmployeePosition() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        EmployeePosition ep = EmployeePositionRepository
                .getRandomEmployeeWithCheckByApi(unit.getId(), null, false);
        PresetClass.defaultShiftPreset(ep, ShiftTimePosition.FUTURE);
        goToSchedule(unit);
        openEmployeesPositionForm(ep);
        String cardNumber = RandomStringUtils.randomNumeric(6);
        changeCardNumber(cardNumber);
        saveButtonClick();
        openEmployeesPositionForm(ep);
        assertCardNumberValue(cardNumber, ep.refreshEmployeePosition());
    }

    @Test(groups = {"ABCHR7007-1", SHIFTS, G2, SCHED36, POCHTA,
            "@Before enable merged view for planned and actual shifts",
            "@Before disable equality check between plan and fact",
            "@Before enable start time check for worked shifts",
            "@Before disallow timesheet editing for past months",
            "@Before disable worked shift comments"},
            dataProvider = "true/false", dataProviderClass = DataProviders.class,
            description = "Отключение возможности корректирование начала смены факт < плана без доступа к переработке")
    @Link(name = "Статья: \"7007_Почта. Настройка позволяющая убрать возможность корректировать начало смены факт<плана\"",
            url = "https://wiki.goodt.me/pages/viewpage.action?pageId=256445240")
    @Owner(MATSKEVICH)
    @TmsLink("60231")
    @Tag(SCHED36)
    @Tag("ABCHR7007-1")
    public void prohibitAdjustmentOfStartOfShift(boolean hasAccess) {
        changeStepNameIfTrue(hasAccess, "Отключение возможности корректирование начала смены факт < плана с доступом к переработке");
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitWithWorkedRosters(false);
        int omId = unit.getId();
        PresetClass.changeOrSetMathParamValue(omId, MathParameterValues.ACCESS_TO_OVERWORK, hasAccess, true);
        EmployeePosition ep = getRandomFromList(EmployeePositionRepository.getEmployeesWithPosIds(unit.getId(), null, true));
        ShiftTimePosition timePosition = ShiftTimePosition.PAST;
        Shift shiftFact = PresetClass.presetForMakeShift(ep, false, timePosition);
        LocalDate date = shiftFact.getDateTimeInterval().getStartDate();
        Shift shiftPlan = PresetClass.presetForMakeShiftDate(ep, date, false, ShiftTimePosition.ALLMONTH);
        LocalTime start = shiftPlan.getDateTimeInterval().toTimeInterval().getStartTime().minusHours(1);
        List<Shift> shiftsBefore = ShiftRepository.getShifts(ep, timePosition);
        List<PermissionType> permissions = new ArrayList<>(Arrays.asList(
                PermissionType.SCHEDULE_VIEW,
                PermissionType.SCHEDULE_EDIT,
                PermissionType.SCHEDULE_EDIT_WORKED,
                PermissionType.ORG_UNIT_EMPLOYEE_PERSONALISATION));
        Role role = PresetClass.createCustomPermissionRole(permissions);
        goToScheduleAsUser(role, unit);
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        clickOnTargetShiftPlanOrFact(ep, date, scheduleWorker, true);
        enterShiftTimeStartOrEnd(start, TimeTypeField.START_TIME);
        createShiftButtonClick();
        String elementText = scheduleWorker.getScheduleShiftElement(ep, date).getText();
        String shiftId = shiftFact.getId().toString().replaceAll("(.{3})", "$0\u00A0").replaceAll("\u00A0$", "");
        String errorText = String.format("Не удается обновить смену %s потому что доступ к переработкам запрещен", shiftId);
        assertNoChangesToShifts(scheduleWorker, date, ep, shiftsBefore, elementText, errorText, timePosition);
    }

    @Ignore(NOT_ACTUAL)
    @Test(groups = {"ABCHR4595-1", G2, SCHED9},
            description = "Запретить создание смены после увольнения")
    @Link(name = "Статья: \"4595_Запретить создавать смену до назначения на должность и после увольнения с должности\"",
            url = "https://wiki.goodt.me/pages/viewpage.action?pageId=217715713")
    @Owner(MATSKEVICH)
    @TmsLink("60229")
    @Tag("ABCHR4595-1")
    @Tag(SCHED9)
    public void forbidCreationOfShiftAfterDismissal() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeePosition(unit);
        LocalDate date = LocalDate.now();
        ShiftTimePosition timePosition = ShiftTimePosition.DEFAULT;
        PresetClass.dismissEmployee(ep, date);
        PresetClass.presetForMakeShiftDate(ep, date, true, timePosition);
        goToSchedule(unit);
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        clickOnTargetShift(ep, date, scheduleWorker);
        LocalDateTime startDateTime = date.atTime(20, 0, 0);
        LocalDateTime endDateTime = date.plusDays(1).atTime(4, 0, 0);
        enterShiftDateStartOrEnd(endDateTime.toLocalDate(), DateTypeField.END_DATE);
        enterShiftTimeStartOrEnd(startDateTime.toLocalTime(), TimeTypeField.START_TIME);
        enterShiftTimeStartOrEnd(endDateTime.toLocalTime(), TimeTypeField.END_TIME);
        createShiftButtonClick();
        assertEditShift(ep, new DateTimeInterval(startDateTime, endDateTime), scheduleWorker);
    }

    @Test(groups = {"ABCHR4653-1", G2, SCHED9,
            "@Before disable all shift comments",
            "@Before disable mandatory comment when editing or deleting shift"},
            description = "Редактирование планового графика в прошлом без разрешения")
    @Link(name = "Статья: \"4653_Добавить права на редактирование планового графика в прошлом\"",
            url = "https://wiki.goodt.me/pages/viewpage.action?pageId=217716596")
    @Owner(MATSKEVICH)
    @TmsLink("60226")
    @Tag("ABCHR4653-1")
    @Tag(SCHED9)
    public void editPlannedShiftInPastWithoutPermission() {
        int editDates = 3;
        changeProperty(SystemProperties.PLAN_EDIT_PAST_DAYS, editDates);
        changeProperty(SystemProperties.PLAN_EDIT_FUTURE_STRONG, true);
        List<PermissionType> permissions = Arrays.asList(PermissionType.SCHEDULE_VIEW,
                                                         PermissionType.SCHEDULE_EDIT,
                                                         PermissionType.ORG_UNIT_EMPLOYEE_PERSONALISATION,
                                                         PermissionType.SCHEDULE_REPORT_CARD);
        Role role = PresetClass.createCustomPermissionRole(permissions);
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(unit.getId(), null, true);
        ShiftTimePosition timePosition = ShiftTimePosition.ALLMONTH;
        LocalDate date = getServerDate().minusDays(editDates + 1);
        PresetClass.presetForMakeShiftDate(ep, date, false, timePosition);
        LocalDate lastEditedDate = getServerDate().minusDays(editDates);
        PresetClass.presetForMakeShiftDate(ep, lastEditedDate, false, timePosition);
        LocalDate dateFree = getServerDate().minusDays(editDates + 2);
        PresetClass.makeClearDate(ep, timePosition, dateFree);
        goToScheduleAsUser(role, unit);
        if (isTimeSheetDisplayed()) {
            listOfSchedulesClick();
            isTimeSheetButtonClick();
        }
        boolean clickback = false;
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        clickback = clickBackAndTurnOnPlan(editDates, clickback);
        Allure.step(String.format("Проверить, что поля в смене сотрудника %s за %s доступны для редактирования, но отредактировать смену невозможно", ep, lastEditedDate),
                    () -> assertFailedShiftEdit(ep, lastEditedDate, scheduleWorker));
        Allure.step(String.format("Проверить, что смену сотрудника %s за %s нельзя удалить", ep, lastEditedDate),
                    () -> assertFailedShiftDelete(ep, lastEditedDate, scheduleWorker));
        clickback = clickBackAndTurnOnPlan(editDates + 1, clickback);
        Allure.step(String.format("Проверить, что смену сотрудника %s за %s нельзя редактировать", ep, date),
                    () -> assertInactiveCell(ep, date, scheduleWorker));
        clickBackAndTurnOnPlan(editDates + 2, clickback);
        Allure.step(String.format("Проверить, что ячейка смены сотрудника %s за %s не активна", ep, dateFree),
                    () -> assertInactiveEmptyCell(ep, scheduleWorker, dateFree));

    }

    @Test(groups = {"ABCHR2820", CONFLICTS, G1, SCHED33,
            "@Before activate the conflict indicator"},
            description = "Вывод и удаление информации о конфликте при создании смены с нарушением междусменного интервала")
    @Link(name = "Статья: \"2820_Добавить конфликт: Нарушение продолжительности междусменного отдыха (менее 12 часов)\"",
            url = "https://wiki.goodt.me/pages/viewpage.action?pageId=204276264")
    @Owner(MATSKEVICH)
    @TmsLink("60256")
    @Tag("ABCHR2820-1")
    @Tag("ABCHR2820-3")
    @Tag(SCHED33)
    public void outputAndDeleteConflictOfShiftIntervalViolation() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        PresetClass.changeOrSetMathParamValue(omId, MathParameterValues.REST_BETWEEN_SHIFTS, 12, true);
        PresetClass.enableConflictCalculationInSysList(ConstraintViolations.SHORT_REST_BETWEEN_SHIFTS);
        EmployeePosition ep = EmployeePositionRepository
                .getRandomEmployeeWithCheckByApi(unit.getId(), null, false);
        LocalDate date = Allure.step("Пресет. Создать конфликт с нарушением междусменного интервала",
                                     () -> PresetClass.createConflictOfShiftIntervalViolation(omId, ep));
        int rosterId = RosterRepository.getActiveRosterThisMonth(omId).getId();
        PresetClass.runConstViolationsCalc(rosterId);
        goToSchedule(unit);
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        checkConstraintViolation(omId, ep, date, scheduleWorker, "Нарушение продолжительности междусменного отдыха у сотрудника", true);
        Allure.step("Удалить смену с нарушением междусменного интервала", () -> PresetClass.makeClearDate(ep, date));
        PresetClass.runConstViolationsCalc(rosterId);
        refreshPageAndAcceptAlertWindow();
        Assert.assertThrows(java.lang.AssertionError.class, () ->
                checkConstraintViolation(omId, ep, date, scheduleWorker, "Нарушение продолжительности междусменного отдыха у сотрудника", true));
    }

    @Test(groups = {"TEST-144", G1, SCHED9},
            description = "Редактирование запроса")
    @Tag(SCHED9)
    @Tag("TEST-144")
    @Link(name = "Статья: \"Расписание\"", url = "https://wiki.goodt.me/x/RAOrCg")
    @TmsLink("61638")
    public void changeRequest() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        ScheduleWorker sWorker = new ScheduleWorker(sb);
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(unit.getId(), null, true);
        ImmutablePair<LocalDate, LocalDate> dates = PresetClass.twoFreeDaysChecker(ep, ShiftTimePosition.DEFAULT);
        ScheduleRequest request = PresetClass.createScheduleRequestForDate(ScheduleRequestStatus.APPROVED, dates.left, ep, ScheduleRequestType.VACATION);
        goToSchedule(unit);
        clickOnRequestBlock(request, sWorker, unit);
        LocalDate newDate = dates.right;
        enterShiftDateStartOrEnd(newDate, DateTypeField.START_DATE);
        if (sb.formEditForm().dateStartOrEndInput(DateTypeField.END_DATE.getName()).isDisplayed()) {
            enterShiftDateStartOrEnd(newDate, DateTypeField.END_DATE);
        }
        clickEditShiftButton();
        assertRequestChange(request, newDate, sWorker, unit, ScheduleRequestStatus.APPROVED);
    }

    @Test(groups = {"TEST-142"}, description = "Отклонение запроса")
    public void rejectRequest() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        goToSchedule(unit);
        ScheduleWorker sWorker = new ScheduleWorker(sb);
        ScheduleRequest request = PresetClass.preSetNotRepeatRequestCheck(ScheduleRequestStatus.APPROVED, unit.getId());
        clickOnRequestBlock(request, sWorker, unit);
        listOfRequestsThreeDotsButtonClick();
        selectAction(RequestAction.REJECT, false);
        assertRequestActionChange(request, ScheduleRequestStatus.CANCELED, unit.getId());
    }

    @Test(groups = {"TEST-143"}, description = "Подтверждение запроса")
    public void acceptRequest() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        goToSchedule(unit);
        ScheduleWorker sWorker = new ScheduleWorker(sb);
        ScheduleRequest request = PresetClass.preSetNotRepeatRequestCheck(ScheduleRequestStatus.CANCELED, unit.getId());
        clickOnRequestBlock(request, sWorker, unit);
        listOfRequestsThreeDotsButtonClick();
        selectAction(RequestAction.ACCEPT, false);
        assertRequestActionChange(request, ScheduleRequestStatus.APPROVED, unit.getId());
    }

    @Test(groups = {"TEST-114.1"}, description = "Переключение на день")
    @Tag("TEST-114.1")
    public void switchDayScope() {
        OrgUnit unit = OrgUnitRepository.getRandomAvailableOrgUnit();
        goToSchedule(unit);
        switchScope(ScopeType.DAY);
        scopeChecker(ScopeType.DAY);
    }

    @Test(groups = {"TEST-114.2"}, description = "Переключение на неделю")
    @Tag("TEST-114.2")
    public void switchWeekScope() {
        OrgUnit unit = OrgUnitRepository.getRandomAvailableOrgUnit();
        goToSchedule(unit);
        switchScope(ScopeType.WEEK);
        scopeChecker(ScopeType.WEEK);
    }

    @Test(groups = {"TEST-114.3", G1, SCHED8},
            description = "Переключение на месяц")
    @Link(name = "Статья: \"Расписание\"", url = "https://wiki.goodt.me/x/RAOrCg")
    @Tag("TEST-114.3")
    @TmsLink("61668")
    @Tag(SCHED8)
    public void switchMonthScope() {
        OrgUnit unit = OrgUnitRepository.getRandomAvailableOrgUnit();
        goToSchedule(unit);
        switchScope(ScopeType.values()[(int) (Math.random() * ((ScopeType.values().length - 1))) + 1]);
        switchScope(ScopeType.MONTH);
        scopeChecker(ScopeType.MONTH);
    }

    @Test(groups = {"TEST-139.1", G0, SHIFTS, SCHED9,
            "@Before enable schedule request: day off"}, description = "Создание запроса отсутствия выходной")
    @Link(name = "Статья: \"Расписание\"", url = "https://wiki.goodt.me/x/RAOrCg")
    @Severity(SeverityLevel.CRITICAL)
    @TmsLink("61643")
    @Tag("TEST-139.1")
    @Tag(SCHED9)
    public void createOffTimeRequest() {
        OrgUnit orgUnit;
        EmployeePosition employeePosition;
        //todo: после заливки дампа на магнит убрать костыль (07.05.2024)
        if (URL_BASE.contains("magnit")) {
            ImmutablePair<OrgUnit, EmployeePosition> orgUnitAndEmployee = OrgUnitRepository.getRandomOrgUnitWithRandomEmployeeForShiftPair(false);
            orgUnit = orgUnitAndEmployee.left;
            employeePosition = orgUnitAndEmployee.right;
        } else {
            orgUnit = OrgUnitRepository.getRandomOrgUnitsForShifts();
            int omId = orgUnit.getId();
            employeePosition = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        }
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        LocalDate date = PresetClass.getFreeDateFromNow(employeePosition);
        goToSchedule(orgUnit);
        clickOnEmptyCell(employeePosition, date, scheduleWorker);
        clickOnPlusCellOnGraph();
        clickOnSelectRequestTypeButton();
        ScheduleRequestType requestType = ScheduleRequestType.OFF_TIME;
        selectRequestType(requestType);
        createShiftButtonClick();
        assertRequestAdding(employeePosition, date, requestType, scheduleWorker, orgUnit, ScheduleRequestStatus.APPROVED);
    }

    @Test(groups = {"TEST-139.2"}, description = "Создание запроса отсутствия отпуск")
    public void createVacationRequest() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = orgUnit.getId();
        EmployeePosition employeePosition = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, false);
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        LocalDate dateValue = PresetClass.getFreeDateForEmployeeShiftPreset(employeePosition);
        goToSchedule(orgUnit);
        clickOnEmptyCell(employeePosition, dateValue, scheduleWorker);
        clickOnPlusCellOnGraph();
        clickOnSelectRequestTypeButton();
        ScheduleRequestType vacation = ScheduleRequestType.VACATION;
        selectRequestType(vacation);
        createShiftButtonClick();
        assertRequestAdding(employeePosition, dateValue, vacation, scheduleWorker, orgUnit, ScheduleRequestStatus.APPROVED);
    }

    @Test(groups = {"TEST-139.4", G1, SHIFTS, SCHED9},
            description = "Создание неявки с указанием даты", dataProvider = "Non-appearance types")
    @Link(name = "Статья: \"Расписание\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=178979652#id-%D0%A0%D0%B0%D1%81%D0%BF%D0%B8%D1%81%D0%B0%D0%BD%D0%B8%D0%B5-%D0%A0%D0%B0%D0%B1%D0%BE%D1%82%D0%B0%D1%81%D0%B7%D0%B0%D0%BF%D1%80%D0%BE%D1%81%D0%B0%D0%BC%D0%B8")
    @TmsLink("60317")
    @Tag("TEST-139.4")
    @Tag(SCHED9)
    public void addNonAppearanceRequest(IntervalType intervalType) {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        EmployeePosition employeePosition = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        changeSystemListEnableValue(ScheduleRequestType.NON_APPEARANCE, intervalType, true);
        LocalDate date = PresetClass.getFreeDateForEmployeeShiftPreset(employeePosition);
        goToSchedule(unit);
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        clickOnEmptyCell(employeePosition, date, scheduleWorker);
        clickOnPlusCellOnGraph();
        clickOnSelectRequestTypeButton();
        selectRequestType(ScheduleRequestType.NON_APPEARANCE, intervalType);
        LocalTime time = null;
        if (intervalType.equals(IntervalType.DATETIME)) {
            time = LocalTime.of(4, 0);
            enterShiftTimeStartOrEnd(time, TimeTypeField.END_TIME);
            changeTestName("Создание неявки с указанием даты и времени");
        }
        createShiftButtonClick();
        assertRequestAdding(employeePosition, date, ScheduleRequestType.NON_APPEARANCE, scheduleWorker, unit, ScheduleRequestStatus.APPROVED, time);
    }

    @Test(groups = {"ABCHR5057-1", G2, SHIFTS, SCHED9},
            description = "Создание повторяющейся неявки")
    @Link(name = "Статья: \"5057_Добавить возможность указывать периодичность для Неявки\"", url = "https://wiki.goodt.me/x/CACeDQ")
    @TmsLink("60247")
    @Tag("ABCHR5057-1")
    @Tag(SCHED9)
    public void addRepeatNonAppearanceRequest() {
        checkFirstDayOfMonth();
        ScheduleRequestType requestType = ScheduleRequestType.NON_APPEARANCE;
        IntervalType intervalType = Stream.of(IntervalType.DATETIME,
                                              IntervalType.DATE).collect(randomItem());
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        changeSystemListEnableValue(requestType, intervalType, true);
        LocalDate date = PresetClass.getFreeDateForEmployeeShiftPreset(ep, ShiftTimePosition.PAST,
                                                                       new DateInterval(LocalDateTools.getFirstDate(),
                                                                                        LocalDateTools.getLastDate().minusDays(8)));
        goToSchedule(unit);
        ScheduleWorker sw = new ScheduleWorker(sb);
        clickOnEmptyCell(ep, date, sw);
        clickOnPlusCellOnGraph();
        clickOnSelectRequestTypeButton();
        selectRequestType(requestType, intervalType);
        LocalTime time = null;
        if (intervalType.equals(IntervalType.DATETIME)) {
            time = LocalTime.of(4, 0);
            enterShiftTimeStartOrEnd(time, TimeTypeField.END_TIME);
        }
        Periodicity periodicity = Stream.of(Periodicity.DAILY,
                                            Periodicity.WEEKLY).collect(randomItem());
        selectPeriodicityForRequest(periodicity);
        LocalDate endRepeatDate = new DateInterval(date.plusDays(periodicity.getRepeatEveryValues() + 1), LocalDateTools.getLastDate()).getRandomDateBetween();
        enterEndRepeatDate(endRepeatDate);
        createShiftButtonClick();
        assertPeriodicalScheduleRequestsAdded(periodicity, date, endRepeatDate, ep, ScheduleRequestType.NON_APPEARANCE,
                                              sw, unit, ScheduleRequestStatus.APPROVED, time);
    }

    @Test(groups = {"TEST-139.5", G1, SHIFTS, SCHED9,
            "@Before enable schedule request: non-appearance"},
            description = "Удаление неявки")
    @Link(name = "Статья: \"Расписание\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=178979652#id-%D0%A0%D0%B0%D1%81%D0%BF%D0%B8%D1%81%D0%B0%D0%BD%D0%B8%D0%B5-%D0%A0%D0%B0%D0%B1%D0%BE%D1%82%D0%B0%D1%81%D0%B7%D0%B0%D0%BF%D1%80%D0%BE%D1%81%D0%B0%D0%BC%D0%B8")
    @TmsLink("60317")
    @Tag("TEST-139.5")
    @Tag(SCHED9)
    private void deleteNonAppearanceRequest() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        ScheduleRequest request = PresetClass.createScheduleRequestOfCertainType(ScheduleRequestStatus.APPROVED, false, unit.getId(),
                                                                                 ScheduleRequestType.NON_APPEARANCE);
        goToSchedule(unit);
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        clickOnRequestBlock(request, scheduleWorker, unit);
        listOfRequestsThreeDotsButtonClick();
        selectAction(RequestAction.DELETE, false);
        assertRequestDeleting(request, scheduleWorker, unit);
    }

    @Test(groups = {"TEST-139.3"}, description = "Создание запроса отсутствия больничный")
    public void createSickLeaveRequest() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = orgUnit.getId();
        EmployeePosition employeePosition = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, false);
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        LocalDate dateValue = PresetClass.getFreeDateForEmployeeShiftPreset(employeePosition);
        goToSchedule(orgUnit);
        clickOnEmptyCell(employeePosition, dateValue, scheduleWorker);
        clickOnPlusCellOnGraph();
        clickOnSelectRequestTypeButton();
        ScheduleRequestType requestType = ScheduleRequestType.SICK_LEAVE;
        selectRequestType(requestType);
        createShiftButtonClick();
        assertRequestAdding(employeePosition, dateValue, requestType, scheduleWorker, orgUnit, ScheduleRequestStatus.APPROVED);
    }

    @Test(groups = {"TEST-135",
            "@Before set default shift duration"},
            description = "Редактирование даты окончания смены")
    public void editDateEndShift() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = orgUnit.getId();
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        EmployeePosition employeePosition = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, false);
        Shift shift = PresetClass.defaultShiftPreset(employeePosition, ShiftTimePosition.FUTURE);
        LocalDate startDate = shift.getDateTimeInterval().getStartDate();
        goToSchedule(orgUnit);
        clickOnTargetShift(employeePosition, startDate, scheduleWorker);
        enterShiftDateStartOrEnd(startDate.plusDays(1), DateTypeField.END_DATE);
        LocalTime time = LocalTime.of(5, 0, 0);
        enterShiftTimeStartOrEnd(time, TimeTypeField.END_TIME);
        clickEditShiftButton();
        assertEditEndDateShift(employeePosition, startDate, startDate.plusDays(1).atTime(time), scheduleWorker);
    }

    @Test(groups = {"TEST-136", SHIFTS, G1, SCHED9,
            "@Before disable check of worked roster before adding shift", "@Before set default shift duration"},
            description = "Создание смены заканчивающейся в следующем дне")
    @Link(name = "Статья: \"Расписание\"", url = "https://wiki.goodt.me/x/RAOrCg")
    @Tag("TEST-136")
    @TmsLink("61646")
    @Tag(SCHED9)
    public void createShiftEndInNextDay() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = orgUnit.getId();
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        EmployeePosition employeePosition = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        LocalDate dateValue = PresetClass.getFreeDateFromNow(employeePosition);
        goToSchedule(orgUnit);
        clickOnEmptyCell(employeePosition, dateValue, scheduleWorker);
        clickOnPlusCellOnGraph();
        enterShiftDateStartOrEnd(dateValue.plusDays(1), DateTypeField.END_DATE);
        LocalTime start = LocalTime.of(10, 0, 0);
        LocalTime end = LocalTime.of(1, 0, 0);
        enterShiftTimeStartOrEnd(start, TimeTypeField.START_TIME);
        enterShiftTimeStartOrEnd(end, TimeTypeField.END_TIME);
        createShiftButtonClick();
        assertCreateShift(employeePosition, new DateTimeInterval(dateValue.atTime(start),
                                                                 dateValue.atTime(end).plusDays(1)), scheduleWorker, true);
    }

    @Ignore("Заменен на api-тест")
    @Test(groups = {"TEST-137", G1, SHIFTS, SCHED9,
            "@Before set default shift duration"},
            description = "Редактирование смены заканчивающейся в следующем дне")
    @Link(name = "Статья: \"Расписание\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=178979652#id-%D0%A0%D0%B0%D1%81%D0%BF%D0%B8%D1%81%D0%B0%D0%BD%D0%B8%D0%B5-%D0%A0%D0%B5%D0%B4%D0%B0%D0%BA%D1%82%D0%B8%D1%80%D0%BE%D0%B2%D0%B0%D0%BD%D0%B8%D0%B5%D1%81%D0%BC%D0%B5%D0%BD")
    @TmsLink("60233")
    @Tag("TEST-137")
    @Tag(SCHED9)
    public void editShiftEndInNextDay() {
        checkLastDayOfMonth();
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = orgUnit.getId();
        ScheduleWorker sw = new ScheduleWorker(sb);
        PresetClass.kpiAndFteChecker(omId);
        EmployeePosition employeePosition = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, false);
        Shift shift = PresetClass.shiftDateEndTomorrowInFuturePreset(employeePosition);
        DateTimeInterval interval = shift.getDateTimeInterval();
        LocalDate startShiftDate = interval.getStartDate();
        goToSchedule(orgUnit);
        clickOnTargetShift(employeePosition, startShiftDate, sw);
        LocalTime start = interval.getStartDateTime().toLocalTime().minusHours(1);
        LocalTime end = interval.getEndDateTime().toLocalTime().plusHours(1);
        enterShiftTimeStartOrEnd(start, TimeTypeField.START_TIME);
        enterShiftTimeStartOrEnd(end, TimeTypeField.END_TIME);
        createShiftButtonClick();
        assertEditShift(employeePosition, new DateTimeInterval(startShiftDate.atTime(start),
                                                               startShiftDate.plusDays(1).atTime(end)), sw);
    }

    @Test(groups = {"TEST-138", SHIFTS, G1, SCHED9,
            "@Before disable check of worked roster before adding shift", "@Before set default shift duration"},
            description = "Создание смены заканчивающейся в следующем месяце")
    @Link(name = "Статья: \"Расписание\"", url = "https://wiki.goodt.me/x/RAOrCg")
    @Tag("TEST-138")
    @TmsLink("61644")
    @Tag(SCHED9)
    public void createShiftEndInNextMonth() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = orgUnit.getId();
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        PresetClass.kpiAndFteChecker(omId);
        EmployeePosition employeePosition = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        LocalDate lastDate = LocalDateTools.getLastDate();
        PresetClass.makeClearDate(employeePosition, lastDate);
        goToSchedule(orgUnit);
        clickOnEmptyCell(employeePosition, lastDate, scheduleWorker);
        clickOnPlusCellOnGraph();
        enterShiftDateStartOrEnd(lastDate.plusDays(1), DateTypeField.END_DATE);
        LocalTime start = LocalTime.of(10, 0, 0);
        LocalTime end = LocalTime.of(1, 0, 0);
        enterShiftTimeStartOrEnd(start, TimeTypeField.START_TIME);
        enterShiftTimeStartOrEnd(end, TimeTypeField.END_TIME);
        createShiftButtonClick();
        assertCreateShiftEndInNextMonth(employeePosition, lastDate.atTime(start),
                                        lastDate.plusDays(1).atTime(end), scheduleWorker);
    }

    @Test(groups = {"TEST-109"}, description = "Создать виртуального сотрудника")
    public void createVirtualEmployee() {
        OrgUnit unit = OrgUnitRepository.getRandomAvailableOrgUnit();
        goToSchedule(unit);
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.SUBDIVISION_PROPERTIES);
        clickOnChevronButton(OmInfoName.EMPLOYEES);
        clickOnPlusButtonEmployee();
        List<String> temp = getEmployeesNamesOnUi();
        clickOnSelectEmployeeChevron();
        Employee employee = EmployeeRepository.getRandomPersonNotWorking(temp);
        String name = employee.getFullName();
        selectAnyEmployee(name);
        chooseJob(JobTitleRepository.getJob("Виртуальный сотрудник"));
        LocalDate startDate = LocalDateTools.randomSeedDate(-5, 24, ChronoUnit.MONTHS, TimeType.RANDOM);
        chooseDatePositionForm(startDate, DateTypeField.START_JOB);
        clickOnFunctionalRolesSelectButton();
        selectFuncRole(PositionGroupRepository.randomPositionGroup());
        chooseDatePositionForm(startDate, DateTypeField.POSITION_START_DATE);
        saveButtonClick();
        assertVirtualEmployeeAdding(unit, name);
    }

    @Test(groups = {"TEST-104"}, description = "Изменить параметры входа")
    public void changeLoginSettings() {
        OrgUnit unit = OrgUnitRepository.getRandomAvailableOrgUnit();
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(unit.getId(), null, false);
        goToSchedule(unit);
        String employeeName = getRandomEmployeeFromUI();
        clickOnEmployeeNameButton(ep);
        clickOnShowButton(EmployeeInfoName.LOGIN_OPTIONS);
        clickOnPencilButton(EmployeeInfoName.LOGIN_OPTIONS);
        String login = RandomStringUtils.randomAlphanumeric(10);
        String pass = "123456Qq";
        sendEmployeeLogin(login);
        sendEmployeePass(pass);
        sendEmployeeConformPass(pass);
        clickOnChangeButton(EmployeeInfoName.LOGIN_OPTIONS);
        assertEmployeeLogin(employeeName, login);
    }

    @Test(groups = {"TEST-145.1"}, description = "Удаление запроса")
    public void deleteRequest() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        ScheduleRequest request = PresetClass.preSetNotRepeatRequestCheck(ScheduleRequestStatus.APPROVED, unit.getId());
        goToSchedule(unit);
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        clickOnRequestBlock(request, scheduleWorker, unit);
        listOfRequestsThreeDotsButtonClick();
        selectAction(RequestAction.DELETE, false);
        assertRequestDeleting(request, scheduleWorker, unit);
    }

    @Test(groups = {"TEST-145.2", G1, SCHED9},
            description = "Удаление одного запроса из серии")
    @Link(name = "Статья: \"Расписание\"", url = "https://wiki.goodt.me/x/RAOrCg")
    @Tag(SCHED9)
    @TmsLink("61637")
    @Tag("TEST-145.2")
    public void deleteOneOfRepeatRequest() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        ScheduleRequest request = PresetClass.preSetRepeatRequestCheck(unit.getId());
        goToSchedule(unit);
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        clickOnRequestBlock(request, scheduleWorker, unit);
        listOfRequestsThreeDotsButtonClick();
        selectAction(RequestAction.DELETE, true);
        clickDeleteRequestRadioButton();
        requestConfirmDeleteButtonClick();
        assertRequestDeleting(request, scheduleWorker, unit);
    }

    @Test(groups = {"TEST-146", SHIFTS, G1, SCHED9,
            "@Before disable check of worked roster before adding shift",
            "@Before disable merged view for planned and actual shifts",
            "@Before disable roster single edited version"},
            description = "Удаление серии запросов")
    @Tag("TEST-146")
    @Link(name = "Статья: \"Расписание\"", url = "https://wiki.goodt.me/x/RAOrCg")
    @TmsLink("61636")
    @Tag(SCHED9)
    public void deleteAllOfRepeatRequest() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        ScheduleRequest request = PresetClass.preSetRepeatRequestCheck(unit.getId());
        goToSchedule(unit);
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        clickOnRequestBlock(request, scheduleWorker, unit);
        listOfRequestsThreeDotsButtonClick();
        selectAction(RequestAction.DELETE, true);
        clickRepeatDeleteRequestRadioButton();
        requestConfirmDeleteButtonClick();
        LocalDate date = request.getDateTimeInterval().getStartDate();
        assertRequestDeleting(request, scheduleWorker, unit, date, date.plusDays(1));
    }

    @Test(groups = {"TEST-107"}, description = "Внести параметр по сотруднику (сохранить)")
    public void addEmployeeParameter() {
        OrgUnit unit = OrgUnitRepository.getRandomAvailableOrgUnit();
        goToSchedule(unit);
        EmployeePosition employeeIdName = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(unit.getId(), null, false);
        int id = employeeIdName.getId();
        EmployeeParams random = EmployeeParams.getRandomSimpleParam();
        PresetClass.checkEmployeeParams(id, random.getId(), random.getName());
        clickOnEmployeeNameButton(employeeIdName);
        clickOnShowButton(EmployeeInfoName.OPTIONS);
        clickOnPencilButton(EmployeeInfoName.OPTIONS);
        String value = RandomStringUtils.randomAlphabetic(10);
        enterParamValue(random, value);
        clickOnChangeButton(EmployeeInfoName.OPTIONS);
        assertParamAdding(value, random, id);
    }

    @Test(groups = {"TEST-140.1", G1, SCHED9,
            "@Before enable schedule request: day off"},
            description = "Создание запроса отсутствия выходной с ежедневной периодичностью")
    @Link(name = "Статья: \"Расписание\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=178979652#id-%D0%A0%D0%B0%D1%81%D0%BF%D0%B8%D1%81%D0%B0%D0%BD%D0%B8%D0%B5-%D0%A1%D0%BE%D0%B7%D0%B4%D0%B0%D0%BD%D0%B8%D0%B5%D0%BE%D0%B4%D0%B8%D0%BD%D0%B0%D1%80%D0%BD%D1%8B%D1%85%D0%B7%D0%B0%D0%BF%D1%80%D0%BE%D1%81%D0%BE%D0%B2%D0%BE%D1%82%D1%81%D1%83%D1%82%D1%81%D1%82%D0%B2%D0%B8%D1%8F")
    @TmsLink("60233")
    @Tag("TEST-140.1")
    @Tag(SCHED9)
    public void createDailyRepeatRequest() {
        Periodicity periodicity = Periodicity.DAILY;
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(orgUnit.getId(), null, false);
        LocalDate date = PresetClass.getFreeDateForEmployeeShiftPreset(ep, ShiftTimePosition.FUTURE, new DateInterval());
        LocalDate endDate = new DateInterval(date.plusDays(periodicity.getRepeatEveryValues()), date.plusDays(Periodicity.DAILY.getRepeatEveryValues() + 10)).getRandomDateBetween();
        goToSchedule(orgUnit);
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        clickOnEmptyCell(ep, date, scheduleWorker);
        clickOnPlusCellOnGraph();
        clickOnSelectRequestTypeButton();
        ScheduleRequestType requestType = ScheduleRequestType.OFF_TIME;
        selectRequestType(requestType);
        selectPeriodicityForRequest(periodicity);
        enterPeriodicityEndDate(endDate);
        List<ScheduleRequest> requestsBefore = ScheduleRequestRepository.getEmployeeScheduleRequests(ep.getEmployee().getId(),
                                                                                                     new DateInterval(date, endDate), orgUnit.getId());
        createShiftButtonClick();
        assertRepeatRequestAdding(ep, requestType, periodicity, date, endDate, scheduleWorker, orgUnit, requestsBefore);
    }

    @Test(groups = {"TEST-140.2", G1, SCHED9,
            "@Before enable schedule request: day off",
            "@Before disable merged view for planned and actual shifts"},
            description = "Создание серии запросов отсутствия выходной с еженедельной периодичностью")
    @Tag("TEST-140.2")
    @Link(name = "Статья: \"Расписание\"", url = "https://wiki.goodt.me/x/RAOrCg")
    @TmsLink("61642")
    @Tag(SCHED9)
    @Severity(SeverityLevel.NORMAL)
    public void createWeeklyRepeatRequest() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        EmployeePosition employeePosition = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(orgUnit.getId(), null, false);
        LocalDate date = PresetClass.getFreeDateFromNow(employeePosition);
        LocalDate endDate = date.plusDays(8);
        PresetClass.checkEmptyCellAndPreset(employeePosition, date, LocalDateTools.now().getMonthValue());
        goToSchedule(orgUnit);
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        clickOnEmptyCell(employeePosition, date, scheduleWorker);
        clickOnPlusCellOnGraph();
        clickOnSelectRequestTypeButton();
        ScheduleRequestType requestType = ScheduleRequestType.OFF_TIME;
        Periodicity periodicity = Periodicity.WEEKLY;
        selectRequestType(requestType);
        clickOnPeriodicitySelectButton();
        selectPeriodicity(periodicity);
        enterPeriodicityEndDate(endDate);
        List<ScheduleRequest> requestsBefore = ScheduleRequestRepository.getEmployeeScheduleRequests(employeePosition.getEmployee().getId(),
                                                                                                     new DateInterval(date, date), orgUnit.getId());
        createShiftButtonClick();
        assertRepeatRequestAdding(employeePosition, requestType, periodicity, date, endDate, scheduleWorker, orgUnit, requestsBefore);
    }

    @Test(groups = {"TEST-140.3", G1, SCHED9,
            "@Before enable schedule request: day off"},
            description = "Создание запроса отсутствия выходной с ежемесячной периодичностью")
    @Link(name = "Статья: \"Расписание\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=178979652#id-%D0%A0%D0%B0%D1%81%D0%BF%D0%B8%D1%81%D0%B0%D0%BD%D0%B8%D0%B5-%D0%A1%D0%BE%D0%B7%D0%B4%D0%B0%D0%BD%D0%B8%D0%B5%D0%BE%D0%B4%D0%B8%D0%BD%D0%B0%D1%80%D0%BD%D1%8B%D1%85%D0%B7%D0%B0%D0%BF%D1%80%D0%BE%D1%81%D0%BE%D0%B2%D0%BE%D1%82%D1%81%D1%83%D1%82%D1%81%D1%82%D0%B2%D0%B8%D1%8F")
    @TmsLink("60233")
    @Tag("TEST-140.3")
    @Tag(SCHED9)
    public void createMonthlyRepeatRequest() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        EmployeePosition employeePosition = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(orgUnit.getId(), null, false);
        LocalDate startDate = PresetClass.getFreeDateFromNow(employeePosition);
        LocalDate endDate = startDate.plusDays(32);
        PresetClass.makeClearDate(employeePosition, startDate);
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        goToSchedule(orgUnit);
        clickOnEmptyCell(employeePosition, startDate, scheduleWorker);
        clickOnPlusCellOnGraph();
        clickOnSelectRequestTypeButton();
        ScheduleRequestType requestType = ScheduleRequestType.OFF_TIME;
        Periodicity periodicity = Periodicity.MONTHLY;
        selectRequestType(requestType);
        clickOnPeriodicitySelectButton();
        selectPeriodicity(periodicity);
        enterPeriodicityEndDate(endDate);
        List<ScheduleRequest> requestsBefore = ScheduleRequestRepository.getEmployeeScheduleRequests(employeePosition.getEmployee().getId(),
                                                                                                     new DateInterval(startDate, startDate), orgUnit.getId());
        createShiftButtonClick();
        assertRepeatRequestAdding(employeePosition, requestType, periodicity, startDate, endDate, scheduleWorker, orgUnit, requestsBefore);
    }

    @Test(groups = {"TEST-108"}, description = "Скорректировать параметр по сотруднику")
    public void adjustEmployeeParameter() {
        OrgUnit unit = OrgUnitRepository.getRandomAvailableOrgUnit();
        goToSchedule(unit);
        EmployeePosition employeePosition = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(unit.getId(), null, false);
        clickOnEmployeeNameButton(employeePosition);
        clickOnShowButton(EmployeeInfoName.OPTIONS);
        clickOnPencilButton(EmployeeInfoName.OPTIONS);
        MathParameters matchParameter = MathParameters.getRandomMathParameter();
        VariantsInMathParameters variant = returnVariant(matchParameter);
        chooseMatchParam(matchParameter);
        chooseVariant(variant);
        clickOnChangeButton(EmployeeInfoName.OPTIONS);
        assertParamChanging(variant, matchParameter, employeePosition.getEmployee());
    }

    @Test(groups = {"TEST-141", "@Before set default shift duration"},
            description = "Создание запроса смены")
    public void createShiftRequest() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        goToSchedule(orgUnit);
        EmployeePosition employeePosition = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(orgUnit.getId(), null, false);
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        LocalDate date = PresetClass.getFreeDateFromNow(employeePosition);
        clickOnEmptyCell(employeePosition, date, scheduleWorker);
        clickOnPlusCellOnGraph();
        clickOnSelectRequestTypeButton();
        ScheduleRequestType requestType = ScheduleRequestType.SHIFT_REQUEST;
        selectRequestType(requestType);
        LocalTime start = LocalTime.of(10, 0, 0);
        LocalTime end = LocalTime.of(19, 0, 0);
        enterShiftTimeStartOrEnd(start, TimeTypeField.START_TIME);
        enterShiftTimeStartOrEnd(end, TimeTypeField.END_TIME);
        createShiftButtonClick();
        assertRequestAdding(employeePosition, date, requestType, scheduleWorker, orgUnit, ScheduleRequestStatus.APPROVED);
    }

    @Ignore("Перенос в api")
    @Test(groups = {"TEST-121.1", SHIFTS, G0, SCHED9,
            "@Before disable all shift comments"},
            description = "Перенос одной смены на свободную ячейку другого сотрудника")
    @Link(name = "Статья: \"Расписание\"", url = "https://wiki.goodt.me/x/RAOrCg")
    @Severity(SeverityLevel.CRITICAL)
    @TmsLink("61661")
    @Tag("TEST-121.1")
    @Tag(SCHED9)
    public void transferOneShiftOnEmptyCellAnotherEmployee() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitsForShifts(true, true, false, false, false);
        int omId = orgUnit.getId();
        ImmutablePair<EmployeePosition, EmployeePosition> twoEmployee = EmployeePositionRepository.getTwoRandomEmployeeWithCheckByApi(omId);
        EmployeePosition firstEmp = twoEmployee.left;
        EmployeePosition secondEmp = twoEmployee.right;
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        Shift firstShift = PresetClass.defaultShiftPreset(firstEmp, ShiftTimePosition.FUTURE);
        LocalDate freeDate = PresetClass.getFreeDateFromNow(secondEmp);
        goToSchedule(orgUnit);
        transferOneShift(firstEmp, secondEmp, firstShift.getDateTimeInterval().getStartDateTime(), freeDate.atStartOfDay(), scheduleWorker);
        clickReplaceShiftButton();
        assertTransferShiftToSameEmployee(firstShift, firstEmp, secondEmp, freeDate, scheduleWorker);
    }

    @Ignore("Перенос api")
    @Test(groups = {"TEST-121.2", SHIFTS, G1, SCHED9,
            "@Before disable check of worked roster before adding shift",
            "@Before disable all shift comments"},
            description = "Перенос одной смены на свободную ячейку одного сотрудника")
    @Link(name = "Статья: \"Расписание\"", url = "https://wiki.goodt.me/x/RAOrCg")
    @Tag("TEST-121.2")
    @TmsLink("61661")
    @Tag(SCHED9)
    public void transferOneShiftOnEmptyCellSameEmployee() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = orgUnit.getId();
        PresetClass.changeOrSetMathParamValue(omId, MathParameterValues.ACCESS_TO_OVERWORK, true, true);
        EmployeePosition emp = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, false);
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        Shift firstShift = PresetClass.defaultShiftPreset(emp, ShiftTimePosition.FUTURE);
        LocalDate freeDate = PresetClass.getFreeDateFromNow(emp, firstShift.getDateTimeInterval().getStartDate());
        goToSchedule(orgUnit);
        transferOneShift(emp, emp, firstShift.getDateTimeInterval().getStartDateTime(), freeDate.atStartOfDay(), scheduleWorker);
        clickReplaceShiftButton();
        assertTransferShiftToSameEmployee(firstShift, emp, emp, freeDate, scheduleWorker);
    }

    @Ignore("Перенос в api")
    @Test(groups = {"TEST-122.1", SHIFTS, G1, SCHED9,
            "@Before disable check of worked roster before adding shift"},
            description = "Перенос смены, которая заканчивается следующим днем, на пустую ячейку другого сотрудника")
    @Link(name = "Статья: \"Расписание\"", url = "https://wiki.goodt.me/x/RAOrCg")
    @Tag("TEST-122.1")
    @TmsLink("61661")
    @Tag(SCHED9)
    public void transferOneShiftEndInNextDay() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitsForShifts(true, true, false, false, false);
        int omId = orgUnit.getId();
        ImmutablePair<EmployeePosition, EmployeePosition> twoEmployee = EmployeePositionRepository.getTwoRandomEmployeeWithCheckByApi(omId);
        EmployeePosition firstEmp = twoEmployee.left;
        EmployeePosition secondEmp = twoEmployee.right;
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        Shift firstShift = PresetClass.shiftDateEndTomorrowInFuturePreset(firstEmp, ShiftTimePosition.DEFAULT);
        LocalDate freeDate = PresetClass.getFreeDateForEmployeeShiftPreset(secondEmp);
        goToSchedule(orgUnit);
        transferOneShift(firstEmp, secondEmp, firstShift.getDateTimeInterval().getStartDateTime(), freeDate.atStartOfDay(), scheduleWorker);
        clickReplaceShiftButton();
        assertTransferShiftToSameEmployee(firstShift, firstEmp, secondEmp, freeDate, scheduleWorker);
    }

    @Test(groups = {"TEST-122.2"},
            description = "Перенос смены, которая заканчивается следующим днем, на пустую ячейку одного сотрудника")
    public void transferOneShiftEndInNextDaySameEmp() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = orgUnit.getId();
        EmployeePosition emp = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, false);
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        Shift firstShift = PresetClass.shiftDateEndTomorrowInFuturePreset(emp, ShiftTimePosition.DEFAULT);
        LocalDate freeDate = PresetClass.getFreeDateForEmployeeShiftPreset(emp, firstShift.getDateTimeInterval().getStartDate());
        goToSchedule(orgUnit);
        transferOneShift(emp, emp, firstShift.getDateTimeInterval().getStartDateTime(), freeDate.atStartOfDay(), scheduleWorker);
        clickReplaceShiftButton();
        assertTransferShiftToSameEmployee(firstShift, emp, emp, freeDate, scheduleWorker);
    }

    @Test(groups = {"TEST-122.3"},
            description = "Перенос смены, которая заканчивается следующим днем, на занятую ячейку другого сотрудника")
    public void transferOneShiftEndInNextDayToAnother() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitsForShifts(true, true, false, false, false);
        int omId = orgUnit.getId();
        ImmutablePair<EmployeePosition, EmployeePosition> twoEmployee = EmployeePositionRepository.getTwoRandomEmployeeWithCheckByApi(omId);
        EmployeePosition firstEmp = twoEmployee.left;
        EmployeePosition secondEmp = twoEmployee.right;
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        Shift fromShift = PresetClass.shiftDateEndTomorrowInFuturePreset(firstEmp, ShiftTimePosition.DEFAULT);
        Shift toShift = PresetClass.defaultShiftPreset(secondEmp);
        goToSchedule(orgUnit);
        transferOneShift(firstEmp, secondEmp, fromShift.getDateTimeInterval().getStartDateTime(), toShift.getDateTimeInterval().getStartDateTime(), scheduleWorker);
        clickReplaceShiftButton();
        assertTransferShift(firstEmp, secondEmp, fromShift, toShift, scheduleWorker);
    }

    @Test(groups = {"TEST-122.4"},
            description = "Перенос смены, которая заканчивается следующим днем, на занятую ячейку одного сотрудника")
    public void transferOneShiftEndInNextDayToSameEmpShift() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = orgUnit.getId();
        EmployeePosition employeePosition = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, false);
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        Shift fromShift = PresetClass.shiftDateEndTomorrowInFuturePreset(employeePosition, ShiftTimePosition.DEFAULT);
        Shift toShift = PresetClass.defaultShiftPreset(employeePosition, fromShift.getDateTimeInterval().getStartDate());
        goToSchedule(orgUnit);
        transferOneShift(employeePosition, employeePosition, fromShift.getDateTimeInterval().getStartDateTime(),
                         toShift.getDateTimeInterval().getStartDateTime(), scheduleWorker);
        clickReplaceShiftButton();
        assertTransferShift(employeePosition, employeePosition, fromShift, toShift, scheduleWorker);
    }

    @Test(groups = {"TEST-123.1"},
            description = "Перенос смены, которая заканчивается следующим днем на последний день месяца, на пустую ячейку другого сотрудника")
    public void transferOneShiftToLastDay() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = orgUnit.getId();
        ImmutablePair<EmployeePosition, EmployeePosition> twoEmployee = EmployeePositionRepository.getTwoRandomEmployeeWithCheckByApi(omId);
        EmployeePosition firstEmp = twoEmployee.left;
        EmployeePosition secondEmp = twoEmployee.right;
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        Shift shift = PresetClass.shiftDateEndTomorrowInFuturePreset(firstEmp, ShiftTimePosition.FUTURE);
        LocalDate lastDate = LocalDateTools.getLastDate();
        PresetClass.makeClearDate(secondEmp, lastDate);
        goToSchedule(orgUnit);
        transferOneShift(firstEmp, secondEmp, shift.getDateTimeInterval().getStartDateTime(), lastDate.atStartOfDay(), scheduleWorker);
        clickReplaceShiftButton();
        assertTransferShiftToSameEmployee(shift, firstEmp, secondEmp, lastDate, scheduleWorker);
    }

    @Test(groups = {"TEST-123.2"},
            description = "Перенос смены, которая заканчивается следующим днем на последний день месяца, на пустую ячейку одного сотрудника")
    public void transferOneShiftToLastDaySameEmp() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = orgUnit.getId();
        EmployeePosition employeePosition = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, false);
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        Shift shift = PresetClass.shiftDateEndTomorrowInFuturePreset(employeePosition, ShiftTimePosition.FUTURE);
        LocalDate lastDate = LocalDateTools.getLastDate();
        PresetClass.makeClearDate(employeePosition, lastDate);
        goToSchedule(orgUnit);
        transferOneShift(employeePosition, employeePosition, shift.getDateTimeInterval().getStartDateTime(), lastDate.atStartOfDay(), scheduleWorker);
        clickReplaceShiftButton();
        assertTransferShiftToSameEmployee(shift, employeePosition, employeePosition, lastDate, scheduleWorker);
    }

    @Test(groups = {"TEST-123.3"},
            description = "Перенос смены, которая заканчивается следующим днем " +
                    "на последний день месяца, на занятую ячейку другому сотруднику")
    public void transferOneShiftToLastShift() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = orgUnit.getId();
        ImmutablePair<EmployeePosition, EmployeePosition> twoEmployee = EmployeePositionRepository.getTwoRandomEmployeeWithCheckByApi(omId);
        EmployeePosition firstEmp = twoEmployee.left;
        EmployeePosition secondEmp = twoEmployee.right;
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        Shift firstShift = PresetClass.shiftDateEndTomorrowInFuturePreset(firstEmp, ShiftTimePosition.FUTURE);
        Shift secondShift = PresetClass.getLastDayMonthShift(secondEmp);
        goToSchedule(orgUnit);
        transferOneShift(firstEmp, secondEmp, firstShift.getDateTimeInterval().getStartDateTime(), secondShift.getDateTimeInterval().getStartDateTime(), scheduleWorker);
        clickReplaceShiftButton();
        assertTransferShift(firstEmp, secondEmp, firstShift, secondShift, scheduleWorker);
    }

    @Test(groups = {"TEST-123.4"},
            description = "Перенос смены, которая заканчивается следующим днем на занятую сменой ячейку последнего дня месяца одного сотрудника")
    public void transferOneShiftToLastShiftSameEmp() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = orgUnit.getId();
        EmployeePosition employeePosition = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, false);
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        Shift secondShift = PresetClass.getLastDayMonthShift(employeePosition);
        Shift shift = PresetClass.shiftDateEndTomorrowInFuturePreset(employeePosition, ShiftTimePosition.FUTURE, secondShift);
        goToSchedule(orgUnit);
        transferOneShift(employeePosition, employeePosition, shift.getDateTimeInterval().getStartDateTime(), secondShift.getDateTimeInterval().getStartDateTime(), scheduleWorker);
        clickReplaceShiftButton();
        assertTransferShift(employeePosition, employeePosition, shift, secondShift, scheduleWorker);
    }

    @Ignore("Перенос в api")
    @Test(groups = {"TEST-127.1", SHIFTS, G1, SCHED9,
            "@Before disable check of worked roster before adding shift"},
            description = "Обмен одной смены разных сотрудников")
    @Link(name = "Статья: \"Расписание\"", url = "https://wiki.goodt.me/x/RAOrCg")
    @Tag("TEST-127.1")
    @TmsLink("61655")
    @Tag(SCHED9)
    public void oneShiftExchangeToAnother() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitsForShifts(true, true, false, false, false);
        int omId = orgUnit.getId();
        PresetClass.changeOrSetMathParamValue(omId, MathParameterValues.ACCESS_TO_OVERWORK, true, true);
        ImmutablePair<EmployeePosition, EmployeePosition> twoEmployee = EmployeePositionRepository.getTwoRandomEmployeeWithCheckByApi(omId);
        EmployeePosition firstEmp = twoEmployee.left;
        EmployeePosition secondEmp = twoEmployee.right;
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        Shift fromShift = PresetClass.defaultShiftPreset(firstEmp);
        Shift toShift = PresetClass.defaultShiftPreset(secondEmp);
        goToSchedule(orgUnit);
        transferOneShift(firstEmp, secondEmp, fromShift.getDateTimeInterval().getStartDateTime(), toShift.getDateTimeInterval().getStartDateTime(), scheduleWorker);
        clickReplaceShiftButton();
        assertTransferShift(firstEmp, secondEmp, fromShift, toShift, scheduleWorker);
    }

    @Ignore("Перенос в api")
    @Test(groups = {"TEST-127.2", SHIFTS, G1, SCHED9,
            "@Before disable check of worked roster before adding shift"},
            description = "Обмен одной смены одного сотрудника")
    @Link(name = "Статья: \"Расписание\"", url = "https://wiki.goodt.me/x/RAOrCg")
    @Tag("TEST-127.2")
    @TmsLink("61655")
    @Tag(SCHED9)
    public void oneShiftExchangeToSame() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = orgUnit.getId();
        EmployeePosition emp = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, false);
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        Shift fromShift = PresetClass.defaultShiftPreset(emp);
        Shift toShift = PresetClass.defaultShiftPreset(emp, fromShift.getDateTimeInterval().getStartDate());
        goToSchedule(orgUnit);
        transferOneShift(emp, emp, fromShift.getDateTimeInterval().getStartDateTime(), toShift.getDateTimeInterval().getStartDateTime(), scheduleWorker);
        clickReplaceShiftButton();
        assertTransferShift(emp, emp, fromShift, toShift, scheduleWorker);
    }

    @Ignore("Перенос в api")
    @Test(groups = {"TEST-124.1", SHIFTS, G0, SCHED9,
            "@Before disable merged view for planned and actual shifts"},
            description = "Копирование одиночной смены на свободную ячейку другого сотрудника")
    @Link(name = "Статья: \"Расписание\"", url = "https://wiki.goodt.me/x/RAOrCg")
    @Severity(SeverityLevel.CRITICAL)
    @TmsLink("61658")
    @Tag("TEST-124.1")
    @Tag(SCHED9)
    public void duplicateOneShiftOnEmptyCellAnotherEmployee() {
        checkLastDayOfMonth();
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitsForShifts(true, true, false, false, false);
        int omId = orgUnit.getId();
        ImmutablePair<EmployeePosition, EmployeePosition> twoEmployee = EmployeePositionRepository.getTwoRandomEmployeeWithCheckByApi(omId);
        EmployeePosition firstEmp = twoEmployee.left;
        EmployeePosition secondEmp = twoEmployee.right;
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        Shift firstShift = defaultShiftPreset(firstEmp, ShiftTimePosition.FUTURE);
        LocalDate freeDate = getFreeDateForEmployeeShiftPreset(secondEmp, ShiftTimePosition.FUTURE);
        goToSchedule(orgUnit);
        transferOneShift(firstEmp, secondEmp, firstShift.getDateTimeInterval().getStartDateTime(), freeDate.atStartOfDay(), scheduleWorker);
        clickDuplicateShiftButton();
        assertDuplicateShift(firstEmp, firstShift, secondEmp, freeDate, scheduleWorker);
    }

    @Ignore("Перенос в api")
    @Test(groups = {"TEST-124.2", SHIFTS, G1, SCHED9, POCHTA,
            "@Before disable check of worked roster before adding shift",
            "@Before disable merged view for planned and actual shifts"},
            description = "Копирование одиночной смены на свободную ячейку одного сотрудника")
    @Link(name = "Статья: \"Расписание\"", url = "https://wiki.goodt.me/x/RAOrCg")
    @Tag("TEST-124.2")
    @TmsLink("61658")
    @Tag(SCHED9)
    public void duplicateOneShiftOnEmptyCellSameEmployee() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = orgUnit.getId();
        PresetClass.changeOrSetMathParamValue(omId, MathParameterValues.ACCESS_TO_OVERWORK, true, true);
        EmployeePosition emp = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, false);
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        Shift firstShift = PresetClass.defaultShiftPreset(emp);
        LocalDate freeDate = PresetClass.getFreeDateForEmployeeShiftPreset(emp, firstShift.getDateTimeInterval().getStartDate());
        goToSchedule(orgUnit);
        transferOneShift(emp, emp, firstShift.getDateTimeInterval().getStartDateTime(), freeDate.atStartOfDay(), scheduleWorker);
        clickDuplicateShiftButton();
        assertDuplicateShift(emp, firstShift, emp, freeDate, scheduleWorker);
    }

    @Ignore("Перенос в api")
    @Test(groups = {"TEST-125.1", SHIFTS, G1, SCHED9,
            "@Before disable check of worked roster before adding shift"},
            description = "Копирование смены, которая заканчивается следующим днем, на пустую ячейку другого сотрудника")
    @Link(name = "Статья: \"Расписание\"", url = "https://wiki.goodt.me/x/RAOrCg")
    @Tag("TEST-125.1")
    @TmsLink("61657")
    @Tag(SCHED9)
    public void duplicateOneShiftEndInNextDay() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitsForShifts(true, true, false, false, false);
        int omId = orgUnit.getId();
        ImmutablePair<EmployeePosition, EmployeePosition> twoEmployee = EmployeePositionRepository.getTwoRandomEmployeeWithCheckByApi(omId);
        EmployeePosition firstEmp = twoEmployee.left;
        EmployeePosition secondEmp = twoEmployee.right;
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        Shift firstShift = PresetClass.shiftDateEndTomorrowInFuturePreset(firstEmp, ShiftTimePosition.DEFAULT);
        LocalDate freeDate = PresetClass.getFreeDateForEmployeeShiftPreset(secondEmp);
        goToSchedule(orgUnit);
        transferOneShift(firstEmp, secondEmp, firstShift.getDateTimeInterval().getStartDateTime(), freeDate.atStartOfDay(), scheduleWorker);
        clickDuplicateShiftButton();
        assertDuplicateShift(firstEmp, firstShift, secondEmp, freeDate, scheduleWorker);
    }

    @Test(groups = {"TEST-125.2"},
            description = "Копирование смены, которая заканчивается следующим днем, на пустую ячейку одного сотрудника")
    public void duplicateOneShiftEndInNextDaySameEmp() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = orgUnit.getId();
        EmployeePosition employeePosition = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, false);
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        Shift firstShift = PresetClass.shiftDateEndTomorrowInFuturePreset(employeePosition, ShiftTimePosition.DEFAULT);
        LocalDate freeDate = PresetClass.getFreeDateForEmployeeShiftPreset(employeePosition, firstShift.getDateTimeInterval().getStartDate());
        goToSchedule(orgUnit);
        transferOneShift(employeePosition, employeePosition, firstShift.getDateTimeInterval().getStartDateTime(), freeDate.atStartOfDay(), scheduleWorker);
        clickDuplicateShiftButton();
        assertDuplicateShift(employeePosition, firstShift, employeePosition, freeDate, scheduleWorker);
    }

    @Test(groups = {"TEST-125.3"},
            description = "Копирование смены, которая заканчивается следующим днем, на занятую ячейку другого сотрудника")
    public void duplicateOneShiftEndInNextDayOnAnotherEmployeeShift() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitsForShifts(true, true, false, false, false);
        int omId = orgUnit.getId();
        ImmutablePair<EmployeePosition, EmployeePosition> twoEmployee = EmployeePositionRepository.getTwoRandomEmployeeWithCheckByApi(omId);
        EmployeePosition firstEmp = twoEmployee.left;
        EmployeePosition secondEmp = twoEmployee.right;
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        Shift firstShift = PresetClass.shiftDateEndTomorrowInFuturePreset(firstEmp, ShiftTimePosition.DEFAULT);
        Shift secondShift = PresetClass.shiftDateEndTomorrowInFuturePreset(secondEmp, ShiftTimePosition.DEFAULT);
        goToSchedule(orgUnit);
        transferOneShift(firstEmp, secondEmp, firstShift.getDateTimeInterval().getStartDateTime(), secondShift.getDateTimeInterval().getStartDateTime(), scheduleWorker);
        clickDuplicateShiftButton();
        assertDuplicateShift(firstEmp, firstShift, secondEmp, secondShift.getDateTimeInterval().getStartDate(), scheduleWorker);
    }

    @Test(groups = {"TEST-125.4"},
            description = "Копирование смены, которая заканчивается следующим днем, на занятую ячейку одного сотрудника")
    public void duplicateOneShiftEndInNextDayOnSameEmployeeShift() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = orgUnit.getId();
        EmployeePosition employeePosition = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, false);
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        Shift firstShift = PresetClass.shiftDateEndTomorrowInFuturePreset(employeePosition, ShiftTimePosition.DEFAULT);
        Shift secondShift = PresetClass.defaultShiftPreset(employeePosition, firstShift.getDateTimeInterval().getStartDate());
        goToSchedule(orgUnit);
        transferOneShift(employeePosition, employeePosition, firstShift.getDateTimeInterval().getStartDateTime(), secondShift.getDateTimeInterval().getStartDateTime(), scheduleWorker);
        clickDuplicateShiftButton();
        assertDuplicateShift(employeePosition, firstShift, employeePosition, secondShift.getDateTimeInterval().getStartDate(), scheduleWorker);
    }

    @Test(groups = {"TEST-126.1"},
            description = "Копирование смены, которая заканчивается следующим днем, на свободную ячейку последнего дня месяца другого сотрудника")
    public void duplicateOneShiftToLastDay() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = orgUnit.getId();
        ImmutablePair<EmployeePosition, EmployeePosition> twoEmployee = EmployeePositionRepository.getTwoRandomEmployeeWithCheckByApi(omId);
        EmployeePosition firstEmp = twoEmployee.left;
        EmployeePosition secondEmp = twoEmployee.right;
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        Shift firstShift = PresetClass.shiftDateEndTomorrowInFuturePreset(firstEmp, ShiftTimePosition.FUTURE);
        LocalDate lastDate = LocalDateTools.getLastDate();
        PresetClass.makeClearDate(secondEmp, lastDate);
        goToSchedule(orgUnit);
        transferOneShift(firstEmp, secondEmp, firstShift.getDateTimeInterval().getStartDateTime(), lastDate.atStartOfDay(), scheduleWorker);
        clickDuplicateShiftButton();
        assertDuplicateShift(firstEmp, firstShift, secondEmp, lastDate, scheduleWorker);
    }

    @Test(groups = {"TEST-126.2"},
            description = "Копирование смены, которая заканчивается следующим днем на последний день месяца, на свободную ячейку одного сотрудника")
    public void duplicateOneShiftToLastDaySameEmployee() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = orgUnit.getId();
        EmployeePosition employeePosition = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, false);
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        Shift firstShift = PresetClass.shiftDateEndTomorrowInFuturePreset(employeePosition, ShiftTimePosition.FUTURE);
        LocalDate lastDate = LocalDateTools.getLastDate();
        goToSchedule(orgUnit);
        transferOneShift(employeePosition, employeePosition, firstShift.getDateTimeInterval().getStartDateTime(), lastDate.atStartOfDay(), scheduleWorker);
        clickDuplicateShiftButton();
        assertDuplicateShift(employeePosition, firstShift, employeePosition, lastDate, scheduleWorker);
    }

    @Test(groups = {"TEST-126.3"},
            description = "Копирование смены, которая заканчивается следующим днем на последний день месяца, на занятую ячейку другого сотрудника")
    public void duplicateOneShiftToLastDayAnotherEmployeeShift() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = orgUnit.getId();
        ImmutablePair<EmployeePosition, EmployeePosition> twoEmployee = EmployeePositionRepository.getTwoRandomEmployeeWithCheckByApi(omId);
        EmployeePosition firstEmp = twoEmployee.left;
        EmployeePosition secondEmp = twoEmployee.right;
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        Shift firstShift = PresetClass.shiftDateEndTomorrowInFuturePreset(firstEmp, ShiftTimePosition.FUTURE);
        Shift secondShift = PresetClass.getLastDayMonthShift(secondEmp);
        goToSchedule(orgUnit);
        transferOneShift(firstEmp, secondEmp, firstShift.getDateTimeInterval().getStartDateTime(), secondShift.getDateTimeInterval().getStartDateTime(), scheduleWorker);
        clickDuplicateShiftButton();
        assertDuplicateShift(firstEmp, firstShift, secondEmp, secondShift.getDateTimeInterval().getStartDate(), scheduleWorker);
    }

    @Test(groups = {"TEST-126.4"},
            description = "Копирование смены, которая заканчивается следующим днем на последний день месяца, на занятую ячейку одного сотрудника")
    public void duplicateOneShiftToLastDaySameEmployeeShift() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = orgUnit.getId();
        EmployeePosition employeePosition = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, false);
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        Shift secondShift = PresetClass.getLastDayMonthShift(employeePosition);
        Shift firstShift = PresetClass.shiftDateEndTomorrowInFuturePreset(employeePosition, ShiftTimePosition.FUTURE, secondShift);
        goToSchedule(orgUnit);
        transferOneShift(employeePosition, employeePosition, firstShift.getDateTimeInterval().getStartDateTime(), secondShift.getDateTimeInterval().getStartDateTime(), scheduleWorker);
        clickDuplicateShiftButton();
        assertDuplicateShift(employeePosition, firstShift, employeePosition, secondShift.getDateTimeInterval().getStartDate(), scheduleWorker);
    }

    @Ignore("Перенос в api")
    @Test(groups = {"TEST-128.1", SHIFTS, G1, SCHED9,
            "@Before disable check of worked roster before adding shift"},
            description = "Копирование смены на смену другого сотрудника")
    @Link(name = "Статья: \"Расписание\"", url = "https://wiki.goodt.me/x/RAOrCg")
    @Tag("TEST-128.1")
    @TmsLink("61654")
    @Tag(SCHED9)
    public void copyShiftToShift() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = orgUnit.getId();
        PresetClass.changeOrSetMathParamValue(omId, MathParameterValues.ACCESS_TO_OVERWORK, true, true);
        ImmutablePair<EmployeePosition, EmployeePosition> twoEmployee = EmployeePositionRepository.getTwoRandomEmployeeWithCheckByApi(omId);
        EmployeePosition firstEmp = twoEmployee.left;
        EmployeePosition secondEmp = twoEmployee.right;
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        Shift firstShift = PresetClass.defaultShiftPreset(firstEmp);
        Shift secondShift = PresetClass.defaultShiftPreset(secondEmp);
        goToSchedule(orgUnit);
        transferOneShift(firstEmp, secondEmp, firstShift.getDateTimeInterval().getStartDateTime(), secondShift.getDateTimeInterval().getStartDateTime(), scheduleWorker);
        clickDuplicateShiftButton();
        assertDuplicateShift(firstEmp, firstShift, secondEmp, secondShift.getDateTimeInterval().getStartDate(), scheduleWorker);
    }

    @Ignore("Перенос в api")
    @Test(groups = {"TEST-128.2", SHIFTS, G1, SCHED9, POCHTA,
            "@Before disable check of worked roster before adding shift"},
            description = "Копирование смены на смену одного сотрудника")
    @Link(name = "Статья: \"Расписание\"", url = "https://wiki.goodt.me/x/RAOrCg")
    @Tag("TEST-128.2")
    @TmsLink("61654")
    @Tag(SCHED9)
    public void copyShiftToShiftForOneEmp() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = orgUnit.getId();
        PresetClass.changeOrSetMathParamValue(omId, MathParameterValues.ACCESS_TO_OVERWORK, true, true);
        EmployeePosition emp = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, false);
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        Shift firstShift = PresetClass.defaultShiftPreset(emp);
        Shift secondShift = PresetClass.defaultShiftPreset(emp, firstShift.getDateTimeInterval().getStartDate());
        goToSchedule(orgUnit);
        transferOneShift(emp, emp, firstShift.getDateTimeInterval().getStartDateTime(), secondShift.getDateTimeInterval().getStartDateTime(), scheduleWorker);
        clickDuplicateShiftButton();
        assertDuplicateShift(emp, firstShift, emp, secondShift.getDateTimeInterval().getStartDate(), scheduleWorker);
    }

    @Test(groups = {"TEST-129", SHIFTS, G1, SCHED9,
            "@Before disable check of worked roster before adding shift",
            "@Before disable mandatory comments when deleting worked shift"},
            description = "Массовое удаление смен")
    @Link(name = "Статья: \"Расписание\"", url = "https://wiki.goodt.me/x/RAOrCg")
    @Tag("TEST-129")
    @TmsLink("61653")
    @Tag(SCHED9)
    public void massDeleteShifts() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = orgUnit.getId();
        PresetClass.changeOrSetMathParamValue(omId, MathParameterValues.ACCESS_TO_OVERWORK, true, true);
        EmployeePosition emp = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        Shift[] shifts = PresetClass.massShiftPresetCheckForEmployee(emp);
        goToSchedule(orgUnit);
        selectTwoShift(emp, shifts, scheduleWorker);
        deleteMassShift();
        assertMassDeleteShift(emp, shifts, scheduleWorker);
    }

    @Ignore("Перенос в api")
    @Test(groups = {"TEST-130.1", SHIFTS, G1, SCHED9,
            "@Before disable check of worked roster before adding shift",
            "@Before disable start time check for worked shifts", "@Before disable all shift comments"},
            description = "Массовое копирование смен на пустые ячейки другого сотрудника")
    @Link(name = "Статья: \"Расписание\"", url = "https://wiki.goodt.me/x/RAOrCg")
    @Tag("TEST-130.1")
    @TmsLink("61652")
    @Tag(SCHED9)
    public void massDuplicateShifts() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitsForShifts(true, true, false, false, false);
        int omId = orgUnit.getId();
        ImmutablePair<EmployeePosition, EmployeePosition> twoEmployee = EmployeePositionRepository.getTwoRandomEmployeeWithCheckByApi(omId);
        PresetClass.changeOrSetMathParamValue(omId, MathParameterValues.ACCESS_TO_OVERWORK, true, true);
        EmployeePosition firstEmp = twoEmployee.left;
        EmployeePosition secondEmp = twoEmployee.right;
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        Shift[] shifts = PresetClass.massShiftPresetCheckForEmployee(firstEmp);
        ImmutablePair<LocalDate, LocalDate> dates = PresetClass.twoFreeDaysChecker(secondEmp, ShiftTimePosition.DEFAULT);
        goToSchedule(orgUnit);
        selectTwoShift(firstEmp, shifts, scheduleWorker);
        batchCopyShift(secondEmp, scheduleWorker, dates.getLeft(), false);
        assertMassDuplicateShifts(firstEmp, shifts, secondEmp, dates, scheduleWorker);
    }

    @Test(groups = {"TEST-130.2"},
            description = "Массовое копирование смен на занятые ячейки другого сотрудника")
    public void massDuplicateOnAnotherShifts() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitsForShifts(true, true, false, false, false);
        int omId = orgUnit.getId();
        ImmutablePair<EmployeePosition, EmployeePosition> twoEmployee = EmployeePositionRepository.getTwoRandomEmployeeWithCheckByApi(omId);
        EmployeePosition firstEmp = twoEmployee.left;
        EmployeePosition secondEmp = twoEmployee.right;
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        Shift[] shifts = PresetClass.massShiftPresetCheckForEmployee(firstEmp);
        Shift[] anotherShifts = PresetClass.massShiftPresetCheckForEmployee(secondEmp);
        goToSchedule(orgUnit);
        selectTwoShift(firstEmp, shifts, scheduleWorker);
        ImmutablePair<LocalDate, LocalDate> dates = new ImmutablePair<>(anotherShifts[0].getDateTimeInterval().getStartDate(),
                                                                        anotherShifts[1].getDateTimeInterval().getStartDate());
        batchCopyShift(secondEmp, scheduleWorker, dates.getLeft(), false);
        assertMassDuplicateShifts(firstEmp, shifts, secondEmp, dates, scheduleWorker);
    }

    @Ignore("Перенос в api")
    @Test(groups = {"ABCHR2885-1", SHIFTS, G1, SCHED9,
            "@Before disable check of worked roster before adding shift",
            "@Before disable start time check for worked shifts", "@Before disable all shift comments"},
            description = "Массовое копирование смен на свободные ячейки одного сотрудника")
    @Link(name = "Статья: \"Расписание\"", url = "https://wiki.goodt.me/x/RAOrCg")
    @Tag("ABCHR2885-1")
    @Tag(SCHED9)
    public void massDuplicateShiftsOneEmployee() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = orgUnit.getId();
        PresetClass.changeOrSetMathParamValue(omId, MathParameterValues.ACCESS_TO_OVERWORK, true, true);
        EmployeePosition employee = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, false);
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        Shift[] shifts = PresetClass.massShiftPresetCheckForEmployee(employee);
        ImmutablePair<LocalDate, LocalDate> dates = PresetClass.twoFreeDaysChecker(employee, ShiftTimePosition.DEFAULT,
                                                                                   shifts[0].getDateTimeInterval().getStartDate(), shifts[1].getDateTimeInterval().getStartDate());
        goToSchedule(orgUnit);
        selectTwoShift(employee, shifts, scheduleWorker);
        batchCopyShift(employee, scheduleWorker, dates.getLeft(), false);
        assertMassDuplicateShifts(employee, shifts, employee, dates, scheduleWorker);
    }

    @Ignore("Пересено в api")
    @Test(groups = {"ABCHR2885-2", SHIFTS, G1, SCHED9,
            "@Before disable check of worked roster before adding shift",
            "@Before disable start time check for worked shifts"},
            description = "Массовое копирование смен на занятые ячейки одного сотрудника")
    @Link(name = "Статья: \"Расписание\"", url = "https://wiki.goodt.me/x/RAOrCg")
    @Tag("ABCHR2885-2")
    @Tag(SCHED9)
    public void massDuplicateOnAnotherShiftsOneEmployee() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = orgUnit.getId();
        PresetClass.changeOrSetMathParamValue(omId, MathParameterValues.ACCESS_TO_OVERWORK, true, true);
        EmployeePosition employee = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, false);
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        Shift[] shifts = PresetClass.massShiftPresetCheckForEmployee(employee);
        Shift[] anotherShifts = PresetClass.massShiftPresetCheckForEmployee(employee, shifts);
        goToSchedule(orgUnit);
        selectTwoShift(employee, shifts, scheduleWorker);
        ImmutablePair<LocalDate, LocalDate> dates = new ImmutablePair<>(anotherShifts[0].getDateTimeInterval().getStartDate(),
                                                                        anotherShifts[1].getDateTimeInterval().getStartDate());
        batchCopyShift(employee, scheduleWorker, dates.getLeft(), false);
        assertMassDuplicateShifts(employee, shifts, employee, dates, scheduleWorker);
    }

    @Ignore("Перенос в api")
    @Test(groups = {"TEST-131.1", SHIFTS, G1, SCHED9,
            "@Before disable check of worked roster before adding shift",
            "@Before disable start time check for worked shifts", "@Before disable all shift comments"},
            description = "Массовый перенос смен на свободные ячейки другого сотрудника")
    @Link(name = "Статья: \"Расписание\"", url = "https://wiki.goodt.me/x/RAOrCg")
    @TmsLink("61651")
    @Tag("TEST-131.1")
    @Tag(SCHED9)
    public void massTransferShifts() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitsForShifts(true, true, false, false, false);
        int omId = orgUnit.getId();
        ImmutablePair<EmployeePosition, EmployeePosition> twoEmployee = EmployeePositionRepository.getTwoRandomEmployeeWithCheckByApi(omId);
        PresetClass.changeOrSetMathParamValue(omId, MathParameterValues.ACCESS_TO_OVERWORK, true, true);
        EmployeePosition firstEmp = twoEmployee.left;
        EmployeePosition secondEmp = twoEmployee.right;
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        Shift[] shifts = PresetClass.massShiftPresetCheckForEmployee(firstEmp);
        LocalDate[] dates = new LocalDate[]
                {shifts[0].getDateTimeInterval().getStartDate(), shifts[1].getDateTimeInterval().getStartDate()};
        PresetClass.makeClearDate(secondEmp, dates[0], dates[1]);
        goToSchedule(orgUnit);
        selectTwoShift(firstEmp, shifts, scheduleWorker);
        batchTransferShift(secondEmp, scheduleWorker, dates[0], false);
        assertMassManipulateShift(firstEmp, secondEmp, shifts, dates);
        assertMassTransferEmptyShifts(firstEmp, shifts, scheduleWorker);
    }

    @Ignore("Перенос в api")
    @Test(groups = {"TEST-131.2", SHIFTS, G1, SCHED9,
            "@Before disable check of worked roster before adding shift"},
            description = "Массовый перенос смен на занятые ячейки другого сотрудника")
    @Link(name = "Статья: \"Расписание\"", url = "https://wiki.goodt.me/x/RAOrCg")
    @TmsLink("61651")
    @Tag("TEST-131.2")
    @Tag(SCHED9)
    public void massTransferToAnotherShifts() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitsForShifts(true, true, false, false, false);
        int omId = orgUnit.getId();
        PresetClass.changeOrSetMathParamValue(omId, MathParameterValues.ACCESS_TO_OVERWORK, true, true);
        ImmutablePair<EmployeePosition, EmployeePosition> twoEmployee = EmployeePositionRepository.getTwoRandomEmployeeWithCheckByApi(omId);
        EmployeePosition firstEmp = twoEmployee.left;
        EmployeePosition secondEmp = twoEmployee.right;
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        Shift[] shifts = PresetClass.massShiftPresetCheckForEmployee(firstEmp);
        LocalDate[] dates = new LocalDate[]{shifts[0].getDateTimeInterval().getStartDate(),
                shifts[1].getDateTimeInterval().getStartDate()};
        PresetClass.massShiftPresetAtSameDays(secondEmp, dates);
        goToSchedule(orgUnit);
        selectTwoShift(firstEmp, shifts, scheduleWorker);
        batchTransferShift(secondEmp, scheduleWorker, dates[0], false);
        assertMassManipulateShift(firstEmp, secondEmp, shifts, dates);
    }

    @Test(groups = {"TEST-93"}, description = "Корректировка времени начала и окончания работ")
    public void adjustStartAndEndWorkTimes() {
        OrgUnit unit = OrgUnitRepository.getRandomAvailableOrgUnit();
        goToSchedule(unit);
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.SUBDIVISION_PROPERTIES);
        clickOnChevronButton(OmInfoName.SCHEDULE);
        clickOnThreeDotsButton();
        int orderNumber = determineActiveScheduleNumber();
        openScheduleSelectionMenu();
        String scheduleId = determineActiveScheduleId(orderNumber);
        int dayId = PresetClass.getAnyDayWithType(scheduleId, Days.DAY);
        exitScheduleSelectionMenu();
        refreshScheduleUI();
        clickOnThreeDotsButton();
        clickOnEditScheduleButton();
        LocalTime startTime = LocalTime.of(new Random().nextInt(14), 20);
        LocalTime endTime = startTime.plusHours(2);
        changeDayStartTime(startTime.toString(), dayId);
        changeDayEndTime(endTime.toString(), dayId);
        clickOnEditionScheduleChangeButton();
        switchDayTimeCheck(dayId, scheduleId, startTime.toString(), endTime.toString());
    }

    @Test(groups = {"TP-2", "TEST-38"},
            description = "Публикация графика при отсутствии расчета смен")
    public void publicationScheduleWithoutShifts() {
        OrgUnit unit = OrgUnitRepository.getRandomAvailableOrgUnit();
        goToSchedule(unit);
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.PUBLICATION);
        LocalDate date = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.RANDOM, LocalDateTools.FIRST).plusYears(new Random().nextInt(4) + 1);
        pickDateInCalendar(date);
        clickPublicationShiftsCalendarButton();
        publishButtonClick();
        assertNotPublish();
    }

    @Test(groups = {"TP-4", "TEST-38"},
            description = "Отмена выбора периода при публикации смен")
    public void deselectingPeriodOfShiftsPublication() {
        OrgUnit unit = OrgUnitRepository.getRandomAvailableOrgUnit();
        goToSchedule(unit);
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.PUBLICATION);
        String periodBefore = sb.formPublishForm().datePeriodField().getAttribute(VALUE);
        clickPublicationShiftsCalendarButton();
        DatePicker dp = new DatePicker(sb.datePickerForm());
        dp.pickMonth(LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.RANDOM, LocalDateTools.FIRST));
        dp.cancelButtonClick();
        String periodAfter = sb.formPublishForm().datePeriodField().getAttribute(VALUE);
        assertDeselectionPublicationPeriod(periodBefore, periodAfter);
        publishButtonClick();
        closePublicationForm();
    }

    @Test(groups = {"TP-5", "TEST-38"}, description = "Выбор года при публикации графика смен")
    public void selectingYearOfShiftsPublication() {
        OrgUnit unit = OrgUnitRepository.getRandomAvailableOrgUnit();
        goToSchedule(unit);
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.PUBLICATION);
        clickPublicationShiftsCalendarButton();
        DatePicker dp = new DatePicker(sb.datePickerForm());
        dp.rightYearSwitch();
        dp.leftYearSwitch();
        dp.leftYearSwitch();
    }

    @Test(groups = {"TP-6", "TEST-38"}, description = "Закрытие окна публикации смен")
    public void goToPublication() {
        OrgUnit unit = OrgUnitRepository.getRandomAvailableOrgUnit();
        goToSchedule(unit);
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.PUBLICATION);
        closePublicationForm();
    }

    @Ignore(NOT_ACTUAL)
    @Test(groups = {"TK2686",
            "@Before forbid shift comments"},
            description = "Невозможность добавления комментария к смене в табеле при выключенной настройке",
            expectedExceptions = AssertionError.class, expectedExceptionsMessageRegExp = ANY + NO_COMMENT_FIELD + ANY)
    @Severity(SeverityLevel.MINOR)
    @Link(name = "Статья: \" 2686_Администратор может добавлять и скрывать комментарии к сменам\"",
            url = "https://wiki.goodt.me/x/oQPFCw")
    @Owner(SCHASTLIVAYA)
    @TmsLink("60246")
    @Tag("TK2686-7")
    @Tag(LIST2)
    public void cannotAddCommentWhenCommentsAreDisabled() {
        checkFirstDayOfMonth();
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitWithWorkedRosters(true);
        int omId = unit.getId();
        EmployeePosition employeePosition = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        Shift shift = PresetClass.defaultShiftPreset(employeePosition, ShiftTimePosition.PAST);
        goToSchedule(unit);
        clickOnTargetShift(employeePosition, shift.getDateTimeInterval().getStartDate(), new ScheduleWorker(sb));
        clickOnCommentMenu();
    }

    @Test(groups = {"SP-1", "TEST-42"}, description = "Расчет при отсутствии прогноза")
    public void calculateWithoutFTE() {
        OrgUnit unit = OrgUnitRepository.getRandomAvailableOrgUnit();
        goToSchedule(unit);
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.CALCULATION_RECALCULATE_SHIFTS);
        LocalDate startDate = LocalDateTools.getFirstDate().plusYears(1);
        LocalDate endDate = LocalDateTools.getLastDate().plusYears(1);
        enterCreateScheduleDateEndOrStart(startDate, DateTypeField.START_DATE);
        enterCreateScheduleDateEndOrStart(endDate, DateTypeField.END_DATE);
        calculateButtonClick();
        checkErrorAvailability();
    }

    @Test(groups = {"SP-2", "TEST-42"}, description = "Закрытие окна \"Расчет/перерасчет смен\"")
    public void closeCalculateForm() {
        OrgUnit unit = OrgUnitRepository.getRandomAvailableOrgUnit();
        goToSchedule(unit);
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.CALCULATION_RECALCULATE_SHIFTS);
        clickCloseButton();
    }

    @Test(groups = {"TK2653-1", G2, SHIFTS, SCHED41,
            "@Before set default shift duration",
            "@Before disable check of worked roster before adding shift"},
            dataProvider = "true/false", dataProviderClass = DataProviders.class,
            description = "Редактирование смены в табеле без указания комментария")
    @Link(name = "Статья: \" 2653_Администратор может включить обязательное заполнение комментария при корректировки смены в табеле\"",
            url = "https://wiki.goodt.me/x/hwPFCw")
    @TmsLink("60233")
    @Tag("TK2653-1")
    @Tag(SCHED41)
    public void editShiftWithoutCommentWhenItRequired(boolean shiftCommentsRequired) {
        checkFirstDayOfMonth();
        PresetClass.setSystemPropertyValue(SystemProperties.FACT_SHIFT_COMMENTS_REQUIRED, shiftCommentsRequired);
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitWithWorkedRosters(true);
        int omId = unit.getId();
        PresetClass.changeOrSetMathParamValue(omId, MathParameterValues.ACCESS_TO_OVERWORK, true, true);
        PresetClass.kpiAndFteChecker(omId);
        EmployeePosition employeePosition = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        Shift shift = PresetClass.defaultShiftPreset(employeePosition, ShiftTimePosition.PAST, true);
        DateTimeInterval interval = shift.getDateTimeInterval().offsetByMinutes(-60);
        goToSchedule(unit);
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        clickOnTargetShift(employeePosition, shift.getDateTimeInterval().getStartDate(), scheduleWorker);
        enterShiftTimeStartOrEnd(interval.getStartDateTime().toLocalTime(), TimeTypeField.START_TIME);
        enterShiftTimeStartOrEnd(interval.getEndDateTime().toLocalTime(), TimeTypeField.END_TIME);
        if (shiftCommentsRequired) {
            Assert.assertThrows(WaitUntilException.class, this::clickEditShiftButton);
            assertErrorMessageDisplayed();
        } else {
            clickEditShiftButton();
            assertEditShift(employeePosition, interval, scheduleWorker);
        }
    }

    @Test(groups = {"TK2043", "TK2043-2", G0, SCHED11,
            "@Before disable two-factor auth",
            "@Before turn off druid"},
            description = "Расчет смен",
            dataProvider = "roles 1, 4", dataProviderClass = DataProviders.class)
    @Link(name = "Статья: \"2043 Применение роли в системе блок \"Расписание\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=197460473")
    @TmsLink("61615")
    @Severity(SeverityLevel.CRITICAL)
    @Tag(SCHED11)
    @Tag("TK2043-2")
    public void shiftCalculation(Role role) {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        PresetClass.publishGraphPreset(GraphStatus.PUBLISH, unit);
        new RoleWithCookies(sb.getWrappedDriver(), role, unit).getPageWithoutWait(SECTION);
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.CALCULATION_RECALCULATE_SHIFTS);
        RosterRepository.getRosters(omId).stream().findAny().orElseThrow(() -> new AssertionError("У подразделения нет ростеров"));
        List<Integer> previousIds = RosterRepository.getRosters(omId).stream().map(Roster::getId).collect(Collectors.toList());
        calculateButtonClick();
        clickOnCloseButton();
        assertScheduleCalculation(previousIds, omId);
    }

    @Test(groups = {"TK2043", G0, SCHED11,
            "@Before disable two-factor auth"},
            description = "Создание новой версии графика при расчете/перерасчете без доступа",
            dataProvider = "roles 3, 5-7", dataProviderClass = DataProviders.class,
            expectedExceptionsMessageRegExp = ANY + OPTION_NOT_DISPLAYED + ANY)
    @Link(name = "Статья: \"2043 Применение роли в системе блок \"Расписание\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=197460473")
    @TmsLink("61615")
    @Severity(SeverityLevel.CRITICAL)
    @Tag(SCHED11)
    @Tag("TK2043-2")
    public void shiftCalculationWithoutPermissions(Role role) {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        goToScheduleAsUser(role, unit);
        threeDotsMenuClick();
        checkFunctionInMenu(VariantsOfFunctions.CALCULATION_RECALCULATE_SHIFTS);
    }

    @Test(groups = {"TK2043", G0, SCHED11,
            "@Before disable two-factor auth"},
            description = "Создание новой версии графика при расчете/перерасчете без доступа",
            expectedExceptions = WaitUntilException.class, expectedExceptionsMessageRegExp = ANY + FAILED_TO_GET_PAGE + ANY)
    @Link(name = "Статья: \"2043 Применение роли в системе блок \"Расписание\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=197460473")
    @TmsLink("61615")
    @Severity(SeverityLevel.CRITICAL)
    @Tag(SCHED11)
    @Tag("TK2043-2")
    public void shiftCalculationSecondRole() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        goToScheduleAsUser(Role.SECOND, unit);
    }

    @Test(groups = {"ТК2043-3", G1, SCHED12,
            "@Before disable two-factor auth", "@Before disable pre-publication checks"},
            description = "Публикация графика (пользователь с разрешениями)",
            dataProvider = "roles 1, 5", dataProviderClass = DataProviders.class)
    @Link(name = "Статья: \"2043 Применение роли в системе блок \"Расписание\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=197460473")
    @Severity(SeverityLevel.NORMAL)
    @Tag("ТК2043-3")
    @Tag(SCHED12)
    public void schedulePublicationWithPermissions(Role role) {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        disablePublishSystemPropertiesIfNoLimitIsSet(unit);
        int omId = unit.getId();
        nonPublishCheck(omId);
        goToScheduleAsUser(role, unit);
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.PUBLICATION);
        publishButtonClick();
        closePublicationForm();
        refreshPage();
        listOfSchedulesClick();
        ZonedDateTime dateTime = ZonedDateTime.now();
        publicationAssert(dateTime, unit);
    }

    @Test(groups = {"ТК2043-3", G1, SCHED12,
            "@Before disable two-factor auth"},
            description = "Публикация графика без доступа", dataProvider = "roles 3, 4, 6-8", dataProviderClass = DataProviders.class,
            expectedExceptions = AssertionError.class, expectedExceptionsMessageRegExp = ANY + OPTION_NOT_DISPLAYED + ANY)
    @Link(name = "Статья: \"2043 Применение роли в системе блок \"Расписание\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=197460473")
    @Severity(SeverityLevel.NORMAL)
    @Tag("ТК2043-3")
    @Tag(SCHED12)
    public void schedulePublicationWithoutPermissions(Role role) {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        goToScheduleAsUser(role, unit);
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.PUBLICATION);
    }

    @Test(groups = {"ТК2043", G1, SCHED23,
            "@Before disable two-factor auth"},
            description = "Выгрузка смен XLSX (пользователь с разрешениями)",
            dataProvider = "roles 1, 6", dataProviderClass = DataProviders.class)
    @Link(name = "Статья: \"2043 Применение роли в системе блок \"Расписание\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=197460473")
    @TmsLink("85831")
    @Severity(SeverityLevel.NORMAL)
    @Tag("ТК2043-4")
    @Tag(SCHED23)
    public void downloadXLSXWithPermissions(Role role) {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        PresetClass.checkAndMakePublicationRoster(omId);
        goToScheduleAsUser(role, unit);
        threeDotsMenuClick();
        String rosterId = String.valueOf(RosterRepository.getActiveRosterThisMonth(omId).getId());
        chooseFunction(VariantsOfFunctions.DOWNLOAD_XLSX);
        FileDownloadCheckerForScheduleBoard checker = new FileDownloadCheckerForScheduleBoard(role,
                                                                                              TypeOfFiles.XLSX, rosterId);
        assetForRightDownloading(TypeOfAcceptContent.PDF_XLSX, checker, role);
    }

    @Test(groups = {"ТК2043", G1, SCHED23,
            "@Before disable two-factor auth"},
            description = "Выгрузка смен XLSX без доступа", dataProvider = "roles 3-5, 7", dataProviderClass = DataProviders.class,
            expectedExceptionsMessageRegExp = ANY + OPTION_NOT_DISPLAYED + ANY)
    @Link(name = "Статья: \"2043 Применение роли в системе блок \"Расписание\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=197460473")
    @TmsLink("85831")
    @Severity(SeverityLevel.NORMAL)
    @Tag("ТК2043-4")
    @Tag(SCHED23)
    public void downloadXLSXWithoutPermissions(Role role) {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        PresetClass.checkAndMakePublicationRoster(omId);
        goToScheduleAsUser(role, unit);
        threeDotsMenuClick();
        checkFunctionInMenu(VariantsOfFunctions.DOWNLOAD_XLSX);
    }

    @Test(groups = {"TK2043-5-1",
            "@Before disable two-factor auth"},
            description = "Управление пожеланиями - подтвердить. 1 роль")
    @Link(name = "Статья: \"2043 Применение роли в системе блок \"Расписание\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=197460473")
    public void acceptRequestFirstRole() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        ScheduleWorker sWorker = new ScheduleWorker(sb);
        ScheduleRequest request = PresetClass.preSetNotRepeatRequestCheck(ScheduleRequestStatus.CANCELED, omId);
        goToScheduleAsUser(Role.FIRST, unit);
        clickOnRequestBlock(request, sWorker, unit);
        listOfRequestsThreeDotsButtonClick();
        selectAction(RequestAction.ACCEPT, false);
        assertRequestActionChange(request, ScheduleRequestStatus.APPROVED, omId);
    }

    @Test(groups = {"TK2043-5-7",
            "@Before disable two-factor auth"},
            description = "Управление пожеланиями - подтвердить. 7 роль")
    @Link(name = "Статья: \"2043 Применение роли в системе блок \"Расписание\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=197460473")
    public void acceptRequestSeventhRole() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        ScheduleWorker sWorker = new ScheduleWorker(sb);
        ScheduleRequest request = PresetClass.preSetNotRepeatRequestCheck(ScheduleRequestStatus.CANCELED, omId);
        goToScheduleAsUser(Role.SEVENTH, unit);
        clickOnRequestBlock(request, sWorker, unit);
        listOfRequestsThreeDotsButtonClick();
        selectAction(RequestAction.ACCEPT, false);
        assertRequestActionChange(request, ScheduleRequestStatus.APPROVED, omId);
    }

    @Test(groups = {"TK2043-6-1",
            "@Before disable two-factor auth"}, description = "Управление пожеланиями - отклонить. 1 роль")
    @Link(name = "Статья: \"2043 Применение роли в системе блок \"Расписание\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=197460473")
    public void rejectRequestFirstRole() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        ScheduleWorker sWorker = new ScheduleWorker(sb);
        ScheduleRequest request = PresetClass.preSetNotRepeatRequestCheck(ScheduleRequestStatus.APPROVED, omId);
        goToScheduleAsUser(Role.FIRST, unit);
        clickOnRequestBlock(request, sWorker, unit);
        listOfRequestsThreeDotsButtonClick();
        selectAction(RequestAction.REJECT, false);
        assertRequestActionChange(request, ScheduleRequestStatus.CANCELED, omId);
    }

    @Test(groups = {"TK2043-6-7",
            "@Before disable two-factor auth"},
            description = "Управление пожеланиями - отклонить. 7 роль")
    @Link(name = "Статья: \"2043 Применение роли в системе блок \"Расписание\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=197460473")
    public void rejectRequestSeventhRole() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        ScheduleWorker sWorker = new ScheduleWorker(sb);
        ScheduleRequest request = PresetClass.preSetNotRepeatRequestCheck(ScheduleRequestStatus.APPROVED, omId);
        goToScheduleAsUser(Role.SEVENTH, unit);
        clickOnRequestBlock(request, sWorker, unit);
        listOfRequestsThreeDotsButtonClick();
        selectAction(RequestAction.REJECT, false);
        assertRequestActionChange(request, ScheduleRequestStatus.CANCELED, omId);
    }

    @Test(groups = {"TK2043-7-1",
            "@Before disable two-factor auth"},
            description = "Управление пожеланиями - удалить. 1 роль")
    @Link(name = "Статья: \"2043 Применение роли в системе блок \"Расписание\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=197460473")
    public void deleteRequestFirstRole() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        ScheduleRequest request = PresetClass.preSetNotRepeatRequestCheck(ScheduleRequestStatus.APPROVED, omId);
        goToScheduleAsUser(Role.FIRST, unit);
        clickOnRequestBlock(request, scheduleWorker, unit);
        listOfRequestsThreeDotsButtonClick();
        selectAction(RequestAction.DELETE, false);
        assertRequestDeleting(request, scheduleWorker, unit);
    }

    @Test(groups = {"TK2043-7-7",
            "@Before disable two-factor auth"},
            description = "Управление пожеланиями - удалить. 7 роль")
    @Link(name = "Статья: \"2043 Применение роли в системе блок \"Расписание\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=197460473")
    public void deleteRequestSeventhRole() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        ScheduleRequest request = PresetClass.preSetNotRepeatRequestCheck(ScheduleRequestStatus.APPROVED, omId);
        goToScheduleAsUser(Role.SEVENTH, unit);
        clickOnRequestBlock(request, scheduleWorker, unit);
        listOfRequestsThreeDotsButtonClick();
        selectAction(RequestAction.DELETE, false);
        assertRequestDeleting(request, scheduleWorker, unit);
    }

    @Test(groups = {"TK2043-7",
            "@Before disable two-factor auth", "@Before disable merged view for planned and actual shifts"},
            dataProvider = "roles 3-6", dataProviderClass = DataProviders.class,
            description = "Управление пожеланиями без доступа на редактирование",
            expectedExceptions = AssertionError.class, expectedExceptionsMessageRegExp = ANY + THREE_DOTS_BUTTON_NOT_DISPLAYED_ON_SHIFT_EDIT_SCREEN + ANY)
    @Link(name = "Статья: \"2043 Применение роли в системе блок \"Расписание\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=197460473")
    public void notAvailableRequest(Role role) {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        ScheduleRequest request = PresetClass.preSetNotRepeatRequestCheck(ScheduleRequestStatus.APPROVED, omId);
        goToScheduleAsUser(role, unit);
        clickOnRequestBlock(request, scheduleWorker, unit);
        listOfRequestsThreeDotsButtonClick();
    }

    @Test(groups = {"TI-7.1",
            "@Before disable two-factor auth"},
            description = "Переход на вкладку \"Расписание\" через меню")
    public void goToSbViaMenu() {
        new RoleWithCookies(sb.getWrappedDriver(), Role.ADMIN).getPage(Section.WELCOME);
        clickSectionSelectionMenuOnPageHeader();
        clickOnScheduleSectionButton();
        checkGoToSchedulePage();
    }

    @Test(groups = {"TI-8.1",
            "@Before disable two-factor auth"},
            description = " Выбор подразделения на вкладке \"Расписание\" через раскрывающийся список")
    public void goToOrgUnitBySelectFromList() {
        new RoleWithCookies(sb.getWrappedDriver(), Role.ADMIN).getPage(Section.WELCOME);
        clickSectionSelectionMenuOnPageHeader();
        clickOnScheduleSectionButton();
        systemSleep(6); //тест неактуален
        clickOnSelectStoreButton();
        String omName = getRandomOrgUnitFromList();
        selectStoreFromList(omName);
        checkTransitionToOrgUnit(omName);
    }

    @Test(groups = {"TI-8.2",
            "@Before disable two-factor auth"},
            description = "Выбор подразделения на вкладке \"Расписание\"")
    public void goToOrgUnitByEnteringName() {
        new RoleWithCookies(sb.getWrappedDriver(), Role.ADMIN).getPage(Section.WELCOME);
        clickSectionSelectionMenuOnPageHeader();
        clickOnScheduleSectionButton();
        systemSleep(6); //тест неактуален
        clickOnSelectStoreButton();
        String omName = OrgUnitRepository.getRandomAvailableOrgUnit().getName();
        enterOmName(omName);
        selectStoreFromList(omName);
        checkTransitionToOrgUnit(omName);
    }

    @Test(groups = "TI-9", description = "Проверка отображения отметки")
    public void integrationCheck() {
        EmployeePosition employeePosition = PresetClass.recognitionPreset();
        OrgUnit orgUnit = employeePosition.getOrgUnit();
        goToOmAndSyncRecord(orgUnit.getId(), orgUnit.getName());
        int recordIndex = getCurrentRecordIndex(employeePosition);
        LocalTime time = LocalTime.now().withSecond(0).withNano(0);
        switchScope(ScopeType.DAY);
        hoverRecord(employeePosition.getEmployee().getShortName(), recordIndex);
        checkRecordTime(time);
    }

    @Test(groups = {"ABCHR2777-5", G1, SCHED11, POCHTA},
            description = "Расчет смен для сотрудника в декрете")
    @Link(name = "Статья: \"2777_Ведение сотрудника в Декрете в WFM\"", url = "https://wiki.goodt.me/x/XAQtD")
    @TmsLink("60282")
    @Owner(SCHASTLIVAYA)
    @Tag("ABCHR2777-5")
    @Tag(SCHED11)
    public void calculateShiftsForEmployeeOnMaternityLeave() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        PresetClass.changeOrSetMathParamValue(ep.getPosition().getPositionGroupId(), MathParameterValues.EXCLUDE_FROM_CALCULATION, false, true, omId);
        Shift shift = PresetClass.defaultShiftPreset(ep, LocalDateTools.getFirstDate(), ShiftTimePosition.ALL_MONTH_WITHOUT_FIRST_AND_LAST);
        LocalDate date = shift.getDateTimeInterval().getStartDate();
        // Смены добавляются на случай, если у сотрудника их совсем нет, чтобы было что проверять.
        PresetClass.presetForMakeShiftDate(ep, date.plusDays(1), false, ShiftTimePosition.ALLMONTH);
        PresetClass.assignMaternityLeaveStatus(ep, date, date.plusMonths(1));

        goToSchedule(unit);
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.CALCULATION_RECALCULATE_SHIFTS);
        enterCreateScheduleDateEndOrStart(LocalDateTools.getFirstDate(), DateTypeField.START_DATE);
        enterCreateScheduleDateEndOrStart(LocalDateTools.getLastDate(), DateTypeField.END_DATE);
        RosterRepository.getRosters(omId).stream().findAny().orElseThrow(() -> new AssertionError("У подразделения нет ростеров"));
        List<Integer> previousIds = RosterRepository.getRosters(omId).stream().map(Roster::getId).collect(Collectors.toList());
        List<Shift> shiftsBefore = ShiftRepository.getShiftsBeforeDate(ep, date);
        calculateButtonClick();
        clickOnCloseButton();
        assertScheduleCalculation(previousIds, omId);
        assertNoShiftsAfterCertainDate(ep, date, shiftsBefore);
    }

    @Test(groups = {"TK2779-1", "TEST-1060"}, description = "Проверка даты начала при создании смены")
    public void checkingStartDateWhenCreatingShift() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        EmployeePosition employeePosition = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, false);
        LocalDate firstDayMonth = LocalDate.now().withDayOfMonth(1);
        PresetClass.makeClearDate(employeePosition, firstDayMonth);
        goToSchedule(unit);
        clickOnEmptyCell(employeePosition, firstDayMonth, scheduleWorker);
        clickOnPlusCellOnGraph();
        enterShiftDateStartOrEnd(firstDayMonth.minusDays(1), DateTypeField.START_DATE);
        assertCantSendWrongValue(firstDayMonth);
    }

    @Test(groups = {"TK2779-2", "TEST-1060",
            "@Before set default shift duration"},
            description = "Проверка даты начала при редактировании смены")
    public void checkingStartDateWhenEditShift() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        EmployeePosition employeePosition = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, false);
        Shift shift = ShiftRepository.getFirstDayMonthShift(employeePosition);
        goToSchedule(unit);
        clickOnTargetShift(employeePosition, shift.getDateTimeInterval().getStartDate(), scheduleWorker);
        enterShiftDateStartOrEnd(LocalDate.now().withDayOfMonth(1).minusDays(1), DateTypeField.START_DATE);
        LocalTime start = LocalTime.of(23, 0, 0);
        enterShiftTimeStartOrEnd(start, TimeTypeField.START_TIME);
        checkingForComments("Другое");
        clickEditShiftButton();
        assertErrorEditShift(shift);
    }

    @Test(groups = {"ABCHR2781-1", G1, SCHED21,
            "@Before disable pre-publication checks",
            "@Before don't show button to publish roster"},
            description = "Отклонение графика, отправленного на утверждение, с указанием комментария")
    @Link(name = "Статья: \"2781_Сохранять комментарий, введенный при отклонении графика с публикации\"",
            url = "https://wiki.goodt.me/pages/viewpage.action?pageId=204276677")
    @Tag("ABCHR2781-1")
    @Tag(SCHED21)
    public void rejectScheduleSentForApprovalWithComment() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitWithActiveRoster().left;
        int omId = unit.getId();
        disablePublishSystemPropertiesIfNoLimitIsSet(unit);
        Roster roster = PresetClass.nonPublishAndApproveChecker(omId);
        if (unit.getChief(getServerDate()) == null) {
            PresetClass.appointEmployeeAChief(unit);
        }
        PresetClass.kpiAndFteChecker(omId);
        goToSchedule(unit);
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.PUBLICATION);
        publishRejectButtonClick();
        String testComment = RandomStringUtils.randomAlphabetic(10);
        sendCommentaryInRejectRoster(testComment);
        saveCommentaryInRejectRoster();
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.COMMENTS_TO_THE_VERSIONS_OF_THE_CALCULATION);
        assertAddRejectComment(roster, testComment);
    }

    @Test(groups = {"ABCHR2781-2", "TEST-1078"},
            description = "Отмена отклонения графика, отправленного на утверждение, с указанием комментария")
    public void cancelRejectScheduleSentForApprovalWithComment() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        Roster roster = PresetClass.nonPublishAndApproveChecker(omId);
        PresetClass.kpiAndFteChecker(omId);
        goToSchedule(unit);
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.PUBLICATION);
        publishRejectButtonClick();
        String testComment = RandomStringUtils.randomAlphabetic(10);
        sendCommentaryInRejectRoster(testComment);
        cancelCommentaryInRejectRoster();
        assertCancelRejectComment(roster);
    }

    @Test(groups = {"ABCHR2781-3", G1, SCHED21,
            "@Before disable pre-publication checks",
            "@Before don't show button to publish roster"
    },
            description = "Отклонение графика, отправленного на утверждение, без указания комментария")
    @Link(name = "Статья: \"2781_Сохранять комментарий, введенный при отклонении графика с публикации\"",
            url = "https://wiki.goodt.me/pages/viewpage.action?pageId=204276677")
    @TmsLink("60711")
    @Tag("ABCHR2781-3")
    @Tag(SCHED21)
    public void rejectScheduleSentForApprovalWithOutComment() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitWithActiveRoster().left;
        int omId = unit.getId();
        disablePublishSystemPropertiesIfNoLimitIsSet(unit);
        Roster roster = PresetClass.nonPublishAndApproveChecker(omId);
        PresetClass.kpiAndFteChecker(omId);
        goToSchedule(unit);
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.PUBLICATION);
        publishRejectButtonClick();
        saveCommentaryInRejectRoster();
        assertErrorInInputField(roster);
    }

    @Test(groups = {"TK2792-1", G1, SCHED8,
            "@Before disable start time check for worked shifts"},
            dataProvider = "employeeHours",
            description = "Отображение информации в поп-ап сотрудника в расписании")
    @Link(name = "Статья: \"2792_Добавить в системные настройки вывод переработок/недоработок\"", url = "https://wiki.goodt.me/x/swL8Cw")
    @TmsLink("60305")
    @Owner(SCHASTLIVAYA)
    @Tag("TK2792-1")
    @Tag(SCHED8)
    public void displayInfoInEmployeeTooltipDependingOnSettings(SystemProperties property,
                                                                List<InEmployeeWorkingHours> expectedLines,
                                                                boolean settingValue) {
        changeProperty(property, settingValue);
        if (property.equals(SystemProperties.SCHEDULE_BOARD_SHOW_SHIFT_HOURS)) {
            changeProperty(SystemProperties.SCHEDULE_BOARD_SHOW_YEAR_OVERTIME_UNDERTIME, settingValue);
        }
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitWithWorkedRosters(true, true);
        int omId = unit.getId();
        PresetClass.changeOrSetMathParamValue(omId, MathParameterValues.ACCESS_TO_OVERWORK, true, true);
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        Shift shift = null;
        if (property.equals(SystemProperties.SCHEDULE_BOARD_SHOW_RATE) && ep.getRate().equals(Double.NaN)) {
            PresetClass.setRate(ep, 1);
        }
        if (property.equals(SystemProperties.SCHEDULE_BOARD_NIGHT_HOURS_INDICATOR)) {
            changeProperty(SystemProperties.SHIFT_CHECK_IN_WORKED_ROSTER_BEFORE_CREATING, false);
            shift = PresetClass.presetForMakeShift(ep, true, ShiftTimePosition.PAST);
        }
        goToScheduleWithCheck(unit);
        hoverOnEmployeeHours(ep);
        assertDisplayInfoInEmployeeTooltip(property, settingValue, unit.getId(), expectedLines, ep, shift);
    }

    @Test(groups = {"ABCHR2939-1", "TEST-1172",
            "@Before set default shift duration"},
            description = "Создание запроса типа \"Смена в другом подразделении\" в Расписании")
    public void createShiftsOtherRequest() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = orgUnit.getId();
        EmployeePosition employeePosition = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, false);
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        LocalDate date = PresetClass.getFreeDateFromNow(employeePosition);
        goToSchedule(orgUnit);
        clickOnEmptyCell(employeePosition, date, new ScheduleWorker(sb));
        clickOnPlusCellOnGraph();
        clickOnSelectRequestTypeButton();
        ScheduleRequestType requestType = ScheduleRequestType.SHIFT_OTHER;
        selectRequestType(requestType);
        LocalTime start = LocalTime.of(10, 0, 0);
        LocalTime end = LocalTime.of(19, 0, 0);
        enterShiftTimeStartOrEnd(start, TimeTypeField.START_TIME);
        enterShiftTimeStartOrEnd(end, TimeTypeField.END_TIME);
        createShiftButtonClick();
        assertRequestAdding(employeePosition, date, requestType, scheduleWorker, orgUnit, ScheduleRequestStatus.APPROVED);
    }

    @Test(groups = {"ABCHR2939-2", "TEST-1172"},
            description = "Создание запроса типа \"Смена в другом подразделении\" в Расписании без указания времени начала и окончания смены")
    public void createShiftsOtherRequestWithOutDates() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = orgUnit.getId();
        EmployeePosition employeePosition = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, false);
        LocalDate date = PresetClass.getFreeDateFromNow(employeePosition);
        goToSchedule(orgUnit);
        clickOnEmptyCell(employeePosition, date, new ScheduleWorker(sb));
        clickOnPlusCellOnGraph();
        clickOnSelectRequestTypeButton();
        ScheduleRequestType requestType = ScheduleRequestType.SHIFT_OTHER;
        selectRequestType(requestType);
        createShiftButtonClick();
        assertErrorSaveWithoutDate(date);
    }

    @Test(groups = {"TEST-1174", "TK2705-1"}, description = "Выгрузка планового графика в формате xlsx")
    public void downloadPlannedScheduleXlsx() {
        PresetClass.setSystemPropertyValue(SystemProperties.JASPER_REPORTS_SHIFTS_PLAN_SHOW, true);
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        TypeOfFiles typeOfFiles = TypeOfFiles.XLSX;
        goToSchedule(unit);
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.DOWNLOAD_PLANNED_SCHEDULE);
        chooseDownloadFileFormat(typeOfFiles);
        pressDownloadButton();
        FileDownloadCheckerForScheduleBoard checker = new FileDownloadCheckerForScheduleBoard(Role.ADMIN, unit.getId(),
                                                                                              typeOfFiles, TypeOfReports.PLANNED_GRAPH);
        assertForDownloadingPath(checker, Role.ADMIN);
    }

    @Test(groups = {"TEST-1174", "TK2705-2"}, description = "Выгрузка планового графика в формате csv")
    public void downloadPlannedScheduleCsv() {
        PresetClass.setSystemPropertyValue(SystemProperties.JASPER_REPORTS_SHIFTS_PLAN_SHOW, true);
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        TypeOfFiles typeOfFiles = TypeOfFiles.CSV;
        goToSchedule(unit);
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.DOWNLOAD_PLANNED_SCHEDULE);
        chooseDownloadFileFormat(typeOfFiles);
        pressDownloadButton();
        FileDownloadCheckerForScheduleBoard checker = new FileDownloadCheckerForScheduleBoard(Role.ADMIN, unit.getId(),
                                                                                              typeOfFiles, TypeOfReports.PLANNED_GRAPH);
        assertForDownloadingPath(checker, Role.ADMIN);
    }

    @Test(groups = {"TEST-1174", "TK2705-3"}, description = "Выгрузка планового графика в формате pdf")
    public void downloadPlannedSchedulePdf() {
        PresetClass.setSystemPropertyValue(SystemProperties.JASPER_REPORTS_SHIFTS_PLAN_SHOW, true);
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        TypeOfFiles typeOfFiles = TypeOfFiles.PDF;
        goToSchedule(unit);
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.DOWNLOAD_PLANNED_SCHEDULE);
        chooseDownloadFileFormat(typeOfFiles);
        pressDownloadButton();
        FileDownloadCheckerForScheduleBoard checker = new FileDownloadCheckerForScheduleBoard(Role.ADMIN, unit.getId(),
                                                                                              typeOfFiles, TypeOfReports.PLANNED_GRAPH);
        assertForDownloadingPath(checker, Role.ADMIN);
    }

    @Test(groups = {"TEST-1174", "TK2705-4"}, description = "Выгрузка планового графика в формате html")
    public void downloadPlannedScheduleHtml() {
        PresetClass.setSystemPropertyValue(SystemProperties.JASPER_REPORTS_SHIFTS_PLAN_SHOW, true);
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        TypeOfFiles typeOfFiles = TypeOfFiles.HTML;
        goToSchedule(unit);
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.DOWNLOAD_PLANNED_SCHEDULE);
        chooseDownloadFileFormat(typeOfFiles);
        pressDownloadButton();
        FileDownloadCheckerForScheduleBoard checker = new FileDownloadCheckerForScheduleBoard(Role.ADMIN, unit.getId(),
                                                                                              typeOfFiles, TypeOfReports.PLANNED_GRAPH);
        assertForDownloadingPath(checker, Role.ADMIN);
    }

    @Test(groups = {"TEST-1162", "ABCHR2707-1"}, description = "Печать нормализованного графика")
    @Tag("TEST-1162")
    public void printNormalizedGraph() {
        PresetClass.setSystemPropertyValue(SystemProperties.SCHEDULE_BOARD_NORMALIZED_PRINT, true);
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        goToSchedule(unit);
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.PRINT_NORMALIZED_SHIFTS);
        String employeePositions = EmployeePositionRepository.getEmployeePositions(PositionRepository.getPositionsArray(unit.getId()))
                .stream().map(EmployeePosition::getId).sorted().map(String::valueOf).collect(Collectors.joining(","));
        FileDownloadCheckerForScheduleBoard checker = new FileDownloadCheckerForScheduleBoard(Role.ADMIN,
                                                                                              TypeOfFiles.PDF, TypeOfReports.NORMALIZED_SHIFTS, unit.getId(), employeePositions);
        assertForRightDownloadingPDF(checker);
    }

    @Test(groups = {"TEST-1246", "ABCHR3087-1"}, description = "Изменение формата отчетов jasper по умолчанию на xlsx")
    public void changeJasperDefaultReportFormatXlsx() {
        TypeOfFiles typeOfFiles = TypeOfFiles.XLSX;
        PresetClass.setSystemPropertyValue(SystemProperties.JASPER_DEFAULT_FORMAT, typeOfFiles.getFileFormat());
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        goToSchedule(unit);
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.REPORT_T_13_FORM);
        assertJasperDefaultReportFormat(typeOfFiles);
    }

    @Test(groups = {"TEST-1246", "ABCHR3087-2"}, description = "Изменение формата отчетов jasper по умолчанию на pdf")
    public void changeJasperDefaultReportFormatPdf() {
        TypeOfFiles typeOfFiles = TypeOfFiles.PDF;
        PresetClass.setSystemPropertyValue(SystemProperties.JASPER_DEFAULT_FORMAT, typeOfFiles.getFileFormat());
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        goToSchedule(unit);
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.REPORT_T_13_FORM);
        assertJasperDefaultReportFormat(typeOfFiles);
    }

    @Test(groups = {"TEST-1246", "ABCHR3087-3"}, description = "Изменение формата отчетов jasper по умолчанию на csv")
    public void changeJasperDefaultReportFormatCsv() {
        TypeOfFiles typeOfFiles = TypeOfFiles.CSV;
        PresetClass.setSystemPropertyValue(SystemProperties.JASPER_DEFAULT_FORMAT, typeOfFiles.getFileFormat());
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        goToSchedule(unit);
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.REPORT_T_13_FORM);
        assertJasperDefaultReportFormat(typeOfFiles);
    }

    @Test(groups = {"TEST-1246", "ABCHR3087-4"}, description = "Изменение формата отчетов jasper по умолчанию на html")
    public void changeJasperDefaultReportFormatHtml() {
        TypeOfFiles typeOfFiles = TypeOfFiles.HTML;
        PresetClass.setSystemPropertyValue(SystemProperties.JASPER_DEFAULT_FORMAT, typeOfFiles.getFileFormat());
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        goToSchedule(unit);
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.REPORT_T_13_FORM);
        assertJasperDefaultReportFormat(typeOfFiles);
    }

    @Test(groups = {"ABCHR4183-1", G1, SHIFTS, SCHED41},
            description = "Добавление комментария к плановой смене при включенной настройке")
    @Link(name = "Статья: \"4183_Комментарий к плановой смене\"", url = "https://wiki.goodt.me/x/8Qb6D")
    @TmsLink("60306")
    @Tag(SCHED41)
    @Tag("ABCHR4183-1")
    public void addCommentToPlanShift() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        PresetClass.nonPublishChecker(omId);
        ShiftTimePosition timePosition = ShiftTimePosition.FUTURE;
        List<Shift> shifts = ShiftRepository.getShiftsForRoster(RosterRepository.getActiveRosterThisMonth(omId).getId(), timePosition.getShiftsDateInterval());
        Assert.assertFalse(shifts.isEmpty(), "В выбранном подразделении нет смен в активном ростере");
        Shift shift = getRandomFromList(shifts);
        EmployeePosition ep = EmployeePositionRepository.getEmployeePositionById(shift.getEmployeePositionId());

        goToSchedule(unit);
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        clickOnTargetShift(ep, shift.getDateTimeInterval().getStartDate(), scheduleWorker);

        String comment = RandomStringUtils.randomAlphabetic(8);
        inputComment(comment);
        clickEditShiftButton();
        clickOnTargetShift(ep, shift.getDateTimeInterval().getStartDate(), scheduleWorker);
        assertShiftCommentAdding(comment, shift);
    }

    @Test(groups = {"ABCHR4189-1", SHIFTS, G1, SCHED9,
            "@Before disable equality check between plan and fact",
            "@Before disable check of worked roster before adding shift",
            "@Before disable all shift comments",
            "@Before disable roster single edited version"},
            description = "Создание смены на день после смены, переходящей на следующие сутки")
    @Link(name = "Статья: \"4189_Рефакторинг фронта для переноса смен\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=217712504")
    @TmsLink("60320")
    @Tag("ABCHR4189-1")
    @Tag(SCHED9)
    public void createShiftAfterShiftEndingInNextDay() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        ShiftTimePosition timePosition = ShiftTimePosition.DEFAULT;
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        Shift shift = PresetClass.presetForMakeShiftWithExcludeDate(ep, true, timePosition, LocalDateTools.getLastDate());
        LocalDate dayAfter = shift.getDateTimeInterval().getEndDate();
        PresetClass.makeClearDate(ep, dayAfter);
        goToSchedule(unit);
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        clickOnEmptyCell(ep, dayAfter, scheduleWorker);
        clickOnPlusCellOnGraph();
        LocalDateTime startTime = shift.getDateTimeInterval().getEndDateTime().minusHours(1);
        enterShiftTimeStartOrEnd(startTime.toLocalTime(), TimeTypeField.START_TIME);
        int duration = new Random().nextInt(4) + 4;
        LocalDateTime endTime = startTime.plusHours(duration);
        if (!startTime.toLocalDate().equals(endTime.toLocalDate())) {
            enterShiftDateStartOrEnd(endTime.toLocalDate(), DateTypeField.END_DATE);
        }
        enterShiftTimeStartOrEnd(endTime.toLocalTime(), TimeTypeField.END_TIME);
        List<Shift> shiftsBefore = ShiftRepository.getShifts(ep, timePosition);
        createShiftButtonClick();
        String errorText = shiftOverlapErrorMessageGenerator(unit, ep, shift);
        assertNoChangesToShifts(scheduleWorker, startTime.toLocalDate(), ep, shiftsBefore, null, errorText, timePosition);
    }

    @Test(groups = {"ABCHR4189-2", SHIFTS, G1, SCHED9,
            "@Before check if first day of month",
            "@Before disable equality check between plan and fact",
            "@Before disable check of worked roster before adding shift",
            "@Before disable all shift comments",
            "@Before enable drag and drop function",
            "@Before enable copy shifts in worked roster"},
            description = "Перемещение смены на день после смены, переходящей на следующие сутки")
    @Link(name = "Статья: \"4189_Рефакторинг фронта для переноса смен\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=217712504")
    @TmsLink("60320")
    @Tag("ABCHR4189-2")
    @Tag(SCHED9)
    public void moveShiftToAfterShiftEndingInNextDay() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        PresetClass.changeOrSetMathParamValue(omId, MathParameterValues.ACCESS_TO_OVERWORK, true, true);
        ShiftTimePosition timePosition = ShiftTimePosition.DEFAULT;
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        Shift firstShift = PresetClass.presetForMakeShiftWithExcludeDate(ep, true, timePosition,
                                                                         timePosition.getShiftsDateInterval().getEndDate(), timePosition.getShiftsDateInterval().getEndDate().minusDays(1));
        LocalDateTime nextDay = firstShift.getDateTimeInterval().getEndDateTime();

        LocalDateTime secondShiftStart = nextDay.toLocalDate().plusDays(1)
                .atTime(nextDay.toLocalTime().minusHours(1));
        PresetClass.presetForMakeShiftDateTime(ep, secondShiftStart, secondShiftStart.plusHours(4), timePosition);

        goToSchedule(unit);
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        transferOneShift(ep, ep, secondShiftStart, nextDay, scheduleWorker);
        List<Shift> shiftsBefore = ShiftRepository.getShifts(ep, timePosition);
        clickReplaceShiftButton();
        String errorText = "Смена уже существует в этом временном промежутке для сотрудника";
        assertNoChangesToShifts(scheduleWorker, nextDay.toLocalDate(), ep, shiftsBefore, null, errorText, timePosition);
    }

    @Test(groups = {"ABCHR4189-4", "ABCHR4189", SHIFTS, G1, SCHED9,
            "@Before disable equality check between plan and fact",
            "@Before disable merged view for planned and actual shifts",
            "@Before disable check of worked roster before adding shift",
            "@Before disable worked shift comments",
            "@Before enable drag and drop function",
            "@Before enable copy shifts in worked roster"},
            description = "Дублирование смены на день после смены, переходящей на следующие сутки")
    @Link(name = "Статья: \"4189_Рефакторинг фронта для переноса смен\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=217712504")
    @TmsLink("60320")
    @Tag("ABCHR4189-4")
    @Tag(SCHED9)
    public void copyShiftToAfterShiftEndingInNextDay() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        PresetClass.changeOrSetMathParamValue(omId, MathParameterValues.ACCESS_TO_OVERWORK, true, true);
        ImmutablePair<EmployeePosition, EmployeePosition> eps = EmployeePositionRepository.getTwoRandomEmployeeWithCheckByApi(omId);
        EmployeePosition ep = eps.left;
        EmployeePosition ep2 = eps.right;
        ShiftTimePosition timePosition = ShiftTimePosition.DEFAULT;
        Shift firstShift = PresetClass.presetForMakeShiftWithExcludeDate(ep, true, timePosition,
                                                                         LocalDateTools.getLastDate(), LocalDateTools.getLastDate().minusDays(1));
        PresetClass.makeClearDate(ep, firstShift.getStartDate().plusDays(1));
        LocalDateTime nextDay = firstShift.getDateTimeInterval().getEndDateTime();
        LocalDateTime secondShiftStart = nextDay.toLocalDate()
                .atTime(nextDay.toLocalTime().minusHours(1));
        PresetClass.presetForMakeShiftDateTime(ep2, secondShiftStart, secondShiftStart.plusHours(4), timePosition);

        goToSchedule(unit);
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        transferOneShift(ep2, ep, secondShiftStart, nextDay, scheduleWorker);
        List<Shift> shiftsBefore = ShiftRepository.getShifts(ep, timePosition);
        clickDuplicateShiftButton();

        String errorText = "Смена уже существует в этом временном промежутке для сотрудника";
        assertNoChangesToShifts(scheduleWorker, nextDay.toLocalDate(), ep, shiftsBefore, null, errorText, timePosition);
    }

    private String getErrorText(OrgUnit unit, EmployeePosition ep, Shift shift) {
        return URL_BASE.contains(ZOZO) ?
                String.format("Пересечение со сменой в подразделении %s по сотруднику %s c %s по %s", unit.getName(), ep.getEmployee().getFullName(),
                              shift.getDateTimeInterval().getStartDateTime().format(UI_DATETIME_WITH_SPACE.getFormat()),
                              shift.getDateTimeInterval().getEndDateTime().format(UI_DATETIME_WITH_SPACE.getFormat())) :
                "Смена уже существует в этом временном промежутке для сотрудника";
    }

    @Test(groups = {"ABCHR4189-5", SHIFTS, G1, SCHED9,
            "@Before disable equality check between plan and fact",
            "@Before disable check of worked roster before adding shift",
            "@Before disable merged view for planned and actual shifts",
            "@Before disable start time check for worked shifts",
            "@Before disable all shift comments",
            "@Before don't show button to publish roster",
            "@Before enable drag and drop function",
            "@Before enable copy shifts in worked roster"},
            description = "Массовое дублирование смен на день после смены, переходящей на следующие сутки")
    @Link(name = "Статья: \"4189_Рефакторинг фронта для переноса смен\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=217712504")
    @TmsLink("60320")
    @Tag("ABCHR4189-5")
    @Tag(SCHED9)
    public void batchCopyShiftToAfterShiftEndingInNextDay() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        PresetClass.changeOrSetMathParamValue(omId, MathParameterValues.ACCESS_TO_OVERWORK, true, true);
        Shift[] movingShifts = PresetClass.massShiftPresetCheckForEmployee(ep);
        LocalDate firstShiftDay = movingShifts[0].getDateTimeInterval().getStartDate();
        ShiftTimePosition timePosition = ShiftTimePosition.DEFAULT;
        ImmutablePair<LocalDate, LocalDate> freeDays = PresetClass.twoFreeDaysChecker(ep, timePosition,
                                                                                      LocalDateTools.getFirstDate(), firstShiftDay, firstShiftDay.plusDays(1), firstShiftDay.plusDays(2));
        LocalDate firstFreeDay = freeDays.left;
        Shift nightShift = PresetClass.presetForMakeShiftDate(ep, firstFreeDay.minusDays(1), true, timePosition);
        LocalDateTime firstShiftStart = LocalDateTime.of(firstShiftDay,
                                                         nightShift.getDateTimeInterval().getEndDateTime().toLocalTime().minusHours(1));
        movingShifts[0] = PresetClass.presetForMakeShiftDateTime(ep, firstShiftStart, firstShiftStart.plusHours(4), timePosition);
        PresetClass.makeClearDate(ep, firstFreeDay);
        goToSchedule(unit);
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        selectTwoShift(ep, movingShifts, scheduleWorker);
        List<Shift> shiftsBefore = ShiftRepository.getShifts(ep, timePosition);
        batchCopyShift(ep, scheduleWorker, firstFreeDay, false);
        String errorText = "Смена уже существует в этом временном промежутке для сотрудника";
        assertNoChangesToShifts(scheduleWorker, firstFreeDay, ep, shiftsBefore, null, errorText, timePosition);
    }

    @Test(groups = {"ABCHR4189-6", SHIFTS, G1, SCHED9,
            "@Before disable equality check between plan and fact",
            "@Before disable check of worked roster before adding shift",
            "@Before disable all shift comments"},
            description = "Редактирование смены после смены, переходящей на следующие сутки")
    @Link(name = "Статья: \"4189_Рефакторинг фронта для переноса смен\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=217712504")
    @TmsLink("60320")
    @Tag("ABCHR4189-6")
    @Tag(SCHED9)
    public void editShiftToAfterShiftEndingInNextDay() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        PresetClass.changeOrSetMathParamValue(omId, MathParameterValues.ACCESS_TO_OVERWORK, true, true);
        ShiftTimePosition timePosition = ShiftTimePosition.DEFAULT;
        Shift firstShift = PresetClass.presetForMakeShiftWithExcludeDate(ep, true, timePosition, LocalDateTools.getLastDate());
        LocalDateTime shiftEndDateTime = firstShift.getDateTimeInterval().getEndDateTime();
        LocalDate nextDay = shiftEndDateTime.toLocalDate();
        PresetClass.presetForMakeShiftDate(ep, nextDay, false, timePosition);

        goToSchedule(unit);
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        clickOnTargetShift(ep, nextDay, scheduleWorker);
        enterShiftTimeStartOrEnd(shiftEndDateTime.toLocalTime().minusHours(1), TimeTypeField.START_TIME);
        List<Shift> shiftsBefore = ShiftRepository.getShifts(ep, timePosition);
        String expectedShiftElementText = scheduleWorker.getScheduleShiftElement(ep, nextDay).getText();
        clickEditShiftButton();
        String errorText = shiftOverlapErrorMessageGenerator(unit, ep, firstShift);
        assertNoChangesToShifts(scheduleWorker, nextDay, ep, shiftsBefore, expectedShiftElementText, errorText, timePosition);
    }

    @Test(groups = {"ABCHR4189-7", SHIFTS, G1, SCHED9,
            "@Before disable equality check between plan and fact",
            "@Before disable check of worked roster before adding shift"},
            description = "Перемещение смены на день с подтвержденным запросом отсутствия",
            expectedExceptions = WaitUntilException.class, expectedExceptionsMessageRegExp = ANY + SHIFT_ACTIONS_NOT_DISPLAYED + ANY)
    @Link(name = "Статья: \"4189_Рефакторинг фронта для переноса смен\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=217712504")
    @TmsLink("60320")
    @Tag("ABCHR4189-7")
    @Tag(SCHED9)
    public void moveShiftToDayWithApprovedAbsence() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        ScheduleRequest absenceRequest = PresetClass.createScheduleRequestApi(ScheduleRequestStatus.APPROVED, false, omId);
        assert absenceRequest != null;
        Employee employee = absenceRequest.getEmployee();
        EmployeePosition ep = EmployeePositionRepository.getEmployeePosition(employee.getFullName(), omId);
        LocalDateTime requestDay = absenceRequest.getDateTimeInterval().getStartDateTime();
        Shift movingShift = ShiftRepository.getShifts(ep, ShiftTimePosition.ALLMONTH)
                .stream()
                .findAny()
                .orElse(PresetClass.presetForMakeShift(ep, false, ShiftTimePosition.ALLMONTH));

        goToSchedule(unit);
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        transferOneShift(ep, ep, movingShift.getDateTimeInterval().getStartDateTime(), requestDay, scheduleWorker);
    }

    @Test(groups = {"ABCHR4189-8", SHIFTS, G1, SCHED9,
            "@Before disable equality check between plan and fact",
            "@Before disable check of worked roster before adding shift",
            "@Before disable start time check for worked shifts",
            "@Before disable mandatory comment when editing or deleting shift",
            "@Before disable all shift comments"},
            description = "Массовое перемещение смен на день с подтвержденным запросом отсутствия")
    @Link(name = "Статья: \"4189_Рефакторинг фронта для переноса смен\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=217712504")
    @TmsLink("60320")
    @Tag("ABCHR4189-8")
    @Tag(SCHED9)
    public void batchMoveShiftToDayWithApprovedAbsence() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts(true, true, false, false, false);
        int omId = unit.getId();
        PresetClass.changeOrSetMathParamValue(omId, MathParameterValues.ACCESS_TO_OVERWORK, true, true);
        List<EmployeePosition> emp = EmployeePositionRepository.getEmployeePositions(omId)
                .stream().filter(e -> !e.isHidden())
                .limit(2)
                .collect(Collectors.toList());
        EmployeePosition firstEmp = emp.get(0);
        ShiftTimePosition timePosition = ShiftTimePosition.DEFAULT;
        LocalDate requestDay = getRandomFromList(timePosition.getShiftsDateInterval().getBetweenDatesList());
        PresetClass.makeClearDate(firstEmp, requestDay);
        PresetClass.createScheduleRequestForDate(ScheduleRequestStatus.APPROVED, requestDay, firstEmp, null);
        EmployeePosition secondEmp = emp.get(1);
        LocalDate freeDay = requestDay.plusDays(1);
        PresetClass.makeClearDate(firstEmp, freeDay);
        LocalDate[] dates = new LocalDate[]{requestDay, freeDay};
        Shift[] movingShifts = PresetClass.massShiftPresetAtSameDays(secondEmp, dates);

        goToSchedule(unit);
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        selectTwoShift(secondEmp, movingShifts, scheduleWorker);
        List<Shift> shiftsBefore = ShiftRepository.getShifts(firstEmp, timePosition);
        batchTransferShift(firstEmp, scheduleWorker, freeDay, true);
        assertNoChangesToShifts(scheduleWorker, requestDay, firstEmp, shiftsBefore, null, null, timePosition);
    }

    @Test(groups = {"ABCHR4189-9", SHIFTS, G1, SCHED9,
            "@Before disable equality check between plan and fact",
            "@Before disable check of worked roster before adding shift",
            "@Before disable start time check for worked shifts",
            "@Before disable all shift comments",
            "@Before enable copy shifts in worked roster",
            "@Before enable drag and drop function"
    }, description = "Массовое дублирование смен на день с подтвержденным запросом отсутствия")
    @Link(name = "Статья: \"4189_Рефакторинг фронта для переноса смен\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=217712504")
    @TmsLink("60320")
    @Tag("ABCHR4189-9")
    @Tag(SCHED9)
    public void batchCopyShiftToDayWithApprovedAbsence() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts(true, false, false, false, true);
        int omId = unit.getId();
        PresetClass.changeOrSetMathParamValue(omId, MathParameterValues.ACCESS_TO_OVERWORK, true, true);
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, false);
        ShiftTimePosition timePosition = ShiftTimePosition.DEFAULT;
        LocalDate requestDay = getRandomFromList(timePosition.getShiftsDateInterval().getBetweenDatesList());
        PresetClass.makeClearDate(ep, requestDay);
        PresetClass.createScheduleRequestForDate(ScheduleRequestStatus.APPROVED, requestDay, ep, null);
        LocalDate freeDay = requestDay.plusDays(1);
        PresetClass.makeClearDate(ep, freeDay);
        Shift[] movingSifts = PresetClass.massShiftPresetCheckForEmployee(ep);

        goToSchedule(unit);
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        selectTwoShift(ep, movingSifts, scheduleWorker);
        List<Shift> shiftsBefore = ShiftRepository.getShifts(ep, timePosition);
        batchCopyShift(ep, scheduleWorker, requestDay, true);
        assertNoChangesToShifts(scheduleWorker, requestDay, ep, shiftsBefore, null, null, timePosition);
    }

    @Test(groups = {"ABCHR4192", G1, LIST16, NOT_MAGNIT_REGRESS,
            "@Before show shift hiring reason",
            "@Before forbid shift exchange use job title",
            "@After remove test shift hiring reasons"},
            dataProvider = "0, 1, 2",
            description = "Редактирование причины привлечения сотрудника для смены на бирже")
    @Link(name = "Статья: \"4192_Пользователь может указать на смене для биржи \"Причина привлечения сотрудника\" со справочником\"", url = "https://wiki.goodt.me/x/Swn6D")
    @TmsLink("60294")
    @Owner(SCHASTLIVAYA)
    @Tag("ABCHR4192-2")
    @Tag(LIST16)
    public void editHiringReason(int numberOfReasons) {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        PositionGroup posGroup = PositionGroupRepository.randomPositionGroup();
        PositionCategory posCat = PositionCategoryRepository.randomDynamicPositionCategory();
        ShiftHiringReason reason = PresetClass.setupHiringReasons(omId, numberOfReasons);
        LocalDate freeShiftDay = ShiftTimePosition.FUTURE.getShiftsDateInterval().getRandomDateBetween();
        Shift shift = PresetClass.makeFreeShift(freeShiftDay, omId, null, posGroup, posCat, reason, null, null, null);
        DateTimeInterval freeShiftInterval = shift.getDateTimeInterval();
        goToSchedule(unit);
        clickFreeShift(freeShiftDay);
        int order = findShiftOrderNumber(freeShiftInterval, posGroup.getName());
        clickThreeDotsForFreeShift(order + 1);
        clickEditButtonForFreeShift("Редактировать");
        if (numberOfReasons == 0) {
            Assert.assertThrows(AssertionError.class, () -> selectHiringReasonForFreeShift(reason));
        } else {
            selectHiringReasonForFreeShift(reason);
            Roster activeRoster = RosterRepository.getActiveRosterThisMonth(unit.getId());
            clickEditShiftButton();
            assertEditFreeShift(unit, activeRoster, reason, shift);
        }
    }

    @Test(groups = {"ABCHR4192", G1, LIST16, MAGNIT,
            "@Before show shift hiring reason",
            "@Before forbid shift exchange use job title",
            "@After remove test shift hiring reasons"},
            dataProvider = "0, 1, 2",
            description = "Редактирование причины привлечения сотрудника для смены на бирже")
    @Link(name = "Статья: \"4192_Пользователь может указать на смене для биржи \"Причина привлечения сотрудника\" со справочником\"", url = "https://wiki.goodt.me/x/Swn6D")
    @TmsLink("60294")
    @Owner(BUTINSKAYA)
    @Tag("ABCHR4192-2")
    @Tag(LIST16)
    public void editHiringReasonMagnit(int numberOfReasons) {
        ImmutablePair<OrgUnit, EmployeePosition> unitAndEmp = OrgUnitRepository.getRandomOrgUnitWithRandomEmployeeForShiftPair(false);
        OrgUnit unit = unitAndEmp.getLeft();
        int omId = unit.getId();
        Position pos = unitAndEmp.getRight().getPosition();
        PositionGroup posGroup = PositionGroupRepository.getPositionGroupById(pos.getPositionGroupId());
        PositionCategory posCat = PositionCategoryRepository.getPositionCategoryById(pos.getPositionCategoryId());
        ShiftHiringReason reason = PresetClass.setupHiringReasonForMagnit(omId, numberOfReasons);
        LocalDate freeShiftDay = ShiftTimePosition.FUTURE.getShiftsDateInterval().getRandomDateBetween();
        PositionType posType = PositionTypeRepository.getPositionTypeById(pos.getPositionTypeId());
        PresetClass.removeFreeShifts(omId, freeShiftDay);
        Shift shift = PresetClass.makeFreeShift(freeShiftDay, omId, null, posGroup, posCat, null, null, posType, null);
        JobTitle jobTitle = JobTitleRepository.getJob(shift.getJobTitle());
        PresetClass.createRule(false, reason, jobTitle, jobTitle, omId);
        DateTimeInterval freeShiftInterval = shift.getDateTimeInterval();
        goToSchedule(unit);
        clickFreeShift(freeShiftDay);
        int order = findShiftOrderNumber(freeShiftInterval, jobTitle.getFullName());
        clickThreeDotsForFreeShift(order + 1);
        clickEditButtonForFreeShift("Редактировать");
        if (numberOfReasons == 0) {
            Assert.assertThrows(AssertionError.class, () -> selectHiringReasonForFreeShift(reason));
        } else {
            selectHiringReasonForFreeShift(reason);
            Roster activeRoster = RosterRepository.getActiveRosterThisMonth(unit.getId());
            clickEditShiftButton();
            assertEditFreeShift(unit, activeRoster, reason, shift);
        }
    }

    @Test(groups = {"ABCHR4192", G1, LIST16, NOT_MAGNIT_REGRESS,
            "@Before show shift hiring reason",
            "@Before disable all shift comments",
            "@Before allow editing plan shifts in future",
            "@Before forbid shift exchange use job title",
            "@After remove test shift hiring reasons"},
            dataProvider = "true/false", dataProviderClass = DataProviders.class,
            description = "Доступ к полю \"Причина привлечения сотрудника\" в карточке свободной смены при наличии прав")
    @Link(name = "Статья: \"4192_Пользователь может указать на смене для биржи \"Причина привлечения сотрудника\" со справочником\"", url = "https://wiki.goodt.me/x/Swn6D")
    @TmsLink("60294")
    @Owner(SCHASTLIVAYA)
    @Tag(LIST16)
    public void accessToHiringReason(boolean hasAccess) {
        changeTestIDDependingOnParameter(hasAccess, "ABCHR4192-3", "ABCHR4192-4",
                                         "Доступ к полю \"Причина привлечения сотрудника\" в карточке свободной смены без прав");
        checkLastDayOfMonth();
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        PresetClass.switchShiftExchange(unit, true);
        PositionGroup posGroup = PositionGroupRepository.randomPositionGroup();
        PositionCategory posCat = PositionCategoryRepository.randomDynamicPositionCategory();
        ShiftHiringReason reason = null;
        LocalDate freeShiftDay = ShiftTimePosition.FUTURE.getShiftsDateInterval().getRandomDateBetween();
        LocalDateTime start = freeShiftDay.isEqual(getDateTimeInFuture().toLocalDate())
                ? getDateTimeInFuture().truncatedTo(ChronoUnit.HOURS)
                : freeShiftDay.atTime(LocalTime.now().withHour((int) (Math.random() * 17))).truncatedTo(ChronoUnit.HOURS);
        LocalDateTime end = start.plusHours(6);
        PresetClass.removeFreeShifts(omId, freeShiftDay);

        List<PermissionType> permissions = new ArrayList<>(Arrays.asList(PermissionType.SCHEDULE_VIEW,
                                                                         PermissionType.SCHEDULE_EDIT,
                                                                         PermissionType.POSITION_GROUPS_VIEW,
                                                                         PermissionType.ORG_UNIT_EMPLOYEE_PERSONALISATION,
                                                                         PermissionType.CREATE_SHIFT_EXCHANGE));
        if (hasAccess) {
            permissions.add(PermissionType.HIRING_REASON_EDIT);
            reason = PresetClass.setupHiringReasonAndEntityPropertyForOrgUnit(omId);
        }
        Role role = PresetClass.createCustomPermissionRole(permissions);
        goToScheduleAsUser(role, unit);
        clickFreeShift(freeShiftDay);
        enterShiftTimeStartOrEnd(start.toLocalTime(), TimeTypeField.START_TIME);
        if (end.isBefore(getServerDateTime())) {
            end = getServerDateTime().plusHours(6);
        }
        if (!freeShiftDay.equals(end.toLocalDate())) {
            enterShiftDateStartOrEnd(end.toLocalDate(), DateTypeField.END_DATE);
        }
        enterShiftTimeStartOrEnd(end.toLocalTime(), TimeTypeField.END_TIME);
        selectPositionCategoryForFreeShift(posCat);
        selectPositionGroupForFreeShift(posGroup);
        if (hasAccess) {
            selectHiringReasonForFreeShift(reason);
        }
        DateTimeInterval interval = new DateTimeInterval(start, end);
        createShiftButtonClick();
        assertAddFreeShift(unit, interval, posGroup, reason);
    }

    @Test(groups = {"ABCHR4192", G1, LIST16, MAGNIT,
            "@Before show shift hiring reason",
            "@Before disable all shift comments",
            "@Before allow editing plan shifts in future",
            "@Before forbid shift exchange use job title",
            "@After remove test shift hiring reasons"},
            dataProvider = "true/false", dataProviderClass = DataProviders.class,
            description = "Доступ к полю \"Причина привлечения сотрудника\" в карточке свободной смены при наличии прав")
    @Link(name = "Статья: \"4192_Пользователь может указать на смене для биржи \"Причина привлечения сотрудника\" со справочником\"", url = "https://wiki.goodt.me/x/Swn6D")
    @TmsLink("60294")
    @Owner(SCHASTLIVAYA)
    @Tag(LIST16)
    public void accessToHiringReasonMagnit(boolean hasAccess) {
        changeTestIDDependingOnParameter(hasAccess, "ABCHR4192-3", "ABCHR4192-4",
                                         "Доступ к полю \"Причина привлечения сотрудника\" в карточке свободной смены без прав");
        ImmutablePair<OrgUnit, EmployeePosition> unitAndEmp = OrgUnitRepository.getRandomOrgUnitWithRandomEmployeeForShiftPair(false);
        OrgUnit unit = unitAndEmp.getLeft();
        Position pos = unitAndEmp.getRight().getPosition();
        int omId = unit.getId();
        PresetClass.switchShiftExchange(unit, true);
        PositionGroup posGroup = PositionGroupRepository.getPositionGroupById(pos.getPositionGroupId());
        PositionCategory posCat = PositionCategoryRepository.getPositionCategoryById(pos.getPositionCategoryId());
        LocalDate freeShiftDay = ShiftTimePosition.FUTURE.getShiftsDateInterval().getRandomDateBetween();
        LocalDateTime start = freeShiftDay.isEqual(getDateTimeInFuture().toLocalDate())
                ? getDateTimeInFuture().truncatedTo(ChronoUnit.HOURS)
                : freeShiftDay.atTime(LocalTime.now().withHour((int) (Math.random() * 17))).truncatedTo(ChronoUnit.HOURS);
        LocalDateTime end = start.plusHours(6);
        PresetClass.removeFreeShifts(omId, freeShiftDay);

        List<PermissionType> permissions = new ArrayList<>(Arrays.asList(PermissionType.SCHEDULE_VIEW,
                                                                         PermissionType.SCHEDULE_EDIT,
                                                                         PermissionType.POSITION_GROUPS_VIEW,
                                                                         PermissionType.ORG_UNIT_EMPLOYEE_PERSONALISATION,
                                                                         PermissionType.CREATE_SHIFT_EXCHANGE));
        ShiftHiringReason reason = null;
        if (hasAccess) {
            reason = PresetClass.setupHiringReasonAndEntityPropertyForOrgUnit(omId);
            permissions.add(PermissionType.HIRING_REASON_EDIT);
            PositionType posType = PositionTypeRepository.getPositionTypeById(pos.getPositionTypeId());
            JobTitle jobTitle = JobTitleRepository.getJob(posType.getName());
            PresetClass.createRule(false, reason, jobTitle, jobTitle, omId);

        }
        Role role = PresetClass.createCustomPermissionRole(permissions);
        goToScheduleAsUser(role, unit);
        clickFreeShift(freeShiftDay);
        enterShiftTimeStartOrEnd(start.toLocalTime(), TimeTypeField.START_TIME);
        if (end.isBefore(getServerDateTime())) {
            end = getServerDateTime().plusHours(6);
        }
        if (!freeShiftDay.equals(end.toLocalDate())) {
            enterShiftDateStartOrEnd(end.toLocalDate(), DateTypeField.END_DATE);
        }
        enterShiftTimeStartOrEnd(end.toLocalTime(), TimeTypeField.END_TIME);
        selectPositionCategoryForFreeShift(posCat);
        selectPositionGroupForFreeShift(posGroup);
        if (hasAccess) {
            selectHiringReasonForFreeShift(reason);
        }
        DateTimeInterval interval = new DateTimeInterval(start, end);
        createShiftButtonClick();
        assertAddFreeShift(unit, interval, posGroup, reason);
    }

    @Test(groups = {"TK2745", "TEST-1175"}, description = "Выгрузка графика в 1С")
    public void unloadingScheduleGraph1C() {
        TypeOfFiles typeOfFiles = TypeOfFiles.ONE_C;
        PresetClass.setSystemPropertyValue(SystemProperties.SCHEDULE_BOARD_DOWNLOAD_1C, true);
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        goToSchedule(unit);
        threeDotsMenuClick();
        URI uriFromButtonAttribute = getUriFromButtonAttribute(VariantsOfFunctions.UPLOAD_IN_1C);
        chooseFunction(VariantsOfFunctions.UPLOAD_IN_1C);
        FileDownloadCheckerForScheduleBoard checker = new FileDownloadCheckerForScheduleBoard(Role.ADMIN, unit.getId(),
                                                                                              typeOfFiles, TypeOfReports.UPLOAD_1C);
        assertForDownloadingPath(checker, uriFromButtonAttribute);
    }

    @Ignore(NOT_ACTUAL)
    @Test(groups = {"ABCHR5480-1",
            "@Before enable outstaff filtering settings"},
            description = "Фильтрация персонала по типу в Расписании",
            dataProvider = "Employee Types")
    @Link(name = "Статья: \"5480 Фильтрация строк с графика рабочего времени на экране главного меню\"", url = "https://wiki.goodt.me/x/Aiv0DQ")
    @TmsLink("60324")
    public void filterEmployees(EmployeeType employeeType, Predicate<EmployeePosition> predicate) {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        PresetClass.switchShiftExchange(unit, true);
        boolean checkOutStaff = false;
        if (employeeType == EmployeeType.INTERNAL_PART_TIMER) {
            PresetClass.temporaryPositionPreset(unit);
        } else if (employeeType == EmployeeType.OUT_STAFF) {
            PresetClass.makeOutStaffShift(unit);
            PresetClass.makeOutStaffEmployee(unit);
            checkOutStaff = true;
        } else {
            PresetClass.checkOwnEmployeeAvailability(unit);
        }
        PresetClass.checkOwnEmployeeAvailability(unit);
        goToSchedule(unit);
        employeeFilterButtonClick();
        pickPersonnelTypeFromEmployeeFilter(employeeType.getName());
        List<String> activeEmployees = activeEmployees();
        filterModeApplyButtonClick();
        List<String> expectedEmployees = EmployeePositionRepository.getEmployeePositions(unit.getId())
                .stream()
                .filter(predicate)
                .map(EmployeePosition::getEmployee)
                .map(Employee::getShortName)
                .map(e -> e.replaceAll("\\s+", " ").trim())
                .collect(Collectors.toList());
        checkFilterModeAndCompareWithApi(activeEmployees, unit.getId(), expectedEmployees, checkOutStaff);
    }

    @Test(groups = {"ABCHR5506", G2, SCHED16},
            description = "Отображение блока \"режим работы подразделения\" в свойствах подразделения",
            dataProvider = "true/false", dataProviderClass = DataProviders.class)
    @Link(name = "Статья: \"5506_Скрыть блок \"Режим работы подразделения\"\"", url = "https://wiki.goodt.me/x/FS70DQ")
    @TmsLink("60239")
    @Owner(SCHASTLIVAYA)
    @Tag(SCHED16)
    public void showOperatingHoursOnlyToUsersWithPermissions(boolean hasPermissions) {
        changeTestIDDependingOnParameter(hasPermissions, "ABCHR5506-2", "ABCHR5506-1",
                                         "Отсутствие блока \"режим работы подразделения\" в свойствах подразделения");
        OrgUnit unit = OrgUnitRepository.getRandomAvailableOrgUnit();
        List<PermissionType> permissions = new ArrayList<>(Arrays.asList(PermissionType.SCHEDULE_VIEW, PermissionType.SCHEDULE_EDIT));
        if (hasPermissions) {
            permissions.add(PermissionType.UNIT_OPERATING_MODE);
        }
        Role role = PresetClass.createCustomPermissionRole(permissions);

        goToScheduleAsUser(role, unit);
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.SUBDIVISION_PROPERTIES);
        if (hasPermissions) {
            clickOnChevronButton(OmInfoName.SCHEDULE);
        } else {
            Assert.assertThrows(WaitUntilException.class, () -> clickOnChevronButton(OmInfoName.SCHEDULE));
        }
    }

    @Test(groups = {"ABCHR3056", "@Before enable additional info indicator", G1, SCHED2, POCHTA},
            dataProvider = "locales",
            description = "Настраиваемое название табеля")
    @Link(name = "Статья: \"3056_Сделать настраиваемым название табеля", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=204278815")
    @TmsLink("60160")
    @Tag("ABCHR3056")
    @Tag(SCHED2)
    public void customizableTimesheet(String locale) {
        changeLocale(locale);
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitWithWorkedRosters(true);
        goToScheduleWithCheck(unit);
        ElementsCollection<AtlasWebElement> indicatorList = sb.formLayout().timesheetIndicator();
        if (indicatorList.isEmpty()) {
            pressMenuI();
            selectItemFromDropDownMenuI(ButtonIDropDownMenu.ADDITIONAL_INFORMATION);
        }
        checkCaptionTimesheet();
    }

    @Ignore(NOT_ACTUAL)
    @Test(groups = {"ABCHR3532",
            "@Before disable schedule request: overtime", "@Before enable overtime access"},
            description = "Добавление запроса сверхурочной работы в табеле")
    @Link(name = "Статья: \"3532_Добавить новый тип запроса \"Сверхурочная работа\"\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=204282080")
    @TmsLink("60328")
    @Tag("ABCHR3532-1")
    @Tag(SCHED9)
    public void addOvertimeRequestToWorkedRoster() {
        checkFirstDayOfMonth();
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        int rosterId = RosterRepository.getActiveRosterThisMonth(omId).getId();
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        Shift shift = ShiftRepository.getShifts(ep, ShiftTimePosition.PAST).stream().findAny().orElse(PresetClass.defaultShiftPreset(ep, ShiftTimePosition.PAST));
        goToSchedule(unit);
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        addRequestForShift(ep, shift.getDateTimeInterval().getStartDate(), scheduleWorker);
        ScheduleRequestType requestType = ScheduleRequestType.OVERTIME;
        clickOnSelectRequestTypeButton();
        selectRequestType(requestType);
        LocalDateTime startTime = shift.getDateTimeInterval().getEndDateTime();
        enterShiftTimeStartOrEnd(startTime.toLocalTime(), TimeTypeField.START_TIME);
        LocalDateTime endTime = startTime.plusHours(new Random().nextInt(4) + 1);
        if (!startTime.toLocalDate().equals(endTime.toLocalDate())) {
            enterShiftDateStartOrEnd(endTime.toLocalDate(), DateTypeField.END_DATE);
        }
        enterShiftTimeStartOrEnd(endTime.toLocalTime(), TimeTypeField.END_TIME);
        List<OutsidePlanResource> overtimeBefore = OutsidePlanResourceRepository.getAllOutsideResources(rosterId, ShiftTimePosition.ALLMONTH);
        clickEditShiftButton();
        assertAddOvertime(ep, scheduleWorker, overtimeBefore, omId,
                          new DateTimeInterval(startTime, endTime));
    }

    @Ignore(NOT_ACTUAL)
    @Test(groups = {"ABCHR3532-3",
            "@Before disable schedule request: overtime", "@Before enable overtime access"},
            description = "Добавление запроса сверхурочной работы в плановом графике")
    @TmsLink("60328")
    @Link(name = "Статья: \"3532_Добавить новый тип запроса \"Сверхурочная работа\"\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=204282080")
    @Tag(SCHED9)
    @Tag("ABCHR3532-3")
    public void addOvertimeRequestToPlannedShifts() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        int rosterId = RosterRepository.getActiveRosterThisMonth(omId).getId();
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        Shift shift = ShiftRepository.getShifts(ep, ShiftTimePosition.FUTURE).stream().findAny().orElse(PresetClass.defaultShiftPreset(ep, ShiftTimePosition.FUTURE));
        goToSchedule(unit);
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        addRequestForShift(ep, shift.getDateTimeInterval().getStartDate(), scheduleWorker);
        ScheduleRequestType requestType = ScheduleRequestType.OVERTIME;
        clickOnSelectRequestTypeButton();
        selectRequestType(requestType);
        LocalDateTime startTime = shift.getDateTimeInterval().getEndDateTime();
        enterShiftTimeStartOrEnd(startTime.toLocalTime(), TimeTypeField.START_TIME);
        LocalDateTime endTime = startTime.plusHours(new Random().nextInt(4) + 1);
        if (!startTime.toLocalDate().equals(endTime.toLocalDate())) {
            enterShiftDateStartOrEnd(endTime.toLocalDate(), DateTypeField.END_DATE);
        }
        enterShiftTimeStartOrEnd(endTime.toLocalTime(), TimeTypeField.END_TIME);
        List<OutsidePlanResource> overtimeBefore = OutsidePlanResourceRepository.getAllOutsideResources(rosterId, ShiftTimePosition.ALLMONTH);
        clickEditShiftButton();
        assertAddOvertime(ep, scheduleWorker, overtimeBefore, omId,
                          new DateTimeInterval(startTime, endTime));
    }

    @Ignore(NOT_ACTUAL)
    @Test(groups = {"ABCHR3532",
            "@Before disable schedule request: overtime", "@Before enable overtime access"},
            description = "Удаление запроса сверхурочной работы")
    @TmsLink("60328")
    @Link(name = "Статья: \"3532_Добавить новый тип запроса \"Сверхурочная работа\"\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=204282080")
    @Tag(SCHED9)
    @Tag("ABCHR3532-4")
    public void deleteOvertimeRequest() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        EmployeePosition employeePosition = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        LocalDateTime requestDayTime = PresetClass.getFreeDateForEmployeeShiftPreset(employeePosition).atTime(10, 0);
        ScheduleRequestType requestType = ScheduleRequestType.OVERTIME;
        int rosterId = RosterRepository.getActiveRosterThisMonth(omId).getId();
        OutsidePlanResource overtimeRequest = PresetClass.createOutsidePlanResource(requestType,
                                                                                    requestDayTime, employeePosition, rosterId);
        goToSchedule(unit);
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        LocalDate overtimeDate = requestDayTime.toLocalDate();
        clickOutsidePlanResourceElement(requestType, scheduleWorker, employeePosition, overtimeDate);
        shiftThreeDotsClick();
        selectAction(RequestAction.DELETE, false);
        assertDeleteOutsidePlanResource(employeePosition, requestType, overtimeRequest, scheduleWorker, rosterId, overtimeDate);
    }

    @Ignore(NOT_ACTUAL)
    @Test(groups = {"ABCHR3532-7",
            "@Before disable schedule request: overtime", "@Before enable overtime access"},
            description = "Изменение запроса сверхурочной работы")
    @TmsLink("60328")
    @Link(name = "Статья: \"3532_Добавить новый тип запроса \"Сверхурочная работа\"\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=204282080")
    @Tag(SCHED9)
    @Tag("ABCHR3532-7")
    public void editOvertimeRequest() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        EmployeePosition employeePosition = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        LocalDateTime requestDayTime = PresetClass.getFreeDateForEmployeeShiftPreset(employeePosition).atTime(10, 0);
        ScheduleRequestType requestType = ScheduleRequestType.OVERTIME;
        int rosterId = RosterRepository.getActiveRosterThisMonth(omId).getId();
        OutsidePlanResource overtimeRequest = PresetClass.createOutsidePlanResource(requestType,
                                                                                    requestDayTime, employeePosition, rosterId);
        List<OutsidePlanResource> overtimeBefore = OutsidePlanResourceRepository.getAllOutsideResources(rosterId, ShiftTimePosition.ALLMONTH);
        goToSchedule(unit);
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        clickOutsidePlanResourceElement(requestType, scheduleWorker, employeePosition, requestDayTime.toLocalDate());
        LocalDateTime endTime = overtimeRequest.getDateTimeInterval().getEndDateTime().minusMinutes(30);
        enterShiftDateStartOrEnd(endTime.toLocalDate(), DateTypeField.END_DATE);
        enterShiftTimeStartOrEnd(endTime.toLocalTime(), TimeTypeField.END_TIME);
        clickEditShiftButton();
        asserEditOvertime(overtimeRequest, overtimeBefore, endTime, rosterId, employeePosition);
    }

    @Ignore(NOT_ACTUAL)
    @Test(groups = {"ABCHR3621"},
            description = "Удаление смены типа Дежурство")
    @TmsLink("60318")
    @Link(name = "Статья: \"3621_Дежурства сотрудников\"", url = "https://wiki.goodt.me/x/WRktD")
    @Tag(SCHED9)
    @Tag("ABCHR3621-2")
    public void deleteOnDutyRequest() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        EmployeePosition employeePosition = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        LocalDate dutyDate = PresetClass.getFreeDateForEmployeeShiftPreset(employeePosition);
        int rosterId = RosterRepository.getActiveRosterThisMonth(omId).getId();
        ScheduleRequestType requestType = ScheduleRequestType.ON_DUTY;
        OutsidePlanResource onDutyRequest = PresetClass.createOutsidePlanResource(requestType, dutyDate.atTime(10, 0),
                                                                                  employeePosition, rosterId);
        goToSchedule(unit);
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        clickOutsidePlanResourceElement(requestType, scheduleWorker, employeePosition, dutyDate);
        shiftThreeDotsClick();
        selectAction(RequestAction.DELETE, false);
        assertDeleteOutsidePlanResource(employeePosition, requestType, onDutyRequest, scheduleWorker, rosterId, dutyDate);
    }

    @Test(groups = {"ABCHR5762", "ABCHR5762-1", X5},
            dataProvider = "true/false", dataProviderClass = DataProviders.class,
            description = "Отображение фактической и плановой смены в одной ячейке (объединение графиков включено)")
    @Link(name = "Статья: \"5762 Реализовать единый интерфейс отображения план/факт\"", url = "https://wiki.goodt.me/x/BTv0DQ")
    @TmsLink("60323")
    public void systemPropertyDictatesIfWorkedAndPlannedShiftsShouldBeMergedOnUI(boolean propertyValue) {
        changeProperty(SystemProperties.SCHEDULE_BOARD_PLAN_FACT_MERGE, propertyValue);
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitWithWorkedRosters(true);
        goToSchedule(unit);
        if (propertyValue) {
            assertMergedUiForShiftsIsActive(unit.getId());
        } else {
            Allure.getLifecycle().updateTestCase(r -> r.setDescription("Отображение фактической и плановой смены в одной ячейке (объединение графиков выключено)"));
            assertUnmergedUiForShiftsIsActive(unit.getId());
        }
    }

    @Ignore(NOT_ACTUAL)
    @Test(groups = {"ABCHR5762", X5,
            "@Before disable all shift comments", "@Before enable check of worked shifts against plan"},
            dataProvider = "true/false", dataProviderClass = DataProviders.class,
            description = "Подсвечивание красным цветом фактической смены, если она отличается от плановой (объединение графиков включено)")
    @Link(name = "Статья: \"5762 Реализовать единый интерфейс отображения план/факт\"", url = "https://wiki.goodt.me/x/BTv0DQ")
    @TmsLink("60323")
    public void workedShiftWithoutPlannedShiftIsHighligthedInRed(boolean propertyValue) {
        changeProperty(SystemProperties.SCHEDULE_BOARD_PLAN_FACT_MERGE, propertyValue);
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitWithWorkedRosters(true);
        int omId = unit.getId();
        PresetClass.changeOrSetMathParamValue(omId, MathParameterValues.ACCESS_TO_OVERWORK, true, true);
        EmployeePosition ep = getRandomFromList(EmployeePositionRepository.getEmployeePositions(omId));
        Shift shift = PresetClass.defaultShiftPreset(ep, ShiftTimePosition.PAST);

        goToSchedule(unit);
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        LocalDate date = shift.getDateTimeInterval().getStartDate();
        AtlasWebElement actualShiftElement = scheduleWorker.getScheduleShiftElement(ep, date);
        Assert.assertNotNull(actualShiftElement, String.format("Не найден элемент смены сотрудника %s за %s", ep, date));
        String planClass = null;
        if (propertyValue) {
            String actualClass = scheduleWorker.getInternalClass(actualShiftElement);
            if (actualClass.contains("red")) {
                Assert.fail("Выбранная фактическая смена уже выделена красным");
            }
            AtlasWebElement planShiftElement = scheduleWorker.getPlanOrFactShiftElement(ep, date, false);
            planClass = scheduleWorker.getInternalClass(planShiftElement);
        }
        clickShiftElement(actualShiftElement, date, ep);
        LocalDateTime newStart = shift.getDateTimeInterval().getStartDate().atTime(2, 30);
        enterShiftTimeStartOrEnd(newStart.toLocalTime(), TimeTypeField.START_TIME);
        LocalDateTime newEnd = newStart.plusHours(6);
        enterShiftDateStartOrEnd(newEnd.toLocalDate(), DateTypeField.END_DATE);
        enterShiftTimeStartOrEnd(newEnd.toLocalTime(), TimeTypeField.END_TIME);
        clickEditShiftButton();
        if (propertyValue) {
            assertMergedUiForShiftsChangesColors(planClass, scheduleWorker, ep, omId, new DateTimeInterval(newStart, newEnd));
        } else {
            Allure.getLifecycle().updateTestCase(r -> r.setDescription("Подсвечивание красным цветом фактической смены, если она отличается от плановой (объединение графиков выключено)"));
            assertUnmergedUIForShiftsChangesColors(scheduleWorker, ep, omId, new DateTimeInterval(newStart, newEnd));
        }
    }

    @Ignore(NOT_ACTUAL)
    @Test(groups = {"ABCHR3739-1",
            "@Before enable schedule request: on duty", "@Before disable merged view for planned and actual shifts",
            "@Before disable mandatory comment when editing or deleting shift"},
            description = "Создание пользователем смены типа Дежурство (с разрешением)",
            dataProvider = "roles 1, 4", dataProviderClass = DataProviders.class)
    @TmsLink("60326")
    @Link(name = "Статья: \"3739_Добавить права на сверхурочную работу и дежурства\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=204283144")
    @Tag(SCHED9)
    @Tag("ABCHR3739-1")
    public void createOnDutyRequestWithPermissions(Role role) {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        Roster roster = RosterRepository.getActiveRosterThisMonth(omId);
        int rosterId = roster.getId();
        EmployeePosition employeePosition = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, false);
        LocalDate date = PresetClass.getFreeDateForEmployeeShiftPreset(employeePosition, ShiftTimePosition.FUTURE);
        List<Shift> shifts = ShiftRepository.getShifts(employeePosition, ShiftTimePosition.ALLMONTH);
        if (shifts.isEmpty()) {
            PresetClass.presetForMakeShiftDate(employeePosition, date.minusDays(1), false, ShiftTimePosition.ALLMONTH, shifts);
        }
        goToScheduleAsUser(role, unit);
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        clickOnEmptyCell(employeePosition, date, scheduleWorker);
        clickOnPlusCellOnGraph();
        ScheduleRequestType requestType = ScheduleRequestType.ON_DUTY;
        clickOnSelectRequestTypeButton();
        selectRequestType(requestType);
        int duration = new Random().nextInt(4) + 1;
        LocalTime start = LocalTime.of(10, 0);
        enterShiftTimeStartOrEnd(start, TimeTypeField.START_TIME);
        LocalTime end = start.plusHours(duration);
        enterShiftTimeStartOrEnd(end, TimeTypeField.END_TIME);
        List<OutsidePlanResource> onDutyBefore = OutsidePlanResourceRepository.getAllOutsideResources(rosterId, ShiftTimePosition.ALLMONTH);
        clickEditShiftButton();
        assertAddOnDuty(employeePosition, scheduleWorker, onDutyBefore,
                        omId, new DateTimeInterval(LocalDateTime.of(date, start), LocalDateTime.of(date, end)));
    }

    @Test(groups = {"ABCHR3739",
            "@Before enable schedule request: on duty", "@Before disable mandatory comment when editing or deleting shift"},
            description = "Создание пользователем смены типа Дежурство (без разрешения)",
            dataProvider = "roles 3, 5", dataProviderClass = DataProviders.class,
            expectedExceptions = AssertionError.class, expectedExceptionsMessageRegExp = ANY + REQUEST_OPTION_NOT_DISPLAYED + ANY)
    @TmsLink("60326")
    @Link(name = "Статья: \"3739_Добавить права на сверхурочную работу и дежурства\"", url = "https://wiki.goodt.me/x/CB0tD")
    @Tag(SCHED9)
    @Tag("ABCHR3739-1")
    public void createOnDutyRequestWithoutPermissions(Role role) {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        EmployeePosition employeePosition = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        LocalDate date = PresetClass.getFreeDateForEmployeeShiftPreset(employeePosition, ShiftTimePosition.FUTURE);
        goToScheduleAsUser(role, unit);
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        clickOnEmptyCell(employeePosition, date, scheduleWorker);
        clickOnPlusCellOnGraph();
        ScheduleRequestType requestType = ScheduleRequestType.ON_DUTY;
        clickOnSelectRequestTypeButton();
        selectRequestType(requestType);
    }

    @Ignore(NOT_ACTUAL)
    @Test(groups = {"ABCHR3739-2",
            "@Before disable schedule request: overtime", "@Before disable mandatory comment when editing or deleting shift",
            "@Before enable overtime access", "@Before disable merged view for planned and actual shifts"},
            description = "Создание пользователем смены типа Сверхурочная работа (с разрешением)",
            dataProvider = "roles 1, 5", dataProviderClass = DataProviders.class)
    @TmsLink("60326")
    @Link(name = "Статья: \"3739_Добавить права на сверхурочную работу и дежурства\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=204283144")
    @Tag(SCHED9)
    @Tag("ABCHR3739-2")
    public void createOvertimeRequestWithPermissions(Role role) {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        Roster roster = RosterRepository.getActiveRosterThisMonth(omId);
        int rosterId = roster.getId();
        EmployeePosition employeePosition = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, false);
        LocalDate date = PresetClass.getFreeDateForEmployeeShiftPreset(employeePosition, ShiftTimePosition.FUTURE);
        ShiftTimePosition timePosition = ShiftTimePosition.ALLMONTH;
        List<Shift> shifts = ShiftRepository.getShifts(employeePosition, timePosition);
        if (shifts.isEmpty()) {
            PresetClass.presetForMakeShiftDate(employeePosition, date.minusDays(1), false, timePosition, shifts);
        }
        goToScheduleAsUser(role, unit);
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        clickOnEmptyCell(employeePosition, date, scheduleWorker);
        clickOnPlusCellOnGraph();
        ScheduleRequestType requestType = ScheduleRequestType.OVERTIME;
        clickOnSelectRequestTypeButton();
        selectRequestType(requestType);
        int duration = new Random().nextInt(4) + 1;
        LocalTime start = LocalTime.of(10, 0);
        enterShiftTimeStartOrEnd(start, TimeTypeField.START_TIME);
        LocalTime end = start.plusHours(duration);
        enterShiftTimeStartOrEnd(end, TimeTypeField.END_TIME);
        List<OutsidePlanResource> overtimeBefore = OutsidePlanResourceRepository.getAllOutsideResources(rosterId, timePosition);
        clickEditShiftButton();
        assertAddOvertime(employeePosition, scheduleWorker, overtimeBefore, omId,
                          new DateTimeInterval(LocalDateTime.of(date, start), LocalDateTime.of(date, end)));
    }

    @Test(groups = {"ABCHR3739"},
            description = "Создание пользователем смены типа Сверхурочная работа/Дежурство (2 роль)",
            expectedExceptions = WaitUntilException.class, expectedExceptionsMessageRegExp = ANY + FAILED_TO_GET_PAGE + ANY)
    @TmsLink("60326")
    @Link(name = "Статья: \"3739_Добавить права на сверхурочную работу и дежурства\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=204283144")
    @Tag(SCHED9)
    @Tag("ABCHR3739-1")
    @Tag("ABCHR3739-2")
    public void createRequestSecondRole() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        Role role = Role.SECOND;
        goToScheduleAsUser(role, unit);
    }

    @Test(groups = {"ABCHR3739",
            "@Before disable mandatory comment when editing or deleting shift"},
            description = "Создание пользователем смены типа Сверхурочная работа (без разрешений)",
            expectedExceptions = AssertionError.class, expectedExceptionsMessageRegExp = ANY + REQUEST_OPTION_NOT_DISPLAYED + ANY,
            dataProvider = "roles 3, 4", dataProviderClass = DataProviders.class)
    @TmsLink("60326")
    @Tag("ABCHR3739-2")
    @Tag(SCHED9)
    @Link(name = "Статья: \"3739_Добавить права на сверхурочную работу и дежурства\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=204283144")
    public void createOvertimeRequestWithoutPermissions(Role role) {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        EmployeePosition employeePosition = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        LocalDate date = PresetClass.getFreeDateForEmployeeShiftPreset(employeePosition, ShiftTimePosition.FUTURE);
        goToScheduleAsUser(role, unit);
        clickOnEmptyCell(employeePosition, date, new ScheduleWorker(sb));
        clickOnPlusCellOnGraph();
        ScheduleRequestType requestType = ScheduleRequestType.OVERTIME;
        clickOnSelectRequestTypeButton();
        selectRequestType(requestType);
    }

    @Ignore("Заменен на api-тест")
    @Test(groups = {"ABCHR3900", G1, SHIFTS, SCHED9,
            "@Before disable check of worked roster before adding shift", "@Before forbid roster edits in past",
            "@Before disallow timesheet editing for past months", "@Before disable all shift comments",
            "@Before disable merged view for planned and actual shifts"},
            dataProvider = "true/false", dataProviderClass = DataProviders.class,
            description = "Создание смены в табеле")
    @Link(name = "Статья: \"3900_Добавить права на редактирование табеля за прошлое\"", url = "https://wiki.goodt.me/x/RwHND")
    @TmsLink("60329")
    @Tag("ABCHR3900-1")
    @Tag(SCHED9)
    private void addShiftInWorkedRoster(boolean hasAccess) {
        checkFirstDayOfMonth();
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitWithWorkedRosters(true);
        int omId = unit.getId();
        PresetClass.changeOrSetMathParamValue(omId, MathParameterValues.ACCESS_TO_OVERWORK, true, true);
        PresetClass.deleteEntityPropertyIfPresent(omId, OrgUnitAttributes.ORG_UNIT_FORMAT.getKey());
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, false);
        List<PermissionType> permissions = new ArrayList<>(Arrays.asList(PermissionType.SCHEDULE_VIEW,
                                                                         PermissionType.SCHEDULE_EDIT,
                                                                         PermissionType.ORG_UNIT_EMPLOYEE_PERSONALISATION));
        if (hasAccess) {
            permissions.add(PermissionType.SCHEDULE_EDIT_WORKED);
        }
        Role role = PresetClass.createCustomPermissionRole(permissions);
        LocalDate date = PresetClass.getFreeDateForEmployeeShiftPreset(ep, ShiftTimePosition.PAST);
        goToScheduleAsUser(role, unit);
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        clickOnEmptyCell(ep, date, new ScheduleWorker(sb));
        if (hasAccess) {
            clickOnPlusCellOnGraph();
            LocalTime start = LocalTime.of(10, 0, 0);
            LocalTime end = LocalTime.of(18, 0, 0);
            enterShiftTimeStartOrEnd(start, TimeTypeField.START_TIME);
            enterShiftTimeStartOrEnd(end, TimeTypeField.END_TIME);
            createShiftButtonClick();
            assertCreateShift(ep, new DateTimeInterval(date.atTime(start),
                                                       date.atTime(end)), scheduleWorker, false);
        } else {
            Assert.assertThrows(WaitUntilException.class, this::clickOnPlusCellOnGraph);
        }
    }

    @Test(groups = {"ABCHR3900", G1, SHIFTS, SCHED9,
            "@Before disable check of worked roster before adding shift",
            "@Before forbid roster edits in past", "@Before disable all shift comments",
            "@Before disallow timesheet editing for past months",
            "@Before disable start time check for worked shifts",
            "@Before disable check of worked roster before adding shift"},
            dataProvider = "true/false", dataProviderClass = DataProviders.class,
            description = "Редактирование смены в табеле")
    @Link(name = "Статья: \"3900_Добавить права на редактирование табеля за прошлое\"", url = "https://wiki.goodt.me/x/RwHND")
    @TmsLink("60329")
    @Tag("ABCHR3900-2")
    @Tag(SCHED9)
    private void editShiftInWorkedRoster(boolean hasAccess) {
        checkFirstDayOfMonth();
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitWithWorkedRosters(true);
        int omId = unit.getId();
        PresetClass.changeOrSetMathParamValue(omId, MathParameterValues.ACCESS_TO_OVERWORK, true, true);
        PresetClass.deleteEntityPropertyIfPresent(omId, OrgUnitAttributes.ORG_UNIT_FORMAT.getKey());
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        List<PermissionType> permissions = new ArrayList<>(Arrays.asList(PermissionType.SCHEDULE_VIEW,
                                                                         PermissionType.SCHEDULE_EDIT,
                                                                         PermissionType.ORG_UNIT_EMPLOYEE_PERSONALISATION));
        if (hasAccess) {
            permissions.add(PermissionType.SCHEDULE_EDIT_WORKED);
        }
        Role role = PresetClass.createCustomPermissionRole(permissions);
        Shift shift = PresetClass.defaultShiftPreset(ep, ShiftTimePosition.PAST);
        LocalDate date = shift.getDateTimeInterval().getStartDate();
        goToScheduleAsUser(role, unit);
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        clickOnTargetShift(ep, date, scheduleWorker);
        LocalTime startTime = shift.getDateTimeInterval().toTimeInterval().getStartTime();
        LocalTime start = startTime.getHour() >= 16 ? startTime.minusHours(1) : startTime.plusHours(1);
        if (hasAccess) {
            enterShiftTimeStartOrEnd(start, TimeTypeField.START_TIME);
            LocalTime endTime = shift.getDateTimeInterval().toTimeInterval().getEndTime();
            LocalTime end = endTime.getHour() >= 22 ? endTime.minusHours(1) : endTime.plusHours(1);
            enterShiftTimeStartOrEnd(end, TimeTypeField.END_TIME);
            clickEditShiftButton();
            assertEditShift(ep, new DateTimeInterval(date.atTime(start),
                                                     date.atTime(end)), scheduleWorker);
        } else {
            Assert.assertThrows(ElementNotInteractableException.class, () ->
                    enterShiftTimeStartOrEnd(start, TimeTypeField.START_TIME));
        }
    }

    @Ignore("Заменен на api-тест")
    @Test(groups = {"ABCHR3900", G1, SHIFTS, SCHED9,
            "@Before disable check of worked roster before adding shift", "@Before forbid roster edits in past",
            "@Before disallow timesheet editing for past months", "@Before disable all shift comments"},
            dataProvider = "true/false", dataProviderClass = DataProviders.class,
            description = "Копирование смены в табеле")
    @Link(name = "Статья: \"3900_Добавить права на редактирование табеля за прошлое\"", url = "https://wiki.goodt.me/x/RwHND")
    @TmsLink("60329")
    @Tag("ABCHR3900-3")
    @Tag(SCHED9)
    private void copyShiftInWorkedRoster(boolean hasAccess) {
        checkFirstDayOfMonth();
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitWithWorkedRosters(true);
        int omId = unit.getId();
        PresetClass.changeOrSetMathParamValue(omId, MathParameterValues.ACCESS_TO_OVERWORK, true, true);
        PresetClass.deleteEntityPropertyIfPresent(omId, OrgUnitAttributes.ORG_UNIT_FORMAT.getKey());
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, false);
        List<PermissionType> permissions = new ArrayList<>(Arrays.asList(PermissionType.SCHEDULE_VIEW,
                                                                         PermissionType.SCHEDULE_EDIT,
                                                                         PermissionType.ORG_UNIT_EMPLOYEE_PERSONALISATION));
        if (hasAccess) {
            permissions.add(PermissionType.SCHEDULE_EDIT_WORKED);
        }
        Role role = PresetClass.createCustomPermissionRole(permissions);
        Shift shift = PresetClass.defaultShiftPreset(ep, ShiftTimePosition.PAST);
        LocalDateTime startDateTime = shift.getDateTimeInterval().getStartDateTime();
        LocalDate date = startDateTime.toLocalDate();
        LocalDate freeDate = PresetClass.getFreeDateForEmployeeShiftPreset(ep, ShiftTimePosition.PAST, date);
        goToScheduleAsUser(role, unit);
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        if (hasAccess) {
            transferOneShift(ep, ep, startDateTime, freeDate.atStartOfDay(), scheduleWorker);
            clickDuplicateShiftButton();
            assertDuplicateShift(ep, shift, ep, freeDate, scheduleWorker);
        } else {
            Assert.assertThrows(WaitUntilException.class, () ->
                    transferOneShift(ep, ep, startDateTime, freeDate.atStartOfDay(), scheduleWorker));
        }
    }

    @Ignore("Заменен на api-тест")
    @Test(groups = {"ABCHR3900", G1, SHIFTS, SCHED9,
            "@Before disable check of worked roster before adding shift", "@Before forbid roster edits in past",
            "@Before disallow timesheet editing for past months", "@Before disable all shift comments",
            "@Before disable start time check for worked shifts"},
            dataProvider = "true/false", dataProviderClass = DataProviders.class,
            description = "Перемещение смены в табеле")
    @Link(name = "Статья: \"3900_Добавить права на редактирование табеля за прошлое\"", url = "https://wiki.goodt.me/x/RwHND")
    @TmsLink("60329")
    @Tag("ABCHR3900-4")
    @Tag(SCHED9)
    private void moveShiftInWorkedRoster(boolean hasAccess) {
        checkFirstDayOfMonth();
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitWithWorkedRosters(true);
        int omId = unit.getId();
        PresetClass.changeOrSetMathParamValue(omId, MathParameterValues.ACCESS_TO_OVERWORK, true, true);
        PresetClass.deleteEntityPropertyIfPresent(omId, OrgUnitAttributes.ORG_UNIT_FORMAT.getKey());
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, false);
        List<PermissionType> permissions = new ArrayList<>(Arrays.asList(PermissionType.SCHEDULE_VIEW,
                                                                         PermissionType.SCHEDULE_EDIT,
                                                                         PermissionType.ORG_UNIT_EMPLOYEE_PERSONALISATION));
        if (hasAccess) {
            permissions.add(PermissionType.SCHEDULE_EDIT_WORKED);
        }
        Role role = PresetClass.createCustomPermissionRole(permissions);
        Shift shift = PresetClass.defaultShiftPreset(ep, ShiftTimePosition.PAST);
        LocalDateTime startDateTime = shift.getDateTimeInterval().getStartDateTime();
        LocalDate date = startDateTime.toLocalDate();
        LocalDate freeDate = PresetClass.getFreeDateForEmployeeShiftPreset(ep, ShiftTimePosition.PAST, date);
        goToScheduleAsUser(role, unit);
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        if (hasAccess) {
            transferOneShift(ep, ep, startDateTime, freeDate.atStartOfDay(), scheduleWorker);
            clickReplaceShiftButton();
            assertTransferShiftToSameEmployee(shift, ep, ep, freeDate, scheduleWorker);
        } else {
            Assert.assertThrows(WaitUntilException.class, () ->
                    transferOneShift(ep, ep, startDateTime, freeDate.atStartOfDay(), scheduleWorker));
        }
    }

    @Test(groups = {"ABCHR3900", G1, SHIFTS, SCHED9,
            "@Before disable check of worked roster before adding shift", "@Before forbid roster edits in past",
            "@Before disallow timesheet editing for past months", "@Before disable all shift comments",
            "@Before disable mandatory comments when deleting worked shift"},
            dataProvider = "true/false", dataProviderClass = DataProviders.class,
            description = "Удаление смены в табеле")
    @Link(name = "Статья: \"3900_Добавить права на редактирование табеля за прошлое\"", url = "https://wiki.goodt.me/x/RwHND")
    @TmsLink("60329")
    @Tag("ABCHR3900-5")
    @Tag(SCHED9)
    private void deleteShiftInWorkedRoster(boolean hasAccess) {
        checkFirstDayOfMonth();
        List<PermissionType> permissions = new ArrayList<>(Arrays.asList(PermissionType.SCHEDULE_VIEW,
                                                                         PermissionType.SCHEDULE_EDIT,
                                                                         PermissionType.ORG_UNIT_EMPLOYEE_PERSONALISATION));
        if (hasAccess) {
            permissions.add(PermissionType.SCHEDULE_EDIT_WORKED);
        }
        Role role = PresetClass.createCustomPermissionRole(permissions);
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitWithWorkedRosters(true);
        int omId = unit.getId();
        PresetClass.changeOrSetMathParamValue(omId, MathParameterValues.ACCESS_TO_OVERWORK, true, true);
        PresetClass.deleteEntityPropertyIfPresent(omId, OrgUnitAttributes.ORG_UNIT_FORMAT.getKey());
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        Shift shift = PresetClass.defaultShiftPreset(ep, ShiftTimePosition.PAST);
        LocalDate date = shift.getDateTimeInterval().getStartDate();
        goToScheduleAsUser(role, unit);
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        clickOnTargetShift(ep, date, scheduleWorker);
        if (hasAccess) {
            shiftThreeDotsClick();
            selectAction(RequestAction.DELETE, false);
            assertDeleteShift(ep, shift, scheduleWorker);
        } else {
            tryToDeleteShiftWithoutPermission();
        }
    }

    @Ignore("Временно исключен из прогона, т.к. на стендах нет данных для вывода индикатора")
    @Test(groups = {"ABCHR2861-1", G1, SCHED2,
            "@Before enable front/back indicators"},
            description = "Отображение РЗ front/back")
    @Link(name = "Статья: \"2861 Почта России Индикатор front/back в расписании\"", url = "https://wiki.goodt.me/x/FQ0tD")
    @TmsLink("60335")
    @Tag("ABCHR2861-1")
    @Tag(SCHED2)
    private void displayFrontAndBackIndicators() {
        int omId = 221962;
        OrgUnit unit = OrgUnitRepository.getOrgUnit(omId);
        goToSchedule(unit);
        clickBackToFTE();
        List<ButtonIDropDownMenu> items = Arrays.asList(ButtonIDropDownMenu.FRONT_INDICATOR,
                                                        ButtonIDropDownMenu.BACK_INDICATOR);
        enableIndicatorsIfDisabled(items);
        assertDisplayedIndicator(items);
    }

    @Test(groups = {"ABCHR3014-1", G1, SCHED7},
            description = "Выбор нескольких групп в фильтре сотрудников")
    @Link(name = "Статья: \"3014_В расписании добавить возможность массово выбирать фильтры по группам\"",
            url = "https://wiki.goodt.me/pages/viewpage.action?pageId=204279882")
    @TmsLink("60336")
    @Tag("ABCHR3014-1")
    @Tag(SCHED7)
    public void massGroupFilter() {
        OrgUnit unit = OrgUnitRepository.getRandomOmWithEmployeeOptions(OrgUnitOptions.WITH_POSITION_GROUPS);
        goToSchedule(unit);
        employeeFilterButtonClick();
        List<EmployeePosition> employeePositions = EmployeePositionRepository.getEmployeePositions(unit.getId());
        Map<Integer, List<String>> groupsWithEmployees = getGroupEmployeesMap(employeePositions);
        ImmutablePair<Integer, List<String>> firstGroup = getEmployeesListFromRandomGroup(groupsWithEmployees);
        List<String> employees = firstGroup.right;
        pickFunctionalRoleFromEmployeeFilter(getPositionGroups().get(firstGroup.left));
        groupsWithEmployees.remove(firstGroup.left);
        ImmutablePair<Integer, List<String>> secondGroup = getEmployeesListFromRandomGroup(groupsWithEmployees);
        pickFunctionalRoleFromEmployeeFilter(getPositionGroups().get(secondGroup.left));
        filterModeApplyButtonClick();
        employees.addAll(secondGroup.right);
        checkFilterMode(employees, unit.getId());
    }

    @Test(groups = {"ABCHR3151", G1, SHIFTS, SCHED36,
            "@Before disable pre-publication checks",
            "@Before don't show button to publish roster",
            "@Before enable check of worked roster before adding shift",
            "@Before disable merged view for planned and actual shifts",
            "@Before disable start time check for worked shifts",
            "@Before disable worked shift comments"},
            description = "Создание смены в табеле при наличии смены в плановом графике")
    @TmsLink("60316")
    @Link(name = "Статья: \"3151_Запрет на создание смены в табеле, которой нет в плановом графике\"", url = "https://wiki.goodt.me/x/7AstD")
    @Tag("ABCHR3151-1")
    @Tag(SCHED36)
    public void addShiftToWorkedRosterIfPlannedShiftIsPresent() {
        checkFirstDayOfMonth();
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitWithWorkedRosters(true);
        int omId = unit.getId();
        PresetClass.changeOrSetMathParamValue(omId, MathParameterValues.ACCESS_TO_OVERWORK, true, true);
        Roster active = RosterRepository.getActiveRosterThisMonth(omId);
        LocalDate date = ShiftTimePosition.PAST.getShiftsDateInterval().getRandomDateBetween();
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        PresetClass.removeShiftFromRoster(active, ep, date);
        Shift shift = PresetClass.presetForMakeShiftDateTime(ep, date.atTime(14, 0, 0),
                                                             date.atTime(22, 0, 0), ShiftTimePosition.FUTURE);
        PresetClass.checkAndMakePublicationRoster(omId);
        PresetClass.presetForEmptyRequestCell(ep.getEmployee(), date);

        goToSchedule(unit);
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        clickOnEmptyCell(ep, date, scheduleWorker);
        clickOnPlusCellOnGraph();
        LocalTime start = shift.getDateTimeInterval().getStartDateTime().toLocalTime();
        LocalTime end = shift.getDateTimeInterval().getEndDateTime().toLocalTime();
        enterShiftTimeStartOrEnd(start, TimeTypeField.START_TIME);
        enterShiftTimeStartOrEnd(end, TimeTypeField.END_TIME);
        createShiftButtonClick();
        assertCreateShift(ep, new DateTimeInterval(date.atTime(start), date.atTime(end)),
                          scheduleWorker, false);
    }

    @Test(groups = {"ABCHR3151", G1, SHIFTS, SCHED36,
            "@Before don't show button to publish roster",
            "@Before enable check of worked roster before adding shift",
            "@Before disable merged view for planned and actual shifts",
            "@Before disable worked shift comments"},
            description = "Создание смены в табеле без смены в плановом графике")
    @Link(name = "Статья: \"3151_Запрет на создание смены в табеле, которой нет в плановом графике\"", url = "https://wiki.goodt.me/x/7AstD")
    @TmsLink("60316")
    @Tag("ABCHR3151-2")
    @Tag(SCHED36)
    public void addShiftToWorkedRosterIfNoPlannedShift() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitWithWorkedRosters(true);
        int omId = unit.getId();
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        LocalDate date = ShiftTimePosition.PAST.getShiftsDateInterval().getRandomDateBetween();
        Roster active = RosterRepository.getActiveRosterThisMonth(omId);
        PresetClass.removeShiftFromRoster(active, ep, date);
        Roster worked = RosterRepository.getWorkedRosterThisMonth(omId);
        PresetClass.removeShiftFromRoster(worked, ep, date);
        PresetClass.presetForEmptyRequestCell(ep.getEmployee(), date);
        PresetClass.checkAndMakePublicationRoster(omId);

        goToSchedule(unit);
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        clickOnEmptyCell(ep, date, scheduleWorker);
        clickOnPlusCellOnGraph();
        LocalTime start = LocalTime.of(10, 0, 0);
        LocalTime end = LocalTime.of(18, 0, 0);
        enterShiftTimeStartOrEnd(start, TimeTypeField.START_TIME);
        enterShiftTimeStartOrEnd(end, TimeTypeField.END_TIME);
        List<Shift> shiftsBefore = ShiftRepository.getShifts(ep, ShiftTimePosition.PAST);
        createShiftButtonClick();
        assertNoChangesToShifts(scheduleWorker, date, ep, shiftsBefore, null,
                                "Смена отсутствует в согласованном графике. Добавьте смену в плановый график.", ShiftTimePosition.PAST);
    }

    @Test(groups = {"TEST-176", G1, SCHED10, POCHTA,
            "@Before functional roles in badges"},
            description = "Отображение функциональной роли \"Back-оператор\"")
    @Link(name = "Отображение индикаторов сотрудников", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=178979652#id-%D0%A0%D0%B0%D1%81%D0%BF%D0%B8%D1%81%D0%B0%D0%BD%D0%B8%D0%B5-%D0%9E%D1%82%D0%BE%D0%B1%D1%80%D0%B0%D0%B6%D0%B5%D0%BD%D0%B8%D0%B5%D0%B8%D0%BD%D0%B4%D0%B8%D0%BA%D0%B0%D1%82%D0%BE%D1%80%D0%BE%D0%B2%D1%81%D0%BE%D1%82%D1%80%D1%83%D0%B4%D0%BD%D0%B8%D0%BA%D0%BE%D0%B2")
    @TmsLink("61606")
    @Tag(SCHED10)
    @Tag("TEST-176")
    public void backOperatorRole() {
        OrgUnit unit = OrgUnitRepository.getRandomOmWithEmployeeOptions(OrgUnitOptions.EMPLOYEES_WITH_POSITION);
        goToSchedule(unit);
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.SUBDIVISION_PROPERTIES);
        clickOnChevronButton(OmInfoName.EMPLOYEES);
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(unit.getId(), null, true);
        Employee employee = ep.getEmployee();

        clickOnEmployeeThreeDots(employee.getFullName());
        editButtonClick();
        clickOnFunctionalRolesSelectButton();

        PositionGroup positionGroup = PositionGroupRepository.getPositionGroupByName("back-оператор");
        selectFuncRole(positionGroup);
        saveButtonClick();
        addFuncRoleCheck(employee, positionGroup);
        orgUnitPropCloseClick();
        Assert.assertTrue(checkTag(ep.getId(), "Back-оператор", true),
                          String.format("Под сотрудником %s не появился тег \"back-оператор\"", employee));
    }

    @Test(groups = {"TEST-176.1", G1, SCHED8,
            "@Before publication without norms lack", "@Before schedule board deviation from standard plan"},
            description = "Подсветка количества часов при превышении нормы")
    @Link(name = "Отображение индикаторов сотрудников", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=178979652#id-%D0%A0%D0%B0%D1%81%D0%BF%D0%B8%D1%81%D0%B0%D0%BD%D0%B8%D0%B5-%D0%9E%D1%82%D0%BE%D0%B1%D1%80%D0%B0%D0%B6%D0%B5%D0%BD%D0%B8%D0%B5%D0%B8%D0%BD%D0%B4%D0%B8%D0%BA%D0%B0%D1%82%D0%BE%D1%80%D0%BE%D0%B2%D1%81%D0%BE%D1%82%D1%80%D1%83%D0%B4%D0%BD%D0%B8%D0%BA%D0%BE%D0%B2")
    @TmsLink("61606")
    @Tag(SCHED8)
    @Tag("TEST-176.1")
    public void highlightOfExcessHours() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        PresetClass.changeOrSetMathParamValue(omId, MathParameterValues.ACCESS_TO_OVERWORK, true, true);
        EmployeePosition position = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        PresetClass.presetForDeviationExcess(unit.getId(), position);
        goToSchedule(unit);
        assertDeviationExcess(unit, position);
    }

    @Test(groups = {"TEST-176.1", G1, SCHED8,
            "@Before publication without norms lack", "@Before schedule board deviation from standard plan",
            "@Before disable pre-publication checks"},
            description = "Подсветка количества часов при нехватке нормы")
    @Link(name = "Отображение индикаторов сотрудников", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=178979652#id-%D0%A0%D0%B0%D1%81%D0%BF%D0%B8%D1%81%D0%B0%D0%BD%D0%B8%D0%B5-%D0%9E%D1%82%D0%BE%D0%B1%D1%80%D0%B0%D0%B6%D0%B5%D0%BD%D0%B8%D0%B5%D0%B8%D0%BD%D0%B4%D0%B8%D0%BA%D0%B0%D1%82%D0%BE%D1%80%D0%BE%D0%B2%D1%81%D0%BE%D1%82%D1%80%D1%83%D0%B4%D0%BD%D0%B8%D0%BA%D0%BE%D0%B2")
    @TmsLink("61606")
    @Tag(SCHED8)
    @Tag("TEST-176.1")
    public void highlightOfLackHours() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitWithPublishedRoster();
        int omId = unit.getId();
        EmployeePosition position = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        PresetClass.presetForDeviationLack(omId, position);
        goToSchedule(unit);
        assertDeviationLack(unit, position);
    }

    @Test(groups = {"ABCHR3006-1", G1, SCHED10},
            description = "Отображение параметра должности \"Ночной сотрудник\"")
    @Link(name = "Статья: \"3006, 3398_Отображение наименования группы пользователя в расписании\"",
            url = "https://wiki.goodt.me/pages/viewpage.action?pageId=204280183")
    @Tag(SCHED10)
    @TmsLink("60341")
    @Tag("ABCHR3006-1")
    public void setEmployeePositionParameter() {
        OrgUnit unit = OrgUnitRepository.getRandomOmWithEmployeeOptions(OrgUnitOptions.EMPLOYEES_WITH_POSITION);
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(unit.getId(), null, false);
        PresetClass.checkEmployeeParams(ep.getEmployee().getId(), MathParameters.NIGHT_EMPLOYEE.getMathParamId(), MathParameters.NIGHT_EMPLOYEE.getNameParam());
        goToSchedule(unit);
        clickOnEmployeeNameButton(ep);
        clickOnParametersPencilButton(EmployeeInfoName.OPTIONS);
        clickOnMathParameterChevronButton(MathParameters.NIGHT_EMPLOYEE);
        chooseVariant(VariantsInMathParameters.ON);
        saveDataInParamsForm();
        refreshPageAndAcceptAlertWindow();
        assertEmployeeTag(ep, MathParameters.NIGHT_EMPLOYEE);
    }

    @Test(groups = {"ABCHR4315", G1, SCHED9},
            description = "Редактирование подтвержденного запроса отсутствия при наличии разрешения на этот тип запроса")
    @Link(name = "Статья: \"4315_Пользователь может редактировать только те типы запросов, на которые у него есть права\"",
            url = "https://wiki.goodt.me/x/lIBSDQ")
    @TmsLink("60309")
    @Tag(SCHED9)
    @Tag("ABCHR4315-1")
    private void editApprovedAbsenceRequestWithPermissions() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        Role role = PresetClass.createCustomPermissionRole(Arrays.asList(PermissionType.SCHEDULE_VIEW,
                                                                         PermissionType.SCHEDULE_EDIT,
                                                                         PermissionType.SCHEDULE_MANAGE_REQUESTS,
                                                                         PermissionType.SCHEDULE_MANAGE_REQUESTS_APPROVE,
                                                                         PermissionType.ORG_UNIT_EMPLOYEE_PERSONALISATION
        ));
        ScheduleRequestType type = ScheduleRequestType.VACATION;
        PresetClass.addScheduleRequestTypeRights(role, type);
        ScheduleRequest request = PresetClass.createScheduleRequestOfCertainType(ScheduleRequestStatus.APPROVED,
                                                                                 false, unit.getId(), type);
        assert request != null;
        EmployeePosition ep = EmployeePositionRepository.getEmployeePosition(request.getEmployee().getFullName(), unit.getId());
        LocalDate newDate = ShiftTimePosition.FUTURE.getShiftsDateInterval().getRandomDateBetween();
        PresetClass.makeClearDate(ep, newDate);
        goToScheduleAsUser(role, unit);
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        clickOnRequestBlock(request, scheduleWorker, unit);
        enterShiftDateStartOrEnd(newDate, DateTypeField.START_DATE);
        if (sb.formEditForm().dateStartOrEndInput(DateTypeField.END_DATE.getName()).isDisplayed()) {
            enterShiftDateStartOrEnd(newDate, DateTypeField.END_DATE);
        }
        clickEditShiftButton();
        assertRequestChange(request, newDate, scheduleWorker, unit, ScheduleRequestStatus.APPROVED);
    }

    @Test(groups = {"ABCHR4315", G1, SCHED9},
            description = "Удаление подтвержденного запроса отсутствия при наличии разрешения на этот тип запроса")
    @Link(name = "Статья: \"4315_Пользователь может редактировать только те типы запросов, на которые у него есть права\"",
            url = "https://wiki.goodt.me/x/lIBSDQ")
    @TmsLink("60309")
    @Tag(SCHED9)
    @Tag("ABCHR4315-2")
    private void deleteApprovedAbsenceRequestWithPermissions() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        Role role = PresetClass.createCustomPermissionRole(Arrays.asList(PermissionType.SCHEDULE_VIEW,
                                                                         PermissionType.SCHEDULE_EDIT,
                                                                         PermissionType.SCHEDULE_MANAGE_REQUESTS,
                                                                         PermissionType.SCHEDULE_MANAGE_REQUESTS_APPROVE,
                                                                         PermissionType.ORG_UNIT_EMPLOYEE_PERSONALISATION
        ));
        ScheduleRequestType type = ScheduleRequestType.getRandomAbsenceRequest();
        PresetClass.addScheduleRequestTypeRights(role, type);
        ScheduleRequest request = PresetClass.createScheduleRequestOfCertainType(ScheduleRequestStatus.APPROVED,
                                                                                 false, unit.getId(), type);
        assert request != null;
        goToScheduleAsUser(role, unit);
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        clickOnRequestBlock(request, scheduleWorker, unit);
        listOfRequestsThreeDotsButtonClick();
        selectAction(RequestAction.DELETE, false);
        assertRequestDeleting(request, scheduleWorker, unit);
    }

    @Test(groups = {"ABCHR4315", G1, SCHED9},
            description = "Редактирование подтвержденного запроса отсутствия без разрешения на этот тип запроса",
            expectedExceptions = WaitUntilException.class, expectedExceptionsMessageRegExp = ANY + THREE_DOTS_BUTTON_NOT_DISPLAYED_ON_SHIFT_EDIT_SCREEN + ANY)
    @Link(name = "Статья: \"4315_Пользователь может редактировать только те типы запросов, на которые у него есть права\"",
            url = "https://wiki.goodt.me/x/lIBSDQ")
    @TmsLink("60309")
    @Tag(SCHED9)
    @Tag("ABCHR4315-3")
    private void deleteApprovedAbsenceRequestWithoutPermissions() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        ScheduleRequestType type = ScheduleRequestType.getRandomAbsenceRequest();
        ScheduleRequest request = PresetClass.createScheduleRequestOfCertainType(ScheduleRequestStatus.APPROVED,
                                                                                 false, unit.getId(), type);
        Role role = PresetClass.createCustomPermissionRole(Arrays.asList(PermissionType.SCHEDULE_VIEW,
                                                                         PermissionType.SCHEDULE_EDIT,
                                                                         PermissionType.SCHEDULE_MANAGE_REQUESTS,
                                                                         PermissionType.SCHEDULE_MANAGE_REQUESTS_APPROVE,
                                                                         PermissionType.ORG_UNIT_EMPLOYEE_PERSONALISATION
        ));
        PresetClass.revokeScheduleRequestTypeRights(role, type);
        assert request != null;
        goToScheduleAsUser(role, unit);
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        clickOnRequestBlock(request, scheduleWorker, unit);
        listOfRequestsThreeDotsButtonClick();
    }

    @Test(groups = {"ABCHR4464-1", G1, SCHED8},
            description = "Переход из Расписания в другой раздел при неопубликованном графике")
    @Link(name = "Ссылка на тест-кейс", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=217714815")
    @TmsLink("60339")
    @Tag(SCHED8)
    @Tag("ABCHR4464-1")
    public void reminderWindowPopsUpIfWebPageChanges() {
        changeProperty(SystemProperties.ROSTER_QUIT_TAB_NOTICE, true);
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        PresetClass.publishGraphPreset(GraphStatus.NOT_PUBLISH, orgUnit);
        goToSchedule(orgUnit);
        clickSectionSelectionMenuOnPageHeader();
        AtlasWebElement partitionElement = takeRandomWebElementFromSectionSelectionMenu(Arrays.asList(
                "Расписание",
                "Учёт рабочего времени",
                "Выход",
                "Личный табель учета",
                "Служба поддержки"));
        String sectionName = partitionElement.getText();
        clickItemFromSectionsMenu(partitionElement, sectionName);
        pressButtonQuit();
        assertGoToDesiredSection(sectionName);
        refreshPageAndAcceptAlertWindow();
    }

    @Test(groups = {"ABCHR4464-4", G1, SCHED8},
            description = "Переход на расписание другого подразделения при неопубликованном графике")
    @Link(name = "Ссылка на тест-кейс", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=217714815")
    @TmsLink("60339")
    @Tag(SCHED8)
    @Tag("ABCHR4464-4")
    public void reminderWindowPopsUpIfNewScheduleChanges() {
        changeProperty(SystemProperties.ROSTER_QUIT_TAB_NOTICE, true);
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        PresetClass.editTypeOfTimeSheetFormation(orgUnit.getId(), "FROM_PLAIN");
        PresetClass.publishGraphPreset(GraphStatus.NOT_PUBLISH, orgUnit);
        goToSchedule(orgUnit);
        clickOnSelectStoreButton();
        sb.formTopBar().storesList().
                waitUntil(Matchers.hasSize(Matchers.greaterThan(1)));
        List<AtlasWebElement> stores = sb.formTopBar().storesList();
        AtlasWebElement webElement = getRandomFromList(stores);
        String orgUnitName = webElement.getText();
        clickOrgUnit(webElement, orgUnitName);
        pressButtonQuit();
        checkTransitionToOrgUnit(orgUnitName);
        refreshPageAndAcceptAlertWindow();
    }

    @Test(groups = {"ABCHR4537-1.1", "ABCHR4537-1", G2, SCHED32,
            "@Before allow free shifts for own employees",
            //"@Before check if last day of month",
            "@Before disable pre-publication checks"},
            description = "Выбор сотрудников, доступных для назначения на смену с биржи (в рамках одного подразделения)",
            dataProvider = "employees for free shifts in same orgUnit")
    @Link(name = "Статья: \"4537_При добавлении смены на биржу проверять и активную и опубликованную версии\"", url = "https://wiki.goodt.me/x/GRX6D")
    @TmsLink("60301")
    @Tag("ABCHR4537-1")
    @Tag(SCHED32)
    @Owner(SCHASTLIVAYA)
    public void viewEmployeesAvailableForAssignmentOfFreeShiftSameOrgUnit(boolean shiftInPublished, boolean shiftInActive, boolean displayed) {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        PositionGroup posGroup = PositionGroupRepository.randomPositionGroup();
        PositionCategory posCat = PositionCategoryRepository.randomDynamicPositionCategory();
        PresetClass.changePosition(ep, posCat, posGroup, null);

        ShiftHiringReason reason = PresetClass.setupHiringReasonAndEntityPropertyForOrgUnit(omId);
        LocalDate freeShiftDay = ShiftTimePosition.FUTURE.getShiftsDateInterval().getRandomDateBetween();
        PresetClass.prepareShifts(ep, freeShiftDay, shiftInPublished, shiftInActive, omId);
        DateTimeInterval freeShiftInterval =
                PresetClass.makeFreeShift(freeShiftDay, omId, null, posGroup, posCat, reason, null, null, null).getDateTimeInterval();
        PresetClass.removeAllTagsFromOrgUnit(unit);

        goToSchedule(unit);
        clickFreeShift(freeShiftDay);
        int order = findShiftOrderNumber(freeShiftInterval, posGroup.getName());
        clickFreeShiftEmployeeList(order + 1);//+1 для человекочитаемого отображения в отчете.
        freeShiftEmployeesCheck(ep, displayed);
    }

    @Test(groups = {"ABCHR4537-1.2", "ABCHR4537-1", G2, SCHED32,
            //"@Before check if last day of month",
            "@Before disable pre-publication checks"},
            description = "Выбор сотрудников, доступных для назначения на смену с биржи (из другого подразделения)",
            dataProvider = "employees for free shifts in different orgUnit")
    @Link(name = "Статья: \"4537_При добавлении смены на биржу проверять и активную и опубликованную версии\"", url = "https://wiki.goodt.me/x/GRX6D")
    @TmsLink("60301")
    @Tag("ABCHR4537-1")
    @Tag(SCHED32)
    public void viewEmployeesAvailableForAssignmentOfFreeShiftDifferentOrgUnit(boolean shiftInPublished, boolean shiftInActive, boolean displayed) {
        ImmutablePair<OrgUnit, OrgUnit> units = OrgUnitRepository.getTwoOrgUnitsForShifts();
        OrgUnit mainUnit = units.left;
        int mainOmId = mainUnit.getId();
        OrgUnit secondUnit = units.right;
        int secondOmId = secondUnit.getId();
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(secondOmId, null, true);
        PositionGroup posGroup = PositionGroupRepository.randomPositionGroup();
        PositionCategory posCat = PositionCategoryRepository.randomDynamicPositionCategory();
        PresetClass.changePosition(ep, posCat, posGroup, null);

        PresetClass.addRandomTagToEmployeeAndOrgUnit(mainUnit, ep.getEmployee());
        ShiftHiringReason reason = PresetClass.setupHiringReasonAndEntityPropertyForOrgUnit(mainOmId);
        LocalDate freeShiftDay = ShiftTimePosition.FUTURE.getShiftsDateInterval().getRandomDateBetween();
        DateTimeInterval freeShiftInterval =
                PresetClass.makeFreeShift(freeShiftDay, mainOmId, null, posGroup, posCat, reason, null, null, null).getDateTimeInterval();
        PresetClass.prepareShifts(ep, freeShiftDay, shiftInPublished, shiftInActive, secondOmId);

        goToSchedule(mainUnit);
        clickFreeShift(freeShiftDay);
        int order = findShiftOrderNumber(freeShiftInterval, posGroup.getName());
        clickFreeShiftEmployeeList(order + 1); //+1 для человекочитаемого отображения в отчете.
        freeShiftEmployeesCheck(ep, displayed);
    }

    @Test(groups = {"ABCHR4579", G1, SHIFTS, LIST20, MAGNIT,
            "@Before disable check of worked roster before adding shift",
            "@Before disable cutting of worked shifts to fit the plan",
            "@Before disable worked shift comments",
            "@Before disable typed limits check"},
            description = "Блокирование табеля для пользователя в заданный день без указания времени")
    @Link(name = "Статья: \"4579_Добавить системный список \"Настройки редактирования табеля\"\"", url = "https://wiki.goodt.me/x/fxf6D")
    @TmsLink("60299")
    @Tag("ABCHR4579-10")
    @Tag(LIST20)
    public void lockWorkedRosterOnCertainDaysPerTableRuleWithNoTimeSpecified() {
        checkFirstDayOfMonth();
        int editDays = 3;
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitWithWorkedRosters(true);
        int omId = unit.getId();
        PresetClass.changeOrSetMathParamValue(omId, MathParameterValues.ACCESS_TO_OVERWORK, true, true);
        Role role = getRoleWithBasicSchedulePermissions();
        DateInterval interval = new DateInterval(LocalDate.now().minusDays(editDays), LocalDate.now().minusDays(1));
        List<LocalDate> datesToCheck = getDatesForCheckingUi(interval);
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        prepareShiftsForCellAccessibilityCheck(datesToCheck, ep, LocalDate.now().minusDays(1));
        PresetClass.addTableRuleToOrgUnit(omId, editDays, null, null,
                                          TableRuleShiftType.TIMESHEET);
        goToScheduleAsUser(role, unit);
        ScheduleWorker sw = new ScheduleWorker(sb);
        List<LocalDate> datesToCheckLastMonth = datesToCheck.stream()
                .filter(date -> date.getMonth() != LocalDate.now().getMonth())
                .collect(Collectors.toList());
        datesToCheck.removeAll(datesToCheckLastMonth);
        assertActiveCells(ep, interval, datesToCheck, sw);
        assertLockedShifts(ep, interval, datesToCheckLastMonth,
                           LocalDateTools.now().minusDays(editDays + 1), sw);
    }

    @Test(groups = {"ABCHR4579", G1, SHIFTS, LIST20, MAGNIT,
            "@Before disable check of worked roster before adding shift",
            "@Before disable cutting of worked shifts to fit the plan",
            "@Before disable worked shift comments",
            "@Before disable typed limits check",
            "@Before disable roster single edited version"},
            description = "Блокирование табеля для пользователя в заданный день с указанием времени",
            dataProvider = "true/false", dataProviderClass = DataProviders.class)
    @Link(name = "Статья: \"4579_Добавить системный список \"Настройки редактирования табеля\"\"", url = "https://wiki.goodt.me/x/fxf6D")
    @TmsLink("60299")
    @Tag("ABCHR4579-11")
    @Tag(LIST20)
    public void lockWorkedRosterOnCertainDaysPerTableRuleWithTimeSpecified(boolean active) {
        Random random = new Random();
        int editDays = random.nextInt(15) + 1;
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitWithWorkedRosters(true);
        int omId = unit.getId();
        if (LocalDate.now().minusDays(editDays).getMonthValue() < LocalDate.now().getMonthValue()) {
            PresetClass.addTableRuleToOrgUnit(omId, null, null,
                                              Collections.singletonList(LocalDate.now().getMonth().length(LocalDate.now().isLeapYear())),
                                              TableRuleStrategy.PREVIOUS_MONTH, TableRuleShiftType.TIMESHEET);
        }
        PresetClass.changeOrSetMathParamValue(omId, MathParameterValues.ACCESS_TO_OVERWORK, true, true);
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        Role role = getRoleWithBasicSchedulePermissions();
        LocalDate now = LocalDate.now();
        DateInterval interval;
        LocalTime time;
        if (active) {
            interval = new DateInterval(LocalDate.now().minusDays(editDays + 1), LocalDate.now().minusDays(1));
            time = LocalTime.of(23, 59);
        } else {
            interval = new DateInterval(LocalDate.now().minusDays(editDays), LocalDate.now().minusDays(1));
            time = LocalTime.of(0, 0);
        }
        List<LocalDate> datesToCheck = getDatesForCheckingUi(interval);
        for (LocalDate localDate : datesToCheck) {
            PresetClass.presetForMakeShiftDate(ep, localDate, false, ShiftTimePosition.PAST_MONTH);
        }
        LocalDate lockedShiftDate = datesToCheck.get(0).minusDays(1);
        PresetClass.presetForMakeShiftDate(ep, lockedShiftDate, false, ShiftTimePosition.PAST_MONTH);
        if (ShiftRepository.getShift(ep, now, ShiftTimePosition.FUTURE) == null) {
            PresetClass.presetForMakeShiftDate(ep, now, false, ShiftTimePosition.FUTURE);
        }
        PresetClass.addTableRuleToOrgUnit(omId, editDays, time, null, TableRuleShiftType.TIMESHEET);
        goToScheduleAsUser(role, unit);
        List<LocalDate> datesToCheckLastMonth = datesToCheck.stream()
                .filter(date -> date.getMonth() != LocalDate.now().getMonth())
                .collect(Collectors.toList());
        datesToCheck.removeAll(datesToCheckLastMonth);
        ScheduleWorker sw = new ScheduleWorker(sb);
        assertActiveCells(ep, interval, datesToCheck, sw);
        assertLockedShifts(ep, interval, datesToCheckLastMonth, lockedShiftDate, sw);
    }

    @Test(groups = {"ABCHR4632", G1, SHIFTS, SCHED9,
            "@Before disable all shift comments", "@Before disallow timesheet editing for past months", "@Before enable checking for roster locks",
            "@After remove added roster locks"},
            description = "Создание запроса расписания в табеле в заблокированном периоде",
            expectedExceptions = WaitUntilException.class, expectedExceptionsMessageRegExp = ANY + NO_PlUS_BUTTON + ANY)
    @Link(name = "Статья: \"4632_Запрет создания запросов при блокировке табеля\"", url = "https://wiki.goodt.me/x/WhT6D")
    @TmsLink("60303")
    @Tag(SCHED9)
    @Tag("ABCHR4632-1")
    public void addScheduleRequestInLockedPeriod() {
        checkFirstDayOfMonth();
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        List<PermissionType> permissions = new ArrayList<>(Arrays.asList(
                PermissionType.SCHEDULE_VIEW,
                PermissionType.SCHEDULE_EDIT,
                PermissionType.SCHEDULE_MANAGE_REQUESTS,
                PermissionType.ORG_UNIT_EMPLOYEE_PERSONALISATION
        ));
        Role role = PresetClass.createCustomPermissionRole(permissions);
        ScheduleRequestType type = ScheduleRequestType.getRandomDayTypeRequest();
        PresetClass.addScheduleRequestTypeRights(role, type);
        ShiftTimePosition timePosition = ShiftTimePosition.PAST;
        LocalDate freeDate = PresetClass.getFreeDateForEmployeeShiftPreset(ep, timePosition);
        DBUtils.addRosterLockForOrgUnit(omId, null, LocalDateTools.getFirstDate(), LocalDate.now());
        goToScheduleAsUser(role, unit);
        clickOnEmptyCell(ep, freeDate, new ScheduleWorker(sb));
        clickOnPlusCellOnGraph();
    }

    @Test(groups = {"ABCHR4632", G1, SHIFTS, SCHED9,
            "@Before disable all shift comments", "@Before disallow timesheet editing for past months", "@Before enable checking for roster locks",
            "@After remove added roster locks"},
            description = "Редактирование запроса расписания в табеле в заблокированном периоде",
            expectedExceptions = ElementNotInteractableException.class, expectedExceptionsMessageRegExp = ELEMENT_NOT_INTERACTABLE)
    @Link(name = "Статья: \"4632_Запрет создания запросов при блокировке табеля\"", url = "https://wiki.goodt.me/x/WhT6D")
    @TmsLink("60303")
    @Tag(SCHED9)
    @Tag("ABCHR4632-3")
    public void editScheduleRequestInLockedPeriod() {
        checkFirstDayOfMonth();
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        List<PermissionType> permissions = new ArrayList<>(Arrays.asList(
                PermissionType.SCHEDULE_VIEW,
                PermissionType.SCHEDULE_EDIT,
                PermissionType.SCHEDULE_MANAGE_REQUESTS,
                PermissionType.ORG_UNIT_EMPLOYEE_PERSONALISATION
        ));
        Role role = PresetClass.createCustomPermissionRole(permissions);
        ScheduleRequestType type = ScheduleRequestType.getRandomDayTypeRequest();
        PresetClass.addScheduleRequestTypeRights(role, type);
        ShiftTimePosition timePosition = ShiftTimePosition.PAST;
        LocalDate freeDate = PresetClass.getFreeDateForEmployeeShiftPreset(ep, timePosition);
        ScheduleRequest request = PresetClass.createScheduleRequestForDate(ScheduleRequestStatus.APPROVED, freeDate,
                                                                           ep, type);
        DBUtils.addRosterLockForOrgUnit(omId, null, LocalDateTools.getFirstDate(), LocalDate.now());
        goToScheduleAsUser(role, unit);
        clickOnRequestBlock(request, new ScheduleWorker(sb), unit);
        LocalDate newDate = freeDate.plusDays(1);
        enterShiftDateStartOrEnd(newDate, DateTypeField.END_DATE);
    }

    @Ignore(NOT_ACTUAL)
    @Test(groups = {"ABCHR4946", G2, SHIFTS, LIST20, MAGNIT,
            "@Before disable check of worked roster before adding shift",
            "@Before disable cutting of worked shifts to fit the plan",
            "@Before enable indication of exchange shifts",
            "@Before disable typed limits check",
            "@After remove table rule"},
            description = "Редактирование смены, назначенной с биржи, в закрытом периоде табеля",
            dataProvider = "Locked Shifts from Exchange",
            expectedExceptions = ElementNotVisibleException.class, expectedExceptionsMessageRegExp = ELEMENT_NOT_VISIBLE)
    @Link(name = "Статья: \"4946_Добавить дату закрытия редактирования для смен с биржи\"", url = "https://wiki.goodt.me/x/KYhSDQ")
    @TmsLink("60296")
    @Tag(LIST20)
    @Tag("ABCHR4946-1")
    public void editExchangeShiftInLockedPeriodOfWorkedRoster(Integer exchangeDaysToEdit, int fixedDay) {
        checkFirstDayOfMonth();
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitWithWorkedRosters(true);
        int rosterId = RosterRepository.getWorkedRosterThisMonth(unit.getId()).getId();
        List<Shift> shifts = ShiftRepository.getShiftsForRoster(rosterId, new DateInterval(LocalDateTools.getFirstDate().plusDays(1),
                                                                                           LocalDate.now().minusDays(1)));
        Shift shift;
        EmployeePosition ep;
        if (shifts.isEmpty()) {
            ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(unit.getId(), null, true);
            shift = PresetClass.defaultShiftPreset(ep, ShiftTimePosition.PAST);
        } else {
            shift = getRandomFromList(shifts);
            ep = EmployeePositionRepository.getEmployeePositionById(shift.getEmployeePositionId());
        }
        DBUtils.makeShiftFromExchange(shift);
        LocalDate date = shift.getDateTimeInterval().getStartDate();
        if (exchangeDaysToEdit == null || exchangeDaysToEdit == 0) {
            int difference = LocalDate.now().compareTo(date);
            exchangeDaysToEdit = difference + 1;
        }
        int day = date.getDayOfMonth();
        PresetClass.addTableRuleToOrgUnit(unit.getId(), exchangeDaysToEdit, null,
                                          null,
                                          TableRuleShiftType.EXCHANGE);
        List<PermissionType> permissionCustomGeneratedTypes = new ArrayList<>(Arrays.asList(
                PermissionType.SCHEDULE_VIEW,
                PermissionType.SCHEDULE_EDIT,
                PermissionType.OTHER_GENERAL_EXCHANGE,
                PermissionType.EDIT_ASSIGNED_EXCHANGE_SHIFT,
                PermissionType.ORG_UNIT_EMPLOYEE_PERSONALISATION
        ));
        Role role = PresetClass.createCustomPermissionRole(permissionCustomGeneratedTypes);
        goToScheduleAsUser(role, unit);
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        clickOnTargetShift(ep, date, scheduleWorker);
        enterShiftTimeStartOrEnd(LocalTime.now(), TimeTypeField.END_TIME);
    }

    @Ignore(NOT_ACTUAL)
    @Test(groups = {"ABCHR4946", G2, SHIFTS, LIST20, MAGNIT,
            "@Before disable check of worked roster before adding shift",
            "@Before disable cutting of worked shifts to fit the plan",
            "@Before enable indication of exchange shifts",
            "@Before disable all shift comments",
            "@Before disable check of worked shifts against plan",
            "@Before allow worked shift editing"},
            description = "Редактирование смены, назначенной с биржи, в доступном периоде табеля",
            dataProvider = "Unlocked Shifts from Exchange")
    @Link(name = "Статья: \"4946_Добавить дату закрытия редактирования для смен с биржи\"", url = "https://wiki.goodt.me/x/KYhSDQ")
    @TmsLink("60296")
    @Tag(LIST20)
    @Tag("ABCHR4946-2")
    public void editExchangeShiftInUnlockedPeriodOfWorkedRoster(EmployeePosition ep, LocalDate date, Shift shift,
                                                                int exchangeDaysToEdit, List<Integer> fixedDays) {
        checkFirstDayOfMonth();
        OrgUnit unit = ep.getOrgUnit();
        PresetClass.changeOrSetMathParamValue(unit.getId(), MathParameterValues.ACCESS_TO_OVERWORK, true, true);
        PresetClass.addTableRuleToOrgUnit(unit.getId(), exchangeDaysToEdit, null, fixedDays,
                                          TableRuleShiftType.TIMESHEET);
        List<PermissionType> permissionCustomGeneratedTypes = new ArrayList<>(Arrays.asList(
                PermissionType.SCHEDULE_VIEW,
                PermissionType.SCHEDULE_EDIT,
                PermissionType.OTHER_GENERAL_EXCHANGE,
                PermissionType.ORG_UNIT_EMPLOYEE_PERSONALISATION
        ));
        Role role = PresetClass.createCustomPermissionRole(permissionCustomGeneratedTypes);
        goToScheduleAsUser(role, unit);
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        clickOnTargetShift(ep, date, scheduleWorker);
        shift = shift.refreshShift();
        Assert.assertNotNull(shift, String.format("Смена сотрудника %s за %s не найдена в api", ep, date));
        LocalDateTime time = shift.getDateTimeInterval().getEndDateTime().minusMinutes(30);
        enterShiftTimeStartOrEnd(time.toLocalTime(), TimeTypeField.END_TIME);
        clickEditShiftButton();
        assertEditShift(ep, new DateTimeInterval(shift.getDateTimeInterval().getStartDateTime(), time), scheduleWorker);
    }

    @Ignore("Заменен на api-тест")
    @Test(groups = {"TEST-133.1", SHIFTS, G1, SCHED9,
            "@Before disable check of worked roster before adding shift",
            "@Before disable mandatory comments when deleting worked shift"},
            description = "Удаление смены за первое число месяца")
    @Link(name = "Удаление смены за первое число месяца", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=178979652")
    @TmsLink("61649")
    @Owner(BUTINSKAYA)
    @Tag("TEST-133.1")
    @Tag(SCHED9)
    public void deleteShiftFirstDayOfMonth() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        PresetClass.kpiAndFteChecker(unit.getId());
        EmployeePosition employeePosition = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(unit.getId(), null, false);
        Shift shift = ShiftRepository.getFirstDayMonthShift(employeePosition);
        goToSchedule(unit);
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        clickOnTargetShift(employeePosition, shift.getDateTimeInterval().getStartDate(), scheduleWorker);
        shiftThreeDotsClick();
        selectAction(RequestAction.DELETE, false);
        assertDeleteShift(employeePosition, shift, scheduleWorker);
    }

    @Test(groups = {"ABCHR4233-1", "@Before download shifts plan", G1, SCHED24, POCHTA},
            description = "Доступ к скачиванию планового графика при наличии разрешения")
    @Link(name = "Статья: \"4233_[Расписание] Убрать кнопку \"Плановый график (без обедов)\" у роли начальник ОПС\"",
            url = "https://wiki.goodt.me/pages/viewpage.action?pageId=217714944")
    @Tag("ABCHR4233-1")
    @TmsLink("60330")
    @Tag(SCHED24)
    @Owner(BUTINSKAYA)
    public void accessToPlanDownload() {
        List<PermissionType> permissionCustomGeneratedTypes = new ArrayList<>(Arrays.asList(
                PermissionType.SCHEDULE_VIEW,
                PermissionType.JASPER_REPORTS_SHIFTS_PLAN,
                PermissionType.ORG_UNIT_EMPLOYEE_PERSONALISATION
        ));
        Role role = PresetClass.createCustomPermissionRole(permissionCustomGeneratedTypes);
        ImmutablePair<OrgUnit, Roster> orgUnitWithRoster = OrgUnitRepository.getRandomOrgUnitWithActiveRoster();
        OrgUnit unit = orgUnitWithRoster.left;
        int rosterId = orgUnitWithRoster.right.getId();
        int unitId = unit.getId();
        PresetClass.checkAndMakePublicationRoster(unitId);
        goToScheduleAsUser(role, unit);
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.DOWNLOAD_PLANNED_SCHEDULE);
        pressDownloadButton();
        URI urlAddress = URI.create(getLinkOnPlan());
        List<Employee> employees = EmployeeRepository.getWorkingEmployees(unitId);
        List<EmployeePosition> employeePositions = EmployeePositionRepository.getEmployeesWithPosIds(unitId, null, true);
        employeePositions.removeIf(ep -> !employees.contains(ep.getEmployee()));
        FileDownloadCheckerForScheduleBoard checker = new FileDownloadCheckerForScheduleBoard(role, unitId,
                                                                                              TypeOfFiles.PDF_ONLY_SCHEDULE,
                                                                                              TypeOfReports.PLANNED_GRAPH,
                                                                                              String.valueOf(rosterId),
                                                                                              employees.stream().map(Employee::getId)
                                                                                                      .map(String::valueOf).collect(Collectors.joining(",")),
                                                                                              employeePositions.stream().map(EmployeePosition::getId)
                                                                                                      .map(String::valueOf).collect(Collectors.joining(","))
        );
        assertForDownloadingPath(checker, urlAddress, role);
    }

    @Test(groups = {"ABCHR4273", SHIFTS, G1, SCHED37, MAGNIT,
            "@Before enable additional work", "@Before display all additional work",
            "@Before disable all shift comments"},
            description = "Добавление дополнительных работ без статуса к существующей смене")
    @Link(name = "Статья: \"4273_Дополнительные работы в рамках смены\"", url = "https://wiki.goodt.me/x/IQr6D")
    @TmsLink("60307")
    @Owner(SCHASTLIVAYA)
    @Tag("ABCHR4273-1")
    @Tag(SCHED37)
    public void addAdditionalWorkWithoutStatusToExistingShift() {
        ImmutablePair<OrgUnit, EmployeePosition> unitAndEmp = OrgUnitRepository.getRandomOrgUnitWithRandomEmployeeForShiftPair(false);
        OrgUnit unit = unitAndEmp.left;
        EmployeePosition ep = unitAndEmp.right;
        int omId = unit.getId();
        PresetClass.changeOrSetMathParamValue(omId, MathParameterValues.ACCESS_TO_OVERWORK, true, true);
        AdditionalWork additionalWork = AdditionalWorkRepository.getTestAddWork(false, "test_addWork");
        Shift shift = PresetClass.defaultShiftPreset(ep, ShiftTimePosition.DEFAULT);
        PresetClass.prepareAdditionalWorkForAllPositions(additionalWork, omId, shift.getStartDate(), false);

        PresetClass.checkAndMakePublicationRoster(omId);

        goToSchedule(unit);
        DateTimeInterval interval = shift.getDateTimeInterval();
        LocalDate date = interval.getStartDate();
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        clickOnTargetShift(ep, date, scheduleWorker);
        clickAddAdditionalWorkButtonInShift();
        selectAdditionalWorkType(additionalWork.getTitle());
        LocalTime startTime = interval.getStartDateTime().toLocalTime().plusHours(1);
        enterAdditionalWorkTimeStartOrEnd(startTime, DateTypeField.START_DATE);
        LocalTime endTime = interval.getEndDateTime().toLocalTime().minusHours(1);
        enterAdditionalWorkTimeStartOrEnd(endTime, DateTypeField.END_DATE);
        clickEditShiftButton();
        assertAddAdditionalWork(scheduleWorker, ep, date, startTime, endTime, shift, additionalWork);
    }

    @Test(groups = {"ABCHR4273", SHIFTS, G1, SCHED37, MAGNIT,
            "@Before enable additional work", "@Before display all additional work",
            "@Before disable all shift comments"},
            description = "Удаление дополнительных работ")
    @Link(name = "Статья: \"4273_Дополнительные работы в рамках смены\"", url = "https://wiki.goodt.me/x/IQr6D")
    @TmsLink("60307")
    @Owner(SCHASTLIVAYA)
    @Tag("ABCHR4273-2")
    @Tag(SCHED37)
    public void deleteAdditionalWork() {
        ImmutablePair<OrgUnit, EmployeePosition> unitAndEmp = OrgUnitRepository.getRandomOrgUnitWithRandomEmployeeForShiftPair(false);
        OrgUnit unit = unitAndEmp.left;
        EmployeePosition ep = unitAndEmp.right;
        int omId = unit.getId();
        PresetClass.changeOrSetMathParamValue(omId, MathParameterValues.ACCESS_TO_OVERWORK, true, true);
        AdditionalWork additionalWork = AdditionalWorkRepository.getTestAddWork(false, "test_addWork");
        Shift shift = PresetClass.presetForMakeShift(ep, false, ShiftTimePosition.DEFAULT);
        PresetClass.prepareAdditionalWorkForAllPositions(additionalWork, omId, shift.getStartDate(), false);
        DateTimeInterval interval = shift.getDateTimeInterval();
        PresetClass.addWorkToShift(interval.getStartDateTime().plusHours(1), interval.getEndDateTime().minusHours(1), shift, additionalWork);

        goToSchedule(unit);
        LocalDate date = interval.getStartDate();
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        clickOnTargetShift(ep, date, scheduleWorker);
        clickDeleteAdditionalWorkButton();
        selectAdditionalWorkToDelete();
        List<ShiftAddWorkLink> additionalWorkBefore = shift.getAdditionalWork();
        clickEditShiftButton();
        assertDeleteAdditionalWork(scheduleWorker, ep, date, additionalWorkBefore);
    }

    @Test(groups = {"ABCHR4273", SHIFTS, G1, SCHED37, MAGNIT,
            "@Before enable additional work", "@Before display all additional work",
            "@Before disable all shift comments"},
            description = "Невозможность назначить дополнительные работы за пределами смены",
            dataProvider = "Additional work time outside of shift")
    @Link(name = "Статья: \"4273_Дополнительные работы в рамках смены\"", url = "https://wiki.goodt.me/x/IQr6D")
    @TmsLink("60307")
    @Owner(SCHASTLIVAYA)
    @Tag("ABCHR4273-4")
    @Tag(SCHED37)
    public void additionalWorkNotAssignableOutsideOfShift(int startTimeOffset, int endTimeOffset, boolean success, int errorTime) {
        String errorMessage = "Некорректное время";
        ImmutablePair<OrgUnit, EmployeePosition> unitAndEmp = OrgUnitRepository.getRandomOrgUnitWithRandomEmployeeForShiftPair(false);
        OrgUnit unit = unitAndEmp.left;
        EmployeePosition ep = unitAndEmp.right;
        int omId = unit.getId();
        PresetClass.changeOrSetMathParamValue(omId, MathParameterValues.ACCESS_TO_OVERWORK, true, true);
        AdditionalWork additionalWork = AdditionalWorkRepository.getTestAddWork(false, "test_addWork");
        Shift shift = PresetClass.presetForMakeShift(ep, false, ShiftTimePosition.DEFAULT);
        PresetClass.prepareAdditionalWorkForAllPositions(additionalWork, omId, shift.getStartDate(), false);
        PresetClass.checkAndMakePublicationRoster(omId);

        goToSchedule(unit);
        LocalDate date = shift.getDateTimeInterval().getStartDate();
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        clickOnTargetShift(ep, date, scheduleWorker);
        clickAddAdditionalWorkButtonInShift();
        LocalDateTime startTime = shift.getDateTimeInterval().getStartDateTime();
        LocalDateTime endTime = shift.getDateTimeInterval().getEndDateTime();
        LocalTime newStartTime = startTime.toLocalTime().plusMinutes(startTimeOffset);
        LocalTime newEndTime = endTime.toLocalTime().plusMinutes(endTimeOffset);
        selectAdditionalWorkType(additionalWork.getTitle());
        enterAdditionalWorkTimeStartOrEnd(newStartTime, DateTypeField.START_DATE);
        enterAdditionalWorkTimeStartOrEnd(newEndTime, DateTypeField.END_DATE);
        if (success) {
            clickEditShiftButton();
            assertAddAdditionalWork(scheduleWorker, ep, date, newStartTime, newEndTime, shift, additionalWork);
            PresetClass.removeAllAdditionalWorkFromShift(shift);
        } else {
            LocalDateTime dateTime;
            if (errorTime == 0) {
                dateTime = startTime;
            } else if (errorTime == 1) {
                dateTime = endTime;
            } else {
                dateTime = startTime.plusHours(2).plusMinutes(1);
            }
            assertErrorInAdditionalWorkInputField(String.format(errorMessage, dateTime.toLocalDate(), dateTime.toLocalTime()), shift);
        }
    }

    @Test(groups = {"ABCHR4273", SHIFTS, G1, SCHED37, MAGNIT,
            "@Before disable additional work", "@Before display all additional work"},
            description = "Скрытие блока дополнительных работ при отключенной настройке",
            expectedExceptions = WaitUntilException.class, expectedExceptionsMessageRegExp = FIRST_ADDITIONAL_WORK)
    @Link(name = "Статья: \"4273_Дополнительные работы в рамках смены\"", url = "https://wiki.goodt.me/x/IQr6D")
    @TmsLink("60307")
    @Owner(SCHASTLIVAYA)
    @Tag("ABCHR4273-5")
    @Tag(SCHED37)
    public void hideAdditionalWorkBlock() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        AdditionalWork addWork = AdditionalWorkRepository.getTestAddWork(false, "test_addWork");
        PresetClass.makeAdditionalWorkEnabled(addWork);
        AddWorkRule.getAllRulesOfAddWork(addWork.getId()).forEach(PresetClass::deleteRequest);
        EmployeePosition ep = PresetClass.prepareAdditionalWorkRule(addWork, omId).left;
        Shift shift = PresetClass.defaultShiftPreset(ep, ShiftTimePosition.FUTURE);
        PresetClass.checkAndMakePublicationRoster(omId);

        goToSchedule(unit);
        clickOnTargetShift(ep, shift.getDateTimeInterval().getStartDate(), new ScheduleWorker(sb));
        clickAddAdditionalWorkButtonInShift();
    }

    @Test(groups = {"ABCHR4275-3", SHIFTS, G1, SCHED37, MAGNIT,
            "@Before enable additional work",
            "@Before display all additional work"},
            dataProvider = "true/false", dataProviderClass = DataProviders.class,
            description = "Редактирование смены с изменением типа дополнительной работы без права \"Редактирование доп. работ в смене\"",
            expectedExceptions = WaitUntilException.class, expectedExceptionsMessageRegExp = FIRST_ADDITIONAL_WORK)
    @Link(name = "4275_Добавить права на блок \"Типы доп. работ\"", url = "https://wiki.goodt.me/x/gQr6D")
    @TmsLink("60243")
    @Tag("ABCHR4275-3")
    @Tag(SCHED37)
    public void editShiftWithAddWorkWithoutPermission(boolean createAddWork) {
        ImmutablePair<OrgUnit, EmployeePosition> unitAndEmp = OrgUnitRepository.getRandomOrgUnitWithRandomEmployeeForShiftPair(false);
        OrgUnit unit = unitAndEmp.left;
        EmployeePosition ep = unitAndEmp.right;
        int omId = unit.getId();
        List<PermissionType> permissions = new ArrayList<>(Arrays.asList(
                PermissionType.SCHEDULE_VIEW,
                PermissionType.SCHEDULE_EDIT,
                PermissionType.SCHEDULE_EDIT_WORKED,
                PermissionType.ORG_UNIT_EMPLOYEE_PERSONALISATION,
                PermissionType.ROSTER_SHIFT_EDIT_OR_CREATE
        ));
        Role role = PresetClass.createCustomPermissionRole(permissions);
        AdditionalWork addWork = AdditionalWorkRepository.getTestAddWork(false, "test_addWork");
        Shift shift = PresetClass.defaultShiftPreset(ep, ShiftTimePosition.FUTURE);
        PresetClass.prepareAdditionalWorkForAllPositions(addWork, omId, shift.getStartDate(), false);
        shift.getAdditionalWork().forEach(PresetClass::deleteRequest);
        DateTimeInterval interval = shift.getDateTimeInterval();
        if (!createAddWork) {
            PresetClass.addWorkToShift(interval.getStartDateTime().plusHours(1), interval.getEndDateTime().minusHours(1), shift, addWork);
        }
        PresetClass.checkAndMakePublicationRoster(omId);

        goToScheduleAsUser(role, unit);
        LocalDate date = interval.getStartDate();
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        clickOnTargetShift(ep, date, scheduleWorker);
        clickAddAdditionalWorkButtonInShift();
    }

    @Test(groups = {"ABCHR4067", G2, MIX2},
            description = "Отображение персональных данных пользователя в модуле \"Расписание\" при наличии прав",
            dataProvider = "true/false", dataProviderClass = DataProviders.class)
    @Link(name = "4067_Обезличивание персональных данных", url = "https://wiki.goodt.me/x/NgH6D")
    @TmsLink("60242")
    @Owner(SCHASTLIVAYA)
    @Tag(MIX2)
    @Severity(SeverityLevel.MINOR)
    public void showPersonalDataDependingOnPermission(boolean hasPermissions) {
        changeTestIDDependingOnParameter(hasPermissions, "ABCHR4067-1", "ABCHR4067-3",
                                         "Отображение персональных данных пользователя в модуле \"Расписание\" при отсутствии прав");
        List<PermissionType> permissions = new ArrayList<>(Collections.singletonList(PermissionType.SCHEDULE_VIEW));
        if (hasPermissions) {
            permissions.add(PermissionType.ORG_UNIT_EMPLOYEE_PERSONALISATION);
        }
        Role role = PresetClass.createCustomPermissionRole(permissions);
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        goToScheduleAsUser(role, unit);
        assertPersonalDataVisibility(hasPermissions);
    }

    @Test(groups = {"ABCHR4090", SHIFTS, G1, SCHED9,
            "@Before disable all shift comments", "@Before disallow timesheet editing for past months", "@Before enable checking for roster locks",
            "@After remove added roster locks",
            "@Before disable check of worked roster before adding shift"},
            description = "Редактирование смены в табеле в заблокированном периоде",
            expectedExceptions = ElementNotInteractableException.class, expectedExceptionsMessageRegExp = ELEMENT_NOT_INTERACTABLE)
    @Link(name = "Статья: \"4090_Запрет редактирования табеля после выгрузки в 1С\"", url = "https://wiki.goodt.me/x/Dgb6D")
    @TmsLink("60304")
    @Tag(SCHED9)
    @Tag("ABCHR4090-1")
    public void editShiftInLockedPeriod() {
        checkFirstDayOfMonth();
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitWithWorkedRosters(true);
        int omId = unit.getId();
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        ShiftTimePosition timePosition = ShiftTimePosition.PAST;
        Shift shift = PresetClass.defaultShiftPreset(ep, timePosition);
        DBUtils.addRosterLockForOrgUnit(omId, null, LocalDateTools.getFirstDate(), LocalDate.now());
        List<PermissionType> permissions = new ArrayList<>(Arrays.asList(
                PermissionType.SCHEDULE_VIEW,
                PermissionType.SCHEDULE_EDIT,
                PermissionType.ORG_UNIT_EMPLOYEE_PERSONALISATION
        ));
        Role role = PresetClass.createCustomPermissionRole(permissions);
        goToScheduleAsUser(role, unit);
        LocalDate date = shift.getDateTimeInterval().getStartDate();
        clickOnTargetShift(ep, date, new ScheduleWorker(sb));
        LocalTime start = LocalTime.of(0, 0, 0);
        enterShiftTimeStartOrEnd(start, TimeTypeField.START_TIME);
    }

    @Test(groups = {"ABCHR4090", SHIFTS, G1, SCHED9,
            "@Before disable all shift comments", "@Before disallow timesheet editing for past months", "@Before enable checking for roster locks",
            "@After remove added roster locks"},
            description = "Создание смены в табеле в заблокированном периоде",
            expectedExceptions = WaitUntilException.class, expectedExceptionsMessageRegExp = ANY + NO_PlUS_BUTTON + ANY)
    @Link(name = "Статья: \"4090_Запрет редактирования табеля после выгрузки в 1С\"", url = "https://wiki.goodt.me/x/Dgb6D")
    @TmsLink("60304")
    @Tag(SCHED9)
    @Tag("ABCHR4090-2")
    public void addShiftInLockedPeriod() {
        checkFirstDayOfMonth();
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitWithWorkedRosters(true);
        int omId = unit.getId();
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        ShiftTimePosition timePosition = ShiftTimePosition.PAST;
        LocalDate date = PresetClass.getFreeDateForEmployeeShiftPreset(ep, timePosition);
        DBUtils.addRosterLockForOrgUnit(omId, null, LocalDateTools.getFirstDate(), LocalDate.now());
        List<PermissionType> permissions = new ArrayList<>(Arrays.asList(
                PermissionType.SCHEDULE_VIEW,
                PermissionType.SCHEDULE_EDIT,
                PermissionType.ORG_UNIT_EMPLOYEE_PERSONALISATION
        ));
        Role role = PresetClass.createCustomPermissionRole(permissions);
        goToScheduleAsUser(role, unit);
        clickOnEmptyCell(ep, date, new ScheduleWorker(sb));
        clickOnPlusCellOnGraph();
    }

    @Test(groups = {"ABCHR4090-2.1", SHIFTS, G3, SCHED9,
            "@Before disable all shift comments", "@Before disallow timesheet editing for past months", "@Before enable checking for roster locks",
            "@After remove added roster locks"},
            description = "Создание смены в табеле на границе заблокированного периода",
            expectedExceptions = WaitUntilException.class, expectedExceptionsMessageRegExp = ANY + NO_PlUS_BUTTON + ANY)
    @Link(name = "Статья: \"4090_Запрет редактирования табеля после выгрузки в 1С\"", url = "https://wiki.goodt.me/x/Dgb6D")
    @TmsLink("60304")
    @Tag(SCHED9)
    public void addShiftAtTheEndOfLockedPeriod() {
        checkFirstDayOfMonth();
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitWithWorkedRosters(true);
        int omId = unit.getId();
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        List<LocalDate> availableDates = new DateInterval(LocalDateTools.getFirstDate().plusDays(1), LocalDate.now()).getBetweenDatesList();
        LocalDate blockEndDate = getRandomFromList(availableDates);
        LocalDate date = blockEndDate.minusDays(1);
        PresetClass.makeClearDate(ep, date);
        DBUtils.addRosterLockForOrgUnit(omId, null, LocalDateTools.getFirstDate(), blockEndDate);
        List<PermissionType> permissions = new ArrayList<>(Arrays.asList(
                PermissionType.SCHEDULE_VIEW,
                PermissionType.SCHEDULE_EDIT,
                PermissionType.ORG_UNIT_EMPLOYEE_PERSONALISATION
        ));
        Role role = PresetClass.createCustomPermissionRole(permissions);
        goToScheduleAsUser(role, unit);
        clickOnEmptyCell(ep, date, new ScheduleWorker(sb));
        clickOnPlusCellOnGraph();
    }

    @Test(groups = {"ABCHR4090", SHIFTS, G1, SCHED9,
            "@Before disable all shift comments", "@Before disallow timesheet editing for past months", "@Before enable checking for roster locks",
            "@Before disable mandatory comments when deleting worked shift",
            "@After remove added roster locks"},
            description = "Удаление смены в табеле в заблокированном периоде")
    @Link(name = "Статья: \"4090_Запрет редактирования табеля после выгрузки в 1С\"", url = "https://wiki.goodt.me/x/Dgb6D")
    @TmsLink("60304")
    @Tag(SCHED9)
    @Tag("ABCHR4090-3")
    public void deleteShiftInLockedPeriod() {
        checkFirstDayOfMonth();
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitWithWorkedRosters(true);
        int omId = unit.getId();
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        ShiftTimePosition timePosition = ShiftTimePosition.PAST;
        Shift shift = PresetClass.defaultShiftPreset(ep, timePosition);
        DBUtils.addRosterLockForOrgUnit(omId, null, LocalDateTools.getFirstDate(), LocalDate.now());
        List<PermissionType> permissions = new ArrayList<>(Arrays.asList(
                PermissionType.SCHEDULE_VIEW,
                PermissionType.SCHEDULE_EDIT,
                PermissionType.ORG_UNIT_EMPLOYEE_PERSONALISATION
        ));
        Role role = PresetClass.createCustomPermissionRole(permissions);
        goToScheduleAsUser(role, unit);
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        LocalDate date = shift.getDateTimeInterval().getStartDate();
        assertInactiveCell(ep, date, scheduleWorker);
    }

    @Test(groups = {"ABCHR4090", SHIFTS, G1, SCHED9,
            "@Before disable all shift comments", "@Before disallow timesheet editing for past months", "@Before enable checking for roster locks",
            "@After remove added roster locks",
            "@Before disable start time check for worked shifts",
            "@Before disable check of worked roster before adding shift"},
            description = "Редактирование смены в табеле вне заблокированного периода")
    @Link(name = "Статья: \"4090_Запрет редактирования табеля после выгрузки в 1С\"", url = "https://wiki.goodt.me/x/Dgb6D")
    @TmsLink("60304")
    @Tag(SCHED9)
    @Tag("ABCHR4090-4")
    public void editShiftOutsideOfLockedPeriod() {
        if (URL_BASE.contains(MAGNIT)) {
            changeProperty(SystemProperties.WORKED_SHIFT_CAN_EDIT_DAYS, "62");
        } else {
            changeProperty(SystemProperties.WORKED_SHIFT_CAN_EDIT_DAYS, 62);
        }
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitWithWorkedRosters(true);
        int omId = unit.getId();
        PresetClass.changeOrSetMathParamValue(omId, MathParameterValues.ACCESS_TO_OVERWORK, true, true);
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        List<LocalDate> availableDates = new DateInterval(LocalDateTools.getFirstDate().minusMonths(1), LocalDate.now().minusDays(2)).getBetweenDatesList();
        LocalDate blockEndDate = getRandomFromList(availableDates);
        LocalDate date = blockEndDate.plusDays(1);
        PresetClass.presetForMakeShiftDate(ep, date, false, ShiftTimePosition.PAST_MONTH);
        DBUtils.addRosterLockForOrgUnit(omId, null, null, blockEndDate);
        List<PermissionType> permissions = new ArrayList<>(Arrays.asList(
                PermissionType.SCHEDULE_VIEW,
                PermissionType.SCHEDULE_EDIT,
                PermissionType.ORG_UNIT_EMPLOYEE_PERSONALISATION
        ));
        Role role = PresetClass.createCustomPermissionRole(permissions);
        goToScheduleAsUser(role, unit);
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        if (!date.getMonth().equals(LocalDate.now().getMonth())) {
            clickBack();
        }
        clickOnTargetShift(ep, date, scheduleWorker);
        LocalTime start = LocalTime.of(0, 0, 0);
        enterShiftTimeStartOrEnd(start, TimeTypeField.START_TIME);
        LocalTime end = start.plusHours(4);
        enterShiftTimeStartOrEnd(end, TimeTypeField.END_TIME);
        clickEditShiftButton();
        assertEditShift(ep, new DateTimeInterval(date.atTime(start), date.atTime(end)), scheduleWorker);
    }

    @Test(groups = {"ABCHR4090", SHIFTS, G1, SCHED9,
            "@Before disable all shift comments", "@Before disallow timesheet editing for past months", "@Before enable checking for roster locks",
            "@After remove added roster locks",
            "@Before disable check of worked roster before adding shift"},
            description = "Редактирование смены в табеле в заблокированном периоде для сотрудника",
            expectedExceptions = ElementNotInteractableException.class, expectedExceptionsMessageRegExp = ELEMENT_NOT_INTERACTABLE)
    @Link(name = "Статья: \"4090_Запрет редактирования табеля после выгрузки в 1С\"", url = "https://wiki.goodt.me/x/Dgb6D")
    @TmsLink("60304")
    @Tag(SCHED9)
    @Tag("ABCHR4090-5")
    public void editShiftInLockedPeriodForSingleEmployee() {
        checkFirstDayOfMonth();
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitWithWorkedRosters(true);
        int omId = unit.getId();
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        ShiftTimePosition timePosition = ShiftTimePosition.PAST;
        Shift shift = PresetClass.defaultShiftPreset(ep, timePosition);
        DBUtils.addRosterLockForOrgUnit(omId, ep.getId(), LocalDateTools.getFirstDate(), LocalDate.now());
        List<PermissionType> permissions = new ArrayList<>(Arrays.asList(
                PermissionType.SCHEDULE_VIEW,
                PermissionType.SCHEDULE_EDIT,
                PermissionType.ORG_UNIT_EMPLOYEE_PERSONALISATION
        ));
        Role role = PresetClass.createCustomPermissionRole(permissions);
        goToScheduleAsUser(role, unit);
        LocalDate date = shift.getDateTimeInterval().getStartDate();
        clickOnTargetShift(ep, date, new ScheduleWorker(sb));
        LocalTime start = LocalTime.of(0, 0, 0);
        enterShiftTimeStartOrEnd(start, TimeTypeField.START_TIME);
    }

    @Test(groups = {"ABCHR4090", SHIFTS, G1, SCHED9,
            "@Before disable all shift comments", "@Before disallow timesheet editing for past months", "@Before enable checking for roster locks",
            "@Before enable drag and drop function", "@Before enable copy shifts in worked roster",
            "@After remove added roster locks",},
            description = "Копирование смены в табеле в заблокированном периоде",
            expectedExceptions = WaitUntilException.class, expectedExceptionsMessageRegExp = ANY + SHIFT_ACTIONS_NOT_DISPLAYED + ANY)
    @Link(name = "Статья: \"4090_Запрет редактирования табеля после выгрузки в 1С\"", url = "https://wiki.goodt.me/x/Dgb6D")
    @TmsLink("60304")
    @Tag(SCHED9)
    @Tag("ABCHR4090-7")
    public void copyShiftInLockedPeriod() {
        checkFirstDayOfMonth();
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitWithWorkedRosters(true);
        int omId = unit.getId();
        ImmutablePair<EmployeePosition, EmployeePosition> eps = EmployeePositionRepository.getTwoRandomEmployeeWithCheckByApi(omId);
        EmployeePosition epFrom = eps.right;
        EmployeePosition epTo = eps.left;
        ShiftTimePosition timePosition = ShiftTimePosition.PAST;
        LocalDate freeDate = PresetClass.getFreeDateForEmployeeShiftPreset(epTo, timePosition);
        Shift shift = PresetClass.defaultShiftPreset(epFrom, timePosition);
        LocalDateTime shiftDate = shift.getDateTimeInterval().getStartDateTime();
        DBUtils.addRosterLockForOrgUnit(omId, null, LocalDateTools.getFirstDate(), LocalDate.now());
        List<PermissionType> permissions = new ArrayList<>(Arrays.asList(
                PermissionType.SCHEDULE_VIEW,
                PermissionType.SCHEDULE_EDIT,
                PermissionType.ORG_UNIT_EMPLOYEE_PERSONALISATION
        ));
        Role role = PresetClass.createCustomPermissionRole(permissions);
        goToScheduleAsUser(role, unit);
        transferOneShift(epFrom, epTo, shiftDate, freeDate.atStartOfDay(), new ScheduleWorker(sb));
    }

    @Test(groups = {"ABCHR4090", SHIFTS, G1, SCHED9,
            "@Before disable all shift comments", "@Before disallow timesheet editing for past months", "@Before enable checking for roster locks",
            "@Before enable drag and drop function",
            "@Before enable copy shifts in worked roster",
            "@After remove added roster locks"},
            description = "Копирование смены в табеле в заблокированный период из незаблокированного",
            expectedExceptions = WaitUntilException.class, expectedExceptionsMessageRegExp = ANY + SHIFT_ACTIONS_NOT_DISPLAYED + ANY)
    @Link(name = "Статья: \"4090_Запрет редактирования табеля после выгрузки в 1С\"", url = "https://wiki.goodt.me/x/Dgb6D")
    @TmsLink("60304")
    @Tag(SCHED9)
    @Tag("ABCHR4090-7.1")
    public void copyShiftInLockedPeriodFromUnlockedPeriod() {
        checkFirstDayOfMonth();
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitWithWorkedRosters(true);
        int omId = unit.getId();
        PresetClass.changeOrSetMathParamValue(omId, MathParameterValues.ACCESS_TO_OVERWORK, true, true);
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        ShiftTimePosition timePosition = ShiftTimePosition.PAST;
        LocalDate freeDate = PresetClass.getFreeDateForEmployeeShiftPreset(ep, timePosition);
        LocalDate blockEndDate = freeDate.plusDays(1);
        LocalDate shiftDate = new DateInterval(blockEndDate, LocalDateTools.getLastDate()).getRandomDateBetween();
        PresetClass.presetForMakeShiftDate(ep, shiftDate, false,
                                           shiftDate.isBefore(LocalDate.now()) ? ShiftTimePosition.PAST : ShiftTimePosition.FUTURE);
        DBUtils.addRosterLockForOrgUnit(omId, null, LocalDateTools.getFirstDate(), blockEndDate);
        List<PermissionType> permissions = new ArrayList<>(Arrays.asList(
                PermissionType.SCHEDULE_VIEW,
                PermissionType.SCHEDULE_EDIT,
                PermissionType.ORG_UNIT_EMPLOYEE_PERSONALISATION
        ));
        Role role = PresetClass.createCustomPermissionRole(permissions);
        goToScheduleAsUser(role, unit);
        transferOneShift(ep, ep, shiftDate.atStartOfDay(), freeDate.atStartOfDay(), new ScheduleWorker(sb));
    }

    @Test(groups = {"ABCHR4110", G2, SHIFTS, SCHED41},
            dataProvider = "true/false", dataProviderClass = DataProviders.class,
            description = "Свободный ввод комментария при редактирования смены из табеля")
    @Link(name = "Статья: \"4110_Комментарий к смене должно быть можно задавать только из списка\"", url = "https://wiki.goodt.me/x/uQ36D")
    @TmsLink("60290")
    @Tag(SCHED41)
    public void customCommentWhileEditingShift(boolean workedShifts) {
        changeTestIDDependingOnParameter(workedShifts, "АВСНR4110-3", "АВСНR4110-1",
                                         "Свободный ввод комментария при редактирования смены из планового графика");
        OrgUnit unit;
        ShiftTimePosition timePosition;
        List<PermissionType> permissions = new ArrayList<>(Arrays.asList(PermissionType.SCHEDULE_VIEW,
                                                                         PermissionType.SCHEDULE_EDIT,
                                                                         PermissionType.SHIFT_ALLOW_CUSTOM_COMMENT,
                                                                         PermissionType.ORG_UNIT_EMPLOYEE_PERSONALISATION,
                                                                         PermissionType.SCHEDULE_DAY_COMMENTS,
                                                                         PermissionType.SHIFT_MANAGE_COMMENT,
                                                                         PermissionType.SHIFT_READ_COMMENT));
        if (workedShifts) {
            checkFirstDayOfMonth();
            timePosition = ShiftTimePosition.PAST;
            permissions.add(PermissionType.SCHEDULE_EDIT_WORKED);
            changeProperty(SystemProperties.SHIFT_CHECK_IN_WORKED_ROSTER_BEFORE_CREATING, false);
            unit = OrgUnitRepository.getRandomOrgUnitWithWorkedRosters(true);
        } else {
            timePosition = ShiftTimePosition.FUTURE;
            unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        }

        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(unit.getId(), null, true);
        Shift shift = PresetClass.defaultShiftPreset(ep, timePosition);
        LocalDate date = shift.getDateTimeInterval().getStartDate();
        Role role = PresetClass.createCustomPermissionRole(permissions);
        goToScheduleAsUser(role, unit);
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        clickOnTargetShift(ep, date, scheduleWorker);
        String comment = RandomStringUtils.randomAlphabetic(8);
        inputComment(comment);
        clickEditShiftButton();
        clickOnTargetShift(ep, date, scheduleWorker);
        assertShiftCommentAdding(comment, shift);
    }

    @Test(groups = {"ABCHR4110", G2, SHIFTS, SCHED41,
            "@Before disable merged view for planned and actual shifts",
            "@Before disable start time check for worked shifts"},
            dataProvider = "true/false", dataProviderClass = DataProviders.class,
            description = "Свободный ввод комментария при создании смены в табеле")
    @Link(name = "Статья: \"4110_Комментарий к смене должно быть можно задавать только из списка\"", url = "https://wiki.goodt.me/x/uQ36D")
    @TmsLink("60290")
    @Owner(SCHASTLIVAYA)
    @Tag(SCHED41)
    public void customCommentWhileAddingShift(boolean workedShifts) {
        changeTestIDDependingOnParameter(workedShifts, "АВСНR4110-4", "АВСНR4110-2",
                                         "Свободный ввод комментария при создании смены из планового графика");
        OrgUnit unit;
        ShiftTimePosition timePosition;
        List<PermissionType> permissions = new ArrayList<>(Arrays.asList(PermissionType.SCHEDULE_VIEW,
                                                                         PermissionType.SCHEDULE_EDIT,
                                                                         PermissionType.SHIFT_ALLOW_CUSTOM_COMMENT,
                                                                         PermissionType.ORG_UNIT_EMPLOYEE_PERSONALISATION,
                                                                         PermissionType.SCHEDULE_DAY_COMMENTS,
                                                                         PermissionType.ROSTER_SHIFT_EDIT_OR_CREATE,
                                                                         PermissionType.SHIFT_READ_COMMENT,
                                                                         PermissionType.SHIFT_MANAGE_COMMENT));
        if (workedShifts) {
            timePosition = ShiftTimePosition.PAST;
            permissions.add(PermissionType.SCHEDULE_EDIT_WORKED);
            changeProperty(SystemProperties.SHIFT_CHECK_IN_WORKED_ROSTER_BEFORE_CREATING, false);
            unit = OrgUnitRepository.getRandomOrgUnitWithWorkedRosters(true);
        } else {
            timePosition = ShiftTimePosition.FUTURE;
            unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        }
        PresetClass.changeOrSetMathParamValue(unit.getId(), MathParameterValues.ACCESS_TO_OVERWORK, true, true);
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(unit.getId(), null, true);
        LocalDate date = PresetClass.getFreeDateForEmployeeShiftPreset(ep, timePosition);
        Role role = PresetClass.createCustomPermissionRole(permissions);
        goToScheduleAsUser(role, unit);
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        clickOnEmptyCell(ep, date, scheduleWorker);
        clickOnPlusCellOnGraph();
        LocalTime start = LocalTime.of(10, 0, 0);
        LocalTime end = LocalTime.of(18, 0, 0);
        enterShiftTimeStartOrEnd(start, TimeTypeField.START_TIME);
        enterShiftTimeStartOrEnd(end, TimeTypeField.END_TIME);
        String comment = RandomStringUtils.randomAlphabetic(8);
        inputComment(comment);
        createShiftButtonClick();
        assertCreateShift(ep, new DateTimeInterval(date.atTime(start),
                                                   date.atTime(end)), scheduleWorker, false);
        Shift shift = ShiftRepository.getShift(ep, date, timePosition);
        clickOnTargetShift(ep, date, scheduleWorker);
        assertShiftCommentAdding(comment, shift);
    }

    @Test(groups = {"ABCHR4110", G2, SHIFTS, SCHED41,
            "@Before allow request comments"},
            description = "Свободный ввод комментария при редактировании запроса")
    @Link(name = "Статья: \"4110_Комментарий к смене должно быть можно задавать только из списка\"", url = "https://wiki.goodt.me/x/uQ36D")
    @TmsLink("60290")
    @Tag("АВСНR4110-5")
    @Owner(SCHASTLIVAYA)
    @Tag(SCHED41)
    public void customCommentWhileEditingRequest() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        Role role = PresetClass.createCustomPermissionRole(Arrays.asList(PermissionType.SCHEDULE_VIEW,
                                                                         PermissionType.SCHEDULE_EDIT,
                                                                         PermissionType.SCHEDULE_MANAGE_REQUESTS,
                                                                         PermissionType.SHIFT_ALLOW_CUSTOM_COMMENT,
                                                                         PermissionType.ORG_UNIT_EMPLOYEE_PERSONALISATION,
                                                                         PermissionType.SHIFT_READ_COMMENT,
                                                                         PermissionType.SHIFT_MANAGE_COMMENT
        ));
        ScheduleRequestType type = ScheduleRequestType.getRandomAbsenceRequest();
        PresetClass.addScheduleRequestTypeRights(role, type);
        ScheduleRequest request = PresetClass.createScheduleRequestOfCertainType(ScheduleRequestStatus.APPROVED,
                                                                                 false, omId, type);
        assert request != null;
        goToScheduleAsUser(role, unit);
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        clickOnRequestBlock(request, scheduleWorker, unit);
        String comment = RandomStringUtils.randomAlphabetic(8);
        inputComment(comment);
        clickEditShiftButton();
        clickOnRequestBlock(request, scheduleWorker, unit);
        assertRequestCommentAdding(comment, request, omId);
    }

    @Test(groups = {"ABCHR4110", G2, SHIFTS, SCHED41,
            "@Before disable filter alias by org unit format",
            "@Before allow request comments"},
            description = "Свободный ввод комментария при создании запроса")
    @Link(name = "Статья: \"4110_Комментарий к смене должно быть можно задавать только из списка\"", url = "https://wiki.goodt.me/x/uQ36D")
    @TmsLink("60290")
    @Tag("АВСНR4110-6")
    @Owner(SCHASTLIVAYA)
    @Tag(SCHED41)
    public void customCommentWhileAddingRequest() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        Role role = PresetClass.createCustomPermissionRole(Arrays.asList(PermissionType.SCHEDULE_VIEW,
                                                                         PermissionType.SCHEDULE_EDIT,
                                                                         PermissionType.SCHEDULE_MANAGE_REQUESTS,
                                                                         PermissionType.SCHEDULE_MANAGE_REQUESTS_APPROVE,
                                                                         PermissionType.SHIFT_ALLOW_CUSTOM_COMMENT,
                                                                         PermissionType.ORG_UNIT_EMPLOYEE_PERSONALISATION,
                                                                         PermissionType.SHIFT_ALLOW_CUSTOM_COMMENT,
                                                                         PermissionType.SHIFT_MANAGE_COMMENT,
                                                                         PermissionType.SHIFT_READ_COMMENT
        ));
        ScheduleRequestType type = ScheduleRequestType.VACATION;
        changeSystemListEnableValue(type, true);
        changeSystemListRequireApprovalValue(type, false);
        PresetClass.addScheduleRequestTypeRights(role, type);

        int omId = unit.getId();
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        LocalDate date = PresetClass.getFreeDateForEmployeeShiftPreset(ep, ShiftTimePosition.FUTURE);
        goToScheduleAsUser(role, unit);
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        clickOnEmptyCell(ep, date, scheduleWorker);
        clickOnPlusCellOnGraph();
        clickOnSelectRequestTypeButton();
        selectRequestType(type);
        String comment = RandomStringUtils.randomAlphabetic(8);
        inputComment(comment);
        createShiftButtonClick();
        assertRequestAdding(ep, date, type, scheduleWorker, unit, ScheduleRequestStatus.APPROVED);
        ScheduleRequest request = ScheduleRequestRepository
                .getEmployeeScheduleRequestsByType(ep.getEmployee().getId(), new DateInterval(date), omId, type)
                .stream().findFirst().orElse(null);
        assert request != null;
        clickOnRequestBlock(request, scheduleWorker, unit);
        assertRequestCommentAdding(comment, request, omId);
    }

    @Test(groups = {"ABCHR4110", G2, SHIFTS, SCHED41},
            dataProvider = "true/false", dataProviderClass = DataProviders.class,
            description = "Ввод комментария при отсутствии разрешения на свободный ввод (плановый график)",
            expectedExceptions = AssertionError.class, expectedExceptionsMessageRegExp = ANY + COMMENT_NOT_INPUTTED + ANY)
    @Link(name = "Статья: \"4110_Комментарий к смене должно быть можно задавать только из списка\"", url = "https://wiki.goodt.me/x/uQ36D")
    @TmsLink("60290")
    @Tag("АВСНR4110-7.1")
    @Owner(SCHASTLIVAYA)
    @Tag(SCHED41)
    public void customShiftCommentWithoutPermission(boolean workedShifts) {
        OrgUnit unit;
        ShiftTimePosition timePosition;
        List<PermissionType> permissions = new ArrayList<>(Arrays.asList(PermissionType.SCHEDULE_VIEW,
                                                                         PermissionType.SCHEDULE_EDIT,
                                                                         PermissionType.ORG_UNIT_EMPLOYEE_PERSONALISATION,
                                                                         PermissionType.SHIFT_MANAGE_COMMENT,
                                                                         PermissionType.SHIFT_READ_COMMENT));
        if (workedShifts) {
            timePosition = ShiftTimePosition.PAST;
            permissions.add(PermissionType.SCHEDULE_EDIT_WORKED);
            changeProperty(SystemProperties.SHIFT_CHECK_IN_WORKED_ROSTER_BEFORE_CREATING, false);
            unit = OrgUnitRepository.getRandomOrgUnitWithWorkedRosters(true);
            changeTestName("Ввод комментария при отсутствии разрешения на свободный ввод (табель)");
        } else {
            timePosition = ShiftTimePosition.FUTURE;
            unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        }
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(unit.getId(), null, true);
        Shift shift = PresetClass.defaultShiftPreset(ep, timePosition);
        Role role = PresetClass.createCustomPermissionRole(permissions);
        goToScheduleAsUser(role, unit);
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        clickOnTargetShift(ep, shift.getDateTimeInterval().getStartDate(), scheduleWorker);
        String comment = RandomStringUtils.randomAlphabetic(8);
        inputComment(comment);
    }

    @Test(groups = {"АВСНR4110-7.2", G2, SHIFTS, SCHED41,
            "@Before allow request comments"},
            description = "Ввод комментария при отсутствии разрешения на свободный ввод (запрос)",
            expectedExceptions = AssertionError.class, expectedExceptionsMessageRegExp = ANY + COMMENT_NOT_INPUTTED + ANY)
    @Link(name = "Статья: \"4110_Комментарий к смене должно быть можно задавать только из списка\"", url = "https://wiki.goodt.me/x/uQ36D")
    @TmsLink("60290")
    @Tag("АВСНR4110-7.2")
    @Owner(SCHASTLIVAYA)
    @Tag(SCHED41)
    public void customRequestCommentWithoutPermission() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        ScheduleRequestType type = ScheduleRequestType.getRandomDayTypeRequest();
        ScheduleRequest request = PresetClass.createScheduleRequestOfCertainType(ScheduleRequestStatus.APPROVED,
                                                                                 false, unit.getId(), type);
        Role role = PresetClass.createCustomPermissionRole(Arrays.asList(PermissionType.SCHEDULE_VIEW,
                                                                         PermissionType.SCHEDULE_EDIT,
                                                                         PermissionType.SCHEDULE_MANAGE_REQUESTS,
                                                                         PermissionType.SHIFT_MANAGE_COMMENT,
                                                                         PermissionType.SHIFT_READ_COMMENT));
        PresetClass.addScheduleRequestTypeRights(role, type);
        goToScheduleAsUser(role, unit);
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        clickOnRequestBlock(request, scheduleWorker, unit);
        String comment = RandomStringUtils.randomAlphabetic(8);
        inputComment(comment);
    }

    @Test(groups = {"ABCHR5780", G2, LIST1,
            "@Before disable merged view for planned and actual shifts",
            "@Before disable filter alias by org unit format",
            "@Before disable all shift comments"},
            dataProvider = "requestStatus",
            description = "Создание автоматически подтвержденного запроса при включенной настройке \"Автоподтверждение запроса\"")
    @Link(name = "Статья: \"5780_Создание подтвержденных/неподтвержденных запросов при наличии флага\"", url = "https://wiki.goodt.me/x/gjH0DQ")
    @TmsLink("60298")
    @Tag(LIST1)
    public void createApprovedAndNotApprovedScheduleRequest(ScheduleRequestStatus status) {
        changeTestIDDependingOnParameter(status == ScheduleRequestStatus.NOT_APPROVED, "ABCHR5780-2", "ABCHR5780-1",
                                         "Создание неподтвержденного запроса при выключенной настройке \"Автоподтверждение запроса\"");
        OrgUnit unit;
        EmployeePosition ep;
        if (URL_BASE.contains("magnit")) {
            ImmutablePair<OrgUnit, EmployeePosition> pair = OrgUnitRepository.getRandomOrgUnitWithRandomEmployeeForShiftPair(false);
            unit = pair.left;
            ep = pair.right;
        } else {
            unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
            ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(unit.getId(), null, false);
        }
        List<PermissionType> permissions = new ArrayList<>(Arrays.asList(
                PermissionType.SCHEDULE_VIEW,
                PermissionType.SCHEDULE_EDIT,
                PermissionType.SCHEDULE_MANAGE_REQUESTS_CREATE,
                PermissionType.ORG_UNIT_EMPLOYEE_PERSONALISATION
        ));
        Role role = PresetClass.createCustomPermissionRole(permissions);
        ScheduleRequestType requestType = ScheduleRequestType.getRandomDayTypeRequest();
        PresetClass.addScheduleRequestTypeRights(role, requestType);
        changeSystemListEnableValue(requestType, true);
        changeSystemListAutoApproveValue(requestType, status == ScheduleRequestStatus.APPROVED);
        LocalDate freeDate = PresetClass.getFreeDateForEmployeeShiftPreset(ep, ShiftTimePosition.FUTURE);
        goToScheduleAsUser(role, unit, ep.getEmployee().getUser());
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        clickOnEmptyCell(ep, freeDate, scheduleWorker);
        clickOnPlusCellOnGraph();
        clickOnSelectRequestTypeButton();
        selectRequestType(requestType);
        createShiftButtonClick();
        assertRequestAdding(ep, freeDate, requestType, scheduleWorker, unit, status);
    }

    @Test(groups = {"ABCHR4281-5", G1, SCHED21,
            "@Before disable pre-publication checks", "@Before publish with lacking norms",
            "@Before publish without checking for yearly overtime limit violation"},
            description = "Доступ к отправке графика на утверждение при наличии прав")
    @Link(name = "4281_Добавить права для блоков Расписание, Оргструктура и Разное", url = "https://wiki.goodt.me/x/yAr6D")
    @TmsLink("60331")
    @Owner(BUTINSKAYA)
    @Tag("ABCHR4281-5")
    @Tag(SCHED21)
    public void accessToSendRosterOnApproval() {
        List<PermissionType> permissions = new ArrayList<>(Arrays.asList(
                PermissionType.SCHEDULE_VIEW,
                PermissionType.SCHEDULE_EDIT,
                PermissionType.ROSTER_SEND_TO_APPROVE,
                PermissionType.ORG_UNIT_EMPLOYEE_PERSONALISATION,
                PermissionType.NORMATIVE_SHOW
        ));
        Role role = PresetClass.createCustomPermissionRole(permissions);
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        PresetClass.publishGraphPreset(GraphStatus.NOT_PUBLISH, unit);
        goToScheduleAsUser(role, unit);
        threeDotsMenuClick();
        ZonedDateTime localDateTimeServer = ZonedDateTime.now();
        chooseFunction(VariantsOfFunctions.FOR_APPROVAL);
        assertPopUpForApproval(localDateTimeServer, unit);
    }

    @Test(groups = {"ABCHR4281-5-1", "ABCHR4281-5", G1, SCHED21,
            "@Before disable pre-publication checks"},
            description = "Доступ к отправке графика на утверждение при отсутствии прав",
            expectedExceptions = AssertionError.class, expectedExceptionsMessageRegExp = ANY + OPTION_NOT_DISPLAYED + ANY)
    @Link(name = "4281_Добавить права для блоков Расписание, Оргструктура и Разное", url = "https://wiki.goodt.me/x/yAr6D")
    @TmsLink("60331")
    @Owner(BUTINSKAYA)
    @Tag("ABCHR4281-5")
    @Tag(SCHED21)
    public void accessToSendRosterOnApprovalWithoutPermission() {
        List<PermissionType> permissions = new ArrayList<>(Arrays.asList(
                PermissionType.SCHEDULE_VIEW,
                PermissionType.SCHEDULE_EDIT,
                PermissionType.ORG_UNIT_EMPLOYEE_PERSONALISATION
        ));
        Role role = PresetClass.createCustomPermissionRole(permissions);
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        PresetClass.publishGraphPreset(GraphStatus.NOT_PUBLISH, unit);
        goToScheduleAsUser(role, unit);
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.FOR_APPROVAL);
    }

    @Test(groups = {"ABCHR4048", G2, SCHED9,
            "@Before disable all shift comments",
            "@Before enable schedule request: day off",
            "@Before check if last day of month"},
            description = "Редактирование своего неподтвержденного запроса сотрудником при наличии прав")
    @Link(name = "Статья: \"4048_Добавить права на удаление не подтвержденных запросов\"", url = "https://wiki.goodt.me/x/EAH6D")
    @TmsLink("60300")
    @Tag("ABCHR4048-1")
    @Tag(SCHED9)
    public void editNotApprovedScheduleRequest() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, false);
        List<PermissionType> permissions = new ArrayList<>(Arrays.asList(
                PermissionType.SCHEDULE_VIEW,
                PermissionType.SCHEDULE_EDIT,
                PermissionType.SCHEDULE_MANAGE_REQUESTS_CREATE,
                PermissionType.ORG_UNIT_EMPLOYEE_PERSONALISATION
        ));
        Role role = PresetClass.createCustomPermissionRole(permissions);
        ScheduleRequestType requestType = ScheduleRequestType.VACATION;
        PresetClass.addScheduleRequestTypeRights(role, requestType);
        LocalDate date = ShiftTimePosition.FUTURE_WITHOUT_LAST_DAY.getShiftsDateInterval().getRandomDateBetween();
        PresetClass.presetForEmptyRequestCell(ep.getEmployee(), date);
        ScheduleRequest scheduleRequest = PresetClass.createScheduleRequestForDate(ScheduleRequestStatus.NOT_APPROVED,
                                                                                   date, ep, requestType);
        LocalDate newDate = date.plusDays(1);
        PresetClass.clearDateForChangeRequest(scheduleRequest, omId, newDate);
        goToScheduleAsUser(role, unit, ep.getEmployee().getUser());
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        clickOnRequestBlock(scheduleRequest, scheduleWorker, unit);
        listOfRequestsThreeDotsButtonClick();
        enterShiftDateStartOrEnd(newDate, DateTypeField.START_DATE);
        if (sb.formEditForm().dateStartOrEndInput(DateTypeField.END_DATE.getName()).isDisplayed()) {
            enterShiftDateStartOrEnd(newDate, DateTypeField.END_DATE);
        }
        clickEditShiftButton();
        assertRequestChange(scheduleRequest, newDate, scheduleWorker, unit, ScheduleRequestStatus.NOT_APPROVED);
    }

    @Test(groups = {"ABCHR4048", G2, SCHED9,
            "@Before disable all shift comments",
            "@Before enable schedule request: day off",
            "@Before check if last day of month"},
            description = "Удаление своего неподтвержденного запроса сотрудником при наличии прав")
    @Link(name = "Статья: \"4048_Добавить права на удаление не подтвержденных запросов\"", url = "https://wiki.goodt.me/x/EAH6D")
    @TmsLink("60300")
    @Tag("ABCHR4048-2")
    @Tag(SCHED9)
    public void deleteNotApprovedScheduleRequest() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, false);
        List<PermissionType> permissions = new ArrayList<>(Arrays.asList(
                PermissionType.SCHEDULE_VIEW,
                PermissionType.SCHEDULE_EDIT,
                PermissionType.SCHEDULE_MANAGE_REQUESTS_CREATE,
                PermissionType.ORG_UNIT_EMPLOYEE_PERSONALISATION
        ));
        Role role = PresetClass.createCustomPermissionRole(permissions);
        ScheduleRequestType requestType = ScheduleRequestType.OFF_TIME;
        PresetClass.addScheduleRequestTypeRights(role, requestType);
        changeSystemListEnableValue(requestType, true);
        LocalDate date = ShiftTimePosition.FUTURE_WITHOUT_LAST_DAY.getShiftsDateInterval().getRandomDateBetween();
        PresetClass.presetForEmptyRequestCell(ep.getEmployee(), date);
        ScheduleRequest scheduleRequest = PresetClass.createScheduleRequestForDate(ScheduleRequestStatus.NOT_APPROVED,
                                                                                   date, ep, requestType);
        goToScheduleAsUser(role, unit, ep.getEmployee().getUser());
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        clickOnRequestBlock(scheduleRequest, scheduleWorker, unit);
        listOfRequestsThreeDotsButtonClick();
        selectAction(RequestAction.DELETE, true);
        assertRequestDeleting(scheduleRequest, scheduleWorker, unit);
    }

    @Test(groups = {"ABCHR4048", G2, SCHED9,
            "@Before disable all shift comments",
            "@Before enable schedule request: day off",
            "@Before check if last day of month"},
            description = "Редактирование чужого неподтвержденного запроса сотрудником при наличии прав")
    @Link(name = "Статья: \"4048_Добавить права на удаление не подтвержденных запросов\"", url = "https://wiki.goodt.me/x/EAH6D")
    @TmsLink("60300")
    @Tag("ABCHR4048-3")
    @Tag(SCHED9)
    public void editNotApprovedScheduleRequestOfAnotherEmployee() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        List<EmployeePosition> epList = EmployeePositionRepository.getSeveralRandomEmployeesWithCheckByApi(omId, 2, false);
        Employee employee = epList.get(1).getEmployee();
        EmployeePosition anotherEp = epList.get(0);
        List<PermissionType> permissions = new ArrayList<>(Arrays.asList(
                PermissionType.SCHEDULE_VIEW,
                PermissionType.SCHEDULE_MANAGE_REQUESTS_CREATE,
                PermissionType.ORG_UNIT_EMPLOYEE_PERSONALISATION
        ));
        Role role = PresetClass.createCustomPermissionRole(permissions);
        ScheduleRequestType requestType = ScheduleRequestType.OFF_TIME;
        PresetClass.addScheduleRequestTypeRights(role, requestType);
        changeSystemListEnableValue(requestType, true);
        LocalDate date = ShiftTimePosition.FUTURE_WITHOUT_LAST_DAY.getShiftsDateInterval().getRandomDateBetween();
        PresetClass.presetForEmptyRequestCell(anotherEp.getEmployee(), date);
        ScheduleRequest scheduleRequest = PresetClass.createScheduleRequestForDate(ScheduleRequestStatus.NOT_APPROVED,
                                                                                   date, anotherEp, requestType);
        goToScheduleAsUser(role, unit, employee.getUser());
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        clickOnRequestBlock(scheduleRequest, scheduleWorker, unit);
        Assert.assertThrows(WaitUntilException.class, this::listOfRequestsThreeDotsButtonClick);
    }

    @Test(groups = {"ABCHR4048", G2, SCHED9,
            "@Before disable all shift comments",
            "@Before enable schedule request: day off",
            "@Before check if last day of month"},
            description = "Редактирование подтвержденного запроса сотрудником с правом только на неподтвержденные запросы",
            expectedExceptions = WaitUntilException.class, expectedExceptionsMessageRegExp = ANY + THREE_DOTS_BUTTON_NOT_DISPLAYED_ON_SHIFT_EDIT_SCREEN + ANY)
    @Link(name = "Статья: \"4048_Добавить права на удаление не подтвержденных запросов\"", url = "https://wiki.goodt.me/x/EAH6D")
    @TmsLink("60300")
    @Tag(SCHED9)
    @Tag("ABCHR4048-4")
    public void editApprovedScheduleRequest() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, false);
        List<PermissionType> permissions = new ArrayList<>(Arrays.asList(
                PermissionType.SCHEDULE_VIEW,
                PermissionType.SCHEDULE_MANAGE_REQUESTS_CREATE,
                PermissionType.ORG_UNIT_EMPLOYEE_PERSONALISATION
        ));
        Role role = PresetClass.createCustomPermissionRole(permissions);
        ScheduleRequestType requestType = ScheduleRequestType.OFF_TIME;
        PresetClass.addScheduleRequestTypeRights(role, requestType);
        changeSystemListEnableValue(requestType, true);
        LocalDate date = ShiftTimePosition.FUTURE_WITHOUT_LAST_DAY.getShiftsDateInterval().getRandomDateBetween();
        PresetClass.presetForEmptyRequestCell(ep.getEmployee(), date);
        ScheduleRequest scheduleRequest = PresetClass.createScheduleRequestForDate(ScheduleRequestStatus.APPROVED,
                                                                                   date, ep, requestType);
        goToScheduleAsUser(role, unit, ep.getEmployee().getUser());
        clickOnRequestBlock(scheduleRequest, new ScheduleWorker(sb), unit);
        listOfRequestsThreeDotsButtonClick();
    }

    @Test(groups = {"ABCHR4363-1", G1, SCHED9,
            "@Before disable merged view for planned and actual shifts"},
            dataProvider = "true/false", dataProviderClass = DataProviders.class,
            description = "Удаление запроса отсутствия в табеле при наличии разрешения")
    @Link(name = "Статья: \"4363_Добавить возможность обычному пользователю удалять смены и запросы в табеле\"", url = "https://wiki.goodt.me/x/9BD6D")
    @TmsLink("60310")
    @Tag(SCHED9)
    private void deleteRequestInWorkedRoster(boolean hasAccess) {
        changeTestIDDependingOnParameter(hasAccess, "ABCHR4363-1", "ABCHR4363-1",
                                         "Удаление запроса отсутствия в табеле при отсутствии разрешения");
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitWithWorkedRosters(true, false);
        int omId = unit.getId();
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        List<PermissionType> permissions = new ArrayList<>(Arrays.asList(PermissionType.SCHEDULE_VIEW,
                                                                         PermissionType.SCHEDULE_EDIT,
                                                                         PermissionType.ORG_UNIT_EMPLOYEE_PERSONALISATION,
                                                                         PermissionType.SCHEDULE_EDIT_WORKED));
        if (hasAccess) {
            permissions.add(PermissionType.SCHEDULE_MANAGE_REQUESTS);
        }
        Role role = PresetClass.createCustomPermissionRole(permissions);
        ShiftTimePosition timePosition = ShiftTimePosition.PAST_MONTH;
        LocalDate freeDate = PresetClass.getFreeDateForEmployeeShiftPreset(ep, timePosition);
        ScheduleRequestType requestType = ScheduleRequestType.getRandomDayTypeRequest();
        PresetClass.addScheduleRequestTypeRights(role, requestType);
        changeSystemListEnableValue(requestType, true);
        ScheduleRequest scheduleRequest = PresetClass.createScheduleRequestForDate(ScheduleRequestStatus.APPROVED,
                                                                                   freeDate, ep, requestType);
        goToScheduleAsUser(role, unit, ep.getEmployee().getUser());
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        if (!freeDate.getMonth().equals(LocalDate.now().getMonth())) {
            clickBack();
        }
        clickOnRequestBlock(scheduleRequest, scheduleWorker, unit);
        if (hasAccess) {
            listOfRequestsThreeDotsButtonClick();
            selectAction(RequestAction.DELETE, true);
            assertRequestDeleting(scheduleRequest, scheduleWorker, unit);
        } else {
            try {
                listOfRequestsThreeDotsButtonClick();
                selectAction(RequestAction.DELETE, true);
                sb.errorMessage("Доступ запрещен").should(DisplayedMatcher.displayed());
            } catch (WaitUntilException e) {
            }
        }
    }

    @Test(groups = {"ABCHR4846-1", SHIFTS, G1, SCHED26, MAGNIT,
            "@After remove add works and rules",
            "@Before enable additional work",
            "@Before disable day off in schedule wizard",
            "@Before enable schedule wizard breaks",
            "@Before enable multiple work breaks"},
            description = "Создание смены с заданной периодичностью, перерывом и доп.работой для сотрудников через мастер планирования")
    @Link(name = "Статья: \"4846_Мастер планирования\"", url = "https://wiki.goodt.me/x/UoRSDQ")
    @TmsLink("60308")
    @Tag(SCHED26)
    @Tag("ABCHR4846-1")
    private void createShiftWithScheduleWizard() {
        Random random = new Random();
        int workDaysInCycle = random.nextInt(3) + 2;
        int freeDaysInCycle = random.nextInt(1) + 1;
        LocalDate dayInPlan = ShiftTimePosition.FUTURE_WITH_NEXT_MONTH.getShiftsDateInterval().getStartDate().plusDays(1);
        LocalDate endCycleDate = dayInPlan.plusDays(workDaysInCycle + freeDaysInCycle);
        ImmutablePair<OrgUnit, EmployeePosition> unitAndEmp = OrgUnitRepository.getRandomOrgUnitWithRandomEmployeeForShiftPair(false);
        OrgUnit unit = unitAndEmp.left;
        int omId = unit.getId();
        EmployeePosition ep = unitAndEmp.right;
        PresetClass.createEmptyPlannedRoster(omId);
        if (dayInPlan.plusDays(workDaysInCycle).getMonth() != LocalDate.now().getMonth()) {
            PresetClass.createEmptyPlannedRoster(omId, LocalDateTools.getFirstDate().plusMonths(1));
        }
        PresetClass.changeOrSetMathParamValue(omId, MathParameterValues.ACCESS_TO_OVERWORK, true, true);
        AdditionalWork addWork = AdditionalWorkRepository.getTestAddWork(false, "test_addWork");
        PresetClass.prepareAdditionalWorkForAllPositions(addWork, omId, LocalDate.now(), false);
        goToSchedule(unit);
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.SCHEDULE_WIZARD);
        enterStartOrEndCycleDate(dayInPlan, DateTypeField.START_CYCLE);
        enterStartOrEndCycleDate(endCycleDate, DateTypeField.END_CYCLE);
        LocalTime startShift = LocalTime.of(14, 0);
        LocalTime endShift = LocalTime.of(22, 0);
        enterShiftTime("Время", startShift, endShift);
        clickAddBreakButton();
        enterShiftTime("Перерыв 1", LocalTime.of(17, 0), LocalTime.of(18, 0));
        setCycleArm();
        enterWorkDaysInCycle(workDaysInCycle);
        enterFreeDaysInCycle(freeDaysInCycle);
        clickEmployeeCheckbox(ep.getEmployee());
        clickAddAdditionalWorkButtonInWizard();
        clickAddWorkTypeChevron();
        chooseAddWorkType(addWork.getOuterId());
        LocalTime startAddWork = LocalTime.of(18, 30);
        LocalTime endAddWork = LocalTime.of(19, 30);
        enterAddWorkTime(startAddWork, endAddWork);
        pressFormButton(true);
        ScheduleWorker sw = new ScheduleWorker(sb);
        assertShiftsAndAddWorksCreated(ep, new DateInterval(dayInPlan, dayInPlan.plusDays(workDaysInCycle - 1)), sw, addWork,
                                       startShift, endShift, startAddWork, endAddWork, 60L);
        assertScheduleRequestsAdded(ep, new DateInterval(dayInPlan.plusDays(workDaysInCycle), endCycleDate.minusDays(1)),
                                    sw, ScheduleRequestType.OFF_TIME, unit, ScheduleRequestStatus.APPROVED);
        assertCreateShift(ep, new DateTimeInterval(
                LocalDateTime.of(endCycleDate, startShift), LocalDateTime.of(endCycleDate, endShift)), sw, 60L);
    }

    @Test(groups = {"ABCHR4846-3", SHIFTS, G1, SCHED26, MAGNIT,
            "@Before enable schedule wizard breaks"},
            description = "Создание запросов отсутствия для сотрудников через мастер планирования")
    @Link(name = "Статья: \"4846_Мастер планирования\"", url = "https://wiki.goodt.me/x/UoRSDQ")
    @TmsLink("60308")
    @Tag(SCHED26)
    @Tag("ABCHR4846-3")
    private void createAbsenceRequestWithScheduleWizard() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        PresetClass.createEmptyPlannedRoster(omId);
        ScheduleRequestType type = ScheduleRequestType.getRandomAbsenceRequest();
        ScheduleRequestAlias alias = changeSystemListEnableValue(type, true);
        LocalDate startDate;
        LocalDate endDate;
        if (LocalDate.now().equals(LocalDateTools.getLastDate())) {
            startDate = LocalDateTools.getLastDate();
            endDate = startDate;
        } else {
            startDate = ShiftTimePosition.FUTURE_WITHOUT_LAST_DAY.getShiftsDateInterval().getRandomDateBetween();
            endDate = new DateInterval(startDate, LocalDateTools.getLastDate()).getRandomDateBetween();
        }
        PresetClass.makeClearDate(ep, startDate, endDate);
        goToSchedule(unit);
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.SCHEDULE_WIZARD);
        clickEmployeeCheckbox(ep.getEmployee());
        assert alias != null;
        enterStartOrEndCycleDate(startDate, DateTypeField.START_CYCLE);
        enterStartOrEndCycleDate(endDate, DateTypeField.END_CYCLE);
        clickRequestChevron();
        selectRequestTypeInScheduleWizard(alias.getTitle());
        if (sb.scheduleWizardForm().timeInput("Время").isDisplayed()) {
            enterShiftTime("Время", LocalTime.of(16, 0), LocalTime.of(18, 0));
        }
        pressFormButton(true);
        ScheduleWorker sw = new ScheduleWorker(sb);
        assertScheduleRequestsAdded(ep, new DateInterval(startDate, endDate), sw, type, unit, ScheduleRequestStatus.APPROVED);
    }

    @Test(groups = {"ABCHR2821", G1, SCHED21,
            "@Before no disable calculate conflicts",
            "@Before schedule board deviation from standard plan",
            "@Before disable check of worked roster before adding shift",
            "@Before conflicts and exceeding norms",
            "@Before don't show button to publish roster"},
            dataProvider = "true/false", dataProviderClass = DataProviders.class,
            description = "Отправка графика на утверждение при наличии критичных конфликтов и превышении нормы часов")
    @Link(name = "Статья: \"42821_Контроль качества версии графика в момент отправки и публикации графика\"",
            url = "https://wiki.goodt.me/x/LQctD")
    @TmsLink("60311")
    private void sendScheduleForApprovalOrPublishWithConstraintViolations(boolean forApproval) {
        changeTestIDDependingOnParameter(forApproval, "ABCHR2821-1", "ABCHR2821-5",
                                         "Публикация графика при наличии критичных конфликтов и нарушением нормы часов");
        addTagDependingOnParameter(forApproval, SCHED21, SCHED12);
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        PresetClass.setMathParamValue(omId, MathParameterValues.ACCESS_TO_OVERWORK, true);
        PresetClass.setMathParamValue(omId, MathParameterValues.SUM_ACCOUNTING_NORM, false);
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        PresetClass.changeParameterFieldForOM(unit, MathParameterRepository.getMathParameter(282), VALUE, true);
        List<String> messageList = Stream.of(ConstraintViolations.VIOLATION_HOUR_NORMS.getName(), ConstraintViolations.SHIFT_CROSS_NEXT_MONTH.getName())
                .collect(Collectors.toCollection(ArrayList::new));
        PresetClass.setPriorityLevelToConstraintViolation(ConstraintViolations.SHIFT_CROSS_NEXT_MONTH, ConstraintViolationLevel.HIGH, false);
        LocalDateTime startTime = LocalDateTime.of(LocalDateTools.getLastDate(), LocalTime.of(12, 0));
        PresetClass.presetForMakeShiftDateTime(ep, startTime, startTime.plusHours(14), ShiftTimePosition.FUTURE);
        PresetClass.presetForDeviationExcess(omId, ep);

        goToSchedule(unit);
        threeDotsMenuClick();
        LocalDateTime nowDateTime = LocalDateTime.now();
        if (forApproval) {
            chooseFunction(VariantsOfFunctions.FOR_APPROVAL);
            assertConstraintViolationDialogAppeared(ep, nowDateTime, omId, messageList, VariantsOfFunctions.FOR_APPROVAL);
        } else {
            chooseFunction(VariantsOfFunctions.PUBLICATION);
            publishButtonClick();
            assertConstraintViolationDialogAppeared(ep, nowDateTime, omId, messageList, VariantsOfFunctions.PUBLICATION);

        }
    }

    @Test(groups = {"ABCHR2821", G1, SCHED21,
            "@Before schedule board deviation from standard plan",
            "@Before disable check of worked roster before adding shift",
            "@Before conflicts and norms lack",
            "@Before don't show button to publish roster"},
            dataProvider = "true/false", dataProviderClass = DataProviders.class,
            description = "Отправка графика на утверждение при нехватке нормы часов")
    @Link(name = "Статья: \"42821_Контроль качества версии графика в момент отправки и публикации графика\"", url = "https://wiki.goodt.me/x/LQctD")
    @TmsLink("60311")
    private void sendScheduleForApprovalOrPublishWithNormsLack(boolean forApproval) {
        changeTestIDDependingOnParameter(forApproval, "ABCHR2821-4", "ABCHR2821-8",
                                         "Публикация графика при нехватке нормы часов");
        addTagDependingOnParameter(forApproval, SCHED21, SCHED12);
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        PresetClass.setMathParamValue(omId, MathParameterValues.SUM_ACCOUNTING_NORM, false);
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        PresetClass.changeParameterFieldForOM(unit, MathParameterRepository.getMathParameter(282), VALUE, true);
        PresetClass.publishGraphPreset(GraphStatus.NOT_PUBLISH, unit);
        PresetClass.presetForDeviationLack(omId, ep);
        List<String> messageList = Stream.of(ConstraintViolations.VIOLATION_HOUR_NORMS.getName()).collect(Collectors.toCollection(ArrayList::new));
        goToSchedule(unit);
        threeDotsMenuClick();
        LocalDateTime nowDateTime = LocalDateTime.now();
        if (forApproval) {
            chooseFunction(VariantsOfFunctions.FOR_APPROVAL);
            assertConstraintViolationDialogAppeared(ep, nowDateTime, omId, messageList, VariantsOfFunctions.FOR_APPROVAL);
        } else {
            chooseFunction(VariantsOfFunctions.PUBLICATION);
            publishButtonClick();
            assertConstraintViolationDialogAppeared(ep, nowDateTime, omId, messageList, VariantsOfFunctions.PUBLICATION);

        }
    }

    @Test(groups = {"ABCHR3531", CONFLICTS, G1,
            "@Before enable overtime access", "@Before activate the conflict indicator"},
            description = "Добавление запроса сверхурочной работы для сотрудника с атрибутом disability",
            dataProvider = "Employee attributes")
    @Link(name = "Статья: \"3531_Конфликт \"Сверхурочная работа для особых сотрудников\"\"", url = "https://wiki.goodt.me/x/0hotD")
    @TmsLink("60314")
    @Tag(SCHED33)
    public void addOvertimeRequestForEmployeeWithAttribute(EmployeeAttributes attribute, String value) {
        changeTestIDDependingOnParameter(!attribute.equals(EmployeeAttributes.CHILD_CARE_VACATION), "ABCHR3531-2",
                                         "ABCHR3531-1", "Добавление запроса сверхурочной работы для сотрудника с атрибутом childCareVacation");
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        PresetClass.addAttributeToEntity(MathParameterEntities.EMPLOYEE, ep.getEmployee().getId(), attribute, value);
        MathParameter mathParam = MathParameterRepository.getMathParameterWithValue(MathParameterEntities.ORGANIZATION_UNIT, "checkViolations");
        PresetClass.changeParameterFieldForOM(unit, mathParam, VALUE, true);
        PresetClass.setPriorityLevelToConstraintViolation(ConstraintViolations.FORBIDDEN_OVERTIME, ConstraintViolationLevel.HIGH, false);
        Shift shift = PresetClass.defaultShiftPreset(ep, ShiftTimePosition.FUTURE);
        LocalDate date = shift.getDateTimeInterval().getStartDate();
        List<OutsidePlanResource> overtimeBefore = OutsidePlanResourceRepository.getAllOutsideResources(RosterRepository.getActiveRosterThisMonth(omId).getId(), ShiftTimePosition.ALLMONTH);
        goToSchedule(unit);
        ScheduleWorker sw = new ScheduleWorker(sb);
        addRequestForShift(ep, date, sw);
        clickOnSelectRequestTypeButton();
        selectRequestType(ScheduleRequestType.OVERTIME);
        LocalDateTime startTime = shift.getDateTimeInterval().getEndDateTime();
        enterShiftTimeStartOrEnd(startTime.toLocalTime(), TimeTypeField.START_TIME);
        LocalDateTime endTime = startTime.plusHours(new Random().nextInt(4) + 1);
        if (!startTime.toLocalDate().equals(endTime.toLocalDate())) {
            enterShiftDateStartOrEnd(endTime.toLocalDate(), DateTypeField.END_DATE);
        }
        enterShiftTimeStartOrEnd(endTime.toLocalTime(), TimeTypeField.END_TIME);
        clickEditShiftButton();
        assertAddOvertime(ep, sw, overtimeBefore, omId, new DateTimeInterval(startTime, endTime));
        clickOnTargetShift(ep, shift.getDateTimeInterval().getStartDate(), sw);
        LocalTime start = shift.getDateTimeInterval().toTimeInterval().getStartTime().minusMinutes(1);
        LocalTime end = shift.getDateTimeInterval().toTimeInterval().getEndTime().minusMinutes(1);
        enterShiftTimeStartOrEnd(start, TimeTypeField.START_TIME);
        enterShiftTimeStartOrEnd(end, TimeTypeField.END_TIME);
        createShiftButtonClick();
        checkConstraintViolation(omId, ep, date, sw, "Сверхурочная работа запрещена для", false);
    }

    @Test(groups = {"ABCHR3547-1", G1, SCHED32},
            description = "Назначение на свободную смену сотрудника в декрете")
    @Link(name = "Статья: \"3547_Сотрудника в декрете не отображать при назначении смены с биржи\"", url = "https://wiki.goodt.me/x/YhstD")
    @TmsLink("60281")
    @Tag(SCHED32)
    @Tag("ABCHR3547-1")
    public void assignFreeShiftToEmployeeOnMaternityLeave() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(unit.getId(), null, true);
        Shift shift = PresetClass.defaultShiftPreset(ep);
        PresetClass.switchShiftExchange(unit, true);
        PresetClass.moveShiftToExchange(shift);
        LocalDate shiftDate = shift.getDateTimeInterval().getStartDate();
        PresetClass.assignMaternityLeaveStatus(ep, shiftDate.minusDays(1), shiftDate.plusDays(1));
        PresetClass.removeAllTagsFromOrgUnit(unit);
        goToSchedule(unit);
        clickFreeShift(shiftDate);
        int orderNumber = findShiftOrderNumber(shift.getDateTimeInterval(), shift.getPosGroup().getName());
        clickFreeShiftEmployeeList(orderNumber + 1);
        freeShiftEmployeesCheck(ep, false);
    }

    @Test(groups = {"ABCHR3196", G1, SHIFTS, SCHED39,
            "@Before disable multiple work breaks",
            "@Before disable all shift comments"},
            description = "Формирование обеда при создании смены с учетом правила для функциональной роли",
            dataProvider = "true/false", dataProviderClass = DataProviders.class)
    @Link(name = "Статья: \"3196_Правила формирования обеда при ручном создании смены\"", url = "https://wiki.goodt.me/x/sgwtD")
    @TmsLink("60315")
    @Tag(SCHED39)
    public void addLunchRules(boolean funcRoleRule) {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithPositionGroup(omId);
        LunchRule lunchRule;
        String lunchRuleStr = "[{\"from\":\"00:00\",\"to\":\"04:00\",\"length\":0},{\"from\":\"04:01\",\"to\":\"08:00\",\"length\":60},{\"from\":\"08:01\",\"to\":\"23:59\",\"length\":90}]";
        int posGroupId = ep.getPosition().getPositionGroupId();
        if (funcRoleRule) {
            addTag("ABCHR3196-1");
            lunchRule = PresetClass.addLunchRuleToPositionGroup(posGroupId, omId, lunchRuleStr);
        } else {
            addTag("ABCHR3196-2");
            PresetClass.deleteLunchRuleFromEmployeePosition(posGroupId, omId);
            lunchRule = PresetClass.addLunchRuleToOrgUnit(omId, lunchRuleStr);
            changeTestName("Формирование обеда при создании смены с учетом параметра для подразделения");
        }
        LocalDate dateValue = PresetClass.getFreeDateFromNow(ep);
        ScheduleWorker sw = new ScheduleWorker(sb);
        goToSchedule(unit);
        clickOnEmptyCell(ep, dateValue, sw);
        clickOnPlusCellOnGraph();
        LocalTime start = LocalTime.of(10, 0, 0);
        LocalTime end = LocalTime.of(20, 0, 0);
        enterShiftTimeStartOrEnd(start, TimeTypeField.START_TIME);
        enterShiftTimeStartOrEnd(end, TimeTypeField.END_TIME);
        createShiftButtonClick();
        int lunchTime = lunchRule.getLunchTime(start, end);
        assertCreateShiftWithLunchRule(ep, new DateTimeInterval(dateValue.atTime(start), dateValue.atTime(end)), sw, lunchTime);
    }

    @Test(groups = {"ABCHR3196-3", G1, SHIFTS, SCHED39,
            "Before disable all shift comments",
            "@Before disable multiple work breaks",
            "@Before disable the conflict indicator"},
            description = "Формирование обеда при создании смены с учетом параметра по умолчанию defaultLunch")
    @Link(name = "Статья: \"3196_Правила формирования обеда при ручном создании смены\"", url = "https://wiki.goodt.me/x/sgwtD")
    @TmsLink("60315")
    @Tag("ABCHR3196-3")
    @Tag(SCHED39)
    public void defaultLunchRule() {
        int lunchTime = 30;
        changeProperty(SystemProperties.DEFAULT_LUNCH, lunchTime);
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        PresetClass.deleteLunchRuleFromEmployeePosition(ep.getPosition().getPositionGroupId(), omId);
        PresetClass.deleteLunchRuleFromOrgUnitAndParent(unit);
        LocalDate dateValue = PresetClass.getFreeDateFromNow(ep);
        ScheduleWorker sw = new ScheduleWorker(sb);
        goToSchedule(unit);
        clickOnEmptyCell(ep, dateValue, sw);
        clickOnPlusCellOnGraph();
        LocalTime start = LocalTime.of(10, 0, 0);
        LocalTime end = LocalTime.of(20, 0, 0);
        enterShiftTimeStartOrEnd(start, TimeTypeField.START_TIME);
        enterShiftTimeStartOrEnd(end, TimeTypeField.END_TIME);
        createShiftButtonClick();
        assertCreateShiftWithLunchRule(ep, new DateTimeInterval(dateValue.atTime(start), dateValue.atTime(end)), sw, lunchTime);
    }

    @Test(groups = {"ABCHR4726-1", G2, LIST7},
            description = "Отображение сотрудника с признаком атрибута \"Выделять в расписании\"")
    @Link(name = "Статья: \"4726_В атрибут назначение добавить выделять в расписании\"", url = "https://wiki.goodt.me/x/sYJSDQ")
    @TmsLink("60285")
    @Tag(LIST7)
    @Tag("ABCHR4726-1")
    public void markEmployeeByBoldFontInSchedule() {
        EntityPropertiesKey key = PresetClass.addAttributeToSystemLists(MathParameterEntities.EMPLOYEE_POSITION, EmployeePositionAttributes.SCHEDULE_BOARD.toString());
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(unit.getId(), null, false);
        String employeeName = ep.getEmployee().getFullName();
        goToSchedule(unit);
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.SUBDIVISION_PROPERTIES);
        clickOnChevronButton(OmInfoName.EMPLOYEES);
        clickOnEmployeeThreeDots(employeeName);
        systemSleep(1); //на мастер-почте без этого ожидания атрибуты не подгружаются, на релизной и без него работает
        chooseEmployeeFunction(employeeName, EmployeeVariants.EMPLOYEE_POSITION_ATTRIBUTES);
        String randomValue = RandomStringUtils.randomAlphabetic(5);
        enterAttributeValue(key.getTitle(), randomValue);
        saveAttributeValueButtonClick();
        orgUnitPropCloseClick();
        refreshPage();
        assertEmployeeMarkedByBoldFont(ep, key, randomValue);
    }

    @Test(groups = {"ABCHR5817", G2, SCHED20, ZOZO},
            dataProvider = "System property for norms display",
            description = "Отображение нормы часов для сотрудников при значении настройки \"TEMPORARY\"")
    @Link(name = "Статья: \"5817_норматив часов сотрудника на временном назначении считать без учета периода назначения только относительно ставки.\"", url = "https://wiki.goodt.me/x/9Df0DQ")
    @TmsLink("60286")
    @Tag(SCHED20)
    public void displayStandardHoursForTemporaryEmployees(String propertyValue, String tag) {
        changeTestName(String.format("Отображение нормы часов для сотрудников при значении настройки %s равном \"%s\"",
                                     SystemProperties.FULL_INTERVAL_NORMS, propertyValue));
        addTag(tag);
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        List<EmployeePosition> eps = EmployeePositionRepository.
                getSeveralRandomEmployeesWithCheckByApi(unit.getId(), 2, true);
        LocalDate start = LocalDateTools.getFirstDate();
        LocalDate end = LocalDateTools.getLastDate();
        LocalDate now = LocalDate.now();
        EmployeePosition tempEp = eps.get(0);
        EmployeePosition permEp = eps.get(1);
        int rosterId = RosterRepository.getActiveRosterThisMonth(unit.getId()).getId();
        PresetClass.setMathParamValue(unit.getId(), MathParameterValues.SUM_ACCOUNTING_NORM, false);
        double tempNormBefore = DeviationFromStandardRepository.getRoundedStandardDeviation(rosterId, start, end, tempEp);
        double permNormBefore = DeviationFromStandardRepository.getRoundedStandardDeviation(rosterId, start, end, permEp);
        if (now.equals(start)) {
            now = start.plusDays(1);
        }
        tempEp = PresetClass.setTemporaryToEmployeePosition(tempEp, now, end, true);
        permEp = PresetClass.setTemporaryToEmployeePosition(permEp, now, end, false);
        changeProperty(SystemProperties.FULL_INTERVAL_NORMS, propertyValue);
        goToSchedule(unit);
        hoverOnEmployeeHours(tempEp.getEmployeePosition());
        hoverOnEmployeeHours(permEp.getEmployeePosition());
        assertEmployeeHoursNorm(tempEp, permEp, tempNormBefore, permNormBefore, propertyValue, rosterId, now, end);
    }

    @Test(groups = {"ABCHR4727-1", G2, LIST7},
            description = "Отображение атрибута с признаком \"Выводить в поп-ап\" во всплывающем окне")
    @Link(name = "Статья: \"4727_В атрибут назначения добавить выводить в pop up в расписании\"", url = "https://wiki.goodt.me/x/sIJSDQ")
    @TmsLink("60284")
    @Tag(LIST7)
    @Tag("ABCHR4727-1")
    public void showEmployeeHoursInPopup() {
        EntityPropertiesKey key = PresetClass.addAttributeToSystemLists(MathParameterEntities.EMPLOYEE_POSITION, EmployeePositionAttributes.POP_UP.toString());
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(unit.getId(), null, false);
        String employeeName = ep.getEmployee().getFullName();
        goToSchedule(unit);
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.SUBDIVISION_PROPERTIES);
        clickOnChevronButton(OmInfoName.EMPLOYEES);
        clickOnEmployeeThreeDots(employeeName);
        systemSleep(1); //на мастер-почте без этого ожидания атрибуты не подгружаются, на релизной и без него работает
        chooseEmployeeFunction(employeeName, EmployeeVariants.EMPLOYEE_POSITION_ATTRIBUTES);
        String randomValue = RandomStringUtils.randomAlphabetic(5);
        enterAttributeValue(key.getTitle(), randomValue);
        saveAttributeValueButtonClick();
        orgUnitPropCloseClick();
        refreshPage();
        hoverOnEmployeeHours(ep);
        assertAttributeDisplayedInPopup(ep, String.format("%s: %s", key.getTitle(), randomValue));
    }

    @Test(groups = {"ABCHR4717-1", G2, SCHED11},
            description = "Выводить текст ошибки в случае, если максимальная длина смены меньше минимальной")
    @Link(name = "Статья: \"4717_Выводить текст ошибки в случае если максимальная длина смены меньше минимальной\"", url = "https://wiki.goodt.me/x/ZyP0DQ")
    @TmsLink("60287")
    @Tag(SCHED11)
    @Tag("ABCHR4717-1")
    public void calculateShiftsForEmployeeWithMaxLessMinShift() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        int minShiftLength = 6;
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        Employee emp = ep.getEmployee();
        PresetClass.setMathParamValue(emp.getId(), MathParameterValues.MAX_LENGTH_OF_SHIFT, 5);
        PresetClass.setMathParamValue(emp.getId(), MathParameterValues.MIN_LENGTH_OF_SHIFT, minShiftLength);

        goToSchedule(unit);
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.CALCULATION_RECALCULATE_SHIFTS);
        enterCreateScheduleDateEndOrStart(LocalDateTools.getFirstDate(), DateTypeField.START_DATE);
        enterCreateScheduleDateEndOrStart(LocalDateTools.getLastDate(), DateTypeField.END_DATE);
        List<Roster> rostersBefore = RosterRepository.getRosters(omId);
        calculateButtonClickWithoutWait();
        String errorMessage = String.format("У сотрудника (%s %s %s) недопустимая длина смены, необходимо проверить заданные минимальную и максимальную длину смены",
                                            emp.getFirstName(), emp.getPatronymicName(), emp.getLastName());
        assertErrorMessageDisplayed(errorMessage);
        clickCloseCalcResult();
        List<Roster> rostersAfter = RosterRepository.getRosters(omId);
        assertRostersNotCreated(rostersBefore, rostersAfter);
    }

    @Test(groups = {"ABCHR3227", G1, SCHED11, POCHTA},
            description = "Расчет смен с нарушением правила пересменок",
            dataProvider = "true/false", dataProviderClass = DataProviders.class)
    @Link(name = "Статья: \"3227_Почта России | Сделать правило по пересменкам необязательным. Создать АТ\"", url = "https://wiki.goodt.me/x/ZA8tD")
    @TmsLink("60288")
    @Tag(SCHED11)
    public void shiftChangeRuleOptional(boolean continueCalculation) {
        changeTestIDDependingOnParameter(continueCalculation, "ABCHR3227-1", "ABCHR3227-2", "Прерывание расчета смен при нарушении правила пересменок");
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        Position chiefPos = PositionRepository.getChief(omId);
        if (chiefPos == null) {
            throw new AssertionError("В подразделении нет руководителя, невозможно установить пересменки так, чтобы было нарушено правило.");
        }
        if (chiefPos.getEmployee() == null) {
            throw new AssertionError("Руководителю подразделения невозможно установить пересменки, правило не будет нарушено.");
        }
        EmployeePosition ep = EmployeePositionRepository.getEmployeePosition(chiefPos.getEmployee().toString(), omId);
        List<EmployeePosition> eps = EmployeePositionRepository.getActualEmployeePositionsWithChief(omId);
        PresetClass.changeOrSetMathParamValue(ep.getId(), MathParameterValues.STAFF_WITH_SHIFT_EXCHANGE_RULES_GROUPS, "Вкл.", true);
        eps.stream().filter(e -> e.getId() != ep.getId())
                .collect(Collectors.toList())
                .forEach(e -> PresetClass.changeOrSetMathParamValue(e.getId(), MathParameterValues.STAFF_WITH_SHIFT_EXCHANGE_RULES_GROUPS, "Выкл.", false));

        goToSchedule(unit);
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.CALCULATION_RECALCULATE_SHIFTS);
        enterCreateScheduleDateEndOrStart(LocalDateTools.getFirstDate(), DateTypeField.START_DATE);
        enterCreateScheduleDateEndOrStart(LocalDateTools.getLastDate(), DateTypeField.END_DATE);
        List<Roster> rostersBefore = RosterRepository.getRosters(omId);
        rostersBefore.stream().findAny().orElseThrow(() -> new AssertionError("У подразделения нет ростеров"));
        List<Integer> previousIds = rostersBefore.stream().map(Roster::getId).collect(Collectors.toList());
        calculateButtonClickWithoutWait();
        String errorMessage = "Невозможно расставить пересменки.";
        if (continueCalculation) {
            clickContinueOrCancelCalculation(errorMessage,
                                             "Продолжить с нарушением пересменок");
            assertScheduleCalculation(previousIds, omId);
        } else {
            clickContinueOrCancelCalculation(errorMessage,
                                             "Отменить расчет");
            List<Roster> rostersAfter = RosterRepository.getRosters(omId);
            assertRostersNotCreated(rostersBefore, rostersAfter);
        }

    }

    @Test(groups = {"ABCHR4622-1", G2, SCHED11, POCHTA},
            description = "Сообщение об ошибке при невозможности рассчитать график без режима работы")
    @Link(name = "Статья: \"4622_Выводить ошибку пользователю о невозможности рассчитать график (отсутствует режим работы)\"", url = "https://wiki.goodt.me/x/xIZSDQ")
    @TmsLink("60280")
    @Tag(SCHED11)
    @Tag("ABCHR4622-1")
    public void calculateShiftsWithoutBusinessHours() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        PresetClass.deleteBusinessHours(omId);
        goToSchedule(unit);
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.CALCULATION_RECALCULATE_SHIFTS);
        enterCreateScheduleDateEndOrStart(LocalDateTools.getFirstDate(), DateTypeField.START_DATE);
        enterCreateScheduleDateEndOrStart(LocalDateTools.getLastDate(), DateTypeField.END_DATE);
        List<Roster> rostersBefore = RosterRepository.getRosters(omId);
        calculateButtonClickWithoutWait();
        assertErrorMessageDisplayed("Невозможно запустить перерасчет - отсутствует режим работы подразделения.");
        clickCloseCalcResult();
        List<Roster> rostersAfter = RosterRepository.getRosters(omId);
        assertRostersNotCreated(rostersBefore, rostersAfter);
    }

    @Test(groups = {"ABCHR4430-4", SHIFTS, G1, SCHED37, MAGNIT,
            "@Before enable additional work",
            "@Before display additional work only with chosen statuses"},
            dataProvider = "add work status",
            description = "Изменение статуса доп.работы в карточке смены")
    @Link(name = "Статья: \"4430_Доработать статусы к Доп. работам\"",
            url = "https://wiki.goodt.me/x/_hX6D")
    @Severity(SeverityLevel.NORMAL)
    @TmsLink("60244")
    @Tag("ABCHR4430-4")
    @Tag(SCHED37)
    public void changeAddWorkStatus(String statusName) {
        ImmutablePair<OrgUnit, EmployeePosition> unitAndEmp = OrgUnitRepository.getRandomOrgUnitWithRandomEmployeeForShiftPair(false);
        OrgUnit unit = unitAndEmp.left;
        EmployeePosition ep = unitAndEmp.right;
        int omId = unit.getId();
        PresetClass.changeOrSetMathParamValue(omId, MathParameterValues.ACCESS_TO_OVERWORK, true, true);
        AdditionalWork additionalWork = AdditionalWorkRepository.getTestAddWork(true, "test_addWork_withStatus");
        Shift shift = PresetClass.defaultShiftPreset(ep, ShiftTimePosition.FUTURE);
        if (additionalWork.getAddWorkStatus(statusName) == null) {
            PresetClass.setStatusToAdditionalWork(additionalWork, statusName);
        }
        PresetClass.prepareAdditionalWorkForAllPositions(additionalWork, omId, shift.getStartDate(), true);
        shift.getAdditionalWork().forEach(PresetClass::deleteRequest);
        DateTimeInterval interval = shift.getDateTimeInterval();
        PresetClass.addWorkToShift(interval.getStartDateTime().plusHours(1), interval.getEndDateTime().minusHours(1), shift, additionalWork);
        PresetClass.checkAndMakePublicationRoster(omId);

        goToSchedule(unit);
        LocalDate date = interval.getStartDate();
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        clickOnTargetShift(ep, date, scheduleWorker);
        clickChevronAddWorkStatus();
        selectAddWorkStatus(statusName);
        clickEditShiftButton();
        assertAddStatusToAdditionalWork(scheduleWorker, ep, date, statusName);
    }

    @Test(groups = {"ABCHR7123", /*G2, LIST22, IN_PROGRESS, MAGNIT,*/
            "@Before disable pre-publication checks",
            "@Before schedule board show limit change notification",
            "@Before disable limit check outstaff",
            "@After remove limits"},
            dataProvider = "Data for limit tests",
            description = "Отключение вывода уведомления о создании Лимита с типом \"Общий\" по сотрудникам Аутстафф")
    @Link(name = "Статья: \"7123_Лимиты. Отключение проверки по лимитам при публикации для аутстаффа и при создании Лимита\"", url = "https://wiki.goodt.me/x/RTBJDw")
    @TmsLink("60213")
    @Tag(LIST22)
    public void disableLimitNotificationForOutstaff(String tag, String testName, LimitType limitType, GraphStatus graphStatus) {
        addTag(tag);
        changeTestName(String.format(testName, limitType.getNameOfType()));
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        ShiftTimePosition timePosition = ShiftTimePosition.ALLMONTH;
        LocalDate date = PresetClass.getFreeDateForEmployeeShiftPreset(ep, timePosition);
        PresetClass.presetForMakeShiftDate(ep, date, false, timePosition);
        PresetClass.publishGraphPreset(graphStatus, unit);
        PositionGroup posGroup = PositionGroupRepository.getAnotherPosGroup(omId);
        Limits limit = new Limits()
                .setOrgUnitId(omId)
                .setLimitType(limitType.toString())
                .setPositionGroupId(posGroup.getId())
                .setPositionGroupName(posGroup.getName());
        PresetClass.createLimit(limit);
        if (limitType.equals(LimitType.GENERAL)) {
            PresetClass.presetForDeviationExcess(omId, ep);
        }
        PresetClass.publishGraphPreset(GraphStatus.PUBLISH, unit);
        PresetClass.addAttributeToEntity(MathParameterEntities.EMPLOYEE, ep.getEmployee().getId(), EmployeeAttributes.OUTSTAFF, true);
        if (graphStatus.equals(GraphStatus.PUBLISH)) {
            goToSchedule(unit);
            assertLimitWarningNotDisplayed();
        } else {
            PresetClass.publishGraphPreset(GraphStatus.NOT_PUBLISH, unit);
            goToSchedule(unit);
            threeDotsMenuClick();
            chooseFunction(VariantsOfFunctions.PUBLICATION);
            publishButtonClick();
            closePublicationForm();
            refreshPage();
            listOfSchedulesClick();
            ZonedDateTime dateTime = ZonedDateTime.now();
            publicationAssert(dateTime, unit);
        }
    }

    @Test(groups = {"ABCHR4120", G2, SCHED35,
            "@Before disable pre-publication checks"},
            dataProvider = "Scope types",
            description = "Отметки о присутствии сотрудников на _ представлении в Расписании")
    @Link(name = "4120_Красные отметки о присутствии сотрудников в течение дня на месячном представлении\"", url = "https://wiki.goodt.me/x/aw36D")
    @TmsLink("60249")
    @Tag(SCHED35)
    public void redMarksOnMonthView(ScopeType scopeType) {
        List<Integer> list = Arrays.asList(1, 2, 3);
        addTag("ABCHR4120-" + list.get(list.size() - (scopeType.ordinal() + 1)));
        String testName = "Отметки о присутствии сотрудников на %s представлении в Расписании";
        changeTestName(String.format(testName, scopeType.getScopeAdjective()));
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        List<EmployeePosition> eps = EmployeePositionRepository.getAllEmployeesWithCheckByApi(omId, null, true);
        if (eps.isEmpty()) {
            throw new AssertionError(NO_TEST_DATA + "В подразделении нет подходящих сотрудников");
        }
        List<EmployeePosition> epsWIthShifts = eps.stream()
                .filter(ep -> ShiftRepository.getShift(ep, LocalDate.now(), ShiftTimePosition.FUTURE) != null)
                .collect(Collectors.toList());
        if (epsWIthShifts.isEmpty()) {
            PresetClass.presetForMakeShiftDate(eps.stream().collect(randomItem()), LocalDate.now(), false, ShiftTimePosition.FUTURE);
        }
        PresetClass.publishGraphPreset(GraphStatus.PUBLISH, unit);
        PresetClass.createMarks(omId, new DateInterval(LocalDate.now()));
        goToSchedule(unit);
        switchScope(scopeType);
        assertMarksOnShiftsCreated(omId, eps);
    }

    @Test(groups = {"ABCHR3475-1", G2, SCHED10},
            description = "Отображение бейджей функциональных ролей")
    @Link(name = "Статья: \"3475_Добавить отображение название функциональной роли в бэйджах\"", url = "https://wiki.goodt.me/x/lhUtD")
    @TmsLink("60253")
    @Tag("ABCHR3475-1")
    @Tag(SCHED10)
    public void displayPositionGroupOnBadge() {
        OrgUnit unit = OrgUnitRepository.getRandomOmWithEmployeeOptions(OrgUnitOptions.EMPLOYEES_WITH_POSITION);
        List<EmployeePosition> eps = EmployeePositionRepository.getSeveralRandomEmployeesWithCheckByApi(unit.getId(), 2, true);
        Set<Integer> posGroupIds = new HashSet<>();
        eps.forEach(ep -> posGroupIds.add(ep.getPosition().getPositionGroupId()));
        String posGroupStr = "[" + posGroupIds.stream()
                .map(Object::toString)
                .collect(Collectors.joining(",")) + "]";
        changeProperty(SystemProperties.SCHEDULE_BOARD_BADGE_POSITION_GROUPS, posGroupStr);
        goToSchedule(unit);
        List<EmployeePosition> epsWithGroup = EmployeePositionRepository.getAllEmployeesWithCheckByApi(unit.getId(), null, true)
                .stream()
                .filter(ep -> posGroupIds.contains(ep.getPosition().getPositionGroupId()))
                .collect(Collectors.toList());
        assertBadgesWithPositionGroups(epsWithGroup, posGroupIds);
    }

    @Test(groups = {"TEST-177", G1, CONFLICTS, SCHED33,
            "@Before activate the conflict indicator"},
            description = "Отображение конфликта \"Нарушение непрерывного отдыха в неделю (42 часа)\"")
    @Link(name = "Статья: \"Расписание\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=178979652#id-%D0%A0%D0%B0%D1%81%D0%BF%D0%B8%D1%81%D0%B0%D0%BD%D0%B8%D0%B5-%D0%9A%D0%BE%D0%BD%D1%84%D0%BB%D0%B8%D0%BA%D1%82%D1%8B")
    @TmsLink("60256")
    @Tag("TEST-177")
    @Tag(SCHED33)
    public void violationOfInterruptedRest() {
        ShiftTimePosition shiftTimePosition = ShiftTimePosition.FUTURE;
        LocalDate now = LocalDate.now();
        LocalDate nextMonday = now.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        if (!nextMonday.plusDays(7).getMonth().equals(now.getMonth())) {
            shiftTimePosition = ShiftTimePosition.NEXT_MONTH;
            LocalDate startOfNextMonth = shiftTimePosition.getShiftsDateInterval().getStartDate();
            List<LocalDate> nextMonthMondays = new ArrayList<>();
            nextMonthMondays.add(startOfNextMonth.withDayOfMonth(8).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)));
            nextMonthMondays.add(startOfNextMonth.withDayOfMonth(15).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)));
            nextMonthMondays.add(startOfNextMonth.withDayOfMonth(22).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)));
            nextMonday = getRandomFromList(nextMonthMondays);
        }
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(unit.getId(), null, false);
        MathParameter mathParam = MathParameterRepository.getMathParameterWithValue(MathParameterEntities.ORGANIZATION_UNIT, "checkViolations");
        PresetClass.changeParameterFieldForOM(unit, mathParam, VALUE, true);
        PresetClass.enableConflictCalculationInSysList(ConstraintViolations.NO_LONG_REST_PER_WEEK);
        PresetClass.setPriorityLevelToConstraintViolation(ConstraintViolations.NO_LONG_REST_PER_WEEK, ConstraintViolationLevel.HIGH, false);
        for (int i = 0; i < 7; i++) {
            PresetClass.presetForMakeShiftDate(ep, nextMonday.plusDays(i), false, shiftTimePosition);
        }
        PresetClass.runConstViolationsCalc(RosterRepository.getNeededRosterId(shiftTimePosition, shiftTimePosition.getShiftsDateInterval(), omId).getId());
        goToSchedule(unit);
        if (shiftTimePosition == ShiftTimePosition.NEXT_MONTH) {
            clickForward();
        }
        checkConstraintViolation(omId, ep, nextMonday, new ScheduleWorker(sb), "Нарушение непрерывного отдыха в неделю (42 часа) у сотрудника", false);
    }

    @Test(groups = {"ABCHR-6311", G2, SHIFTS, SCHED41,
            "@Before deletion not request",
            "@Before comments on deleting shifts",
            "@Before comments on plan shifts",
            "@Before disable strong lock plan"},
            dataProvider = "true/false", dataProviderClass = DataProviders.class,
            description = "Разрешен свободный ввод комментария при одиночном удалении смены в плане, если включено “Расписание. Свободный ввод комментария к смене”")
    @Link(name = "Статья: \"6311_Ограничить свободный ввод комментария при удалении смены\"", url = "https://wiki.goodt.me/x/fiRJDw")
    @TmsLink("60232")
    @Tag(SCHED41)
    public void allowCommentsOnShiftDeleteInPlan(boolean hasPermission) {
        changeTestIDDependingOnParameter(hasPermission, "ABCHR-6311-1", "ABCHR-6311-3",
                                         "Запрещен свободный ввод комментария при одиночном удалении смены в плане, если не включено “Расписание. Свободный ввод комментария к смене”");
        checkFirstDayOfMonth();
        checkLastDayOfMonth();
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(unit.getId(), null, true);
        User user = ep.getEmployee().getUser();
        List<PermissionType> permissions = new ArrayList<>(Arrays.asList(PermissionType.SCHEDULE_EDIT,
                                                                         PermissionType.SCHEDULE_VIEW,
                                                                         PermissionType.ORG_UNIT_EMPLOYEE_PERSONALISATION));
        if (hasPermission) {
            permissions.add(PermissionType.SHIFT_ALLOW_CUSTOM_COMMENT);
        }
        Role role = PresetClass.createCustomPermissionRole(permissions);
        LocalDate date = new DateInterval(LocalDate.now().plusDays(1), LocalDateTools.getLastDate()).getRandomDateBetween();
        changeProperty(SystemProperties.PLAN_EDIT_FUTURE_DAYS, date.compareTo(LocalDate.now().minusDays(1)));
        Shift shift = PresetClass.presetForMakeShiftDate(ep, date, false, ShiftTimePosition.FUTURE);
        goToScheduleAsUser(role, unit, user);
        ScheduleWorker sw = new ScheduleWorker(sb);
        clickOnTargetShift(ep, date, sw);
        shiftThreeDotsClick();
        selectAction(RequestAction.DELETE, true);
        String comment = RandomStringUtils.randomAlphabetic(8);
        enterComment(comment);
        if (hasPermission) {
            pressDeleteButton();
            assertDeleteShift(ep, shift, sw);
        } else {
            assertEquals(sb.shiftDeletionDialog().deleteButton().getAttribute("disabled"), "true", "Кнопка \"Удалить\" активна");
            assertShiftNotDeleted(ep, shift, sw);
        }
    }

    @Test(groups = {"ABCHR-6311", G2, SHIFTS, SCHED41,
            "@Before disable merged view for planned and actual shifts",
            "@Before deletion not request",
            "@Before comments on deleting shifts",
            "@Before comments on shifts",
            "@Before disable check of worked roster before adding shift"},
            dataProvider = "true/false", dataProviderClass = DataProviders.class,
            description = "Разрешен свободный ввод комментария при одиночном удалении смены в табеле, если включено “Расписание. Свободный ввод комментария к смене”")
    @Link(name = "Статья: \"6311_Ограничить свободный ввод комментария при удалении смены\"", url = "https://wiki.goodt.me/x/fiRJDw")
    @TmsLink("60232")
    @Tag(SCHED41)
    public void allowCommentsOnShiftDeleteInTable(boolean hasPermission) {
        changeTestIDDependingOnParameter(hasPermission, "ABCHR-6311-1", "ABCHR-6311-3",
                                         "Запрещен свободный ввод комментария при одиночном удалении смены в табеле, если не включено “Расписание. Свободный ввод комментария к смене”");
        checkFirstDayOfMonth();
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitWithWorkedRosters(true);
        int omId = unit.getId();
        PresetClass.changeOrSetMathParamValue(omId, MathParameterValues.ACCESS_TO_OVERWORK, true, true);
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        User user = ep.getEmployee().getUser();
        List<PermissionType> permissions = new ArrayList<>(Arrays.asList(PermissionType.SCHEDULE_EDIT,
                                                                         PermissionType.SCHEDULE_VIEW,
                                                                         PermissionType.SCHEDULE_EDIT_WORKED,
                                                                         PermissionType.ORG_UNIT_EMPLOYEE_PERSONALISATION,
                                                                         PermissionType.SHIFT_READ_COMMENT,
                                                                         PermissionType.SHIFT_MANAGE_COMMENT));
        if (hasPermission) {
            permissions.add(PermissionType.SHIFT_ALLOW_CUSTOM_COMMENT);
        }
        ShiftTimePosition position = ShiftTimePosition.PAST;
        Role role = PresetClass.createCustomPermissionRole(permissions);
        LocalDate date = position.getShiftsDateInterval().getRandomDateBetween();
        Shift shift = PresetClass.presetForMakeShiftDate(ep, date, false, position);
        goToScheduleAsUser(role, unit, user);
        ScheduleWorker sw = new ScheduleWorker(sb);
        clickOnTargetShift(ep, date, sw);
        shiftThreeDotsClick();
        selectAction(RequestAction.DELETE, true);
        String comment = RandomStringUtils.randomAlphabetic(5);
        enterComment(comment);
        pressDeleteButton();
        if (hasPermission) {
            assertDeleteShift(ep, shift, sw);
        } else {
            assertShiftNotDeleted(ep, shift, sw);
        }
    }

    @Test(groups = {"ABCHR5045-1", G2, SHIFTS, SCHED36,
            "@Before enable check of worked shifts against plan",
            "@Before disable check of worked roster before adding shift",
            "@Before disable cutting of worked shifts to fit the plan",
            "@Before disable pre-publication checks",
            "@Before publish with lacking norms"},
            description = "Подсвечивание табельной смены, отсутствующей в плане")
    @Link(name = "Статья: \"5045_Подсвечивать смены в табеле, которые введены без плана или не соответствуют плану\"", url = "https://wiki.goodt.me/x/RYCyDQ")
    @TmsLink("60230")
    @Tag("ABCHR5045-1")
    @Tag(SCHED36)
    public void highlightShiftsInTableAbsentInPlan() {
        checkFirstDayOfMonth();
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitWithWorkedRosters(true);
        int omId = unit.getId();
        PresetClass.makeWorkDays(omId);
        PresetClass.changeOrSetMathParamValue(omId, MathParameterValues.ACCESS_TO_OVERWORK, true, true);
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        LocalDate date = ShiftTimePosition.PAST.getShiftsDateInterval().getRandomDateBetween();
        PresetClass.makeClearDate(ep, date);
        PresetClass.publishGraphPreset(GraphStatus.PUBLISH, unit);
        Allure.addAttachment("Смена", String.format("Смена сотрудника %s за %s отсутствует в плановом графике", ep, date));
        PresetClass.presetForMakeShiftDate(ep, date, false, ShiftTimePosition.PAST);
        changeProperty(SystemProperties.SCHEDULE_BOARD_CHECK_WORKED_DIFF_PLAN, true);
        goToSchedule(unit);
        assertShiftCellDiffersFromPlan(ep, date, true);
    }

    @Test(groups = {"ABCHR5045-2", G2, SHIFTS, SCHED36,
            "@Before enable check of worked shifts against plan",
            "@Before disable check of worked roster before adding shift",
            "@Before disable cutting of worked shifts to fit the plan",
            "@Before disable pre-publication checks",
            "@Before publish with lacking norms"},
            dataProvider = "true/false", dataProviderClass = DataProviders.class,
            description = "Подсвечивание табельной смены, которая не совпадает по времени с плановой")
    @Link(name = "Статья: \"5045_Подсвечивать смены в табеле, которые введены без плана или не соответствуют плану\"", url = "https://wiki.goodt.me/x/RYCyDQ")
    @TmsLink("60230")
    @Tag("ABCHR5045-2")
    @Tag(SCHED36)
    public void highlightShiftsInTableDifferFromPlan(boolean differFromPlan) {
        checkFirstDayOfMonth();
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitWithWorkedRosters(true);
        int omId = unit.getId();
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        LocalDate date = ShiftTimePosition.PAST.getShiftsDateInterval().getRandomDateBetween();
        PresetClass.changeOrSetMathParamValue(omId, MathParameterValues.ACCESS_TO_OVERWORK, true, true);
        PresetClass.makeWorkDays(omId);
        Shift shift = PresetClass.presetForMakeShiftDate(ep, date, false, ShiftTimePosition.FUTURE);
        PresetClass.publishGraphPreset(GraphStatus.PUBLISH, unit);
        DateTimeInterval interval;
        if (differFromPlan) {
            interval = shift.getDateTimeInterval().offsetByMinutes(30);
        } else {
            interval = shift.getDateTimeInterval();
        }
        PresetClass.presetForMakeShiftDateTime(ep, interval.getStartDateTime(), interval.getEndDateTime(), ShiftTimePosition.PAST);
        changeProperty(SystemProperties.SCHEDULE_BOARD_CHECK_WORKED_DIFF_PLAN, true);
        goToSchedule(unit);
        assertShiftCellDiffersFromPlan(ep, date, differFromPlan);
    }

    @Test(groups = {"ABCHR3529-1", G1, CONFLICTS, SCHED33},
            description = "Отображение конфликта \"Отдых после смены меньше, чем две рабочие длины самой смены\"")
    @Link(name = "Статья: \"3529_Конфликт по контролю межсменного отдыха\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=204282691")
    @TmsLink("60224")
    @Tag("ABCHR3529-1")
    @Tag(SCHED33)
    public void violationOfBetweenShiftRest() {
        checkLastDayOfMonth();
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        MathParameter mathParam = MathParameterRepository.getMathParameterWithValue(MathParameterEntities.ORGANIZATION_UNIT, "checkViolations");
        PresetClass.changeParameterFieldForOM(unit, mathParam, VALUE, true);
        PresetClass.enableConflictCalculationInSysList(ConstraintViolations.REST_AFTER_SHIFT_LESS_THEN_TWO_SHIFTS);
        PresetClass.setPriorityLevelToConstraintViolation(ConstraintViolations.REST_AFTER_SHIFT_LESS_THEN_TWO_SHIFTS, ConstraintViolationLevel.HIGH, false);

        LocalDate date = new DateInterval(LocalDate.now(), LocalDateTools.getLastDate().minusDays(1)).getRandomDateBetween();
        LocalDateTime startDateTime = LocalDateTime.of(date, LocalTime.of(14, 0));
        PresetClass.presetForMakeShiftDateTime(ep, startDateTime, startDateTime.plusHours(8), ShiftTimePosition.FUTURE);
        PresetClass.presetForMakeShiftDateTime(ep, startDateTime.plusDays(1).minusHours(1), startDateTime.plusDays(1).plusHours(7), ShiftTimePosition.FUTURE);
        PresetClass.runConstViolationsCalc(RosterRepository.getActiveRosterThisMonth(omId).getId());
        goToSchedule(unit);
        checkConstraintViolation(omId, ep, date, new ScheduleWorker(sb), String.format("Отдых после смены %s у сотрудника", date.format(API.getFormat())), true);
    }

    @Test(groups = {"ABCHR7041", G2, SHIFTS, LIST20, MAGNIT,
            "@Before don't show button to publish roster",
            "@Before disable timesheet rule start and end date required",
            "@Before disable check of worked roster before adding shift",
            "@Before disable cutting of worked shifts to fit the plan",
            "@Before disable worked shift comments",
            "@Before disable typed limits check"},
            description = "До наступления дня блокировки в табеле доступны текущий и предыдущий месяц (с логикой \"Предыдущий месяц\")",
            dataProvider = "true/false", dataProviderClass = DataProviders.class)
    @Link(name = "Статья: \"7041_Блокировка табеля предыдущего месяца\"", url = "https://wiki.goodt.me/x/gBhJDw")
    @TmsLink("60228")
    @Owner(SCHASTLIVAYA)
    @Tag(LIST20)
    public void lockWorkedRosterForPreviousMonthOnBlockDay(boolean rosterAvailable) {
        changeTestIDDependingOnParameter(rosterAvailable, "ABCHR7041-2", "ABCHR7041-1",
                                         "В день блокировки закрывается предыдущий месяц табеля (с логикой \"Предыдущий месяц\")");
        if (rosterAvailable) {
            checkLastDayOfMonth();
        }
        checkFirstDayOfMonth();
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitWithWorkedRosters(true);
        int omId = unit.getId();
        TableRuleRepository.getRuleForOrgUnit(omId).forEach(PresetClass::deleteRequest);
        DBUtils.removeRosterLocksForOrgUnit(omId);
        EntityProperty prop = EntityPropertyRepository.getAllPropertiesFromUnit(omId)
                .stream()
                .filter(e -> e.getPropKey().equals(ORG_UNIT_FORMAT))
                .findFirst()
                .orElse(null);
        if (Objects.nonNull(prop)) {
            TableRuleRepository.getAllRules()
                    .stream()
                    .filter(p -> p.getValue().contains(prop.getValue().toString()))
                    .filter(u -> u.getOrgUnitName() == null)
                    .forEach(PresetClass::deleteRequest);
        }
        PresetClass.changeOrSetMathParamValue(omId, MathParameterValues.ACCESS_TO_OVERWORK, true, true);
        Role role = getRoleWithBasicSchedulePermissions();
        LocalDate lockDate = selectRandomTableLockDate();
        DateInterval currentMonthWorkedInterval = new DateInterval(LocalDateTools.getFirstDate(), lockDate.minusDays(1));
        List<LocalDate> datesToCheck = getDatesForCheckingUi(currentMonthWorkedInterval);
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        prepareShiftsForCellAccessibilityCheck(datesToCheck, ep, lockDate);
        List<Shift> shifts = ShiftRepository.getShifts(ep, ShiftTimePosition.PREVIOUS_MONTH)
                .stream().limit(3).collect(Collectors.toList());
        PresetClass.addTableRuleToOrgUnit(omId, null, null,
                                          Collections.singletonList(rosterAvailable ? LocalDate.now().getDayOfMonth() + 1 : lockDate.getDayOfMonth()),
                                          TableRuleStrategy.PREVIOUS_MONTH, TableRuleShiftType.TIMESHEET);
        goToScheduleAsUser(role, unit);
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        if (rosterAvailable) {
            assertActiveCells(ep, currentMonthWorkedInterval, datesToCheck, scheduleWorker);
            clickBack();
            assertActiveCellsLastMonth(shifts, ep, scheduleWorker);
        } else {
            assertActiveCells(ep, currentMonthWorkedInterval, datesToCheck, scheduleWorker);
            clickBack();
            assertInactiveCellsLastMonth(shifts, ep, scheduleWorker);
        }
    }

    @Ignore(NOT_ACTUAL)
    @Test(groups = {"ABCHR7041", G2, SHIFTS},
            description = "Количество дней, доступных для редактирования, не влияет на закрытие табеля (с логикой \"Предыдущий месяц\")")
    @Link(name = "Статья: \"7041_Блокировка табеля предыдущего месяца\"", url = "https://wiki.goodt.me/x/gBhJDw")
    @TmsLink("60228")
    @Owner(SCHASTLIVAYA)
    @Tag(LIST20)
    @Tag("ABCHR7041-3")
    //todo объединить с lockWorkedRosterOnCertainDaysPerTableRuleWithNoTimeSpecified после завершения задачи
    public void workedShiftsForCurrentMonthAreEditableRegardlessOfTableRuleWithPreviousMonthStrategy() {
        checkFirstDayOfMonth();
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitWithWorkedRosters(true);
        int omId = unit.getId();
        PresetClass.changeOrSetMathParamValue(omId, MathParameterValues.ACCESS_TO_OVERWORK, true, true);
        Role role = getRoleWithBasicSchedulePermissions();
        LocalDate lockDate = selectRandomTableLockDate();
        DateInterval interval = new DateInterval(LocalDateTools.getFirstDate(), lockDate.minusDays(1));
        List<LocalDate> datesToCheck = getDatesForCheckingUi(interval);
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        prepareShiftsForCellAccessibilityCheck(datesToCheck, ep, lockDate);
        PresetClass.addTableRuleToOrgUnit(omId, 1, null,
                                          Collections.singletonList(lockDate.getDayOfMonth()),
                                          TableRuleStrategy.PREVIOUS_MONTH, TableRuleShiftType.TIMESHEET);

        goToScheduleAsUser(role, unit);
        assertActiveCells(ep, interval, datesToCheck, new ScheduleWorker(sb));
    }

    @Test(groups = {"ABCHR4164", G1, SCHED22, POCHTA,
            "@Before disable check of worked roster before adding shift"},
            dataProvider = "true/false", dataProviderClass = DataProviders.class,
            description = "Ручное подтверждение табеля начальником подразделения")
    @Link(name = "Статья: \"4164_Подтверждение табеля начальником ОПС\"", url = "https://wiki.goodt.me/x/egv6D")
    @TmsLink("60212")
    @Owner(BUTINSKAYA)
    @Tag(SCHED22)
    public void manualApproveTable(boolean hasPermission) {
        changeTestIDDependingOnParameter(hasPermission, "ABCHR4164-1", "ABCHR4164-2",
                                         "Отсутствие доступа к утверждению табеля для сотрудника, не являющегося начальником");
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitWithNotApprovedWorkedRoster();
        checkFirstDayOfMonthByOrgUnit(unit);
        List<PermissionType> permissions = getBasicSchedulePermissions();
        if (hasPermission) {
            permissions.add(PermissionType.PLAN_TABEL_SHIFT_APPROVE);
        }
        Role role = PresetClass.createCustomPermissionRole(permissions);
        int omId = unit.getId();
        Set<Integer> days = new HashSet<>(Ints.asList(new Random().ints(5, 1, LocalDate.now().lengthOfMonth() + 1).toArray()));
        String daysStr = "[" + days.stream()
                .sorted()
                .map(Object::toString)
                .collect(Collectors.joining(",")) + "]";
        changeProperty(SystemProperties.WORKED_DAYS_APPROVE, daysStr);
        goToScheduleAsUser(role, unit);
        threeDotsMenuClick();
        if (hasPermission) {
            chooseFunction(VariantsOfFunctions.APPROVE_TABLE);
            assertDatesForTableConfirm(omId);
            clickApproveButton();
            assertTableConfirmed(unit.getId(), "Фактическое посещение");
        } else {
            Assert.assertThrows(AssertionError.class, () -> chooseFunction(VariantsOfFunctions.APPROVE_TABLE));
        }
    }

    @Test(groups = {"ABCHR3639", G2, OTHER20, POCHTA,
            "@After remove calculation hint"},
            description = "Расчет графика с выводом подсказки")
    @Link(name = "Статья: \"3639_Выводить подсказку перед запуском расчета\"", url = "https://wiki.goodt.me/x/VxwtD")
    @TmsLink("60210")
    @Owner(SCHASTLIVAYA)
    @Tag(OTHER20)
    @Tag("ABCHR3639-2")
    public void displayHintBeforeShiftCalculation() {
        FileManual hint = PresetClass.getCalculationHint();
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        goToSchedule(unit);
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.CALCULATION_RECALCULATE_SHIFTS);
        clickCalculationButton();
        assertCalculationHintIsDisplayed(hint);
        confirmShiftCalculation();
        assertCalculationStartedWithoutErrorCheck(unit);
    }

    @Test(groups = {"ABCHR2800-1", G1, SCHED8,
            "@Before show overnight shift in both months",
            "@Before disable pre-publication checks",
            "@Before disable roster single edited version"},
            description = "Отображение переходящей смены между месяцами в графике обоих месяцев",
            dataProvider = "true/false", dataProviderClass = DataProviders.class)
    @Link(name = "Статья: \"2800_Отображение смены переходящей между месяцами\"", url = "https://wiki.goodt.me/x/mAYtD")
    @TmsLink("60496")
    @Owner(SCHASTLIVAYA)
    @Tag(SCHED8)
    @Tag("ABCHR2800-1")
    public void showOverNightShiftInBothMonths(boolean pastMidnight) {
        LocalDate nextMonthStart = LocalDateTools.getFirstDate().plusMonths(1);
        LocalDate lastDate = LocalDateTools.getLastDate();
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApiNotFiredOnDate(omId, true, nextMonthStart);
        LocalDateTime shiftStart;
        LocalDateTime shiftEnd;
        if (pastMidnight) {
            shiftStart = lastDate.atTime(18, 0);
            shiftEnd = nextMonthStart.atTime(6, 0);
        } else {
            shiftStart = lastDate.atTime(12, 0);
            shiftEnd = nextMonthStart.atTime(0, 0);
        }
        try {
            RosterRepository.getActiveRoster(omId, new DateInterval(nextMonthStart, nextMonthStart.with(TemporalAdjusters.lastDayOfMonth())));
        } catch (AssertionError e) {
            PresetClass.createEmptyPlannedRoster(omId, nextMonthStart);
        }
        PresetClass.makeClearDate(ep, shiftEnd.toLocalDate());
        PresetClass.presetForMakeShiftDateTime(ep, shiftStart, shiftEnd, ShiftTimePosition.FUTURE);
        PresetClass.publishGraphPreset(GraphStatus.PUBLISH, unit);
        goToSchedule(unit);
        ScheduleWorker sw = new ScheduleWorker(sb);
        assertCreateShift(ep, new DateTimeInterval(shiftStart, shiftEnd), sw, true);
        clickForward();
        assertDisplayOvernightShift(ep, sw, pastMidnight);
    }

    @Test(groups = {"ABCHR3514-1", G1, SCHED12,
            "@Before allow manager to publish rosters",
            "Before show button to publish roster"},
            description = "Публикация менеджером первой версии расписания")
    @Link(name = "Статья: \"3514_[Публикация графика] Управляющие АЗС могут публиковать любую версию графика.\"", url = "https://wiki.goodt.me/x/_R0tD")
    @Tag("ABCHR3514-1")
    @TmsLink("60206")
    @Tag(SCHED12)
    @Owner(SCHASTLIVAYA)
    public void managerCannotPublishInitialRoster() {
        OrgUnit unit = OrgUnitRepository.getRandomLowLevelOrgUnitWithUnpublishedInitialRosterAndChief();
        Position position = PositionRepository.getChief(unit.getId());
        if (position == null) {
            throw new AssertionError("В подразделении нет руководителя");
        }
        Employee employee = position.getEmployee();
        User user = employee.getUser();
        List<PermissionType> permissions = Arrays.asList(PermissionType.SCHEDULE_EDIT,
                                                         PermissionType.SCHEDULE_VIEW,
                                                         PermissionType.SCHEDULE_PUBLISH_SHIFTS);
        Role role = PresetClass.createCustomPermissionRole(permissions);
        goToScheduleAsUser(role, unit, user);
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.PUBLICATION);
        publishButtonClick();
        assertRosterNotPublished(unit, "Требуется первичная публикация расписания");
    }

    @Test(groups = {"ABCHR5784", G2, N2,
            "@Before disable pre-publication checks"},
            description = "Отображение кликабельной иконки \"конверт\" для пользователя.",
            dataProvider = "true/false", dataProviderClass = DataProviders.class)
    @Link(name = "Статья: \"5784_Необходимо убрать иконку уведомлений, если нет прав на просмотр\\получение.\"", url = "https://wiki.goodt.me/x/Hzz0DQ")
    @TmsLink("60193")
    @Tag(N2)
    @Owner(BUTINSKAYA)
    public void displayEnvelope(boolean hasPermission) {
        changeTestIDDependingOnParameter(hasPermission, "ABCHR5784-1", "ABCHR5784-2",
                                         "При отсутствии прав у пользователя иконка \"конверт\" не отображается.");
        changeProperty(SystemProperties.PLAN_EDIT_FUTURE_DAYS, -1);
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        List<PermissionType> permissions = new ArrayList<>(Arrays.asList(PermissionType.SCHEDULE_VIEW,
                                                                         PermissionType.ORG_UNIT_EMPLOYEE_PERSONALISATION));
        if (hasPermission) {
            permissions.add(PermissionType.NOTIFY_VIEW);
            permissions.add(PermissionType.NOTIFY_MANAGER);
            permissions.add(PermissionType.NOTIFY_ON_SCHEDULE_PUBLISH_SHIFTS);
        }
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(unit.getId(), null, true);
        LocalDate date = LocalDate.now();
        PresetClass.presetForMakeShiftDateTime(ep, date.atTime(14, 0, 0),
                                               date.atTime(22, 0, 0), ShiftTimePosition.FUTURE);
        PresetClass.checkAndMakePublicationRoster(unit.getId());
        Role role = PresetClass.createCustomPermissionRole(permissions);
        goToScheduleAsUser(role, unit, ep.getEmployee().getUser());
        if (hasPermission) {
            clickEnvelope();
            assertNotificationsOpened();
        } else {
            Assert.assertThrows(AssertionError.class, this::clickEnvelope);
        }
    }

    @Test(groups = {"ABCHR5784", G1, N2},
            description = "Переход в раздел \"Уведомления\" из основного меню пользователя.",
            dataProvider = "true/false", dataProviderClass = DataProviders.class)
    @Link(name = "Статья: \"5784_Необходимо убрать иконку уведомлений, если нет прав на просмотр\\получение.\"", url = "https://wiki.goodt.me/x/Hzz0DQ")
    @TmsLink("60193")
    @Tag(N2)
    @Owner(BUTINSKAYA)
    @Tag("ABCHR5784-3")
    public void goToNotificationsSection(boolean hasPermission) {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        List<PermissionType> permissions = new ArrayList<>(Arrays.asList(PermissionType.SCHEDULE_VIEW,
                                                                         PermissionType.ORG_UNIT_EMPLOYEE_PERSONALISATION));
        if (hasPermission) {
            permissions.add(PermissionType.NOTIFY_VIEW);
        }
        Role role = PresetClass.createCustomPermissionRole(permissions);
        goToScheduleAsUser(role, unit);
        clickSectionSelectionMenuOnPageHeader();
        if (hasPermission) {
            assertItemExistsInSectionsMenu(Section.MESSAGES.getName());
        } else {
            Assert.assertThrows(AssertionError.class, () -> assertItemExistsInSectionsMenu(Section.MESSAGES.getName()));
        }
    }

    @Test(groups = {"ABCHR5812", SCHED8, G2,
            "@Before disable merged view for planned and actual shifts",
            "@Before disable check of worked roster before adding shift",
            "@Before enable worked shift on roster joint",
            "@Before don't show button to publish roster"},
            description = "При переходящей со вчерашнего дня смене количество смен и часов считается правильно",
            dataProvider = "true/false", dataProviderClass = DataProviders.class)
    @Link(name = "Статья: \"5812_Неправильно считает кол-во смен и часов\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=234110420")
    @Owner(KHOROSHKOV)
    @TmsLink("60248")
    public void calcWeeklyAndMonthlyTotalHours(boolean month) {
        changeTestIDDependingOnParameter(month, "ABCHR5812-1", "ABCHR5812-2",
                                         "Корректный расчет и отображение суммы часов в недельном отображении расписания");
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = orgUnit.getId();
        PresetClass.changeOrSetMathParamValue(omId, MathParameterValues.ACCESS_TO_OVERWORK, true, true);
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        LocalDate date = LocalDate.now();
        Roster active = RosterRepository.getActiveRosterThisMonth(omId);
        PresetClass.removeShiftFromRoster(active, ep, date.minusDays(1));
        ShiftTimePosition timePosition = LocalDate.now().equals(LocalDateTools.getFirstDate())
                ? ShiftTimePosition.PREVIOUS_MONTH : ShiftTimePosition.DEFAULT;
        PresetClass.presetForMakeShiftDateTime(ep, date.minusDays(1).atTime(22, 0, 0),
                                               date.atTime(8, 0, 0), timePosition);
        goToSchedule(orgUnit);
        if (!month) {
            switchScope(ScopeType.WEEK);
            scopeChecker(ScopeType.WEEK);
        }
        hoverOnEmployeeHours(ep);
        ShiftTimePosition workedTimePosition = month ? ShiftTimePosition.PAST : ShiftTimePosition.PAST_THIS_WEEK;
        ShiftTimePosition activeTimePosition = month ? ShiftTimePosition.FUTURE : ShiftTimePosition.FUTURE_THIS_WEEK;
        Roster workedRoster = RosterRepository.getWorkedRosterThisMonth(omId);
        Roster activeRoster = RosterRepository.getActiveRosterThisMonth(omId);
        List<Shift> shifts = new ArrayList<>();
        shifts.addAll(ShiftRepository.getShiftsByEmployeePositionAndRoster(ep, workedTimePosition.getShiftsDateInterval(), workedRoster));
        shifts.addAll(ShiftRepository.getShiftsByEmployeePositionAndRoster(ep, activeTimePosition.getShiftsDateInterval(), activeRoster));
        if (LocalDate.now().equals(LocalDateTools.getFirstDate())) {
            Shift firstDayShift = ShiftRepository.getShift(ep, LocalDateTools.getFirstDate().minusDays(1), timePosition);
            LocalDateTime endDateTime = firstDayShift.getDateTimeInterval().getEndDateTime();
            firstDayShift.setDateTimeInterval(new DateTimeInterval(date.atStartOfDay(), endDateTime));
            shifts.add(firstDayShift);
        }
        assertTimeEquality(ep, shifts, month);
    }

    @Test(groups = {"ABCHR6133-1", SCHED33, G2},
            description = "Блок конфликтов отображается у пользователя с разрешением",
            dataProvider = "true/false", dataProviderClass = DataProviders.class)
    @Link(name = "Статья: \"6133_Права на блок конфликты\"", url = "https://wiki.goodt.me/x/gYs_Dg")
    @TmsLink("60201")
    @Tag("SCHED33")
    public void accessToConstraintViolationSettings(boolean hasAccess) {
        changeTestIDDependingOnParameter(hasAccess, "ABCHR6133-1", "ABCHR6133-1",
                                         "Блок конфликтов не отображается у пользователя без разрешения");
        List<PermissionType> permissions = getBasicSchedulePermissions();
        if (hasAccess) {
            permissions.add(PermissionType.CONSTRAINT_VIOLATION_SETTINGS_EDIT);
        }
        ImmutablePair<OrgUnit, EmployeeEssence> pair = OrgUnitRepository.getRandomOmWithEmployeeByOptions(OrgUnitOptions.EMPLOYEES_WITH_POSITION);
        OrgUnit unit = pair.left;
        EmployeePosition ep = pair.right.getEmployeePosition();
        Role role = PresetClass.createCustomPermissionRole(permissions);
        goToScheduleAsUser(role, unit, ep.getEmployee().getUser());
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.SUBDIVISION_PROPERTIES);
        if (hasAccess) {
            Allure.step("Проверить, что в карточке подразделения есть блок \"Установки конфликтов\"", () -> clickOnChevronButton(OmInfoName.CONFLICTS));
        } else {
            Allure.step("Проверить, что в карточке подразделения нет блока \"Установки конфликтов\"",
                        () -> Assert.assertThrows(WaitUntilException.class, () -> clickOnChevronButton(OmInfoName.CONFLICTS)));
        }
    }

    @Test(groups = {SCHED32, G1, MAGNIT}, description = "Удаление свободной смены в табеле", dataProvider = "true/false", dataProviderClass = DataProviders.class)
    @Link(name = "Статья: \"Биржа смен в расписании\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=270096571")
    @Owner(KHOROSHKOV)
    @TmsLink("113836")
    @Tag("MAGNIT-19")
    public void removeFreeShiftInTimesheet(boolean removalMethod) {
        String means = removalMethod ? "Удаление в виджете свободных смен" : "Удаление в форме редактирования свободной смены";
        Allure.addAttachment("Способ удаления", means);
        ImmutablePair<OrgUnit, EmployeePosition> unitAndEmp = OrgUnitRepository.getRandomOrgUnitWithRandomEmployeeForShiftPair(false);
        int omId = unitAndEmp.getLeft().getId();
        ShiftTimePosition timePosition = ShiftTimePosition.PAST;
        LocalDate date = timePosition.getShiftsDateInterval().getStartDate();
        Position pos = unitAndEmp.getRight().getPosition();
        PresetClass.removeFreeShifts(omId, date);
        PositionGroup posGroup = PositionGroupRepository.getPositionGroupById(pos.getPositionGroupId());
        PositionType postType = PositionTypeRepository.getPositionTypeById(pos.getPositionTypeId());
        PositionCategory posCat = PositionCategoryRepository.getPositionCategoryById(pos.getPositionCategoryId());
        Roster roster = RosterRepository.getNeededRosterId(timePosition, new DateInterval(), omId);
        PresetClass.makeFreeShift(date, omId, roster, posGroup, posCat, null, null, postType, null);
        goToSchedule(unitAndEmp.getLeft());
        clickFreeShift(date);
        clickThreeDotsForFreeShift(1);
        if (removalMethod) {
            clickEditButtonForFreeShift("Удалить");
            clickOnButtonSaved();
        } else {
            clickEditButtonForFreeShift("Редактировать");
            shiftThreeDotsClick();
            shiftThreeDotsClickDelete();
        }
        assertNotActiveFreeShifts(omId, date);
    }

    @Test(groups = {SCHED32, G1, MAGNIT}, description = "Нельзя создать свободную смену больше 24 часов")
    @Link(name = "Статья: \"Биржа смен в расписании\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=270096571")
    @Owner(KHOROSHKOV)
    @TmsLink("114000")
    @Tag("MAGNIT-20")
    public void testShiftCreationLimit() {
        changeProperty(SystemProperties.MAX_SHIFT_LENGTH, null);
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        LocalDate date = LocalDate.now();
        int omId = orgUnit.getId();
        LocalDateTime localDateTime = date.atTime(0, 0);
        PresetClass.setUnitOwnershipValue(omId);
        PresetClass.removeFreeShifts(omId, date);
        goToSchedule(orgUnit);
        clickFreeShift(date);
        enterShiftDateStartOrEnd(date, DateTypeField.START_DATE);
        enterShiftDateStartOrEnd(date.plusDays(1), DateTypeField.END_DATE);
        enterShiftTimeStartOrEnd(localDateTime.toLocalTime(), TimeTypeField.START_TIME);
        enterShiftTimeStartOrEnd(localDateTime.toLocalTime().plusMinutes(1), TimeTypeField.END_TIME);
        selectRandomJobTitle();
        selectRandomEmployeeAttractionReason();
        createShiftButtonClick();
        String expectedErrorMessage = "Должна быть не позднее, чем " +
                localDateTime.plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        assertErrorInField("Дата окончания", expectedErrorMessage);
        assertNotActiveFreeShifts(omId, date);
    }

    @Test(groups = {SCHED32, G1, MAGNIT, "@Before show shift hiring reason"},
            description = "Наличие действий со свободной сменой (редактировать, копировать, удалить) под ролью Админ ТТ")
    @Link(name = "Статья: \"Биржа смен в расписании\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=270096571")
    @Owner(KHOROSHKOV)
    @TmsLink("114001")
    @Tag("MAGNIT-21")
    public void availabilityOfActionsWithFreeShift() {
        ImmutablePair<OrgUnit, EmployeePosition> unitAndEmp = OrgUnitRepository.getRandomOrgUnitWithRandomEmployeeForShiftPair(false);
        int omId = unitAndEmp.getLeft().getId();
        Position pos = unitAndEmp.getRight().getPosition();
        LocalDate date = LocalDate.now();
        PresetClass.removeFreeShifts(omId, date);
        PositionGroup posGroup = PositionGroupRepository.getPositionGroupById(pos.getPositionGroupId());
        PositionType postType = PositionTypeRepository.getPositionTypeById(pos.getPositionTypeId());
        PositionCategory posCat = PositionCategoryRepository.getPositionCategoryById(pos.getPositionCategoryId());
        Roster roster = RosterRepository.getActiveRosterThisMonth(omId);
        PresetClass.makeFreeShift(date, omId, roster, posGroup, posCat, null, null, postType, null);
        goToScheduleAsUser(Role.ADMIN_TT, unitAndEmp.getLeft());
        clickFreeShift(date);
        clickThreeDotsForFreeShift(1);
        clickEditButtonForFreeShift("Редактировать");
        clickCloseButton();
        clickFreeShift(date);
        clickThreeDotsForFreeShift(1);
        clickEditButtonForFreeShift("Копировать");
        clickCloseButton();
        clickFreeShift(date);
        clickThreeDotsForFreeShift(1);
        clickEditButtonForFreeShift("Удалить");
        clickOnButtonSaved();
        assertNotActiveFreeShifts(omId, date);
    }

    @Test(groups = {SCHED32, G1, MAGNIT, "@Before disable shift hiring reason"}, description = "Копирование свободной смены")
    @Link(name = "Статья: \"Биржа смен в расписании\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=270096571")
    @Owner(KHOROSHKOV)
    @TmsLink("114002")
    @Tag("MAGNIT-22")
    public void copyFreeShift() {
        changeProperty(SystemProperties.SCHEDULE_BOARD_ATTACH_AGREEMENT, false);
        changeProperty(SystemProperties.SCHEDULE_REQUEST_SHOW_CREATE_TIME, false);
        ImmutablePair<OrgUnit, EmployeePosition> unitAndEmp = OrgUnitRepository.getRandomOrgUnitWithRandomEmployeeForShiftPair(false);
        OrgUnit orgUnit = unitAndEmp.getLeft();
        EmployeePosition emp = unitAndEmp.getRight();
        int omId = orgUnit.getId();
        Position pos = emp.getPosition();
        LocalDate date = LocalDate.now();
        PresetClass.removeFreeShifts(omId, date);
        PositionGroup posGroup = PositionGroupRepository.getPositionGroupById(pos.getPositionGroupId());
        PositionType postType = PositionTypeRepository.getPositionTypeById(pos.getPositionTypeId());
        PositionCategory posCat = PositionCategoryRepository.getPositionCategoryById(pos.getPositionCategoryId());
        Roster roster = RosterRepository.getActiveRosterThisMonth(omId);
        PresetClass.makeFreeShift(date, omId, roster, posGroup, posCat, null, null, postType, null);
        Shift freeShift = ShiftRepository.getFreeShifts(omId, date)
                .stream().findFirst().orElseThrow(() -> new AssertionError("Свободная смена за " + date + " не найдена"));
        goToScheduleAsUser(Role.ADMIN_TT, orgUnit);
        clickFreeShift(date);
        int countShift = sb.freeShiftList().startDates().size();
        clickThreeDotsForFreeShift(1);
        clickEditButtonForFreeShift("Копировать");
        createShiftButtonClick();
        clickFreeShift(date);
        assertAddFreeShift(orgUnit, freeShift.getDateTimeInterval(), posGroup, null);
        assertCountFreeShift(countShift);
    }

    @Test(groups = {IN_PROGRESS, SCHED26, G1, MAGNIT,
            "@Before disable typed limits check",
            "@Before disable pre-publication checks"},
            description = "Ограничение смены больше 24 часов с МП в одном ОМ")
    @Link(name = "Статья: \"6903_МАГНИТ. Ограничение смены больше 24 часов с МП\"",
            url = "https://wiki.goodt.me/pages/viewpage.action?pageId=256454394")
    @Owner(KHOROSHKOV)
    @TmsLink("117359")
    @Tag("ABCHR-6903-1")
    public void checkTimeLimitForSignIn() {
        changeProperty(SystemProperties.MIN_SHIFT_LENGTH, 0);
        changeProperty(SystemProperties.POSITION_EMPLOYEE_POSITIONS_ALLOW_INTERSECTIONS, true);
        ImmutablePair<OrgUnit, EmployeePosition> unitAndEmp = OrgUnitRepository.getRandomOrgUnitWithRandomEmployeeForShiftPair(true);
        OrgUnit orgUnit = unitAndEmp.getLeft();
        EmployeePosition emp = unitAndEmp.getRight();
        Employee employee = emp.getEmployee();
        int omId = orgUnit.getId();
        PresetClass.disableRequiredAddWorks();
        ShiftTimePosition timePosition = ShiftTimePosition.FUTURE;
        LocalDate date = timePosition.getShiftsDateInterval().getRandomDateBetween();
        LocalDateTime shiftStart = date.atTime(1, 0);
        LocalDateTime shiftEnd = date.atTime(23, 0);
        DateTimeInterval dateTimeInterval = new DateTimeInterval(shiftStart, shiftEnd);
        PositionGroup posGroup = PositionGroupRepository.getPositionGroupById(emp.getPosition().getPositionGroupId());
        PositionCategory posCat = PositionCategoryRepository.getPositionCategoryById(emp.getPosition().getPositionCategoryId());
        PresetClass.makeClearDate(emp, date);
        PresetClass.createScheduleRequestForDate(ScheduleRequestStatus.APPROVED, date, emp, ScheduleRequestType.OFF_TIMES);
        PositionType posType = PositionTypeRepository.getPositionTypeByName(emp.getPosition().getName());
        ShiftHiringReason hiringReason = ShiftHiringReasonRepository.getRandomShiftHiringReason();
        Roster roster = RosterRepository.getActiveRosterThisMonth(omId);
        Shift freeShift = PresetClass.createFreeShift(date, omId, roster, posGroup, posCat, hiringReason, dateTimeInterval, posType);
        JobTitle jobTitle = JobTitleRepository.getJob(freeShift.getJobTitle());
        PresetClass.createRule(false, hiringReason, jobTitle, jobTitle, omId);
        PresetClass.publishGraphPreset(GraphStatus.PUBLISH, orgUnit);
        PresetClass.assignFreeShiftToEmployee(employee, freeShift);
        List<Shift> shiftsBefore = ShiftRepository.getShifts(emp, timePosition);
        goToSchedule(orgUnit);
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.SCHEDULE_WIZARD);
        enterStartOrEndCycleDate(date, DateTypeField.START_CYCLE);
        enterStartOrEndCycleDate(date.plusDays(1), DateTypeField.END_CYCLE);
        LocalTime startShift = LocalTime.of(23, 15);
        LocalTime endShift = LocalTime.of(3, 0);
        enterShiftTime("Время", startShift, endShift);
        clickEmployeeCheckbox(employee);
        pressFormButton(false);
        assertForHourLimitExceeded(date, employee.getFullName());
        List<Shift> shiftsAfter = ShiftRepository.getShifts(emp, timePosition);
        assertNotChanged(shiftsBefore, shiftsAfter);
    }

    @Test(groups = {IN_PROGRESS, SCHED26, G1, MAGNIT,
            "@After remove an employee from the current schedule",
            "@Before disable typed limits check",
            "@Before disable pre-publication checks"},
            description = "Ограничение смены больше 24 часов с МП в двух разных ОМ")
    @Link(name = "Статья: \"6903_МАГНИТ. Ограничение смены больше 24 часов с МП\"",
            url = "https://wiki.goodt.me/pages/viewpage.action?pageId=256454394")
    @Owner(KHOROSHKOV)
    @TmsLink("117359")
    @Tag("ABCHR-6903-2")
    public void checkTimeLimitForSignInAcrossLocations() {
        changeProperty(SystemProperties.MIN_SHIFT_LENGTH, 0);
        changeProperty(SystemProperties.POSITION_EMPLOYEE_POSITIONS_ALLOW_INTERSECTIONS, true);
        Map<OrgUnit, EmployeePosition> orgUnitEmployeePairMap = OrgUnitRepository.getOrgUnitEmployeePairFromEachUnitPair(true);
        OrgUnit targetUnit = orgUnitEmployeePairMap.keySet().stream().reduce((first, second) -> second).orElse(null);
        EmployeePosition sourceEmp = orgUnitEmployeePairMap.values().stream().reduce((first, second) -> first).orElse(null);

        PresetClass.disableRequiredAddWorks();
        int randomDate = new Random().nextInt(11) + 1;
        LocalDate date = LocalDate.now();
        LocalDate startDateOfYear = date.withDayOfYear(randomDate);
        LocalDate endDateOfYear = date.withDayOfYear(date.lengthOfYear());
        Employee employee = sourceEmp.getEmployee();
        ShiftTimePosition timePosition = ShiftTimePosition.FUTURE;
        LocalDate dateShift = timePosition.getShiftsDateInterval().getRandomDateBetween();
        LocalDateTime shiftStart = dateShift.atTime(0, 0);
        LocalDateTime shiftEnd = dateShift.atTime(5, 0);
        PresetClass.makeClearDate(sourceEmp, dateShift);
        PresetClass.presetForMakeShiftDateTime(sourceEmp, shiftStart, shiftEnd, timePosition);
        EmployeePosition createdEmp = PresetClass
                .appointAnEmployeeToPosition(sourceEmp.getPosition(), employee, targetUnit, startDateOfYear, endDateOfYear);
        Reporter.getCurrentTestResult()
                .getTestContext().setAttribute("employeePosition", createdEmp);
        List<Shift> shiftsBefore = ShiftRepository.getShifts(sourceEmp, timePosition);
        goToSchedule(targetUnit);
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.SCHEDULE_WIZARD);
        enterStartOrEndCycleDate(dateShift, DateTypeField.START_CYCLE);
        enterStartOrEndCycleDate(dateShift.plusDays(1), DateTypeField.END_CYCLE);
        LocalTime startShift = LocalTime.of(6, 0);
        LocalTime endShift = LocalTime.of(6, 0);
        enterShiftTime("Время", startShift, endShift);
        clickEmployeeCheckbox(employee);
        pressFormButton(false);
        assertForHourLimitExceeded(dateShift, employee.getFullName());
        List<Shift> shiftsAfter = ShiftRepository.getShifts(sourceEmp, timePosition);
        assertNotChanged(shiftsBefore, shiftsAfter);
    }

    @Test(groups = {IN_PROGRESS, SCHED26, G1, MAGNIT,
            "@After remove an employee from the current schedule",
            "@Before disable typed limits check",
            "@Before disable pre-publication checks"},
            description = "Ограничение смены больше 24 часов с МП в двух разных ОМ (смена с биржи)")
    @Link(name = "Статья: \"6903_МАГНИТ. Ограничение смены больше 24 часов с МП\"",
            url = "https://wiki.goodt.me/pages/viewpage.action?pageId=256454394")
    @Owner(KHOROSHKOV)
    @TmsLink("117359")
    @Tag("ABCHR-6903-3")
    public void checkTimeLimitForSignInAcrossLocationsAndExchange() {
        changeProperty(SystemProperties.MIN_SHIFT_LENGTH, 0);
        changeProperty(SystemProperties.POSITION_EMPLOYEE_POSITIONS_ALLOW_INTERSECTIONS, true);
        Map<OrgUnit, EmployeePosition> orgUnitEmployeePairMap = OrgUnitRepository.getOrgUnitEmployeePairFromEachUnitPair(true);
        OrgUnit targetUnit = orgUnitEmployeePairMap.keySet().stream().reduce((first, second) -> second).orElse(null);
        OrgUnit sourceUnit = orgUnitEmployeePairMap.keySet().stream().reduce((first, second) -> first).orElse(null);
        EmployeePosition sourceEmp = orgUnitEmployeePairMap.values().stream().reduce((first, second) -> first).orElse(null);

        PresetClass.disableRequiredAddWorks();
        Employee employee = sourceEmp.getEmployee();
        int randomDate = new Random().nextInt(11) + 1;
        LocalDate dateNow = LocalDate.now();
        LocalDate startDateOfYear = dateNow.withDayOfYear(randomDate);
        LocalDate endDateOfYear = dateNow.withDayOfYear(dateNow.lengthOfYear());
        EmployeePosition createdEmp = PresetClass
                .appointAnEmployeeToPosition(sourceEmp.getPosition(), employee, targetUnit, startDateOfYear, endDateOfYear);
        Reporter.getCurrentTestResult()
                .getTestContext().setAttribute("employeePosition", createdEmp);
        int sourceOmId = sourceUnit.getId();
        ShiftTimePosition timePosition = ShiftTimePosition.FUTURE;
        LocalDate date = timePosition.getShiftsDateInterval().getRandomDateBetween();
        LocalDateTime shiftStart = date.atTime(1, 0);
        LocalDateTime shiftEnd = date.atTime(20, 0);
        DateTimeInterval dateTimeInterval = new DateTimeInterval(shiftStart, shiftEnd);
        PositionGroup posGroup = PositionGroupRepository.getPositionGroupById(sourceEmp.getPosition().getPositionGroupId());
        PositionCategory posCat = PositionCategoryRepository.getPositionCategoryById(sourceEmp.getPosition().getPositionCategoryId());
        PresetClass.makeClearDate(sourceEmp, date);
        PresetClass.createScheduleRequestForDate(ScheduleRequestStatus.APPROVED, date, sourceEmp, ScheduleRequestType.OFF_TIMES);
        PositionType posType = PositionTypeRepository.getPositionTypeByName(sourceEmp.getPosition().getName());
        ShiftHiringReason hiringReason = ShiftHiringReasonRepository.getRandomShiftHiringReason();
        Roster roster = RosterRepository.getActiveRosterThisMonth(sourceOmId);
        Shift freeShift = PresetClass.createFreeShift(date, sourceOmId, roster, posGroup, posCat, hiringReason, dateTimeInterval, posType);
        JobTitle jobTitle = JobTitleRepository.getJob(freeShift.getJobTitle());
        PresetClass.createRule(false, hiringReason, jobTitle, jobTitle, sourceOmId);
        PresetClass.publishGraphPreset(GraphStatus.PUBLISH, sourceUnit);
        PresetClass.assignFreeShiftToEmployee(employee, freeShift);
        List<Shift> shiftsBefore = ShiftRepository.getShifts(sourceEmp, timePosition);

        goToSchedule(targetUnit);
        threeDotsMenuClick();
        chooseFunction(VariantsOfFunctions.SCHEDULE_WIZARD);
        enterStartOrEndCycleDate(date, DateTypeField.START_CYCLE);
        enterStartOrEndCycleDate(date.plusDays(1), DateTypeField.END_CYCLE);
        LocalTime startShift = LocalTime.of(21, 0);
        LocalTime endShift = LocalTime.of(9, 0);
        enterShiftTime("Время", startShift, endShift);
        clickEmployeeCheckbox(employee);
        pressFormButton(false);
        assertForHourLimitExceeded(date, employee.getFullName());
        List<Shift> shiftsAfter = ShiftRepository.getShifts(sourceEmp, timePosition);
        assertNotChanged(shiftsBefore, shiftsAfter);
    }

    @Test(groups = {"ABCHR100936-1", G2, SCHED9, IN_PROGRESS, MAGNIT},
            description = "При отмене интеграционного отсутствия в прошлом месяце, когда на подразделение действует " +
                    "только правило табеля с логикой \"Предыдущие дни\",  в БД создаётся правило исключения из правил табеля")
    @Link(name = "100936_[Магнит.Исключение из правил табеля] доп. доработки системного списка при автоматическом открытии (Предыдущий месяц)+ доп кейс",
            url = "https://wiki.goodt.me/pages/viewpage.action?pageId=291440245")
    @TmsLink("118298")
    @Tag(SCHED9)
    @Owner(KHOROSHKOV)
    public void integrationAbsenceCancellationWithPreviousDaysLogic() {
        ImmutablePair<OrgUnit, EmployeePosition> unitAndEmp = OrgUnitRepository.getRandomOrgUnitWithRandomEmployeeForShiftPair(false);
        OrgUnit orgUnit = unitAndEmp.getKey();
        int omId = orgUnit.getId();
        EmployeePosition emp = unitAndEmp.getRight();
        Employee employee = emp.getEmployee();
        if (Objects.isNull(employee.getOuterId())) {
            employee.setOuterId(PresetClass.generateRandomStringWithDash(36, 6));
            PresetClass.updateEmployee(employee);
        }
        LocalDate date = ShiftTimePosition.FUTURE.getShiftsDateInterval().getRandomDateBetween();
        LocalDate startDate = date.minusMonths(1).withDayOfMonth(1);
        LocalDate endDate = date.minusMonths(1);
        PresetClass.addTableRuleToOrgUnit(omId, date.getDayOfMonth(), null, null, TableRuleShiftType.TIMESHEET);
        ScheduleRequestAlias alias = ScheduleRequestAliasRepository.getRandomEnabledAlias();
        DateTimeInterval dateTimeInterval = new DateTimeInterval(startDate.atTime(0, 0), endDate.atTime(23, 59));
        ScheduleRequest scheduleRequest = PresetClass.createRequestAbsenceInDateInterval(emp.refreshEmployeePosition(), alias, dateTimeInterval, omId);

        goToScheduleAsUser(Role.ADMIN_TT, orgUnit);
        clickBack();
        ScheduleWorker scheduleWorker = new ScheduleWorker(sb);
        assertRequestAdding(emp, scheduleRequest.getStartDate(), scheduleRequest.getType(), scheduleWorker, orgUnit, scheduleRequest.getStatus());
        alias.setOuterId("SCHEDULE_REQUEST");
        Map<String, String> params = Pairs.newBuilder().stopOnError(false).openPrevEmployeePosition(false).buildMap();
        String path = makePath(INTEGRATION_JSON, REMOVED);
        PresetClass.deleteRequestAbsenceInDateInterval(emp, alias, dateTimeInterval, params, path);
        refreshPage();
        clickBack();
        assertRequestDeleting(scheduleRequest, scheduleWorker, orgUnit);
        String expectedResult = "I";
        String sqlSourceResult = DBUtils.getSourceInSysListTimesheetEditPermission(omId, startDate, endDate);
        Allure.addAttachment("Проверить, что в ячейке source находится результат " + expectedResult,
                             "Ожидаемый результат: " + expectedResult + "\nФактический результат: " + sqlSourceResult);
        Assert.assertEquals(sqlSourceResult, expectedResult);
    }

    @Test(groups = {"IM8445-1", G2, INTEGRATION, IN_PROGRESS, EFES, "@After remove requestType"},
            description = "Удаление отсутствий при отправке DELETE")
    @Link(name = "8445_удаление реквестов по API",
            url = "https://wiki.goodt.me/pages/viewpage.action?pageId=270076177")
    @TmsLink("118763")
    @Tag(INTEGRATION)
    @Owner(KHOROSHKOV)
    public void removingAbsencesOnSubmission() {
        ImmutablePair<OrgUnit, EmployeePosition> unitAndEmp = OrgUnitRepository.getRandomOrgUnitWithRandomEmployeeForShiftPair(false);
        OrgUnit orgUnit = unitAndEmp.getKey();
        EmployeePosition emp = unitAndEmp.getRight();
        ScheduleRequestAlias alias = ScheduleRequestAliasRepository.getAliasByName("Отпуск");
        ScheduleRequestType requestType = ScheduleRequestType.getType(alias.getType());
        ScheduleRequestAlias scheduleRequestAlias = PresetClass.addScheduleRequestType(requestType, null, false);
        Reporter.getCurrentTestResult().getTestContext().setAttribute("scheduleRequestAlias", scheduleRequestAlias);
        LocalDate date = ShiftTimePosition.FUTURE.getShiftsDateInterval().getRandomDateBetween();
        DateTimeInterval dateTimeInterval = new DateTimeInterval(date.atTime(0, 0), date.atTime(23, 59));
        ScheduleRequest scheduleRequest = PresetClass.createRequestAbsenceInDateInterval(emp.refreshEmployeePosition(), alias, dateTimeInterval, orgUnit.getId());
        Map<String, String> params = Pairs.newBuilder().stopOnError(false).deleteIntersections(true).processShifts("true").buildMap();
        String path = makePath(INTEGRATION_JSON, SCHEDULE_REQUESTS);
        emp.getPosition().setOuterId(null);
        PresetClass.deleteRequestAbsenceInDateInterval(emp, alias, dateTimeInterval, params, path);
        goToSchedule(orgUnit);
        assertRequestDeleting(scheduleRequest, new ScheduleWorker(sb), orgUnit);
    }
}
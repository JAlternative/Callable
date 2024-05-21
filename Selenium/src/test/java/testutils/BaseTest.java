package testutils;

import io.qameta.allure.Allure;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Step;
import io.qameta.allure.model.Label;
import io.qameta.allure.model.Parameter;
import io.qameta.allure.util.ResultsUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchSessionException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.ITestNGMethod;
import org.testng.Reporter;
import org.testng.annotations.*;
import utils.db.DBUtils;
import utils.tools.CustomTools;
import utils.tools.LocalDateTools;
import wfm.PresetClass;
import wfm.components.orgstructure.MathParameterEntities;
import wfm.components.schedule.AddWorkStatus;
import wfm.components.schedule.DateUnit;
import wfm.components.schedule.ScheduleRequestType;
import wfm.components.schedule.SystemProperties;
import wfm.components.utils.PermissionType;
import wfm.components.utils.Role;
import wfm.models.EntityPropertiesKey;
import wfm.models.Limits;
import wfm.models.OrgUnit;
import wfm.models.User;
import wfm.repository.EntityPropertyKeyRepository;
import wfm.repository.KpiRepository;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.qameta.allure.util.ResultsUtils.bytesToHex;
import static io.qameta.allure.util.ResultsUtils.getMd5Digest;
import static java.util.Comparator.comparing;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static ru.yandex.qatools.matchers.webdriver.TextMatcher.text;
import static utils.ErrorMessagesForReport.NO_VALID_DATE;
import static utils.Links.LOG;
import static utils.tools.CustomTools.changeProperty;
import static utils.tools.CustomTools.changeSystemListEnableValue;
import static wfm.repository.CommonRepository.URL_BASE;

public class BaseTest {

    private static final Matcher<String> concealedMatcher = matchesPattern("^[\\*\\ \\.]+$");

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before set default shift duration"},
            description = "Установить значения по умолчанию для системных настроек минимальной и максимальной длины смены")
    public void setDefaultShiftLimits() {
        List<SystemProperties> properties = new ArrayList<>();
        properties.add(SystemProperties.MAX_SHIFT_LENGTH);
        properties.add(SystemProperties.MIN_SHIFT_LENGTH);
        for (SystemProperties prop : properties) {
            changeProperty(prop, Integer.parseInt(prop.getDefaultValue()));
        }
    }

    @AfterGroups(value = "@After delete test shift comment", alwaysRun = true)
    public void clean() {
        deleteTestShiftComment();
    }

    @Step("Пресет. Удалить тестовый комментарий")
    public void deleteTestShiftComment() {
        PresetClass.deleteAllShiftEditReasonsMatchesText("тестовый");
    }

    @AfterGroups(value = "TEST-1174", alwaysRun = true,
            description = "Вернуть значения возможности выгрузки планового графика в состояние по умолчанию.")
    public void setDefaultProperties2() {
        SystemProperties jasperReportsShiftsPlanShow = SystemProperties.JASPER_REPORTS_SHIFTS_PLAN_SHOW;
        PresetClass.setSystemPropertyValue(jasperReportsShiftsPlanShow, Boolean.parseBoolean(jasperReportsShiftsPlanShow.getDefaultValue()));
    }

    @AfterGroups(value = "TEST-1162", alwaysRun = true,
            description = "Вернуть значения параметра возможности печати нормализованного графика в состояние по умолчанию.")
    public void setDefaultProperties3() {
        SystemProperties jasperReportsShiftsPlanShow = SystemProperties.SCHEDULE_BOARD_NORMALIZED_PRINT;
        PresetClass.setSystemPropertyValue(jasperReportsShiftsPlanShow, Boolean.parseBoolean(jasperReportsShiftsPlanShow.getDefaultValue()));
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before enable overtime access"},
            description = "Включить системную настройку \"Доступ к сверхурочной работе\"")
    public void enableOvertimeAccess() {
        changeProperty(SystemProperties.ALLOW_SHIFT_OVERTIME, true);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before change setting with name hint"},
            description = "Включить системную настройку \"Подсказка с полным ФИО сотрудника\"")
    public void changeSettingWithNameHint() {
        changeProperty(SystemProperties.SCHEDULE_BOARD_FULL_NAME_HINT, true);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before comments on shifts"},
            description = "Включить комментарии к сменам")
    public void commentsOnShifts() {
        changeProperty(SystemProperties.FACT_SHIFT_COMMENTS_REQUIRED, true);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before comments on plan shifts"},
            description = "Включить комментарии к сменам в плановом графике")
    public void commentsOnPlanShifts() {
        changeProperty(SystemProperties.PLAN_SHIFT_COMMENTS_DELETE_REQUIRED_FUTURE, true);
        changeProperty(SystemProperties.PLAN_SHIFT_COMMENTS_DELETE_REQUIRED_PAST, true);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before deletion not request"},
            description = "Отключить системную настройку \"Заменять смены на запросы расписания вместо их удаления\"")
    public void deletionNotRequest() {
        changeProperty(SystemProperties.SHOW_SCHEDULE_REQUEST_CHANGE_LIST, false);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before comments on deleting shifts"},
            description = "Включить комментарии к удалению смен")
    public void commentsOnDeletingShifts() {
        changeProperty(SystemProperties.FACT_SHIFT_COMMENTS_DELETE_REQUIRED, true);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before enable additional info indicator"},
            description = "Включить индикатор дополнительной информации")
    public void setExtraInfoIndicator() {
        changeProperty(SystemProperties.SCHEDULE_BOARD_HELP_INDICATOR, true);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before allow roster edits in past"},
            description = "Разрешить пользователям редактировать ростер в прошлом")
    public void allowRosterEditInPast() {
        changeProperty(SystemProperties.PLAN_EDIT_PAST_DAYS, 31);
        if (URL_BASE.contains("magnit")) {
            changeProperty(SystemProperties.WORKED_SHIFT_CAN_EDIT_DAYS, "31");
        } else {
            changeProperty(SystemProperties.WORKED_SHIFT_CAN_EDIT_DAYS, 31);
        }
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before allow worked shift editing"},
            description = "Разрешить пользователям редактировать табель")
    public void allowWorkedShiftEditing() {
        changeProperty(SystemProperties.WORKED_SHIFT_CAN_EDIT_DAYS, "31");
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before allow plan shift editing"},
            description = "Разрешить пользователям редактировать плановый график")
    public void allowPlanShiftEditing() {
        changeProperty(SystemProperties.PLAN_EDIT_PAST_DAYS, 31);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before forbid roster edits in past"},
            description = "Запретить пользователям редактировать ростер в прошлом")
    public void forbidRosterEditInPast() {
        if (URL_BASE.contains("magnit")) {
            changeProperty(SystemProperties.WORKED_SHIFT_CAN_EDIT_DAYS, "0");
        } else {
            changeProperty(SystemProperties.WORKED_SHIFT_CAN_EDIT_DAYS, 0);
        }
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before disable check of worked roster before adding shift"},
            description = "Отключить системную настройку \"Проверка существования смены в опубликованном ростере перед добавлением в табеле\"")
    public void disableCheckAgainstWorkedShifts() {
        changeProperty(SystemProperties.SHIFT_CHECK_IN_WORKED_ROSTER_BEFORE_CREATING, false);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before enable check of worked roster before adding shift"},
            description = "Включить системную настройку \"Проверка существования смены в опубликованном ростере перед добавлением в табеле\"")
    public void enableCheckAgainstWorkedShifts() {
        changeProperty(SystemProperties.SHIFT_CHECK_IN_WORKED_ROSTER_BEFORE_CREATING, true);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before disable cutting of worked shifts to fit the plan"},
            description = "Отключить системную настройку \"План всегда равняется факту\"")
    public void disableCuttingOfWorkedShifts() {
        changeProperty(SystemProperties.WORKED_SHIFT_CUT_TO_PLAN, false);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before enable indication of exchange shifts"},
            description = "Отключить системную настройку \"Индикация назначенных смен с биржи\"")
    public void enableIndicatorsOfExchangeShifts() {
        changeProperty(SystemProperties.SCHEDULE_BOARD_SHIFTS_FROM_EXCHANGE_INDICATE, true);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before disable typed limits check"},
            description = "Отключить системную настройку \"Проверка типизированных лимитов\"")
    public void disableTypedLimitsCheck() {
        changeProperty(SystemProperties.ENABLE_TYPED_LIMITS_CHECK, false);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before disable pre-publication checks"},
            description = "Отключить проверки перед публикацией графика")
    public void disableHourNorm() {
        List<SystemProperties> properties = new ArrayList<>();
        properties.add(SystemProperties.ROSTER_PUBLISH_WITHOUT_EXCEEDING_NORMS);
        properties.add(SystemProperties.ROSTER_PUBLISH_WITHOUT_CONFLICTS);
        properties.add(SystemProperties.ROSTER_PUBLISH_CALC_TOTAL_LIMIT_EMPLOYEE);
        properties.add(SystemProperties.ROSTER_PUBLISH_WITHOUT_NORMS_LACK);
        properties.add(SystemProperties.ROSTER_PUBLISH_CHECK_INTERSECTIONS);
        properties.add(SystemProperties.ROSTER_PUBLISH_CHECK_INHERITANCE);
        for (SystemProperties prop : properties) {
            changeProperty(prop, false);
        }
        changeProperty(SystemProperties.ROSTER_PUBLISH_WITHOUT_YEAR_OVERTIME_LIMIT_VIOLATION, 0);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before enable outstaff filtering settings"},
            description = "Включить системные настройки \"Строки OutStaff в расписании\" и \"Фильтр по типу персонала в расписании\"")
    public void outStaffSettings() {
        List<SystemProperties> properties = new ArrayList<>();
        properties.add(SystemProperties.SCHEDULE_BOARD_OUT_STAFF_ROWS);
        properties.add(SystemProperties.SCHEDULE_BOARD_PERSONAL_TYPE_FILTER);
        for (SystemProperties prop : properties) {
            changeProperty(prop, true);
        }
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before disable all shift comments"})
    public void disableAllShiftComments() {
        changeProperty(SystemProperties.FACT_SHIFT_COMMENTS_REQUIRED, false);
        changeProperty(SystemProperties.FACT_SHIFT_COMMENTS_DELETE_REQUIRED, false);
        changeProperty(SystemProperties.PLAN_SHIFT_COMMENTS_DELETE_REQUIRED_FUTURE, false);
        changeProperty(SystemProperties.PLAN_SHIFT_COMMENTS_DELETE_REQUIRED_PAST, false);
        changeProperty(SystemProperties.PLAN_SHIFT_COMMENTS_REQUIRED_FUTURE, false);
        changeProperty(SystemProperties.PLAN_SHIFT_COMMENTS_REQUIRED_PAST, false);
        changeProperty(SystemProperties.PLAN_EDIT_FUTURE_DAYS, -1);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before disable worked shift comments"})
    public void disableWorkedShiftComments() {
        changeProperty(SystemProperties.FACT_SHIFT_COMMENTS_REQUIRED, false);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before disallow timesheet editing for past months"})
    public void disallowTimesheetEditing() {
        changeProperty(SystemProperties.SCHEDULE_BOARD_TIMESHEET_PAST_MONTHS, "-1");
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before disable multiple work breaks"})
    public void disableMultipleWorkBreaks() {
        changeProperty(SystemProperties.ENABLE_MULTIPLE_WORK_BREAKS, false);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before enable multiple work breaks"})
    public void enableMultipleWorkBreaks() {
        changeProperty(SystemProperties.ENABLE_MULTIPLE_WORK_BREAKS, true);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before enable checking for roster locks"})
    public void enableRosterLockCheck() {
        changeProperty(SystemProperties.CHECK_SHIFTS_ON_LOCK, true);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before disable timesheet rule start and end date required"})
    public void disableTimesheetRuleStartEndDateRequired() {
        changeProperty(SystemProperties.TIMESHEET_RULE_START_END_DATE_REQUIRED, false);
    }

    @AfterGroups(alwaysRun = true, value = "@After remove added roster locks",
            description = "Удалить блоки ростеров, добавленные во время теста")
    public void removeAddedRosterLocks(ITestContext c) {
        List<String> attributes = c.getAttributeNames().stream()
                .filter(a -> a.startsWith(DBUtils.getPrefix()))
                .collect(Collectors.toList());
        if (!attributes.isEmpty()) {
            for (String attribute : attributes) {
                long id = (long) c.getAttribute(attribute);
                DBUtils.removeRosterLockForOrgUnitById(id);
            }
        }
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before enable schedule request: day off"},
            description = "Включить тип запроса \"Выходной\"")
    public void enableDaysOffIfTurnedOff() {
        changeSystemListEnableValue(ScheduleRequestType.OFF_TIME, true);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before disable schedule request: overtime"},
            description = "Выключить тип запроса расписания \"Сверхурочная работа\"")
    public void disableOvertimeShiftRequest() {
        changeSystemListEnableValue(ScheduleRequestType.OVERTIME, false);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before enable merged view for planned and actual shifts"},
            description = "Включить объединенное отображение планового и фактического графиков в разделе расписания")
    public void enableScheduleBoardMergeView() {
        changeProperty(SystemProperties.SCHEDULE_BOARD_PLAN_FACT_MERGE, true);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before activate the conflict indicator"},
            description = "Активировать индикатор конфликтов")
    public void activateConflictIndicator() {
        changeProperty(SystemProperties.ON_CONFLICTS, true);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before disable the conflict indicator"},
            description = "Деактивировать индикатор конфликтов")
    public void deactivateConflictIndicator() {
        changeProperty(SystemProperties.ON_CONFLICTS, false);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before no disable calculate conflicts"},
            description = "Не отключать расчёт конфликтов")
    public void noDisableCalculateConflicts() {
        changeProperty(SystemProperties.DISABLE_CALCULATE_CONFLICTS, false);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before activate allow shift overtime"},
            description = "Активировать доступ к сверхурочной работе")
    public void activateAllowShiftOvertime() {
        changeProperty(SystemProperties.ALLOW_SHIFT_OVERTIME, true);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before show the rate in the schedule"},
            description = "Показывать ставку в расписании")
    public void showRateInSchedule() {
        changeProperty(SystemProperties.SCHEDULE_BOARD_SHOW_RATE, true);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before enable check of worked shifts against plan"},
            description = "Включить настройку \"Индикация смен в табеле при расхождении с планом\"")
    public void enableCheckOfWorkedShiftsAgainstPlan() {
        changeProperty(SystemProperties.SCHEDULE_BOARD_CHECK_WORKED_DIFF_PLAN, true);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before disable check of worked shifts against plan"},
            description = "Выключить настройку \"Индикация смен в табеле при расхождении с планом\"")
    public void disableCheckOfWorkedShiftsAgainstPlan() {
        changeProperty(SystemProperties.SCHEDULE_BOARD_CHECK_WORKED_DIFF_PLAN, false);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before disable equality check between plan and fact"},
            description = "Выключить объединенное отображение планового и фактического графиков в разделе расписания")
    public void disableCheckForEqualityBetweenPlanAndFact() {
        changeProperty(SystemProperties.WORKED_SHIFTS_EQUAL_PLAN, false);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before enable schedule request: on duty"},
            description = "Включить тип запроса расписания \"Дежурство\"")
    public void enableOvertimeShiftRequest() {
        changeProperty(SystemProperties.ALLOW_OVERTIME_DUTY, true);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before disable mandatory comment when editing or deleting shift"},
            description = "Отключить требование комментария при редактировании или удалении смены")
    public void disableCommentRequirementOnShiftEdit() {
        changeProperty(SystemProperties.PLAN_SHIFT_COMMENTS_REQUIRED_FUTURE, false);
        changeProperty(SystemProperties.FACT_SHIFT_COMMENTS_REQUIRED, false);
        changeProperty(SystemProperties.FACT_SHIFT_COMMENTS_DELETE_REQUIRED, false);
        changeProperty(SystemProperties.PLAN_SHIFT_COMMENTS_DELETE_REQUIRED_FUTURE, false);
        changeProperty(SystemProperties.PLAN_EDIT_FUTURE_DAYS, -1);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before disable strong lock plan"},
            description = "Отключить строгую блокировку редактирования плана")
    public void disableStrongLockPlan() {
        changeProperty(SystemProperties.PLAN_EDIT_FUTURE_STRONG, false);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before allow editing plan shifts in future"})
    public void allowEditingPlanShiftsInFuture() {
        changeProperty(SystemProperties.PLAN_EDIT_FUTURE_DAYS, -1);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before publication without norms lack"},
            description = "Включить публикацию без нехватки норм")
    @Step("Включить системную настройку \"Публикация без нехватки норм\"")
    public void enablePublicationWithoutNormsLack() {
        changeProperty(SystemProperties.ROSTER_PUBLISH_WITHOUT_NORMS_LACK, true);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before publish with lacking norms"},
            description = "Отлючить публикацию без нехватки норм")
    @Step("Выключить системную настройку \"Публикация без нехватки норм\"")
    public void disablePublicationWithoutNormsLack() {
        changeProperty(SystemProperties.ROSTER_PUBLISH_WITHOUT_NORMS_LACK, false);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before schedule board deviation from standard plan"},
            description = "Отключить контроль планового времени в текущем месяце на расписании")
    @Step("Отключить системную настройку \"Контроль планового времени в текущем месяце на расписании\"")
    public void disableDeviationFromStandardPlan() {
        changeProperty(SystemProperties.SCHEDULE_BOARD_DEVIATION_FROM_STANDARD_PLAN, false);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before functional roles in badges"},
            description = "Функциональные роли, отображаемые в бейджах")
    @Step("Установить системной настройке scheduleBoardBadgePositionGroups значение [4] - функциональная роль \"back-оператор\"")
    public void setFuncRolesInBadges() {
        changeProperty(SystemProperties.SCHEDULE_BOARD_BADGE_POSITION_GROUPS, "[4]");
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before download shifts plan"},
            description = "Функциональные роли, отображаемые в бейджах")
    @Step("Установить системной настройке scheduleBoardBadgePositionGroups значение [4] - функциональная роль \"back-оператор\"")
    public void setJasperProperties() {
        changeProperty(SystemProperties.JASPER_REPORTS_ROSTER_MAIN_SHOW, true);
        changeProperty(SystemProperties.JASPER_REPORTS_ROSTER_MAIN_CAPTION, "Плановый график");
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before enable schedule request: non-appearance"})
    public void enableNonAppearance() {
        changeSystemListEnableValue(ScheduleRequestType.NON_APPEARANCE, true);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before move to exchange not only shifts from exchange"})
    public void moveToExchangeNotOnlyShiftsFromExchange() {
        changeProperty(SystemProperties.SHIFT_EXCHANGE_ONLY_EXCHANGE, false);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before move to exchange only shifts from exchange"})
    public void moveToExchangeOnlyShiftsFromExchange() {
        changeProperty(SystemProperties.SHIFT_EXCHANGE_ONLY_EXCHANGE, true);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before allow free shifts for own employees"})
    public void allowFreeShiftsForOwnEmployees() {
        changeProperty(SystemProperties.SHIFT_EXCHANGE_ONLY_EXTERNAL, false);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before allow shift exchange outstaff with accept"})
    public void allowShiftExchangeOutstaffWithAccept() {
        changeProperty(SystemProperties.SHIFT_EXCHANGE_OUTSTAFF_WITH_ACCEPT, true);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before forbid shift exchange use job title"})
    public void forbidShiftExchangeUseJobTitle() {
        changeProperty(SystemProperties.SHIFT_EXCHANGE_USE_JOB_TITLE, false);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before allow shift exchange use job title"})
    public void allowShiftExchangeUseJobTitle() {
        changeProperty(SystemProperties.SHIFT_EXCHANGE_USE_JOB_TITLE, true);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before allow free shifts for external employees"})
    public void allowFreeShiftsForExternalEmployees() {
        changeProperty(SystemProperties.SHIFT_EXCHANGE_ONLY_EXTERNAL, true);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before disable additional work"})
    public void disableAdditionalWork() {
        changeProperty(SystemProperties.SHIFT_ADDITIONAL_WORK_ENABLED, false);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before enable additional work"})
    public void enableAdditionalWork() {
        changeProperty(SystemProperties.SHIFT_ADDITIONAL_WORK_ENABLED, true);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before disable day off in schedule wizard"})
    public void disableDayOff() {
        changeProperty(SystemProperties.SHIFT_WIZARD_DISABLE_DAY_OFF, false);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before enable front/back indicators"})
    public void enableFrontBackIndicators() {
        changeProperty(SystemProperties.SCHEDULE_BOARD_FRONT_BACK_INDICATOR, true);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before disable payout days"})
    public void disablePayoutDays() {
        changeProperty(SystemProperties.PAYOUTS_DAYS, "");
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before display all additional work"})
    public void displayAllAdditionalWork() {
        changeProperty(SystemProperties.SHIFT_ADDITIONAL_WORK_ACTIVE_STATUSES, "");
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before display additional work only with chosen statuses"},
            description = "Отображать доп. работы только с указанными статусами")
    public void displayAdditionalWorkWithStatuses() {
        changeProperty(SystemProperties.SHIFT_ADDITIONAL_WORK_ACTIVE_STATUSES,
                       String.format("[\"%s\",\"%s\"]", AddWorkStatus.PLANNED.getStatusName(), AddWorkStatus.DONE.getStatusName()));
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before show shift hiring reason"})
    public void showShiftHiringReason() {
        changeProperty(SystemProperties.SHIFT_EXCHANGE_SHOW_HIRING_REASON, true);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before disable shift hiring reason"})
    public void disableShiftHiringReason() {
        changeProperty(SystemProperties.SHIFT_EXCHANGE_SHOW_HIRING_REASON, false);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before show shift exchange"})
    public void showShiftExchange() {
        changeProperty(SystemProperties.ON_FREE_SHIFTS, true);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before conflicts and exceeding norms"})
    public void conflictsAndExceedingNorms() {
        changeProperty(SystemProperties.ROSTER_PUBLISH_WITHOUT_CONFLICTS, true);
        changeProperty(SystemProperties.ROSTER_PUBLISH_WITHOUT_EXCEEDING_NORMS, true);
        changeProperty(SystemProperties.ROSTER_PUBLISH_WITHOUT_NORMS_LACK, false);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = "@Before show all employee groups")
    public void showAllEmployeeGroups() {
        changeProperty(SystemProperties.EMPLOYEE_EXCLUDE_GROUPS, "");
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before conflicts and norms lack"})
    public void conflictsAndNormsLack() {
        changeProperty(SystemProperties.ROSTER_PUBLISH_WITHOUT_CONFLICTS, false);
        changeProperty(SystemProperties.ROSTER_PUBLISH_WITHOUT_EXCEEDING_NORMS, false);
        changeProperty(SystemProperties.ROSTER_PUBLISH_WITHOUT_NORMS_LACK, true);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before publish with exceeding norms"})
    public void publishWithExceedingNorms() {
        changeProperty(SystemProperties.ROSTER_PUBLISH_WITHOUT_EXCEEDING_NORMS, false);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before publish without checking for yearly overtime limit violation"})
    public void publishWithoutCheckingForOfYearlyOvertimeLimitViolation() {
        changeProperty(SystemProperties.ROSTER_PUBLISH_WITHOUT_YEAR_OVERTIME_LIMIT_VIOLATION, 0);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before publish with strict block of yearly overtime limit violation"})
    public void publishWithStrictBlockOfYearlyOvertimeLimitViolation() {
        changeProperty(SystemProperties.ROSTER_PUBLISH_WITHOUT_YEAR_OVERTIME_LIMIT_VIOLATION, 1);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before publish with lenient block of yearly overtime limit violation"})
    public void publishWithLenientCheckOfYearlyOvertimeLimitViolation() {
        changeProperty(SystemProperties.ROSTER_PUBLISH_WITHOUT_YEAR_OVERTIME_LIMIT_VIOLATION, 2);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before disable merged view for planned and actual shifts"},
            description = "Выключить объединенное отображение планового и фактического графиков в разделе расписания")
    public void disableScheduleBoardMergeView() {
        changeProperty(SystemProperties.SCHEDULE_BOARD_PLAN_FACT_MERGE, false);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before allow request comments"})
    public void allowRequestComments() {
        changeProperty(SystemProperties.REQUEST_COMMENTS_ALLOW, true);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before disable mandatory comments when deleting worked shift"})
    public void disableMandatoryCommentsWhenDeletingWorkedShift() {
        changeProperty(SystemProperties.FACT_SHIFT_COMMENTS_DELETE_REQUIRED, false);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = "@Before opening and closing shifts")
    public void openingAndClosingShifts() {
        changeProperty(SystemProperties.SCHEDULE_BOARD_SHIFTS_INDICATOR, true);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = "@Before show additional information on shift exchange")
    public void enableAdditionalInformation() {
        changeProperty(SystemProperties.ADDITIONAL_INFORMATION_ON_SHIFT_EXCHANGE, true);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = "@Before enable shift exchange create version on assign")
    public void enableShiftExchangeCreateVersionOnAssign() {
        changeProperty(SystemProperties.SHIFT_EXCHANGE_CREATE_VERSION_ON_ASSIGN, true);
    }

    @AfterClass(alwaysRun = true,
            description = "Восстановить все настройки типов запросов расписания, измененные в ходе теста")
    public void revertSystemListScheduleRequestValues() {
        PresetClass.revertRequest();
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before disable start time check for worked shifts"},
            description = "Выключить объединенное отображение планового и фактического графиков в разделе расписания")
    public void disableStartTimeCheckForWorkedShifts() {
        changeProperty(SystemProperties.ENABLE_START_TIME_CHECK_FOR_WORKED_SHIFTS, false);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before enable start time check for worked shifts"},
            description = "Включить проверку фактического начала смены на условие не ранее, чем начало смены по плану при ручном создании/редактировании фактической смены.")
    public void enableStartTimeCheckForWorkedShifts() {
        changeProperty(SystemProperties.ENABLE_START_TIME_CHECK_FOR_WORKED_SHIFTS, true);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = "@Before allow change OuterId")
    public void allowChangeOuterId() {
        changeProperty(SystemProperties.ALLOW_CHANGE_OUTER_ID, true);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = "@Before schedule board show limit change notification")
    public void allowScheduleBoardShowLimitChangeNotification() {
        changeProperty(SystemProperties.SCHEDULE_BOARD_SHOW_LIMIT_CHANGE_NOTIFICATION, true);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = "@Before disable limit check outstaff")
    public void disableLimitCheckOutstaff() {
        changeProperty(SystemProperties.LIMIT_CHECK_OUTSTAFF_ENABLED, false);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = "@Before allow manager to publish rosters")
    public void allowManagersToPublishRosters() {
        changeProperty(SystemProperties.MANAGER_SECONDARY_PUBLICATION_ALLOW, true);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = "@Before keep shifts under requests")
    public void keepShiftsUnderRequests() {
        changeProperty(SystemProperties.KEEP_SHIFTS_UNDER_REQUESTS, false);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = "@Before enable schedule wizard breaks")
    public void enableScheduleWizardBreaks() {
        changeProperty(SystemProperties.SCHEDULE_WIZARD_ENABLE_BREAKS, true);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = "@Before enable publication notifications for managers")
    public void enablePublicationNotificationsForManagers() {
        changeProperty(SystemProperties.MANAGER_PUBLICATION_NOTIFICATION_ALLOW, true);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = "@Before disable publication notifications for managers")
    public void disablePublicationNotificationsForManagers() {
        changeProperty(SystemProperties.MANAGER_PUBLICATION_NOTIFICATION_ALLOW, false);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = "@Before enable shift exchange mobile approve")
    public void enableShiftExchangeMobileApprove() {
        changeProperty(SystemProperties.SHIFT_EXCHANGE_MOBILE_APPROVE, true);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = "@Before disable shift exchange mobile approve")
    public void disableShiftExchangeMobileApprove() {
        changeProperty(SystemProperties.SHIFT_EXCHANGE_MOBILE_APPROVE, false);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = "@Before disable roster single edited version")
    public void disableRosterSingleEditedVersion() {
        changeProperty(SystemProperties.ROSTER_SINGLE_EDITED_VERSION, false);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before disable roster publish without conflicts"})
    public void disableRosterPublishWithoutConflicts() {
        changeProperty(SystemProperties.ROSTER_PUBLISH_WITHOUT_CONFLICTS, false);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before don't show button to publish roster"})
    public void doNotShowButtonToPublishRoster() {
        changeProperty(SystemProperties.SHOW_BUTTON_TO_PUBLISH_ROSTER, false);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before show button to publish roster"})
    public void showButtonToPublishRoster() {
        changeProperty(SystemProperties.SHOW_BUTTON_TO_PUBLISH_ROSTER, true);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before enable copy shifts in worked roster"})
    public void enableWorkedRosterCopyShift() {
        changeProperty(SystemProperties.WORKED_ROSTER_COPY_SHIFTS, true);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before enable drag and drop function"})
    public void enableDragAndDropFunction() {
        changeProperty(SystemProperties.ENABLE_DRAG_AND_DROP_FUNCTION, true);
    }

    @AfterClass(alwaysRun = true, description = "Удалить правила табеля, созданные в ходе теста")
    public void removeTestTableRule(ITestContext c) {
        PresetClass.deleteTestTableRule(c);
    }

    @AfterMethod(alwaysRun = true, onlyForGroups = {"@After remove table rule"},
            description = "Удалить правила табеля, созданные в ходе теста")
    public void removeTestTableRuleOnlyForGroups(ITestContext c) {
        PresetClass.deleteTestTableRule(c);
    }

    @AfterMethod(alwaysRun = true, onlyForGroups = {"@After reset Hidden Fields In Card UI"},
            description = "Cкрывать в UI поля в карточке назначения")
    public void resetHiddenFieldsInCardUI() {
        changeProperty(SystemProperties.HIDE_UI_FIELDS_IN_DESTINATION_CARDS, "NONE");
    }

    @AfterGroups(alwaysRun = true, value = "@After remove add works and rules",
            description = "Удалить все доп. работы и правила, созданные в ходе теста")
    public void revertAddWorksAndRules() {
        CustomTools.revertAdditionalWorksAndRules();
    }

    @AfterMethod(alwaysRun = true, onlyForGroups = {"@After remove limits"},
            description = "Удалить все лимиты, созданные в ходе теста")
    public void removeLimits(ITestContext c) {
        List<String> limits = c.getAttributeNames()
                .stream()
                .filter(e -> e.contains("Limits_"))
                .collect(Collectors.toList());
        for (String entry : limits) {
            Limits limit = (Limits) c.getAttribute(entry);
            if (limit != null) {
                PresetClass.removeLimit(limit);
                c.removeAttribute(entry);
            }
        }
    }

    @BeforeMethod(alwaysRun = true,
            description = "Установить длину пароля")
    public void setPasswordLength() {
        changeProperty(SystemProperties.PASSWORD_COMPOSITION_REQUIREMENTS_MAXIMAL_LENGTH, 20);
    }

    @BeforeMethod(alwaysRun = true,
            description = "Отключить смену пароля у пользователей с чекбоксом \"Нужно сменить пароль\"")
    public void disableChangePasswordOnEnter() {
        changeProperty(SystemProperties.CHANGE_PASSWORD_ON_ENTER, false);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = "@Before disable change password in specified period")
    public void disableChangePasswordInSpecifiedPeriod() {
        changeProperty(SystemProperties.PASSWORD_CHANGE_IN_SPECIFIED_PERIOD, 0);
    }

    //    @AfterMethod(alwaysRun = true, //todo возможно, это можно удалить, т.к. есть хук
    //            description = "Удалить тестовую роль")
    //    public void deleteTestRole(ITestContext c) {
    //        PresetClass.deleteTestRole(c);
    //    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = "@Before turn off druid")
    public void setDataForDruid() {
        if (URL_BASE.contains("zozo")) {
            changeProperty(SystemProperties.KPI_FORECAST_STORE_IS_DRUID, false);
            changeProperty(SystemProperties.KPI_STORE_IS_DRUID, false);
            changeProperty(SystemProperties.DRUID_ADAPTER_URL, "");
        }
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = "@Before enable worked shift on roster joint")
    public void enableWorkedShiftOnRosterJoint() {
        changeProperty(SystemProperties.NORM_WORKED_SHIFT_ON_ROSTER_JOINT, true);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = "@Before disable filter alias by org unit format")
    public void disableFilterAliasByOrgUnitFormat() {
        changeProperty(SystemProperties.FILTER_ALIAS_BY_ORG_UNIT_FORMAT, false);
    }

    /**
     * Метод добавляет хук на случай остановки теста (например, во время отладки). Он также сработает и при обычном (не аварийном) завершении теста,
     * но удаление второй раз выполнено не будет, т.к. PresetClass.deleteTestRoles() убирает за собой атрибут, который хранит ссылку на роль.
     */
    @BeforeClass
    public static void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> PresetClass.deleteTestRole(Reporter.getCurrentTestResult().getTestContext())));
    }

    public static LocalDateTime getServerDateTime() {
        return Objects.requireNonNull(PresetClass.getBuildInfo()).getServerTime();
    }

    public LocalDate getServerDate() {
        return getServerDateTime().toLocalDate();
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = "@Before check if last day of month")
    public void checkLastDayOfMonth() {
        if (LocalDate.now().equals(LocalDateTools.getLastDate())) {
            throw new AssertionError(NO_VALID_DATE + "Сегодня последний день месяца.");
        }
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = "@Before today is penultimate day of month")
    public void checkTodayIsPenultimateDayOfMonth() {
        if (LocalDate.now().equals(LocalDateTools.getLastDate().minusDays(1))) {
            throw new AssertionError(NO_VALID_DATE + "Сегодня предпоследний день месяца.");
        }
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = "@Before check if first day of month")
    public void checkFirstDayOfMonth() {
        if (LocalDate.now().equals(LocalDateTools.getFirstDate())) {
            throw new AssertionError(NO_VALID_DATE + "Сегодня первый день месяца. Работа с табелем невозможна.");
        }
    }

    public void checkFirstDayOfMonthByOrgUnit(OrgUnit unit) {
        LocalDate date = Objects.isNull(unit.getTimeZone())
                ? LocalDate.now(ZoneId.of("UTC")) : ZonedDateTime.now(unit.getTimeZone()).toLocalDate();
        if (date.equals(LocalDateTools.getFirstDate())) {
            throw new AssertionError(NO_VALID_DATE + "Сегодня первый день месяца. Работа с табелем невозможна.");
        }
    }

    public void checkCertainNumberOfDaysInFuture(LocalDate lastAcceptableDate) {
        if (LocalDate.now().isAfter(lastAcceptableDate)) {
            Assert.fail(String.format("%sНе хватает дат в будущем в этом месяце", NO_VALID_DATE));
        }
    }

    public void checkCertainNumberOfDaysInPast(LocalDate firstAcceptableDate) {
        if (LocalDate.now().isBefore(firstAcceptableDate)) {
            Assert.fail(String.format("%sНе хватает дат в прошлом в этом месяце", NO_VALID_DATE));
        }
    }

    /**
     * Возвращает кастомную роль с разрешениями на просмотр и редактирование расписания (+перс. данные)
     */
    public Role getRoleWithBasicSchedulePermissions() {
        List<PermissionType> permissions = getBasicSchedulePermissions();
        return PresetClass.createCustomPermissionRole(permissions);
    }

    /**
     * Возвращает список разрешений на просмотр и редактирование расписания (+перс. данные)
     */
    public List<PermissionType> getBasicSchedulePermissions() {
        return new ArrayList<>(Arrays.asList(
                PermissionType.SCHEDULE_VIEW,
                PermissionType.SCHEDULE_EDIT,
                PermissionType.ORG_UNIT_EMPLOYEE_PERSONALISATION
        ));
    }

    public Matcher<WebElement> getMatcherDependingOnPermissions(boolean hasPermissions) {
        if (hasPermissions) {
            return text(Matchers.not(concealedMatcher));
        } else {
            return text(concealedMatcher);
        }
    }

    /**
     * Добавляет к тесту указанный тег
     */
    public void addTag(String tag) {
        Allure.getLifecycle()
                .updateTestCase(tr -> {
                    List<Label> currentLabels = tr.getLabels();
                    currentLabels.add(ResultsUtils.createTagLabel(tag));
                    tr.setLabels(currentLabels);
                });
    }

    /**
     * Добавляет к тесту указанный грейд
     */
    public void addSeverityLabel(SeverityLevel severityLevel) {
        Allure.getLifecycle()
                .updateTestCase(tr -> {
                    List<Label> currentLabels = tr.getLabels();
                    currentLabels.add(ResultsUtils.createSeverityLabel(severityLevel));
                    tr.setLabels(currentLabels);
                });
    }

    /**
     * Меняет имя теста в отчете.
     */
    public void changeTestName(String name) {
        Allure.getLifecycle().updateTestCase(tr -> tr.setName(name));
    }

    /**
     * Меняет имя степа в отчете.
     */
    public static void changeStepName(String name) {
        Allure.getLifecycle().updateStep(tr -> tr.setName(name));
    }

    public static void changeStepNameDependingOnParameter(boolean param, String nameIfTrue, String nameIfFalse) {
        if (param) {
            changeStepName(nameIfTrue);
        } else {
            changeStepName(nameIfFalse);
        }
    }

    /**
     * Меняет айди и название параметризованного теста. В description теста уже должно стоять название,
     * соответствующее значению true
     *
     * @param param       параметр, передаваемый датапровайдером
     * @param idIfTrue    айди теста, если param==true
     * @param idIfFalse   айди теста, если param==false
     * @param nameIfFalse новое название теста, если param==false
     */
    public void changeTestIDDependingOnParameter(boolean param, String idIfTrue, String idIfFalse, String nameIfFalse) {
        if (param) {
            addTag(idIfTrue);
        } else {
            addTag(idIfFalse);
            changeTestName(nameIfFalse);
        }
    }

    /**
     * Добавляет тег параметризованного теста
     *
     * @param param      параметр, передаваемый датапровайдером
     * @param tagIfTrue  айди теста, если param==true
     * @param tagIfFalse айди теста, если param==false
     */
    public void addTagDependingOnParameter(boolean param, String tagIfTrue, String tagIfFalse) {
        if (param) {
            addTag(tagIfTrue);
        } else {
            addTag(tagIfFalse);
        }
    }

    /**
     * Меняет название степа, если параметр == true
     */
    public void changeStepNameIfTrue(boolean param, String nameIfTrue) {
        if (param) {
            changeStepName(nameIfTrue);
        }
    }

    /**
     * Задает грейд теста в зависимости от переданного параметра
     */
    public void changeTestSeverityDependingOnParameter(boolean param, SeverityLevel levelIfTrue, SeverityLevel levelIfFalse) {
        addSeverityLabel(param ? levelIfTrue : levelIfFalse);
    }

    /**
     * Возвращает пользователя с указанными разрешениями для указанного подразделения
     *
     * @param permissions набор разрешений
     * @param unit        подразделение, на которое распространяются указанные разрешения
     * @return пользователь
     */
    public static User getUserWithPermissions(List<PermissionType> permissions, OrgUnit unit) {
        Role role = PresetClass.createCustomPermissionRole(permissions);
        return PresetClass.addCustomRoleToUser(role, RandomStringUtils.randomAlphanumeric(10), null, unit);
    }

    /**
     * Пересчитывает historyId исходя из списка исключённых параметров
     */
    public void excludeParametersAndRecalculateHistoryId(List<String> parameterNames) {
        Allure.getLifecycle().updateTestCase(tr -> {
            ITestNGMethod method = Reporter.getCurrentTestResult().getMethod();
            List<Parameter> currentParameters = tr.getParameters();
            List<Parameter> foundParameters = currentParameters.stream()
                    .filter(parameter -> parameterNames.contains(parameter.getName()))
                    .collect(Collectors.toList());
            currentParameters.removeAll(foundParameters);
            try {
                tr.setHistoryId(getHistoryId(method, currentParameters));
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
            foundParameters.forEach(parameter -> parameter.setExcluded(true));
            currentParameters.addAll(foundParameters);
            tr.setParameters(currentParameters);
        });
    }

    // Копия https://github.com/allure-framework/allure-java/blob/main/allure-testng/src/main/java/io/qameta/allure/testng/AllureTestNg.java#L634
    protected String getHistoryId(final ITestNGMethod method, final List<Parameter> parameters) throws UnsupportedEncodingException {
        final MessageDigest digest = getMd5Digest();
        final String testClassName = method.getTestClass().getName();
        final String methodName = method.getMethodName();
        digest.update(testClassName.getBytes(StandardCharsets.UTF_8));
        digest.update(methodName.getBytes(StandardCharsets.UTF_8));
        parameters.stream()
                .sorted(comparing(Parameter::getName).thenComparing(Parameter::getValue))
                .forEachOrdered(parameter -> {
                    digest.update(parameter.getName().getBytes(StandardCharsets.UTF_8));
                    digest.update(parameter.getValue().getBytes(StandardCharsets.UTF_8));
                });
        final byte[] bytes = digest.digest();
        return bytesToHex(bytes);
    }

    /**
     * Возвращает пользователя с указанными разрешениями для подразделения, в котором он является сотрудником
     *
     * @param permissions список разрешений
     */
    public static User getUserWithPermissions(List<PermissionType> permissions) {
        Role role = PresetClass.createCustomPermissionRole(permissions);
        Allure.addAttachment("Разрешения роли", role.getPermissions()
                .stream()
                .map(PermissionType::getTitle)
                .collect(Collectors.joining("\n")));
        return PresetClass.addCustomRoleToUser(role, RandomStringUtils.randomAlphanumeric(10), null);
    }

    /**
     * Смотрит на время сервера и возвращает время, которое точно будет считаться будущим, если попадет на сегодняшний день.
     * Дело в том, что смена будет считаться сменой в прошлом, если хотя бы одна ее минута не находится в будущем относительно времени сервера.
     * Тут берется запас в час.
     */
    public static LocalDateTime getDateTimeInFuture() {
        LocalDateTime serverTime = getServerDateTime();
        return serverTime.plusHours(5).truncatedTo(ChronoUnit.MINUTES);
    }

    public static void closeDriver(WebDriver driver) {
        try {
            driver.quit();
        } catch (NoSuchSessionException e) {
            LOG.info("Сессии не существует");
        }
    }

    public static void cleanCookiesAndOpenNewTab(WebDriver driver) {
        driver.manage().deleteAllCookies();
        String oldHandle = driver.getWindowHandle();
        ((JavascriptExecutor) driver).executeScript("window.open()");
        driver.switchTo().window(oldHandle);
        driver.close();
        Set<String> handles = driver.getWindowHandles();
        driver.switchTo().window(handles.iterator().next());
    }

    public static void setBrowserTimeout(WebDriver driver, int timeoutInSeconds) {
        try {
            driver.manage().timeouts().implicitlyWait(timeoutInSeconds, TimeUnit.SECONDS);
        } catch (NoSuchSessionException e) {
            LOG.info("Сессии не существует");
        }
    }

    public void disablePublishSystemPropertiesIfNoLimitIsSet(OrgUnit unit) {
        KpiRepository kpiRepository = new KpiRepository(DateUnit.MONTH);
        kpiRepository.setOrgUnit(unit);
        Double hourLimit = kpiRepository.getHourLimit().getValue();
        if (hourLimit == 0) {
            Allure.addAttachment("У подразделения нет лимитов часов на месяц.",
                                 "Отключаем системную настройку \"Публикация без нехватки норм\"");
            changeProperty(SystemProperties.ROSTER_PUBLISH_WITHOUT_NORMS_LACK, false);
        }
    }

    @AfterMethod(alwaysRun = true, onlyForGroups = "@After revert position attribute value",
            description = "Вернуть значение атрибута у тестируемой позиции в исходное состояние")
    public void revertPositionAttributeValue(ITestContext c) {
        List<String> parameters = c.getAttributeNames()
                .stream()
                .filter(e -> e.contains("posAttribute_"))
                .collect(Collectors.toList());
        if (!parameters.isEmpty()) {
            int posId = (int) c.getAttribute("posAttribute_position");
            String posAtrKey = (String) c.getAttribute("posAttribute_key");
            PresetClass.deleteEntityPropertyIfPresent(posId, MathParameterEntities.POSITION, posAtrKey);
            if (parameters.contains("posAttribute_value")) {
                String posAtrValue = (String) c.getAttribute("posAttribute_value");
                EntityPropertiesKey key = EntityPropertyKeyRepository.getPropertyByKey(MathParameterEntities.POSITION.getKeys(), posAtrKey);
                PresetClass.addEntityPropertyValue(MathParameterEntities.POSITION, posId, key, posAtrValue);
            }
        }
    }

}

package wfm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.Allure;
import io.qameta.allure.Step;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.http.*;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.events.EventFiringWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.Reporter;
import utils.BuildInfo;
import utils.Links;
import utils.Params;
import utils.Projects;
import utils.authorization.ClientReturners;
import utils.db.DBUtils;
import utils.tools.*;
import wfm.components.analytics.KpiType;
import wfm.components.analytics.ParamName;
import wfm.components.orgstructure.*;
import wfm.components.positioncategories.WorkGraphFilter;
import wfm.components.schedule.*;
import wfm.components.systemlists.IntervalType;
import wfm.components.systemlists.LimitType;
import wfm.components.systemlists.TableRuleShiftType;
import wfm.components.systemlists.TableRuleStrategy;
import wfm.components.utils.PermissionType;
import wfm.components.utils.PositionCharacteristics;
import wfm.components.utils.Role;
import wfm.models.*;
import wfm.models.PhoneType;
import wfm.repository.*;
import wfm.repository.listener.ResponseEventWebdriver;
import wfm.repository.listener.WebDriverEventCapture;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static utils.ErrorMessagesForReport.*;
import static utils.Links.EMPLOYEE_POSITIONS;
import static utils.Links.MATH_PARAMETERS;
import static utils.Links.ORGANIZATION_UNIT_ID;
import static utils.Links.POSITIONS;
import static utils.Links.SECURED_OPERATION_DESCRIPTORS;
import static utils.Links.*;
import static utils.Params.CALCULATION_MODE;
import static utils.Params.COMMENT;
import static utils.Params.EMPLOYEE_POSITION;
import static utils.Params.JOB_TITLE;
import static utils.Params.MATH_PARAMETER;
import static utils.Params.POSITION_CATEGORY;
import static utils.Params.POSITION_GROUP;
import static utils.Params.POSITION_TYPE;
import static utils.Params.SCHEDULE_REQUEST_TYPE;
import static utils.Params.TAGS;
import static utils.Params.*;
import static utils.authorization.CookieRW.cleanCookieFile;
import static utils.tools.CustomTools.*;
import static utils.tools.RequestFormers.*;
import static wfm.repository.CommonRepository.URL_BASE;
import static wfm.repository.CommonRepository.getToken;

/**
 * @author Evgeny Gurkin 18.08.2020
 */
public class PresetClass {

    private static final String PRESET_URL = Links.getTestProperty("release");
    private static final ContentType HAL_JSON = ContentType.create("application/hal+json", Consts.UTF_8);
    private static final String INTEGRATION_BIO_URL = getTestProperty("integrationBio");
    private static final Logger LOG = LoggerFactory.getLogger(PresetClass.class);
    private static final String REQUEST_LOGGER = "Запрос по адресу {}";
    private static final String JSON_LOGGER = "Отправлен JSON {}";
    private static final String HOURS_DEVIATION_LOGGER = "hoursDeviation: {}";
    private static final String STATUS_SHIFT = "Смена";
    private static final Random RANDOM = new Random();
    private static final String CASE_FTE = "Fte";
    private static final String CASE_KPI = "KPI";
    private static final String STRING = "STRING";
    public static final String CHANGED_SCHEDULE_REQUEST_ALIAS = "CHANGED_SCHEDULE_REQUEST_ALIAS";
    private static final String TEST_TABLE_RULE = "testTableRule";
    public static final String TEST_ROLE = "testRole";

    private PresetClass() {
    }

    public static void main(String[] args) {
        PresetClass.deleteTestRoles();
    }

    /**
     * Возвращает созданный объект
     *
     * @param response ответ сервера
     * @param tClass   класс созданного объекта
     */
    public static <T> T getCreatedObject(HttpResponse response, Class<T> tClass) {
        String location = response.getFirstHeader("location").getValue();
        return getClassObjectFromJson(tClass, getJsonFromUri(Projects.WFM, URI.create(location)));
    }

    /**
     * Смотрит ростеры оргюнита, если активный опубликован, смотрит остальные ростеры, если они все опубликованы,
     * то создает новый ростер и отправляет на утверждение, если есть неопубликованный, то просто отправляет случайный на
     * утверждение, если активный не опубликован и не утвержден то просто отправляет его на утверждение.
     */
    public static Roster nonPublishAndApproveChecker(int omId) {
        List<Roster> rosters = RosterRepository.getRosters(omId);
        Roster activeRoster = rosters.stream()
                .filter(Roster::isActive)
                .findAny()
                .orElseThrow(() -> new AssertionError(
                        String.format("%sВ оргюните №%d нет активного ростера", NO_TEST_DATA, omId)));
        if (activeRoster.isPublished()) {
            List<Roster> nonPublishedRosters = rosters
                    .stream()
                    .filter(roster -> !roster.isPublished() && roster.getVersion() != 0)
                    .collect(Collectors.toList());
            Roster rosterForApprove;
            if (nonPublishedRosters.isEmpty()) {
                presetForMakeShift(EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true),
                                   false, ShiftTimePosition.FUTURE);
                rosterForApprove = RosterRepository.getActiveRosterThisMonth(omId);
            } else {
                rosterForApprove = getRandomFromList(nonPublishedRosters);
            }
            makeRosterActive(rosterForApprove.getId());
            makeRosterOnApproval(rosterForApprove.getId());
            return rosterForApprove;
        } else if (!activeRoster.isOnApproval()) {
            makeRosterOnApproval(activeRoster.getId());
            return activeRoster;
        }
        return activeRoster;
    }

    /**
     * Для создания смены с учетом в каком промежутке времени от текущей даты удобнее взаимодействовать с ростерами
     *
     * @param dateEndInNextDay конец смены в следующем дне - true
     * @param timePosition     временной промежуток, в который необходимо создать смену
     * @return объект, описывающий созданную смену
     */
    public static Shift presetForMakeShift(EmployeePosition position, boolean dateEndInNextDay,
                                           ShiftTimePosition timePosition) {
        DateInterval dateInterval = timePosition.getShiftsDateInterval();
        List<Shift> shifts = ShiftRepository.getShifts(position, timePosition);
        List<LocalDate> dates = shifts.stream()
                .map(shift -> shift.getDateTimeInterval().getStartDate()).collect(Collectors.toList());
        LocalDate needDate;
        if (dates.isEmpty()) {
            needDate = dateInterval.getRandomDateBetween();
        } else if (dates.size() < dateInterval.difference()) {
            List<LocalDate> emptyDates = dateInterval.subtract(dates);
            needDate = getRandomFromList(emptyDates);
        } else {
            Shift shift = getRandomFromList(shifts);
            deleteRequest(shift);
            needDate = shift.getDateTimeInterval().getStartDate();
        }
        presetForEmptyRequestCell(position.getEmployee(), needDate);
        return presetForMakeShiftDate(position, needDate, dateEndInNextDay, timePosition, shifts);
    }

    /**
     * Создает смену в указанном временном промежутке. Если нет свободной даты в данном промежутке, выбрасывает AssertionError
     *
     * @param dateEndInNextDay конец смены в следующем дне - true
     * @param timePosition     временной промежуток, в который необходимо создать смену
     * @return объект, описывающий созданную смену
     */
    public static Shift presetForMakeShiftWithoutDeleteRequest(EmployeePosition position, boolean dateEndInNextDay,
                                                               ShiftTimePosition timePosition) {
        DateInterval dateInterval = timePosition.getShiftsDateInterval();
        List<Shift> shifts = ShiftRepository.getShifts(position, timePosition);
        List<LocalDate> dates = shifts.stream()
                .map(shift -> shift.getDateTimeInterval().getStartDate()).collect(Collectors.toList());
        LocalDate needDate;
        List<LocalDate> emptyDates;
        if (dates.isEmpty()) {
            needDate = getRandomFromList(dateInterval.getBetweenDatesList());
        } else if (dates.size() < dateInterval.difference()) {
            emptyDates = dateInterval.subtract(dates);
            needDate = getRandomFromList(emptyDates);
        } else {
            throw new AssertionError(NO_VALID_DATE + "Не найдено подходящей даты в данном временном промежутке");
        }
        presetForEmptyRequestCell(position.getEmployee(), needDate);
        return presetForMakeShiftDate(position, needDate, dateEndInNextDay, timePosition, shifts);
    }

    /**
     * Создает смену для аутстафф
     *
     * @param unit подразделение, где создается смена
     */
    @Step("Создать смену для аутстафф для подразделения {unit.name}")
    public static LocalDate makeOutStaffShift(OrgUnit unit) {
        Roster roster = RosterRepository.getActiveRosterThisMonth(unit.getId());
        List<Shift> outStaffShiftsFromApi = ShiftRepository.getShiftsForRoster(roster.getId(), new DateInterval())
                .stream()
                .filter(s -> s.getExchangeStatus().equals(BID_CREATED))
                .collect(Collectors.toList());
        if (outStaffShiftsFromApi.isEmpty()) {
            DateTimeInterval dateTimeInterval = new DateTimeInterval(LocalDateTime.now().plusDays(1),
                                                                     LocalDateTime.now().plusDays(1).plusHours(6));
            noEmployeeShiftMaker(roster, dateTimeInterval, null, null, null, null, null);
            return dateTimeInterval.getStartDate();
        } else {
            List<LocalDate> dates = outStaffShiftsFromApi.stream().map(e -> e.getDateTimeInterval().getStartDate()).collect(Collectors.toList());
            LOG.info("У подразделения {} (ID {}) найдены смены для аутстафф: {}. Новых смен не создано.", unit.getName(), unit.getId(), dates);
            return getRandomFromList(dates);
        }
    }

    /**
     * Создает свободную смену на бирже
     *
     * @param freeShiftDay день, в который нужно создать смену
     * @param roster       ростер, где создается смена
     * @param reason       причина привлечения сотрудника (nullable)
     */
    public static Shift makeFreeShift(LocalDate freeShiftDay, int omId, Roster roster, PositionGroup posGroup, PositionCategory posCat,
                                      ShiftHiringReason reason, DateTimeInterval dateTimeInterval, PositionType posType, RepeatRule rule) {
        if (roster == null) {
            roster = RosterRepository.getActiveRosterThisMonth(omId);
        }
        OrgUnit unit = OrgUnitRepository.getOrgUnit(roster.getOrganizationUnitId());
        switchShiftExchange(unit, true);
        if (Objects.isNull(dateTimeInterval)) {
            dateTimeInterval = new DateTimeInterval(freeShiftDay.atTime(14, 0), freeShiftDay.atTime(22, 0));
        }
        return noEmployeeShiftMaker(roster, dateTimeInterval, posGroup, posCat, reason, posType, rule);
    }

    /**
     * Назначить свободную смену сотруднику
     */
    public static Shift assignFreeShiftToEmployee(Employee employee, Shift freeShift) {
        String employeeFullName = employee.getFullName();
        EmployeePosition employeePosition = EmployeePositionRepository.getAvailableEmployeesForShiftFreeAssignment(freeShift.getId())
                .stream().filter(e -> e.getEmployee().getFullName().equals(employeeFullName)).findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Сотрудник " + employeeFullName + " не найден в списке сотрудник доступных на назначение на свободную смену"));
        Map<String, ImmutablePair<String, String>> links = new HashMap<>();
        ImmutablePair<String, String> link1 = new ImmutablePair<>(Params.HREF, employeePosition.getSelfLink());
        ImmutablePair<String, String> link2 = new ImmutablePair<>(Params.HREF, freeShift.getLink(REL_TAKE_SHIFTS_EXCHANGE));
        links.put(Params.EMPLOYEE_POSITION, link1);
        links.put(Params.SELF, link2);
        new ApiRequest.PostBuilder(makePath(SHIFTS, EXCHANGE, freeShift.getId()))
                .withBody(new ImmutablePair<>(Params.LINKS, links))
                .withStatus(200)
                .send();
        return freeShift.refreshShift(); //обновление employeePositionId
    }

    /**
     * Для удаления запроса или смен сотрудников по ссылке
     *
     * @param link - ссылка на удаление или href из шедулереквеста
     */
    public static void deleteRequest(URI link) {
        deleteMaker(link);
    }

    public static void deleteRequest(String link) {
        deleteMaker(URI.create(link));
    }

    public static <T extends HasLinks> void deleteRequest(T object) {
        deleteMaker(URI.create(getSelfLink(object)));
        //        Allure.addAttachment(String.format("Удаление смены за %s", startDate),
        //                             "ID смены: " + shift.getId()); //todo bring back logging, make it type-independent
    }

    public static Pairs.Builder getShiftCalculationParamBuilder(LocalDate start, LocalDate end,
                                                                boolean rerostering, boolean withMinDeviation) {
        return Pairs.newBuilder()
                .from(start)
                .rerostering(rerostering)
                .to(end)
                .withMinDeviation(withMinDeviation);
    }

    /**
     * Делает расчёт/перерасчёт расписания
     *
     * @param omId             id оргюнита
     * @param rerostering      расчет по новым сотрудникам
     * @param withMinDeviation с минимальным отклонением
     */
    public static void calculateRoster(int omId, LocalDate start, LocalDate end,
                                       boolean rerostering, boolean withMinDeviation) {
        List<NameValuePair> nameValuePairs = getShiftCalculationParamBuilder(start, end, rerostering, withMinDeviation).build();
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(Links.CALC_JOBS, omId, ROSTERING), nameValuePairs);
        HttpResponse response = requestMaker(uri, new JSONObject(), RequestBuilder.post(), HAL_JSON);
        assertStatusCode(response, 201, uri.toString());
        Allure.addAttachment("Расчёт",
                             String.format("Для подразделения с ID %s был запущен расчёт расписания на интервале от %s до %s", omId, start, end));
        String header = response.getFirstHeader("Location").getValue();
        boolean isCalculationStarted = PresetClass.checkForAssignedCalcInstance(header);
        if (!isCalculationStarted) {
            PresetClass.startLastActiveCalculationNode();
        }
        CalculationStatusRepository.waitForCalculation(header);
    }

    private static void startLastActiveCalculationNode() {
        JSONObject json = getJsonFromUri(Projects.WFM, URI.create(makePath(URL_BASE, API_V1, "calc-instances")));
        if (Objects.nonNull(json)) {
            List<CalcInstance> allCalcInstances = getListFromJsonObject(json.getJSONObject(EMBEDDED), CalcInstance.class);
            CalcInstance calcInstance = allCalcInstances.stream()
                    .max(Comparator.comparing(CalcInstance::getLastLiveTime))
                    .orElseThrow(NoSuchElementException::new);
            if (!calcInstance.getStatus().equals("ACTIVE")) {
                new ApiRequest.PostBuilder(calcInstance.getLink("startInstance")).withStatus(200).send();
            }
        }
    }

    private static boolean checkForAssignedCalcInstance(String header) {
        int tries = 3;
        for (int i = 0; i < tries; i++) {
            JSONObject object = getJsonFromUri(Projects.WFM, URI.create(header));
            LOG.info(Objects.nonNull(object) ? object.toString() : "{}");
            CalcJob calcJob = CustomTools.getClassObjectFromJson(CalcJob.class, object);
            String calcJobInstanceName = calcJob.getInstanceName();
            if (!calcJobInstanceName.isEmpty()) {
                return true;
            }
            systemSleep(2);
        }
        return false;
    }

    /**
     * Пресет создания смены по заданным параметрам, задает дату, время начала и конца смены.
     * Если на заданную дату существует смена, и она соответствует значению, заданному в dateEndInNextDay,
     * то возвращается она. Иначе она удаляется.
     *
     * @param startDate        день начала смены
     * @param dateEndInNextDay флаг должна ли созданная смена кончаться завтрашним днем
     * @param timePosition     флаг, должна ли смена создаваться в будущем или это не важно
     * @param shifts           все смены сотрудника
     */
    public static Shift presetForMakeShiftDate(EmployeePosition employeePosition, LocalDate startDate,
                                               boolean dateEndInNextDay,
                                               ShiftTimePosition timePosition, List<Shift> shifts) {
        Shift presentShift = shifts.stream()
                .filter(e -> e.getDateTimeInterval().getStartDate().equals(startDate)
                        && e.isNextDayEnd() == dateEndInNextDay)
                .findFirst().orElse(null);
        if (presentShift != null) {
            return presentShift;
        }
        LocalDateTime endDateTime;
        LocalDateTime startDateTime;
        if (dateEndInNextDay) {
            startDateTime = startDate.atTime(22, 0, 0);
            endDateTime = startDate.plusDays(1).atTime(8, 0, 0);
            LocalDate endDate = endDateTime.toLocalDate();
            shifts.stream().filter(shift -> shift.getDateTimeInterval().getStartDate().isEqual(endDate))
                    .findFirst().ifPresent(PresetClass::deleteRequest);
        } else {
            startDateTime = startDate.atTime(14, 0, 0);
            endDateTime = startDate.atTime(22, 0, 0);
        }
        makeClearDate(employeePosition, timePosition, startDate);
        Roster roster = RosterRepository.getNeededRosterId(timePosition, new DateInterval(startDate.with(TemporalAdjusters.firstDayOfMonth()),
                                                                                          startDate.with(TemporalAdjusters.lastDayOfMonth())),
                                                           employeePosition.getOrgUnit().getId());
        return shiftPresetMaker(roster, employeePosition, new DateTimeInterval(startDateTime, endDateTime));
    }

    /**
     * Пресет создания нескольких смен.
     * Если на заданную дату существует смена, не переходящая на следующий день,
     * то возвращается она. Иначе она удаляется.
     *
     * @param dates        дни, на которых необходимы смены
     * @param timePosition флаг, должна ли смена создаваться в будущем или это не важно
     * @param shifts       все смены сотрудника
     */
    public static List<Shift> presetForMakeShiftsDates(EmployeePosition employeePosition, List<LocalDate> dates,
                                                       ShiftTimePosition timePosition, List<Shift> shifts) {
        List<Shift> shiftsReturn = new ArrayList<>();
        for (LocalDate date : dates) {
            shiftsReturn.add(presetForMakeShiftDate(employeePosition, date, false, timePosition, shifts));
        }
        return shiftsReturn;
    }

    public static List<Shift> presetForMakeShiftsDates(EmployeePosition employeePosition, List<LocalDate> dates,
                                                       ShiftTimePosition timePosition) {
        List<Shift> shiftsReturn = new ArrayList<>();
        for (LocalDate date : dates) {
            shiftsReturn.add(presetForMakeShiftDate(employeePosition, date, false, timePosition));
        }
        return shiftsReturn;
    }

    public static Shift presetForMakeShiftDate(EmployeePosition employeePosition, LocalDate startDate,
                                               boolean dateEndInNextDay,
                                               ShiftTimePosition timePosition) {
        return presetForMakeShiftDate(employeePosition, startDate, dateEndInNextDay, timePosition,
                                      ShiftRepository.getShifts(employeePosition, timePosition));
    }

    /**
     * Пресет создания смены по заданным параметрам, задает временной промежуток, время начала и конца смены.
     * Если на заданную дату уже существует смена, она удаляется.
     *
     * @param startTime    время начала смены
     * @param timePosition временной промежуток, в который необходимо создать смену
     */
    public static Shift presetForMakeShiftTime(EmployeePosition employeePosition, LocalTime startTime, LocalTime endTime,
                                               ShiftTimePosition timePosition, LocalDate... excludedDates) {
        List<LocalDate> dateList = timePosition.getShiftsDateInterval().subtract(Arrays.asList(excludedDates));
        LocalDate date = getRandomFromList(dateList);
        return presetForMakeShiftDateTime(employeePosition, date.atTime(startTime), date.atTime(endTime),
                                          timePosition, ShiftRepository.getShifts(employeePosition, timePosition));
    }

    /**
     * Создать свободную смену вместе с причиной привлечения сотрудника
     */
    public static Shift createFreeShift(LocalDate date, int omId, Roster roster, PositionGroup posGroup,
                                        PositionCategory posCat, ShiftHiringReason hiringReason,
                                        DateTimeInterval dateTimeInterval, PositionType posType) {
        List<Shift> freeShiftsBefore = ShiftRepository.getFreeShifts(omId, date);
        PresetClass.makeFreeShift(date, omId, roster, posGroup, posCat, hiringReason, dateTimeInterval, posType, null);
        List<Shift> freeShiftsAfter = ShiftRepository.getFreeShifts(omId, date);
        freeShiftsAfter.removeAll(freeShiftsBefore);
        Shift shift = freeShiftsAfter.stream().findFirst()
                .orElseThrow(() -> new AssertionError("Не найдена созданная свободная смена за " + date));
        if (URL_BASE.contains("magnit")) {
            Allure.step(String.format("Отправить запрос на добавление к свободной смене причины привлечения \"%s\"",
                                      hiringReason.getTitle()), () -> PresetClass.addHiringReason(shift, hiringReason));
        }
        return shift;
    }

    /**
     * Пресет создания смены по заданным параметрам, задает дату, время начала и конца смены.
     * Если на заданную дату уже существует смена, она удаляется.
     *
     * @param startDateTime день начала смены
     * @param timePosition  флаг, должна ли смена создаваться в будущем или это не важно
     * @param shifts        все смены сотрудника
     */
    public static Shift presetForMakeShiftDateTime(EmployeePosition employeePosition, LocalDateTime startDateTime,
                                                   LocalDateTime endDateTime,
                                                   ShiftTimePosition timePosition, List<Shift> shifts) {
        LocalDate endDate = endDateTime.toLocalDate();
        shifts.stream()
                .filter(shift -> shift.getDateTimeInterval().getStartDate().isEqual(endDate))
                .findFirst()
                .ifPresent(PresetClass::deleteRequest);
        if (timePosition.equals(ShiftTimePosition.PREVIOUS_MONTH)) {
            makeClearDate(employeePosition, timePosition, startDateTime.toLocalDate());
        } else {
            makeClearDate(employeePosition, startDateTime.toLocalDate());
        }
        Roster roster = RosterRepository.getNeededRosterId(timePosition, new DateInterval(), employeePosition.getOrgUnit().getId());
        return shiftPresetMaker(roster, employeePosition, new DateTimeInterval(startDateTime, endDateTime));
    }

    public static Shift presetForMakeShiftDateTime(EmployeePosition employeePosition, LocalDateTime startDateTime,
                                                   LocalDateTime endDateTime,
                                                   ShiftTimePosition timePosition) {
        return presetForMakeShiftDateTime(employeePosition, startDateTime, endDateTime,
                                          timePosition, ShiftRepository.getShifts(employeePosition, timePosition));
    }

    /**
     * Для очистки запросов сотрудника за выбранную дату
     *
     * @param employee сотрудник
     * @param date     дата для очистки запроса
     */
    public static void presetForEmptyRequestCell(Employee employee, LocalDate date) {
        LOG.info("Выполняем поиск запросов для {}, за {} ", employee.getFullName(), date);
        String urlEnding = makePath(EMPLOYEES, employee.getId(), SCHEDULE_REQUESTS);
        List<NameValuePair> nameValuePairs = Pairs.newBuilder().from(date).to(date).build();
        JSONObject embedded = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, urlEnding, nameValuePairs);
        JSONArray requests = getJsonArrayFromJsonObject(embedded);
        JSONObject tempObj;
        if (requests != null) {
            tempObj = requests.getJSONObject(0);
            String value = tempObj.getJSONObject(LINKS).getJSONObject(SELF).getString(HREF);
            deleteRequest(value);
            Allure.addAttachment(String.format("Пресет для очистки запросов за %s", date),
                                 String.format("Был удален запрос за %s у сотрудника %s", date, employee.getFullName()));
        }
    }

    /**
     * Вспомогательный метод для создания смены. Собирает стартовый JSON.
     * Далее к нему необходимо добавлять разные строки в зависимости от типа создаваемой смены (см. shiftPresetMaker и noEmployeeShiftMaker)
     *
     * @param roster           ростер, в который добавляется смена
     * @param dateTimeInterval дата и время начала и конца смены
     */
    private static JSONObject commonShiftJsonParts(Roster roster, DateTimeInterval dateTimeInterval) {
        String urlEnding = makePath(ROSTERS, roster.getId(), POSITION_CATEGORY_ROSTERS);
        JSONObject someRosterCategory = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, urlEnding);
        JSONArray positionCategory = getJsonArrayFromJsonObject(someRosterCategory);
        int positions = positionCategory != null ? positionCategory.getJSONObject(0).getInt(ID) : (int) JSONObject.NULL;
        JSONObject miniObject = new JSONObject();
        JSONObject dateTimeIntervalJson = new JSONObject();
        dateTimeIntervalJson.put(START_DATE_TIME, dateTimeInterval.getStartDateTime());
        dateTimeIntervalJson.put(END_DATE_TIME, dateTimeInterval.getEndDateTime());
        miniObject.put(DATE_TIME_INTERVAL, dateTimeIntervalJson);
        miniObject.put(ROSTER + "Id", roster.getId());
        miniObject.put(POSITION_CATEGORY_ROSTER_ID, positions);
        return miniObject;
    }

    /**
     * Вспомогательный метод для создания смены. Посылает запрос на создание и проверяет код ответа
     *
     * @param miniObject JSON-объект для создания смены
     * @param logMessage сообщение, которое будет выводить в логах
     */
    private static Shift sendApiRequestForShift(JSONObject miniObject, String logMessage, LocalDate date, String allureContent) {
        String pathShift = miniObject.has(REPEAT_RULE) && !miniObject.getJSONObject(REPEAT_RULE).get(NAME).equals("Не повторять") ? SHIFT_EXCHANGE_RULE : SHIFTS;
        URI uriPreset = setUri(Projects.WFM, CommonRepository.URL_BASE, pathShift, Pairs.newBuilder().calculateConstraints(true).build());
        HttpResponse response = requestMaker(uriPreset, miniObject, RequestBuilder.post(), HAL_JSON);
        LOG.info(REQUEST_LOGGER, uriPreset);
        LOG.info(JSON_LOGGER, miniObject);
        if (response == null) {
            throw new AssertionError(FAILED_PRESET + "Не удалось создать смену");
        }
        if (response.getStatusLine().getStatusCode() != 201) {
            miniObject.put(STATUS, "APPROVED");
            miniObject.put("commentText", "Другое");
            response = requestMaker(uriPreset, miniObject, RequestBuilder.post(), HAL_JSON);
            LOG.info("Первый запрос не прошел, второй запрос по адресу: {}", uriPreset);
            LOG.info(JSON_LOGGER, miniObject);
        }
        LOG.info(logMessage);
        LOG.info(REQUEST_LOGGER, uriPreset);
        LOG.info(JSON_LOGGER, miniObject);
        assertStatusCode(response, 201, uriPreset.toString());
        Allure.addAttachment(String.format("Пресет для создания смены за %s", date), allureContent);
        return getCreatedObject(response, Shift.class);
    }

    /**
     * Создает смену
     *
     * @param roster           ростер, в который добавляется смена
     * @param employeePosition позиция, для которой создается смена
     * @param dateTimeInterval дата и время начала и конца смены
     */
    private static Shift shiftPresetMaker(Roster roster, EmployeePosition employeePosition, DateTimeInterval dateTimeInterval) {
        JSONObject miniObject = commonShiftJsonParts(roster, dateTimeInterval);
        miniObject.put(EMPLOYEE_POSITION_ID, employeePosition.getId());
        miniObject.put(STATUS_NAME, STATUS_SHIFT);
        String empName = employeePosition.getEmployee().getFullName();
        String logMessage = String.format("Формирование запроса создания смены для %s за %s число", empName, dateTimeInterval.getStartDateTime());
        String allureContent = String.format("Для сотрудника %s была создана смена с рабочим временем %s", empName, dateTimeInterval);
        return sendApiRequestForShift(miniObject, logMessage, dateTimeInterval.getStartDate(), allureContent);
    }

    /**
     * Создает смену без сотрудника (для биржи смен). Для создания свободной смены нужно указать группу и категорию позиции.
     * Для создания позиции аутстафф группа и категория позиции должны быть null.
     * Если только один из этих параметров null, метод роняет тест.
     *
     * @param roster           ростер, в который добавляется смена
     * @param dateTimeInterval дата и время начала и конца смены
     * @param posGroup         группа позиции (на UI "Функциональная роль"), для которой создается смена
     * @param posCat           категория позиции, для которой создается смена
     */
    public static Shift noEmployeeShiftMaker(Roster roster, DateTimeInterval dateTimeInterval, PositionGroup posGroup, PositionCategory posCat,
                                             ShiftHiringReason reason, PositionType posType, RepeatRule rule) {
        JSONObject miniObject = commonShiftJsonParts(roster, dateTimeInterval);
        String logMessage;
        String allureContent;
        if (posCat == null && posGroup == null) {
            posGroup = PositionGroupRepository.getPositionGroupByName("Аутстафф");
            posCat = PositionCategoryRepository.getAllPositionCategoriesByFilter(WorkGraphFilter.ALL)
                    .stream().filter(g -> g.getName().contains("АУТ"))
                    .collect(randomItem());
            miniObject.put(Params.OUTSTAFF, true);
            logMessage = String.format("Формирование запроса создания смены для аутстафф за %s число", dateTimeInterval.getStartDateTime());
            allureContent = "Для сотрудника аутстафф была создана смена с рабочим временем %s";
        } else if (posCat != null && posGroup != null) {
            JSONObject links = new JSONObject();
            if (reason != null) {
                miniObject.put(HIRING_REASON_TEXT, reason.getTitle());
                links.put("shiftHiringReason", reason.getLinkWrappedInJson(SELF));
            }
            links.put(POSITION_GROUP, posGroup.getLinkWrappedInJson(SELF));
            links.put(ROSTER, roster.getLinkWrappedInJson(SELF));
            if (posCat != null && posType != null) {
                miniObject.put(JOB_TITLE, posType.getName());
                int jobTitleId = JobTitleRepository.getJob(posType.getName()).getId();
                links.put(JOB_TITLE, new JSONObject().put(HREF, setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(JOB_TITLES, jobTitleId))));
            }
            links.put(SELF, new JSONObject().put(HREF, setUri(Projects.WFM, CommonRepository.URL_BASE, SHIFTS)));
            miniObject.put(LINKS, links);

            miniObject.put(Params.OUTSTAFF + "Position", JSONObject.NULL);
            miniObject.put(IS_SHIFT_MODEL, true);
            JSONObject repeatRule = new JSONObject();
            if (Objects.isNull(rule)) {
                repeatRule.put(PERIODICITY, NO_REPEAT);
                repeatRule.put(NAME, "Не повторять");
                miniObject.put(REPEAT_RULE, repeatRule);
            } else {
                repeatRule.put(PERIODICITY, rule.getPeriodicity());
                repeatRule.put(NAME, rule.getName());
                miniObject.put(REPEAT_RULE, repeatRule);

                JSONObject dateInterval = new JSONObject();
                dateInterval.put(START_DATE, dateTimeInterval.getStartDate());
                dateInterval.put(END_DATE, dateTimeInterval.getEndDate());
                miniObject.put(DATE_INTERVAL, dateInterval);

                JSONObject timeInterval = new JSONObject();
                timeInterval.put(START_TIME, dateTimeInterval.getStartDateTime().toLocalTime());
                timeInterval.put(END_TIME, dateTimeInterval.getEndDateTime().toLocalTime());
                miniObject.put(TIME_INTERVAL, timeInterval);
                miniObject.put(REPEAT, rule.getPeriodicity());
            }

            logMessage = String.format("Формирование запроса создания свободной смены за %s для группы \"%s\" и категории \"%s\"",
                                       dateTimeInterval.getStartDateTime(), posGroup.getName(), posCat.getName());
            allureContent = "Была создана свободная смена с рабочим временем %s для группы " + posGroup.getName() + " и категории " + posCat.getName();
        } else {
            throw new AssertionError(FAILED_PRESET + "PositionGroup и PositionCategory должны быть либо одновременно null, либо одновременно не null");
        }
        miniObject.put(STATUS_NAME + "_", STATUS_SHIFT);
        miniObject.put(POSITION_CATEGORY, getJsonFromUri(Projects.WFM, posCat.getSelfLink()));
        miniObject.put(POSITION_CATEGORY + "Id", posCat.getCategoryId());
        miniObject.put(POSITION_GROUP, getJsonFromUri(Projects.WFM, posGroup.getSelfLink()));
        miniObject.put(EMPLOYEE, JSONObject.NULL);
        miniObject.put(EMPLOYEE_POSITION, JSONObject.NULL);
        miniObject.put(END_DATE, dateTimeInterval.getEndDate());
        miniObject.put(POSITION, JSONObject.NULL);
        miniObject.put(START_DATE, dateTimeInterval.getStartDate());
        return sendApiRequestForShift(miniObject, logMessage, dateTimeInterval.getStartDate(), String.format(allureContent, dateTimeInterval));
    }

    /**
     * Добавить разрешения к ролевой модели пользователя
     *
     * @param permissions - список разрешений
     * @param userRole    - ролевая модель
     */
    private static void addTypesToUser(List<Permission> permissions, UserRole userRole) {
        permissions.stream().map(Permission::getId).forEach(id -> addTypeToUser(id, userRole));
    }

    /**
     * Добавить разрешения на математические параметры к ролевой модели пользователя
     *
     * @param mathParameters - список мат параметров
     * @param userRole       - ролевая модель
     */
    private static void addMathTypesToUser(List<MathParameter> mathParameters, UserRole userRole) {
        mathParameters.stream().map(MathParameter::getMathParameterId).forEach(mathParamId -> addMathTypeToUser(mathParamId, userRole));

    }

    /**
     * Добавить одно разрешение на просмотр мат. параметра к ролевой модели пользователя
     *
     * @param mathParamId - айди математического параметра
     * @param userRole    - ролевая модель
     */
    private static void addMathTypeToUser(int mathParamId, UserRole userRole) {
        String urlEnding = makePath(USER_ROLES, userRole.getId(), MATH_PARAMETERS);
        JSONObject miniObject = new JSONObject();
        JSONObject links = new JSONObject();
        JSONObject self = new JSONObject();
        JSONObject mathParameter = new JSONObject();
        self.put(HREF, userRole.getMathParametersLink());
        links.put(SELF, self);
        mathParameter.put(HREF, setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(MATH_PARAMETERS, mathParamId)));
        links.put(MATH_PARAMETER, mathParameter);
        miniObject.put(LINKS, links);
        URI uriPreset = setUri(Projects.WFM, CommonRepository.URL_BASE, urlEnding);
        HttpResponse response = requestMaker(uriPreset, miniObject, RequestBuilder.post(), HAL_JSON);
        LOG.info(REQUEST_LOGGER, uriPreset);
        LOG.info(JSON_LOGGER, miniObject);
        assertStatusCode(response, uriPreset.toString());
    }

    /**
     * Добавить одно разрешение к ролевой модели пользователя
     *
     * @param permissionId - айди разрешения
     * @param userRole     - ролевая модель
     */
    private static void addTypeToUser(int permissionId, UserRole userRole) {
        String urlEnding = makePath(USER_ROLES, userRole.getId(), SECURED_OPERATION_DESCRIPTORS);
        JSONObject miniObject = new JSONObject();
        JSONObject links = new JSONObject();
        JSONObject self = new JSONObject();
        JSONObject permission = new JSONObject();
        self.put(HREF, userRole.getSecuredOperationDescriptorLink());
        links.put(SELF, self);
        links.put("permission", permission);
        permission.put(HREF, setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(PERMISSIONS, permissionId)));
        miniObject.put(LINKS, links);
        miniObject.put(LOCAL, true);
        URI uriPreset = setUri(Projects.WFM, CommonRepository.URL_BASE, urlEnding);
        HttpResponse response = requestMaker(uriPreset, miniObject, RequestBuilder.post(), HAL_JSON);
        LOG.info(REQUEST_LOGGER, uriPreset);
        LOG.info(JSON_LOGGER, miniObject);
        assertStatusCode(response, uriPreset.toString());
    }

    /**
     * Добавить ролевую модель с указанным именем
     *
     * @param userRoleName - имя роли
     */
    public static UserRole addUserRole(String userRoleName) {
        JSONObject miniObject = new JSONObject();
        miniObject.put(NAME, userRoleName);
        miniObject.put(DESCRIPTION, ZonedDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm:ss a")));
        URI uriPreset = setUri(Projects.WFM, CommonRepository.URL_BASE, USER_ROLES);
        HttpResponse response = requestMaker(uriPreset, miniObject, RequestBuilder.post(), HAL_JSON);
        LOG.info(REQUEST_LOGGER, uriPreset);
        LOG.info(JSON_LOGGER, miniObject);
        assertStatusCode(response, 201, uriPreset.toString());
        UserRole role = getCreatedObject(response, UserRole.class);
        Reporter.getCurrentTestResult().getTestContext().setAttribute(TEST_ROLE, role.getSelfLink());
        return role;
    }

    /**
     * Добавить пользователю роль, для указанного оргюнита, с датой окончания
     *
     * @param user      пользователь
     * @param role      роль
     * @param orgUnitId айди оргюнита
     * @param endDate   дата окончания действия роли
     */
    private static void addRoleToUser(User user, UserRole role, List<Integer> orgUnitId, LocalDate endDate) {
        //сначала добавляется роль, а потом добавляется оргюнит, по другому не работает
        int roleId = role.getId();
        if (!user.getRolesIds().contains(roleId)) {
            addOnlyRoleForUser(user, roleId, endDate, orgUnitId);
        } else {
            //если роль существует, убедиться, что срок ее действия не истек
            User.RoleInUser roleInUser = user.getRoles()
                    .stream()
                    .filter(r -> r.getUserRoleId() == roleId)
                    .collect(Collectors.toList()).get(0);
            LocalDate currentEndDate = roleInUser.getEndRoleDate();
            if (currentEndDate != null && currentEndDate.isBefore(LocalDate.now())) {
                LOG.info("Истек срок действия роли пользователя, присваиваем новую дату окончания");
                prolongRoleForUser(roleInUser, endDate);
            }
        }
        addOrgUnitToRoleInUser(user, roleId, orgUnitId);
    }

    /**
     * Запрос на добавление роли к пользователю с указанием даты окончания.
     *
     * @param user    пользователь, которому добавляем роль
     * @param roleId  айди добавляемой роли
     * @param endDate дата окончания действия роли
     */
    private static void addOnlyRoleForUser(User user, int roleId, LocalDate endDate, List<Integer> orgUnitId) {
        JSONObject miniObject = new JSONObject();
        JSONObject links = new JSONObject();
        JSONObject self = new JSONObject();
        JSONObject userRole = new JSONObject();
        URI userRolesUri = RequestFormers.setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(USERS, user.getId(), ROLES));
        userRole.put(HREF, setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(USER_ROLES, roleId)));
        self.put(HREF, userRolesUri.toString());
        links.put(SELF, self);
        links.put(REL_USER_ROLE, userRole);
        miniObject.put(LINKS, links);
        miniObject.put(FROM, LocalDateTools.now().minusDays(15));
        miniObject.put(ORGANIZATION_UNIT_IDS, orgUnitId);
        miniObject.put("_selectedOrgUnits", orgUnitId);
        if (endDate != null) {
            miniObject.put(TO, endDate);
        }
        HttpResponse response = requestMaker(userRolesUri, miniObject, RequestBuilder.post(), HAL_JSON);
        LOG.info(REQUEST_LOGGER, userRolesUri);
        LOG.info(JSON_LOGGER, miniObject);
        assertStatusCode(response, HttpStatus.SC_CREATED, userRolesUri.toString());
    }

    /**
     * Обновляет дату окончания роли пользователя
     *
     * @param roleInUser айди роли у конкретного пользователя
     * @param endDate    новая дата окончания действия роли
     */
    private static void prolongRoleForUser(User.RoleInUser roleInUser, LocalDate endDate) {
        JSONObject miniObject = new JSONObject();
        JSONObject links = new JSONObject();
        JSONObject self = new JSONObject();
        JSONObject userRole = new JSONObject();
        URI userRolesUri = RequestFormers.setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(ORG_UNIT_ROLE, roleInUser.getId()));
        userRole.put(HREF, setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(USER_ROLES, roleInUser.getUserRoleId())));
        self.put(HREF, userRolesUri);
        links.put(SELF, self);
        links.put(REL_USER_ROLE, userRole);
        miniObject.put(LINKS, links);
        miniObject.put(FROM, LocalDateTools.now().minusDays(15));
        miniObject.put(TO, endDate);
        HttpResponse response = requestMaker(userRolesUri, miniObject, RequestBuilder.put(), HAL_JSON);
        LOG.info(REQUEST_LOGGER, userRolesUri);
        LOG.info(JSON_LOGGER, miniObject);
        assertStatusCode(response, userRolesUri.toString());
    }

    /**
     * Запрос на добавление роли к пользователю с указанием оргюнитов
     *
     * @param user      пользователь для которого добавляем роль
     * @param roleId    айди роли
     * @param orgUnitId список айди оргюнитов для добавления
     */
    private static void addOrgUnitToRoleInUser(User user, int roleId, List<Integer> orgUnitId) {
        String orgUnitsLink = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, makePath(USERS, user.getId(), ROLES, roleId))
                .getJSONObject(LINKS).getJSONObject(REL_ORG_UNITS).getString(HREF);
        String number = orgUnitsLink.substring(orgUnitsLink.lastIndexOf(ORG_UNIT_ROLE) + ORG_UNIT_ROLE.length() + 1, orgUnitsLink.lastIndexOf("/"));
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(ORG_UNIT_ROLE, number, ORG_UNITS));
        JSONArray orgUnits = new JSONArray();
        for (Integer integer : orgUnitId) {
            orgUnits.put(integer);
        }
        JSONObject miniObject = new JSONObject();
        miniObject.put(ORG_SELF, orgUnits);
        miniObject.put(ORG_CHILD, orgUnits);
        HttpResponse response = requestMaker(uri, miniObject, RequestBuilder.post(), HAL_JSON);
        LOG.info(REQUEST_LOGGER, uri);
        LOG.info(JSON_LOGGER, miniObject);
        assertStatusCode(response, uri.toString());
    }

    /**
     * Проверяет у сотрудника наличие пользователя, если нет, то добавляет
     */
    public static void addUser(Employee employee) {
        if (Objects.nonNull(employee.refreshEmployee().getLink(REL_ACCOUNT))) {
            return;
        }
        URI uriPreset = setUri(Projects.WFM, CommonRepository.URL_BASE, USERS);
        String username = RandomStringUtils.randomAlphanumeric(10);
        String tempPass = RandomStringUtils.randomAlphanumeric(10);
        JSONObject miniObject = new JSONObject();
        miniObject.put(USERNAME, username);
        miniObject.put(PASSWORD, tempPass);
        miniObject.put(ID, employee.getId());
        JSONObject links = new JSONObject();
        links.put(EMPLOYEE, employee.getLinkWrappedInJson(SELF));
        links.put(SELF, new JSONObject().put(HREF, uriPreset));
        miniObject.put(LINKS, links);
        HttpResponse response = requestMaker(uriPreset, miniObject, RequestBuilder.post(), HAL_JSON);
        LOG.info(REQUEST_LOGGER, uriPreset);
        LOG.info(JSON_LOGGER, miniObject);
        Allure.addAttachment("Данные для входа под пользователем",
                             String.format("Пользователь: \"%s\", пароль: \"%s\"", username, tempPass));
        assertStatusCode(response, 201, uriPreset.toString());
    }

    /**
     * Изменяет пароль пользователя на указанный
     *
     * @param user     пользователь, у которого меняем пароль
     * @param tempPass новый пароль
     */
    public static void changePassword(User user, String tempPass) {
        changePassword(user, tempPass, false);
    }

    public static User changePassword(User user, String tempPass, boolean changePassword) {
        String username = user.getUsername();
        JSONObject miniObject = new JSONObject();
        miniObject.put("username", username);
        miniObject.put("password", tempPass);
        URI uriPreset;
        miniObject.put("employeeId", user.getEmployee().getId());
        miniObject.put("changePassword", changePassword);
        uriPreset = setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(USERS, user.getId(), "change-password"));
        HttpResponse response = requestMaker(uriPreset, miniObject, RequestBuilder.put(), HAL_JSON);
        LOG.info(REQUEST_LOGGER, uriPreset);
        LOG.info(JSON_LOGGER, miniObject);
        Allure.addAttachment("Данные для входа под пользователем",
                             String.format("Пользователь: \"%s\", пароль: \"%s\"", username, tempPass));
        assertStatusCode(response, uriPreset.toString());
        return user.refresh();
    }

    /**
     * Очищаем пользователя от всех ролей, указанных в списке
     *
     * @param rolesToDelete список айди ролей
     * @param user          айди пользователя
     */
    public static void clearUserFromRoles(Set<Integer> rolesToDelete, User user) {
        for (int id : rolesToDelete) {
            User.RoleInUser role = user.getRoles().stream()
                    .filter(roleInUser -> roleInUser.getUserRoleId() == id || roleInUser.getId() == id)
                    .findAny()
                    .orElse(null);
            deleteOrgUnitsFromRoles(role);
            URI uriPreset;
            //определяем, какую роль нужно удалить: user-role или org-unit-role, в зависимости от этого формируется ссылка на удаление
            List<Integer> userRoles = UserRoleRepository.getUserRoles()
                    .stream()
                    .map(UserRole::getId)
                    .collect(Collectors.toList());
            if (userRoles.contains(id)) {
                assert role != null;
                deleteOrgUnitsFromRole(role.getId());
                uriPreset = setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(ORG_UNIT_ROLE, role.getId()));
            } else {
                uriPreset = setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(ORG_UNIT_ROLE, id));
            }
            deleteRequest(uriPreset);
        }
    }

    /**
     * Очищаем роль пользователя от всех оргюнитов
     */
    private static void deleteOrgUnitsFromRoles(User.RoleInUser role) {
        if (role != null && !role.getOrgUnitList().isEmpty()) {
            URI uriPreset = role.getLink(REMOVE_ORG_UNITS_ALL);
            LOG.info(REQUEST_LOGGER, uriPreset);
            deleteRequest(uriPreset);
            /* //todo удалить после закрытия TEST-1662
            Закомментированный код стабильно удаляет подразделения из роли, но пока оставляю нестабильный вариант выше, чтобы собрать примеров для баг-репорта.
            URI uriPreset = role.getLink("removeOrgUnits");
            JSONObject miniObject = new JSONObject().put("orgUnitIds", role.getOrgUnitList());
            HttpResponse response = requestMaker(uriPreset, miniObject, RequestBuilder.post(), HAL_JSON);
            LOG.info(REQUEST_LOGGER, uriPreset);
            assertStatusCode(response, uriPreset.toString());
             */
        } else {
            LOG.info("К роли не было привязано ни одного оргюнита.");
        }
    }

    /**
     * Удаляет все орг юниты у роли
     *
     * @param orgUnitRoleId - айди орг юнит роли. Можно получить с помощью метода User.RoleInUser#getOrgUnitRole()
     */
    public static void deleteOrgUnitsFromRole(int orgUnitRoleId) {
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(ORG_UNIT_ROLE, orgUnitRoleId, ORG_UNITS));
        JSONObject json = getJsonFromUri(Projects.WFM, uri);
        LOG.info(REQUEST_LOGGER, uri);
        if (!json.has(EMBEDDED)) {
            return;
        }
        JSONArray orgUnits = json.getJSONObject(EMBEDDED).getJSONArray(REL_ORG_UNITS);
        List<OrgUnitInRole> orgUnitsInRole = CustomTools.getListFromJsonArray(orgUnits, OrgUnitInRole.class);
        if (!orgUnitsInRole.isEmpty()) {
            JSONObject miniObject = new JSONObject();
            JSONArray array = new JSONArray();
            for (OrgUnitInRole orgUnit : orgUnitsInRole) {
                array.put(orgUnit.getId());
            }
            miniObject.put(ORGANIZATION_UNIT_IDS, array);
            URI uriPreset = setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(ORG_UNIT_ROLE, orgUnitRoleId, ORG_UNITS_REMOVE));
            HttpResponse response = requestMaker(uriPreset, miniObject, RequestBuilder.post(), HAL_JSON);
            LOG.info(REQUEST_LOGGER, uriPreset);
            assertStatusCode(response, 200, uri.toString());
        }

    }

    /**
     * Принимает роль, которую нужно очистить (нужно для ее дальнейшего удаления). Находит пользователей, которым присвоена эта роль,
     * находит у них соответствующие embeddedRole, очищает эти роли от оргюнитов, после чего удаляет их.
     *
     * @param role - роль, которую нужно очистить
     */

    public static void clearRoleFromUsers(UserRole role) {
        //ищем сотрудников, которым присвоена интересующая нас роль
        List<NameValuePair> pair = Pairs.newBuilder().roleIds(role.getId()).withUnattached(true).includeDate(LocalDate.now()).build();
        URI employeesWithRole = setUri(Projects.WFM, CommonRepository.URL_BASE, EMPLOYEES, pair);
        JSONObject json = getJsonFromUri(Projects.WFM, employeesWithRole);
        List<Employee> employees = getListFromJsonObject(json, Employee.class);
        //находим пользователей для этих сотрудников
        List<User> users = new ArrayList<>();
        for (Employee employee : employees) {
            JSONObject userJson = getJsonFromUri(Projects.WFM, employee.getLink(REL_ACCOUNT));
            User user = getClassObjectFromJson(User.class, userJson);
            users.add(user);
        }
        //находим роли этих пользователей, передаем ту, которая нас интересует, на дальнейшую очистку
        for (User user : users) {
            Set<Integer> roleIds = user.getRoles().stream()
                    .filter(e -> e.getUserRoleId() == role.getId())
                    .map(User.RoleInUser::getId)
                    .collect(Collectors.toSet());
            clearUserFromRoles(roleIds, user);
        }
    }

    /**
     * Добавить к пользователю заместитителя
     *
     * @param toWhom - пользователь к котрому добавим заместителя
     * @param toAdd  - будущий заместитель
     */
    public static void addDeputyToUser(User toWhom, User toAdd) {
        JSONObject miniObject = new JSONObject();
        JSONObject links = new JSONObject();
        JSONObject user = new JSONObject();
        JSONObject self = new JSONObject();
        miniObject.put(FROM, LocalDate.now().minusDays(20));
        miniObject.put(TO, LocalDate.now().plusDays(100));
        miniObject.put(LINKS, links);
        user.put(HREF, toAdd.getSelfLink());
        URI uriPreset = URI.create(toWhom.getLink("userDeputys"));
        self.put(HREF, uriPreset);
        links.put(USER, user);
        links.put(SELF, self);
        HttpResponse response = requestMaker(uriPreset, miniObject, RequestBuilder.post(),
                                             ContentType.create(Params.HAL_JSON));
        LOG.info(REQUEST_LOGGER, uriPreset);
        LOG.info(JSON_LOGGER, miniObject);
        assertStatusCode(response, 201, uriPreset.toString());
    }

    /**
     * Удаляет из роли все разрешения из списка
     *
     * @param permissionsIds - айди разрешения из списка
     * @param userRole       - ролевая модель
     */
    private static void removePermissions(Set<Integer> permissionsIds, UserRole userRole) {
        int roleId = userRole.getId();
        for (int id : permissionsIds) {
            URI uriPreset = setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(USER_ROLES, roleId, SECURED_OPERATION_DESCRIPTORS, id));
            LOG.info("Удаляем доступ {} у роли {}", id, userRole.getName());
            deleteRequest(uriPreset);
        }
    }

    /**
     * Удаляет из роли разрешение на мат параметр
     *
     * @param mathParamId mathParamId   - айди разрешения из списка
     * @param userRole    - ролевая модель
     */
    private static void removeMathParamPermission(Integer mathParamId, UserRole userRole) {
        int roleId = userRole.getId();
        URI uriPreset = setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(USER_ROLES, roleId, MATH_PARAMETERS, mathParamId));
        LOG.info("Удаляем доступ на мат параметр {} у роли {}", mathParamId, userRole.getName());
        deleteRequest(uriPreset);
    }

    /**
     * Проверяет, есть ли у оргюнита расписание
     *
     * @param scheduleType тип расписания которое нам нужно
     * @param unit         подразделение
     */
    public static boolean checkOrgUnitBusinessHours(ScheduleType scheduleType, OrgUnit unit) {
        String urlEnding = makePath(ORGANIZATION_UNITS, unit.getId(), BUSINESS_HOURS);
        JSONObject empObject = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, urlEnding);
        List<BusinessHours> businessHoursList = getListFromJsonObject(empObject, BusinessHours.class);
        String orgUnitSelectionAttachmentTitle = "Выбор оргюнита";
        String orgUnitSelectionAttachmentContents = "Был выбран оргЮнит \"%s\" с графиком работы";
        if (!businessHoursList.isEmpty()) {
            switch (scheduleType) {
                case ANY_TYPE:
                    Allure.addAttachment(orgUnitSelectionAttachmentTitle, String.format(orgUnitSelectionAttachmentContents, unit.getName()));
                    return true;
                case SALE_AND_SERVICE:
                    Set<ScheduleType> scheduleTypes = businessHoursList.stream().map(BusinessHours::getEnumType).collect(Collectors.toSet());
                    Allure.addAttachment(orgUnitSelectionAttachmentTitle, String.format(orgUnitSelectionAttachmentContents, unit.getName()));
                    if (scheduleTypes.size() == 2) {
                        return true;
                    }
                    break;
                default:
                    for (BusinessHours businessHours : businessHoursList) {
                        if (businessHours.getEnumType() == scheduleType) {
                            Allure.addAttachment(orgUnitSelectionAttachmentTitle,
                                                 "Был выбран оргЮнит с активным графиком: " + scheduleType.getNameOfType());
                            return true;
                        }
                    }
            }
        }
        return false;
    }

    /**
     * Задает значение для системной настройки
     *
     * @param sysProp - системная настройка
     * @param value   - значение
     */
    public static <T> void setSystemPropertyValue(SystemProperties sysProp, T value) {
        SystemProperty<T> systemProperty = SystemPropertyRepository.getSystemProperty(sysProp);
        if (value != null && !(value.getClass().equals(systemProperty.getDataType()))) {
            throw new ClassCastException(String.format("%sПереданное значение %s нельзя привести к классу %s",
                                                       FAILED_PRESET, value, systemProperty.getDataType()));
        }
        JSONObject miniObject = new JSONObject();
        miniObject.put(ENABLED, true);
        miniObject.put(KEY, systemProperty.getKey());
        miniObject.put(TITLE, systemProperty.getTitle());
        miniObject.put(DESCRIPTION, systemProperty.getDescription());
        miniObject.put(VALUE, value);
        miniObject.put(TYPE, systemProperty.getType());
        URI uriPreset = systemProperty.getSelfLink();
        HttpResponse response = requestMaker(uriPreset, miniObject, RequestBuilder.put(),
                                             ContentType.create(Params.HAL_JSON).withCharset(Charset.defaultCharset()));
        LOG.info(REQUEST_LOGGER, uriPreset);
        LOG.info(JSON_LOGGER, miniObject);
        assertStatusCode(response, uriPreset.toString());
        Allure.addAttachment("Пресет для изменения настройки",
                             String.format("Значение настройки \"%s\" установлено в %s", sysProp.getKey().toString(), value.toString()));
    }

    /**
     * Удаляет все комментарии причины изменения смены имени
     *
     * @param nameToMatch - имя причины
     */
    public static void deleteAllShiftEditReasonsMatchesText(String nameToMatch) {
        ShiftEditReasonRepository.getShiftEditReasons()
                .stream()
                .filter(shiftEditReason -> shiftEditReason.getTitle().toLowerCase().contains(nameToMatch))
                .map(ShiftEditReason::getSelfLink).forEach(PresetClass::deleteRequest);
    }

    private static UserRole addRole(Role role) {
        List<UserRole> userRoles = UserRoleRepository.getUserRoles();
        UserRole userRole = userRoles.stream().filter(u -> u.getName()
                .equals(role.getName())).findAny().orElseGet(() -> addUserRole(role.getName()));
        DBUtils.deleteDuplicatePermissionIds(userRole.getId());
        Set<Integer> rolePermissionsIds = userRole.getSecuredOperationDescriptor().getPermissionIds();

        List<Permission> matchMap = PermissionRepository.getPermissions();
        List<PermissionType> needTypes = role.getPermissions();

        Set<Integer> redundantPermissions = new HashSet<>(rolePermissionsIds);

        Set<Integer> roleMathParamIds = userRole.getMathParameters().getMathParamIds();
        Set<Integer> currentMathParamPermissions = new HashSet<>(roleMathParamIds);
        List<MathParameter> needMathTypes = role.getMathParameters();
        needMathTypes.stream()
                .map(MathParameter::getMathParameterId)
                .filter(id -> !currentMathParamPermissions.contains(id))
                .collect(Collectors.toList())
                .forEach(id -> addMathTypeToUser(id, userRole));
        Set<Integer> needMathIds = needMathTypes.stream().map(MathParameter::getMathParameterId)
                .collect(Collectors.toSet());
        currentMathParamPermissions.stream()
                .filter(id -> !needMathIds.contains(id))
                .forEach(id -> removeMathParamPermission(id, userRole));
        //оставили из всех допусков, только для нашей роли
        matchMap = matchMap.stream().filter(permission -> needTypes.contains(permission.getPermissionType())).collect(Collectors.toList());
        //получили лишние доступы
        matchMap.stream().map(Permission::getId).collect(Collectors.toList()).forEach(redundantPermissions::remove);
        //удалили те, которые уже есть у данной роли
        matchMap = matchMap.stream().filter(permission -> !rolePermissionsIds.contains(permission.getId())).collect(Collectors.toList());
        if (!matchMap.isEmpty()) {
            addTypesToUser(matchMap, userRole);
        }
        if (!redundantPermissions.isEmpty()) {
            removePermissions(redundantPermissions, userRole);
        }

        return userRole;
    }

    /**
     * Добавляет роль с указанным оргюнитом и датой окончания для случайного сотрудника
     *
     * @param role    роль для добавления
     * @param unit    оргюнит который будет в роли
     * @param endDate дата окончания действия роли
     */
    public static User addRoleToRandomUser(Role role, LocalDate endDate, OrgUnit... unit) {
        UserRole userRole = addRole(role);
        Employee employeeWithRole = EmployeeRepository.getRandomEmployeeWithAccount(userRole.getId(), false);
        Employee random = employeeWithRole != null ? employeeWithRole :
                EmployeeRepository.getRandomEmployeeWithAccount(true);
        User randomUser = random.getUser();
        if (randomUser.getRoles().stream().anyMatch(r -> r.getUserRoleId() == userRole.getId())) {
            clearOrgUnitsFromUser(randomUser, userRole);
        }
        addRoleToUser(randomUser, userRole, (unit != null && unit[0] != null) ?
                              Stream.of(unit).map(OrgUnit::getId).collect(Collectors.toList()) :
                              Collections.singletonList(OrgUnitRepository.getRandomOrgUnit().getId()),
                      endDate != null ? endDate : LocalDate.now().plusYears(2));
        LOG.info("Сотруднику с именем {} будет присвоена роль {}",
                 random.getFullName(), userRole.getName());
        return randomUser;
    }

    /**
     * Добавляет роль c указанной датой окончания для случайного сотрудника.
     * Роль будет применена для подразделения, в котором сотрудник работает
     *
     * @param role    роль для добавления
     * @param endDate дата окончания действия роли
     */
    public static User addRoleToRandomUser(Role role, LocalDate endDate) {
        UserRole userRole = addRole(role);
        Employee employeeWithRole = EmployeeRepository.getRandomEmployeeWithAccount(userRole.getId(), false);
        Employee random = employeeWithRole != null ? employeeWithRole :
                EmployeeRepository.getRandomEmployeeWithAccount(true);
        EmployeePosition ep = EmployeePositionRepository.getFirstActiveEmployeePositionFromEmployee(random);
        User randomUser = random.getUser();
        if (randomUser.getRoles().stream().anyMatch(r -> r.getUserRoleId() == userRole.getId())) {
            clearOrgUnitsFromUser(randomUser, userRole);
        }
        OrgUnit unit = ep.getOrgUnit();
        addRoleToUser(randomUser, userRole, Collections.singletonList(unit.getId()),
                      endDate != null ? endDate : LocalDate.now().plusYears(2));
        LOG.info("Сотруднику с именем {} будет присвоена роль {}",
                 random.getFullName(), userRole.getName());
        return randomUser.refresh();
    }

    private static void clearOrgUnitsFromUser(User user, UserRole userRole) {
        String urlEnding = makePath(ORG_UNIT_ROLE, user.getId(), ROLES, userRole.getId(), ORG_UNITS_REMOVE_ALL);
        deleteRequest(setUri(Projects.WFM, CommonRepository.URL_BASE, urlEnding));
    }

    /**
     * Добавляет роль с указанным оргюнитом и датой окончания для конкретного пользователя
     *
     * @param role    - роль для добавления
     * @param unit    - оргюнит который будет в роли
     * @param endDate - дата окончания действия роли
     */
    public static User addRoleToTargetUser(Role role, OrgUnit unit, LocalDate endDate, User user) {
        List<UserRole> userRoles = UserRoleRepository.getUserRoles();
        UserRole userRole = userRoles.stream().filter(u -> u.getName()
                .equals(role.getName())).findAny().orElseGet(() -> addUserRole(role.getName()));
        DBUtils.deleteDuplicatePermissionIds(userRole.getId());
        user.getRoles()
                .stream()
                .filter(r -> r.getUserRoleId() == userRole.getId() || r.getId() == userRole.getId())
                .findAny().ifPresent(PresetClass::deleteOrgUnitsFromRoles);
        Set<Integer> rolePermissionsIds = userRole.getSecuredOperationDescriptor().getPermissionIds();
        List<Permission> matchMap = PermissionRepository.getPermissions();
        List<PermissionType> needTypes = role.getPermissions();
        Set<Integer> redundantPermissions = new HashSet<>(rolePermissionsIds);
        //оставили из всех допусков, только для нашей роли
        matchMap = matchMap.stream().filter(permission -> needTypes.contains(permission.getPermissionType())).collect(Collectors.toList());
        //получили лишние доступы
        matchMap.stream().map(Permission::getId).collect(Collectors.toList()).forEach(redundantPermissions::remove);
        //удалили те, которые уже есть у данной роли
        matchMap = matchMap.stream().filter(permission -> !rolePermissionsIds.contains(permission.getId())).collect(Collectors.toList());
        if (!matchMap.isEmpty()) {
            addTypesToUser(matchMap, userRole);
        }
        if (!redundantPermissions.isEmpty()) {
            removePermissions(redundantPermissions, userRole);
        }
        Set<Integer> roles = user.getRolesIds();
        if (!roles.isEmpty()) {
            roles.remove(userRole.getId());
            clearUserFromRoles(roles, user);
        }
        addRoleToUser(user, userRole, unit != null ?
                              Stream.of(unit).map(OrgUnit::getId).collect(Collectors.toList()) :
                              Collections.singletonList(OrgUnitRepository.getRandomOrgUnit().getId()),
                      endDate != null ? endDate : LocalDate.now().plusYears(2));
        LOG.info("Сотруднику с именем {} будет присвоена роль {}",
                 user.getEmployee().getFullName(), role.getName());
        return user;
    }

    /**
     * Создает кастомную роль.
     *
     * @param permissionTypes список разрешений, которые должны быть у роли.
     * @return роль
     */
    public static Role createCustomPermissionRole(List<PermissionType> permissionTypes) {
        String testName;
        try {
            testName = Reporter.getCurrentTestResult().getName();
        } catch (NullPointerException npe) {
            testName = Thread.currentThread().getStackTrace()[2].getMethodName();
        }
        String roleName = String.format("test_role_%s_%s", RandomStringUtils.randomAlphabetic(5), testName);
        Role role = Role.CUSTOM;
        role.setName(roleName);
        List<PermissionType> temp = new ArrayList<>(permissionTypes);
        role.setPermissionTypes(temp);
        //нужно иначе будут проблемы при выборе другого юзера
        cleanCookieFile(Projects.WFM, role);
        LOG.info("Сотруднику будет присвоена кастомная роль со следующими разрешениями: {}", permissionTypes);
        Allure.addAttachment("Разрешения роли", permissionTypes
                .stream()
                .map(PermissionType::getTitle)
                .collect(Collectors.toList())
                .toString());
        return role;
    }

    /**
     * Создает вспомогательную роль (для теста с двумя ролями).
     *
     * @param permissionTypes список разрешений, которые должны быть у роли.
     * @return роль
     */
    public static Role createSecondaryPermissionRole(List<PermissionType> permissionTypes) {
        Role role = Role.TEST;
        role.setPermissionTypes(permissionTypes);
        //нужно иначе будут проблемы при выборе другого юзера
        cleanCookieFile(Projects.WFM, role);
        LOG.info("Сотруднику будет присвоена кастомная роль со следующими разрешениями: {}", permissionTypes);
        return role;
    }

    /**
     * Добавляет мат параметры к роли.
     *
     * @param role           роль, к которой нужно добавить мат. параметры
     * @param mathParameters список разрешений на мат. параметры, который должны быть у роли
     * @return роль
     */
    public static Role addMathParamsPermissionsToRole(Role role, List<MathParameter> mathParameters) {
        role.setMathParameters(mathParameters);
        return role;
    }

    /**
     * Добавляет заданное число случайных мат. параметров с указанным представлением и прикрепляет их к роли
     *
     * @param role          роль, которой нужен доступ к мат. параметрам
     * @param entity        сущность, для которой ищутся мат. параметры
     * @param viewType      тип представления данных на UI (например, список, таблица
     * @param numberOfItems количество мат. параметров, которое нужно вернуть
     */
    public static List<MathParameter> addSeveralMathParametersToRole(Role role, MathParameterEntities entity, String viewType, int numberOfItems) {
        List<MathParameter> params = getRandomFromList(MathParameterRepository.getMathParametersWithEntity(entity)
                                                               .stream()
                                                               .filter(p -> p.getViewType().equals(viewType)
                                                                       && !p.getMathValues().isEmpty())
                                                               .collect(Collectors.toList()), numberOfItems);
        role.setMathParameters(params);
        List<String> stringParamNames = params
                .stream()
                .map(MathParameter::getShortName)
                .collect(Collectors.toList());
        LOG.info("Добавляем к роли {} права на мат. параметры {}", role.getName(), stringParamNames);
        Allure.addAttachment("Добавление прав на мат. параметры для роли " + role.getName(), stringParamNames.toString());
        return params;
    }

    /**
     * Смотрит мат параметр у сотрудника и очищает его если он не пустой
     *
     * @param employeeId - айди сотрудника
     * @param id         - айди параметра
     * @param name       - название параметра для отчета
     */
    public static void checkEmployeeParams(int employeeId, int id, String name) {
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(EMPLOYEES, employeeId, MATH_PARAMETER_VALUES, id));
        try {
            setUrlAndInitiateForApi(uri, Projects.WFM);
            deleteRequest(uri);
            Allure.addAttachment("Пресет для очистки мат. параметров", "Параметр у сотрудника был предварительно очищен");
        } catch (AssertionError e) {
            Allure.addAttachment("Пресет для очистки мат. параметров", "Параметр у сотрудника был изначально пустой");
        }
        Allure.addAttachment("Выбранный параметр",
                             String.format("В ходе теста будет использован параметр с названием %s", name));
    }

    /**
     * Отправляет ростер на утверждение
     *
     * @param rosterID айди ростера
     */
    public static void makeRosterOnApproval(int rosterID) {
        String path = makePath(ROSTERS, rosterID, ON_APPROVE);
        List<NameValuePair> pairs = Pairs.newBuilder().force(false).build();
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, path, pairs);
        HttpResponse response = requestMaker(uri, new JSONObject(), RequestBuilder.put(), HAL_JSON);
        assertStatusCode(response, uri.toString());
        checkRosterActionSuccess(response);
        LOG.info("Пресет на утверждение графика ростера № {}", rosterID);
        LOG.info(REQUEST_LOGGER, uri);
        Allure.addAttachment("Пресет утверждения графика",
                             "При помощи пресета был отправлен на утверждение график ростера № " + rosterID);
    }

    /**
     * Снимает с ростера галку "на утверждении"
     *
     * @param rosterID айди ростера
     */
    public static void rejectRosterApproval(int rosterID) {
        String path = makePath(ROSTERS, "reject-approve", rosterID);
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, path);
        HttpResponse response = requestMaker(uri, new JSONObject(), RequestBuilder.put(), HAL_JSON);
        LOG.info("Снимаем с утверждения график ростера № {}", rosterID);
        LOG.info(REQUEST_LOGGER, uri);
        assertStatusCode(response, uri.toString());
        Allure.addAttachment("Удаление у графика признака \"На утверждении\"",
                             "При помощи пресета был отправлен на снятие с утверждения графика ростера № " + rosterID);
    }

    /**
     * Для создания смены с учетом в каком промежутке времени от текущей даты удобнее взаимодействовать с ростерами и
     * учетом дат, которые нельзя трогать, так как они должны остаться для дальнейших операций
     *
     * @param dateEndInNextDay - конец смены в следующем дне - true
     * @param timePosition     - выборка по датам осущевствляется от текущего времени - true
     * @return номер дня в месяце когда была создана смена
     */
    public static Shift presetForMakeShiftWithExcludeDate(EmployeePosition position, boolean dateEndInNextDay,
                                                          ShiftTimePosition timePosition, LocalDate... excludeDate) {
        List<LocalDate> excludedDates = Arrays.asList(excludeDate);
        DateInterval dateInterval = timePosition.getShiftsDateInterval();
        List<Shift> shifts = ShiftRepository.getShifts(position, timePosition).stream()
                .filter(shift -> !excludedDates.contains(shift.getDateTimeInterval().getStartDate())).collect(Collectors.toList());
        List<LocalDate> dates = shifts.stream()
                .map(shift -> shift.getDateTimeInterval().getStartDate()).collect(Collectors.toList());
        LocalDate needDate;
        if (dates.size() < dateInterval.difference()) {
            List<LocalDate> emptyDates = dateInterval.subtract(dates);
            emptyDates.removeAll(excludedDates);
            needDate = getRandomFromList(emptyDates);
        } else {
            Shift shift = getRandomFromList(shifts);
            deleteRequest(shift);
            needDate = shift.getDateTimeInterval().getStartDate();
        }
        presetForEmptyRequestCell(position.getEmployee(), needDate);
        return presetForMakeShiftDate(position, needDate, dateEndInNextDay, timePosition, shifts);
    }

    /**
     * Создает конфликт с нарушением междусменного интервала
     */
    public static LocalDate createConflictOfShiftIntervalViolation(int omId, EmployeePosition ep) {
        changeOrSetMathParamValue(omId, MathParameterValues.CHECK_VIOLATIONS, true, true);
        setPriorityLevelToConstraintViolation(ConstraintViolations.SHORT_REST_BETWEEN_SHIFTS, ConstraintViolationLevel.HIGH, false);
        LocalDate date = getFreeDateForEmployeeShiftPreset(ep, ShiftTimePosition.ALLMONTH, ShiftTimePosition.FUTURE.getShiftsDateInterval());
        presetForMakeShiftDateTime(ep, date.minusDays(1).atTime(15, 0, 0),
                                   date.atTime(0, 0, 0), ShiftTimePosition.FUTURE);
        presetForMakeShiftDateTime(ep, date.atTime(8, 0, 0),
                                   date.atTime(17, 0, 0), ShiftTimePosition.FUTURE);
        return date;
    }

    /**
     * Создает комментарий причину для изменения смены с указанным названием
     *
     * @param name - название причины
     */
    public static void createShiftEditReason(String name) {
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, SHIFTS_EDIT_REASON);
        JSONObject positionCategory = new JSONObject();
        positionCategory.put("code", "test");
        positionCategory.put(TITLE, name);
        HttpResponse response = requestMaker(uri, positionCategory, RequestBuilder.post(), HAL_JSON);
        assertStatusCode(response, 201, uri.toString());
    }

    /**
     * Изменить комментарий причину изменения смены
     *
     * @param shiftCommentReasonUri - старое название причины
     * @param newName               - новое
     */
    public static void updateShiftEditReason(URI shiftCommentReasonUri, String newName) {
        JSONObject positionCategory = new JSONObject();
        positionCategory.put("code", "test");
        positionCategory.put(TITLE, newName);
        HttpResponse response = requestMaker(shiftCommentReasonUri, positionCategory, RequestBuilder.put(), HAL_JSON);
        assertStatusCode(response, shiftCommentReasonUri.toString());
        Allure.addAttachment("Пресет для изменения комментария о причине редактирования смены",
                             String.format("Был изменен комментарий в запросе на: %s", newName));
    }

    /**
     * Пресет. Создает в расписании запрос
     *
     * @param repeat повторяемый или нет
     * @param omId   айди оргюнита
     * @return объект запроса
     */
    public static ScheduleRequest createScheduleRequestApi(ScheduleRequestStatus status, boolean repeat, int omId) {
        return createScheduleRequestOfCertainType(status, repeat, omId, ScheduleRequestType.OFF_TIME);
    }

    /**
     * Пресет. Создает в расписании запрос заданного типа
     *
     * @param repeat повторяемый или нет
     * @param omId   айди оргюнита
     * @param type   тип запроса
     * @return объект запроса
     */
    public static ScheduleRequest createScheduleRequestOfCertainType(ScheduleRequestStatus status, boolean repeat,
                                                                     int omId, ScheduleRequestType type) {
        ScheduleRequestAlias requestType;
        try {
            requestType = ScheduleRequestAliasRepository.getAlias(type);
        } catch (NoSuchElementException e) {
            requestType = addScheduleRequestType(type);
        }
        EmployeePosition employeePosition = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, false);
        Employee employee = employeePosition.getEmployee();
        int id = employee.getId();
        JSONObject miniObject = new JSONObject();
        JSONObject dateInterval = new JSONObject();
        LocalDate startDate;
        LocalDate endDate;
        String startDateInInterval;
        String endDateInInterval;
        String uriEnd;
        String forAllure;
        List<NameValuePair> pairs = Pairs.newBuilder().calculateConstraints(true).build();
        if (repeat) {
            forAllure = "повторяющегося";
            ImmutablePair<LocalDate, LocalDate> days = twoFreeDaysChecker(employeePosition, ShiftTimePosition.FUTURE);
            startDate = days.getLeft();
            endDate = days.getRight();
            uriEnd = makePath(SCHEDULE_REQUEST_RULES);
            miniObject.put("repeat", "DAILY");
            dateInterval.put(START_DATE, startDate.toString());
            dateInterval.put(END_DATE, endDate.toString());
            miniObject.put(DATE_INTERVAL, dateInterval);
            miniObject.put("periodicityStartDate", "null");
            miniObject.put("periodicityEndDate", endDate);
            miniObject.put(EMPLOYEE_POSITION_ID, "null");
        } else {
            forAllure = "одиночного";
            startDate = getFreeDateFromNow(employeePosition);
            startDateInInterval = startDate + "T00:00:00";
            endDateInInterval = startDate + "T23:59:59";
            endDate = startDate;
            uriEnd = makePath(SCHEDULE_REQUESTS);
            dateInterval.put(START_DATE_TIME, startDateInInterval);
            dateInterval.put(END_DATE_TIME, endDateInInterval);
            miniObject.put(DATE_TIME_INTERVAL, dateInterval);
        }

        miniObject.put(TYPE, requestType.getType());
        miniObject.put("aliasCode", requestType.getOuterId());
        miniObject.put(START_DATE, startDate);
        miniObject.put(END_DATE, endDate);
        miniObject.put("daylong", true);
        miniObject.put(STATUS, status.toString());
        miniObject.put(ROSTER_ID_JSON, "null");
        miniObject.put(EMPLOYEE_ID, id);
        miniObject.put("positionId", employeePosition.getPosition().getId());
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, uriEnd, pairs);
        HttpResponse response = requestMaker(uri, miniObject, RequestBuilder.post(), HAL_JSON);
        LOG.info("Для создания {} события типа {} отправлен запрос на адрес: {}", forAllure, status, uri);
        LOG.info(JSON_LOGGER, miniObject);
        assertStatusCode(response, 201, uri.toString());
        Allure.addAttachment(String.format("Пресет для создания запроса за %s", startDate),
                             String.format("В расписании не было %s запроса, в ходе пресета был создан запрос %s", forAllure, miniObject));
        List<ScheduleRequest> request = ScheduleRequestRepository.getEmployeeScheduleRequests(employee.getId(), new DateInterval(startDate, startDate), omId);
        if (!request.isEmpty()) {
            return request.get(0);
        }
        throw new AssertionError(FAILED_PRESET + "Не удалось создать запрос");
    }

    /**
     * Пресет. Создает в расписании запрос на определенную дату
     */
    public static ScheduleRequest createScheduleRequestForDate(ScheduleRequestStatus status, LocalDate date,
                                                               EmployeePosition employeePosition, ScheduleRequestType type) {
        Employee employee = employeePosition.getEmployee();
        int id = employee.getId();
        JSONObject miniObject = new JSONObject();
        JSONObject dateInterval = new JSONObject();
        String startDateInInterval = date + "T00:00:00";
        dateInterval.put(START_DATE_TIME, startDateInInterval);
        String endDateInInterval = date + "T23:59:59";
        dateInterval.put(END_DATE_TIME, endDateInInterval);
        miniObject.put(DATE_TIME_INTERVAL, dateInterval);
        String uriEnd = makePath(SCHEDULE_REQUESTS);
        List<NameValuePair> pairs = Pairs.newBuilder().calculateConstraints(true).build();
        ScheduleRequestAlias requestType;
        if (type == null) {
            requestType = ScheduleRequestAliasRepository.getAlias(ScheduleRequestType.OFF_TIME);
        } else {
            requestType = ScheduleRequestAliasRepository.getAlias(type);
        }
        miniObject.put(TYPE, requestType.getType());
        miniObject.put("aliasCode", requestType.getOuterId());
        miniObject.put(START_DATE, date);
        miniObject.put(END_DATE, date);
        miniObject.put("daylong", true);
        miniObject.put(STATUS, status.toString());
        miniObject.put(ROSTER_ID_JSON, "null");
        miniObject.put(EMPLOYEE_ID, id);
        miniObject.put(POSITION_ID, employeePosition.getPosition().getId());
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, uriEnd, pairs);
        HttpResponse response = requestMaker(uri, miniObject, RequestBuilder.post(), HAL_JSON);
        LOG.info("Для создания одиночного запроса статуса {} отправлен запрос на адрес: {}", status, uri);
        LOG.info(JSON_LOGGER, miniObject);
        assertStatusCode(response, 201, uri.toString());
        Allure.addAttachment(String.format("Пресет для создания запроса за %s", date),
                             String.format("В расписании не было одиночного запроса, в ходе пресета был создан запрос типа %s для сотрудника %s", requestType.getType(), employeePosition));
        return getCreatedObject(response, ScheduleRequest.class);
    }

    @Step("\"Пресет.\" Проверить у ростера с id {rosterId} текущее значение комментария , если не удовлетворяет условию {value}, то изменить")
    public static Map<String, String> rosterCommentCheck(int rosterId, CommentValue value) {
        String urlEnding = makePath(ROSTERS, rosterId);
        JSONObject someObject = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, urlEnding);
        String version = someObject.get(VERSION).toString();
        String comment;
        try {
            comment = someObject.getString(DESCRIPTION);
        } catch (JSONException e) {
            comment = "";
        }
        if (value == CommentValue.EXIST) {
            if (comment.equals("")) {
                comment = createOrDeleteRosterComment(rosterId, version, CommentValue.EXIST);
            } else {
                Allure.addAttachment("Ростер",
                                     String.format("В данный момент у ростера %s комментарий уже существует, нет необходимости в пересете.",
                                                   version));
            }
        }
        HashMap<String, String> rostersParams = new HashMap<>();
        rostersParams.put(VERSION, version);
        rostersParams.put(COMMENT, comment);
        rostersParams.put(ID, String.valueOf(rosterId));
        return rostersParams;
    }

    /**
     * Пресет.
     * <p>
     * Очищает даты в ячейке расписания для указанной позиции сотрудника, удаляет смены, а затем удаляет все запросы
     *
     * @param employeePosition позиция сотрудника
     * @param dates            даты, которые нужно очистить
     */
    public static void makeClearDate(EmployeePosition employeePosition, LocalDate... dates) {
        makeClearDate(employeePosition, null, dates);
    }

    public static void makeClearDate(EmployeePosition employeePosition, ShiftTimePosition position, LocalDate... dates) {
        for (LocalDate date : dates) {
            changeProperty(SystemProperties.SCHEDULE_BOARD_CHECK_WORKED_DIFF_PLAN, false);
            Shift shift = ShiftRepository.getShift(employeePosition, date, position);
            if (shift != null) {
                deleteRequest(shift);
            }
            int counter = 0;
            while (ShiftRepository.getShift(employeePosition, date, position) != null && counter < 3) {
                LOG.info("Ожидание удаления смены");
                systemSleep(2); //цикл
                counter++;
            }
            presetForEmptyRequestCell(employeePosition.getEmployee(), date);
        }
    }

    /**
     * Удаляет смены из указанного ростера
     *
     * @param roster   ростер, из которого нужно удалить смены
     * @param ep       позиция сотрудника
     * @param interval временной интервал, в течение которого нужно удалить смены
     */
    public static void clearDateForRoster(Roster roster, EmployeePosition ep, DateInterval interval) {
        List<Shift> shifts = ShiftRepository.getShiftsForRoster(roster.getId(), interval).stream()
                .filter(s -> s.getEmployeePositionId().equals(ep.getId())).collect(Collectors.toList());
        for (Shift shift : shifts) {
            if (shift != null) {
                deleteRequest(shift);
            }
            for (LocalDate date : interval.getBetweenDatesList()) {
                presetForEmptyRequestCell(ep.getEmployee(), date);
            }
        }
    }

    /**
     * Удаляет свободные смены на заданную дату
     *
     * @param omId айди подразделения
     * @param date дата, за которую нужно очистить свободные смены
     */
    public static void removeFreeShifts(int omId, LocalDate date) {
        List<Shift> freeShifts = ShiftRepository.getFreeShifts(omId, date);
        for (Shift shift : freeShifts) {
            deleteRequest(shift);
        }
    }

    /**
     * Удаляет смену сотрудника в заданный день в конкретном ростере
     */
    public static void removeShiftFromRoster(Roster roster, EmployeePosition ep, LocalDate date) {
        ShiftRepository.getShiftsForRoster(roster.getId(), new DateInterval())
                .stream()
                .filter(e -> e.getEmployeePositionId() == ep.getId() && e.getDateTimeInterval().getStartDate().equals(date))
                .findFirst().ifPresent(PresetClass::deleteRequest);
    }

    /**
     * Возвращает свободную дату для позиции сотрудника расписания в будущих днях.
     */
    public static LocalDate getFreeDateFromNow(EmployeePosition employeePosition, LocalDate... exceptDate) {
        return getFreeDateForEmployeeShiftPreset(employeePosition, ShiftTimePosition.FUTURE, exceptDate);
    }

    /**
     * Пресет.
     * <p>
     * Проверяет наличие двух свободных дней у сотрудника, с учетом дат исключений
     *
     * @param emp           позиция сотрудника
     * @param timePosition  временной диапазон для поиска
     * @param excludedDates даты исключения
     */
    public static ImmutablePair<LocalDate, LocalDate> twoFreeDaysChecker(EmployeePosition emp, ShiftTimePosition
            timePosition, LocalDate... excludedDates) {
        //берем даты удаляем исключения
        DateInterval dateInterval = timePosition.getShiftsDateInterval();
        List<LocalDate> dateList = dateInterval.subtract(Arrays.asList(excludedDates));
        //берем случайную дату из списка и смотрим если доступные даты завтра, если нет, смотрим на вчерашний день
        LocalDate random = getRandomFromList(dateList);
        LocalDate nextAfterRandom = random.plusDays(1);
        if (!dateList.contains(nextAfterRandom)) {
            nextAfterRandom = random.minusDays(1);
        }
        //ищем смены за найденные даты, если они существуют то удаляем их
        List<Shift> shifts = ShiftRepository.getShifts(emp, timePosition);
        Shift first = shifts.stream().filter(shift -> shift.getDateTimeInterval().getStartDate()
                .isEqual(random)).findFirst().orElse(null);
        LocalDate finalNextAfterRandom = nextAfterRandom;
        Shift second = shifts.stream().filter(shift -> shift.getDateTimeInterval().getStartDate()
                .isEqual(finalNextAfterRandom)).findFirst().orElse(null);
        if (first != null) {
            deleteRequest(first);
        }
        if (second != null) {
            deleteRequest(second);
        }
        //очищаем от запросов
        presetForEmptyRequestCell(emp.getEmployee(), random);
        presetForEmptyRequestCell(emp.getEmployee(), nextAfterRandom);
        if (random.isBefore(nextAfterRandom)) {
            return new ImmutablePair<>(random, nextAfterRandom);
        } else {
            return new ImmutablePair<>(nextAfterRandom, random);
        }
    }

    @Step("\"Пресет.\" Проверить есть ли у ОргЮнита {freeComments} дней без комментариев в промежутке с {dateFrom} до {dateTo} комментарии, если нет, то очистить")
    public static List<String> checkDayComment(LocalDate dateFrom, LocalDate dateTo, int freeComments, int omId) {
        int currentMaximum = LocalDate.now().lengthOfMonth();
        String urlEnding = makePath(ORGANIZATION_UNITS, omId, WORKING_SCHEDULE_DAYS, COMMENTS);
        LOG.info("Ищем в дате с {} по {}, {} дней без комментариев", dateFrom, dateTo, freeComments);
        List<NameValuePair> nameValuePairs = Pairs.newBuilder()
                .from(dateFrom)
                .to(dateTo)
                .size(currentMaximum)
                .build();
        JSONObject someObject = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, urlEnding, nameValuePairs);
        JSONArray eventsList = getJsonArrayFromJsonObject(someObject);
        if (eventsList != null) {
            int size = eventsList.length();
            LOG.info("Количество комментариев до теста: {}", size);
            List<String> comments = new ArrayList<>();
            for (int j = 0; j < size; j++) {
                JSONObject temp = eventsList.getJSONObject(j);
                comments.add(temp.getString(TEXT));
            }
            if (size + freeComments > currentMaximum) {
                for (int i = 0; i < freeComments; i++) {
                    JSONObject anyComment = eventsList.getJSONObject(i);
                    comments.remove(anyComment.getString(TEXT));
                    String link = anyComment.getJSONObject(LINKS).getJSONObject(SELF).get(HREF).toString();
                    deleteRequest(link);
                    Allure.addAttachment("Пресет. ", String.format("В оргЮните оказалось %d комментариев", size));
                }
            } else {
                Allure.addAttachment("Комментарии",
                                     "У оргЮнита было достаточно свободных мест для новых комментариев, всего комментариев сейчас" + size);
            }
            return comments;
        } else {
            LOG.info("В оргюните нет комментариев к дням");
            Allure.addAttachment("Проверка.", "У оргЮнита нет комментариев, пресет не понадобился.");
            return new ArrayList<>();
        }
    }

    /**
     * Пресет. Добавляет коммент к расписанию
     *
     * @param omNumber - номер оргюнита
     * @return хешмап с текстом и датой созданного комментария
     */
    public static Map<String, String> addComment(int omNumber) {
        LocalDate date = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.THAT, LocalDateTools.RANDOM);
        String randomText = RandomStringUtils.randomAlphabetic(10);
        String urlEnding = makePath(ORGANIZATION_UNITS, omNumber, WORKING_SCHEDULE_DAYS, COMMENTS);
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, urlEnding);
        JSONObject miniObject = new JSONObject();
        miniObject.put(TEXT, randomText);
        miniObject.put(DATE, date);
        JSONObject links = new JSONObject();
        links.put(SELF, new JSONObject().put(HREF, setUri(Projects.WFM, CommonRepository.URL_BASE, urlEnding)));
        links.put(USER, new JSONObject().put(HREF, setUri(Projects.WFM, CommonRepository.URL_BASE, ADMINISTRATORS)));
        links.put(Links.ORGANIZATION_UNIT, new JSONObject().put(HREF, setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(ORGANIZATION_UNITS, omNumber))));
        miniObject.put(LINKS, links);
        HttpResponse response = requestMaker(uri, miniObject, RequestBuilder.post(), HAL_JSON);
        LOG.info("Для добавление комментария отправлен запрос на адрес: {}", uri);
        LOG.info(JSON_LOGGER, miniObject);
        assertStatusCode(response, 201, uri.toString());
        HashMap<String, String> temp = new HashMap<>();
        temp.put(TEXT, randomText);
        temp.put(DATE, date.toString());
        Allure.addAttachment("Комментарий",
                             String.format("В оргЮните не было комментариев, поэтому был создан комментарий в пресете: \"%s\", с датой %s",
                                           randomText, date));
        return temp;
    }

    @Step("\"Пресет.\" Удалить дату увольнения сотрудника")
    public static void deleteEmployeeDismissalDate(OrgUnit orgUnit) {
        List<EmployeePosition> employeePositions = EmployeePositionRepository.getEmployeePositionsWithoutFiredEmployees(orgUnit.getId(), LocalDateTools.getLastDate(), false)
                .stream()
                .filter(empPos -> empPos.getEmployee().getEndWorkDate() != null)
                .filter(empPos -> !empPos.getPosition().getName().equals(JobTitleRepository.getJob("Управляющий (Директор магазина)").getFullName()))
                .collect(Collectors.toList());
        if (employeePositions.isEmpty()) {
            Allure.addAttachment("Сотрудники с датой увольнения",
                                 "Ни одного сотрудника с датой увольнения нет");
            return;
        }
        EmployeePosition tempEmployeePosition = getRandomFromList(employeePositions);
        Position tempPosition = tempEmployeePosition.getPosition();
        Employee tempEmployee = tempEmployeePosition.getEmployee();
        JSONObject miniObject = new JSONObject();
        JSONObject links = new JSONObject();
        miniObject.put(LINKS, links);
        JSONObject self = new JSONObject();
        links.put(SELF, self);
        self.put(HREF, tempEmployeePosition.getSelfLink());
        JSONObject employee = new JSONObject();
        links.put(EMPLOYEE_JSON, employee);
        employee.put(HREF, tempEmployee.getSelfLink());
        JSONObject position = new JSONObject();
        links.put(POSITION, position);
        position.put(HREF, tempPosition.getSelfLink());
        JSONObject dateInterval = new JSONObject();
        miniObject.put(DATE_INTERVAL, dateInterval);
        dateInterval.put(START_DATE, tempEmployeePosition.getDateInterval().startDate);
        dateInterval.put(END_DATE, JSONObject.NULL);
        String urlEnding = makePath(EMPLOYEE_POSITIONS, tempEmployeePosition.getId());
        Allure.addAttachment("Сотрудники с датой увольнения",
                             String.format("У сотрудника %s удалена дата окончания работы", tempEmployee));
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, urlEnding);
        HttpResponse response = requestMaker(uri, miniObject, RequestBuilder.put(), HAL_JSON);
        assertStatusCode(response, uri.toString());
    }

    /**
     * Прессет. Увольняет выбранного сотрудника выбранным днём
     */
    public static void dismissEmployee(EmployeePosition employeePosition, LocalDate dateDismissal) {
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(EMPLOYEE_POSITIONS, employeePosition.getId()));
        Position tempPosition = employeePosition.getPosition();
        Employee tempEmployee = employeePosition.getEmployee();
        JSONObject json = new JSONObject();
        JSONObject links = new JSONObject();
        json.put(LINKS, links);
        links.put(SELF, new JSONObject().put(HREF, employeePosition.getSelfLink()));
        links.put(EMPLOYEE_JSON, new JSONObject().put(HREF, tempEmployee.getSelfLink()));
        links.put(POSITION, new JSONObject().put(HREF, tempPosition.getSelfLink()));
        JSONObject dateInterval = new JSONObject();
        json.put(DATE_INTERVAL, dateInterval);
        dateInterval.put(START_DATE, employeePosition.getDateInterval().startDate);
        dateInterval.put(END_DATE, dateDismissal.toString());
        HttpResponse response = requestMaker(uri, json, RequestBuilder.put(), HAL_JSON);
        LOG.info(JSON_LOGGER, json);
        assertStatusCode(response, 200, uri.toString());
        Allure.addAttachment("Увольнение", String.format("Сотрудник \"%s\" уволен %s", tempEmployee, dateDismissal));
    }

    /**
     * Пресет. Создает очищенный параметр без значений
     *
     * @return название очищенного параметра
     */
    public static String makeClearParam(OrgUnit orgUnit) {
        String targetParamName = (wfm.components.analytics.ParamName.values()[RANDOM
                .nextInt(ParamName.values().length)]).getName();
        LOG.info("Для очищения выбран параметр {}", targetParamName);
        JSONArray jsonArrayMathParameterValues = CommonRepository.getMathParameterValuesArray(orgUnit.getId());
        String selfUrl = null;
        for (int i = 0; i < jsonArrayMathParameterValues.length(); i++) {
            JSONObject tempObj = jsonArrayMathParameterValues.getJSONObject(i);
            Object tempSelfHref = tempObj.getJSONObject(LINKS).getJSONObject(SELF).get(HREF);
            String tempParam = tempObj.getJSONObject(LINKS).getJSONObject(MATH_PARAMETER).getString(HREF);
            URI valueParam = URI.create(tempParam);
            JSONObject someObjectKpiValues1 = new JSONObject(setUrlAndInitiateForApi(valueParam, Projects.WFM));
            Object paramName = someObjectKpiValues1.get(COMMON_NAME);
            String name = String.valueOf(paramName);
            selfUrl = String.valueOf(tempSelfHref);
            if (name.equals(targetParamName)) {
                break;
            }
        }
        LOG.info("Запрос на удаление параметров отправлена на: {}", selfUrl);
        deleteRequest(selfUrl);
        return targetParamName;
    }

    /**
     * Меняет состояние поля availableForCalculation
     *
     * @param makeAvailableForCalculation - true, если нужно включить юнит в расчет
     * @param orgUnit                     - юнит, у которого нужно изменить значение поля
     */
    public static void changeAvailabilityForCalculation(OrgUnit orgUnit, boolean makeAvailableForCalculation) {
        String urlEnding = makePath(ORGANIZATION_UNITS, orgUnit.getId());
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, urlEnding);
        OrgUnit newParent = orgUnit.getParentOrgUnit();
        JSONObject miniObject = new JSONObject();
        JSONObject links = new JSONObject();
        JSONObject self = new JSONObject();
        final String self1 = orgUnit.getLinks().get(SELF);
        self.put(HREF, self1);
        JSONObject parentLink = new JSONObject();
        final String newParentSelf = newParent.getLinks().get(SELF);
        parentLink.put(HREF, newParentSelf);
        links.put(SELF, self);
        links.put(PARENT, parentLink);
        miniObject.put(LINKS, links);
        miniObject.put(ID, orgUnit.getId());
        miniObject.put(NAME, orgUnit.getName());
        miniObject.put(OUTER_ID, orgUnit.getOuterId());
        miniObject.put(TAGS, orgUnit.getTags());
        miniObject.put(DATE_FROM, orgUnit.getDateInterval().getStartDate());
        final LocalDate endDate = orgUnit.getDateInterval().getEndDate();
        miniObject.put(DATE_TO, endDate == null ? JSONObject.NULL : endDate);
        miniObject.put(EMAIL, orgUnit.getEmail());
        miniObject.put(AVAILABLE_FOR_CALCULATION, makeAvailableForCalculation);
        miniObject.put(ACTIVE, true);
        miniObject.put(ORG_UNIT_TYPE_ID, orgUnit.getOrganizationUnitTypeId());

        JSONObject parentObject = new JSONObject();
        JSONObject linksParent = new JSONObject();
        JSONObject selfParent = new JSONObject();
        selfParent.put(HREF, newParentSelf);
        linksParent.put(SELF, selfParent);
        parentObject.put(LINKS, linksParent);
        parentObject.put(ID, newParent.getId());
        miniObject.put(PARENT, parentObject);

        HttpResponse response = requestMaker(uri, miniObject, RequestBuilder.put(), HAL_JSON);
        LOG.info("Запрос на добавление тегов для: {}", orgUnit);
        LOG.info(JSON_LOGGER, miniObject);
        assertStatusCode(response, uri.toString());
    }

    /**
     * Добавляет теги к ростеру
     *
     * @param tags    - список тегов
     * @param orgUnit - название оргюнита
     */
    public static void doPostRequestAddTags(List<String> tags, OrgUnit orgUnit) {
        String urlEnding = makePath(ORGANIZATION_UNITS, TAGS, orgUnit.getId());
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, urlEnding);
        OrgUnit newParent = orgUnit.getParentOrgUnit();
        JSONObject miniObject = new JSONObject();
        JSONObject links = new JSONObject();
        JSONObject self = new JSONObject();
        final String self1 = orgUnit.getLinks().get(SELF);
        self.put(HREF, self1);
        JSONObject parentLink = new JSONObject();
        final String newParentSelf = newParent.getLinks().get(SELF);
        parentLink.put(HREF, newParentSelf);
        links.put(SELF, self);
        links.put(PARENT, parentLink);
        miniObject.put(LINKS, links);
        miniObject.put(ID, orgUnit.getId());
        miniObject.put(NAME, orgUnit.getName());
        miniObject.put(TAGS, String.join(", ", tags));
        miniObject.put(DATE_FROM, orgUnit.getDateInterval().getStartDate());
        final LocalDate endDate = orgUnit.getDateInterval().getEndDate();
        miniObject.put(DATE_TO, endDate == null ? JSONObject.NULL : endDate);
        miniObject.put(EMAIL, orgUnit.getEmail());
        miniObject.put(AVAILABLE_FOR_CALCULATION, orgUnit.isAvailableForCalculation());
        miniObject.put(ACTIVE, true);
        miniObject.put(ORG_UNIT_TYPE_ID, orgUnit.getOrganizationUnitTypeId());

        JSONObject parentObject = new JSONObject();
        JSONObject linksParent = new JSONObject();
        JSONObject selfParent = new JSONObject();
        selfParent.put(HREF, newParentSelf);
        linksParent.put(SELF, selfParent);
        parentObject.put(LINKS, linksParent);
        parentObject.put(ID, newParent.getId());
        miniObject.put(PARENT, parentObject);

        HttpResponse response = requestMaker(uri, miniObject, RequestBuilder.put(), HAL_JSON);
        LOG.info("Запрос на добавление тегов для: {}", orgUnit);
        LOG.info(JSON_LOGGER, miniObject);
        assertStatusCode(response, uri.toString());
    }

    /**
     * Изменения даты и времени смены в апи
     *
     * @param days       какой день для изменения, выходной, рабочий или не указано
     * @param scheduleId номер оргюнита
     * @param isoWeekDay будний или выходной день
     * @param dayId      айди смены, которую будем изменять
     */
    public static void apiChangeDay(Days days, String scheduleId, int isoWeekDay, String dayId) {
        String urlEnding = makePath(BUSINESS_HOURS_LIST, scheduleId, DAYS);
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, urlEnding);
        JSONObject miniObject = new JSONObject();
        HttpUriRequest some = null;
        int expectedCode = -1;
        if (days.equals(Days.DAY_OFF)) {
            LOG.info(dayId);
            some = RequestBuilder.delete(uri + "/" + dayId)
                    .build();
            expectedCode = 200;
        } else if (days.equals(Days.DAY)) {
            JSONObject dataIntervalJson = new JSONObject();
            DayOfWeek tempDay = DayOfWeek.of(isoWeekDay);
            LocalTime startDate = LocalTime.parse("09:00:00");
            LocalTime endDate = LocalTime.parse("23:00:00");
            dataIntervalJson.put(START_TIME, startDate);
            dataIntervalJson.put(END_TIME, endDate);
            miniObject.put("dayOfWeek", tempDay);
            miniObject.put(ISO_WEEK_DAY, isoWeekDay);
            miniObject.put(TIME_INTERVAL, dataIntervalJson);
            StringEntity requestEntity = new StringEntity(miniObject.toString(), HAL_JSON);
            some = RequestBuilder.post()
                    .setUri(uri)
                    .setEntity(requestEntity)
                    .build();
            expectedCode = 201;
        }
        HttpResponse response = null;
        try {
            response = ClientReturners.httpClientReturner(Projects.WFM).execute(some);
        } catch (IOException e) {
            e.printStackTrace();
        }
        LOG.info("Запрос на изменение дня типа {}, день недели {}, ID дня: {}", days.getNameOfDay(), isoWeekDay, dayId);
        LOG.info(JSON_LOGGER, miniObject);
        assertStatusCode(Objects.requireNonNull(response), expectedCode, uri != null ? uri.toString() : null);
    }

    /**
     * Берем объект кастомной роли, проверяем существует ли с именем таким же как в кастомной роли, если нет - создаем новую,
     * Потом проверяем на наличие всех разрешений, айдишники для пермишенов берутся динамично.
     * Последним шагом ищем человека с этой ролью, если такого нет, то берем случайного и присваеваем ему роль
     *
     * @param role - Кастомная роль, определенная в енаме
     */
    public static User addCustomRoleToUser(Role role, String tempPassword, LocalDate endDate, OrgUnit... unit) {
        User randomUser = addRoleToRandomUser(role, endDate, unit);
        changePassword(randomUser, tempPassword);
        randomUser.setPassword(tempPassword);
        return randomUser;
    }

    /**
     * Вариация меотда выше, но подразделение, для которого будет применена роль,
     * будет взято из тех, где работает сотрудник
     */
    public static User addCustomRoleToUser(Role role, String tempPassword, LocalDate endDate) {
        User randomUser = addRoleToRandomUser(role, endDate);
        changePassword(randomUser, tempPassword);
        randomUser.setPassword(tempPassword);
        return randomUser;
    }

    /**
     * Берем объект кастомной роли, проверяем существует ли с именем таким же как в кастомной роли, если нет - создаем новую,
     * Потом проверяем на наличие всех разрешений, айдишники для пермишенов берутся динамично.
     * Последним шагом берем конкретного пользователя и наделяем его этой ролью для указанного оргюнита
     *
     * @param role - Кастомная роль, определенная в енаме или рукотворная при помощи createCustomPermissionRole
     */
    public static User addCustomRoleToUser(Role role, String tempPassword, OrgUnit unit, LocalDate endDate, User
            user) {
        User targetUser = addRoleToTargetUser(role, unit, endDate, user);
        changePassword(targetUser, tempPassword);
        targetUser.setPassword(tempPassword);
        return targetUser;
    }

    /**
     * Присваивает конкретному пользователю кастомную роль с указанными разрешениями
     *
     * @param permissions список разрешений
     * @param user        пользователь
     * @param unit        подразделение, для которого нужно разрешение
     */
    public static User givePermissionsToTargetUser(List<PermissionType> permissions, User user, OrgUnit unit) {
        Role role = PresetClass.createCustomPermissionRole(permissions);
        return PresetClass.addCustomRoleToUser(role, RandomStringUtils.randomAlphanumeric(10), unit, null, user);
    }

    /**
     * Для удаления смены, если она есть в конкретный день и очистка от запросов сотрудника (владельца смены)
     *
     * @param month - номер месяца в году
     */
    public static void checkEmptyCellAndPreset(EmployeePosition position, LocalDate date, int month) {
        LocalDate current = LocalDate.now();
        if (month == current.getMonthValue()) {
            Shift shift = ShiftRepository.getShift(position, date, null);
            if (shift != null) {
                deleteRequest(shift);
            }
        }
        presetForEmptyRequestCell(position.getEmployee(), date);
    }

    /**
     * Формирование JSON объекта для публикации на определенную дату и отправка POST запроса на публикацию
     *
     * @param uri - ссылка для публикации FTE или KPI
     */
    static void kpiAndFtePreset(URI uri) {
        JSONObject miniObject = new JSONObject();
        miniObject.put(MONTH, LocalDateTools.getFirstDate());
        miniObject.put(PUBLISHED, true);
        HttpResponse response = requestMaker(uri, miniObject, RequestBuilder.post(), HAL_JSON);
        LOG.info("На адрес {} был отправлен запрос на изменение:\n {}", uri, miniObject);
        try {
            assertStatusCode(response, uri.toString());
        } catch (AssertionError error) {
            LOG.info("Не удалось сделать запрос, ошибка: {}", response.getStatusLine().getStatusCode());
        }

    }

    /**
     * Проверяет статус публикации активного ростера за текущий месяц, если он не опубликован, то публикует его
     *
     * @param omNumber - номер оргюнита
     */
    public static void checkAndMakePublicationRoster(int omNumber) {
        Roster roster = RosterRepository.getActiveRosterThisMonth(omNumber);
        if (!roster.isPublished()) {
            makeRosterPublication(roster.getId());
        }
    }

    /**
     * Из списка всех ростеров для ОМ на текущий месяц берем активный, и проверяем что он опубликован.
     * Если он опубликован, то создаем смену для сотрудника у которого нет длинных шедулереквестов и
     * есть позиция в текущем ОМ до конца месяца.
     *
     * @param omId - id текущего ОМ
     */
    public static void nonPublishChecker(int omId) {
        if (RosterRepository.getActiveRosterThisMonth(omId).isPublished()) {
            presetForMakeShift(EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true),
                               false, ShiftTimePosition.FUTURE);
        }
        Allure.addAttachment("Подготовка ростера", "В ростер была добавлена смена, чтобы изменить его статус на \"Не опубликован\"");
    }

    /**
     * Отправляет запрос в апи на создание события
     *
     * @param eventIsRepeatable тип создаваемого события, повторяемое или не повторяемое
     * @param omId              ID оргюнита
     */
    public static void presetEvent(int omId, boolean eventIsRepeatable) {
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, ORGANIZATION_UNIT_EVENTS);
        JSONObject miniObject = miniObjectForAddEventPreset(omId, eventIsRepeatable);
        HttpResponse response = requestMaker(uri, miniObject, RequestBuilder.post(), HAL_JSON);
        assertStatusCode(response, 201, uri.toString());
        LOG.info("Запрос на создание события: {}", miniObject);
    }

    /**
     * Формирует JSONObject объект для отправки запроса создания события
     *
     * @param eventIsRepeatable - тип создаваемого события, повторяемое или не повторяемое
     * @param omId              - ID оргюнита
     * @return JSONObject для запроса на создание события
     */
    private static JSONObject miniObjectForAddEventPreset(int omId, boolean eventIsRepeatable) {
        JSONObject miniObject = new JSONObject();
        LocalDateTime startDateTime = LocalDateTime.now().plusDays(1).withHour(8);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        LocalDateTime endDateTime = startDateTime.plusHours(10);
        miniObject.put(VALUE, "10");
        JSONObject dataIntervalJson = new JSONObject();
        dataIntervalJson.put(START_DATE_TIME, startDateTime.format(formatter));
        dataIntervalJson.put(END_DATE_TIME, endDateTime.format(formatter));
        miniObject.put(DATE_TIME_INTERVAL, dataIntervalJson);
        if (eventIsRepeatable) {
            return settingsForRepeatEvent(miniObject, omId, new DateTimeInterval(startDateTime, endDateTime).toDateInterval());
        } else {
            return settingsForNonRepeatEvent(miniObject, omId);
        }
    }

    /**
     * Формирует часть запроса для неповторяемого события
     *
     * @param miniObject - общие для событий параметры
     * @param omId       - ID оргюнита
     * @return JSONObject для формироавания полного запроса
     */
    private static JSONObject settingsForNonRepeatEvent(JSONObject miniObject, int omId) {
        String outerId = OrgUnitRepository.getOrgUnit(omId).getOuterId();
        miniObject.put(OUTER_ID, outerId);
        miniObject.put(REPEAT_RULE, (Object) null);
        JSONObject links = new JSONObject();
        JSONObject self = new JSONObject();
        self.put(HREF, setUri(Projects.WFM, CommonRepository.URL_BASE, ORGANIZATION_UNIT_EVENTS));
        links.put(SELF, self);
        JSONObject orgUnit = new JSONObject();
        orgUnit.put(HREF, setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(ORGANIZATION_UNITS, omId)));
        links.put(ORG_UNIT_JSON, orgUnit);
        JSONObject organizationUnitEventType = new JSONObject();
        organizationUnitEventType.put(HREF, setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(ORGANIZATION_UNIT_EVENT_TYPES, 2)));
        links.put(ORG_UNIT_EVENT_TYPE, organizationUnitEventType);
        miniObject.put(LINKS, links);
        miniObject.put(ID, (Object) null);
        return miniObject;
    }

    /**
     * Формирует часть запроса для повторяемого события
     *
     * @param miniObject - общие для событий параметры
     * @param omId       - ID оргюнита
     * @return JSONObject для формирования полного запроса
     */
    private static JSONObject settingsForRepeatEvent(JSONObject miniObject, int omId, DateInterval dateInterval) {
        JSONObject links = new JSONObject();
        JSONObject orgUnit = new JSONObject();
        orgUnit.put(HREF, setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(ORGANIZATION_UNITS, omId)));
        links.put(ORG_UNIT_JSON, orgUnit);
        JSONObject self = new JSONObject();
        self.put(HREF, setUri(Projects.WFM, CommonRepository.URL_BASE, ORGANIZATION_UNIT_EVENTS));
        links.put(SELF, self);
        JSONObject organizationUnitEventType = new JSONObject();
        organizationUnitEventType.put(HREF, setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(ORGANIZATION_UNIT_EVENT_TYPES, 1)));
        links.put(ORG_UNIT_EVENT_TYPE, organizationUnitEventType);
        miniObject.put(LINKS, links);
        JSONObject type = new JSONObject();
        String name = "Поставка";
        type.put("duringPeriod", name);
        type.put(SHORT_NAME, name);
        type.put(SHORT_NAME, name);
        type.put(OUTER_ID, "supply");
        JSONObject links1 = new JSONObject();
        JSONObject self1 = new JSONObject();
        self1.put(HREF, setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(ORGANIZATION_UNIT_EVENT_TYPES, 1)));
        links1.put(SELF, self1);
        JSONObject measureUnit = new JSONObject();
        measureUnit.put(HREF, setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(MEASURE_UNITS, 1)));
        links1.put("measureUnit", measureUnit);
        JSONObject organizationUnitEvents = new JSONObject();
        organizationUnitEvents.put(HREF, setUri(Projects.WFM, CommonRepository.URL_BASE,
                                                makePath(ORGANIZATION_UNIT_EVENT_TYPES, 1, ORGANIZATION_UNIT_EVENTS)));
        links1.put("organizationUnitEvents", organizationUnitEvents);
        type.put(LINKS, links1);
        type.put(ID, 1);
        miniObject.put(TYPE, type);
        miniObject.put("units", (Object) null);
        JSONObject repeatRule = new JSONObject();
        repeatRule.put("periodicity", "WEEKLY");
        repeatRule.put(NAME, "Еженедельно");
        repeatRule.put(END_DATE, dateInterval.endDate.plusDays(10));
        repeatRule.put("custom", false);
        repeatRule.put(START_DATE, dateInterval.startDate);
        miniObject.put(REPEAT_RULE, repeatRule);
        miniObject.put(LOCAL, true);
        return miniObject;
    }

    /**
     * Создает или удаляет один комментарий к ростеру
     *
     * @param rosterId      - ID ростера
     * @param rosterVersion - номер версии ростера
     * @param value         - выбор создание или удаления комментария
     *                      EMPTY - удаление
     *                      EXIST - создание
     * @return случайно созданную строку из 8 символов, необходимо при создании комментария
     */
    public static String createOrDeleteRosterComment(int rosterId, String rosterVersion, CommentValue value) {
        String urlEnding = makePath(ROSTERS, rosterId);
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, urlEnding);
        JSONObject object = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, urlEnding);
        String randomText = RandomStringUtils.randomAlphabetic(8);
        String forAllure = "";
        if (value.equals(CommentValue.EMPTY)) {
            object.put(DESCRIPTION, "");
            forAllure = "удален";
        } else if (value.equals(CommentValue.EXIST)) {
            object.put(DESCRIPTION, randomText);
            forAllure = "создан";
            Allure.addAttachment("Создание комментария",
                                 String.format("У версии графика №%s был создан комментарий \"%s\" в пресете.", rosterVersion, randomText));
        }
        HttpResponse response = requestMaker(uri, object, RequestBuilder.put(), ContentType.APPLICATION_JSON);
        Allure.addAttachment(forAllure + "ие комментария",
                             String.format("У версии графика: %s был %s комментарий в пресете.", rosterVersion, forAllure));
        LOG.info("Запрос на {}ие комментария к ростеру по адресу: {}", forAllure, uri);
        assertStatusCode(response, uri.toString());
        return randomText;
    }

    public static void rosterPublishCheck(int rosterId) {
        String urlEnding = makePath(ROSTERS, rosterId);
        JSONObject someObject = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, urlEnding);
        boolean bool = someObject.getBoolean(PUBLISHED);
        LOG.info("Статус публикации ростера № {}: {}", rosterId, bool);
        if (!bool) {
            makeRosterPublication(rosterId);
        } else {
            Allure.addAttachment("Публикация графика",
                                 "В ходе пресета не был опубликован активный график, т.к. он уже был опубликован");
        }
    }

    @Step("Пресет. Проверить, что текущее количество навыков у сотрудника совпадает с условием")
    public static int addOrDeleteSkills(int employeeId, boolean hasSkills) {
        String urlEnding = makePath(EMPLOYEES, employeeId, SKILL_VALUES);
        int size = CommonRepository.checkValueOfSkill(employeeId);
        HttpResponse response;
        if (!hasSkills) {
            if (size != 0) {
                URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, urlEnding);
                for (int i = 1; i < 4; i++) {
                    deleteRequest(makePath(uri, i));
                }
                size = 0;
                Allure.addAttachment("Удаление навыков", "В ходе пресета были удалены все навыки");
            } else {
                Allure.addAttachment("Удаление навыков", "Пресет не понадобился, т. к. у сотрудника и так нет навыков");
            }
        } else {
            if (size == 0) {
                JSONObject miniObject = new JSONObject();
                miniObject.put(EMBEDDED, "null");
                miniObject.put("_page", "null");
                miniObject.put(VALUE, "true");
                miniObject.put(TYPE, "BOOLEAN");
                miniObject.put(NAME, "Административная должность");
                miniObject.put("skillMathType", "MASTER");
                miniObject.put(OUTER_ID, "MASTER");
                miniObject.put(STATUS, "true");
                miniObject.put(LOCAL, "true");
                URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(urlEnding, 3));
                response = requestMaker(uri, miniObject, RequestBuilder.put(), ContentType.APPLICATION_JSON);
                size = 1;
                LOG.info("В ходе пресета был добавлен 1 навык");
                Allure.addAttachment("Добавление навыка", "В ходе пресета был добавлен 1 навык");
                assertStatusCode(response, uri.toString());
            } else if (size == 3) {
                URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, urlEnding);
                deleteRequest((makePath(uri, (1 + RANDOM.nextInt(3)))));
                LOG.info("Навык был удален, чтобы осталось 2 навыка");
                size = 2;
            } else {
                Allure.addAttachment("Добавление навыка", "У данного сотрудника и так 1 или 2 навыка");
            }
        }
        return size;
    }

    /**
     * Отправляет запрос в апи на создание E-mail адреса
     *
     * @param employee     сотрудник
     * @param emailAddress адрес, который будет создан
     */
    public static void makeEmailInApi(Employee employee, String emailAddress) {
        String urlEnding = makePath(EMPLOYEES, employee.getId());
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, urlEnding);
        JSONObject miniObject = new JSONObject();
        miniObject.put(EMAIL, emailAddress);
        HttpResponse response = requestMaker(uri, miniObject, RequestBuilder.patch(), HAL_JSON);
        LOG.info("Выполнение запроса для создание email-адреса для сотрудника {}", employee);
        Allure.addAttachment("Добавление email", String.format("Сотруднику %s добавлен email %s", employee, emailAddress));
        assertStatusCode(response, uri.toString());
    }

    /**
     * Отправляет запрос в апи на добавление мобильного телефона
     *
     * @param employee ID сотрудника
     * @param type     тип номера
     */
    public static void addPhone(Employee employee, PhoneTypes type) {
        String phone = 7 + RandomStringUtils.randomNumeric(10);
        String urlEnding = makePath(EMPLOYEES, employee.getId(), PHONES);
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, urlEnding);
        PhoneType phoneType = CommonRepository.getPhoneTypes()
                .stream()
                .filter(pt -> pt.getName().equals(type.getPhoneName()))
                .collect(randomItem());
        JSONObject miniObject = new JSONObject();
        miniObject.put("number", phone);
        try {
            miniObject.put(TYPE, new JSONObject(new ObjectMapper().writeValueAsString(phoneType)).put(NAME, phoneType.getName()));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        JSONObject links = new JSONObject();
        links.put(REL_PHONE_TYPE, phoneType.getLinkWrappedInJson(SELF));
        miniObject.put(LINKS, links);
        HttpResponse response = requestMaker(uri, miniObject, RequestBuilder.post(), HAL_JSON);
        LOG.info("Выполнение запроса для создание телефона для сотрудника {}", employee);
        Allure.addAttachment("Добавление номера телефона", String.format("Сотруднику %s добавлен телефон %s", employee, phone));
        assertStatusCode(response, 201, uri.toString());
    }

    /**
     * Осуществляет проверку наличия FTE и KPI (с типом 1 и 4),
     * в случае отсутствия вызывает метод для публикации данных на текущий месяц
     *
     * @param omId - id текущего ОМ
     */
    public static void kpiAndFteChecker(int omId) {
        LocalDate strDate = LocalDateTools.getFirstDate();
        //fte
        String urlEnding = makePath(ORGANIZATION_UNITS, omId, PUBLISHED_FTE_LISTS, strDate);
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, urlEnding);
        JSONObject fteObject = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, urlEnding);
        boolean fteStatus = fteObject.getBoolean(PUBLISHED);
        if (!fteStatus) {
            kpiAndFtePreset(uri);
        }
        /*
        urlEnding = makePath(ORGANIZATION_UNITS, omId, PUBLISHED_FORECASTS, 1, strDate);
        uri = setUri(Projects.WFM, CommonRepository.URL_BASE, urlEnding);
        JSONObject kpiObject = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, urlEnding);
        boolean kpiStatus = kpiObject.getBoolean(PUBLISHED);
        if (!kpiStatus) {
            kpiAndFtePreset(uri);
        }
        try {
            urlEnding = makePath(ORGANIZATION_UNITS, omId, PUBLISHED_FORECASTS, 4, strDate);
            uri = setUri(Projects.WFM, CommonRepository.URL_BASE, urlEnding);
            JSONObject kpiObject4 = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, urlEnding);
            boolean kpiStatus4 = kpiObject4.getBoolean(PUBLISHED);
            if (!kpiStatus4) {
                kpiAndFtePreset(uri);
            }
        } catch (AssertionError e) {
            LOG.info("В ходе пресета невозможно было исправить текущий статус kpi по количеству чеков");
        }
        */
    }

    /**
     * Отправляет запрос в апи на то чтобы он стал активным по его ID номеру
     */
    public static void makeRosterActive(int rosterID) {
        String urlEnding = makePath(ROSTERS, rosterID);
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, urlEnding);
        JSONObject miniObject = new JSONObject();
        miniObject.put(ACTIVE, true);
        HttpResponse response = requestMaker(uri, miniObject, RequestBuilder.put(), HAL_JSON);
        LOG.info("Пресет на активацию графика ростера № {}", rosterID);
        LOG.info(REQUEST_LOGGER, uri);
        assertStatusCode(response, uri.toString());
        Allure.addAttachment("Пресет активации графика",
                             "При помощи пресета был был активирован график активного ростера № " + rosterID);
    }

    /**
     * Отправляет запрос в апи на публикацию ростера по его ID номеру
     *
     * @param rosterID - ID ростера для публикации
     */
    public static void makeRosterPublication(int rosterID) {
        String urlEnding = makePath(ROSTERS, rosterID, PUBLISH);
        NameValuePair pair = newNameValue("force", false);
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, urlEnding, Collections.singletonList(pair));
        JSONObject miniObject = new JSONObject();
        miniObject.put(ACTIVE, true);
        miniObject.put(PUBLISHED, true);
        HttpResponse response = requestMaker(uri, miniObject, RequestBuilder.put(), HAL_JSON);
        assertStatusCode(response, uri.toString());
        checkRosterActionSuccess(response);
        LOG.info("Пресет на публикацию графика активного ростера оргюнита.");
        LOG.info("Запрос по адресу {} с телом {}", uri, miniObject);
        Allure.addAttachment("Пресет публикации графика",
                             "При помощи пресета был опубликован график активного ростера.");
    }

    /**
     * Пресет.
     * <p>
     * Ищет свободные даты для позиции сотрудника и временном диапазоне, так же есть дата которая не будет браться
     * для очистки или как свободная, выбирает случайную дату из доступных и очищает ее.
     *
     * @param employeePosition позиция сотрудника для поиска
     * @param timePosition     в каком промежутке ищем дату
     * @param exceptDate       даты исключения
     */
    public static LocalDate getFreeDateForEmployeeShiftPreset(EmployeePosition employeePosition,
                                                              ShiftTimePosition timePosition, LocalDate... exceptDate) {
        DateInterval dateInterval = timePosition.getShiftsDateInterval();
        List<LocalDate> betweenDatesList = dateInterval.getBetweenDatesList();
        betweenDatesList.removeAll(Arrays.asList(exceptDate));
        if (betweenDatesList.isEmpty()) {
            throw new AssertionError(NO_VALID_DATE + "В текущем месяце нет подходящих дат");
        }
        LocalDate random = getRandomFromList(betweenDatesList);
        makeClearDate(employeePosition, random);
        return random;
    }

    public static LocalDate getFreeDateForEmployeeShiftPreset(EmployeePosition employeePosition, ShiftTimePosition timePosition, DateInterval interval) {
        return getFreeDateForEmployeeShiftPreset(employeePosition, timePosition, interval, new ArrayList<>());
    }

    public static LocalDate getFreeDateForEmployeeShiftPreset(EmployeePosition employeePosition, ShiftTimePosition timePosition,
                                                              DateInterval interval, List<LocalDate> exceptDates) {
        List<LocalDate> betweenDatesList = interval.getBetweenDatesList();
        List<LocalDate> datesInPosition = timePosition.getShiftsDateInterval().getBetweenDatesList();
        betweenDatesList.retainAll(datesInPosition);
        betweenDatesList.removeAll(exceptDates);
        if (betweenDatesList.isEmpty()) {
            throw new AssertionError(NO_VALID_DATE + "В текущем месяце нет подходящих дат");
        }
        LocalDate random = getRandomFromList(betweenDatesList);
        ShiftRepository.getShifts(employeePosition, timePosition)
                .stream()
                .filter(s -> s.getEmployeePositionId() == employeePosition.getId()
                        && s.getDateTimeInterval().getStartDate().equals(random))
                .findAny()
                .ifPresent(PresetClass::deleteRequest);
        PresetClass.presetForEmptyRequestCell(employeePosition.getEmployee(), random);
        return random;
    }

    /**
     * Возвращает свободную дату для позиции сотрудника расписания в зависимости от того какая дата месяца.
     */
    public static LocalDate getFreeDateForEmployeeShiftPreset(EmployeePosition employeePosition, LocalDate...
            exceptDate) {
        return getFreeDateForEmployeeShiftPreset(employeePosition, ShiftTimePosition.DEFAULT, exceptDate);
    }

    /**
     * Отправляет запрос в апи на изменение типа расписания
     *
     * @param type       - тип создаваемого расписания SALE/SERVICE
     * @param scheduleId - ID расписания для изменения
     */
    public static void preChangeType(ScheduleType type, Integer scheduleId) {
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, BUSINESS_HOURS_LIST);
        JSONObject miniObject = new JSONObject();
        JSONObject dataIntervalJson = new JSONObject();
        dataIntervalJson.put(END_DATE, "2110-10-10");
        dataIntervalJson.put(START_DATE, "2110-10-10");
        miniObject.put(TYPE, type.getNameOfType());
        miniObject.put(DATE_INTERVAL, dataIntervalJson);
        JSONObject links = new JSONObject();
        links.put(ORG_UNIT_JSON, setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(ORGANIZATION_UNITS, scheduleId)));
        miniObject.put(LINKS, links);
        HttpResponse response = requestMaker(uri, miniObject, RequestBuilder.post(), HAL_JSON);
        LOG.info("Запрос на создание расписания: {}", miniObject);
        assertStatusCode(response, 201, uri.toString());
    }

    /**
     * Возвращает пару распознания, где левая часть - айди сотрудника, а правая - айди распознавания
     */
    public static ImmutablePair<Integer, Integer> recognizeBioPreset() {
        URI uri = setUri(Projects.BIO, INTEGRATION_BIO_URL, "recognition");
        JSONObject miniObject = new JSONObject();
        File folder = new File("src/test/resources/img/");
        File[] files = folder.listFiles();
        miniObject.put("image", encoder(files != null ? files[RANDOM.nextInt(files.length)] : new File("")));
        miniObject.put("tolerance", 0.515);
        miniObject.put("zoneOffset", 18000);
        miniObject.put("purpose", "RECORD");
        miniObject.put("method", "SIMPLE");
        HttpResponse response = requestMaker(uri, miniObject, RequestBuilder.post(), HAL_JSON, Projects.BIO);
        assertStatusCode(response, uri.toString());
        JSONObject responseMessage = new JSONObject();
        try {
            responseMessage = new JSONObject(EntityUtils.toString(response.getEntity()));
        } catch (IOException e) {
            LOG.error("Не был получен ответ ввиде JSON объекта");
        }
        String personName = responseMessage.optString("personName");
        LOG.info("Распознан: {}", personName);
        Allure.addAttachment("Распознавание",
                             String.format("Сотрудник: %s%nВремя: %s", personName, LocalDateTime.now()));
        int id = responseMessage.optInt("personId");
        if (id != 0) {
            return new ImmutablePair<>(id, responseMessage.optInt("recordId"));
        }
        Assert.fail("Не удалось никого распознать");
        return null;
    }

    @Step("\"Пресет.\" Проверить есть ли у ОргЮнита комментарии в промежутке с {dateInterval.startDate}" +
            " до {dateInterval.endDate} комментарии, если нет, то добавить")
    public static Map<String, String> getDayCommentPreset(DateInterval dateInterval, OrgUnit orgUnit) {
        String urlEnding = makePath(ORGANIZATION_UNITS, orgUnit.getId(), WORKING_SCHEDULE_DAYS, COMMENTS);
        List<NameValuePair> nameValuePairs = Pairs.newBuilder()
                .from(dateInterval.startDate)
                .to(dateInterval.endDate)
                .build();
        JSONObject someObject = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, urlEnding, nameValuePairs);
        JSONArray eventsList = getJsonArrayFromJsonObject(someObject);
        if (eventsList != null) {
            JSONObject anyComment = eventsList.getJSONObject(RANDOM.nextInt(eventsList.length()));
            String date = anyComment.get(DATE).toString();
            String text = anyComment.getString(TEXT);
            HashMap<String, String> getDayComment = new HashMap<>();
            getDayComment.put(DATE, date);
            getDayComment.put(TEXT, text);
            Allure.addAttachment("Комментарии",
                                 String.format("У оргЮнита уже был комментарий. Выбран комментарий с датой %s, и текстом \"%s\"",
                                               date, text));
            return getDayComment;
        } else {
            return addComment(orgUnit.getId());
        }
    }

    /**
     * Очищает 3 мат параметра, метод пресета взамен ПНА-6.3.4.2
     */
    public static void clearMathParameters(OrgUnit orgUnit) {
        List<MathParameter> mathParameters = MathParameterRepository.getMathParametersByOrgUnit(orgUnit);
        List<String> parametersName = new ArrayList<>();
        parametersName.add("Конверсия для расчета РЗ");
        parametersName.add("Производительность (операции)");
        parametersName.add("Производительность (трафик)");
        List<MathParameter> mathParametersForDelete = mathParameters.stream()
                .filter(mp -> parametersName.contains(mp.getName())).collect(Collectors.toList());
        if (!mathParametersForDelete.isEmpty()) {
            List<String> nameParameters = mathParametersForDelete.stream().map(MathParameter::getName).collect(Collectors.toList());
            mathParametersForDelete.forEach(mp -> deleteMathParameterById(mp.getMathParameterId(), orgUnit.getId()));
            Allure.addAttachment("Очистка мат параметров",
                                 String.format("В результате действия пресета у оргюнита \"%s\" были удалены мат параметры с названиями: %s",
                                               orgUnit.getName(), String.join(", ", nameParameters)));
        }
    }

    /**
     * Удаляет мат параметр у оргюнита
     *
     * @param mathParamId - айди мат параметра
     * @param orgID       - айди оргюнита
     */
    private static void deleteMathParameterById(int mathParamId, int orgID) {
        URI uriPreset = setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(ORG_UNITS, orgID, MATH_PARAMETER_VALUES, mathParamId));
        deleteRequest(uriPreset);
    }

    /**
     * Проверяем если у выбранного оргюнита сообщение об ошибке по выбранному параметру, если нет, создаем нужную ошибку
     *
     * @param orgId       - ID оргюнита
     * @param whatWeCheck - указываем какой параметр проверяем
     *                    - KPI
     *                    - Fte
     *                    - shifts
     */
    public static void checkForErrorKPIorFteOrShiftsAndPreset(int orgId, String whatWeCheck) {
        String urlPart = "";
        switch (whatWeCheck) {
            case CASE_KPI:
                urlPart = KPI_FORECAST;
                break;
            case CASE_FTE:
                urlPart = FTE;
                break;
            case SHIFTS:
                urlPart = ROSTERING;
                break;
        }
        String urlEnding = makePath(BATCH, urlPart, orgId);
        try {
            getJsonFromUri(Projects.WFM, PRESET_URL, urlEnding);
        } catch (AssertionError error) {
            makeErrorKPI(orgId, whatWeCheck);
        }
    }

    /**
     * Создает ошибку при рассчете для выбранного оргюнита и параметра. Ошибка создается за счет того что отправляется
     * запрос на год вперед от текущего. Обычно данных там никаких нет и апи выдает ошибку.
     *
     * @param orgId       - оргюнит для которого создаем ошибку
     * @param whatWeCrash - для какого рассчета создаем ошибку
     */
    private static void makeErrorKPI(int orgId, String whatWeCrash) {
        String urlPart = "";
        String forAllure = "";
        switch (whatWeCrash) {
            case CASE_KPI:
                urlPart = KPI_FORECAST_BATCH;
                forAllure = KPI;
                break;
            case CASE_FTE:
                urlPart = CALCULATE_FTE_BATCH;
                forAllure = FTE;
                break;
            case SHIFTS:
                urlPart = ROSTERING_BATCH;
                forAllure = "смены";
                break;
        }
        String urlEnding = makePath(BATCH, urlPart);
        URI uri = setUri(Projects.WFM, PRESET_URL, urlEnding);
        LocalDate fromDate = LocalDateTools.getFirstDate().plusYears(1);
        LocalDate toDate = fromDate.with(TemporalAdjusters.lastDayOfMonth());
        int[] idArray = new int[1];
        idArray[0] = orgId;
        JSONObject miniObject = new JSONObject();
        switch (whatWeCrash) {
            case CASE_KPI:
                miniObject.put("algorithm", "DAYS_ARMA");
                miniObject.put("monthSumCoefficient", 0);
                miniObject.put("sumPredictionMethod", "YEAR_PATTERN");
                break;
            case CASE_FTE:
                miniObject.put("altAlgorithm", 0);
                miniObject.put(STRATEGY, "B");
                miniObject.put("useMathParam", false);
                break;
            case SHIFTS:
                miniObject.put("ignoreNoe", false);
                miniObject.put("withMinDeviation", false);
                break;
        }
        miniObject.put("organizationUnitIds", idArray);
        miniObject.put(FROM, fromDate.toString());
        miniObject.put(TO, toDate.toString());
        StringEntity requestEntity = new StringEntity(miniObject.toString(), HAL_JSON);
        HttpUriRequest some = RequestBuilder.post()
                .setUri(uri)
                .setEntity(requestEntity)
                .build();
        HttpResponse response = null;
        try {
            response = ClientReturners.httpClientReturner(Projects.WFM).execute(some);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Assert.assertEquals(response != null ? response.getStatusLine().getStatusCode() : 0, 200);
        Allure.addAttachment("Создание ошибки расчета",
                             String.format("В %s расчете оргюнита с ID %d была создана ошибка", forAllure, orgId));
    }

    public static void changeParameterField(MathParameter mathParameter, String fieldToChange, Object newValue) {
        String urlEnding = makePath(MATH_PARAMETERS, mathParameter.getMathParameterId());
        URI uri = RequestFormers.setUri(Projects.WFM, PRESET_URL, urlEnding);
        JSONObject temp = getJsonFromUri(Projects.WFM, PRESET_URL, urlEnding);
        temp.remove(fieldToChange);
        temp.put(fieldToChange, newValue);
        HttpResponse response = requestMaker(uri, temp, RequestBuilder.put(), HAL_JSON);
        assertStatusCode(response, uri.toString());
        Allure.addAttachment("Пресет для изменения значения поля мат. параметра", String.format("Запросом в api заменить значение поля %s мат. параметра %s на %s", fieldToChange, mathParameter.getCommonName(), newValue));
    }

    public static void changeParameterFieldForOM(OrgUnit orgUnit, MathParameter mathParameter, String fieldToChange, Object newValue) {
        String urlEnding = makePath(MATH_PARAMETERS, mathParameter.getMathParameterId());
        urlEnding = urlEnding.substring(urlEnding.indexOf("/") + 1);
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(ORG_UNITS, orgUnit.getId(), MATH_PARAMETER_VALUES, urlEnding));
        try {
            JSONObject temp = getJsonFromUri(Projects.WFM, uri);
            temp.remove(fieldToChange);
            temp.put(fieldToChange, newValue);
            HttpResponse response = requestMaker(uri, temp, RequestBuilder.put(), HAL_JSON);
            assertStatusCode(response, uri.toString());
            Allure.addAttachment("Пресет для изменения мат. параметра", String.format("Значение поля \"%s\" мат. параметра \"%s\" изменено на \"%s\"",
                                                                                      fieldToChange, mathParameter.getCommonName(), newValue));
        } catch (AssertionError e) {
            LOG.info("У оргюнита нет параметра по адресу {}", uri);
        }
    }

    private static Pairs.Builder getBuilder(int... omId) {
        Pairs.Builder builder = Pairs.newBuilder();
        if (omId.length != 0) {
            builder.orgUnitId(omId[0]);
        }
        return builder;
    }

    /**
     * Устанавливает значение мат. параметра для сущности
     *
     * @param entityId       айди сущности
     * @param mathParamValue мат. параметр, который будем изменять
     * @param newValue       новое значение мат. параметра
     * @param omId           id подразделения, для которого нужно применить параметр (опционально)
     */
    public static void setMathParamValue(int entityId, MathParameterValues mathParamValue, Object newValue, int... omId) {
        MathParameter mathParam = MathParameterRepository.getMathParameterWithValue(mathParamValue);
        String path = makePath(mathParamValue.getEntity().getLink(), entityId, MATH_PARAMETER_VALUES);
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, path, getBuilder(omId).build());
        JSONObject miniObject = new JSONObject();
        miniObject.put(TYPE, mathParamValue.getType());
        miniObject.put(NAME, mathParamValue.getName());
        miniObject.put(OUTER_ID, mathParamValue.getOuterId());
        miniObject.put(ENTITY, mathParamValue.getEntity().toString());
        miniObject.put(VALUE, newValue);
        JSONObject links = new JSONObject();
        links.put(SELF, new JSONObject().put(HREF, uri));
        links.put(MATH_PARAMETER, new JSONObject().put(HREF, setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(MATH_PARAMETERS, mathParam.getMathParameterId()))));
        miniObject.put(LINKS, links);
        LOG.info(JSON_LOGGER, miniObject);
        HttpResponse response = requestMaker(uri, miniObject, RequestBuilder.post(), HAL_JSON);
        assertStatusCode(response, 201, "Не удалось добавить мат параметр");
        LOG.info("Сотруднику был добавлен мат параметр \"{}\" со значением {}", mathParam.getCommonName(), newValue);
        Allure.addAttachment("Пресет для добавления мат. параметра",
                             String.format("Запросом в api добавлен мат. параметр \"%s\" со значением %s", mathParam.getCommonName(), newValue));
    }

    /**
     * Отправляет запрос POST, если мат.параметр у сущности отсутствовал и PUT с новым значением в противном случае
     *
     * @param entityId       айди сущности
     * @param mathParamValue мат. параметр, который будем изменять
     * @param newValue       новое значение мат. параметра
     * @param setValue       задает, устанавливать ли мат параметр в случае его отсутствия у сущности
     */
    public static void changeOrSetMathParamValue(int entityId, MathParameterValues mathParamValue, Object newValue, boolean setValue, int... omId) {
        MathParameter mathParam = MathParameterRepository.getMathParameterWithValue(mathParamValue);
        String urlEnding = makePath(mathParamValue.getEntity().getLink(), entityId, MATH_PARAMETER_VALUES, mathParam.getMathParameterId());
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, urlEnding, getBuilder(omId).build());
        try {
            JSONObject temp = getJsonFromUri(Projects.WFM, uri);
            temp.remove(VALUE);
            temp.put(VALUE, newValue);
            HttpResponse response = requestMaker(uri, temp, RequestBuilder.put(), HAL_JSON);
            assertStatusCode(response, uri.toString());
            Allure.addAttachment("Пресет для изменения мат. параметра", String.format("Значение поля \"value\" мат. параметра \"%s\" изменено на \"%s\"",
                                                                                      mathParam.getCommonName(), newValue));
        } catch (AssertionError e) {
            if (setValue) {
                LOG.info("Присваиваем мат параметру {} значение {}", mathParamValue.getName(), newValue);
                if (omId.length == 0) {
                    setMathParamValue(entityId, mathParamValue, newValue);
                } else {
                    setMathParamValue(entityId, mathParamValue, newValue, omId[0]);
                }
            } else {
                LOG.info("У сотрудника не установлен мат параметр {}", mathParamValue.getName());
            }
        }
    }

    @Step("Пресет. Проверить что в текущем графике есть день с типом {day}")
    public static int getAnyDayWithType(String scheduleId, Days day) {
        Map<Integer, String> tempDays = CommonRepository.getWorkingDays(scheduleId);
        int isoWeekday = 0;
        try {
            if (day.equals(Days.DAY)) {
                isoWeekday = tempDays.keySet()
                        .stream().filter(s -> tempDays.get(s) != null).findAny()
                        .orElseThrow(() -> new NoSuchElementException(NO_TEST_DATA + "В мапе нет подходящих значений"));
            } else if (day.equals(Days.DAY_OFF)) {
                isoWeekday = tempDays.keySet()
                        .stream().filter(s -> tempDays.get(s) == null).findAny()
                        .orElseThrow(() -> new NoSuchElementException(NO_TEST_DATA + "В мапе нет подходящих значений"));
            }
        } catch (NoSuchElementException e) {
            isoWeekday = tempDays.keySet().stream().findAny()
                    .orElseThrow(() -> new AssertionError(NO_TEST_DATA + "В оргюните нет смен"));
            String dayId = tempDays.get(isoWeekday);
            apiChangeDay(day, scheduleId, isoWeekday, dayId);
            Allure.addAttachment("Пресет.", "Будет сменем тип дня на нужный в пресете");
        }
        return isoWeekday;
    }

    @Step("В пресете добавить роль \"{role.name}\" случайному сотруднику с аккаунтом")
    public static User addRoleToEmployee(Role role, boolean hasDeputy) {
        User user = addRoleToRandomUser(role, null, (OrgUnit) null);
        List<UserDeputy> deputies = user.getUserDeputies();
        if (hasDeputy && deputies.isEmpty()) {
            addDeputyToUser(user, EmployeeRepository.getRandomEmployeeWithAccount(false, user.getEmployee()).getUser());
        }
        if (deputies.size() > 1 || !hasDeputy) {
            if (hasDeputy) {
                deputies.remove(0);
            }
            deputies.forEach(deputy -> deleteRequest(deputy.getLink(SELF)));
        }
        return user;
    }

    /**
     * Проверяет теги сотрудника, если у него нет тегов то добавляет один тег. Возвращает актуальный список тегов
     */
    public static List<String> checkEmployeeTags(Employee employee) {
        List<String> empTags = employee.getActualTags();
        if (empTags.isEmpty()) {
            List<String> allTags = new ArrayList<>(CommonRepository.getTags().keySet());
            String randomTag = getRandomFromList(allTags);
            addTagForEmployee(employee, randomTag);
            empTags = Collections.singletonList(randomTag);
        }
        return empTags;
    }

    /**
     * Добавляет тег сотруднику
     */
    public static void addTagForEmployee(Employee employee, String tag) {
        URI path = setUri(Projects.WFM, PRESET_URL, makePath(EMPLOYEES, TAGS, employee.getId()));
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(TAGS, tag);
        HttpResponse response = requestMaker(path, jsonObject, RequestBuilder.put(), HAL_JSON, Projects.WFM);
        assertStatusCode(response, path.toString());
        Allure.addAttachment("Добавление тега сотруднику", String.format("Сотруднику %s был добавлен тег \"%s\"",
                                                                         employee.getFullName(), tag));
    }

    /**
     * Добавляет тег подразделению
     */
    public static void addTagForOrgUnit(OrgUnit unit, String tag) {
        URI path = setUri(Projects.WFM, PRESET_URL, makePath(ORG_UNITS, TAGS, unit.getId()));
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(TAGS, tag);
        HttpResponse response = requestMaker(path, jsonObject, RequestBuilder.put(), HAL_JSON, Projects.WFM);
        assertStatusCode(response, path.toString());
        Allure.addAttachment("Добавление тега подразделению",
                             String.format("Подразделению %s (ID %s) был добавлен тег \"%s\"",
                                           unit.getName(), unit.getId(), tag));
    }

    /**
     * Удаляет все теги у подразделения. Ускоряет поиск сотрудников для свободной смены, если нужен сотрудник из того же подразделения.
     */
    public static void removeAllTagsFromOrgUnit(OrgUnit unit) {
        URI path = setUri(Projects.WFM, PRESET_URL, makePath(ORG_UNITS, TAGS, unit.getId()));
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(TAGS, "");
        jsonObject.put(ID, JSONObject.NULL);
        JSONObject linksJson = new JSONObject();
        linksJson.put(ORG_UNIT_JSON, new JSONObject().put(HREF, unit.getLinks().get(SELF)));
        linksJson.put(SELF, new JSONObject().put(HREF, path));
        jsonObject.put(LINKS, linksJson);
        HttpResponse response = requestMaker(path, jsonObject, RequestBuilder.put(), HAL_JSON, Projects.WFM);
        assertStatusCode(response, path.toString());
        Allure.addAttachment("Удаление тегов подразделения",
                             String.format("У подразделения %s (ID %s) были удалены все теги",
                                           unit.getName(), unit.getId()));
    }

    /**
     * Изменить тип ростера для заданного типа запроса расписания
     *
     * @param rolesId             id ростера
     * @param scheduleRequestType тип запроса расписания
     * @param rosterType          тип ростера
     */
    public static void changeScheduleRequestRoster(int rolesId, ScheduleRequestType scheduleRequestType, RosterTypes rosterType) {
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(USER_ROLES, rolesId, SCHEDULE_REQUEST));
        JSONObject object = new JSONObject();
        object.put(ROSTER_TYPE, rosterType.getValue());
        object.put(SCHEDULE_REQUEST_TYPE, scheduleRequestType.toString());
        HttpResponse response = requestMaker(uri, object, RequestBuilder.post(), HAL_JSON);
        assertStatusCode(response, 200, uri.toString());
    }

    public static void deleteTestRole(ITestContext c) {
        String roleName = String.valueOf(c.getAttribute(TEST_ROLE));
        if (!roleName.equals("null")) {
            UserRole userRole = getClassObjectFromJson(UserRole.class, getJsonFromUri(Projects.WFM, URI.create(roleName)));
            deleteRole(userRole);
            c.removeAttribute(TEST_ROLE);
        }
    }

    public static void deleteRole(UserRole userRole) {
        clearRoleFromUsers(userRole);
        try {
            deleteRequest(userRole);
        } catch (AssertionError e) {
            Allure.addAttachment("Не удалось удалить тестовую роль", userRole.getName());
            LOG.error("Не удалось удалить тестовую роль {}", userRole.getName());
        }
    }

    /**
     * Удаляет все роли, у которых есть "test" в названии
     */
    public static void deleteTestRoles() {
        List<UserRole> userRoles = UserRoleRepository.getUserRoles().stream()
                .filter(userRole -> userRole.getName().contains("test_role_"))
                .collect(Collectors.toList());
        for (UserRole userRole : userRoles) {
            deleteRole(userRole);
        }
    }

    /**
     * Создает роль с разрешениями
     *
     * @param withCreating - true роль с разрешением на создание предпочтений
     *                     - false роль без разрешения на создание предпочтений
     */
    public static Role createRoleWithPersonalSchedule(boolean withCreating) {
        List<PermissionType> permissionTypes = new ArrayList<>();
        permissionTypes.add(PermissionType.SCHEDULE_PERSONAL);
        permissionTypes.add(PermissionType.SCHEDULE_VIEW);
        if (withCreating) {
            permissionTypes.add(PermissionType.SCHEDULE_MANAGE_REQUESTS_CREATE);
        }
        return createCustomPermissionRole(permissionTypes);
    }

    public static Position createFreePosition(DateInterval dateInterval, int orgUnit) {
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(POSITIONS));

        PositionCategory posCat = PositionCategoryRepository.randomPositionCategory();
        PositionType posType = PositionTypeRepository.randomPositionType();
        PositionGroup posGroup = PositionGroupRepository.randomPositionGroup();
        String name = posType.getName();

        JSONObject miniObject = new JSONObject();
        miniObject.put(DATE_INTERVAL, new JSONObject().put(START_DATE, dateInterval.startDate));

        JSONObject posCatObject = new JSONObject();
        posCatObject.put(CALCULATION_MODE, posCat.getCalculationMode());
        posCatObject.put(CATEGORY_ID, posCat.getCategoryId());
        posCatObject.put(NAME, posCat.getName());
        posCatObject.put(OUTER_ID, posCat.getOuterId());
        posCatObject.put(LINKS, posCat.getLinks());
        miniObject.put(POSITION_CATEGORY, posCatObject);

        miniObject.put(JOB_TITLE, name);

        JSONObject posTypeObject = new JSONObject();
        posTypeObject.put(ID, posType.getId());
        posTypeObject.put(NAME, posType.getName());
        posTypeObject.put(OUTER_ID, posType.getOuterId());
        posTypeObject.put(LINKS, posType.getLinks());
        miniObject.put(POSITION_TYPE, posTypeObject);

        JSONObject posGroupObject = new JSONObject();
        posTypeObject.put(ID, posGroup.getId());
        posTypeObject.put(FTE_POSITION_GROUP, posGroup.getFtePositionGroup());
        posTypeObject.put(NAME, posGroup.getName());
        posTypeObject.put(LINKS, posGroup.getLinks());
        miniObject.put(POSITION_GROUP, posGroupObject);

        JSONObject links = new JSONObject();
        links.put(SELF, new JSONObject().put(HREF, uri));
        links.put(POSITION_TYPE, posType.getLinkWrappedInJson(SELF));
        links.put(POSITION_CATEGORY, posCat.getLinkWrappedInJson(SELF));
        links.put(POSITION_GROUP, posGroup.getLinkWrappedInJson(SELF));
        links.put(ORG_UNIT_JSON, new JSONObject().put(HREF, setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(ORGANIZATION_UNITS, orgUnit))));
        miniObject.put(LINKS, links);
        miniObject.put(NAME, name);
        miniObject.put(ACTIVE, true);

        HttpResponse response = requestMaker(uri, miniObject, RequestBuilder.post(), HAL_JSON);
        assertStatusCode(response, 201, "Не удалось добавить свободную позицию");
        Allure.addAttachment("Пресет для добавления пустой позиции",
                             String.format("В оргюнит %s была добавлена пустая позиция: %s",
                                           OrgUnitRepository.getOrgUnit(orgUnit).getName(), name));
        LOG.info("В оргюнит {} была добавлена пустая позиция: {}", OrgUnitRepository.getOrgUnit(orgUnit).getName(), name);
        LOG.info(JSON_LOGGER, miniObject);
        return getCreatedObject(response, Position.class);
    }

    /**
     * Смотрит количество тегов у оргюнита, и если их количество не соответствует, то делает запрос на изменение
     * и возвращает список актуальных тегов
     *
     * @param orgUnit  - оргюнит, который проверяем на теги
     * @param tagValue - енам, который указывает, сколько тегов нам нужно
     * @return список актуальных тегов
     */
    public static List<String> tagPreset(OrgUnit orgUnit, TagValue tagValue) {
        List<String> tags = new ArrayList<>();
        List<String> orgUnitTags = orgUnit.getTags();
        switch (tagValue) {
            case ONE:
                if (orgUnitTags.size() == 1) {
                    tags = orgUnitTags;
                    break;
                }
                tags.add(RandomStringUtils.randomAlphabetic(10));
                doPostRequestAddTags(tags, orgUnit);
                break;
            case NO_ONE:
                if (orgUnitTags.isEmpty()) {
                    tags = orgUnitTags;
                    break;
                }
                doPostRequestAddTags(tags, orgUnit);
                break;
            case SEVERAl:
                if (orgUnitTags.size() > 1) {
                    tags = orgUnitTags;
                    break;
                }
                tags.add(RandomStringUtils.randomAlphabetic(10));
                tags.add(RandomStringUtils.randomAlphabetic(10));
                doPostRequestAddTags(tags, orgUnit);
                break;
        }
        Allure.addAttachment("Тэги", String.format("\"%s\" имеет %d tags, и их названия: %s",
                                                   orgUnit.getName(), tagValue.ordinal(), tags));
        return tags;
    }

    /**
     * Сравнивает список сотрудников на UI с теми, которые взяты из апи, выбирает только тех, кто есть на UI
     * и проверяет на наличие Email адреса, затем активирует пресет на создание или очистку адреса для одного сотрудника,
     * если в этом есть необходимость
     *
     * @param positions   - сотрудники из апи
     * @param employeesUi - имена сотрудников с UI
     * @param withEmail   - должен быть сотрудник с адресом
     */
    public static void checkAndMakePresetEmail(List<EmployeePosition> positions, List<String> employeesUi,
                                               boolean withEmail) {
        String putEmail = "";
        ArrayList<Employee> allVerifyEmp = new ArrayList<>();
        for (String visibleName : employeesUi) {
            for (EmployeePosition employeePosition : positions) {
                Employee employee = employeePosition.getEmployee();
                String shortName = employee.getShortName();
                if (shortName.equals(visibleName)) {
                    putEmail = checkEmployeeForEmail(employee, withEmail, allVerifyEmp);
                }
            }
        }
        if (allVerifyEmp.size() == positions.size()) {
            Employee randomEmployee = allVerifyEmp.get(RANDOM.nextInt(allVerifyEmp.size()));
            makeEmailInApi(randomEmployee, putEmail);
        }
    }

    private static String checkEmployeeForEmail(Employee employee, boolean withEmail, ArrayList<Employee> allVerifyEmp) {
        String mail = employee.getEmail();
        String putEmail = "";
        if (!withEmail) {
            if (mail != null && mail.contains("@")) {
                allVerifyEmp.add(employee);
            }
        } else {
            putEmail = generateRandomEmail();
            if (mail == null || !mail.contains("@")) {
                allVerifyEmp.add(employee);
            }
        }
        return putEmail;
    }

    @Step("\"Пресет.\" Проверить наличие одиночного запроса в текущем оргЮните, при отсутствии - создать через Api")
    public static ScheduleRequest preSetNotRepeatRequestCheck(ScheduleRequestStatus status, int omId) {
        List<ScheduleRequest> tempRequests = ScheduleRequestRepository.getScheduleRequestsThisMonth(omId).stream()
                .filter(request -> !request.isRepeatable() && request.getStatus() == status &&
                        request.getType() != ScheduleRequestType.SHIFT).collect(Collectors.toList());
        if (!tempRequests.isEmpty()) {
            ScheduleRequest needRequest = getRandomFromList(tempRequests);
            Allure.addAttachment("Проверка", String.format("Был найден запрос с датой%s у сотрудника с именем ",
                                                           needRequest.getDateTimeInterval().getStartDate()));
            Shift shift = ShiftRepository.getShift(EmployeePositionRepository.getEmployeePosition(needRequest.getEmployee().getShortName(), omId),
                                                   needRequest.getDateTimeInterval().getStartDate(), null);
            if (shift != null) {
                deleteRequest(shift);
            }
            return needRequest;
        } else {
            return createScheduleRequestApi(status, false, omId);
        }
    }

    @Step("\"Пресет.\" Проверить наличие серии запросов в текущем подразделении; при отсутствии создать через Api")
    public static ScheduleRequest preSetRepeatRequestCheck(int omId) {
        List<ScheduleRequest> tempRequests = ScheduleRequestRepository.getScheduleRequestsThisMonth(omId).stream()
                .filter(request -> request.isRepeatable() && request.getStatus() == ScheduleRequestStatus.APPROVED &&
                        request.getType() != ScheduleRequestType.SHIFT).collect(Collectors.toList());
        if (!tempRequests.isEmpty()) {
            ScheduleRequest needRequest = getRandomFromList(tempRequests);
            Allure.addAttachment("Проверка",
                                 String.format("Была найдена серия запросов с датой начала%s у сотрудника с именем %s",
                                               needRequest.getDateTimeInterval().getStartDate(), needRequest.getDateTimeInterval().getEndDate()));
            Shift shift = ShiftRepository.getShift(EmployeePositionRepository.getEmployeePosition(needRequest.getEmployee().getShortName(), omId),
                                                   needRequest.getDateTimeInterval().getStartDate(), null);
            if (shift != null) {
                deleteRequest(shift);
            }
            return needRequest;
        } else {
            return createScheduleRequestApi(ScheduleRequestStatus.APPROVED, true, omId);
        }
    }

    @Step("Пресет. Отправить запрос на распознование, выполнить принудительную синхронизацию")
    public static EmployeePosition recognitionPreset() {
        JSONObject employeesPositionsJSON = getJsonFromUri(Projects.WFM, PRESET_URL,
                                                           makePath(EMPLOYEES, recognizeBioPreset().left, EMPLOYEE_POSITIONS));
        LocalDate date = LocalDate.now().plusDays(1);
        return getListFromJsonObject(employeesPositionsJSON, EmployeePosition.class).stream()
                .filter(e -> e.getDateInterval().includesDate(date))
                .findFirst().orElseThrow(() -> new AssertionError(NO_TEST_DATA + "Распознанный ранее сотрудник не числится на должности"));
    }

    /**
     * Просматривает смены для сотрудника, если у него нет смены на последний день то создает ее.
     * Если есть, то просто возвращает эту смену
     *
     * @param employeePosition - сотрудник с должностью
     */
    public static Shift getLastDayMonthShift(EmployeePosition employeePosition) {
        LocalDate lastDate = LocalDateTools.getLastDate();
        Shift shift = ShiftRepository.getShift(employeePosition, lastDate, null);
        return shift != null ? shift : ShiftRepository.getLastDayMonthShift(employeePosition);
    }

    /**
     * Для сотрудника ищет любую смену за текущий месяц. Если ее нет, то создает смену
     */
    public static Shift defaultShiftPreset(EmployeePosition employeePosition) {
        return defaultShiftPreset(employeePosition, ShiftTimePosition.DEFAULT);
    }

    public static Shift defaultShiftPreset(EmployeePosition employeePosition, ShiftTimePosition timePosition) {
        return defaultShiftPreset(employeePosition, timePosition, false);
    }

    public static Shift defaultShiftPreset(EmployeePosition employeePosition, ShiftTimePosition timePosition, boolean withoutComment) {
        List<Shift> shifts = ShiftRepository.getShifts(employeePosition, timePosition);
        if (withoutComment) {
            shifts = shifts.stream()
                    .filter(s -> s.getComment() == null)
                    .collect(Collectors.toList());
        }
        Shift shift = !shifts.isEmpty() ? getRandomFromList(shifts)
                : presetForMakeShift(employeePosition, false, timePosition);
        Allure.addAttachment("Проверка наличия смены у сотрудника с именем " + employeePosition, shift.toString());
        return shift;
    }

    /* метод когда работаем со сменами одного и того же сотрудника чтобы не кликать на одну и ту же дату*/
    @Step("Пресет. Проверить наличие смены у того же сотрудника с именем {employeePosition}")
    public static Shift defaultShiftPreset(EmployeePosition employeePosition, LocalDate firstDate) {
        return defaultShiftPreset(employeePosition, firstDate, ShiftTimePosition.DEFAULT);
    }

    /**
     * Ищет смену сотрудника за определённый период, исключая указанную дату.
     * Если смены нет, то создаёт её
     */
    public static Shift defaultShiftPreset(EmployeePosition employeePosition, LocalDate firstDate, ShiftTimePosition timePosition) {
        List<Shift> shifts = ShiftRepository.getShifts(employeePosition, timePosition);
        shifts.removeIf(shift -> shift.getDateTimeInterval().getStartDate().isEqual(firstDate));
        Shift shift = !shifts.isEmpty() ? getRandomFromList(shifts)
                : presetForMakeShift(employeePosition, false, timePosition);
        Allure.addAttachment(STATUS_SHIFT, shift.toString());
        return shift;
    }

    @Step("Проверить есть ли у сотрудника с именем {employeePosition} смена, заканчивающаяся следующим днем")
    public static Shift shiftDateEndTomorrowInFuturePreset(EmployeePosition employeePosition,
                                                           ShiftTimePosition timePosition) {
        List<Shift> shifts = ShiftRepository.getShifts(employeePosition, timePosition).stream()
                .filter(Shift::isNextDayEnd).collect(Collectors.toList());
        return !shifts.isEmpty() ? getRandomFromList(shifts) :
                presetForMakeShift(employeePosition, true, timePosition);
    }

    @Step("Проверить есть ли у сотрудника с именем {employeePosition} смена, заканчивающаяся следующим днем")
    public static Shift shiftDateEndTomorrowInFuturePreset(EmployeePosition employeePosition,
                                                           ShiftTimePosition timePosition, Shift shift) {
        List<Shift> shifts = ShiftRepository.getShifts(employeePosition, timePosition).stream()
                .filter(Shift::isNextDayEnd).collect(Collectors.toList());
        shifts.remove(shift);
        return !shifts.isEmpty() ? getRandomFromList(shifts) :
                presetForMakeShiftWithExcludeDate(employeePosition, true,
                                                  timePosition, shift.getDateTimeInterval().getStartDate());
    }

    @Step("Проверить есть ли у сотрудника с именем {employeePosition} в будущих днях смена, заканчивающаяся следующим днем")
    public static Shift shiftDateEndTomorrowInFuturePreset(EmployeePosition employeePosition) {
        LocalDate current = LocalDateTools.now();
        if (current.isEqual(current.with(TemporalAdjusters.lastDayOfMonth()))) {
            throw new IllegalArgumentException(NO_VALID_DATE + "Операция не может быть совершена, потому что сегодня последний день месяца");
        }
        return shiftDateEndTomorrowInFuturePreset(employeePosition, ShiftTimePosition.FUTURE);
    }

    @Step("Проверить есть ли у сотрудника с именем {employeePosition} нужные нам ячейки для выделения")
    public static Shift[] massShiftPresetCheckForEmployee(EmployeePosition employeePosition, Shift... excludedShift) {
        List<Shift> shifts = ShiftRepository.getShifts(employeePosition, ShiftTimePosition.DEFAULT);
        List<Shift> excludedShifts = Arrays.asList(excludedShift);
        List<LocalDate> excludedDate = excludedShifts.stream().map(Shift::getDateTimeInterval)
                .map(DateTimeInterval::getStartDate).collect(Collectors.toList());
        shifts.removeAll(excludedShifts);
        LocalDate lastDate = LocalDateTools.getLastDate();
        Shift secondShift = null;
        Shift firstShift;
        if (!shifts.isEmpty()) {
            firstShift = getRandomFromList(shifts);
            LocalDate anotherShiftDate = firstShift.getDateTimeInterval().getStartDate().plusDays(1);
            if (firstShift.getDateTimeInterval().getStartDate().isEqual(lastDate)
                    || excludedDate.contains(anotherShiftDate)) {
                anotherShiftDate = anotherShiftDate.minusDays(2);
            }
            LocalDate finalAnotherShiftDate = anotherShiftDate;
            for (Shift shift : shifts) {
                if (shift.getDateTimeInterval().getStartDate().equals(finalAnotherShiftDate)) {
                    secondShift = shift;
                    break;
                }
            }
            if (secondShift == null) {
                secondShift = presetForMakeShiftDate(employeePosition, finalAnotherShiftDate,
                                                     false, ShiftTimePosition.DEFAULT, shifts);
            }
        } else {
            List<LocalDate> dates = ShiftTimePosition.DEFAULT.getShiftsDateInterval().subtract(excludedDate);
            LocalDate random = getRandomFromList(dates);
            LocalDate random2 = random.plusDays(1);
            if (!dates.contains(random2)) {
                random2 = random.minusDays(1);
            }
            firstShift = presetForMakeShiftDate(employeePosition, random, false,
                                                ShiftTimePosition.DEFAULT, shifts);
            secondShift = presetForMakeShiftDate(employeePosition, random2, false,
                                                 ShiftTimePosition.DEFAULT, shifts);
        }
        Shift[] shiftsReturn = new Shift[2];
        shiftsReturn[0] = firstShift;
        shiftsReturn[1] = secondShift;
        return shiftsReturn;
    }

    @Step("Проверить есть ли у второго сотрудника с именем {employeePosition.employee.lastName} " +
            "{employeePosition.employee.firstName} нужные нам ячейки")
    public static Shift[] massShiftPresetAtSameDays(EmployeePosition employeePosition, LocalDate[] dates) {
        LocalDate start = dates[0];
        LocalDate end = dates[1];
        Shift first = ShiftRepository.getShift(employeePosition, start, null);
        Shift second = ShiftRepository.getShift(employeePosition, end, null);
        List<Shift> shifts = ShiftRepository.getShifts(employeePosition, ShiftTimePosition.DEFAULT);
        if (first == null) {
            presetForEmptyRequestCell(employeePosition.getEmployee(), start);
            first = presetForMakeShiftDate(employeePosition, start, false,
                                           ShiftTimePosition.DEFAULT, shifts);
        }
        if (second == null) {
            presetForEmptyRequestCell(employeePosition.getEmployee(), end);
            second = presetForMakeShiftDate(employeePosition, end, false,
                                            ShiftTimePosition.DEFAULT, shifts);
        }
        Shift[] shiftsReturn = new Shift[2];
        shiftsReturn[0] = first;
        shiftsReturn[1] = second;
        return shiftsReturn;
    }

    @Step("Пресет. Проверить статус графика оргюнита {orgUnit.name} и изменить его на \"{graphStatus.statusName}\"")
    public static void publishGraphPreset(GraphStatus graphStatus, OrgUnit orgUnit) {
        int orgId = orgUnit.getId();
        switch (graphStatus) {
            case PUBLISH:
                rosterPublishCheck(RosterRepository.getActiveRosterThisMonth(orgId).getId());
                break;
            case NOT_PUBLISH:
                Roster activeRosterThisMonth = RosterRepository.getActiveRosterThisMonth(orgId);
                if (activeRosterThisMonth.isPublished()) {
                    presetForMakeShift(EmployeePositionRepository.getRandomEmployeeWithCheckByApi(orgId, null, false),
                                       false, ShiftTimePosition.FUTURE);
                    Allure.addAttachment("Создание неопубликованного графика",
                                         "Так как текущий активный график опубликован, для того чтобы появился новый " +
                                                 "неопубликованный график, мы создали новый график при помощи внесения изменения в расписание");
                } else if (activeRosterThisMonth.isOnApproval()) {
                    makeRosterPublication(activeRosterThisMonth.getId());
                    presetForMakeShift(EmployeePositionRepository.getRandomEmployeeWithCheckByApi(orgId, null, false),
                                       false, ShiftTimePosition.FUTURE);
                    Allure.addAttachment("Создание неопубликованного графика",
                                         "Так как текущий активный график находится на утверждении, для того чтобы появился" +
                                                 " новый неопубликованный график, мы отправили активный на публикацию, а затем создали " +
                                                 "новый график при помощи внесения изменения в расписание");
                }
                break;
            case ON_APPROVAL:
                nonPublishAndApproveChecker(orgId);
                break;
        }
    }

    @Step("ПРЕСЕТ. Проверить есть ли у оргЮнита {freeComments} свободных мест для комментариев у ростеров," +
            " если нет, то удалить необходимое количество комментариев")
    public static int checkFreeRostersComment(int freeComments, int omNumber) {
        List<Roster> rosters = RosterRepository.getRosters(omNumber);
        List<Roster> rostersWithComments = rosters.stream()
                .filter(key ->
                        {
                            String description = key.getDescription();
                            return description != null && !description.equals("");
                        }
                )
                .collect(Collectors.toList());
        Allure.addAttachment("Текущие комментарии", "До пресета были следующие комментарии "
                + rostersWithComments.stream().map(Roster::getDescription).collect(Collectors.joining(", ")));
        int size = rostersWithComments.size();
        Roster randomRoster = !rostersWithComments.isEmpty() ? getRandomFromList(rostersWithComments) : getRandomFromList(rosters);
        int version = randomRoster.getVersion();
        int id = randomRoster.getId();
        String allureAttachmentName = "Пресет для очистки места под комментарии";
        if (freeComments == 2) {
            if (rosters.size() - rostersWithComments.size() == 1) {
                createOrDeleteRosterComment(id, String.valueOf(version), CommentValue.EMPTY);
                size--;
                Allure.addAttachment(allureAttachmentName, "В ходе пресета был удален один комментарий");
            } else if (rosters.size() - rostersWithComments.size() == 0) {
                for (int i = 0; i < 2; i++) {
                    createOrDeleteRosterComment(id, String.valueOf(version), CommentValue.EMPTY);
                    rosters.remove(version);
                    size--;
                }
                Allure.addAttachment(allureAttachmentName, "В ходе пресета был удалены два комментария");
            } else {
                Allure.addAttachment(allureAttachmentName, "В ходе пресета не были удалены комментарии");
            }
        }
        if (freeComments == 1) {
            if (rosters.size() - rostersWithComments.size() == 0) {
                createOrDeleteRosterComment(id, String.valueOf(version), CommentValue.EMPTY);
                size--;
                Allure.addAttachment(allureAttachmentName, "В ходе пресета был удален 1 комментарий");
            }
        } else {
            Allure.addAttachment(allureAttachmentName, "В ходе пресета не были удалены комментарии");
        }
        return size;
    }

    /**
     * Берет случайную дату для изменения запроса и очищает эту дату от запросов и смен
     *
     * @param request запрос, который будем изменять
     * @param omId    оргюнит, где это все происходит
     * @return дата, на которую будем изменять запрос
     */
    public static LocalDate getRandomDateForChangeRequest(ScheduleRequest request, int omId) {
        LocalDate endDate = request.getDateTimeInterval().getStartDate().plusDays((long) RANDOM.nextInt(10) + 5);
        clearDateForChangeRequest(request, omId, endDate);
        return endDate;
    }

    public static void clearDateForChangeRequest(ScheduleRequest request, int omId, LocalDate endDate) {
        Employee employee = request.getEmployee();
        presetForEmptyRequestCell(employee, endDate);
        if (endDate.getMonthValue() == LocalDateTools.now().getMonthValue()) {
            Shift shift = ShiftRepository.getShift(EmployeePositionRepository.getEmployeePosition(employee.getShortName(), omId),
                                                   endDate, null);
            if (shift != null) {
                deleteRequest(shift);
            }
        }
    }

    /**
     * Создает новую категорию позиции в апи
     *
     * @param testName - название для категории позиции
     */
    public static void makeNewTestPositionCategory(String testName) {
        URI uri = setUri(Projects.WFM, PRESET_URL, POSITION_CATEGORIES);
        JSONObject positionCategory = new JSONObject();
        positionCategory.put(NAME, testName);
        positionCategory.put(CALCULATION_MODE, "DYNAMIC");
        HttpResponse response = requestMaker(uri, positionCategory, RequestBuilder.post(), HAL_JSON);
        LOG.info("На адрес {} был отправлен запрос на изменение: {}", uri, positionCategory);
        Assert.assertEquals(response != null ? response.getStatusLine().getStatusCode() : 0, 201,
                            "preset. В пресете не удалось создать новую категорию позиции");
        Allure.addAttachment("Создание тестовой категории позиции",
                             "В результате действия пресета была создана тестовая категория позиция с именем: " + testName);
    }

    /**
     * Удаляет категорию позиции по ее айди
     */
    public static void deletePositionCategory(int id) {
        URI uri = setUri(Projects.WFM, PRESET_URL, makePath(POSITION_CATEGORIES, id));
        deleteRequest(uri);
    }

    /**
     * Удаляет категорию позиции по ее имени. Нужно, чтобы очищать категории после теста
     *
     * @param name - тестовое имя категории позиции
     */
    public static void deleteTestPositionCategory(String name) {
        List<PositionCategory> allPositionCategoriesByFilter = PositionCategoryRepository
                .getAllPositionCategoriesByFilter(WorkGraphFilter.ALL);
        //конструкция усложнена чтобы при отсутствии категории с именем он не падал с ошибкой
        allPositionCategoriesByFilter.stream()
                .filter(positionCategory -> positionCategory.getName().contains(name)).
                forEach(pc -> deletePositionCategory(pc.getCategoryId()));
        Allure.addAttachment("Удаление тестовой категории позиции",
                             "В результате действия пресета была удалена тестовая категория позиция с именем: " + name);
    }

    /**
     * Создает новый тип позиции
     *
     * @param testName - имя создаваемого типа позиции
     */
    public static void makeNewPositionType(String testName) {
        URI uri = setUri(Projects.WFM, PRESET_URL, POSITION_TYPES);
        JSONObject positionType = new JSONObject();
        positionType.put(NAME, testName);
        HttpResponse response = requestMaker(uri, positionType, RequestBuilder.post(), HAL_JSON);
        LOG.info("На адрес {} был отправлен запрос на изменение: {}", uri, positionType);
        Assert.assertEquals(response != null ? response.getStatusLine().getStatusCode() : 0, 201,
                            "preset. Не удалось создать новый тип позиции");
        Allure.addAttachment("Создание тестового типа позиции",
                             "В результате действия пресета был создан тестовый тип позиции с именем: " + testName);
    }

    /**
     * Удаляет тип позиции по айди
     */
    private static void deletePositionType(int id) {
        URI uri = setUri(Projects.WFM, PRESET_URL, makePath(POSITION_TYPES, id));
        deleteRequest(uri);
    }

    /**
     * Удаляет тип позиции по его имени. Нужно, чтобы очищать типы позиций после теста
     *
     * @param name - тестовое имя типа позиции
     */
    public static void deleteTestPositionType(String name) {
        Map<String, Integer> allPositionTypes = CommonRepository.getAllPositionTypesForPositionType();
        if (allPositionTypes.containsKey(name)) {
            int id = allPositionTypes.get(name);
            deletePositionType(id);
            Allure.addAttachment("Удаление тестового типа позиции",
                                 "В результате действия пресета была удалена тестовая позиция с именем: " + name);
        }
    }

    /**
     * Меняет родительский оргюнит
     *
     * @param orgUnit   - оргюнит у которого меняем родителя
     * @param newParent - новый родитель для оргюнита
     */
    public static void changeParentOrgUnit(OrgUnit orgUnit, OrgUnit newParent) {
        JSONObject miniObject = new JSONObject();
        JSONObject links = new JSONObject();
        JSONObject self = new JSONObject();
        final String self1 = orgUnit.getLinks().get(SELF);
        self.put(HREF, self1);
        JSONObject parentLink = new JSONObject();
        final String newParentSelf = newParent.getLinks().get(SELF);
        parentLink.put(HREF, newParentSelf);
        links.put(SELF, self);
        links.put(PARENT, parentLink);
        miniObject.put(LINKS, links);
        miniObject.put(ID, orgUnit.getId());
        miniObject.put(NAME, orgUnit.getName());
        miniObject.put(DATE_FROM, orgUnit.getDateInterval().getStartDate());
        final LocalDate endDate = orgUnit.getDateInterval().getEndDate();
        miniObject.put(DATE_TO, endDate == null ? JSONObject.NULL : endDate);
        miniObject.put(EMAIL, orgUnit.getEmail());
        miniObject.put(AVAILABLE_FOR_CALCULATION, orgUnit.isAvailableForCalculation());
        miniObject.put(ACTIVE, true);

        JSONObject parentObject = new JSONObject();
        JSONObject linksParent = new JSONObject();
        JSONObject selfParent = new JSONObject();
        selfParent.put(HREF, newParentSelf);
        linksParent.put(SELF, selfParent);
        parentObject.put(LINKS, linksParent);
        parentObject.put(ID, newParent.getId());
        miniObject.put(PARENT, parentObject);

        URI uriPreset = setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(ORGANIZATION_UNITS, orgUnit.getId()));
        HttpResponse response = requestMaker(uriPreset, miniObject, RequestBuilder.put(), HAL_JSON);
        LOG.info(REQUEST_LOGGER, uriPreset);
        LOG.info(JSON_LOGGER, miniObject);
        assertStatusCode(response, uriPreset.toString());
        Allure.addAttachment("Пресет смены родителя оргюнита",
                             String.format("В результате действия пресета оргюниту \"%s\" был изменен родительский оргнюнит на: %s",
                                           orgUnit.getName(), newParent.getName()));
    }

    /**
     * Меняет тип оргюнита
     *
     * @param orgUnit - оргюнит у которого меняем тип
     */
    public static void changeTypeOrgUnit(OrgUnit orgUnit, int typeId) {
        OrgUnit newParent = orgUnit.getParentOrgUnit();
        JSONObject miniObject = new JSONObject();
        JSONObject links = new JSONObject();
        JSONObject self = new JSONObject();
        final String self1 = orgUnit.getLinks().get(SELF);
        self.put(HREF, self1);
        JSONObject parentLink = new JSONObject();
        final String newParentSelf = newParent.getLinks().get(SELF);
        parentLink.put(HREF, newParentSelf);
        links.put(SELF, self);
        links.put(PARENT, parentLink);
        miniObject.put(LINKS, links);
        miniObject.put(ID, orgUnit.getId());
        miniObject.put(NAME, orgUnit.getName());
        miniObject.put(DATE_FROM, orgUnit.getDateInterval().getStartDate());
        final LocalDate endDate = orgUnit.getDateInterval().getEndDate();
        miniObject.put(DATE_TO, endDate == null ? JSONObject.NULL : endDate);
        miniObject.put(EMAIL, orgUnit.getEmail());
        miniObject.put(AVAILABLE_FOR_CALCULATION, orgUnit.isAvailableForCalculation());
        miniObject.put(ACTIVE, true);
        miniObject.put(ORG_UNIT_TYPE_ID, typeId);

        JSONObject parentObject = new JSONObject();
        JSONObject linksParent = new JSONObject();
        JSONObject selfParent = new JSONObject();
        selfParent.put(HREF, newParentSelf);
        linksParent.put(SELF, selfParent);
        parentObject.put(LINKS, linksParent);
        parentObject.put(ID, newParent.getId());
        miniObject.put(PARENT, parentObject);

        URI uriPreset = setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(ORGANIZATION_UNITS, orgUnit.getId()));
        HttpResponse response = requestMaker(uriPreset, miniObject, RequestBuilder.put(), HAL_JSON);
        LOG.info(REQUEST_LOGGER, uriPreset);
        LOG.info(JSON_LOGGER, miniObject);
        assertStatusCode(response, uriPreset.toString());
        Allure.addAttachment("Пресет смены типа оргюнита",
                             String.format("В результате действия пресета оргюниту: \"%s\" был изменен тип на: %d",
                                           orgUnit.getName(), typeId));
    }

    /**
     * Меняет родительский оргюнит на случайный оргюнит самой высокой иерархии, записывает старые данные в файл
     * чтобы потом можно было вернуть
     *
     * @param orgUnit - оргюнит у которого меняем родителя
     */
    public static void changeOrgUnitParentToHighLevelOrgUnit(OrgUnit orgUnit) {
        List<Integer> integers = new ArrayList<>(CommonRepository.getAllOrgUnitTypes().keySet());
        Collections.sort(integers);
        int unitTypeId = integers.get(0);
        List<OrgUnit> orgUnitsByTypeId = OrgUnitRepository.getOrgUnitsByTypeId(unitTypeId);
        OrgUnit newParentUnit = getRandomFromList(orgUnitsByTypeId);
        if (orgUnit.getParentId() != newParentUnit.getId()) {
            changeParentOrgUnit(orgUnit, newParentUnit);
        }
        //Запись данных в файл, чтобы потом вернуть все назад
        File file = new File("src/main/resources/tempParentForOm.properties");
        try (FileWriter fileWriter = new FileWriter(file, false)) {
            fileWriter.write(String.format("orgId=%d%nparentOrgId=%d%norgTypeId=%d",
                                           orgUnit.getId(), orgUnit.getParentId(), orgUnit.getOrganizationUnitTypeId()));
        } catch (IOException e) {
            LOG.info("Файл со сломанными базовыми тестами отсутствует", e);
        }
    }

    /**
     * Метод возвращает настройки типа оргюнита и родительского оргюнита назад, т.к если тест падает,
     * настройки возвращались в исходное состояние
     */
    public static void makeTypeAndParentUnitSettingBack() {
        Properties properties = new Properties();
        //читаем файл
        File file = new File("src/main/resources/tempParentForOm.properties");
        try (FileInputStream fileInput = new FileInputStream(file)) {
            properties.load(fileInput);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            Files.delete(file.toPath());
        } catch (IOException e) {
            throw new AssertionError(FAILED_PRESET + "Файл не был удалён");
        }
        //ищем оргюниты по их айди
        OrgUnit orgUnit = OrgUnitRepository.getOrgUnit(Integer.parseInt(properties.getProperty("orgId")));
        OrgUnit oldParent = OrgUnitRepository.getOrgUnit(Integer.parseInt(properties.getProperty("parentOrgId")));
        int oldOrganizationUnitTypeId = Integer.parseInt(properties.getProperty("orgTypeId"));

        //пресет, чтобы вернуть все в обратное состояние
        PresetClass.changeTypeOrgUnit(orgUnit, oldOrganizationUnitTypeId);
        PresetClass.changeParentOrgUnit(orgUnit, oldParent);
    }

    public static void switchRequestValue(String param, ScheduleRequestType type, boolean isTrue) {
        ScheduleRequestAlias requestType = ScheduleRequestAliasRepository.getAlias(type);
        JSONObject miniObject = new JSONObject();
        miniObject.put(param, isTrue);
        miniObject.put(OUTER_ID, requestType.getOuterId());
        miniObject.put(TITLE, requestType.getTitle());
        miniObject.put(SHORT_NAME, requestType.getShortName());
        miniObject.put(TYPE, requestType.getType());
        miniObject.put(LINKS, requestType.getLinks());

        URI uri = URI.create(requestType.getSelfLink());
        HttpResponse response = requestMaker(uri, miniObject, RequestBuilder.put(), HAL_JSON);
        assertStatusCode(response, uri.toString());
    }

    public static BuildInfo getBuildInfo() {
        URI uri = URI.create(makePath(CommonRepository.URL_BASE, "build-info"));
        HttpUriRequest requestValues = RequestBuilder.get(uri).build();
        requestValues.addHeader("accept", "ext/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
        String json;
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpResponse response = httpClient.execute(requestValues);
            if (response.getStatusLine().getStatusCode() == 404) {
                return null;
            }
            assertStatusCode(response, 200, uri.toString());
            HttpEntity values = response.getEntity();
            json = EntityUtils.toString(values);
        } catch (Exception e) {
            LOG.info("Не удалось получить информацию о версии стенда", e);
            return null;
        }
        return new BuildInfo(new JSONObject(json));
    }

    /**
     * Проверяет, что в оргюните есть собственные сотрудники (не временный и без "АУТ" в названии должности).
     * Возвращает случайного собственного сотрудника.
     * Если таких нет, бере одного случайного сотрудника и меняет его позицию так, чтобы он стал собственным, и возвращает его.
     *
     * @param unit оргюнит, где проводится проверка
     */
    public static EmployeePosition checkOwnEmployeeAvailability(OrgUnit unit) {
        List<EmployeePosition> employeePositions = EmployeePositionRepository.getEmployeePositionsWithoutFiredEmployees(unit.getId(), LocalDate.now(), false)
                .stream()
                .filter(e -> e.getEmployee().getEndWorkDate() == null)
                .collect(Collectors.toList());
        List<EmployeePosition> ownPositions = employeePositions.stream()
                .filter(e -> !e.isTemporary() && !e.getPosition().getName().contains("АУТ"))
                .collect(Collectors.toList());
        if (ownPositions.isEmpty()) {
            EmployeePosition ownPosition = getRandomFromList(employeePositions);
            PositionCategory posCat = PositionCategoryRepository.getAllPositionCategoriesByFilter(WorkGraphFilter.ALL)
                    .stream()
                    .filter(pos -> !pos.getName().contains("АУТ"))
                    .collect(randomItem());
            PositionGroup posGroup = PositionGroupRepository.getPositionGroupByPartialName(posCat.getName());
            PositionType posType = PositionTypeRepository.getPositionTypeByName(posGroup.getName());
            changePosition(ownPosition, posCat, posGroup, posType);
            LOG.info("У оргюнита \"{}\" (ID {}) не найдены собственные сотрудники", unit.getName(), unit.getId());
            return ownPosition;
        }
        LOG.info("У оргюнита \"{}\" (ID {}) найдены собственные сотрудники", unit.getName(), unit.getId());
        return getRandomFromList(ownPositions);
    }

    /**
     * Меняет настройку "Использовать биржу смен" для оргюнита
     *
     * @param orgUnit  подразделение, для которого нужно включить или выключить использование биржи
     * @param newValue желаемое значение настройки
     */
    public static void switchShiftExchange(OrgUnit orgUnit, boolean newValue) {
        MathParameter mathParameter = MathParameterRepository.getMathParameters()
                .stream()
                .filter(e -> e.getOuterId().equals("onFreeShifts"))
                .collect(Collectors.toList()).get(0);

        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(ORG_UNITS, orgUnit.getId(), MATH_PARAMETER_VALUES));
        JSONObject temp = new JSONObject();
        temp.put("hidden", mathParameter.isHidden());
        temp.put(INHERITED, false);
        temp.put(ROW, JSONObject.NULL);
        temp.put(COL, JSONObject.NULL);
        temp.put(LOGIC_GROUP_ID, JSONObject.NULL);
        temp.put(OUTER_ID, mathParameter.getOuterId());
        temp.put(NAME, mathParameter.getName());
        temp.put(COMMON_NAME, mathParameter.getCommonName());
        temp.put(SHORT_NAME, mathParameter.getShortName());
        temp.put(TYPE, mathParameter.getType());
        temp.put(ENTITY, mathParameter.getEntity());
        temp.put(VIEW_TYPE, "LIST");
        temp.put(VALUES, JSONObject.NULL);

        temp.put(VALUE, newValue);
        temp.put(DELETED, false);
        temp.put(PARENT_VALUE, JSONObject.NULL);

        JSONObject links = new JSONObject();
        links.put(REL_MATH_PARAMETER, mathParameter.getLinkWrappedInJson(SELF));
        links.put(SELF, new JSONObject().put(HREF, uri));
        temp.remove(LINKS);
        temp.put(LINKS, links);
        HttpResponse response = requestMaker(uri, temp, RequestBuilder.post(), HAL_JSON);
        LOG.info("Меняем настройку \"Использовать биржу смен\" для оргюнита \"{}\" (ID {}) на положение {}", orgUnit.getName(), orgUnit.getId(), newValue);
        assertStatusCode(response, 201, uri.toString());
        Allure.addAttachment("Изменение значения настройки \"Использовать биржу смен\"",
                             String.format("Для подразделения \"%s\" (ID %s) была изменена настройка \"Использовать биржу смен\" на положение %s",
                                           orgUnit.getName(), orgUnit.getId(), newValue));
    }

    /**
     * Проверяет, есть ли в оргюните временные сотрудники. Если нет, берет одного активного на данный момент сотрудника,
     * включает для него атрибут "временный перевод" и возвращает его.
     *
     * @return созданная/найденная временная позиция
     */
    public static EmployeePosition temporaryPositionPreset(OrgUnit unit) {
        List<EmployeePosition> employeePositions = EmployeePositionRepository.getEmployeePositionsWithoutFiredEmployees(unit.getId(), LocalDate.now(), false);
        List<EmployeePosition> temporaryPositions = employeePositions.stream().filter(EmployeePosition::isTemporary).collect(Collectors.toList());
        String allureTitle = "Пресет. Поиск сотрудника с атрибутом \"Временный перевод\"";
        if (temporaryPositions.isEmpty()) {
            EmployeePosition employeePosition = getRandomFromList(employeePositions);
            return setTemporaryToEmployeePosition(employeePosition, LocalDate.now(), LocalDateTools.getLastDate(), true);
        } else {
            EmployeePosition ep = getRandomFromList(temporaryPositions);
            String allureContent = String.format("В подразделении %s (ID %s) был найден сотрудник с атрибутом \"временный перевод\": %s",
                                                 unit.getName(), unit.getId(), ep.getEmployee().getFullName());
            LOG.info(allureContent);
            Allure.addAttachment(allureTitle, allureContent);
            return ep;
        }
    }

    /**
     * Ищет атстаффового сотрудника в подразделении. Если таких нет, берет случайного и мменяет его позицию.
     */
    public static void makeOutStaffEmployee(OrgUnit unit) {
        List<EmployeePosition> employeePositions = EmployeePositionRepository.getEmployeePositionsWithoutFiredEmployees(unit.getId(), LocalDate.now(), false)
                .stream().filter(e -> e.getEmployee().getEndWorkDate() == null).collect(Collectors.toList());
        List<EmployeePosition> outStaffPositions = employeePositions.stream().filter(ep -> ep.getPosition().getName().contains("АУТ")).collect(Collectors.toList());
        if (outStaffPositions.isEmpty()) {
            EmployeePosition outStaffEP = getRandomFromList(employeePositions);
            PositionCategory posCat = getRandomFromList(PositionCategoryRepository.getPositionCategoriesByPartialName("АУТ"));
            PositionType posType = PositionTypeRepository.getPositionTypeByName(posCat.getName());
            PositionGroup posGroup = PositionGroupRepository.getPositionGroupByName("Аутстафф");
            PresetClass.changePosition(outStaffEP, posCat, posGroup, posType);
            Allure.addAttachment("Пресет. Изменение сотрудника", "Сотрудник {} был переведен на должность {}", outStaffEP.toString(), posCat.getName());
        } else {
            LOG.info("У оргюнита \"{}\" (ID {}) найдены сотрудники аутстафф", unit.getName(), unit.getId());
        }
    }

    /**
     * Устанавливает позиции сотрудника признак "Временный перевод"
     *
     * @return измененная позиция сотрудника
     */
    @Step("Установить значение признака temporary в \"{temporary}\" сотруднику {ep}")
    public static EmployeePosition setTemporaryToEmployeePosition(EmployeePosition ep, LocalDate startDate, LocalDate endDate, boolean temporary) {
        URI uri = URI.create(ep.getSelfLink());
        JSONObject miniObject = new JSONObject();
        JSONObject links = new JSONObject();
        links.put(EMPLOYEE, ep.getEmployee().getLinkWrappedInJson(SELF));
        links.put(POSITION, ep.getPosition().getLinkWrappedInJson(SELF));
        links.put(SELF, ep.getLinkWrappedInJson(SELF));
        miniObject.put(LINKS, links);
        JSONObject dateInterval = new JSONObject();
        dateInterval.put(START_DATE, startDate.toString());
        dateInterval.put(END_DATE, endDate.toString());
        miniObject.put(DATE_INTERVAL, dateInterval);
        miniObject.put(TEMPORARY, temporary);
        try {
            miniObject.put(RATE, ep.getRate());
        } catch (JSONException e) {
            //Выпадение этой ошибки значит, что ставка не указана: на zozo, например, значение будет NaN. В таких случаях мы ловим ошибку и просто не добавляем поле "rate"
        }
        LOG.info(String.valueOf(miniObject));
        HttpResponse response = requestMaker(uri, miniObject, RequestBuilder.put(), HAL_JSON);
        assertStatusCode(response, uri.toString());
        JSONObject newEmployeePositionJSON = getJsonFromUri(Projects.WFM, uri);
        LOG.info("Сотруднику {} был выставлен признак \"временный перевод\"", ep.getEmployee().getFullName());
        Allure.addAttachment("Период работы",
                             String.format("Сотруднику %s установлен период работы с %s по %s",
                                           ep, startDate, endDate));
        if (temporary) {
            Allure.addAttachment("Признак",
                                 String.format("Сотруднику %s установлен признак \"Временный перевод\"", ep));
        }
        return getClassObjectFromJson(EmployeePosition.class, newEmployeePositionJSON);
    }

    /**
     * Меняет сотруднику категорию позиции на заданную
     */
    public static void changePosition(EmployeePosition employeePosition, PositionCategory posCat, PositionGroup posGroup, PositionType posType) {
        Position position = employeePosition.getPosition();
        URI epUri = setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(POSITIONS, position.getId()));
        JSONObject positionJSON = getJsonFromUri(Projects.WFM, epUri);
        List<String> changesLog = new ArrayList<>();
        if (posCat != null && position.getPositionCategoryId() != posCat.getCategoryId()) {
            positionJSON = changePosition(positionJSON, PositionCharacteristics.CATEGORY, posCat.getCategoryId());
            changesLog.add("должность: " + posCat.getName());
            positionJSON.remove(NAME);
            positionJSON.put(NAME, posCat.getName());
        }
        if (posGroup != null && position.getPositionGroupId() != posGroup.getId()) {
            changesLog.add("функциональная роль: " + posGroup.getName());
            positionJSON = changePosition(positionJSON, PositionCharacteristics.GROUP, posGroup.getId());
        }
        if (posType != null && position.getPositionTypeId() != posType.getId()) {
            changesLog.add("функциональная роль: " + posType.getName());
            positionJSON = changePosition(positionJSON, PositionCharacteristics.TYPE, posType.getId());
        }
        positionJSON.put(OUTER_ID, posCat.getOuterId());

        HttpResponse response = requestMaker(epUri, positionJSON, RequestBuilder.put(), HAL_JSON);
        LOG.info("Вносим в должность сотрудника {} следующие изменения: {}", employeePosition, changesLog);
        assertStatusCode(response, epUri.toString());
        changeEmployeePosition(employeePosition.getId(), positionJSON);
        Allure.addAttachment("Изменение позиции сотрудника",
                             String.format("Была изменения позиция сотрудника %s: %s",
                                           employeePosition, changesLog));
    }

    /**
     * Заменяет части JSON, получаемого по запросу /api/v1/positions/{id}
     *
     * @param json           редактируемый JSON
     * @param characteristic характеристика позиции из енама для подтягивания нужных строк для ссылок и формирования JSON
     * @param objectId       айди характеристики
     * @return - обновленный JSON
     */
    private static JSONObject changePosition(JSONObject json, PositionCharacteristics characteristic, int objectId) {
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(characteristic.getLinkPart(), objectId));
        JSONObject charJSON = getJsonFromUri(Projects.WFM, uri);

        json.remove(characteristic.getJSONPart());
        json.put(characteristic.getJSONPart(), charJSON);

        JSONObject linksJSON = json.getJSONObject(LINKS);

        linksJSON.remove(characteristic.getJSONPart());
        linksJSON.put(characteristic.getJSONPart(), charJSON.getJSONObject(LINKS).getJSONObject(SELF));
        json.remove(LINKS);
        json.put(LINKS, linksJSON);

        return json;
    }

    /**
     * Отправляет запрос PUT на /api/v1/employee-positions/{id}
     *
     * @param employeePositionId айди позиции, которую нужно изменить
     * @param positionJson       JSON позиции, привязанной к данной employeePosition
     */
    private static void changeEmployeePosition(int employeePositionId, JSONObject positionJson) {
        URI posUri = setUri(Projects.WFM, CommonRepository.URL_BASE,
                            makePath(EMPLOYEE_POSITIONS, employeePositionId));
        JSONObject employeePositionJSON = getJsonFromUri(Projects.WFM, posUri);

        employeePositionJSON.remove(CARD_NUMBER);
        employeePositionJSON.put(CARD_NUMBER, String.format("%06d", new Random().nextInt(1000000)));

        employeePositionJSON.remove(NAME);
        employeePositionJSON.remove(ID);
        employeePositionJSON.put("dismissed", JSONObject.NULL);
        JSONObject employeeJSON = employeePositionJSON.getJSONObject(EMBEDDED).getJSONObject(EMPLOYEE);
        employeeJSON.getJSONObject(LINKS).remove("account");
        employeePositionJSON.put(EMPLOYEE, employeeJSON);
        employeePositionJSON.remove(EMBEDDED);
        employeePositionJSON.put(POSITION, positionJson);
        employeePositionJSON.put(END_DATE, JSONObject.NULL);
        employeePositionJSON.put("isChief", false);
        employeePositionJSON.put("isEdit", true);
        employeePositionJSON.put("show", true);
        employeePositionJSON.put(START_DATE, LocalDateTools.getFirstDate());

        JSONObject epSelfLink = employeePositionJSON.getJSONObject(LINKS).getJSONObject(SELF);
        employeePositionJSON.remove(LINKS);
        JSONObject epLinks = new JSONObject().put(SELF, epSelfLink);
        epLinks.put(EMPLOYEE, employeeJSON.getJSONObject(LINKS).getJSONObject(SELF));
        epLinks.put(POSITION, positionJson.getJSONObject(LINKS).getJSONObject(SELF));
        employeePositionJSON.put(LINKS, epLinks);

        HttpResponse response = requestMaker(posUri, employeePositionJSON, RequestBuilder.put(), HAL_JSON);
        assertStatusCode(response, posUri.toString());
    }

    /**
     * Делает сотрудника руководителем
     *
     * @param employeePositionId айди позиции, которую нужно изменить
     */
    public static void addChief(int employeePositionId) {
        URI posUri = setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(EMPLOYEE_POSITIONS, employeePositionId));
        JSONObject employeePositionJSON = getJsonFromUri(Projects.WFM, posUri);

        employeePositionJSON.remove(NAME);
        employeePositionJSON.remove(ID);
        employeePositionJSON.put("dismissed", JSONObject.NULL);
        JSONObject employeeJSON = employeePositionJSON.getJSONObject(EMBEDDED).getJSONObject(EMPLOYEE);
        employeeJSON.getJSONObject(LINKS).remove("account");
        employeePositionJSON.put(EMPLOYEE, employeeJSON);
        JSONObject positionJson = employeePositionJSON.getJSONObject(EMBEDDED).getJSONObject(POSITION);
        employeePositionJSON.remove(EMBEDDED);
        employeePositionJSON.put(POSITION, positionJson);
        employeePositionJSON.put(END_DATE, JSONObject.NULL);
        employeePositionJSON.put(START_DATE, LocalDateTools.getFirstDate());
        employeePositionJSON.put("isChief", false);

        JSONObject epSelfLink = employeePositionJSON.getJSONObject(LINKS).getJSONObject(SELF);
        employeePositionJSON.remove(LINKS);
        JSONObject epLinks = new JSONObject().put(SELF, epSelfLink);
        epLinks.put(EMPLOYEE, employeeJSON.getJSONObject(LINKS).getJSONObject(SELF));
        epLinks.put(POSITION, positionJson.getJSONObject(LINKS).getJSONObject(SELF));
        employeePositionJSON.put(LINKS, epLinks);

        HttpResponse response = requestMaker(posUri, employeePositionJSON, RequestBuilder.put(), HAL_JSON);
        assertStatusCode(response, posUri.toString());
    }

    /**
     * Изменить табельный номер
     *
     * @param cardNumber табельный номер
     */
    public static void changeCardNumber(EmployeePosition ep, String cardNumber) {
        URI posUri = setUri(Projects.WFM, CommonRepository.URL_BASE,
                            makePath(EMPLOYEE_POSITIONS, ep.getId()));
        JSONObject employeePositionJSON = getJsonFromUri(Projects.WFM, posUri);

        employeePositionJSON.remove(CARD_NUMBER);
        employeePositionJSON.put(CARD_NUMBER, cardNumber);

        employeePositionJSON.remove(NAME);
        employeePositionJSON.remove(ID);
        employeePositionJSON.put("dismissed", JSONObject.NULL);
        JSONObject employeeJSON = employeePositionJSON.getJSONObject(EMBEDDED).getJSONObject(EMPLOYEE);
        employeeJSON.getJSONObject(LINKS).remove("account");
        employeePositionJSON.put(EMPLOYEE, employeeJSON);
        JSONObject positionJson = employeePositionJSON.getJSONObject(EMBEDDED).getJSONObject(POSITION);
        employeePositionJSON.remove(EMBEDDED);
        employeePositionJSON.put(POSITION, positionJson);
        employeePositionJSON.put(END_DATE, JSONObject.NULL);
        employeePositionJSON.put(START_DATE, LocalDateTools.getFirstDate());

        JSONObject epSelfLink = employeePositionJSON.getJSONObject(LINKS).getJSONObject(SELF);
        employeePositionJSON.remove(LINKS);
        JSONObject epLinks = new JSONObject().put(SELF, epSelfLink);
        epLinks.put(EMPLOYEE, employeeJSON.getJSONObject(LINKS).getJSONObject(SELF));
        epLinks.put(POSITION, positionJson.getJSONObject(LINKS).getJSONObject(SELF));
        employeePositionJSON.put(LINKS, epLinks);

        HttpResponse response = requestMaker(posUri, employeePositionJSON, RequestBuilder.put(), HAL_JSON);
        assertStatusCode(response, posUri.toString());
    }

    /**
     * Создает запрос на дежурство или сверхурочную работу
     *
     * @param requestType тип запроса: дежурство/сверхурочная работа
     * @param startTime   время начала запроса
     * @param ep          employeePosition, для которой создается запрос
     * @param rosterId    айди ростера, в котором создается запрос
     * @param duration    желаемая продолжительность запроса (до 4 часов включительно)
     */
    public static OutsidePlanResource createOutsidePlanResource(ScheduleRequestType requestType, LocalDateTime startTime, EmployeePosition ep, int rosterId, int duration) {
        JSONObject requestJson = new JSONObject();
        requestJson.put(TYPE, requestType.toString());
        requestJson.put(START_DATE, startTime.toLocalDate());
        requestJson.put(END_DATE, startTime.toLocalDate());

        JSONObject repeatRule = new JSONObject();
        repeatRule.put(PERIODICITY, NO_REPEAT);
        repeatRule.put(NAME, "Не повторять");

        int epId = ep.getId();
        JSONObject epJSON = getJsonFromUri(Projects.WFM,
                                           setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(EMPLOYEE_POSITIONS, epId)));

        requestJson.put(EMPLOYEE, epJSON.getJSONObject(EMBEDDED).getJSONObject(EMPLOYEE));
        requestJson.put(POSITION, epJSON.getJSONObject(EMBEDDED).getJSONObject(POSITION));
        requestJson.put(IS_OVERTIME_WORK_MODEL, true);
        requestJson.put(IS_SHIFT_MODEL, false);
        requestJson.put("statusName_", STATUS_SHIFT);
        requestJson.put(ROSTER_ID_JSON, rosterId);

        JSONObject links = new JSONObject();
        links.put(SELF, new JSONObject().put(HREF, setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(OUTSIDE_PLAN, ROSTER, rosterId)).toString()));
        links.put(EMPLOYEE_POSITION, epJSON.getJSONObject(LINKS).getJSONObject(SELF));
        links.put(ROSTER, new JSONObject().put(HREF, setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(ROSTERS, rosterId)).toString()));
        requestJson.put(LINKS, links);

        LocalDateTime endTime = startTime.plusHours(duration);
        requestJson.put(DATE_TIME_INTERVAL, new JSONObject().put(START_DATE_TIME, startTime).put(END_DATE_TIME, endTime));
        requestJson.put(EMPLOYEE_POSITION_ID, epId);

        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(OUTSIDE_PLAN, ROSTER, rosterId), Pairs.newBuilder().calculateConstraints(true).build());
        HttpResponse response = requestMaker(uri, requestJson, RequestBuilder.post(), HAL_JSON);
        assertStatusCode(response, 201, uri.toString());
        String requestName = requestType.getName();
        Allure.addAttachment(String.format("Пресет: создание запроса типа \"%s\"", requestName),
                             String.format("Для сотрудника %s создан запрос на %s по адресу %s",
                                           ep, startTime.toLocalDate(), uri));
        LOG.info(JSON_LOGGER, requestJson);
        LOG.info("Запрос на создание запроса типа \"{}\" по адресу: {}", requestName, uri);
        return getCreatedObject(response, OutsidePlanResource.class);
    }

    /**
     * Создает запрос на дежурство или сверхурочную работу
     *
     * @param requestType тип запроса: дежурство/сверхурочная работа
     * @param startTime   время начала запроса
     * @param ep          employeePosition, для которой создается запрос
     * @param rosterId    айди ростера, в котором создается запрос
     */
    public static OutsidePlanResource createOutsidePlanResource(ScheduleRequestType requestType, LocalDateTime startTime, EmployeePosition ep, int rosterId) {
        return createOutsidePlanResource(requestType, startTime, ep, rosterId, 4);
    }

    /**
     * Создает запрос на работу сверхурочно на эфесе
     *
     * @param startTime время начала запроса
     * @param ep        employeePosition, для которой создается запрос
     * @param rosterId  айди ростера, в котором создается запрос
     * @param duration  желаемая продолжительность запроса (до 4 часов включительно)
     */
    public static OutsidePlanResource createOutsidePlanResourceEFES(LocalDateTime startTime, EmployeePosition ep, int rosterId, int duration) {
        makeClearDate(ep, startTime.toLocalDate());
        JSONObject requestJson = new JSONObject();
        ScheduleRequestType requestType = ScheduleRequestType.OVERTIME_WORK;
        requestJson.put("subType", requestType.toString());
        requestJson.put(START_DATE, startTime.toLocalDate());
        requestJson.put(END_DATE, startTime.toLocalDate());
        requestJson.put(POSITION_CATEGORY_ROSTER_ID, ShiftRepository.getShiftsForRoster(rosterId, new DateInterval()).get(0).getPositionCategoryRosterId());
        JSONObject links = new JSONObject();
        links.put(SELF, new JSONObject().put(HREF, setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(SHIFTS)).toString()));
        links.put(EMPLOYEE_POSITION, ep.getLinkWrappedInJson(SELF));
        links.put(ROSTER, new JSONObject().put(HREF, setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(ROSTERS, rosterId)).toString()));
        requestJson.put(LINKS, links);
        requestJson.put(DATE_TIME_INTERVAL, new JSONObject().put(START_DATE_TIME, startTime).put(END_DATE_TIME, startTime.plusHours(duration)));
        requestJson.put(EMPLOYEE_POSITION_ID, ep.getId());
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(SHIFTS), Pairs.newBuilder().calculateConstraints(true).build());
        HttpResponse response = requestMaker(uri, requestJson, RequestBuilder.post(), HAL_JSON);
        assertStatusCode(response, 201, uri.toString());
        String requestName = requestType.getName();
        Allure.addAttachment(String.format("Пресет: создание запроса типа \"%s\"", requestName),
                             String.format("Для сотрудника %s создан запрос на %s по адресу %s",
                                           ep, startTime.toLocalDate(), uri));
        LOG.info(JSON_LOGGER, requestJson);
        LOG.info("Запрос на создание запроса типа \"{}\" по адресу: {}", requestName, uri);
        return getCreatedObject(response, OutsidePlanResource.class);
    }

    /**
     * Добавляет смены сотруднику до тех пор, пока фактическое количество часов не станет больше стандартного
     *
     * @param unitId   айди орг юнита
     * @param position позиция выбранного сотрудника
     */
    public static void presetForDeviationExcess(int unitId, EmployeePosition position) {
        double hoursDeviation = Objects.requireNonNull(
                        DeviationFromStandardRepository.getDeviation(RosterRepository.getActiveRosterThisMonth(unitId),
                                                                     RosterRepository.getWorkedRosterThisMonth(unitId), position))
                .getDeviation();
        LOG.info(HOURS_DEVIATION_LOGGER, hoursDeviation);
        List<Shift> shifts;
        while (hoursDeviation <= 0) {
            shifts = ShiftRepository.getShifts(position, ShiftTimePosition.ALLMONTH);
            if (shifts.isEmpty()) {
                PresetClass.presetForMakeShiftDate(position, LocalDate.now(), false, ShiftTimePosition.ALLMONTH, shifts);
            }
            Shift shift;
            try {
                shift = presetForMakeShiftWithoutDeleteRequest(position, false, ShiftTimePosition.FUTURE);
            } catch (AssertionError ex) {
                shift = presetForMakeShiftWithoutDeleteRequest(position, false, ShiftTimePosition.PAST);
            }
            DateTimeInterval interval = shift.getDateTimeInterval();
            long hours = interval.getStartDateTime().until(interval.getEndDateTime(), ChronoUnit.HOURS);
            String log = String.format("Была добавлена смена c id %s %s часов за %s число",
                                       shift.getId(), hours - shift.getLunch() / 60, interval.getStartDateTime());
            LOG.info(log);
            Allure.addAttachment("Пресет по созданию избытка часов", log);
            hoursDeviation += interval.getStartDateTime().until(interval.getEndDateTime(), ChronoUnit.HOURS) - (double) shift.getLunch() / 60;
            LOG.info(HOURS_DEVIATION_LOGGER, hoursDeviation);
        }
    }

    /**
     * Удаляет смены сотруднику до тех пор, пока фактическое количество часов не станет меньше стандартного
     *
     * @param position позиция выбранного сотрудника
     * @param unitId   орг юнит айди
     */
    public static void presetForDeviationLack(int unitId, EmployeePosition position) {
        DeviationFromStandard deviation = DeviationFromStandardRepository.getDeviation(RosterRepository.getActiveRosterThisMonth(unitId),
                                                                                       RosterRepository.getWorkedRosterThisMonth(unitId), position);
        double factHours = Math.round(Math.floor(deviation.getFact() * 60) / 60 * 100d) / 100d;
        double standardHours = Math.round(Math.floor(deviation.getStandard() * 60) / 60 * 100d) / 100d;
        double hoursDeviation = factHours - standardHours;
        LOG.info(HOURS_DEVIATION_LOGGER, hoursDeviation);
        List<Shift> shifts;
        List<Shift> factShifts;
        while (hoursDeviation >= 0) {
            Roster factRoster = RosterRepository.getNeededRosterId(ShiftTimePosition.PAST, new DateInterval(), unitId);
            LOG.info("factRoster: {}", factRoster.getId());
            factShifts = ShiftRepository.getShifts(position, ShiftTimePosition.PAST);
            shifts = ShiftRepository.getShifts(position, ShiftTimePosition.FUTURE);
            shifts.addAll(factShifts);
            Shift shift = getRandomFromList(shifts);
            DateTimeInterval interval = shift.getDateTimeInterval();
            long hours = interval.getStartDateTime().until(interval.getEndDateTime(), ChronoUnit.HOURS);
            deleteRequest(shift);
            String log = String.format("Было удалено %s часов смены c id %s за %s число",
                                       hours - (double) shift.getLunch() / 60, shift.getId(),
                                       interval.getStartDateTime());
            LOG.info(log);
            Allure.addAttachment("Пресет по созданию нехватки часов", log);
            hoursDeviation -= hours - (double) shift.getLunch() / 60;
        }
    }

    /**
     * Ищет сотрудника с определенной ролью. Если у сотрудника не проставлены даты начала для любой из имеющихся ролей, добавляет их.
     */
    public static Employee getEmployeesWithCertainRolesAndStartDatesForAllRoles(UserRole role) {
        List<Employee> employees = EmployeeRepository.getEmployeesByRoleId(role.getId());
        while (!employees.isEmpty()) {
            Employee emp = getRandomFromList(employees);
            User user = emp.getUser();
            List<User.RoleInUser> userRoles = user.getRoles();
            List<User.RoleInUser> noStartDate = userRoles.stream().filter(userRole -> userRole.getStartRoleDate() == null).collect(Collectors.toList());
            if (noStartDate.isEmpty()) {
                return emp;
            } else {
                for (User.RoleInUser roleInUser : noStartDate) {
                    UserRole roleToUpdate = UserRoleRepository.getUserRoleById(roleInUser.getUserRoleId());
                    addRoleToUser(user, roleToUpdate, roleInUser.getOrgUnitList(), roleInUser.getEndRoleDate());
                }
            }
        }
        throw new AssertionError(String.format("%sНе найдено подходящих сотрудников с ролью %s", NO_TEST_DATA, role.getName()));
    }

    /**
     * Добавляет к роли права на определенный тип запроса расписания
     *
     * @param role роль, к которой нужно добавить права
     * @param type тип запроса, на который добавляем права
     */
    public static void addScheduleRequestTypeRights(Role role, ScheduleRequestType type) {
        ScheduleRequestAlias alias = ScheduleRequestAliasRepository.getAlias(type);
        List<UserRole> userRoles = UserRoleRepository.getUserRoles();
        UserRole userRole = userRoles.stream()
                .filter(u -> u.getName().equals(role.getName()))
                .findFirst()
                .orElseGet(() -> addUserRole(role.getName()));
        int userRoleId = userRole.getId();
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(USER_ROLES, userRoleId, SCHEDULE_REQUEST));
        JSONObject initialJson = getJsonFromUri(Projects.WFM, uri);
        if (initialJson.length() != 0) {
            JSONArray array = initialJson.getJSONObject(EMBEDDED).getJSONArray("scheduleRequests");
            for (Object requestObject : array) {
                JSONObject requestJson = (JSONObject) requestObject;
                String string = requestJson.getString(SCHEDULE_REQUEST_TYPE);
                JSONObject aliasLink = requestJson.getJSONObject(LINKS).optJSONObject(SCHEDULE_REQUEST_ALIAS);
                String rosterType = requestJson.optString(ROSTER_TYPE);
                if (string.equals(alias.getType()) && aliasLink != null && alias.getSelfLink().equals(aliasLink.optString(HREF))
                        && Objects.equals(RosterTypes.ANY.getValue(), rosterType)) {
                    LOG.info("У роли уже есть разрешение на запросы расписания типа \"{}\"", alias.getType());
                    Allure.addAttachment("Пресет для добавления прав на тип запроса расписания пользователю",
                                         String.format("У роли уже есть права на запросы расписания типа \"%s\"", alias.getTitle()));
                    return;
                }
            }
        }
        JSONObject miniObject = new JSONObject();
        JSONObject links = new JSONObject();
        links.put(SCHEDULE_REQUEST_ALIAS, alias.getLinkWrappedInJson(SELF));
        links.put(SELF, new JSONObject().put(HREF, uri.toString()));
        miniObject.put(ROSTER_TYPE, JSONObject.NULL);
        miniObject.put(LINKS, links);
        HttpResponse response = requestMaker(uri, miniObject, RequestBuilder.post(), HAL_JSON);
        LOG.info(REQUEST_LOGGER, uri);
        LOG.info(JSON_LOGGER, miniObject);
        assertStatusCode(response, uri.toString());
        Allure.addAttachment("Пресет для добавления прав на тип запроса расписания пользователю",
                             String.format("Были добавлены права на запросы расписания типа \"%s\"", alias.getTitle()));
    }

    /**
     * Удаляет у роли права на определенный тип запроса расписания
     *
     * @param role роль, у которой нужно удалить права
     * @param type тип запроса, на который удаляем права
     */
    public static void revokeScheduleRequestTypeRights(Role role, ScheduleRequestType type) {
        ScheduleRequestAlias requestType = ScheduleRequestAliasRepository.getAlias(type);
        List<UserRole> userRoles = UserRoleRepository.getUserRoles();
        UserRole userRole = userRoles.stream()
                .filter(u -> u.getName().equals(role.getName()))
                .findFirst()
                .orElse(null);
        if (userRole == null) {
            return;
        }
        int userRoleId = userRole.getId();
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(USER_ROLES, userRoleId, ALIAS, requestType.getAlias()));
        LOG.info(REQUEST_LOGGER, uri);
        deleteRequest(uri);
        Allure.addAttachment("Пресет для удаления прав на тип запроса расписания пользователю",
                             String.format("Были удалены права на запросы расписания типа \"%s\"", requestType.getTitle()));
    }

    /**
     * @param driver
     * @return eventFiringWebDriver
     * Метод оборачивает driver:
     * driver = PresetClass.addEventListenerForNavigate(driver)
     * для возможности получения событий Navigate драйвера.(Остальные события легко будет добавить в
     * класс WebDriverEventCapture)
     * <p>
     * События отлавливаются классом ResponseEventWebdriver.
     * TODO не успел доработать и проверить работоспособность всего этого.
     */

    public static EventFiringWebDriver addEventListenerForNavigate(WebDriver driver) {
        EventFiringWebDriver eventFiringWebDriver = new EventFiringWebDriver(driver);
        WebDriverEventCapture webDriverEventCapture = new WebDriverEventCapture();
        eventFiringWebDriver.register(webDriverEventCapture);
        ResponseEventWebdriver responseEventWebdriver = new ResponseEventWebdriver();
        webDriverEventCapture.registerCallback(responseEventWebdriver);
        webDriverEventCapture.doSomething(null);
        return eventFiringWebDriver;
    }

    /**
     * Добавляет причину привлечения сотрудника
     *
     * @return причина привлечения
     */
    public static ShiftHiringReason addShiftHiringReason() {
        String reasonText = "testReason_" + RandomStringUtils.randomAlphabetic(10);
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, SHIFT_HIRING_REASON);
        JSONObject reasonJson = new JSONObject().put(TITLE, reasonText);
        reasonJson.put(LINKS, new JSONObject().put(SELF, new JSONObject().put(HREF, uri)));
        HttpResponse response = RequestFormers.requestMaker(uri, reasonJson, RequestBuilder.post(), HAL_JSON);
        LOG.info("Создаем причину привлечения сотрудника: {}", reasonText);
        LOG.info(REQUEST_LOGGER, uri);
        LOG.info(JSON_LOGGER, reasonJson);
        assertStatusCode(response, 201, uri.toString());
        ShiftHiringReason reason = getCreatedObject(response, ShiftHiringReason.class);
        Allure.addAttachment("Создание случайной причины привлечения", reason.getTitle());
        return reason;
    }

    /**
     * Добавляет к причине привлечения сотрудника атрибут подразделения
     */
    public static void addEntityPropertyToShiftHiringReason(ShiftHiringReason reason, EntityPropertiesKey property, Object value) {
        JSONObject miniObject = new JSONObject();
        miniObject.put(PROP_KEY, property.getKey());
        miniObject.put(VALUE, value);
        miniObject.put(TYPE, property.getDataType());
        JSONObject propertiesKeyLink = property.getLinkWrappedInJson(SELF);
        miniObject.put(LINKS, new JSONObject().put(Params.ENTITY_PROPERTIES_KEY, propertiesKeyLink));
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(SHIFT_HIRING_REASON, reason.getId(), ENTITY_PROPERTIES));
        HttpResponse response = requestMaker(uri, miniObject, RequestBuilder.post(), HAL_JSON);
        LOG.info("Добавляем атрибут \"{}\" со значением \"{}\" к причине \"{}\"", property.getTitle(), value, reason.getTitle());
        LOG.info(REQUEST_LOGGER, uri);
        LOG.info(JSON_LOGGER, miniObject);
        assertStatusCode(response, 201, uri.toString());
        Allure.addAttachment("Добавление атрибута подразделения к причине привлечения сотрудника",
                             String.format("Добавлен атрибут \"%s\" со значением \"%s\" к причине \"%s\"", property.getTitle(), value, reason.getTitle()));
    }

    /**
     * Создает и удаляет смены в активном и опубликованном графиках для сотрудника
     *
     * @param ep               сотрудник, сменами которого манипулируем
     * @param freeShiftDay     день, в который нужны/не нужны смены
     * @param shiftInPublished нужна ли смена в опубликованном графике
     * @param shiftInActive    нужна ли смена в активном графике
     * @param omId             айди подразделения
     */
    public static void prepareShifts(EmployeePosition ep, LocalDate freeShiftDay, boolean shiftInPublished, boolean shiftInActive, int omId) {
        makeClearDate(ep, freeShiftDay);
        if (shiftInPublished) {
            PresetClass.presetForMakeShiftDate(ep, freeShiftDay, false, ShiftTimePosition.FUTURE);
        }
        Roster activeRoster = RosterRepository.getActiveRosterThisMonth(omId);
        rosterPublishCheck(activeRoster.getId());
        if (shiftInActive) {
            PresetClass.presetForMakeShiftDate(ep, freeShiftDay, false, ShiftTimePosition.FUTURE);
        }
        LOG.info("Подготовка смен сотрудника {} за {}: смена в опубликованном графике: {}, смена в активном графике: {}",
                 ep, freeShiftDay, shiftInPublished, shiftInActive);
        Allure.addAttachment(String.format("Подготовка смен сотрудника %s за %s", ep, freeShiftDay),
                             String.format("Смена в опубликованном графике: %s, смена в активном графике: %s", shiftInPublished, shiftInActive));
    }

    /**
     * Берет случайный тег подразделения, присваивает его заданным подразделению и сотруднику
     */
    public static void addRandomTagToEmployeeAndOrgUnit(OrgUnit unit, Employee employee) {
        String tag = RandomStringUtils.randomAlphabetic(10);
        PresetClass.addTagForOrgUnit(unit, tag);
        PresetClass.addTagForEmployee(employee, tag);
    }

    /**
     * Создает причину привлечения сотрудника, берет случайный атрибут подразделения со строковым значением, добавляет атрибут к причине
     *
     * @param omId ID подразделения
     * @return причина привлечения
     */
    public static <T> ImmutablePair<ShiftHiringReason, EntityProperty<T>> setupHiringReasonAndEntityPropertyForOrgUnitAndReturnWithKey(int omId) {
        ShiftHiringReason reason = addShiftHiringReason();
        String value = RandomStringUtils.randomAlphabetic(5);
        EntityProperty<T> property = addAttributeToEntity(MathParameterEntities.ORGANIZATION_UNIT, omId, OrgUnitAttributes.ORG_UNIT_FORMAT, value);
        String keys = MathParameterEntities.ORGANIZATION_UNIT.getKeys();
        EntityPropertiesKey key = EntityPropertyKeyRepository.getPropertyByKey(keys, property.getPropKey());
        addEntityPropertyToShiftHiringReason(reason, key, value);
        return new ImmutablePair<>(reason, property);
    }

    public static ShiftHiringReason setupHiringReasonAndEntityPropertyForOrgUnit(int omId) {
        return setupHiringReasonAndEntityPropertyForOrgUnitAndReturnWithKey(omId).left;
    }

    /**
     * Удалить все причины привлечения на стенде
     */
    private static void deleteAllHiringReasons() {
        List<ShiftHiringReason> allReasons = ShiftHiringReasonRepository.getShiftHiringReasons();
        for (ShiftHiringReason reason : allReasons) {
            PresetClass.deleteRequest(reason);
        }
    }

    public static ShiftHiringReason setupHiringReasons(int omId, int numberOfReasons) {
        if (numberOfReasons == 0) {
            return setupNoHiringReasons(omId);
        } else if (numberOfReasons == 1) {
            return setupOneHiringReason(omId);
        } else {
            return setupTwoHiringReasons(omId);
        }
    }

    /**
     * Назначить подразделению такой атрибут, чтобы он не соответствовал ни одной причине привлечения сотрудника
     */
    private static ShiftHiringReason setupNoHiringReasons(int omId) {
        deleteAllHiringReasons();
        ShiftHiringReason reason = PresetClass.addShiftHiringReason();
        EntityPropertiesKey key = EntityPropertyKeyRepository.getAllProperties(MathParameterEntities.ORGANIZATION_UNIT.getKeys())
                .stream()
                .filter(k -> k.getDataType().equals(STRING))
                .collect(randomItem());
        PresetClass.addEntityPropertyToShiftHiringReason(reason, key, RandomStringUtils.randomAlphabetic(2));
        PresetClass.addEntityPropertyValue(MathParameterEntities.ORGANIZATION_UNIT, omId, key, RandomStringUtils.randomAlphabetic(6));
        return reason;
    }

    public static ShiftHiringReason setupHiringReasonForMagnit(int omId, int numberOfReasons) {
        EntityPropertiesKey key = EntityPropertyKeyRepository.getPropertyByKey(MathParameterEntities.ORGANIZATION_UNIT.getKeys(), ORG_UNIT_FORMAT);
        EntityProperty<String> property = EntityPropertyRepository.getEntityPropertyByKey(MathParameterEntities.ORGANIZATION_UNIT, omId, ORG_UNIT_FORMAT);
        if (property == null) {
            property = addAttributeToEntity(MathParameterEntities.ORGANIZATION_UNIT, omId, OrgUnitAttributes.ORG_UNIT_FORMAT, "ММ");
        }
        String value = property.getValue().toString();
        List<NameValuePair> pairs = Pairs.newBuilder().size(1000).build();
        JSONArray formats = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, makePath(Links.ENTITY_PROPERTIES_VALUE, key.getSelfId(), VALUES), pairs).getJSONObject(EMBEDDED).getJSONArray("attributeValues");
        List<AttributeValue> attributeValues = CustomTools.getListFromJsonArray(formats, AttributeValue.class);
        Predicate<AttributeValue> predicate = numberOfReasons == 0
                ? attr -> !attr.getValue().equals(value)
                : attr -> attr.getValue().equals(value);
        AttributeValue attributeValue = attributeValues.stream()
                .filter(predicate)
                .findAny()
                .orElseThrow(() -> new AssertionError("У подразделения нет атрибута \"Принадлежность подразделения\""));
        ShiftHiringReason reason = PresetClass.addShiftHiringReason();
        PresetClass.addEntityPropertyToShiftHiringReason(reason, key, attributeValue);
        if (numberOfReasons > 1) {
            ShiftHiringReason secondReason = PresetClass.addShiftHiringReason();
            PresetClass.addEntityPropertyToShiftHiringReason(secondReason, key, attributeValue);
        }
        return reason;
    }

    /**
     * Назначить подразделению такой атрибут, чтобы он соответствовал ровно одной причине привлечения сотрудника
     */
    private static <T> ShiftHiringReason setupOneHiringReason(int omId) {
        deleteAllHiringReasons();
        ImmutablePair<ShiftHiringReason, EntityProperty<T>> pair = PresetClass.setupHiringReasonAndEntityPropertyForOrgUnitAndReturnWithKey(omId);
        ShiftHiringReason reason = pair.left;
        EntityProperty<T> p = pair.right;
        List<ShiftHiringReason> allReasons = ShiftHiringReasonRepository.getShiftHiringReasons();
        allReasons.remove(reason);
        for (ShiftHiringReason r : allReasons) {
            r.getAttachedEntityProperties()
                    .stream()
                    .filter(e -> e.getPropKey().equals(p.getPropKey()))
                    .forEach(PresetClass::deleteRequest);
        }
        return reason;
    }

    /**
     * Назначить подразделению такой атрибут, чтобы он соответствовал нескольким причинам привлечения сотрудника
     */
    private static <T> ShiftHiringReason setupTwoHiringReasons(int omId) {
        deleteAllHiringReasons();
        ShiftHiringReason initialReason = PresetClass.setupHiringReasonAndEntityPropertyForOrgUnit(omId);
        ShiftHiringReason secondReason = PresetClass.addShiftHiringReason();
        EntityProperty<T> property = getRandomFromList(initialReason.getAttachedEntityProperties());
        EntityPropertiesKey key = EntityPropertyKeyRepository.getAllProperties(MathParameterEntities.ORGANIZATION_UNIT.getKeys())
                .stream()
                .filter(k -> k.getKey().equals(property.getPropKey()))
                .collect(randomItem());
        PresetClass.addEntityPropertyToShiftHiringReason(secondReason, key, property.getValue());
        PresetClass.addEntityPropertyValue(MathParameterEntities.ORGANIZATION_UNIT, omId, key, property.getValue());
        return secondReason;
    }

    /**
     * Добавляет заданный тип запроса расписания
     *
     * @param type           тип создаваемого запроса
     * @param intervalType   тип интервала для запросов неявки
     * @param bindToPosition true - привязка к назначению, false - привязка к физ.лицу
     */
    public static ScheduleRequestAlias addScheduleRequestType(ScheduleRequestType type, IntervalType intervalType, boolean bindToPosition) {
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(SCHEDULE_REQUESTS, ALIAS));
        JSONObject miniObject = new JSONObject();
        miniObject.put(BIND_TO_POSITION, bindToPosition);
        miniObject.put(ENABLED, true);
        miniObject.put(SHORT_NAME, type.getShortName());
        miniObject.put(LINKS, new JSONObject().put(SELF, new JSONObject().put(HREF, uri)));
        StringBuffer typeName = new StringBuffer(type.getName());
        if (intervalType != null) {
            miniObject.put(INTERVAL_TYPE, intervalType.toString());
            typeName.append("_").append(intervalType);
        }
        miniObject.put(TITLE, typeName.toString());
        miniObject.put(TYPE, type.toString());
        miniObject.put(OUTER_ID, RandomStringUtils.randomNumeric(18));
        HttpResponse response = requestMaker(uri, miniObject, RequestBuilder.post(), HAL_JSON);
        LOG.info("Создаем тип запроса расписания \"{}\" с типом интервала \"{}\"", type.getName(), intervalType);
        LOG.info(REQUEST_LOGGER, uri);
        LOG.info(JSON_LOGGER, miniObject);
        assertStatusCode(response, 201, uri.toString());
        Allure.addAttachment("Создание типа запроса расписания",
                             String.format("Создан тип запроса \"%s\" с типом интервала \"%s\"", type.getName(), intervalType));
        return getCreatedObject(response, ScheduleRequestAlias.class);
    }

    /**
     * Добавляет заданный тип запроса расписания
     *
     * @param type тип создаваемого запроса
     */
    public static ScheduleRequestAlias addScheduleRequestType(ScheduleRequestType type) {
        return addScheduleRequestType(type, null, true);
    }

    /**
     * Удаляет заданный тип запроса расписания
     */
    public static void deleteScheduleRequestType(ScheduleRequestAlias alias) {
        LOG.info("Удаляем тип запроса расписания \"{}\" с типом интервала", alias.getType());
        deleteRequest(alias);
        Allure.addAttachment("Удаление типа запроса расписания",
                             String.format("Удален тип запроса \"%s\"", alias.getType()));

    }

    /**
     * Создает правило табеля
     */
    public static TableRule addTableRule(Integer deepEdit, LocalTime timeEdit, JSONArray fixedDays, String value, TableRuleStrategy strategy,
                                         int omId, TableRuleShiftType shiftType, LocalDate date) {
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, TIMESHEET_EDIT_RULE);
        JSONObject miniObject = new JSONObject();
        if (deepEdit != null) {
            if (deepEdit != -1) {
                miniObject.put(DEEP_EDIT, deepEdit);
            }
        }
        if (fixedDays != null) {
            miniObject.put(FIXED_FAYS, fixedDays);
        }
        if (timeEdit != null) {
            miniObject.put(TIME_EDIT, timeEdit.truncatedTo(ChronoUnit.MINUTES));
        }
        miniObject.put(SHIFT_TYPE, shiftType);
        miniObject.put(VALUE, value);
        miniObject.put(STRATEGY, strategy.name());
        miniObject.put(ORG_SELF, Collections.singletonList(omId));
        miniObject.put(START_DATE, date);
        miniObject.put(END_DATE, date.withDayOfYear(date.lengthOfYear()));
        HttpResponse response = requestMaker(uri, miniObject, RequestBuilder.post(), HAL_JSON);
        LOG.info("Добавляем правило табеля");
        LOG.info(REQUEST_LOGGER, uri);
        LOG.info(JSON_LOGGER, miniObject);
        assertStatusCode(response, uri.toString());
        Allure.addAttachment("Создание правила табеля", miniObject.toString());
        ITestContext c = Reporter.getCurrentTestResult().getTestContext();
        TableRule rule = TableRuleRepository.getRuleByValue(value);
        c.setAttribute(TEST_TABLE_RULE, rule.getSelfLink());
        return rule;
    }

    public static void deleteTestTableRule(ITestContext c) {
        String rule = String.valueOf(c.getAttribute(TEST_TABLE_RULE));
        if (!rule.equals("null")) {
            PresetClass.deleteRequest(rule);
            c.removeAttribute(TEST_TABLE_RULE);
        }
    }

    /**
     * Добавляет атрибут в системные списки
     */
    public static EntityPropertiesKey addKey(String keyLink, String key, String title, String dataType, boolean forCalculate) {
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(Links.ENTITY_PROPERTIES_KEY, keyLink));
        JSONObject miniObject = new JSONObject();
        miniObject.put(DATA_TYPE, dataType);
        miniObject.put(FOR_CALCULATE, forCalculate);
        miniObject.put(KEY, key);
        miniObject.put(TITLE, title);
        JSONObject links = new JSONObject();
        links.put(SELF, new JSONObject().put(HREF, setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(Links.ENTITY_PROPERTIES_KEY, keyLink))));
        miniObject.put(LINKS, links);
        HttpResponse response = requestMaker(uri, miniObject, RequestBuilder.post(), HAL_JSON);
        LOG.info("Добавляем атрибут \"{}\" с ключом \"{}\" в системные списки", title, key);
        LOG.info(REQUEST_LOGGER, uri);
        LOG.info(JSON_LOGGER, miniObject);
        assertStatusCode(response, 201, uri.toString());
        Allure.addAttachment("Добавление атрибута в системные списки", String.format("Был добавлен атрибут \"%s\" с ключом \"%s\"", title, key));
        return getCreatedObject(response, EntityPropertiesKey.class);
    }

    /**
     * Добавляет атрибут в системные списки
     */
    public static EntityPropertiesKey addKey(String keyLink, String key, String title, String dataType, String display) {
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(Links.ENTITY_PROPERTIES_KEY, keyLink));
        JSONObject miniObject = new JSONObject();
        miniObject.put(DATA_TYPE, dataType);
        miniObject.put(DISPLAY, display);
        miniObject.put(KEY, key);
        miniObject.put(TITLE, title);
        JSONObject links = new JSONObject();
        links.put(SELF, new JSONObject().put(HREF, setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(Links.ENTITY_PROPERTIES_KEY, keyLink))));
        miniObject.put(LINKS, links);
        HttpResponse response = requestMaker(uri, miniObject, RequestBuilder.post(), HAL_JSON);
        LOG.info("Добавляем атрибут \"{}\" с ключом \"{}\" и параметром display \"{}\" в системные списки", title, key, display);
        LOG.info(REQUEST_LOGGER, uri);
        LOG.info(JSON_LOGGER, miniObject);
        assertStatusCode(response, 201, uri.toString());
        Allure.addAttachment("Добавление атрибута в системные списки",
                             String.format("Был добавлен атрибут \"%s\" с ключом \"%s\" и параметром display \"%s\"", title, key, display));
        return getCreatedObject(response, EntityPropertiesKey.class);
    }

    /**
     * Меняет значение заданного атрибута для сущности
     *
     * @param entity сущность (подразделение или сотрудник)
     * @param id     идентификатор сущности
     */
    public static <T> void changeEntityPropertyValue(MathParameterEntities entity, int id, EntityProperty<T> property, T value) {
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(entity.getLink(), id, ENTITY_PROPERTIES, property.getPropKey()));
        JSONObject valueObject = OrgUnitRepository.getValueInEntityPropertiesInOrgUnit(id);
        String startValue = valueObject == null ? "" : valueObject.getString(VALUE);
        JSONObject miniObject = new JSONObject();
        miniObject.put(VALUE, value);
        JSONObject keyObject = getJsonFromUri(Projects.WFM, property.getLink(Params.ENTITY_PROPERTIES_KEY));
        miniObject.put(KEY, keyObject);
        miniObject.put(LINKS, property.getLinks());
        miniObject.put(DELETED, false);
        miniObject.put(TYPE, property.getType());
        miniObject.put(PROP_KEY, property.getTitle());
        HttpResponse response = requestMaker(uri, miniObject, RequestBuilder.put(), HAL_JSON);
        LOG.info("Меняем значение атрибута \"{}\" c \"{}\" на \"{}\"", property.getTitle(), startValue, value);
        LOG.info(REQUEST_LOGGER, uri);
        LOG.info(JSON_LOGGER, miniObject);
        assertStatusCode(response, uri.toString());
        Allure.addAttachment("Изменение значения атрибута", String.format("Значение атрибута \"%s\" изменено c \"%s\" на \"%s\"", property.getTitle(), startValue, value));
    }

    /**
     * Присваивает атрибут и задает его значение
     *
     * @param entity сущность (подразделение или сотрудник)
     * @param id     идентификатор сущности
     */
    public static <T> void addEntityPropertyValue(MathParameterEntities entity, int id, EntityPropertiesKey property, T value) {
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(entity.getLink(), id, ENTITY_PROPERTIES));
        JSONObject miniObject = new JSONObject();
        miniObject.put(VALUE, value);

        JSONObject keyObject = getJsonFromUri(Projects.WFM, property.getSelfLink());
        miniObject.put(KEY, keyObject);
        miniObject.put(TYPE, property.getDataType());
        miniObject.put(PROP_KEY, property.getTitle());

        JSONObject links = new JSONObject();
        links.put(Params.ENTITY_PROPERTIES_KEY, property.getLinkWrappedInJson(SELF));
        links.put(SELF, new JSONObject().put(HREF, setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(entity.getLink(), id, ENTITY_PROPERTIES))));
        miniObject.put(LINKS, links);
        HttpResponse response = requestMaker(uri, miniObject, RequestBuilder.post(), HAL_JSON);
        LOG.info("Добавляем атрибут {} со значением {}", property.getTitle(), value);
        LOG.info(REQUEST_LOGGER, uri);
        LOG.info(JSON_LOGGER, miniObject);
        assertStatusCode(response, 201, uri.toString());
        Allure.addAttachment("Присвоение атрибута",
                             String.format("Задан атрибут \"%s\" со значением \"%s\"",
                                           property.getTitle(), value));
    }

    /**
     * Собирает и отправляет запрос на удаление атрибута у подразделения
     *
     * @param omId     айди подразделения
     * @param property объект атрибута
     */
    public static <T> void deleteEntityPropertyValue(int omId, EntityProperty<T> property) {
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(ORG_UNITS, omId, ENTITY_PROPERTIES, property.getPropKey()));
        LOG.info("Удаляем атрибут {}", property.getTitle());
        deleteRequest(uri);
        Allure.addAttachment("Удаление атрибута подразделения",
                             String.format("У подразделения удален атрибут \"%s\"", property.getTitle()));
    }

    public static <T> void deleteEntityPropertyValue(int omId, MathParameterEntities entity, EntityProperty<T> property) {
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(entity.getLink(), omId, ENTITY_PROPERTIES, property.getPropKey()));
        LOG.info("Удаляем атрибут {}", property.getTitle());
        deleteRequest(uri);
        Allure.addAttachment("Удаление атрибута",
                             String.format("У сущности удален атрибут \"%s\"", property.getTitle()));
    }

    /**
     * Удаляет атрибут подразделения, если он задан
     *
     * @param omId         айди подразделения
     * @param propertyName название (ключ) атрибута
     */
    public static void deleteEntityPropertyIfPresent(int omId, String propertyName) {
        EntityPropertyRepository.getAllPropertiesFromUnit(omId)
                .stream()
                .filter(e -> e.getPropKey().equals(propertyName))
                .findFirst().ifPresent(property -> deleteEntityPropertyValue(omId, property));
        Allure.addAttachment("Удаление атрибута подразделения", String.format("У подразделения с ID %s удален атрибут \"%s\"", omId, propertyName));
    }

    public static void deleteEntityPropertyIfPresent(int id, MathParameterEntities entity, String propertyName) {
        EntityPropertyRepository.getAllPropertiesFromEntity(entity, id)
                .stream()
                .filter(e -> e.getPropKey().equals(propertyName))
                .findFirst().ifPresent(property -> deleteEntityPropertyValue(id, entity, property));
    }

    /**
     * Добавляет правило табеля с заданными значениями указанному подразделению. Если для подразделения не указан атрибут org_unit_format, то он присваивается
     *
     * @param omId      айди подразделения
     * @param deepEdit  количество дней в прошлом
     * @param fixedDays дни блокировки
     */
    public static void addTableRuleToOrgUnit(int omId, Integer deepEdit, LocalTime timeEdit, List<Integer> fixedDays, TableRuleStrategy strategy, TableRuleShiftType shiftType) {
        String ruleName = setUnitOwnershipValue(omId);
        List<TableRule> rules = TableRuleRepository.getRuleForOrgUnit(omId).stream()
                .filter(r -> r.getDeepEdit() != 0)
                .collect(Collectors.toList());
        if (!rules.isEmpty()) {
            PresetClass.removeTableRules(rules);
        }
        PresetClass.addTableRule(deepEdit, timeEdit, new JSONArray(fixedDays), ruleName, strategy, omId, shiftType, LocalDate.now().withDayOfMonth(1));
        String notSpecified = "не задано";
        Allure.addAttachment("Добавление правила табеля", String.format(
                "Название правила: %s\n" +
                        "Кол-во дней в прошлом: %s\n" +
                        "Дни блокировки: %s\n" +
                        "Время блокировки: %s\n" +
                        "Логика блокировки: %s\n" +
                        "Для чего настройка: %s",
                ruleName, deepEdit,
                fixedDays == null ? notSpecified : fixedDays,
                timeEdit == null ? notSpecified : timeEdit,
                strategy.getString(),
                shiftType));
    }

    public static void addTableRuleToOrgUnit(int omId, int deepEdit, LocalTime timeEdit, List<Integer> fixedDays, TableRuleShiftType shiftType) {
        addTableRuleToOrgUnit(omId, deepEdit, timeEdit, fixedDays, TableRuleStrategy.UP_TO_DATE, shiftType);
    }

    /**
     * Создать правило биржи смен аттрибута принадлежности подразделения
     *
     * @param outside          - смен вне подразделения
     * @param jobTitleShift    - должность, на кот. назначается
     * @param jobTitleEmployee - должность, с кот. назначается
     */
    public static void createRule(boolean outside, ShiftHiringReason shiftHiringReason,
                                  JobTitle jobTitleShift, JobTitle jobTitleEmployee, int omId) {
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, EXCHANGE_RULE_MG);
        EntityProperty property = EntityPropertyRepository.getEntityPropertyByKey(MathParameterEntities.ORGANIZATION_UNIT, omId, ORG_UNIT_FORMAT);
        if (property == null) {
            setUnitOwnershipValue(omId);
            property = EntityPropertyRepository.getEntityPropertyByKey(MathParameterEntities.ORGANIZATION_UNIT, omId, ORG_UNIT_FORMAT);
        }
        String unitAffiliation = property.getValue().toString();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(OUTSIDE, outside);
        jsonObject.put(SHIFT_VALUE, unitAffiliation);
        jsonObject.put(EMPLOYEE_VALUE, unitAffiliation);

        JSONObject links = new JSONObject();
        JSONObject attribute = new JSONObject();
        attribute.put(HREF, property.getLink(Params.ENTITY_PROPERTIES_KEY));
        links.put("attribute", attribute);

        JSONObject hiringReason = new JSONObject();
        hiringReason.put(HREF, shiftHiringReason.getLink(SELF));
        links.put("shiftHiringReason", hiringReason);

        JSONObject shiftJobTitle = new JSONObject();
        shiftJobTitle.put(HREF, jobTitleShift.getLink(SELF));
        links.put(JOB_TITLE_SHIFT, shiftJobTitle);

        JSONObject jobTitleEmp = new JSONObject();
        jobTitleEmp.put(HREF, jobTitleEmployee.getLink(SELF));
        links.put(JOB_TITLE_EMPLOYEE, jobTitleEmp);
        jsonObject.put(LINKS, links);

        HttpResponse response = requestMaker(uri, jsonObject, RequestBuilder.post(), HAL_JSON);
        LOG.info(REQUEST_LOGGER, uri);
        LOG.info(JSON_LOGGER, jsonObject);
        assertStatusCode(response, 200, uri.toString());
        Allure.addAttachment("Создано правило биржи смен",
                             String.format("Задан атрибут \"%s\" со значением \"%s\", с причиной привлечения \"%s\" для подразделения с id: \"%s\"",
                                           property.getTitle(), unitAffiliation, shiftHiringReason.getTitle(), omId));
    }

    /**
     * Создает пустой ростер для создания смен через мастер планирования
     *
     * @param omId id орг юнита
     */
    public static void createEmptyPlannedRoster(int omId, LocalDate firstDate) {
        int[] omIdArray = {omId};
        String stand = CommonRepository.URL_BASE;
        String uri = makePath(stand, Projects.JOBAPP.getApi(), CREATE_EMPTY_PLANNED_ROSTER);
        URI uriPreset = URI.create(uri);
        JSONObject miniObject = new JSONObject();
        miniObject.put(DATE, firstDate);
        miniObject.put(stand.contains("pochta") ? ORG_UNIT_CHILDREN : REL_ORG_UNIT_SELF, omIdArray);
        HttpResponse response = stand.contains("magnitqa") ? requestMaker(uriPreset, miniObject, RequestBuilder.post(), HAL_JSON, Projects.WFM, ImmutablePair.of("Wfm-Internal", getToken())) : requestMaker(uriPreset, miniObject, RequestBuilder.post(), HAL_JSON);
        LOG.info(REQUEST_LOGGER, uriPreset);
        LOG.info(JSON_LOGGER, miniObject);
        assertStatusCode(response, 200, uriPreset.toString());
        Allure.addAttachment("Пресет для создания пустого ростера", "Был добавлен пустой ростер для создания смен через \"Мастер планирования\"");
    }

    public static void createEmptyPlannedRoster(int omId) {
        createEmptyPlannedRoster(omId, LocalDateTools.getFirstDate());
    }

    /**
     * Берет со стенда любую доп. работу. Если такой доп. работы нет, создает новую доп. работу.
     *
     * @param hasStatuses Нужны ли статусы у доп. работы
     */
    public static AdditionalWork getRandomAdditionalWork(boolean hasStatuses) {
        AdditionalWork addWork;
        String status;
        if (hasStatuses) {
            addWork = AdditionalWorkRepository.getRandomAdditionalWorkWithStatuses();
            status = "со статусами";
        } else {
            addWork = AdditionalWorkRepository.getRandomAdditionalWorkWithoutStatuses();
            status = "без статусов";
        }
        if (addWork == null) {
            return addAdditionalWork(hasStatuses);
        } else {
            LOG.info("Выбрана доп. работа {} \"{}\"", status, addWork.getTitle());
            Allure.addAttachment("Выбор доп. работы", String.format("Доп.работа \"%s\" %s", addWork.getTitle(), status));
            return addWork;
        }
    }

    /**
     * Добавляет статус к доп. работе
     *
     * @param addWork доп. работа, к которой нужно добавить статус
     * @param name    название статуса
     */
    public static void setStatusToAdditionalWork(AdditionalWork addWork, String name) {
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(SHIFTS_ADD_WORK, addWork.getId(), STATUSES));
        JSONObject miniObject = new JSONObject();
        miniObject.put("addWorkId", addWork.getId());
        miniObject.put(STATUS, name);
        HttpResponse response = requestMaker(uri, miniObject, RequestBuilder.post(), HAL_JSON);
        LOG.info("Добавляем к доп. работе статус \"{}\"", name);
        LOG.info(REQUEST_LOGGER, uri);
        LOG.info(JSON_LOGGER, miniObject);
        assertStatusCode(response, 201, uri.toString());
        Allure.addAttachment("Добавление статуса к доп. работе", name);
    }

    /**
     * Добавляет тип доп. работы в системный справочник
     */
    public static AdditionalWork addAdditionalWork(boolean hasStatuses) {
        return addAdditionalWork(hasStatuses, null);
    }

    public static AdditionalWork addAdditionalWork(boolean hasStatuses, String outerId) {
        if (outerId == null) {
            outerId = "test_aw_" + RandomStringUtils.randomAlphabetic(8);
        }
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, SHIFTS_ADD_WORK);
        JSONObject miniObject = new JSONObject();
        miniObject.put(OUTER_ID, outerId);
        miniObject.put(TITLE, outerId);
        miniObject.put("disabled", false);
        if (hasStatuses) {
            miniObject.put("hasStatuses", true);
        }
        JSONObject links = new JSONObject();
        links.put(SELF, new JSONObject().put(HREF, setUri(Projects.WFM, CommonRepository.URL_BASE, SHIFTS_ADD_WORK)));
        HttpResponse response = requestMaker(uri, miniObject, RequestBuilder.post(), HAL_JSON);
        LOG.info("Добавляем доп. работу");
        LOG.info(REQUEST_LOGGER, uri);
        LOG.info(JSON_LOGGER, miniObject);
        assertStatusCode(response, 201, uri.toString());
        Allure.addAttachment("Создание доп. работы", miniObject.toString());
        ITestContext c = Reporter.getCurrentTestResult().getTestContext();
        AdditionalWork addWork = getCreatedObject(response, AdditionalWork.class);
        c.setAttribute("Additional_work_" + outerId, addWork);
        return addWork;
    }

    /**
     * @param work доп работы, к которой нужно добавить правило
     * @param omId айди орг юнита
     * @return пара назначение сотрудника - правило доп работы
     */
    public static ImmutablePair<EmployeePosition, AddWorkRule> prepareAdditionalWorkRule(AdditionalWork work, int omId) {
        EntityProperty property = EntityPropertyRepository.getEntityPropertyByKey(MathParameterEntities.ORGANIZATION_UNIT, omId, ORG_UNIT_FORMAT);
        if (property == null) {
            property = addAttributeToEntity(MathParameterEntities.ORGANIZATION_UNIT, omId, OrgUnitAttributes.ORG_UNIT_FORMAT, "ММ");
        }
        List<Map<String, Object>> result = DBUtils.getPosGroupNameAndPosNameFromOmWithFormat(property.getValue().toString());
        List<EmployeePosition> employees = EmployeePositionRepository.getAllEmployeesWithCheckByApi(omId, null, true);
        List<String> positions = employees.stream()
                .map(e -> e.getPosition().getName())
                .collect(Collectors.toList());
        Collections.shuffle(result);
        Map<String, Object> record = result.stream()
                .filter(m -> positions.contains(m.get("position_name")))
                .findAny()
                .orElseThrow(() -> new AssertionError("В данном подразделении никому нельзя добавить доп работу"));
        EmployeePosition ep = employees.stream()
                .filter(emp -> emp.getPosition().getName().equals(record.get("position_name")))
                .findAny()
                .orElseThrow(() -> new AssertionError("Не нашелся подходящий сотрудник для назначения доп работы"));
        return new ImmutablePair<>(ep, addRuleToAdditionalWork(work, ep.getPosition().getName(), record.get("position_group_name").toString(), true, omId));
    }

    public static AddWorkRule addRuleToAdditionalWork(AdditionalWork work, String jobTitleName, String positionGroupName, boolean isNecessary, int omId) {
        //todo требуется рефакторинг
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(SHIFTS_ADD_WORK, RULES));
        JSONObject addWorkObject = new JSONObject();
        addWorkObject.put(ID, work.getId());
        addWorkObject.put(TITLE, work.getTitle());
        addWorkObject.put(OUTER_ID, work.getOuterId());
        addWorkObject.put("hasStatuses", work.isHasStatuses());
        addWorkObject.put("disabled", work.isDisabled());
        JSONObject addWorkLinks = new JSONObject();
        addWorkLinks.put("addWorkRule", new JSONObject().put(HREF, setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(SHIFT_ADD_WORK, work.getId(), RULES))));
        addWorkLinks.put(STATUSES, new JSONObject().put(HREF, setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(SHIFT_ADD_WORK, work.getId(), STATUSES))));
        addWorkLinks.put(SELF, new JSONObject().put(HREF, setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(SHIFT_ADD_WORK, work.getId()))));
        addWorkObject.put(LINKS, work.getLinks());

        OrgUnit unit = OrgUnitRepository.getOrgUnit(omId);
        JSONArray orgUnitsArray = new JSONArray();
        JSONObject orgUnitObject = new JSONObject();
        orgUnitObject.put(ACTIVE, true);
        orgUnitObject.put(AVAILABLE_FOR_CALCULATION, unit.isAvailableForCalculation());
        orgUnitObject.put(DATE_FROM, unit.getDateInterval().getStartDate());
        orgUnitObject.put(ID, unit.getId());
        orgUnitObject.put(NAME, unit.getName());
        orgUnitObject.put(OUTER_ID, unit.getOuterId());
        orgUnitObject.put(ORG_UNIT_TYPE_ID, unit.getOrganizationUnitTypeId());
        orgUnitObject.put(LINKS, unit.getLinks());
        orgUnitsArray.put(orgUnitObject);

        JSONObject jobTitleObject = new JSONObject();
        JobTitle jobTitle = JobTitleRepository.getJob(jobTitleName);
        jobTitleObject.put(ACTIVE, true);
        jobTitleObject.put(ID, jobTitle.getId());
        jobTitleObject.put(NAME, jobTitle.getFullName());
        jobTitleObject.put(LINKS, jobTitle.getLinks());

        JSONObject positionGroup = new JSONObject();
        positionGroup.put(NAME, positionGroupName);
        positionGroup.put(FTE_POSITION_GROUP, true);
        PositionGroup posGroup = PositionGroupRepository.getPositionGroupByName(positionGroupName);
        positionGroup.put(ID, posGroup.getId());
        positionGroup.put(LINKS, posGroup.getLinks());

        JSONObject miniObject = new JSONObject();
        miniObject.put("toAllPositions", false);
        miniObject.put("addWork", addWorkObject);
        miniObject.put(JOB_TITLE, jobTitleObject);
        miniObject.put(REL_ORG_UNITS, orgUnitsArray);
        miniObject.put(POSITION_GROUP, positionGroup);
        miniObject.put(START_DATE, LocalDate.now());
        miniObject.put(FORMAT, EntityPropertyRepository.getEntityPropertyByKey(MathParameterEntities.ORGANIZATION_UNIT, omId, ORG_UNIT_FORMAT).getValue().toString());
        HttpResponse response = requestMaker(uri, miniObject, RequestBuilder.post(), HAL_JSON);
        LOG.info("Добавляем правило дополнительной работы");
        LOG.info(REQUEST_LOGGER, uri);
        LOG.info(JSON_LOGGER, miniObject);
        assertStatusCode(response, 201, uri.toString());
        Allure.addAttachment("Правило доп работы", String.format("Было создано правило доп работы \"%s\"", miniObject));
        return getCreatedObject(response, AddWorkRule.class);
    }

    public static AddWorkRule addRuleToAdditionalWork(AdditionalWork work, int omId, LocalDate date) {
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(SHIFTS_ADD_WORK, RULES));
        JSONObject addWorkObject = new JSONObject();
        addWorkObject.put(ID, work.getId());
        addWorkObject.put(TITLE, work.getTitle());
        addWorkObject.put(OUTER_ID, work.getOuterId());
        addWorkObject.put("hasStatuses", work.isHasStatuses());
        addWorkObject.put("disabled", work.isDisabled());
        JSONObject addWorkLinks = new JSONObject();
        addWorkLinks.put("addWorkRule", new JSONObject().put(HREF, setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(SHIFT_ADD_WORK, work.getId(), RULES))));
        addWorkLinks.put(STATUSES, new JSONObject().put(HREF, setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(SHIFT_ADD_WORK, work.getId(), STATUSES))));
        addWorkLinks.put(SELF, new JSONObject().put(HREF, setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(SHIFT_ADD_WORK, work.getId()))));
        addWorkObject.put(LINKS, work.getLinks());

        OrgUnit unit = OrgUnitRepository.getOrgUnit(omId);
        JSONArray orgUnitsArray = new JSONArray();
        JSONObject orgUnitObject = new JSONObject();
        orgUnitObject.put(ACTIVE, true);
        orgUnitObject.put(AVAILABLE_FOR_CALCULATION, unit.isAvailableForCalculation());
        orgUnitObject.put(DATE_FROM, unit.getDateInterval().getStartDate());
        orgUnitObject.put(ID, unit.getId());
        orgUnitObject.put(NAME, unit.getName());
        orgUnitObject.put(OUTER_ID, unit.getOuterId());
        orgUnitObject.put(ORG_UNIT_TYPE_ID, unit.getOrganizationUnitTypeId());
        orgUnitObject.put(LINKS, unit.getLinks());
        orgUnitsArray.put(orgUnitObject);

        JSONObject miniObject = new JSONObject();
        miniObject.put("toAllPositions", true);
        miniObject.put("addWork", addWorkObject);
        miniObject.put(REL_ORG_UNITS, orgUnitsArray);
        miniObject.put(POSITION_GROUP, JSONObject.NULL);
        miniObject.put(JOB_TITLE, JSONObject.NULL);
        miniObject.put(START_DATE, date);
        miniObject.put(FORMAT, EntityPropertyRepository.getEntityPropertyByKey(MathParameterEntities.ORGANIZATION_UNIT, omId, ORG_UNIT_FORMAT).getValue().toString());
        HttpResponse response = requestMaker(uri, miniObject, RequestBuilder.post(), HAL_JSON);
        LOG.info("Добавляем правило дополнительной работы");
        LOG.info(REQUEST_LOGGER, uri);
        LOG.info(JSON_LOGGER, miniObject);
        assertStatusCode(response, 201, uri.toString());
        Allure.addAttachment("Правило доп работы", String.format("Было создано правило доп работы \"%s\"", miniObject));
        return getCreatedObject(response, AddWorkRule.class);
    }

    /**
     * Отключает все обязательные доп работы, включает нужную доп работы, выставляет флажок со статусом,
     * добавляет правило
     *
     * @param addWork   - доп работа
     * @param omId      - айди орг юнита
     * @param shiftDate - дата начала действия правила доп работы
     * @param status    - использует ли доп работа статусы
     */
    public static void prepareAdditionalWorkForAllPositions(AdditionalWork addWork, int omId, LocalDate shiftDate, boolean status) {
        disableRequiredAddWorks();
        makeAdditionalWorkEnabled(addWork);
        changeStatus(addWork, status);
        AddWorkRule.getAllRulesOfAddWork(addWork.getId()).forEach(PresetClass::deleteRequest);
        addRuleToAdditionalWork(addWork, omId, shiftDate);
    }

    /**
     * Ищет атрибут с указанным значением поля display, если такого атрибута нет, создает новый
     */
    @Step("Создать атрибут с признаком \"{display}\"")
    public static EntityPropertiesKey addAttributeToSystemLists(MathParameterEntities entity, String display) {
        EntityPropertiesKey key = EntityPropertyKeyRepository.getPropertyByDisplay(entity.getKeys(), display);
        if (key == null) {
            key = PresetClass.addKey(entity.getKeys(), RandomStringUtils.randomAlphabetic(8), RandomStringUtils.randomAlphabetic(8),
                                     STRING, display);
            Allure.addAttachment("Создание атрибута", String.format("Создан атрибут с признаком \"%s\"", display));
        } else {
            Allure.addAttachment("Создание атрибута", String.format("Атрибут с признаком \"%s\" уже был ранее создан", display));
        }
        return key;
    }

    /**
     * Ищет случайный атрибут в системном списке, если список пуст, создаёт новый
     */
    @Step("Создать атрибут в системном списке")
    public static EntityPropertiesKey addAttributeToSystemLists(MathParameterEntities entity) {
        EntityPropertiesKey key = EntityPropertyKeyRepository.getRandomProperty(entity.getKeys());
        if (key == null) {
            key = PresetClass.addKey(entity.getKeys(), RandomStringUtils.randomAlphabetic(8), RandomStringUtils.randomAlphabetic(8),
                                     "STRING", false);
            Allure.addAttachment("Создание атрибута", String.format("Создан атрибут с названием \"%s\"", key.getTitle()));
        } else {
            Allure.addAttachment("Создание атрибута", String.format("В системном списке уже есть атрибут \"%s\"",
                                                                    key.getTitle()));
        }
        return key;
    }

    @Step("Проверить, есть ли у позиции {posId} атрибут {propertiesKey}")
    public static void checkIfPositionHasAttribute(int posId, String propertiesKey) {
        ITestContext c = Reporter.getCurrentTestResult().getTestContext();
        c.setAttribute("posAttribute_position", posId);
        c.setAttribute("posAttribute_key", propertiesKey);
        EntityProperty existingProperty = EntityPropertyRepository.getAllPropertiesFromEntity(MathParameterEntities.POSITION, posId)
                .stream()
                .filter(e -> e.getPropKey().equals(propertiesKey))
                .findFirst()
                .orElse(null);

        if (Objects.nonNull(existingProperty)) {
            c.setAttribute("posAttribute_value", existingProperty.getValue());
            deleteEntityPropertyIfPresent(posId, MathParameterEntities.POSITION, propertiesKey);
            Allure.addAttachment("Проверка наличия заполненного атрибута до начала теста", String.format("У позиции %d есть атрибут %s со значением %s", posId, propertiesKey, existingProperty
                    .getValue()));
        } else {
            Allure.addAttachment("Проверка наличия заполненного атрибута до начала теста", String.format("У позиции %d нет атрибута %s", posId, propertiesKey));
        }
    }

    /**
     * Добавляет атрибут к сущности (сотруднику или подразделению)
     *
     * @param entity    сущность
     * @param id        идентификатор сущности
     * @param attribute атрибут
     * @param value     значение атрибута
     */
    public static <T> EntityProperty addAttributeToEntity(MathParameterEntities entity, int id, Attributes attribute, T value) {
        EntityProperty<T> property = EntityPropertyRepository.getEntityPropertyByKey(entity, id, attribute.getKey());
        if (property == null) {
            EntityPropertiesKey key = EntityPropertyKeyRepository.getPropertyByKey(entity.getKeys(), attribute.getKey());
            if (key == null) {
                PresetClass.addKey(entity.getKeys(), attribute.getKey(), attribute.getTitle(), attribute.getDataType(), false);
                key = EntityPropertyKeyRepository.getPropertyByKey(entity.getKeys(), attribute.getKey());
            }
            PresetClass.addEntityPropertyValue(entity, id, key, value);
            property = EntityPropertyRepository.getEntityPropertyByKey(entity, id, attribute.getKey());
        } else {
            property.setValue(value);
            PresetClass.changeEntityPropertyValue(entity, id, property, value);
        }
        LOG.info("К сущности {} с id {} добавлен атрибут {}", entity, id, attribute.getKey());
        Allure.addAttachment("Добавление атрибута к сущности", String.format("К сущности %s с id %d добавлен атрибут \"%s\"",
                                                                             entity, id, attribute.getKey()));
        return property;
    }

    public static void deleteAdditionalWorkAndRules(AdditionalWork work) {
        List<AddWorkRule> rules = AddWorkRule.getAllRulesOfAddWork(work.getId());
        for (AddWorkRule rule : rules) {
            LOG.info("Удаляем правило из доп. работы");
            deleteRequest(rule);
        }
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(SHIFTS_ADD_WORK, work.getId()));
        LOG.info("Удаляем доп. работу с id {}", work.getId());
        deleteRequest(uri);
    }

    /**
     * Добавляет доп. работу заданного типа к смене
     */
    public static ShiftAddWorkLink addWorkToShift(LocalDateTime from, LocalDateTime to, Shift shift, AdditionalWork additionalWork) {
        JSONObject addWorkSelfLink = additionalWork.getLinkWrappedInJson(SELF);
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, "shifts-add-work-link");
        JSONObject object = new JSONObject();
        object.put(TO, to);
        object.put(FROM, from);
        JSONObject links = new JSONObject();
        links.put(Links.SHIFT, shift.getLinkWrappedInJson(SELF));
        links.put("shiftAddWork", addWorkSelfLink);
        links.put(SELF, new JSONObject().put(HREF, uri));
        object.put(LINKS, links);

        HttpResponse response = requestMaker(uri, object, RequestBuilder.post(), HAL_JSON);
        LOG.info("Добавляем доп. работу к смене {} c {} до {}", shift, from, to);
        LOG.info(REQUEST_LOGGER, uri);
        LOG.info(JSON_LOGGER, object);
        assertStatusCode(response, 201, uri.toString());
        Allure.addAttachment("Добавление доп. работы",
                             String.format("К смене %s была добавлена доп. работа с %s до %s", shift, from, to));
        return getCreatedObject(response, ShiftAddWorkLink.class);
    }

    /**
     * Удаляет у заданной смены все доп. работы
     */
    public static void removeAllAdditionalWorkFromShift(Shift shift) {
        List<ShiftAddWorkLink> additionalWorkBefore = shift.getAdditionalWork();
        for (ShiftAddWorkLink addWork : additionalWorkBefore) {
            deleteRequest(addWork);
        }
    }

    public static ConstraintViolationSettings setPriorityLevelToConstraintViolation(ConstraintViolations violation, ConstraintViolationLevel level, boolean applicableForWorkedRoster) {
        List<ConstraintViolationSettings> constrViolationList = ConstraintViolationSettingsRepository.getConstraintViolationSettings();
        JSONObject miniObject = new JSONObject();
        miniObject.put(ACTIVE, true);
        miniObject.put(VISIBLE, true);
        miniObject.put(TYPE, violation.toString());
        miniObject.put(LEVEL, level.toString());
        ConstraintViolationSettings conflict = constrViolationList.stream()
                .filter(c -> c.getType().equals(violation.toString())).findFirst().orElse(null);
        if (Objects.isNull(conflict)) {
            return PresetClass.createConflict(miniObject, violation, level);
        } else if (conflict.getType().equals(violation.toString()) && conflict.getLevel().equals(level)) {
            return conflict;
        }
        URI uri = URI.create(conflict.getSelfLink());
        JSONObject self = new JSONObject();
        self.put(SELF, new JSONObject().put(HREF, uri.toString()));
        miniObject.put(LINKS, self);
        if (applicableForWorkedRoster) {
            miniObject.put("applicableForWorkedRoster", true);
        }
        HttpResponse response = requestMaker(uri, miniObject, RequestBuilder.put(), HAL_JSON);
        LOG.info("Устанавливаем приоритет {} конфликту {}", level, violation);
        LOG.info(REQUEST_LOGGER, uri);
        LOG.info(JSON_LOGGER, miniObject);
        assertStatusCode(response, uri.toString());
        Allure.addAttachment("Приоритет для конфликта", String.format("Установили приоритет \"%s\" для конфликта \"%s\"", level, violation.getName()));
        return conflict.refresh();
    }

    public static void deleteLocalConstraintViolationSetting(int omId, String constraintViolationType) {
        ConstraintViolationSettings constraintViolation = ConstraintViolationSettingsRepository.getConstraintViolationByType(ConstraintViolationSettingsRepository.getConstraintViolationSettingsByOrgUnit(omId), constraintViolationType);
        ConstraintViolationLevel level = constraintViolation.getLevel();
        String selfLink = constraintViolation.getSelfLink();
        URI uri = URI.create(selfLink);
        if (level.equals(ConstraintViolationLevel.OFF)) {
            deleteMaker(uri);
        }
    }

    public static ConstraintViolationSettings createConflict(JSONObject miniObject, ConstraintViolations violation, ConstraintViolationLevel level) {
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, CONSTRAINT_VIOLATIONS_GLOBAL_SETTINGS);
        HttpResponse response = requestMaker(uri, miniObject, RequestBuilder.post(), HAL_JSON);
        LOG.info("Постим конфликт с названием {} и приоритетом {}", violation, level);
        assertStatusCode(response, HttpStatus.SC_CREATED, uri.toString());
        String link = Arrays.stream(response.getHeaders("Location")).findFirst().get().getValue();
        return getClassObjectFromJson(ConstraintViolationSettings.class, getJsonFromUri(Projects.WFM, link));
    }

    /**
     * Добавляет правило обеда подразделению
     *
     * @param omId       айди подразделения
     * @param lunchRules строка с правилами обеда
     */
    public static LunchRule addLunchRuleToOrgUnit(int omId, String lunchRules) {
        MathParameter mathParam = MathParameterRepository.getMathParameterWithValue(MathParameterEntities.ORGANIZATION_UNIT, LUNCH_RULES);
        deleteLunchRuleFromOrgUnit(omId);
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(ORG_UNITS, omId, MATH_PARAMETER_VALUES));
        Allure.addAttachment("Добавление правила обеда",
                             String.format("Подразделению с id %d добавлено правило обеда %s", omId, lunchRules));
        return createLunchRule(uri, mathParam, lunchRules);
    }

    /**
     * Добавляет правило обеда функциональной роли
     *
     * @param empPosId   айди позиции пользователя
     * @param omId       айди подразделения
     * @param lunchRules строка с правилами обеда
     */
    public static LunchRule addLunchRuleToPositionGroup(int empPosId, int omId, String lunchRules) {
        MathParameter mathParam = MathParameterRepository.getMathParameterWithValue(MathParameterEntities.POSITION_GROUP, LUNCH_RULES);
        NameValuePair pair = newNameValue(Links.ORGANIZATION_UNIT_ID, omId);
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(POSITION_GROUPS, empPosId, MATH_PARAMETER_VALUES), Collections.singletonList(pair));
        Allure.addAttachment("Добавление правила обеда",
                             String.format("Функциональной роли с id %d добавлено правило обеда %s", empPosId, lunchRules));
        return createLunchRule(uri, mathParam, lunchRules);
    }

    /**
     * Отправляет post запрос для создания правила обеда
     */
    private static LunchRule createLunchRule(URI uri, MathParameter mathParam, String lunchRules) {
        JSONObject miniObject = new JSONObject();
        miniObject.put(TYPE, STRING);
        miniObject.put(COMMON_NAME, mathParam.getCommonName());
        miniObject.put(OUTER_ID, mathParam.getOuterId());
        miniObject.put(ENTITY, mathParam.getEntity());
        miniObject.put(VALUE, lunchRules);
        JSONObject links = new JSONObject();
        links.put(MATH_PARAMETER, new JSONObject().put(HREF, setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(MATH_PARAMETERS, mathParam.getMathParameterId()))));
        links.put(SELF, new JSONObject().put(HREF, uri));
        miniObject.put(LINKS, links);
        HttpResponse response = requestMaker(uri, miniObject, RequestBuilder.post(), HAL_JSON);
        LOG.info(REQUEST_LOGGER, uri);
        LOG.info(JSON_LOGGER, miniObject);
        assertStatusCode(response, 201, uri.toString());
        return new LunchRule(miniObject);
    }

    /**
     * Удаляет правило обеда у орг юнита
     *
     * @param omId идентификатор подразделения
     */
    public static void deleteLunchRuleFromOrgUnit(int omId) {
        MathParameter mathParam = MathParameterRepository.getMathParameterWithValue(MathParameterEntities.ORGANIZATION_UNIT, "lunchRules");
        if (mathParam != null) {
            URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(ORG_UNITS, omId, MATH_PARAMETER_VALUES, mathParam.getMathParameterId()));
            LOG.info("Удаляем правила обеда");
            deleteRequest(uri);
            Allure.addAttachment("Удаление правила обеда у орг юнита", String.format("Удалено правило обеда у орг юнита с id %d", omId));
        }
    }

    public static void deleteLunchRuleFromOrgUnitAndParent(OrgUnit unit) {
        deleteLunchRuleFromOrgUnit(unit.getId());
        if (CommonRepository.URL_BASE.contains("pochta")) {
            OrgUnit parent = unit.getParentOrgUnit();
            while (parent != null) {
                deleteLunchRuleFromOrgUnit(parent.getId());
                parent = parent.getParentOrgUnit();
            }
        }
    }

    /**
     * Удаляет правило обеда у функциональной роли
     *
     * @param empPosId айди позиции пользователя
     * @param omId     айди подразделения
     */
    public static void deleteLunchRuleFromEmployeePosition(int empPosId, int omId) {
        MathParameter mathParam = MathParameterRepository.getMathParameterWithValue(MathParameterEntities.POSITION_GROUP, LUNCH_RULES);
        NameValuePair pair = newNameValue(ORGANIZATION_UNIT_ID, omId);
        JSONObject miniObject = new JSONObject();
        miniObject.put(TYPE, STRING);
        miniObject.put(COMMON_NAME, mathParam.getCommonName());
        miniObject.put(OUTER_ID, mathParam.getOuterId());
        miniObject.put(ENTITY, mathParam.getEntity());
        miniObject.put(VALUE, "");
        JSONObject links = new JSONObject();
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(POSITION_GROUPS, empPosId, MATH_PARAMETER_VALUES), Collections.singletonList(pair));
        links.put(MATH_PARAMETER, new JSONObject().put(HREF, setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(MATH_PARAMETERS, mathParam.getMathParameterId()))));
        links.put(SELF, new JSONObject().put(HREF, uri));
        miniObject.put(LINKS, links);
        HttpResponse response = requestMaker(uri, miniObject, RequestBuilder.post(), HAL_JSON);
        LOG.info(REQUEST_LOGGER, uri);
        LOG.info(JSON_LOGGER, miniObject);
        assertStatusCode(response, 201, uri.toString());
    }

    /**
     * Отправляет put запрос для изменения типа формирования табеля
     */
    public static void editTypeOfTimeSheetFormation(int omId, String typeOfFormation) {
        MathParameter mathParam = MathParameterRepository.getMathParameterWithValue(MathParameterEntities.ORGANIZATION_UNIT, TABLE_MODE_CREATE);
        JSONObject miniObject = new JSONObject();
        miniObject.put(TYPE, STRING);
        miniObject.put(COMMON_NAME, mathParam.getCommonName());
        miniObject.put(OUTER_ID, mathParam.getOuterId());
        miniObject.put(ENTITY, mathParam.getEntity());
        miniObject.put(VALUE, typeOfFormation);
        JSONObject links = new JSONObject();
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(ORG_UNITS, omId, MATH_PARAMETER_VALUES, mathParam.getMathParameterId()));
        links.put(ENTITY, new JSONObject().put(HREF, setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(ORG_UNITS, omId))));
        links.put(MATH_PARAMETER, new JSONObject().put(HREF, setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(MATH_PARAMETERS, mathParam.getMathParameterId()))));
        links.put(SELF, new JSONObject().put(HREF, uri));
        miniObject.put(LINKS, links);
        HttpResponse response = requestMaker(uri, miniObject, RequestBuilder.put(), HAL_JSON);
        LOG.info(REQUEST_LOGGER, uri);
        LOG.info(JSON_LOGGER, miniObject);
        assertStatusCode(response, uri.toString());
    }

    /**
     * Передает смену сотрудника на биржу
     */
    public static void moveShiftToExchange(Shift shift) {
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(SHIFTS, EXCHANGE, shift.getId()));
        JSONObject json = new JSONObject();
        json.put(LINKS, new JSONObject().put(SELF, new JSONObject().put(HREF, uri)));
        HttpResponse response = requestMaker(uri, json, RequestBuilder.put(), HAL_JSON);
        assertStatusCode(response, uri.toString());
        Allure.addAttachment("Передача смены на биржу", "Смена за " + shift.getDateTimeInterval() + " передана на биржу.");
    }

    /**
     * Назначает сотруднику статус "Декрет" на указанный промежуток времени
     */
    public static void assignMaternityLeaveStatus(EmployeePosition ep, LocalDate from, LocalDate to) {
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(EMPLOYEES, ep.getEmployee().getId(), STATUSES));
        JSONObject json = new JSONObject();
        json.put(FROM, from);
        json.put(TO, to);

        EmployeeStatusType statusType = EmployeeStatusTypeRepository.getStatusTypeByOuterId("DECREE");
        JSONObject statusTypeJson = getJsonFromUri(Projects.WFM, setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(EMPLOYEES_STATUS_TYPES, statusType.getId())));
        json.put(STATUS_TYPE, statusTypeJson);
        json.put(LINKS, new JSONObject().put(SELF, new JSONObject().put(HREF, uri)));
        HttpResponse response = requestMaker(uri, json, RequestBuilder.post(), HAL_JSON);
        LOG.info(JSON_LOGGER, json);
        assertStatusCode(response, 201, uri.toString());
        Allure.addAttachment("Присвоение статуса \"декрет\"",
                             String.format("Сотруднику %s присвоен статус \"декрет\" с %s по %s", ep, from, to));
    }

    /**
     * Удаляет все графики работ подразделения
     */
    public static void deleteBusinessHours(int omId) {
        List<BusinessHours> businessDaysList = BusinessHoursRepository.scheduleType(omId);
        if (!businessDaysList.isEmpty()) {
            for (BusinessHours businessHours : businessDaysList) {
                deleteRequest(businessHours);
                Allure.addAttachment("Удаление режима работы",
                                     String.format("Удален режим работы подразделения с id %d для типа \"%s\"", omId, businessHours.getType()));
            }
        }
    }

    public static void createBusinessHours(int omId, DateInterval interval, String type) {
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, BUSINESS_HOURS_LIST);
        JSONObject json = new JSONObject();
        JSONObject dateInterval = new JSONObject();
        dateInterval.put(START_DATE, interval.getStartDate());
        dateInterval.put(END_DATE, interval.getEndDate());
        json.put(DATE_INTERVAL, dateInterval);
        json.put(TYPE, type);
        json.put(NAME, type);
        JSONObject links = new JSONObject();
        links.put(SELF, new JSONObject().put(HREF, uri));
        links.put(ORG_UNIT_JSON, new JSONObject().put(HREF, setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(ORGANIZATION_UNITS, omId))));
        json.put(LINKS, links);
        HttpResponse response = requestMaker(uri, json, RequestBuilder.post(), HAL_JSON);
        LOG.info(JSON_LOGGER, json);
        assertStatusCode(response, 201, uri.toString());
        Allure.addAttachment("Создан график работы подразделения",
                             String.format("Создан режим работы подразделения с id %d для типа \"%s\"", omId, type));
    }

    /**
     * Делает день рабочим
     *
     * @param isoWeekday порядковый номер дня недели
     * @param link       ссылка на день
     */
    public static void makeWorkDay(int isoWeekday, String link) {
        JSONObject miniObject = new JSONObject();
        JSONObject timeIntervalJson = new JSONObject();
        timeIntervalJson.put(START_TIME, "09:00:00");
        timeIntervalJson.put(END_TIME, "18:00:00");
        miniObject.put(TIME_INTERVAL, timeIntervalJson);
        miniObject.put(ISO_WEEK_DAY, isoWeekday);
        JSONObject type = new JSONObject();
        type.put(NAME, "dialogs.orgUnit.dayTypes.work");
        type.put(CODE, "work");
        miniObject.put(TYPE, type);
        JSONObject links = new JSONObject();
        links.put(SELF, new JSONObject().put(HREF, link));
        miniObject.put(LINKS, links);
        HttpResponse response = requestMaker(URI.create(link), miniObject, RequestBuilder.post(), HAL_JSON);
        LOG.info(REQUEST_LOGGER, link);
        LOG.info(JSON_LOGGER, miniObject);
        assertStatusCode(response, 201, link);
    }

    public static List<BusinessDays> getBusinessDays(int omId) {
        String urlEnding = makePath(ORG_UNITS, omId, BUSINESS_HOURS);
        JSONObject jsonObject = getJsonFromUri(Projects.WFM, setUri(Projects.WFM, CommonRepository.URL_BASE, urlEnding));
        JSONArray businessHoursList = getJsonArrayFromJsonObject(jsonObject);
        if (businessHoursList != null) {
            JSONObject businessHours = businessHoursList.getJSONObject(0);
            String daysLink = businessHours.getJSONObject(LINKS).getJSONObject(DAYS).getString(HREF);
            JSONObject json = getJsonFromUri(Projects.WFM, daysLink);
            LOG.info("Получен JSON: {}", json);
            return CustomTools.getListFromJsonArray(json.getJSONObject(EMBEDDED).getJSONArray(DAYS), BusinessDays.class);
        }
        return new ArrayList<>();
    }

    /**
     * Делает все дни подразделения во всех графиках рабочими
     */
    public static void makeWorkDays(int omId) {
        List<BusinessHours> businessHoursList = BusinessHoursRepository.scheduleType(omId);
        for (BusinessHours hours : businessHoursList) {
            List<BusinessDays> businessDays = hours.getBusinessDays();
            List<Integer> weekdays = businessDays.stream().map(BusinessDays::getIsoWeekday).collect(Collectors.toList());
            List<Integer> week = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7));
            week.removeAll(weekdays);
            String link = businessDays.get(0).getLink();
            if (!week.isEmpty()) {
                for (int day : week) {
                    makeWorkDay(day, link);
                    systemSleep(1); //цикл
                }
            }
        }
    }

    public static void makeWorkDaysForOm(int omId) {
        List<BusinessDays> businessDays = getBusinessDays(omId);
        List<Integer> weekdays = businessDays.stream().map(BusinessDays::getIsoWeekday).collect(Collectors.toList());
        List<Integer> week = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7));
        week.removeAll(weekdays);
        if (!week.isEmpty()) {
            for (int day : week) {
                makeWorkDay(day, businessDays.get(0).getLink());
                systemSleep(1); //цикл
            }
        }
    }

    public static EmployeePosition setRate(EmployeePosition ep, double rate) {
        URI uri = URI.create(ep.getSelfLink());
        JSONObject miniObject = new JSONObject();
        JSONObject links = new JSONObject();
        links.put(EMPLOYEE, ep.getEmployee().getLinkWrappedInJson(SELF));
        links.put(POSITION, ep.getPosition().getLinkWrappedInJson(SELF));
        links.put(SELF, new JSONObject().put(HREF, uri.toString()));
        miniObject.put(LINKS, links);
        JSONObject dateInterval = new JSONObject();
        dateInterval.put(START_DATE, ep.getDateInterval().getStartDate().toString());
        miniObject.put(DATE_INTERVAL, dateInterval);
        miniObject.put(TEMPORARY, false);
        miniObject.put(RATE, rate);
        miniObject.put(CARD_NUMBER, ep.getCardNumber());
        LOG.info(String.valueOf(miniObject));
        HttpResponse response = requestMaker(uri, miniObject, RequestBuilder.put(), HAL_JSON);
        assertStatusCode(response, uri.toString());
        JSONObject newEmployeePositionJSON = getJsonFromUri(Projects.WFM, uri);
        Allure.addAttachment("Ставка",
                             String.format("Сотруднику %s установлена ставка %f", ep, rate));
        return getClassObjectFromJson(EmployeePosition.class, newEmployeePositionJSON);
    }

    /**
     * Создает отметки о присутствии на текущий день
     *
     * @param orgUnitId - айди орг. юнита
     * @param interval  - временной интервал
     */
    public static void createMarks(int orgUnitId, DateInterval interval) {
        String urlEnding = makePath(SYS_BIO, CREATE_RECORDS_FROM_PLAN);
        List<NameValuePair> nameValuePairs = Pairs.newBuilder()
                .from(interval.startDate)
                .to(interval.endDate)
                .orgUnitId(orgUnitId)
                .accuracyInMinutes(15)
                .build();
        String urlBase = CommonRepository.URL_BASE;
        URI uri = setUri(Projects.JOBAPP, urlBase, urlEnding, nameValuePairs);
        HttpResponse response = requestMaker(uri, new JSONObject(), RequestBuilder.post(), HAL_JSON);
        assertStatusCode(response, uri.toString());
        Allure.addAttachment("Добавление отметок о посещении", "Были созданы отметки о присутствии на текущую дату");
    }

    public static Limits createLimit(Limits limit) {
        return createLimit(limit, 201, null);
    }

    /**
     * Создает лимит в системных списках (на текущий момент актуально для магнита)
     *
     * @param limit   объект лимита
     * @param code    ожидаемый статус-код
     * @param message ожидаемое сообщение об ошибке
     */
    public static Limits createLimit(Limits limit, int code, String message) {
        if (limit.getFrom() == null) {
            limit.setFrom(LocalDateTools.getFirstDate());
        }
        if (limit.getTo() == null) {
            limit.setTo(LocalDateTools.getLastDate());
        }
        if (limit.getLimit() == 0 && limit.getLimitType().equals(LimitType.GENERAL.toString())) {
            limit.setLimit(100);
        } else if (limit.getLimit() == 0) {
            limit.setLimit(6);
        }
        if (limit.getOrgType() == null) {
            limit.setOrgType(getRandomFromList(CommonRepository.getOrgTypes()));
        }
        if (limit.getPeriod() == null) {
            limit.setPeriod("MONTH");
        }
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, TYPED_LIMITS);
        limit.setLinks(new JSONObject().put(Params.SELF, Collections.singletonMap(Params.HREF, uri)));
        ApiRequest.Builder builder = new ApiRequest.PostBuilder(uri.toString()).withBody(limit);
        if (code != 201 && message != null) {
            builder.withMessage(message).withStatus(code);
        }
        Limits response = builder.send().returnCreatedObject();
        if (response != null) {
            ITestContext c = Reporter.getCurrentTestResult().getTestContext();
            c.setAttribute("Limits_" + response.hashCode(), response);
        }
        return response;
    }

    public static void removeLimit(Limits limit) {
        LOG.info("Удаляем лимит с типом {} и значением, равным {}", limit.getLimitType(), limit.getLimit());
        Reporter.getCurrentTestResult().getTestContext().removeAttribute("Limits_" + limit.hashCode());
        deleteRequest(limit.getSelfLink());
    }

    public static FileManual uploadCalculationHint() {
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, "file-manual");
        Path path = Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "files", "169 Текст подсказки_2 (2).pdf");
        RequestFormers.uploadFile(uri, path.toString());
        return getCalculationHint();
    }

    /**
     * Возвращает подсказку, отображаемую перед началом расчета
     */
    public static FileManual getCalculationHint() {
        FileManual hint = CommonRepository.getFileManuals()
                .stream()
                .filter(m -> m.getType().equals("CALCULATION_HINT"))
                .findFirst()
                .orElse(null);
        if (hint == null) {
            hint = uploadCalculationHint();
        }
        return hint;
    }

    public static void changeRequestAliasProperties(ScheduleRequestAlias oldAlias, ScheduleRequestAlias newAlias) {
        ITestContext c = Reporter.getCurrentTestResult().getTestContext();
        Object contextValue = c.getAttribute(CHANGED_SCHEDULE_REQUEST_ALIAS);
        if (contextValue != null) {
            List<ScheduleRequestAlias> list = new ArrayList<>((List<ScheduleRequestAlias>) contextValue);
            if (list.stream().noneMatch(a -> a.getOuterId().equals(oldAlias.getOuterId()))) {
                list.add(oldAlias);
            }
            c.setAttribute(CHANGED_SCHEDULE_REQUEST_ALIAS, list);
        } else {
            c.setAttribute(CHANGED_SCHEDULE_REQUEST_ALIAS, new ArrayList<>(Arrays.asList(oldAlias)));
        }
        new ApiRequest.PutBuilder(newAlias.getSelfPath()).withBody(newAlias).send();
    }

    public static void revertRequest() {
        ITestContext c = Reporter.getCurrentTestResult().getTestContext();
        List<ScheduleRequestAlias> list = (List<ScheduleRequestAlias>) c.getAttribute(CHANGED_SCHEDULE_REQUEST_ALIAS);
        if (list == null) {
            return;
        }
        for (ScheduleRequestAlias alias : list) {
            new ApiRequest.PutBuilder(alias.getSelfPath()).withBody(alias).send();
        }
    }

    /**
     * Метод назначает руководителем первого из списка оргюнитов
     * Для назначения требуется отправить 3 запроса
     *
     * @param orgUnit подразделение, которому назначаем руководителя
     */
    public static EmployeePosition appointEmployeeAChief(OrgUnit orgUnit) {
        EmployeePosition employeePosition = EmployeePositionRepository.getEmployeePositions(orgUnit.getId())
                .stream().findFirst().orElseThrow(() -> new AssertionError("В оргюните c id №" + orgUnit.getId() + " нет сотрудников"));
        String positionLink = makePath(POSITIONS, employeePosition.getPosition().getId());
        URI uriPositions = RequestFormers.setUri(Projects.WFM, CommonRepository.URL_BASE, positionLink);

        changeEmployeePositionToChief(employeePosition);
        changePositionToChief(employeePosition, orgUnit, uriPositions);
        changeOrgUnitToChief(uriPositions, orgUnit);
        Allure.addAttachment("Назначить руководителя подразделению",
                             "Подразделению с id: " + orgUnit.getId() + " назначен руководитель " + employeePosition.getEmployee().getFullName());
        return employeePosition.refreshEmployeePosition();
    }

    /**
     * PUT запрос employee-position /api/v1/employee-positions/{id}
     * Меняем поле isChief
     *
     * @param employeePosition позиция сотрудника
     */
    private static void changeEmployeePositionToChief(EmployeePosition employeePosition) {
        URI uriChief = URI.create(employeePosition.getSelfLink());
        Position tempPosition = employeePosition.getPosition();
        Employee tempEmployee = employeePosition.getEmployee();
        JSONObject json = new JSONObject();
        json.put(IS_CHIEF, true);

        JSONObject links = new JSONObject();
        links.put(SELF, employeePosition.getLinkWrappedInJson(SELF));
        links.put(EMPLOYEE_JSON, tempEmployee.getLinkWrappedInJson(SELF));
        links.put(POSITION, tempPosition.getLinkWrappedInJson(SELF));
        json.put(LINKS, links);

        JSONObject dateInterval = new JSONObject();
        dateInterval.put(START_DATE, employeePosition.getDateInterval().startDate);
        dateInterval.put(END_DATE, LocalDate.now().toString());
        json.put(DATE_INTERVAL, dateInterval);

        LOG.info(JSON_LOGGER, json);
        assertStatusCode(requestMaker(uriChief, json, RequestBuilder.put(), HAL_JSON), uriChief.toString());
    }

    /**
     * PUT запрос position /api/v1/positions/{id}
     *
     * @param employeePosition позиция сотрудника
     * @param orgUnit          подразделение, которому назначаем руководителя
     * @param uriPositions     адрес, по которому будем обращаться
     */
    private static void changePositionToChief(EmployeePosition employeePosition, OrgUnit orgUnit, URI uriPositions) {
        Position position = employeePosition.getPosition();
        JSONObject jsonPositions = new JSONObject();
        jsonPositions.put(DATE_INTERVAL, new JSONObject().put(START_DATE, position.getDateInterval()));
        jsonPositions.put(ID, position.getId());
        jsonPositions.put(JOB_TITLE, JobTitleRepository.getJob(employeePosition.getPosition().getName()).getFullName());
        jsonPositions.put(NAME, position.getName());
        jsonPositions.put(Params.ORGANIZATION_UNIT_ID, orgUnit.getId());
        jsonPositions.put(OUTER_ID, position.getOuterId());

        PositionCategory posCat = PositionCategoryRepository.getPositionCategoryById(position.getPositionCategoryId());
        PositionType posType = PositionTypeRepository.getPositionTypeById(position.getPositionTypeId());
        PositionGroup posGroup = PositionGroupRepository.getPositionGroupById(position.getPositionGroupId());

        JSONObject posCatObject = new JSONObject();
        posCatObject.put(CALCULATION_MODE, posCat.getCalculationMode());
        posCatObject.put(CATEGORY_ID, posCat.getCategoryId());
        posCatObject.put(NAME, posCat.getName());
        posCatObject.put(OUTER_ID, posCat.getOuterId());
        posCatObject.put(LINKS, posCat.getLinks());
        jsonPositions.put(POSITION_CATEGORY, posCatObject);

        JSONObject posTypeObject = new JSONObject();
        posTypeObject.put(ID, posType.getId());
        posTypeObject.put(NAME, posType.getName());
        posTypeObject.put(OUTER_ID, posType.getOuterId());
        posTypeObject.put(LINKS, posType.getLinks());
        jsonPositions.put(POSITION_TYPE, posTypeObject);

        JSONObject posGroupObject = new JSONObject();
        posTypeObject.put(ID, posGroup.getId());
        posTypeObject.put(FTE_POSITION_GROUP, posGroup.getFtePositionGroup());
        posTypeObject.put(NAME, posGroup.getName());
        posTypeObject.put(LINKS, posGroup.getLinks());
        jsonPositions.put(POSITION_GROUP, posGroupObject);

        JSONObject linksPos = new JSONObject();
        linksPos.put(SELF, new JSONObject().put(HREF, uriPositions));
        linksPos.put(POSITION_TYPE, posType.getLinkWrappedInJson(SELF));
        linksPos.put(POSITION_CATEGORY, posCat.getLinkWrappedInJson(SELF));
        linksPos.put(POSITION_GROUP, posGroup.getLinkWrappedInJson(SELF));
        linksPos.put(ORG_UNIT_JSON, new JSONObject().put(HREF, setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(ORGANIZATION_UNITS, orgUnit))));
        jsonPositions.put(LINKS, linksPos);

        LOG.info(JSON_LOGGER, jsonPositions);
        assertStatusCode(requestMaker(uriPositions, jsonPositions, RequestBuilder.put(), HAL_JSON), uriPositions.toString());
    }

    /**
     * PUT запрос org-units /api/v1/org-units/{id}/chief
     *
     * @param orgUnit      подразделение, которому назначаем руководителя
     * @param uriPositions адрес, по которому будем обращаться
     */
    private static void changeOrgUnitToChief(URI uriPositions, OrgUnit orgUnit) {
        URI uriOrgUnit = URI.create(orgUnit.getLinks().get(SELF) + "/" + REL_ORGANIZATION_UNIT_CHIEF);
        JSONObject jsonOrgUnit = new JSONObject();
        JSONObject jsonChiefPosition = new JSONObject();
        JSONObject jsonHref = new JSONObject();
        jsonHref.put(HREF, uriPositions);
        jsonChiefPosition.put(REL_ORGANIZATION_UNIT_CHIEF_POSITION, jsonHref);
        jsonOrgUnit.put(LINKS, jsonChiefPosition);
        assertStatusCode(requestMaker(uriOrgUnit, jsonOrgUnit, RequestBuilder.put(), HAL_JSON), uriOrgUnit.toString());
    }

    /**
     * Вспомогательный метод магнит стенда, чтобы использовалась только существующие
     * принадлежности подразделения, если там null, то тогда из трёх случайных вариантов
     * Добавляет принадлежность подразделения, если она отсутствует
     */
    public static String setUnitOwnershipValue(int omId) {
        String orgUnitFormatValue;
        boolean isMagnit = Links.getTestProperty(RELEASE).contains("magnit");
        String[] magnitAttr = {"ММ", "МА", "МК"};
        JSONObject entityPropertyAsJSON = OrgUnitRepository.getValueInEntityPropertiesInOrgUnit(omId);
        if (Objects.isNull(entityPropertyAsJSON)) {
            if (isMagnit) {
                orgUnitFormatValue = magnitAttr[new Random().nextInt(magnitAttr.length)];
            } else {
                orgUnitFormatValue = RandomStringUtils.randomAlphanumeric(8);
            }
            Allure.addAttachment("Проверка наличия принадлежности у подразделения",
                                 String.format("У подразделения с id %d нет принадлежности, будет добавлена принадлежность %s", omId, orgUnitFormatValue));
            addAttributeToEntity(MathParameterEntities.ORGANIZATION_UNIT, omId, OrgUnitAttributes.ORG_UNIT_FORMAT, orgUnitFormatValue);
        } else if (isMagnit && !Arrays.asList(magnitAttr).contains(entityPropertyAsJSON.optString(VALUE))) {
            orgUnitFormatValue = magnitAttr[new Random().nextInt(magnitAttr.length)];
            Allure.addAttachment("Проверка наличия принадлежности у подразделения",
                                 String.format("У подразделения с id %d нет принадлежности с одним из значений %s, будет добавлена принадлежность %s", omId, Arrays.asList(magnitAttr), orgUnitFormatValue));
            addAttributeToEntity(MathParameterEntities.ORGANIZATION_UNIT, omId, OrgUnitAttributes.ORG_UNIT_FORMAT, orgUnitFormatValue);
        } else {
            orgUnitFormatValue = entityPropertyAsJSON.getString(VALUE);
            Allure.addAttachment("Проверка наличия принадлежности у подразделения",
                                 String.format("У подразделения с id %d есть принадлежность %s", omId, orgUnitFormatValue));
        }
        return orgUnitFormatValue;
    }

    /**
     * Оставить только смены с типом 'Смена', исключая выходные, неявки и т.д.
     */
    public static List<Shift> filterShiftsOnTypes(List<Shift> shifts, OrgUnit orgUnit, EmployeePosition employeePosition, ShiftTimePosition shiftTimePosition) {
        List<LocalDate> datesFromScheduleRequests = ScheduleRequestRepository.getEmployeeScheduleRequests(employeePosition.getEmployee().getId(), shiftTimePosition.getShiftsDateInterval(), orgUnit.getId())
                .stream()
                .map(request -> request.getDateTimeInterval().getAllDatesInInterval())
                .flatMap(List::stream)
                .collect(Collectors.toList());

        for (Iterator<Shift> iterator = shifts.iterator(); iterator.hasNext(); ) {
            Shift shift = iterator.next();
            LocalDate shiftStartDate = shift.getStartDate();

            if (datesFromScheduleRequests.contains(shiftStartDate)) {
                iterator.remove();
            }
        }
        return shifts;
    }

    private static void removeTableRules(List<TableRule> rules) {
        for (TableRule rule : rules) {
            URI uriPreset = setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(TIMESHEET_EDIT_RULE, rule.getId()));
            LOG.info("Удаляем правило табеля у орг юнита \"{}\"", rule.getOrgUnitName());
            deleteRequest(uriPreset);
        }
    }

    /**
     * Удаляет статус у сотрудника
     *
     * @param status объект класса EmployeeStatus
     */
    public static void deleteEmployeeStatus(EmployeeStatus status) {
        JSONObject json = getJsonFromUri(Projects.WFM, URI.create(status.getSelfLink().replaceAll("/status/\\d+", "")));
        Employee employee = getClassObjectFromJson(Employee.class, json);
        URI uriPreset = setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(EMPLOYEES, employee.getId(), STATUS, status.getId() + "/"));
        LOG.info("Удаляем статус у сотрудника {}", employee.getFullName());
        deleteRequest(uriPreset);
    }

    /**
     * Устанавливает временной промежуток для начала и окончания работы и должности сотрудника
     *
     * @param ep       позиция сотрудника
     * @param interval временной интервал для назначения сотрудника
     */
    public static void setPositionDateInterval(EmployeePosition ep, DateInterval interval) {
        URI posUri = URI.create(ep.getPosition().getSelfLink());
        JSONObject positionJSON = getJsonFromUri(Projects.WFM, posUri);
        positionJSON.remove(DATE_INTERVAL);
        JSONObject dateIntervalJson = new JSONObject();
        dateIntervalJson.put(START_DATE, interval.getStartDate());
        dateIntervalJson.put(END_DATE, interval.getEndDate());
        positionJSON.put(DATE_INTERVAL, dateIntervalJson);
        LOG.info("positionJSON: {}", positionJSON);
        HttpResponse response = requestMaker(posUri, positionJSON, RequestBuilder.put(), HAL_JSON);
        assertStatusCode(response, posUri.toString());
        URI uri = URI.create(ep.getSelfLink());
        JSONObject employeePositionJSON = getJsonFromUri(Projects.WFM, uri);

        employeePositionJSON.remove(DATE_INTERVAL);
        employeePositionJSON.remove(START_DATE);
        employeePositionJSON.remove(END_DATE);
        employeePositionJSON.remove(EMBEDDED);
        employeePositionJSON.remove(LINKS);

        JSONObject links = new JSONObject();
        links.put(POSITION, new JSONObject().put(HREF, ep.getPosition().getSelfLink()));
        links.put(EMPLOYEE, new JSONObject().put(HREF, ep.getEmployee().getSelfLink()));
        links.put(SELF, new JSONObject().put(HREF, ep.getSelfLink()));

        employeePositionJSON.put(LINKS, links);
        employeePositionJSON.put(POSITION, positionJSON);

        employeePositionJSON.put(DATE_INTERVAL, dateIntervalJson);
        employeePositionJSON.put(START_DATE, interval.getStartDate());
        employeePositionJSON.put(END_DATE, interval.getEndDate());
        LOG.info("employeePositionJSON: {}", employeePositionJSON);
        response = requestMaker(uri, employeePositionJSON, RequestBuilder.put(), HAL_JSON);
        assertStatusCode(response, uri.toString());
    }

    /**
     * Если начало назначения сотрудника после 1ого числа предыдущего месяца,
     * а конец позиции - ранее последнего числа следующего месяца, то интервал дат позиции меняется следующим образом:
     * 1ое число предыдущего месяца - последнее число следующего месяца
     *
     * @param ep - позиция сотрудника
     */
    public static void setDateIntervalForPositionIfNeeded(EmployeePosition ep) {
        if (!CommonRepository.URL_BASE.contains("magnit")) {
            LocalDate startDate = ep.getDateInterval().getStartDate();
            LocalDate endDate = ep.getDateInterval().getEndDate();
            LocalDate startDatePrevMonth = LocalDateTools.getFirstDate().minusMonths(1);
            LocalDate endDateNextMonth = LocalDate.now().plusMonths(1).with(TemporalAdjusters.lastDayOfMonth());
            if (startDate != null) {
                if (startDate.isAfter(startDatePrevMonth)) {
                    startDate = startDatePrevMonth;
                    setPositionDateInterval(ep, new DateInterval(startDatePrevMonth, endDate));
                }
            }
            if (endDate != null) {
                if (endDate.isBefore(endDateNextMonth)) {
                    setPositionDateInterval(ep, new DateInterval(startDate, endDateNextMonth));
                }
            }
        }
    }

    /**
     * Запустить расчет конфликтов
     *
     * @param rosterId айди ростера
     */
    public static void runConstViolationsCalc(int rosterId) {
        JSONObject miniObject = new JSONObject();
        URI uri = RequestFormers.setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(CONFLICT_CALCULATION, rosterId, ROSTER));
        HttpResponse response = requestMaker(uri, miniObject, RequestBuilder.put(), HAL_JSON);
        LOG.info(REQUEST_LOGGER, uri);
        assertStatusCode(response, uri.toString());
    }

    public static void makeAdditionalWorkEnabled(AdditionalWork addWork) {
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(SHIFTS_ADD_WORK, addWork.getId()));
        JSONObject miniObject = new JSONObject();
        miniObject.put(OUTER_ID, addWork.getOuterId());
        miniObject.put(TITLE, addWork.getTitle());
        miniObject.put(ID, addWork.getId());
        miniObject.put("disabled", false);
        miniObject.put(LINKS, addWork.getLinks());
        HttpResponse response = requestMaker(uri, miniObject, RequestBuilder.put(), HAL_JSON);
        LOG.info("Включаем доп. работу");
        LOG.info(REQUEST_LOGGER, uri);
        LOG.info(JSON_LOGGER, miniObject);
        assertStatusCode(response, uri.toString());

    }

    /**
     * Добавить причину привлечения сотрудника к свободной смене
     */
    public static void addHiringReason(Shift shift, ShiftHiringReason hiringReason) {
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(SHIFTS, shift.getId(), HIRING_REASON));
        JSONObject href = new JSONObject().put(HREF, hiringReason.getSelfLink());
        JSONObject shiftHiringReason = new JSONObject().put("shiftHiringReason", href);
        JSONObject miniObject = new JSONObject().put(LINKS, shiftHiringReason);
        HttpResponse response = RequestFormers.requestMaker(uri, miniObject, RequestBuilder.put(), HAL_JSON);
        LOG.info("Создаем причину привлечения сотрудника: {}", hiringReason.getTitle());
        assertStatusCode(response, 200, uri.toString());
    }

    public static void setCaptureLogsAttribute() {
        ITestContext c = Reporter.getCurrentTestResult().getTestContext();
        c.setAttribute("Capture logs", true);
    }

    public static void changeLimit(Limits limit) {
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(TYPED_LIMITS, limit.getSelfId()));
        JSONObject miniObject = getJsonFromUri(Projects.WFM, uri);
        miniObject.put(LIMIT, 8);
        HttpResponse response = requestMaker(uri, miniObject, RequestBuilder.put(), HAL_JSON);
        LOG.info("Меняем лимит для позиции: {}", limit.getJobTitleName());
        assertStatusCode(response, 200, uri.toString());
    }

    /**
     * Изменить значение атрибута мобильной группы у сотрудника
     */
    public static void toggleEmployeeMobileGroupStatus(Employee employee, boolean value) {
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(EMPLOYEES, employee.getId(), ENTITY_PROPERTIES));
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(VALUE, value);
        jsonObject.put(PROP_KEY, "mobile_group_employee");
        jsonObject.put(TYPE, "BOOLEAN");
        jsonObject.put(LOCAL, true);
        jsonObject.put(DELETED, false);

        JSONObject links = new JSONObject();
        JSONObject selfLink = new JSONObject();
        selfLink.put(HREF, uri.toString());
        links.put(SELF, selfLink);

        JSONObject entityPropertiesKeyLink = new JSONObject();
        entityPropertiesKeyLink.put(HREF, CommonRepository.URL_BASE + "/api/v1/entity-properties-key/28");
        links.put(Params.ENTITY_PROPERTIES_KEY, entityPropertiesKeyLink);
        jsonObject.put(LINKS, links);

        HttpResponse response = requestMaker(uri, jsonObject, RequestBuilder.post(), HAL_JSON);
        LOG.info("Изменяем атрибут МГ на  {} у сотрудника {}", value, employee.getFullName());
        assertStatusCode(response, 201, uri.toString());
    }

    public static void increaseLimitForPosition(int omId, EmployeePosition ep, EntityProperty property) {
        List<Limits> limits = LimitsRepository.getDayLimitForPosition(ep, property);
        for (Limits limit : limits) {
            if (limit.getLimit() < 8) {
                changeLimit(limit);
            }
        }
    }

    /**
     * @param value - скрыть или отобразить группу в системных списках
     */
    public static void hideFunctionality(PositionGroup posGroup, boolean value) {
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(POSITION_GROUPS, posGroup.getId()));
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(ID, posGroup.getId());
        jsonObject.put(NAME, posGroup.getName());
        jsonObject.put("hidden", value);
        JSONObject self = new JSONObject();
        self.put(SELF, new JSONObject().put(HREF, uri.toString()));
        jsonObject.put(LINKS, self);
        HttpResponse response = requestMaker(uri, jsonObject, RequestBuilder.put(), HAL_JSON);
        LOG.info("Скрывать группу в системных списках {} изменено на {}", posGroup.getName(), value);
        assertStatusCode(response, 200, uri.toString());
    }

    public static void makeKpiCorrection(OrgUnit unit, KpiType kpiType, List<LocalDate> dates) {
        int kpiSessId = KpiRepository.getKpiCorrectionSessionId(unit, kpiType);
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(KPI_CORRECTION_SESSIONS, kpiSessId, REPLACEMENTS));
        for (LocalDate date : dates) {
            JSONObject miniObject = new JSONObject();
            miniObject.put("datetime", date.format(DateTimeFormatter.ISO_LOCAL_DATE) + "T00:00");
            miniObject.put("delta", 800);
            miniObject.put("replacementType", "UNIFORM_DISTRIBUTION");
            miniObject.put("timeUnit", "MONTH");
            HttpResponse response = requestMaker(uri, miniObject, RequestBuilder.post(), HAL_JSON);
            assertStatusCode(response, 201, uri.toString());
        }
        LOG.info(REQUEST_LOGGER, uri);
        saveKpiCorrection(kpiSessId);
    }

    public static void saveKpiCorrection(Integer kpiCorrSessionId) {
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(KPI_CORRECTION_SESSIONS, kpiCorrSessionId));
        JSONObject miniObject = new JSONObject();
        miniObject.put(COMMENT, "test");
        miniObject.put("containsReplacements", true);
        miniObject.put(NUMBER, 1);
        miniObject.put(PUBLISHED, true);
        miniObject.put("timestamp", LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) + "T" + LocalTime.now());
        miniObject.put(USERNAME, "superuser");
        HttpResponse response = requestMaker(uri, miniObject, RequestBuilder.put(), HAL_JSON);
        assertStatusCode(response, 200, uri.toString());

    }

    public static void enableConflictCalculationInSysList(ConstraintViolations constraintViolation) {
        String urlEnding = makePath(CONFLICT, CONSTRAINT_VIOLATION_TYPES);
        JSONArray constraintViolationList = getJsonArrayFromUri(Projects.WFM, setUri(API, CommonRepository.URL_BASE, urlEnding), ImmutablePair.of("Wfm-Internal", getToken()));
        for (int i = 0; i < constraintViolationList.length(); i++) {
            JSONObject temp = constraintViolationList.getJSONObject(i);
            if (temp.getString(NAME).equals(constraintViolation.getName())) {
                if (!temp.getBoolean(ACTIVE)) {
                    temp.put(ACTIVE, true);
                    LOG.info(constraintViolationList.toString());
                    urlEnding = makePath(urlEnding, "update");
                    URI uri = setUri(API, CommonRepository.URL_BASE, urlEnding);
                    HttpResponse response = requestMaker(uri, constraintViolationList, RequestBuilder.put(),
                                                         ContentType.APPLICATION_JSON, Projects.WFM, ImmutablePair.of("Wfm-Internal", getToken()));
                    assertStatusCode(response, uri.toString());
                    Allure.addAttachment("Статус конфликта в системном списке \"Настройка отображения конфликтов\"",
                                         String.format("Конфликт %s был включен пресетом", constraintViolation.getName()));
                    return;
                }
            }
        }
    }

    /**
     * Назначить сотрудника на должность
     */
    public static EmployeePosition appointAnEmployeeToPosition(Position position, Employee employee, OrgUnit orgUnit,
                                                               LocalDate startDate, LocalDate endDate) {
        URI uriPosition = setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(POSITIONS));
        JSONObject positionJson = createPositionAppointmentJson(position, orgUnit, uriPosition.toString(), startDate, endDate);
        HttpResponse responsePosition = requestMaker(uriPosition, positionJson, RequestBuilder.post(), HAL_JSON);
        assertStatusCode(responsePosition, 201, responsePosition.toString());

        JSONObject employeeJson = createEmployeeAppointmentJson(employee, orgUnit, startDate, endDate);

        int positionId = DBUtils.getPositionId(position, orgUnit, startDate);
        URI uriEmployeePosition = setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(POSITIONS, positionId, EMPLOYEE_POSITIONS));
        JSONObject employeePosition = createEmployeePositionAppointmentJson(positionJson, employeeJson, uriEmployeePosition.toString());
        HttpResponse responseEmployeePosition = requestMaker(uriEmployeePosition, employeePosition, RequestBuilder.post(), HAL_JSON);
        assertStatusCode(responseEmployeePosition, 201, responseEmployeePosition.toString());
        return getCreatedObject(responseEmployeePosition, EmployeePosition.class);
    }

    /**
     * @param startDate    - какая на данный момент дата начала работы
     * @param newStartDate - новая дата начала работы
     * @param newEndDate   - новая дата окончания работы
     */

    public static EmployeePosition updateEmployeeStartDateInSchedule(Position position, Employee employee, OrgUnit orgUnit,
                                                                     LocalDate startDate, LocalDate newStartDate, LocalDate newEndDate) {

        int positionId = DBUtils.getPositionId(position, orgUnit, startDate);
        URI uriPosition = setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(POSITIONS, positionId));
        JSONObject positionJson = createPositionAppointmentJson(position, orgUnit, uriPosition.toString(), newStartDate, newEndDate);
        HttpResponse responsePosition = requestMaker(uriPosition, positionJson, RequestBuilder.put(), HAL_JSON);
        assertStatusCode(responsePosition, 200, responsePosition.toString());

        JSONObject employeeJson = createEmployeeAppointmentJson(employee, orgUnit, newStartDate, newEndDate);

        int employeePositionId = DBUtils.getEmployeePositionId(positionId);
        URI uriEmployeePosition = setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(EMPLOYEE_POSITIONS, employeePositionId));
        JSONObject employeePosition = createEmployeePositionAppointmentJson(positionJson, employeeJson, uriEmployeePosition.toString());
        HttpResponse responseEmployeePosition = requestMaker(uriEmployeePosition, employeePosition, RequestBuilder.put(), HAL_JSON);
        assertStatusCode(responseEmployeePosition, 200, responseEmployeePosition.toString());
        return position.refreshPositions().getEmployeePosition();
    }

    private static JSONObject createPositionAppointmentJson(Position position, OrgUnit orgUnit, String selfUrl,
                                                            LocalDate startDate, LocalDate endDate) {
        PositionCategory positionCategory = PositionCategoryRepository.getPositionCategoryById(position.getPositionCategoryId());
        PositionType positionType = PositionTypeRepository.getPositionTypeById(position.getPositionTypeId());
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(ACTIVE, true);
        jsonObject.put(POSITION_CATEGORY, new JSONObject()
                .put(FOR_EXCHANGE, JSONObject.NULL)
                .put(NAME, positionCategory.getName())
                .put(CATEGORY_ID, positionCategory.getCategoryId())
                .put(OUTER_ID, positionCategory.getOuterId())
                .put(LINKS, positionType.getLinks()));
        jsonObject.put(JOB_TITLE, positionType.getName());
        jsonObject.put(POSITION_TYPE, new JSONObject()
                .put(NAME, positionType.getName())
                .put(POSITION_INDEX, JSONObject.NULL)
                .put(OUTER_ID, positionType.getOuterId())
                .put(LINKS, positionType.getLinks())
                .put(ID, positionType.getId())
        );
        jsonObject.put(DATE_INTERVAL, new JSONObject()
                .put(START_DATE, startDate)
                .put(END_DATE, endDate)
        );
        jsonObject.put(OUTER_ID, position.getOuterId());
        jsonObject.put(LINKS, new JSONObject()
                .put(SELF, new JSONObject()
                        .put(HREF, selfUrl)
                )
                .put(POSITION_TYPE, new JSONObject()
                        .put(HREF, positionType.getLink(SELF))
                )
                .put(POSITION_CATEGORY, new JSONObject()
                        .put(HREF, positionCategory.getLink(SELF))
                )
                .put(ORG_UNIT_JSON, new JSONObject()
                        .put(HREF, orgUnit.getLinks().get(SELF))
                )
        );
        jsonObject.put(NAME, position.getName());
        return jsonObject;
    }

    private static JSONObject createEmployeeAppointmentJson(Employee employee, OrgUnit orgUnit, LocalDate startDate, LocalDate endDate) {
        JSONObject jsonObject = new JSONObject();

        jsonObject.put(ID, JSONObject.NULL);
        jsonObject.put(DATE_INTERVAL, new JSONObject()
                .put(START_DATE, startDate)
                .put(END_DATE, endDate)
        );
        jsonObject.put(START_DATE, startDate);
        jsonObject.put(END_DATE, endDate);
        jsonObject.put(EMPLOYEE, new JSONObject()
                .put(ORG_UNIT_ID, orgUnit.getId())
                .put(OUTER_ID, employee.getOuterId())
                .put(FIRST_NAME, employee.getFirstName())
                .put(PATRONYMIC_NAME, employee.getPatronymicName())
                .put(LAST_NAME, employee.getLastName())
                .put(START_WORK_DATE, employee.getStartWorkDate())
                .put(END_WORK_DATE, employee.getEndWorkDate())
                .put(GENDER, employee.getGender())
                .put(SNILS, employee.getSnils())
                .put(EMAIL, employee.getEmail())
                .put(NEED_MENTOR, false)
                .put(VIRTUAL, false)
                .put(PHONE, JSONObject.NULL)
                .put(OUT_SOURCE, false)
                .put(LINKS, new JSONObject()
                        .put(SELF, new JSONObject()
                                .put(HREF, employee.getLink(SELF)
                                )
                        )
                        .put(ID, employee.getId())
                        .put(EMBEDDED, new JSONObject()
                                .put(AVATAR, new JSONObject()
                                        .put(EXIST, false)
                                        .put(LINKS, new JSONObject()
                                                .put(SELF, new JSONObject()
                                                        .put(HREF, employee.getLink(SELF) + "/avatar")
                                                )
                                        )
                                )
                        )
                        .put(LOCAL, true)
                ));
        return jsonObject;
    }

    private static JSONObject createEmployeePositionAppointmentJson(JSONObject positionJson, JSONObject employeeJson, String selfUrl) {
        JSONObject employeePositionJson = new JSONObject();
        employeePositionJson.put(POSITION, positionJson);
        employeePositionJson.put(EMPLOYEE, employeeJson);
        employeePositionJson.put(LINKS, new JSONObject()
                .put(SELF, new JSONObject()
                        .put(HREF, selfUrl)
                )
                .put(EMPLOYEE, new JSONObject()
                        .put(HREF, employeeJson.getJSONObject(EMPLOYEE).getJSONObject(LINKS).getJSONObject(SELF).optString(HREF))
                )
                .put(POSITION, new JSONObject()
                        .put(HREF, selfUrl.substring(0, selfUrl.lastIndexOf('/')))
                )

        );
        employeePositionJson.put(IS_EDIT, true);
        employeePositionJson.put(DISMISSED, JSONObject.NULL);
        employeePositionJson.put(TEMPORARY, JSONObject.NULL);
        employeePositionJson.put(CARD_NUMBER, RandomStringUtils.randomNumeric(6));
        employeePositionJson.put(RATE, 1);
        employeePositionJson.put(OPERATIONAL_ZONE_TITLE, JSONObject.NULL);
        employeePositionJson.put(OPERATIONAL_ZONE_ID, JSONObject.NULL);
        employeePositionJson.put(SHOW, true);
        return employeePositionJson;
    }

    /**
     * Отключает обязательные доп работы
     */
    public static void disableRequiredAddWorks() {
        for (AdditionalWork w : AdditionalWorkRepository.getAdditionalWorks()) {
            List<AddWorkRule> rules = AddWorkRule.getAllRulesOfAddWork(w.getId());
            List<AddWorkRule> requiredRules = rules.stream()
                    .filter(r -> r.getRequiredType() != null && r.getRequiredType() != "")
                    .collect(Collectors.toList());
            if (!requiredRules.isEmpty()) {
                disableAddWork(w);
            }
        }
    }

    /**
     * Отключает доп работу (disabled = true)
     */
    public static void disableAddWork(AdditionalWork work) {
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(SHIFTS_ADD_WORK, work.getSelfId()));
        JSONObject miniObject = getJsonFromUri(Projects.WFM, uri);
        miniObject.put("disabled", true);
        HttpResponse response = requestMaker(uri, miniObject, RequestBuilder.put(), HAL_JSON);
        assertStatusCode(response, 200, uri.toString());
    }

    /**
     * Выставляет или снимает доп работе флажок "Использует статусы"
     * в зависимости от значения параметра "status"
     */
    public static AdditionalWork changeStatus(AdditionalWork work, boolean status) {
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(SHIFTS_ADD_WORK, work.getSelfId()));
        JSONObject miniObject = getJsonFromUri(Projects.WFM, uri);
        miniObject.put("hasStatuses", status);
        HttpResponse response = requestMaker(uri, miniObject, RequestBuilder.put(), HAL_JSON);
        assertStatusCode(response, 200, uri.toString());
        return work.setHasStatuses(status);
    }

    public static Employee updateEmployee(Employee employee) {
        JSONObject employeeJson = new JSONObject();
        employeeJson.put(FIRST_NAME, employee.getFirstName());
        employeeJson.put(PATRONYMIC_NAME, employee.getPatronymicName());
        employeeJson.put(LAST_NAME, employee.getLastName());
        employeeJson.put(GENDER, employee.getGender());
        employeeJson.put(SNILS, employee.getSnils());
        employeeJson.put(EMAIL, employee.getEmail());
        employeeJson.put(START_WORK_DATE, employee.getStartWorkDate());
        employeeJson.put(END_WORK_DATE, employee.getEndWorkDate());
        employeeJson.put(NEED_MENTOR, employee.isNeedMentor());
        employeeJson.put(OUTER_ID, employee.getOuterId());
        employeeJson.put(ID, employee.getId());
        JSONObject links = new JSONObject();
        JSONObject self = new JSONObject();
        self.put(HREF, employee.getSelfLink());
        links.put(SELF, self);
        employeeJson.put(LINKS, links);

        new ApiRequest.PutBuilder(makePath(EMPLOYEES, employee.getId()))
                .withBody(employeeJson.toString())
                .withStatus(200)
                .send();
        return employee.refreshEmployee();
    }

    /**
     * Сгенерировать случайный outerId
     *
     * @param length       - длинна id
     * @param dashPosition - через сколько символов ставить тире
     */
    public static String generateRandomStringWithDash(int length, int dashPosition) {
        StringBuilder sb = new StringBuilder(RandomStringUtils.random(length, true, true));
        for (int i = dashPosition; i < sb.length(); i += dashPosition + 1) {
            sb.insert(i, '-');
        }
        return sb.toString();
    }

    public static JSONObject createScheduleRequest(EmployeePosition emp, String type, LocalDate startDate,
                                                   LocalDate endDate, LocalTime startTime, LocalTime endTime) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(POSITION_OUTER_ID, emp.getPosition().getOuterId());
        jsonObject.put(EMPLOYEE_OUTER_ID, emp.getEmployee().getOuterId());
        jsonObject.put(TYPE, type);
        jsonObject.put(START_DATE, startDate);
        jsonObject.put(END_DATE, endDate);
        jsonObject.put(START_TIME, startTime);
        jsonObject.put(END_TIME, endTime);
        return jsonObject;
    }

    @Step("Создать запрос отсутствия {alias.type} за {dateTimeInterval}")
    public static ScheduleRequest createRequestAbsenceInDateInterval(EmployeePosition emp, ScheduleRequestAlias alias,
                                                                     DateTimeInterval dateTimeInterval, int omId) {
        JSONObject jsonObject = createScheduleRequest(emp, alias.getOuterId(), dateTimeInterval.getStartDate(),
                                                      dateTimeInterval.getEndDate(), dateTimeInterval.getStartDateTime().toLocalTime(), dateTimeInterval.getEndDateTime().toLocalTime());
        new ApiRequest.PostBuilder(makePath(INTEGRATION_JSON, SCHEDULE_REQUESTS))
                .withBody(new JSONArray().put(jsonObject).toString())
                .withParams(Pairs.newBuilder().stopOnError(false).deleteIntersections(true).processShifts("true").buildMap())
                .withStatus(200)
                .send();
        return ScheduleRequestRepository
                .getEmployeeScheduleRequests(emp.getEmployee().getId(), dateTimeInterval.toDateInterval(), omId)
                .stream().filter(s -> s.getType().toString().equals(alias.getType()) && s.getDateTimeInterval().equals(dateTimeInterval))
                .findFirst().orElseThrow(() -> new AssertionError(String.format("Тип запроса %s за %s у %s не найден",
                                                                                alias.getType(), dateTimeInterval.toDateInterval(), emp.getEmployee().getFullName())));

    }

    @Step("Удалить запрос отсутствия {alias.type} за {dateTimeInterval}")
    public static void deleteRequestAbsenceInDateInterval(EmployeePosition emp, ScheduleRequestAlias alias,
                                                          DateTimeInterval dateTimeInterval, Map<String, String> params, String path) {
        JSONObject jsonObject = createScheduleRequest(emp, alias.getOuterId(), dateTimeInterval.getStartDate(),
                                                      dateTimeInterval.getEndDate(), dateTimeInterval.getStartDateTime().toLocalTime(), dateTimeInterval.getEndDateTime().toLocalTime());
        ApiRequest.Builder request = URL_BASE.contains("efes") ? new ApiRequest.DeleteBuilder(path) : new ApiRequest.PostBuilder(path);
        request.withBody(new JSONArray().put(jsonObject).toString())
                .withParams(params)
                .withStatus(200)
                .send();
    }

    /**
     * Смотрит все employeePositions у сотрудника, меняет даты окончания работы таким образом,
     * чтобы у сотрудника были и открытые, и закрытые назначения
     */
    public static void presetForOpenedAndClosedEmployeePositions(Employee employee) {
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(EMPLOYEES, employee.getId(), EMPLOYEE_POSITIONS));
        JSONObject json = getJsonFromUri(Projects.WFM, uri);
        List<EmployeePosition> employeePositions = getListFromJsonObject(json, EmployeePosition.class);
        boolean hasOpenedPositions = employeePositions.stream().anyMatch(ep -> ep.getDateInterval().includesDate(LocalDate.now()));
        boolean hasClosedPositions = employeePositions.stream().anyMatch(ep -> ep.getDateInterval().getEndDate().isBefore(LocalDate.now()));
        if (!(hasOpenedPositions && hasClosedPositions)) {
            EmployeePosition employeePosition = getRandomFromList(employeePositions);
            if (!hasOpenedPositions) {
                setPositionDateInterval(employeePosition, new DateInterval(employeePosition.getDateInterval().startDate, null));
                if (employeePosition.isTemporary()) {
                    new ApiRequest.PutBuilder(makePath(EMPLOYEE_POSITIONS, employeePosition.getId())).withBody(employeePosition.setTemporary(false)).withStatus(200).send();
                }
            } else {
                setPositionDateInterval(employeePosition, new DateInterval(employeePosition.getDateInterval().startDate, LocalDate.now().withDayOfMonth(1)));
            }
        }
    }

    public static void presetForClosedEmployeePositions(Employee employee) {
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(EMPLOYEES, employee.getId(), EMPLOYEE_POSITIONS));
        JSONObject json = getJsonFromUri(Projects.WFM, uri);
        List<EmployeePosition> employeePositions = getListFromJsonObject(json, EmployeePosition.class);
        boolean hasClosedPositions = employeePositions.stream().allMatch(ep -> ep.getDateInterval().getEndDate().isBefore(LocalDate.now()));
        if (!hasClosedPositions) {
            List<EmployeePosition> openedPositions = employeePositions.stream().filter(ep -> ep.getDateInterval().includesDate(LocalDate.now())).collect(Collectors.toList());
            for (EmployeePosition employeePosition : openedPositions) {
                int index = openedPositions.indexOf(employeePosition);
                setPositionDateInterval(employeePosition, new DateInterval(employeePosition.getDateInterval().startDate, LocalDate.now().minusDays(index)));
                if (employeePosition.isTemporary()) {
                    new ApiRequest.PutBuilder(makePath(EMPLOYEE_POSITIONS, employeePosition.getId())).withBody(employeePosition.setTemporary(false)).withStatus(200).send();
                }
            }
        }
    }

    public static void presetForOpenedAndClosedEmployeePositionsThisMonth(Employee employee) {
        DateInterval allMonth = ShiftTimePosition.ALLMONTH.getShiftsDateInterval();
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(EMPLOYEES, employee.getId(), EMPLOYEE_POSITIONS));
        JSONObject json = getJsonFromUri(Projects.WFM, uri);
        List<EmployeePosition> employeePositions = getListFromJsonObject(json, EmployeePosition.class);
        boolean hasPositionsOpeningThisMonth = employeePositions.stream().anyMatch(employeePosition -> allMonth.includesDate(employeePosition.getDateInterval().startDate));
        boolean hasPositionsClosingThisMonth = employeePositions.stream().anyMatch(employeePosition -> {
            LocalDate endDate = employeePosition.getDateInterval().endDate;
            if (endDate != null) {
                return allMonth.includesDate(endDate);
            } else {
                return false;
            }
        });
        if (!(hasPositionsClosingThisMonth && hasPositionsOpeningThisMonth)) {
            List<EmployeePosition> twoEmployeePositions = getRandomFromList(employeePositions, 2);
            setPositionDateInterval(twoEmployeePositions.get(0), new DateInterval(allMonth.startDate, LocalDate.now()));
            setPositionDateInterval(twoEmployeePositions.get(1), new DateInterval(LocalDate.now().plusDays(1), allMonth.endDate));
            for (EmployeePosition employeePosition : twoEmployeePositions) {
                if (employeePosition.isTemporary()) {
                    new ApiRequest.PutBuilder(makePath(EMPLOYEE_POSITIONS, employeePosition.getId())).withBody(employeePosition.setTemporary(false)).withStatus(200).send();
                }
            }
        }
    }
}

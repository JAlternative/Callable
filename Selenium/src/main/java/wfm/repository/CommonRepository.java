package wfm.repository;

import io.qameta.allure.Allure;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.Links;
import utils.Params;
import utils.Projects;
import utils.tools.LocalDateTools;
import utils.tools.Pairs;
import wfm.ApiRequest;
import wfm.components.orgstructure.ConstraintViolations;
import wfm.components.schedule.AppDefaultLocale;
import wfm.components.schedule.KPIOrFTE;
import wfm.components.schedule.ShiftTimePosition;
import wfm.components.utils.OmDbType;
import wfm.models.*;
import wfm.models.PhoneType;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

import static utils.ErrorMessagesForReport.NO_TEST_DATA;
import static utils.Links.EMPLOYEE_POSITIONS;
import static utils.Links.*;
import static utils.Params.MATH_PARAMETER;
import static utils.Params.TAGS;
import static utils.Params.*;
import static utils.tools.CustomTools.*;
import static utils.tools.RequestFormers.*;
import static wfm.components.schedule.SystemProperties.APP_DEFAULT_LOCALE;
import static wfm.repository.SystemPropertyRepository.getSystemProperty;

public class CommonRepository {

    public static final String URL_BASE = Links.getTestProperty(RELEASE);
    public static final String NOTIFY_APP = Links.getTestProperty("notify_app");
    private static final Logger LOG = LoggerFactory.getLogger(CommonRepository.class);
    private static final Random RANDOM = new Random();

    private CommonRepository() {
    }

    //TODO еще детальнее посмотреть на эти запросы, если надо создать сущности

    /**
     * Проверяет количество навыков
     *
     * @param id - айди сотрудника
     * @return - количество навыков
     */
    public static int checkValueOfSkill(int id) {
        String urlEnding = makePath(EMPLOYEES, id, SKILL_VALUES);
        JSONObject someObjectKpiValues = getJsonFromUri(Projects.WFM, URL_BASE, urlEnding);
        JSONArray jsonArrayFromJsonObject = getJsonArrayFromJsonObject(someObjectKpiValues);
        int size = 0;
        if (jsonArrayFromJsonObject == null) {
            Allure.addAttachment("Проверка", "У вабранного сотрудника навыки отстутсвтуют");
        } else {
            size = jsonArrayFromJsonObject.length();
            Allure.addAttachment("Проверка", "У выбранного сотрудника имеется " + size + " навык(а)");
        }
        return size;
    }

    /**
     * Берет массив значений мат параметров
     *
     * @param omNumber - номер оргюнита
     * @return массив значений JSON
     */
    public static JSONArray getMathParameterValuesArray(int omNumber) {
        String urlPath = makePath(ORGANIZATION_UNITS, omNumber, MATH_PARAMETER_VALUES);
        JSONObject someObjectPositionsForSearch = getJsonFromUri(Projects.WFM, URL_BASE, urlPath);
        int pageSize = someObjectPositionsForSearch.getJSONObject(PAGE).getInt("totalElements");
        List<NameValuePair> nameValuePairs = Pairs.newBuilder()
                .page(0)
                .size(pageSize)
                .build();
        JSONObject someObjectMathParameter = getJsonFromUri(Projects.WFM, URL_BASE, urlPath, nameValuePairs);
        return getJsonArrayFromJsonObject(someObjectMathParameter);
    }

    /**
     * Смотрит мат параметры
     *
     * @return возвращает имя параметра и его значение
     */
    public static Map<String, String> getMathParameterValues(int omNumber) {
        JSONArray jsonArrayMathParameterValues = getMathParameterValuesArray(omNumber);
        Map<String, String> conformity = new HashMap<>();
        for (int i = 0; i < jsonArrayMathParameterValues.length(); i++) {
            JSONObject tempObj = jsonArrayMathParameterValues.getJSONObject(i);
            Object tempValue = tempObj.get(VALUE);
            Object tempParam = tempObj.getJSONObject(LINKS).getJSONObject(MATH_PARAMETER).get(HREF);
            URI valueParam = URI.create(String.valueOf(tempParam));
            JSONObject someObjectCommonName = new JSONObject(setUrlAndInitiateForApi(valueParam, Projects.WFM));
            Object paramName = someObjectCommonName.get(SHORT_NAME);
            String value = String.valueOf(tempValue);
            String name = String.valueOf(paramName);
            conformity.put(name, value);
        }
        return conformity;
    }

    /**
     * Проверяет комментарий к расписанию за указанную дату
     *
     * @param date - дата для проверки комментария
     * @return возвращает false если комментарий есть и true Если комментария нет
     */
    public static Boolean getEmptyDayCommentNotExistStatus(OrgUnit orgUnit, LocalDate date) {
        String urlEnding = makePath(ORGANIZATION_UNITS, orgUnit.getId(), WORKING_SCHEDULE_DAYS, COMMENTS);
        List<NameValuePair> nameValuePairs = Pairs.newBuilder()
                .from(date)
                .to(date)
                .build();
        JSONObject someObject = getJsonFromUri(Projects.WFM, URL_BASE, urlEnding, nameValuePairs);
        return getJsonArrayFromJsonObject(someObject) == null;
    }

    /**
     * @param scheduleId - айди текущего графика
     * @return хэшкарту, где ключ - дата исключения, а значение - тип исключения.
     */
    public static Map<LocalDate, String> getSpecialDays(DateInterval dateInterval, String scheduleId) {
        String urlEnding = makePath(BUSINESS_HOURS_LIST, scheduleId, SPECIAL_DAYS);
        Map<LocalDate, String> specialDays = new HashMap<>();
        List<NameValuePair> nameValuePairs = Pairs.newBuilder()
                .from(dateInterval.startDate)
                .to(dateInterval.endDate)
                .build();
        JSONObject temp = getJsonFromUri(Projects.WFM, URL_BASE, urlEnding, nameValuePairs);
        JSONArray tempSpecialDays = getJsonArrayFromJsonObject(temp);
        for (int i = 0; i < tempSpecialDays.length(); i++) {
            JSONObject tempObj = tempSpecialDays.getJSONObject(i);
            String date = tempObj.getString(DATE);
            String kpiBehavior = tempObj.getString("kpiBehavior");
            specialDays.put(LocalDate.parse(date), kpiBehavior);
        }
        return specialDays;
    }

    public static JSONArray getSpecialDaysArray(String num) {
        String urlSpecialDays = makePath(BUSINESS_HOURS_LIST, num, SPECIAL_DAYS);
        JSONObject jsonSpecialDays = getJsonFromUri(Projects.WFM, URL_BASE, urlSpecialDays,
                                                    Pairs.newBuilder().size(100000).build());
        if (jsonSpecialDays.has(EMBEDDED)) {
            return jsonSpecialDays.getJSONObject(EMBEDDED).getJSONArray(REL_SPECIAL_DAYS);
        }
        return new JSONArray();
    }

    /**
     * @return scheduleType  Map в которой ключ - Id_расписания, значение - Рабочие_дни
     */
    public static Map<Integer, String> getWorkingDays(String scheduleId) {
        String urlEnding = makePath(BUSINESS_HOURS_LIST, scheduleId, DAYS);
        Map<Integer, String> workingDays = new HashMap<>();
        for (int i = 1; i <= 7; i++) {
            workingDays.put(i, null);
        }
        JSONArray weekdays = getJsonArrayFromJsonObject(getJsonFromUri(Projects.WFM, URL_BASE, urlEnding));
        if (weekdays != null) {
            for (int j = 0; j < weekdays.length(); j++) {
                JSONObject tempObj = weekdays.getJSONObject(j);
                Integer isoWeekday = tempObj.getInt(ISO_WEEK_DAY);
                String dayId = tempObj.getJSONObject(LINKS).getJSONObject(SELF).getString(HREF);
                workingDays.replace(isoWeekday, dayId.substring(dayId.lastIndexOf("/")).substring(1));
            }
            return workingDays;
        } else {
            return workingDays;
        }
    }

    /**
     * @param scheduleId айди текущего расписания
     * @param isoWeekDay порядковый номер дня
     * @return хэшкарту из даты начала и окончания рабочего дня.
     */
    public static Map<String, String> getWorkingDaysTime(String scheduleId, int isoWeekDay) {
        String urlEnding = makePath(BUSINESS_HOURS_LIST, scheduleId, DAYS);
        JSONArray weekdays = getJsonFromUri(Projects.WFM, URL_BASE, urlEnding)
                .getJSONObject(EMBEDDED).getJSONArray("days");
        Map<String, String> workingDays = new HashMap<>();
        for (int j = 0; j < weekdays.length(); j++) {
            JSONObject tempObj = weekdays.getJSONObject(j);
            if (tempObj.getInt(ISO_WEEK_DAY) == isoWeekDay) {
                JSONObject timeInterval = tempObj.getJSONObject(TIME_INTERVAL);
                String startTime = timeInterval.get(START_TIME).toString();
                String endTime = timeInterval.get(END_TIME).toString();
                workingDays.put(START_TIME, startTime.substring(0, 5));
                workingDays.put(END_TIME, endTime.substring(0, 5));
                return workingDays;
            }
        }
        return new HashMap<>();
    }

    /**
     * @param scheduleId айди расписания
     * @return возвращает список выходных(исключений)
     */
    public static List<String> getBusinessHoursDaysOff(String scheduleId) {
        String urlEnding = makePath(BUSINESS_HOURS_LIST, scheduleId, BUSINESS_HOURS_DAYS_OFF);
        JSONObject businessHours = getJsonFromUri(Projects.WFM, URL_BASE, urlEnding, Pairs.newBuilder().size(100000).build());
        JSONArray tempDays = businessHours.has(EMBEDDED) ?
                businessHours.getJSONObject(EMBEDDED).getJSONArray("businessHoursDaysOff") : new JSONArray();
        ArrayList<String> daysOff = new ArrayList<>();
        for (int i = 0; i < tempDays.length(); i++) {
            JSONObject tempObj = tempDays.getJSONObject(i);
            String date = tempObj.get(DATE).toString();
            daysOff.add(date);
        }
        return daysOff;
    }

    /**
     * возвращает список имен и айди типов событий
     */
    public static Map<Integer, String> getEventTypes() {
        Map<Integer, String> eventTypeNames = new HashMap<>();
        JSONObject eventTypes = getJsonFromUri(Projects.WFM, URL_BASE, ORGANIZATION_UNIT_EVENT_TYPES);
        JSONArray eventTypesArray = eventTypes.getJSONObject(EMBEDDED).getJSONArray(REL_ORGANIZATION_UNIT_EVENT_TYPES);
        for (int i = 0; i < eventTypesArray.length(); i++) {
            JSONObject eventType = eventTypesArray.getJSONObject(i);
            String eventTypeName = eventType.getString(NAME);
            int eventTypeId = eventType.getInt(ID);
            eventTypeNames.put(eventTypeId, eventTypeName);
        }
        return eventTypeNames;
    }

    /**
     * возвращает список имен и айди групп позиций
     */
    public static Map<Integer, String> getPositionGroups() {
        Map<Integer, String> positionGroups = new HashMap<>();
        JSONObject groups = getJsonFromUri(Projects.WFM, URL_BASE, POSITION_GROUPS);
        JSONArray groupsArray = groups.getJSONObject(EMBEDDED).getJSONArray(REL_POSITION_GROUPS);
        for (int i = 0; i < groupsArray.length(); i++) {
            JSONObject positionGroup = groupsArray.getJSONObject(i);
            String groupName = positionGroup.getString(NAME);
            int groupId = positionGroup.getInt(ID);
            positionGroups.put(groupId, groupName);
        }
        return positionGroups;
    }

    public static List<String> getVisibleKpiOptions() {
        JSONArray array = getJsonFromUri(Projects.WFM, URL_BASE, makePath(SYSTEM_PROPERTIES))
                .getJSONObject(EMBEDDED).getJSONArray(REL_PROPERTIES);
        for (int i = 0; i < array.length(); i++) {
            JSONObject temp = array.getJSONObject(i);
            if (temp.getString(KEY).equals("kpi.visible")) {
                return Arrays.asList(temp.getString(VALUE).split(","));
            }
        }
        return new ArrayList<>();
    }

    /**
     * @param orgId        id просматриваемого оргюнита.
     * @param kpiExist     параметр наличия прогноза расчета.
     * @param previousYear с учетом предыдущего года
     * @return months  месяца с наличием/отсутствием прогноза расчета.
     */
    public static List<Integer> getOrgUnitsKpiMonth(int orgId, boolean kpiExist, boolean previousYear) {
        int year;
        LocalDate now = LocalDate.now();
        if (previousYear) {
            year = now.getYear() - 1;
        } else {
            year = now.getYear();
        }
        LocalDate last = LocalDate.of(year, 12, 31);
        LocalDate start = LocalDate.of(year, 1, 1);
        String urlEnding = getCorrectionUri(orgId);
        if (urlEnding == null) {
            return new ArrayList<>();
        }
        String[] parts = urlEnding.split("v1/");
        List<NameValuePair> pairs = Pairs.newBuilder()
                .from(start)
                .to(last)
                .timeUnit("MONTH").build();
        JSONObject array = getJsonFromUri(Projects.WFM, parts[0], parts[1], pairs);
        List<Kpi> kpiList = getListFromJsonObject(array, Kpi.class);
        return kpiList.stream()
                .filter(kpi -> kpi.getValue() > 0.0 && kpiExist)
                .map(kpi -> kpi.getDateTime().getMonthValue())
                .collect(Collectors.toList());
    }

    /**
     * @param orgId    id просматриваемого оргюнита
     * @param fteExist параметр наличия ресурсной потребности
     * @return months месяца с наличием/отсутствием ресурсной потребности
     */
    public static List<Integer> getOrgUnitsFteMonth(int orgId, boolean fteExist, boolean previousYear) {
        int year;
        LocalDate now = LocalDate.now();
        if (previousYear) {
            year = now.getYear() - 1;
        } else {
            year = now.getYear();
        }
        LocalDate last = LocalDate.of(year, 12, 31);
        LocalDate start = LocalDate.of(year, 1, 1);
        String urlEnding = makePath(ORGANIZATION_UNITS, orgId, FTE_GROUPS);
        List<NameValuePair> pairs = Pairs.newBuilder().from(start).to(last).level(1).build();
        JSONArray array = getJsonFromUri(Projects.WFM, URL_BASE, urlEnding, pairs)
                .getJSONObject(EMBEDDED)
                .getJSONArray("fteList");
        List<Integer> months = new ArrayList<>();
        for (int j = 0; j < array.length(); j++) {
            JSONObject temp = array.getJSONObject(j);
            if (temp.getJSONObject("values").getDouble("FULL") == 0.0 ^ fteExist) {
                int month = LocalDateTime.parse(temp.getString("datetime")).getMonthValue();
                months.add(month);
            }
        }
        return months;
    }

    /**
     * Находит даты публикации KPI в оргюните.
     */
    public static List<LocalDate> checkMonthsPublication(int orgId, KPIOrFTE kpiOrFTE) {
        String path = KPIOrFTE.FTE == kpiOrFTE ? PUBLISHED_FTE_LISTS : makePath(PUBLISHED_FORECASTS, 1);
        String urlEnding = makePath(ORGANIZATION_UNITS, orgId, path);
        JSONObject jsonObject = getJsonFromUri(Projects.WFM, URL_BASE, urlEnding);
        List<LocalDate> dateList = new ArrayList<>();
        if (jsonObject.isNull(EMBEDDED)) {
            return dateList;
        }
        String arrayPath = KPIOrFTE.FTE == kpiOrFTE ? REL_PUBLISHED_FTE_LISTS : REL_PUBLISHED_FORECASTS;
        JSONArray publishedForecasts = jsonObject.getJSONObject(EMBEDDED).getJSONArray(arrayPath);
        for (int i = 0; i < publishedForecasts.length(); i++) {
            JSONObject monthObject = publishedForecasts.getJSONObject(i);
            LocalDate month = LocalDate.parse(monthObject.getString("month"));
            dateList.add(month);
        }
        return dateList;
    }

    /**
     * должны возвращаться значения kpi. Если результат выполнения запроса пустой, то оргюнит использовать нельзя
     *
     * @param orgId - айди оргюнита
     * @return сломанный false, исправный true
     */
    public static boolean checkKpiValuesNotBroken(int orgId) {
        int year = LocalDate.now().getYear();
        LocalDate last = LocalDateTools.getDate(LocalDateTools.THAT, LocalDateTools.THAT, LocalDateTools.THAT);
        LocalDate start = LocalDateTools.getDate(year - 1, LocalDateTools.THAT, LocalDateTools.THAT);
        String urlEnding = makePath(ORGANIZATION_UNITS, orgId, KPI, 1, KPI_FORECAST_VALUES);
        List<NameValuePair> pairs = Pairs.newBuilder().from(start).to(last).level(0).build();
        JSONObject jsonObject = getJsonFromUri(Projects.WFM, URL_BASE, urlEnding, pairs);
        return !jsonObject.isNull(EMBEDDED);
    }

    public static String getCorrectionUri(int omId) {
        try {
            String path = makePath(ORGANIZATION_UNITS, omId, KPI, 1, KPI_FORECAST_CORRECTION_SESSION);
            String href = getJsonFromUri(Projects.WFM, URL_BASE, path)
                    .optJSONObject(LINKS)
                    .optJSONObject(REL_KPI_FORECAST_DIAGNOSTICS_VALUES)
                    .optString(HREF);
            return href.split("\\{")[0];
        } catch (AssertionError e) {
            LOG.info("У оргюнита {} не был найден kpi", omId);
        }
        return null;
    }

    /**
     * Берет список видимых названий KPI
     */
    public static List<String> getVisibleKpiNames() {
        List<String> outerIds = getVisibleKpiOptions();
        JSONArray array = getJsonFromUri(Projects.WFM, URL_BASE, makePath(KPI_LIST))
                .getJSONObject(EMBEDDED).getJSONArray(REL_KPIS);
        List<String> names = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject temp = array.getJSONObject(i);
            if (outerIds.contains(temp.getString(OUTER_ID))) {
                names.add(temp.getString(NAME));
            }
        }
        return names;
    }

    public static List<String> getVisibleKpiOuterIds() {
        JSONObject object = getJsonFromUri(Projects.WFM, URL_BASE, makePath(SYSTEM_PROPERTIES, 12));
        return Arrays.asList(object.getString(VALUE).split(","));
    }

    /**
     * Проверает у оргюнита значение мат параметра с айди 201, он же статус FTE группы
     */
    public static boolean checkFteGroupStatus(OrgUnit orgUnit) {
        String path = makePath(ORG_UNITS, orgUnit.getId(), MATH_PARAMETER_VALUES, 201);
        JSONObject mathParameterObject;
        try {
            mathParameterObject = getJsonFromUri(Projects.WFM, URL_BASE, path);
        } catch (AssertionError e) {
            return false;
        }
        return mathParameterObject.optBoolean(VALUE);
    }

    /**
     * Проверяет статус есть ли в оргюните эмплоепозишен сотрудника
     *
     * @param employeeId - сотрудник которого ищем
     * @param orgUnitId  - в каком оргюните ищем
     */
    public static boolean approvePosition(int employeeId, int orgUnitId) {
        String urlEnding = makePath(EMPLOYEES, employeeId, EMPLOYEE_POSITIONS);
        List<NameValuePair> pair = Pairs.newBuilder().from(LocalDate.now()).build();
        JSONObject empObject = getJsonFromUri(Projects.WFM, URL_BASE, urlEnding, pair);
        if (empObject.length() == 0 ||
                empObject.getJSONObject(EMBEDDED).getJSONArray(Params.EMPLOYEE_POSITIONS).length() == 0) {
            return false;
        } else {
            return getListFromJsonObject(empObject, EmployeePosition.class)
                    .stream()
                    .anyMatch(ep -> ep.getOrgUnit().getId() == orgUnitId);
        }
    }

    /**
     * собирает все теги для оргюнитов
     *
     * @return ключи название тега, значение айди тега
     */
    public static Map<String, String> getTags() {
        List<NameValuePair> pairs = Pairs.newBuilder().size(100000).entity("OrganizationUnit").build();
        JSONArray array;
        try {
            array = getJsonFromUri(Projects.WFM, URL_BASE, TAGS, pairs).getJSONObject(EMBEDDED)
                    .getJSONArray(TAGS);
        } catch (org.json.JSONException e) {
            try {
                array = getJsonFromUri(Projects.WFM, URL_BASE, TAGS, pairs).getJSONObject(EMBEDDED)
                        .getJSONArray("tagResourceList");
            } catch (org.json.JSONException ex) {
                throw new AssertionError(NO_TEST_DATA + "На тестовом стенде нет тегов");
            }
        }
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject temp = array.getJSONObject(i);
            map.put(temp.getString(NAME), temp.get(ID).toString());
        }
        return map;
    }

    /**
     * Собирает все виды типов паозиций (продавец, кладовщик и тд)
     *
     * @return - список с именами типа позиции
     */
    public static List<String> getAllPositionTypes() {
        List<String> positionTypesList = new ArrayList<>();
        JSONObject typesObject = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, POSITION_TYPES);
        JSONArray allTypes = typesObject.getJSONObject(EMBEDDED).getJSONArray("positionTypes");
        for (int i = 0; i < allTypes.length(); i++) {
            JSONObject singleType = allTypes.getJSONObject(i);
            String typeName = singleType.getString(NAME);
            positionTypesList.add(typeName);
        }
        return positionTypesList;
    }

    /**
     * Берет все типы позиций и возвращает мапу гед ключи это названия типов, а значения это их айди
     */
    public static Map<String, Integer> getAllPositionTypesForPositionType() {
        JSONObject jsonObject = getJsonFromUri(Projects.WFM, URL_BASE, POSITION_TYPES);
        JSONArray typesArray = jsonObject.getJSONObject(EMBEDDED).getJSONArray(REL_POSITION_TYPES);
        Map<String, Integer> positionTypes = new HashMap<>();
        for (int i = 0; i < typesArray.length(); i++) {
            JSONObject positionType = typesArray.getJSONObject(i);
            String typeName = positionType.getString(NAME);
            int id = positionType.getInt("id");
            positionTypes.put(typeName, id);
        }
        return positionTypes;
    }

    /**
     * Собирает все виды типов оргюнитов (куст, дивизион и тд)
     *
     * @return мэп с айди и именем типа оргюнита
     */
    public static Map<Integer, String> getAllOrgUnitTypes() {
        Map<Integer, String> typesList = new HashMap<>();
        JSONObject typesObject = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, ORGANIZATION_UNIT_TYPES);
        JSONArray allTypes = typesObject.getJSONObject(EMBEDDED).getJSONArray("orgUnitTypes");
        for (int i = 0; i < allTypes.length(); i++) {
            JSONObject singleType = allTypes.getJSONObject(i);
            String typeName = singleType.getString(NAME);
            int typeID = singleType.getInt("id");
            typesList.put(typeID, typeName);
        }
        return typesList;
    }

    /**
     * Из списка всех типов оргюнитов выбирает случайное
     *
     * @return имя и айди типа оргюнита
     */
    public static ImmutablePair<String, Integer> getRandomUnitType() {
        Map<Integer, String> allTypes = getAllOrgUnitTypes();
        List<Integer> idList = new ArrayList<>(allTypes.keySet());
        int id = getRandomFromList(idList);
        return new ImmutablePair<>(allTypes.get(id), id);
    }

    /**
     * Из списка оргюнитов убирает тип который уже есть у оргюнита и убирает подразделение самого высого уровня
     *
     * @param excludeTypeUnit - оргюнит тип которого надо исключить
     * @return - случайный тип подходящий под условия
     */
    public static ImmutablePair<String, Integer> getRandomOrgUnitTypeExceptMostHigherAndAnother(OrgUnit excludeTypeUnit) {
        Map<Integer, String> allOrgUnitTypes = CommonRepository.getAllOrgUnitTypes();
        List<Integer> integers = new ArrayList<>(allOrgUnitTypes.keySet());
        Collections.sort(integers);
        int unitTypeId = integers.get(0);
        allOrgUnitTypes.remove(unitTypeId);
        allOrgUnitTypes.remove(excludeTypeUnit.getOrganizationUnitTypeId());
        List<Integer> idList = new ArrayList<>(allOrgUnitTypes.keySet());
        int id = getRandomFromList(idList);
        return new ImmutablePair<>(allOrgUnitTypes.get(id), id);
    }

    /**
     * Выбирает случайный тип оргюнита, кроме того что есть у выбранного оргюнита
     *
     * @param unit - выбранный оргюнит
     * @return тип оргюнита
     */
    public static ImmutablePair<String, Integer> getTypeIdExceptOne(OrgUnit unit) {
        Map<Integer, String> allTypes = getAllOrgUnitTypes();
        allTypes.remove(unit.getOrganizationUnitTypeId());
        List<String> valuesList = new ArrayList<>(allTypes.values());
        int randomIndex = RANDOM.nextInt(valuesList.size());
        return new ImmutablePair<>(valuesList.get(randomIndex), randomIndex + 1);
    }

    /**
     * возвращает случайный тег для оргюнита
     */
    public static String getOrgUnitTag(OrgUnit unit) {
        String urlEnding = makePath(ORGANIZATION_UNITS, TAGS, unit.getId());
        JSONObject empObject = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, urlEnding);
        String[] tagString = empObject.get(TAGS).toString().trim().split(",");
        return tagString[RANDOM.nextInt(tagString.length)];
    }

    public static int getCurrentRecordIndex(EmployeePosition employeePosition) {
        List<LocalDateTime> timePeriodsWithRecords = BioRecordResourceListRepository
                .getRecordsResourceList(employeePosition, new DateInterval(LocalDate.now()))
                .stream()
                .map(r -> r.getDateTime().withMinute(0).withSecond(0).withNano(0))
                .distinct()
                .collect(Collectors.toList());
        LOG.info("Количество часов с отметками у {} составило {}", employeePosition.getEmployee().getFullName(),
                 timePeriodsWithRecords.size());
        int index = timePeriodsWithRecords.indexOf(LocalDateTime.now().withMinute(0).withNano(0).withSecond(0));
        if (index == -1) {
            throw new AssertionError("Не удалось найти отметку в апи с временем: " + LocalTime.now());
        }
        return index;
    }

    /**
     * @param listOrgUnits Массив желаемых ОМ
     * @param currentUrl   текущий юрл стенда
     * @return Возвращает список списков путей для желаемых ОМ
     * @author Simon, m.druzhinin
     * Метод определяет пути для раскрытия дерева ОМ
     */
    public static List<List<String>> getTreePath(List<Integer> listOrgUnits, String currentUrl) {
        //Список путей для отдельного ОМ
        List<List<String>> routeToOrgUnits = new ArrayList<>();
        for (int i = 0; listOrgUnits.size() > i; i++) {
            List<String> emptyArray = new ArrayList<>();
            routeToOrgUnits.add(emptyArray);
            int orgId = listOrgUnits.get(i);
            OrgUnit orgUnitById = new OrgUnit(getJsonFromUri(Projects.WFM, currentUrl, makePath(ORGANIZATION_UNITS, orgId)));
            routeToOrgUnits.get(i).add(orgUnitById.getName());
            Integer parentId = orgUnitById.getParentId();
            while (parentId != null) {
                OrgUnit tempUnit = new OrgUnit(getJsonFromUri(Projects.WFM, currentUrl, makePath(ORGANIZATION_UNITS, parentId)));
                routeToOrgUnits.get(i).add(tempUnit.getName());
                parentId = tempUnit.getParentId();
            }
        }
        return routeToOrgUnits;
    }

    /**
     * Данный метод возвращает объект класса Map, в котором K - id оргюнита,
     * V - имя рандомно выбранного ОМ.
     *
     * @param uri - запрос для обращения к api
     * @return пара значений, id ОМ и его имя в api
     */
    public static Map<Integer, String> getMapWithRandomIdAndName(URI uri) {
        Map<Integer, String> nameAndId = new HashMap<>();
        JSONObject temp = new JSONObject(setUrlAndInitiateForApi(uri, Projects.WFM));
        JSONArray emp = temp.getJSONObject(EMBEDDED).getJSONArray(REL_ORGANIZATION_UNITS);
        int empNum = RANDOM.nextInt(emp.length());
        int id = emp.getJSONObject(empNum).getInt(ID);
        String name = emp.getJSONObject(empNum).getString(NAME);
        nameAndId.put(id, name);
        return nameAndId;
    }

    /**
     * берет случайный таг из апи
     *
     * @return слева название тега, справа его айди
     */
    public static ImmutablePair<String, String> getRandomTagFromApi() {
        Map<String, String> tags = getTags();
        String tag = getRandomFromList(new ArrayList<>(tags.keySet()));
        return new ImmutablePair<>(tag, tags.get(tag));
    }

    /**
     * берет случайный таг из апи
     *
     * @param exceptTags - список тэгов, которые надо исключить из выборки
     * @return слева название тега, справа его айди
     */
    public static String getRandomTagFromApi(List<String> exceptTags) {
        Map<String, String> tags = getTags();
        return getRandomFromList(new ArrayList<>(tags.keySet()).stream()
                                         .filter(t -> !exceptTags.contains(t)).collect(Collectors.toList()));
    }

    /**
     * Берет массив юзеров
     */
    public static JSONArray getUsers() {
        List<NameValuePair> pairs = Pairs.newBuilder().page(0).size(10000).build();
        JSONObject user = getJsonFromUri(Projects.WFM, URL_BASE, USERS, pairs);
        return user.getJSONObject(EMBEDDED).getJSONArray(USERS);
    }

    /**
     * Определение типа ДБ по главному ОМ
     */
    public static OmDbType getTypeDb(String url) {
        JSONObject jsonObject = getJsonFromUri(Projects.WFM, url, MAIN_ORGANIZATION_UNIT);
        JSONArray jsonArray = jsonObject.getJSONObject(EMBEDDED).getJSONArray(REL_ORGANIZATION_UNITS);
        List<OrgUnit> mainUnits = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            mainUnits.add(new OrgUnit(jsonArray.getJSONObject(i)));
        }
        if (mainUnits.size() != 1) {
            throw new AssertionError("Несколько главных ОМ");
        }
        OrgUnit main = mainUnits.get(0);
        Optional<OmDbType> optionalType = Arrays.stream(OmDbType.values())
                .filter(omDbType -> main.getName().equalsIgnoreCase(omDbType.getMainOmName()))
                .findAny();
        return optionalType.orElse(OmDbType.INVENTIV);
    }

    public static EmployeeStatusType getEmployeeStatusTypeByName(String name) {
        URI uri = setUri(Projects.WFM, URL_BASE, EMPLOYEES_STATUS_TYPES);
        JSONObject json = getJsonFromUri(Projects.WFM, uri);
        return getListFromJsonObject(json, EmployeeStatusType.class)
                .stream()
                .filter(e -> e.getTitle().equals(name))
                .findAny()
                .orElseThrow(() -> new AssertionError(NO_TEST_DATA + "На данном стенде нет статуса \"" + name + "\""));
    }

    public static List<String> getConstraintViolations(int omId, EmployeePosition ep, ShiftTimePosition shiftTimePosition) {
        String excludeString = ConstraintViolations.EXCEEDING_HOURS_PER_WEEK + "," + ConstraintViolations.SHORTAGE_HOURS_PER_WEEK;
        String urlEnding = makePath(CONFLICT, ROSTER, RosterRepository.getNeededRosterId(shiftTimePosition, shiftTimePosition.getShiftsDateInterval(), omId).getId());
        List<NameValuePair> pairs = Pairs.newBuilder()
                .exclude(excludeString)
                .excludeIgnored(false)
                .from(LocalDateTools.getFirstDate())
                .to(LocalDateTools.getLastDate()).build();
        JSONArray constraintViolationList = getJsonArrayFromUri(Projects.WFM, setUri(API, CommonRepository.URL_BASE, urlEnding, pairs), ImmutablePair.of("Wfm-Internal", getToken()));
        List<String> textConstrViolations = new ArrayList<>();
        for (int i = 0; i < constraintViolationList.length(); i++) {
            JSONObject temp = constraintViolationList.getJSONObject(i);
            if (!temp.isNull(EMPLOYEE_ID)) {
                if (temp.getInt(EMPLOYEE_ID) == ep.getEmployee().getId()) {
                    textConstrViolations.add(temp.getString(TEXT));
                }
            }
        }
        return textConstrViolations;
    }

    public static int getPositionCategoryRosterId(int omId, ShiftTimePosition timePosition) {
        String urlEnding = makePath(ROSTERS, RosterRepository.getNeededRosterId(timePosition, new DateInterval(), omId).getId(), Links.POSITION_CATEGORY_ROSTERS);
        URI uri = setUri(Projects.WFM, URL_BASE, urlEnding);
        JSONObject posCatRoster = getJsonArrayFromJsonObject(getJsonFromUri(Projects.WFM, uri)).getJSONObject(0);
        String link = posCatRoster.getJSONObject(LINKS).getJSONObject(SELF).getString(HREF);
        return Integer.parseInt(link.substring(link.lastIndexOf("/") + 1));

    }

    /**
     * Возвращает список возможных бизнес направлений для орг юнита
     */
    public static List<String> getAvailableBusinessDirections(int omId) {
        String urlEnding = makePath(ORG_UNITS, omId, "available-business-directions");
        URI uri = setUri(Projects.WFM, URL_BASE, urlEnding);
        JSONArray jsonArray = getJsonArrayFromUri(Projects.WFM, uri);
        List<String> businessDirections = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            businessDirections.add(jsonArray.getString(i));
        }
        return businessDirections;
    }

    public static List<String> getOrgTypes() {
        String urlEnding = makePath("entity-properties-value", 3, VALUES);
        List<NameValuePair> pair = Pairs.newBuilder().size(1000).build();
        URI uri = setUri(Projects.WFM, URL_BASE, urlEnding, pair);
        JSONArray jsonArray = getJsonFromUri(Projects.WFM, uri).getJSONObject(EMBEDDED).getJSONArray("attributeValues");
        List<String> result = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            result.add(jsonArray.getJSONObject(i).getString(VALUE));
        }
        return result;
    }

    /**
     * Возвращает список всех типов телефонов
     */
    public static List<PhoneType> getPhoneTypes() {
        JSONArray phoneTypes = getJsonFromUri(Projects.WFM, setUri(Projects.WFM, CommonRepository.URL_BASE, PHONE_TYPES))
                .getJSONObject(EMBEDDED).getJSONArray(REL_PHONE_TYPES);
        return getListFromJsonArray(phoneTypes, PhoneType.class);
    }

    /**
     * Возвращает список файлов из раздела "Справка"
     */
    public static List<FileManual> getFileManuals() {
        JSONArray fileManuals = getJsonFromUri(Projects.WFM, setUri(Projects.WFM, CommonRepository.URL_BASE, "file-manual"))
                .getJSONObject(EMBEDDED).getJSONArray("manualFiles");
        return getListFromJsonArray(fileManuals, FileManual.class);
    }

    /**
     * Получить название действия под локалью в translation.json по json ключам.
     *
     * @param function - jsonPath (адрес нужного значения)
     */
    public static String getLocalizedName(String function) {
        String locale = getSystemProperty(APP_DEFAULT_LOCALE).getValue().toString();
        String name = new ApiRequest.GetBuilder(makePath(URL_BASE, LOCALES, locale, TRANSLATION_JSON)).send().returnJsonValue(function);
        if (Objects.isNull(name)) {
            locale = locale.equals(AppDefaultLocale.RU.getLocale()) ? AppDefaultLocale.RU_RUSSIANPOST.getLocale() : AppDefaultLocale.RU.getLocale();
            name = new ApiRequest.GetBuilder(makePath(URL_BASE, LOCALES, locale, TRANSLATION_JSON)).send().returnJsonValue(function);
        }
        return name;
    }

    public static String getToken() {
        String urlEnding = makePath("auth", "jwt", "issue");
        List<NameValuePair> pairs = Pairs.newBuilder().expired_millis(75000).build();
        String token = getJsonFromUri(Projects.WFM, URL_BASE, urlEnding, pairs).getString("token");
        LOG.info("Получен токен: {}", token);
        return token;
    }

    /**
     * Извлечь JSON из response
     */
    public static JSONObject extractJSONObjectFromResponse(HttpResponse response) {
        try {
            String responseContent = EntityUtils.toString(response.getEntity());
            if (responseContent.startsWith("[")) {
                responseContent = responseContent.substring(1, responseContent.length() - 1);
            }
            return new JSONObject(responseContent);
        } catch (IOException | JSONException e) {
            throw new RuntimeException("Не удалось преобразовать ответ в JSONObject: " + e.getMessage());
        }
    }
}

package dbtest;

import apitest.HelperMethods;
import common.DataProviders;
import io.qameta.allure.*;
import io.qameta.allure.testng.Tag;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ContentType;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;
import reporting.TestListener;
import testutils.BaseTest;
import utils.Links;
import utils.Params;
import utils.Projects;
import utils.db.DBUtils;
import utils.tools.Format;
import utils.tools.LocalDateTools;
import utils.tools.Pairs;
import wfm.ApiRequest;
import wfm.PresetClass;
import wfm.components.orgstructure.MathParameterEntities;
import wfm.components.orgstructure.OrganizationUnitTypeId;
import wfm.components.schedule.ScheduleRequestStatus;
import wfm.components.schedule.ScheduleRequestType;
import wfm.components.schedule.ShiftTimePosition;
import wfm.components.utils.PermissionType;
import wfm.models.*;
import wfm.repository.*;

import java.net.URI;
import java.sql.Date;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static common.Groups.*;
import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;
import static utils.Links.*;
import static utils.Params.*;
import static utils.db.DBUtils.*;
import static utils.integration.IntegrationUtils.timestampConverter;
import static utils.tools.RequestFormers.*;
import static utils.tools.RequestFormers.assertStatusCode;

@Listeners({TestListener.class})
public class DataBaseTest extends BaseTest {
    final String JSON_LOGGER = "Отправлен JSON {}";
    final String EMPLOYEE_ID = "employee_id";
    final String POSITION_ID = "position_id";
    final String ALIAS_ID = "alias_id";
    final String CREATE_MODE = "create_mode";
    final String START_DATE_TIME = "startdatetime";
    final String END_DATE_TIME = "enddatetime";

    /**
     * Создает меп для отправки запроса, полученного из интеграции
     *
     * @param ep        сотрудник, для которого создаем запрос
     * @param date      дата запроса
     * @param startTime время начала
     * @param endTime   время окончания
     * @param alias     тип запроса
     */
    private Map<String, Object> prepareBodyForCreatingScheduleRequestViaIntegration(EmployeePosition ep, LocalDate date, LocalTime startTime, LocalTime endTime, ScheduleRequestAlias alias) {
        String dateString = date.toString();
        Map<String, Object> body = new HashMap<>();
        body.put(EMPLOYEE_OUTER_ID, ep.getEmployee().getOuterId());
        body.put(POSITION_OUTER_ID, ep.getPosition().getOuterId());
        body.put(TYPE, alias.getOuterId());
        body.put(START_DATE, dateString);
        body.put(START_TIME, startTime.format(Format.TIME.getFormat()));
        body.put(END_DATE, dateString);
        body.put(END_TIME, endTime.format(Format.TIME.getFormat()));
        return body;
    }

    /**
     * Собирает JSON для удаления назначения
     *
     * @param ep объект назначения
     */
    private Map<String, Object> makeObjectForDeletingEmployeePositionViaApi(EmployeePosition ep) {
        Map<String, Object> map = new HashMap<>();
        map.put(EMPLOYEE_OUTER_ID, ep.getEmployee().getOuterId());
        map.put(POSITION_OUTER_ID, ep.getPosition().getOuterId());
        map.put(TYPE, MathParameterEntities.EMPLOYEE_POSITION);
        map.put(START_DATE, ep.getDateInterval().getStartDate());
        map.put(END_DATE, ep.getDateInterval().getEndDate());
        return map;
    }

    @Step("Проверить, что в БД появилась запись о запросе типа \"{alias.title}\" с {start} по {end} для сотрудника {ep} и признаком \"{createMode}\"")
    private void assertRequestAppearedInDB(EmployeePosition ep, LocalDateTime start, LocalDateTime end, ScheduleRequestAlias alias, String createMode) {
        Map<String, Object> result = getLatestRequest();
        Allure.addAttachment("Последняя запись в таблице schedule_request", result.toString().replace(",", ",\n"));
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(timestampConverter(result.get(START_DATE_TIME)), start, "Время начала не совпало:");
        softAssert.assertEquals(timestampConverter(result.get(END_DATE_TIME)), end, "Время окончания не совпало:");
        softAssert.assertEquals(result.get(EMPLOYEE_ID), (long) ep.getEmployee().getId(), "ID сотрудника не совпал:");
        softAssert.assertTrue(result.get(POSITION_ID) == null || result.get(POSITION_ID).equals((long) ep.getPosition().getId()), "ID позиции не совпал:");
        softAssert.assertEquals(result.get(ALIAS_ID), (long) alias.getAlias(), "ID типа запроса не совпал:");
        softAssert.assertEquals(result.get(CREATE_MODE), createMode, "Признак создания не совпал:");
        softAssert.assertAll();
    }

    @Step("Проверить ответ сервера и удаление записи из БД")
    private void assertRecordRemoved(List<Map<String, Object>> before, ZonedDateTime now, ApiRequest request, EmployeePosition ep) {
        List<Map<String, Object>> after = DBUtils.getEmployeePositionByOuterId(ep.getEmployee().getOuterId());
        Assert.assertEquals(before.size() - 1, after.size(), "Размер списка назначений не уменьшился на 1");
        SoftAssert softAssert = new SoftAssert();
        String unExpectedValue = "Значение поля \"%s\" не соответствует ожидаемому:";
        String success = "success";
        String callType = "callType";
        String events = "events";
        softAssert.assertTrue(request.returnJsonValue(success), String.format(unExpectedValue, success));
        softAssert.assertEquals(request.returnJsonValue(events).toString(), "[]", String.format(unExpectedValue, events));
        softAssert.assertEquals(request.returnJsonValue(callType).toString(), "REMOVED_OBJECTS", String.format(unExpectedValue, callType));
        ZonedDateTime responseTime = ZonedDateTime.of(LocalDateTime.parse(request.returnJsonValue("dateTime")), ZoneId.of("UTC"));
        softAssert.assertTrue(now.until(responseTime, ChronoUnit.SECONDS) < 60, "Время удаления не совпало:");
        after.removeAll(before);
        String notDeletedEntry = "В БД осталась запись с %s=%s, которая должна была быть удалена";
        if (!after.isEmpty()) {
            for (Map<String, Object> entry : after) {
                String outerId = ep.getPosition().getOuterId();
                softAssert.assertNotEquals(entry.get("p_" + OUTER_ID.toLowerCase()), outerId,
                                           String.format(notDeletedEntry, POSITION_OUTER_ID, outerId));
                LocalDate startDate = ep.getDateInterval().getStartDate();
                softAssert.assertNotEquals(entry.get(START_DATE.toLowerCase()), startDate,
                                           String.format(notDeletedEntry, START_DATE, startDate));
            }
        }
        softAssert.assertAll();
    }

    @Step("Проверить, что в БД записано время начала расчета и ID инициатора расчета")
    public void assertCalculationUserIsRecorded(int id, ZonedDateTime start, User user) {
        Map<String, Object> result = getCalcJobById(id);
        SoftAssert softAssert = new SoftAssert();
        ZonedDateTime convertedDbTime = ZonedDateTime.of(timestampConverter(result.get("create_time")), ZoneId.of("UTC"));
        softAssert.assertTrue(ChronoUnit.SECONDS.between(convertedDbTime, start) < 10, "Время начала не совпало:");
        long userId = user == null ? DBUtils.getSuperuserId() : user.getId();
        softAssert.assertEquals(result.get("creator_id"), userId, "ID инициатора расчета не совпал:");
        softAssert.assertAll();
        Allure.addAttachment("Проверка", "ID инициатора расчета и время создания расчета проверены и соответствуют действительности");
    }

    private void assertScheduleRequest(ApiRequest request, LocalDate date, String employeeOuterId, String positionOuterId, boolean isCorrect) {
        StringBuilder createDirectAbsenceBuilder = new StringBuilder();
        createDirectAbsenceBuilder.append("Запись импорта integrationcallresult c success = true");
        StringBuilder createAssignmentAbsenceBuilder = new StringBuilder();
        createAssignmentAbsenceBuilder.append("Проверить, что на стороне ВФМ создана запись лога импорта integrationcallresult c success = false. ");
        createAssignmentAbsenceBuilder.append("Запись ошибки импорта integrationevent (которая ссылается на лог импорта по integrationcallresult_id).");
        changeStepNameDependingOnParameter(isCorrect, createDirectAbsenceBuilder.toString(), createAssignmentAbsenceBuilder.toString());

        String valueCallTable = isCorrect ? request.returnJsonValue("dateTime") : request.returnJsonValue("events[0].errorMessage");
        SoftAssert softAssert = new SoftAssert();
        String unExpectedValue = "Значение поля \"%s\" не соответствует ожидаемому:";
        softAssert.assertEquals(request.returnJsonValue(CALL_TYPE).toString(), "SCHEDULE_REQUESTS_IMPORT", String.format(unExpectedValue, CALL_TYPE));
        if (isCorrect) {
            checkScheduleRequestForEmployeeAndPositionInAbsenceInterval(employeeOuterId, positionOuterId, date);
            softAssert.assertTrue(request.returnJsonValue(SUCCESS), String.format(unExpectedValue, SUCCESS));
            softAssert.assertEquals(request.returnJsonValue(EVENTS).toString(), "[]", String.format(unExpectedValue, EVENTS));
            checkIfIntegrationCallTableHasRecord(null, valueCallTable, "SCHEDULE_REQUESTS_IMPORT", isCorrect);
        } else {
            String dateTime = request.returnJsonValue("dateTime").toString().replace('T', ' ');
            String sql = String.format("select id from integrationcallresult WHERE datetime::text like '%s", dateTime) + ".%'";
            int integrationCallId = getIntegrationCallResultId(sql);
            softAssert.assertFalse(request.returnJsonValue(SUCCESS), String.format(unExpectedValue, SUCCESS));
            checkIfIntegrationEventTableHasFailedRecord(integrationCallId, valueCallTable);
            checkIfIntegrationCallTableHasRecord(integrationCallId, null, "SCHEDULE_REQUESTS_IMPORT", false);
        }
        softAssert.assertAll();
    }

    @Step("Проверить, что создан запрос отсутствия schedule_request")
    public void checkScheduleRequestForEmployeeAndPositionInAbsenceInterval(String employeeOuterId, String positionOuterId, LocalDate date) {
        List<Map<String, Object>> scheduleRequest = DBUtils.getScheduleRequestByOuterId(employeeOuterId, positionOuterId, date);
        Assert.assertFalse(scheduleRequest.isEmpty(), String.format(
                "В базе данных не появилась запись в таблице schedule_request со значениями employeeOuterId: %s и positionOuterId: %s",
                employeeOuterId, positionOuterId));
    }

    @Step("Проверить, что в таблице integrationevent есть запись о неудавшемся создании сущности")
    public int checkIfIntegrationEventTableHasFailedRecord(String message) {
        Integer integrationCallResultId = getIntegrationCallResultIdByMessage(message);
        Assert.assertNotNull(integrationCallResultId, "Запись в таблице integrationevent не обнаружена");
        return integrationCallResultId;
    }

    @Step("Проверить, что в таблице integrationevent есть запись о неудавшемся создании сущности")
    public int checkIfIntegrationEventTableHasFailedRecord(Integer integrationCallId, String message) {
        Integer integrationeventId = getIntegrationeventIdByCallResultIdAndMessage(integrationCallId, message);
        Assert.assertNotNull(integrationeventId, "Запись в таблице integrationevent не обнаружена");
        return integrationeventId;
    }

    @Step("Проверить, что в таблице integrationcallresult есть запись с типом {callType} и результатом success = {isCorrect}")
    public void checkIfIntegrationCallTableHasRecord(Integer integrationCallResultId, String dateTime, String callType, boolean isCorrect) {
        String sql = "SELECT success FROM integrationcallresult ";
        if (Objects.nonNull(integrationCallResultId)) {
            sql += "WHERE id = " + integrationCallResultId;
        } else if (Objects.nonNull(dateTime)) {
            dateTime = dateTime.replace('T', ' ');
            sql += String.format("WHERE datetime::text LIKE '%s'", dateTime + ".%");
        }

        boolean actualResult;
        try {
            actualResult = getIntegrationCallResultSuccess(sql);
        } catch (NullPointerException ex) {
            LOG.error(ex.getMessage()); //миллисекунды из response и из бд иногда расходятся
            String adjustedSql = adjustSqlForMillis(sql, dateTime);
            actualResult = getIntegrationCallResultSuccess(adjustedSql);
        }
        Assert.assertEquals(actualResult, isCorrect, "Результат получения записи из таблицы integrationcallresult не соответствует ожидаемому");
    }

    private String adjustSqlForMillis(String sql, String dateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime parsedDateTime = LocalDateTime.parse(dateTime, formatter).minusSeconds(1);
        String adjustedTime = parsedDateTime.toString().replace('T', ' ');
        return sql.replace(dateTime, adjustedTime);
    }

    @Step("Проверить, что в таблице {table} была создана запись с {variable} = \"{value}\"")
    public void checkIfTableHasRecordByValue(String table, String variable, String value, boolean isCorrect) {
        changeStepNameDependingOnParameter(isCorrect, String.format("Проверить, что в таблице %s была создана запись с %s = \"%s\"", table, variable, value),
                                           String.format("Проверить, что в таблице %s нет записи с %s = \"%s\"", table, variable, value));
        String sql = String.format("SELECT * FROM %s WHERE %s = '%s';", table, variable, value);
        boolean isEntityCreated = Objects.nonNull(checkIfEntityExistsInDB(sql));
        String assertMessage = isCorrect ? String.format("Запись в таблице %s не был создана", table) : String.format("Запись в таблице %s была создана, несмотря на некорректные входные данные", table);
        Assert.assertEquals(isEntityCreated, isCorrect, assertMessage);
    }

    @Step("Отправить запрос на создание подразделения")
    public ImmutablePair<String, String> sendPostRequestToCreateOrgUnit(boolean isCorrect) {
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(INTEGRATION_JSON, ORG_UNITS));
        String randomName = "test_OrgUnit_" + RandomStringUtils.randomAlphanumeric(10);
        JSONObject miniObject = new JSONObject();
        miniObject.put(OUTER_ID, randomName);
        miniObject.put(ACTIVE, true);
        miniObject.put(AVAILABLE_FOR_CALCULATION, JSONObject.NULL);
        miniObject.put(DATE_FROM, LocalDate.now());
        miniObject.put(NAME, randomName);
        OrganizationUnitTypeId lowestOrgUnitTypeId = OrganizationUnitTypeId.getLowestOrgUnitTypeWithOrgUnits();
        miniObject.put(ORG_UNIT_TYPE_OUTER_ID, lowestOrgUnitTypeId.getOuterId());
        if (isCorrect) {
            OrganizationUnitTypeId parentOrgUnitTypeId = OrganizationUnitTypeId.getRandomParentOrgUnitType(lowestOrgUnitTypeId, true);
            OrgUnit unit = OrgUnitRepository.getRandomOrgUnitByTypeId(parentOrgUnitTypeId);
            miniObject.put(PARENT_OUTER_ID, unit.getOuterId());
        } else {
            miniObject.put(PARENT_OUTER_ID, "test_OrgUnit_error_" + RandomStringUtils.randomAlphabetic(10));
        }
        miniObject.put(ZONE_ID, JSONObject.NULL);
        miniObject.put(PROPERTIES, JSONObject.NULL);
        String miniObjectAsStr = '[' + miniObject.toString() + ']';
        LOG.info(JSON_LOGGER, miniObject);
        HttpResponse response = requestMaker(uri, miniObjectAsStr, RequestBuilder.post(), ContentType.APPLICATION_JSON, Projects.WFM);
        int correctStatusCode = isCorrect ? 200 : 400;
        assertStatusCode(response, correctStatusCode, uri.toString());
        JSONObject responseBody = CommonRepository.extractJSONObjectFromResponse(response);
        Allure.addAttachment("Результат выполнения запроса", "POST " + response.getStatusLine().getStatusCode() + " " + uri);
        Allure.addAttachment("Тело запроса", miniObjectAsStr);
        Allure.addAttachment("Тело ответа", responseBody.toString());
        if (correctStatusCode == 200) {
            String dateTime = responseBody.getString("dateTime");
            return new ImmutablePair<>(randomName, dateTime);
        } else {
            String errorMessage = responseBody.getString("message");
            return new ImmutablePair<>(randomName, errorMessage);
        }
    }

    public String sendPostRequestToCreateEmployeePosition(JSONObject requestBody, boolean orgUnitExists) {
        String miniObjectAsStr = '[' + requestBody.toString() + ']';
        LOG.info(JSON_LOGGER, requestBody);
        List<NameValuePair> pairs = Pairs.newIntegrationBuilder().concreteDates(true).processShifts("delete").build();
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(INTEGRATION_JSON, "employee-positions-full"), pairs);
        HttpResponse response = requestMaker(uri, miniObjectAsStr, RequestBuilder.post(), ContentType.APPLICATION_JSON, Projects.WFM);
        assertStatusCode(response, 200, uri.toString());
        JSONObject responseBody = CommonRepository.extractJSONObjectFromResponse(response);
        Allure.addAttachment("Результат выполнения запроса", "POST " + response.getStatusLine().getStatusCode() + " " + uri);
        Allure.addAttachment("Тело запроса", miniObjectAsStr);
        Allure.addAttachment("Тело ответа", responseBody.toString());
        boolean success = responseBody.getBoolean("success");
        Assert.assertEquals(success, orgUnitExists, "Результат отправки запроса не соответствует ожидаемому");
        if (orgUnitExists) {
            return responseBody.getString("dateTime");
        } else {
            return responseBody.getJSONArray("events").getJSONObject(0).getString("errorMessage");
        }
    }

    @Step("Отправить запрос на создание назначения")
    private String createOrCloseEmployeePosition(JSONObject requestBody, boolean orgUnitExists, boolean toClose) {
        changeStepNameIfTrue(toClose, "Отправить запрос на закрытие назначения");
        return sendPostRequestToCreateEmployeePosition(requestBody, orgUnitExists);
    }

    public JSONObject prepareBodyForCreatingEmployeePosition(boolean orgUnitExists) {
        JSONObject miniObject = new JSONObject();
        LocalDate date = LocalDate.now().minusDays(1);
        miniObject.put(START_WORK_DATE, date);
        miniObject.put(END_WORK_DATE, JSONObject.NULL);
        miniObject.put(NUMBER, "test_cardNumber_" + RandomStringUtils.randomAlphanumeric(10));
        miniObject.put(RATE, 1.00);

        JSONObject employee = new JSONObject();
        employee.put(OUTER_ID, "test_employeeOuterId_" + RandomStringUtils.randomAlphanumeric(10));
        employee.put(FIRST_NAME, RandomStringUtils.randomAlphabetic(10));
        employee.put(LAST_NAME, RandomStringUtils.randomAlphabetic(10));
        employee.put(START_DATE, date);
        employee.put(END_WORK_DATE, JSONObject.NULL);
        employee.put(PROPERTIES, new JSONObject().put("КисКод", "CN-" + randomNumeric(7)));

        JSONObject position = new JSONObject();
        position.put(NAME, JobTitleRepository.randomJobTitle().getFullName());
        position.put(OUTER_ID, "test_positionOuterId_" + RandomStringUtils.randomAlphanumeric(10));
        position.put(REL_ORGANIZATION_UNIT_CHIEF, JSONObject.NULL);
        String orgUnitOuterId = orgUnitExists ? OrgUnitRepository.getRandomOrgUnit().getOuterId() : "test_OrgUnit_error_" + RandomStringUtils.randomAlphanumeric(10);
        position.put("organizationUnit", new JSONObject().put(OUTER_ID, orgUnitOuterId));
        position.put(Params.POSITION_TYPE, new JSONObject().put(OUTER_ID, PositionTypeRepository.randomPositionType().getOuterId()));
        position.put(Params.POSITION_GROUP, new JSONObject().put(NAME, PositionGroupRepository.randomPositionGroup().getName()));
        position.put(Params.POSITION_CATEGORY, new JSONObject().put(OUTER_ID, PositionCategoryRepository.randomPositionCategory().getOuterId()));
        position.put(START_WORK_DATE, date);
        position.put(END_WORK_DATE, JSONObject.NULL);
        position.put(PROPERTIES, JSONObject.NULL);
        miniObject.put(EMPLOYEE, employee);
        miniObject.put(POSITION, position);
        return miniObject;
    }

    public JSONObject createEmployeePositionToClose() {
        JSONObject requestBody = prepareBodyForCreatingEmployeePosition(true);
        sendPostRequestToCreateEmployeePosition(requestBody, true);
        String employeeName = requestBody.getJSONObject(EMPLOYEE).getString(FIRST_NAME) + " " + requestBody.getJSONObject(EMPLOYEE).getString(LAST_NAME);
        String positionName = requestBody.getJSONObject(POSITION).getString(NAME);
        Allure.addAttachment("Создание назначения для закрытия", String.format("Было создано назначение с сотрудником %s и должностью %s", employeeName, positionName));
        return requestBody;
    }

    @Step("Создание запроса для бизнес-драйвера")
    private String createRequestBusinessDriver(Boolean isCorrect, String value, String kpiOuterId, String organizationUnitOuterId) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(DATE, LocalDate.now());
        jsonObject.put(TIME, "00:00");
        jsonObject.put(VALUE, value);
        jsonObject.put(KPI, kpiOuterId);
        jsonObject.put(ORG_UNIT_OUTER_ID, organizationUnitOuterId);

        List<NameValuePair> pairs = Pairs.newIntegrationBuilder().forecast(true).build();
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(INTEGRATION_JSON, KPI), pairs);

        JSONArray jsonArray = new JSONArray().put(jsonObject);
        LOG.info(JSON_LOGGER, jsonArray);
        HttpResponse response = requestMaker(uri, jsonArray, RequestBuilder.post(), ContentType.APPLICATION_JSON, Projects.WFM);

        Allure.addAttachment("Результат выполнения запроса", "POST " + response.getStatusLine().getStatusCode() + " " + uri);
        Allure.addAttachment("Тело запроса", jsonArray.toString());
        Allure.addAttachment("Тело ответа", response.toString());

        int expectedStatusCode = (isCorrect == null) ? 500 : 200;
        assertStatusCode(response, expectedStatusCode, uri.toString());

        if (expectedStatusCode == 200) {
            Header dateHeader = response.getFirstHeader(HttpHeaders.DATE);
            ZonedDateTime zonedDateTime = ZonedDateTime.parse(dateHeader.getValue(), DateTimeFormatter.RFC_1123_DATE_TIME);
            LocalDateTime responseTime = zonedDateTime.toLocalDateTime();
            return responseTime.toString();
        } else {
            return CommonRepository.extractJSONObjectFromResponse(response).getString("message");
        }
    }

    @Step("Проверить, что назначение с card_number = \"{cardNumber}\" закрыто {closeDate}")
    public void checkIfEmployeePositionWasClosed(String cardNumber, LocalDate closeDate) {
        String sql = String.format("SELECT * FROM %s WHERE %s = '%s';", "employeeposition", "card_number", cardNumber);
        Map<String, Object> result = checkIfEntityExistsInDB(sql);
        Assert.assertNotNull(result, String.format("Назначение с card_number = %s не найдено в БД", cardNumber));
        Date actualCloseDate = (Date) result.get("enddate");
        Assert.assertNotNull(actualCloseDate, "У найденного назначения не проставлена дата закрытия");
        LocalDate actualCloseDateAsLocalDate = actualCloseDate.toLocalDate();
        Assert.assertEquals(actualCloseDateAsLocalDate, closeDate, "Фактическая дата закрытия не совпадает с ожидаемой");
    }

    @Step("Проверить, что в таблице kpiBaseValue была создана запись с value: {value} и outerId: {organizationUnitOuterId}")
    private void checkKpiBaseValue(String value, String organizationUnitOuterId, String kpiOuterId) {
        String sql = String.format("SELECT kb.value FROM kpibasevalue kb\n" +
                "JOIN organizationunit o ON o.id = kb.organizationunit_id\n" +
                "JOIN kpi k ON k.id = kb.kpi_id\n" +
                "WHERE o.outerid = '%s'\n" +
                "AND k.outerid = '%s'\n" +
                "ORDER BY kb.id DESC\n" +
                "LIMIT 1;", organizationUnitOuterId, kpiOuterId);
        String sqlValue = DBUtils.getValueByKpiBaseValue(sql);
        Assert.assertEquals(Double.valueOf(sqlValue), Double.valueOf(value), "Значение в таблице не соответствует ожидаемому.");
    }

    @Test(groups = {"ABCHR5667", G2, SCHED9, POCHTA},
            description = "Для запросов, приходящих по интеграции, в БД проставляется признак IN")
    @Link(name = "Статья: \"5667_Признак создания запроса по интеграции\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=234109632")
    @TmsLink("#60238")
    @Owner(SCHASTLIVAYA)
    @Tag("ABCHR5667-1")
    @Tag(SCHED9)
    private void intRequestsHaveIN() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(unit.getId(), null, true);
        ScheduleRequestAlias alias = ScheduleRequestAliasRepository.getRandomEnabledAlias();
        LocalDate date = ShiftTimePosition.DEFAULT.getShiftsDateInterval().getRandomDateBetween();
        PresetClass.makeClearDate(ep, date);
        LocalTime startTime = LocalTime.of(0, 0);
        LocalTime endTime = LocalTime.of(23, 59);
        Map<String, Object> body = prepareBodyForCreatingScheduleRequestViaIntegration(ep, date, startTime, endTime, alias);
        new ApiRequest.PostBuilder(makePath(INTEGRATION_JSON, SCHEDULE_REQUESTS))
                .withBody(Collections.singletonList(body))
                .withStatus(200)
                .send();
        assertRequestAppearedInDB(ep, date.atTime(startTime), date.atTime(endTime), alias, "IN");
    }

    @Test(groups = {"ABCHR5667", G2, SCHED9},
            description = "Для запросов, созданных в wfm вручную, в БД проставляется признак MN")
    @Link(name = "Статья: \"5667_Признак создания запроса по интеграции\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=234109632")
    @TmsLink("#60238")
    @Owner(SCHASTLIVAYA)
    @Tag("ABCHR5667-2")
    @Tag(SCHED9)
    private void wfmRequestsHaveMN() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(unit.getId(), null, true);
        ScheduleRequestAlias alias = ScheduleRequestAliasRepository.getAlias(ScheduleRequestType.NON_APPEARANCE);
        LocalDate date = ShiftTimePosition.DEFAULT.getShiftsDateInterval().getRandomDateBetween();
        ScheduleRequest request = Allure.step("Создать запрос", () -> {
            PresetClass.makeClearDate(ep, date);
            return HelperMethods.createScheduleRequest(ScheduleRequestStatus.APPROVED, ep, alias, date);
        });
        assertRequestAppearedInDB(ep, request.getDateTimeInterval().getStartDateTime(),
                                  request.getDateTimeInterval().getEndDateTime(),
                                  alias, "MN");
    }

    @Test(groups = {"ITR", G1, INTEGRATION},
            description = "Удаленные записи")
    @Link(name = "Статья: \"Интеграционные тесты для регресса\"", url = "https://wiki.goodt.me/x/S4DgDQ")
    @TmsLink("TEST-1596")
    @Owner(SCHASTLIVAYA)
    @Tag("ITR-1")
    @Tag(INTEGRATION)
    private void removeRecord() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(unit.getId(), null, true);
        PresetClass.defaultShiftPreset(ep, ShiftTimePosition.FUTURE);
        List<Map<String, Object>> before = DBUtils.getEmployeePositionByOuterId(ep.getEmployee().getOuterId());
        Map<String, Object> map = makeObjectForDeletingEmployeePositionViaApi(ep);
        ApiRequest request = new ApiRequest.PostBuilder(makePath(INTEGRATION_JSON, REMOVED))
                .withParams(Collections.singletonMap("stop-on-error", "false"))
                .withBody(Collections.singletonList(map))
                .withStatus(200)
                .send();
        assertRecordRemoved(before, ZonedDateTime.now(), request, ep);
    }

    @Test(groups = {"ABCHR5755", G2, SCHED11},
            description = "В БД фиксируется id суперюзера, когда он запускает расчет",
            dataProvider = "true/false", dataProviderClass = DataProviders.class)
    @Link(name = "Статья: \"5755_Инициатор запуска расчета\"", url = "https://wiki.goodt.me/x/Pzr0DQ")
    @TmsLink("#60214")
    @Owner(SCHASTLIVAYA)
    @Severity(SeverityLevel.MINOR)
    @Tag(SCHED11)
    private void recordUserIdWhenCalculationIsStarted(boolean isSuperuser) {
        changeTestIDDependingOnParameter(isSuperuser, "ABCHR5755-2", "ABCHR5755-1",
                                         "В БД фиксируется id пользователя, который запустил расчет");
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        User user;
        if (isSuperuser) {
            user = null;
        } else {
            user = getUserWithPermissions(Arrays.asList(PermissionType.SCHEDULE_VIEW,
                                                        PermissionType.SCHEDULE_EDIT,
                                                        PermissionType.SCHEDULE_CALCULATION_SHIFTS),
                                          unit);
        }
        Map<String, String> params = PresetClass.getShiftCalculationParamBuilder(getServerDate(),
                                                                                 LocalDateTools.getLastDate(),
                                                                                 false,
                                                                                 false)
                .buildMap();
        ApiRequest request = Allure.step("Отправить запрос на расчет смен", () ->
                new ApiRequest.PostBuilder(makePath(Links.CALC_JOBS, unit.getId(), ROSTERING))
                        .withParams(params)
                        .withUser(user)
                        .send());

        ZonedDateTime now = ZonedDateTime.now();
        int id = request.returnCreatedObjectId();
        assertCalculationUserIsRecorded(id, now, user);
    }

    @Test(groups = {"IntegrRegress", G2, INTEGRATION, MAGNIT}, description = "Создание оргЮнита напрямую в ВФМ", dataProvider = "true/false",
            dataProviderClass = DataProviders.class)
    @Link(name = "Список регресс тестов - Интеграция", url = "https://wiki.goodt.me/x/pu66Dw")
    @TmsLink("101073")
    @Tag(INTEGRATION)
    public void createOrgUnitDirectlyInWFM(boolean isCorrect) {
        changeTestIDDependingOnParameter(isCorrect, "IntegrRegress - 1", "IntegrRegress - 2",
                                         "Создание ошибочной записи оргЮнита при импорте напрямую в ВФМ (с привязкой к несуществующему вышестоящему подразделению)");
        ImmutablePair<String, String> requestResult = sendPostRequestToCreateOrgUnit(isCorrect);
        if (!isCorrect) {
            int integrationCallId = checkIfIntegrationEventTableHasFailedRecord(requestResult.right);
            checkIfIntegrationCallTableHasRecord(integrationCallId, null, "ORGANIZATION_UNIT_IMPORT", false);
        } else {
            checkIfIntegrationCallTableHasRecord(null, requestResult.right, "ORGANIZATION_UNIT_IMPORT", true);
        }
        checkIfTableHasRecordByValue("organizationunit", "outerid", requestResult.left, isCorrect);
    }

    @Test(groups = {"IntegrRegress", "IntegrRegress3", G2, INTEGRATION, MAGNIT}, description = "Создание нового сотрудника и назначения напрямую в ВФМ", dataProvider = "true/false",
            dataProviderClass = DataProviders.class)
    @Link(name = "Список регресс тестов - Интеграция", url = "https://wiki.goodt.me/x/pu66Dw")
    @TmsLink("101079")
    @Tag(INTEGRATION)
    public void createEmployeePositionDirectlyInWFM(boolean isCorrect) {
        changeTestIDDependingOnParameter(isCorrect, "IntegrRegress - 3", "IntegrRegress - 5",
                                         "Создание нового сотрудника и назначения, когда в ВФМ нет оргЮнита, для которого импортируется сотрудник напрямую в ВФМ");
        JSONObject requestBody = prepareBodyForCreatingEmployeePosition(isCorrect);
        String requestResult = createOrCloseEmployeePosition(requestBody, isCorrect, false);
        if (!isCorrect) {
            int integrationCallId = checkIfIntegrationEventTableHasFailedRecord(requestResult);
            checkIfIntegrationCallTableHasRecord(integrationCallId, null, "EMPLOYEE_POSITION_FULL_IMPORT", false);
        } else {
            checkIfIntegrationCallTableHasRecord(null, requestResult, "EMPLOYEE_POSITION_FULL_IMPORT", true);
            checkIfTableHasRecordByValue(POSITION, "outerid", requestBody.getJSONObject(POSITION).getString(OUTER_ID), true);
            checkIfTableHasRecordByValue(EMPLOYEE, "outerid", requestBody.getJSONObject(EMPLOYEE).getString(OUTER_ID), true);
        }
        checkIfTableHasRecordByValue("employeeposition", "card_number", requestBody.getString(NUMBER), isCorrect);
    }

    @Test(groups = {"IntegrRegress", "IntegrRegress4", G2, INTEGRATION, MAGNIT}, description = "Закрытие существующего назначения напрямую в ВФМ")
    @Link(name = "Список регресс тестов - Интеграция", url = "https://wiki.goodt.me/x/pu66Dw")
    @TmsLink("101079")
    @Tag("IntegrRegress - 4")
    @Tag(INTEGRATION)
    public void closeEmployeePositionDirectlyInWFM() {
        JSONObject requestBody = createEmployeePositionToClose();
        requestBody.remove(END_WORK_DATE);
        LocalDate closeDate = LocalDate.now();
        requestBody.put(END_WORK_DATE, closeDate);
        String requestResult = createOrCloseEmployeePosition(requestBody, true, true);
        checkIfIntegrationCallTableHasRecord(null, requestResult, "EMPLOYEE_POSITION_FULL_IMPORT", true);
        checkIfEmployeePositionWasClosed(requestBody.getString(NUMBER), closeDate);
    }

    @Test(groups = {"IntegrRegress", "IntegrRegress6", G2, INTEGRATION, MAGNIT}, description = "Создание нового отсутствия напрямую в ВФМ",
            dataProvider = "true/false", dataProviderClass = DataProviders.class)
    @Link(name = "Список регресс тестов - Интеграция", url = "https://wiki.goodt.me/x/pu66Dw")
    @TmsLink("102537")
    @Tag(INTEGRATION)
    public void createAbsenceDirectlyWFM(boolean isCorrect) {
        changeTestIDDependingOnParameter(isCorrect, "IntegrRegress - 6", "IntegrRegress - 7",
                                         "Создание нового отсутствия с привязкой к назначению, которого нет в ВФМ, прямой импорт в ВФМ");
        ImmutablePair<OrgUnit, EmployeePosition> unitAndEmp = OrgUnitRepository.getRandomOrgUnitWithRandomEmployeeForShiftPair(false);
        EmployeePosition ep = unitAndEmp.getRight();
        ScheduleRequestAlias alias = ScheduleRequestAliasRepository.getAlias(ScheduleRequestType.getRandomScheduleRequestType());
        LocalDate date = ShiftTimePosition.FUTURE.getShiftsDateInterval().getRandomDateBetween();
        String employeeOuterId = isCorrect ? ep.getEmployee().getOuterId() : RandomStringUtils.randomAlphanumeric(5);
        String positionOuterId = isCorrect ? ep.getPosition().getOuterId() : RandomStringUtils.randomAlphanumeric(5);
        ScheduleRequest scheduleRequest = new ScheduleRequest()
                .setStatus(ScheduleRequestStatus.APPROVED.toString())
                .setEmployeeOuterId(employeeOuterId)
                .setPositionOuterId(positionOuterId)
                .setDateTimeInterval(new DateTimeInterval(date.atStartOfDay(),
                                                          date.atTime(23, 59, 59)))
                .setType(alias.getType())
                .setStartTime("00:00")
                .setEndTime("23:59");

        ScheduleRequest[] requests = {scheduleRequest};
        ApiRequest request = new ApiRequest.PostBuilder(makePath(INTEGRATION_JSON, SCHEDULE_REQUESTS))
                .withBody(requests)
                .withParams(Pairs.newBuilder()
                                    .stopOnError(false)
                                    .deleteIntersections(true)
                                    .splitRequests(true)
                                    .processShifts("delete")
                                    .startDateShiftFilter(true)
                                    .buildMap())
                .withStatus(200)
                .send();
        assertScheduleRequest(request, date, employeeOuterId, positionOuterId, isCorrect);
    }

    @Test(groups = {"IntegrRegress", "IntegrRegress12", G2, INTEGRATION, MAGNIT, IN_PROGRESS},
            description = "Создание бизнес-драйвера (kpi) по интеграции, прямой импорт в ВФМ",
            dataProvider = "true/false/null", dataProviderClass = DataProviders.class)
    @Link(name = "Список регресс тестов - Интеграция", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=263909030")
    @TmsLink("113999")
    @Tag(INTEGRATION)
    public void createBusinessDriver(Boolean isCorrect) {
        if (Objects.isNull(isCorrect)) {
            addTag("IntegrRegress14");
            changeTestName("Создание бизнес-драйвера на стороне ВФМ, с привязкой к kpi.outerid, которого нет в ВФМ, прямой импорт в ВФМ");
        } else if (!isCorrect) {
            addTag("IntegrRegress13");
            changeTestName("Создание бизнес-драйвера на стороне ВФМ, с привязкой к подразделению, которого нет в ВФМ, прямой импорт в ВФМ");
        }
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        String value = randomNumeric(3);
        String kpiOuterId = (isCorrect == null) ? "limit212321312" : "limit2";
        String organizationUnitOuterId = (isCorrect != null && isCorrect) ? orgUnit.getOuterId() : randomNumeric(8);
        String responseFromServer = createRequestBusinessDriver(isCorrect, value, kpiOuterId, organizationUnitOuterId);

        if (isCorrect != null) {
            if (isCorrect) {
                checkKpiBaseValue(value, organizationUnitOuterId, kpiOuterId);
            }
            checkIfIntegrationCallTableHasRecord(null, responseFromServer, null, true);
        } else {
            int integrationCallId = checkIfIntegrationEventTableHasFailedRecord(responseFromServer);
            checkIfIntegrationCallTableHasRecord(integrationCallId, null, null, false);
        }
    }
}
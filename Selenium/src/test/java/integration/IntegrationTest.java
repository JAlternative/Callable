package integration;

import wfm.ApiRequest;
import io.qameta.allure.*;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;
import utils.authorization.CsvLoader;
import utils.db.DBUtils;
import utils.tools.Pairs;
import wfm.components.orgstructure.OrgUnitOptions;
import wfm.components.schedule.ShiftTimePosition;
import wfm.models.*;
import wfm.repository.EmployeePositionRepository;
import wfm.repository.OrgUnitRepository;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static common.Groups.G0;
import static common.Groups.IN_PROGRESS;
import static utils.Links.*;
import static utils.Params.*;
import static utils.integration.IntegrationUtils.*;
import static utils.tools.CustomTools.getRandomFromList;
import static utils.tools.RequestFormers.makePath;

public class IntegrationTest {
    final String INT_DB = "INT_DB";
    final String INT_STAND = "INT_STAND";
    final String UPDATED = "updated";
    final String START_DATE_TIME = "startdatetime";
    final String END_DATE_TIME = "enddatetime";
    final String PLANNED = "planned";
    final String EP_FULL_NAME = "ep_fullname";
    final String WFM_ID = "wfm_id";
    final String ORG_UNIT_GUID = "org_unit_guid";
    final String DATE = "date";

    @Test
    private void dbSample() {
        OrgUnit unit = OrgUnitRepository.getRandomOmWithEmployeeOptions(OrgUnitOptions.EMPLOYEES_WITH_POSITION);
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(unit.getId(), null, true);
        int epId = ep.getId();
        List<Shift> listOne = DBUtils.getShifts(epId);
        List<Shift> listTwo = DBUtils.getShifts(epId);
        int size = listOne.size();
        SoftAssert softAssert = new SoftAssert();
        for (int i = 0; i < size; i++) {
            softAssert.assertTrue(listOne.get(i).equalsWithId(listTwo.get(i)));
        }
        softAssert.assertAll();
    }
    private ImmutablePair<EmployeePosition, Object> createAndExportRequest(Map<String, String> intRelations, String entity,
                                                                           ShiftTimePosition timePosition, Class tClass) {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitWithWorkedRosters(false);
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(unit.getId(), null, true);
        Object request = createScheduleRequestDependingOnClass(tClass, ep, timePosition, unit);
        ZonedDateTime updatedTime = sendIntegrationRequest(intRelations, entity, timePosition);
        assertRequestTransfer(ep, updatedTime, request, timePosition);
        return new ImmutablePair<>(ep, request);
    }

    @DataProvider(name = "past/future, shift/request")
    private Object[][] pastFutureShiftRequest() {
        Object[][] array = new Object[4][];
        array[0] = new Object[]{ShiftTimePosition.PAST, Shift.class, "%s и выгрузка фактических смен /export/gendalf/timesheet"};
        array[1] = new Object[]{ShiftTimePosition.FUTURE, Shift.class, "%s и выгрузка плановых смен /export/gendalf/planned-timesheet"};
        array[2] = new Object[]{ShiftTimePosition.PAST, ScheduleRequest.class, "%s и выгрузка фактических отсутствий /export/gendalf/timesheet"};
        array[3] = new Object[]{ShiftTimePosition.FUTURE, ScheduleRequest.class, "%s и выгрузка плановых отсутствий /export/gendalf/planned-timesheet"};
        return array;
    }

    @Step("Отправить запрос на сервис интеграции")
    private ZonedDateTime sendIntegrationRequest(Map<String, String> intRelations, String entity, ShiftTimePosition timePosition) {
        Map<String, String> params = Pairs.newIntegrationBuilder()
                .period(LocalDate.now().withDayOfMonth(1))
                .updatedFrom(LocalDate.now().atStartOfDay())
                .updatedTo(LocalDate.now().atStartOfDay().plusDays(1))
                .formatTt(entity)
                .buildMap();
        String endpoint;
        if (timePosition == ShiftTimePosition.PAST) {
            endpoint = TIMESHEET;
        } else {
            endpoint = PLANNED_TIMESHEET;
        }
        IntegrationApiHelpers.getRequest(makePath(EXPORT, GENDALF, endpoint), params, intRelations.get(INT_STAND));
        return ZonedDateTime.now();
    }

    @Step("Проверить, что элемент расписания {r} появился в интеграционной таблице")
    private <T> void assertRequestTransfer(EmployeePosition ep, ZonedDateTime updatedTime, T r, ShiftTimePosition timePosition) {
        int id = getFieldValue(r, ID);
        Map<String, Object> result = DBUtils.getIntegrationRequestById(id);
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(ep.getEmployee().getFullName(), result.get(EP_FULL_NAME), "ФИО сотрудника не совпали");
        softAssert.assertEquals(r.getClass().getSimpleName(), convertDBTypeToClassName(result.get(TYPE)),
                                "Тип запроса расписания не совпал");
        ZonedDateTime updatedDateTime = ZonedDateTime.of(timestampConverter(result.get(UPDATED)), ZoneId.of("UTC+3"));
        softAssert.assertTrue(updatedTime.until(updatedDateTime, ChronoUnit.SECONDS) < 60,
                              "Время обновления не совпало");
        DateTimeInterval dateTimeInterval = getFieldValue(r, DATE_TIME_INTERVAL);
        softAssert.assertEquals(dateTimeInterval.getStartDateTime(), timestampConverter(result.get(START_DATE_TIME)),
                                "Время начала не совпало");
        softAssert.assertEquals(dateTimeInterval.getEndDateTime(), timestampConverter(result.get(END_DATE_TIME)),
                                "Время окончания не совпало");
        softAssert.assertEquals(timePosition.equals(ShiftTimePosition.FUTURE), result.get(PLANNED),
                                String.format("Значение столбца \"%s\" не совпало", PLANNED));
        softAssert.assertAll();
    }

    @Test(groups = {"IM6893-4", "IM6893-7", "IM6893-10", "IM6893-13", G0, IN_PROGRESS},
            dataProvider = "past/future, shift/request")
    @Link(url = "https://wiki.goodt.me/pages/viewpage.action?pageId=252085308",
            name = "Статья: \"6893_[Интеграция c Гендальф. Магнит] В api по выгрузке плановых графиков целевого табеля добавить параметр принадлежности подразделения (баг)\"")
    @TmsLink("TEST-1536")
    @Severity(SeverityLevel.CRITICAL)
    public void createAndExport(ShiftTimePosition timePosition, Class tClass, String testDesc) {
        Allure.getLifecycle().updateTestCase(t -> t.setName(String.format(testDesc, "Создание")));
        String entity = "ММ";
        Map<String, String> intRelations = getRandomFromList(CsvLoader.integrationReturner());
        DBUtils.resetLastUpdate(entity, intRelations.get(INT_DB));
        createAndExportRequest(intRelations, entity, timePosition, tClass);
    }

    @Test(groups = {"IM6893-6", "IM6893-9", "IM6893-12", "IM6893-15", G0, IN_PROGRESS},
            dataProvider = "past/future, shift/request")
    @Link(url = "https://wiki.goodt.me/pages/viewpage.action?pageId=252085308",
            name = "Статья: \"6893_[Интеграция c Гендальф. Магнит] В api по выгрузке плановых графиков целевого табеля добавить параметр принадлежности подразделения (баг)\"")
    @TmsLink("TEST-1536")
    @Severity(SeverityLevel.CRITICAL)
    public void deleteAndExport(ShiftTimePosition timePosition, Class tClass, String testDesc) {
        Allure.getLifecycle().updateTestCase(t -> t.setName(String.format(testDesc, "Удаление")));
        String entity = "ММ";
        Map<String, String> intRelations = getRandomFromList(CsvLoader.integrationReturner());
        DBUtils.resetLastUpdate(entity, intRelations.get(INT_DB));
        List<Map<String, Object>> results = DBUtils.getIntegrationRequests(LocalDate.now().minusMonths(1).withDayOfMonth(1),
                                                                           tClass, intRelations.get(INT_DB));
        Object request;
        if (results.isEmpty()) {
            ImmutablePair<EmployeePosition, Object> pair = createAndExportRequest(intRelations, entity, timePosition, tClass);
            request = pair.right;
        } else {
            Map<String, Object> result = getRandomFromList(results);
            OrgUnit unit = OrgUnitRepository.getOrgUnitByOuterId((String) result.get(ORG_UNIT_GUID));
            EmployeePosition ep = EmployeePositionRepository.getEmployeePosition((String) result.get(EP_FULL_NAME), unit.getId());
            request = fetchScheduleRequestDependingOnClass(tClass, ep, (Long) result.get(WFM_ID), LocalDate.parse(result.get(DATE).toString()));
        }
        int requestId = getFieldValue(request, ID);
        new ApiRequest.DeleteBuilder(makePath(convertClassNameToLinkPart(tClass), requestId)).send();
        sendIntegrationRequest(intRelations, entity, timePosition);
        Assert.assertNull(DBUtils.getIntegrationRequestById(requestId));
    }

    @Test(groups = {"IM6893-5", "IM6893-8", "IM6893-11", "IM6893-14", G0, IN_PROGRESS},
            dataProvider = "past/future, shift/request")
    @Link(url = "https://wiki.goodt.me/pages/viewpage.action?pageId=252085308",
            name = "Статья: \"6893_[Интеграция c Гендальф. Магнит] В api по выгрузке плановых графиков целевого табеля добавить параметр принадлежности подразделения (баг)\"")
    @TmsLink("TEST-1536")
    @Severity(SeverityLevel.CRITICAL)
    public void editAndExport(ShiftTimePosition timePosition, Class tClass, String testDesc) {
        Allure.getLifecycle().updateTestCase(t -> t.setName(String.format(testDesc, "Изменение")));
        String entity = "ММ";
        Map<String, String> intRelations = getRandomFromList(CsvLoader.integrationReturner());
        DBUtils.resetLastUpdate(entity, intRelations.get(INT_DB));
        ImmutablePair<EmployeePosition, Object> pair = createAndExportRequest(intRelations, entity, timePosition, tClass);
        Object request = pair.right;
        EmployeePosition ep = pair.left;
        DateTimeInterval interval = getFieldValue(request, DATE_TIME_INTERVAL);
        setFieldValue(request, DATE_TIME_INTERVAL, interval.offsetByMinutes(30));
        new ApiRequest.PutBuilder(makePath(convertClassNameToLinkPart(tClass), getFieldValue(request, ID)))
                .withBody(request)
                .send();
        ZonedDateTime updatedTime = sendIntegrationRequest(intRelations, entity, timePosition);
        assertRequestTransfer(ep, updatedTime, request, timePosition);
    }
}

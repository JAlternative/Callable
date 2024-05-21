package apitest;

import common.DataProviders;
import io.qameta.allure.*;
import io.qameta.allure.testng.Tag;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.http.NameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Ignore;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;
import reporting.TestListener;
import testutils.BaseTest;
import utils.Links;
import utils.Params;
import utils.Projects;
import utils.tools.LocalDateTools;
import utils.tools.Pairs;
import utils.tools.RequestFormers;
import wfm.ApiRequest;
import wfm.PresetClass;
import wfm.components.orgstructure.*;
import wfm.components.schedule.*;
import wfm.components.systemlists.TableRuleShiftType;
import wfm.components.utils.PermissionType;
import wfm.components.utils.Role;
import wfm.models.*;
import wfm.repository.*;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

import static apitest.HelperMethods.*;
import static common.ErrorMessagesForRegExp.ANY;
import static common.Groups.*;
import static utils.ErrorMessagesForReport.NO_VALID_DATE;
import static utils.Links.*;
import static utils.Params.*;
import static utils.tools.CustomTools.*;
import static utils.tools.RequestFormers.*;
import static wfm.repository.CommonRepository.*;

@Listeners({TestListener.class})
public class ScheduleBoardApiTest extends BaseTest {

    @DataProvider(name = "daily, weekly periodicity")
    private static Object[][] overtimeWithPermission() {
        Object[][] array = new Object[2][];
        array[0] = new Object[]{Periodicity.DAILY};
        array[1] = new Object[]{Periodicity.WEEKLY};
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

    @DataProvider(name = "roleAndShiftPosition (create)")
    private static Object[][] createThirdDayShiftNearTwoOvernights() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitsForShifts(false, false, false, false, false);
        List<PermissionType> permissions = new ArrayList<>(Arrays.asList(
                PermissionType.ORGANIZATION_UNIT_EDIT,
                PermissionType.SCHEDULE_VIEW,
                PermissionType.SCHEDULE_EDIT,
                PermissionType.ORG_UNIT_EMPLOYEE_PERSONALISATION,
                PermissionType.SCHEDULE_CREATE_RV_SV,
                PermissionType.SHIFT_READ_COMMENT,
                PermissionType.SHIFT_MANAGE_COMMENT));
        Role role = PresetClass.createCustomPermissionRole(permissions);
        Object[][] array = new Object[8][];
        array[0] = new Object[]{orgUnit, role, "AFTER_PLAN", "после 2х ночных смен идущих подряд, вручную (план)", 1};
        array[1] = new Object[]{orgUnit, role, "AFTER_FACT", "после 2х ночных смен идущих подряд, вручную (табель)", 1};
        array[2] = new Object[]{orgUnit, role, "BEFORE_PLAN", "перед 2х ночных смен идущих подряд, вручную (план)", 4};
        array[3] = new Object[]{orgUnit, role, "BEFORE_FACT", "перед 2х ночных смен идущих подряд, вручную (табель)", 4};
        array[4] = new Object[]{orgUnit, role, "BETWEEN_PLAN", "между 2х ночных смен идущих подряд, вручную (план)", 10};
        array[5] = new Object[]{orgUnit, role, "BETWEEN_FACT", "между 2х ночных смен идущих подряд, вручную (табель)", 10};
        array[6] = new Object[]{orgUnit, role, "AFTER_PLAN_FACT", "после 2х ночных смен идущих подряд (1-я в табеле, 2-я в плане), вручную", 7};
        array[7] = new Object[]{orgUnit, role, "BETWEEN_PLAN_FACT", "в табеле между 2х ночных смен (1-я в табеле, 2-я в плане), вручную", 13};
        return array;
    }

    @DataProvider(name = "roleAndShiftPosition (copy or change)")
    private static Object[][] copyThirdDayShiftNearTwoOvernights() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitsForShifts(false, false, false, false, false);
        List<PermissionType> permissions = new ArrayList<>(Arrays.asList(
                PermissionType.ORGANIZATION_UNIT_EDIT,
                PermissionType.SCHEDULE_VIEW,
                PermissionType.SCHEDULE_EDIT,
                PermissionType.ORG_UNIT_EMPLOYEE_PERSONALISATION,
                PermissionType.SCHEDULE_CREATE_RV_SV,
                PermissionType.SHIFT_READ_COMMENT,
                PermissionType.SHIFT_MANAGE_COMMENT));
        Role role = PresetClass.createCustomPermissionRole(permissions);
        Object[][] array = new Object[16][];
        array[0] = new Object[]{orgUnit, role, "AFTER_PLAN", "после 2х ночных смен идущих подряд, копированием (план)", 2};
        array[1] = new Object[]{orgUnit, role, "AFTER_FACT", "после 2х ночных смен идущих подряд, копированием (табель)", 2};
        array[2] = new Object[]{orgUnit, role, "BEFORE_PLAN", "перед 2х ночных смен идущих подряд, копированием (план)", 5};
        array[3] = new Object[]{orgUnit, role, "BEFORE_FACT", "перед 2х ночных смен идущих подряд, копированием (табель)", 5};
        array[4] = new Object[]{orgUnit, role, "BETWEEN_PLAN", "между 2х ночных смен идущих подряд, копированием (план)", 11};
        array[5] = new Object[]{orgUnit, role, "BETWEEN_FACT", "между 2х ночных смен идущих подряд, копированием (табель)", 11};
        array[6] = new Object[]{orgUnit, role, "AFTER_PLAN_FACT", "после 2х ночных смен идущих подряд (1-я в табеле, 2-я в плане), копированием", 8};
        array[7] = new Object[]{orgUnit, role, "BETWEEN_PLAN_FACT", "в табеле между 2х ночных смен (1-я в табеле, 2-я в плане), копированием", 14};
        array[8] = new Object[]{orgUnit, role, "AFTER_PLAN", "после 2х ночных смен идущих подряд, перемещением (план)", 3};
        array[9] = new Object[]{orgUnit, role, "AFTER_FACT", "после 2х ночных смен идущих подряд, перемещением (табель)", 3};
        array[10] = new Object[]{orgUnit, role, "BEFORE_PLAN", "перед 2х ночных смен идущих подряд, перемещением (план)", 6};
        array[11] = new Object[]{orgUnit, role, "BEFORE_FACT", "перед 2х ночных смен идущих подряд, перемещением (табель)", 6};
        array[12] = new Object[]{orgUnit, role, "BETWEEN_PLAN", "между 2х ночных смен идущих подряд, перемещением (план)", 12};
        array[13] = new Object[]{orgUnit, role, "BETWEEN_FACT", "между 2х ночных смен идущих подряд, перемещением (табель)", 12};
        array[14] = new Object[]{orgUnit, role, "AFTER_PLAN_FACT", "после 2х ночных смен идущих подряд (1-я в табеле, 2-я в плане), перемещением", 9};
        array[15] = new Object[]{orgUnit, role, "BETWEEN_PLAN_FACT", "в табеле между 2х ночных смен (1-я в табеле, 2-я в плане), перемещением", 15};
        return array;
    }

    /**
     * Формирует ссылки для post запроса на назначение сотрудника на свободную смену
     *
     * @param ep        айди позиции сотрудника
     * @param freeShift айди свободной смены (с биржи)
     */
    public ImmutablePair<String, Map<String, ImmutablePair<String, String>>> makeLinksForAssignEmployeeOnFreeShift(EmployeePosition ep, Shift freeShift) {
        Map<String, ImmutablePair<String, String>> links = new HashMap<>();
        ImmutablePair<String, String> link1 = new ImmutablePair<>(Params.HREF, ep.getSelfLink());
        ImmutablePair<String, String> link2 = new ImmutablePair<>(Params.HREF, freeShift.getLink("takeShiftFromExchange"));
        links.put(Params.EMPLOYEE_POSITION, link1);
        links.put(Params.SELF, link2);
        return new ImmutablePair<>(Params.LINKS, links);
    }

    public Map<String, Map<String, String>> makeLinksForShiftAddWorkLink(AdditionalWork addWork, Shift shift) {
        Map<String, Map<String, String>> links = new HashMap<>();
        Map<String, String> link1 = Collections.singletonMap(Params.HREF, addWork.getSelfLink());
        Map<String, String> link2 = Collections.singletonMap(Params.HREF, shift.getSelfLink());
        Map<String, String> link3 = Collections.singletonMap(Params.HREF, makePath(CommonRepository.URL_BASE, API_V1, SHIFTS_ADD_WORK_LINK));
        links.put(Params.SHIFT_ADD_WORK, link1);
        links.put(Params.SHIFT, link2);
        links.put(Params.SELF, link3);
        return links;
    }

    public Map<String, Map<String, String>> makeLinksForExchange(ExchangeRule rule, Roster roster) {
        Map<String, Map<String, String>> links = new HashMap<>();
        Map<String, String> link1 = Collections.singletonMap(Params.HREF, rule.getLink("jobTitleShift"));
        Map<String, String> link2 = Collections.singletonMap(Params.HREF, roster.getSelfLink());
        Map<String, String> link3 = Collections.singletonMap(Params.HREF, setUri(Projects.WFM, CommonRepository.URL_BASE, SHIFTS).toString());
        links.put(Params.JOB_TITLE, link1);
        links.put("roster", link2);
        links.put(SELF, link3);
        return links;
    }

    private Map<String, Object> makeBodyForMassShiftDelete(DateInterval interval, EmployeePosition ep, boolean table) {
        Map<String, Object> body = new HashMap<>();
        body.put(DATE_FROM, interval.getStartDate().toString());
        body.put(DATE_TO, interval.getEndDate().toString());
        body.put(EMPLOYEE_POSITION_ID, ep.getId());
        body.put(CALCULATE_CONSTRAINTS, true);
        body.put("table", table);
        body.put(Params.COMMENT, RandomStringUtils.randomAlphabetic(8));
        return body;
    }

    /**
     * Проверяет, что свободная смена была удалена из списка свободных смен и добавлена в расписание сотруднику
     */
    public static void assertAssignEmployeeOnFreeShift(List<Shift> shiftsBefore, List<Shift> freeShiftsBefore, Shift freeShift, EmployeePosition ep, int omId) {
        List<Shift> freeShiftsAfter = ShiftRepository.getFreeShifts(omId, freeShift.getDateTimeInterval().getStartDate());
        List<EmployeePosition> epsAfter = EmployeePositionRepository.getEmployeePositionsOnDate(ep.getEmployee().getFullName(), freeShift.getStartDate(), omId);
        assertDelete(freeShiftsBefore, freeShiftsAfter, freeShift);
        Allure.step("Проверить, что смена удалена", () -> assertDelete(freeShiftsBefore, freeShiftsAfter, freeShift));
        EmployeePosition epAfter;
        List<Shift> shiftsAfter;
        if (URL_BASE.contains(ZOZO)) {
            epAfter = EmployeePositionRepository.getEmployeePosition(ep.getEmployee().getFullName(), omId);
            shiftsAfter = ShiftRepository.getShifts(epAfter, ShiftTimePosition.FUTURE);
        } else {
            shiftsAfter = ShiftRepository.getShifts(epsAfter, ShiftTimePosition.FUTURE);
            epAfter = getTemporaryEmployeePosition(epsAfter, freeShift);
        }
        freeShift.setEmployeePositionId(epAfter.getId());
        Allure.step("Проверить, что смена добавлена в расписание сотруднику", () -> assertPost(shiftsBefore, shiftsAfter, freeShift));
    }

    public static void assertAssignEmployeeOnFreeShiftMobileGroup(List<Shift> shiftsBefore, List<Shift> freeShiftsBefore, Shift freeShift, EmployeePosition ep, int targetOmId, int sourceOmId) {
        List<Shift> freeShiftsAfter = ShiftRepository.getFreeShifts(targetOmId, freeShift.getDateTimeInterval().getStartDate());
        List<EmployeePosition> epsAfterTarget = EmployeePositionRepository.getEmployeePositionsOnDate(ep.getEmployee().getFullName(), freeShift.getStartDate(), targetOmId);
        List<EmployeePosition> epsAfterSource = EmployeePositionRepository.getEmployeePositionsOnDate(ep.getEmployee().getFullName(), freeShift.getStartDate(), sourceOmId);
        assertDelete(freeShiftsBefore, freeShiftsAfter, freeShift);
        Allure.step("Проверить, что смена удалена", () -> assertDelete(freeShiftsBefore, freeShiftsAfter, freeShift));
        List<Shift> shiftsAfter = ShiftRepository.getShifts(epsAfterTarget, ShiftTimePosition.FUTURE);
        shiftsAfter.addAll(ShiftRepository.getShifts(epsAfterSource, ShiftTimePosition.FUTURE));
        EmployeePosition epAfter = getTemporaryEmployeePosition(epsAfterTarget, freeShift);
        freeShift.setEmployeePositionId(epAfter.getId());
        Allure.step("Проверить, что смена добавлена в расписание сотруднику", () -> assertPost(shiftsBefore, shiftsAfter, freeShift));
    }

    private static EmployeePosition getTemporaryEmployeePosition(List<EmployeePosition> employeePositions, Shift freeShift) {
        return employeePositions.stream()
                .filter(e -> e.isTemporary())
                .filter(e -> e.getDateInterval().getStartDate().equals(freeShift.getStartDate()))
                .filter(e -> e.getDateInterval().getEndDate().equals(freeShift.getEndDate()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Временное назначение не создалось"));
    }

    @Step("Проверить, что смена сотрудника с именем {firstEmp} была продублирована сотруднику с именем {secondEmp}")
    private void assertCopyShift(EmployeePosition firstEmp, LocalDate dayFrom, EmployeePosition secondEmp,
                                 LocalDate dayTo) {
        Shift duplicateShift = ShiftRepository.getShift(secondEmp, dayTo, null);
        ShiftRepository.getShift(firstEmp, dayFrom, null).assertSameTime(duplicateShift);
        Allure.addAttachment("Смена", duplicateShift.toString());
    }

    @Step("Проверить перемещение смен")
    private void assertTransferShifts(EmployeePosition firstEmp, EmployeePosition secondEmp, Shift firstShift, Shift secondShift) {
        Shift newFirstShift = ShiftRepository.getShift(firstEmp, firstShift.getDateTimeInterval().getStartDate(), null);
        Shift newSecondShift = ShiftRepository.getShift(secondEmp, secondShift.getDateTimeInterval().getStartDate(), null);
        secondShift.assertSameTime(newFirstShift);
        firstShift.assertSameTime(newSecondShift);
    }

    @Step("Проверить перемещение смены на свободную дату")
    private void assertTransferShift(EmployeePosition firstEmp, EmployeePosition secondEmp, Shift firstShift, LocalDate secondShiftDate) {
        LocalDate firstDate = firstShift.getDateTimeInterval().getStartDate();
        Shift newFirstShift = ShiftRepository.getShift(firstEmp, firstDate, null);
        Shift newSecondShift = ShiftRepository.getShift(secondEmp, secondShiftDate, null);
        Assert.assertNull(newFirstShift, String.format("У сотрудника %s всё ещё осталась смена на %s", firstEmp, firstDate));
        firstShift.assertSameTime(newSecondShift);
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

    /**
     * Редактировать время смены
     */
    private void editShiftTime(Shift shift, DateTimeInterval dateTimeInterval, User user, int status, String message) {
        shift.setDateTimeInterval(dateTimeInterval);
        shift.setOutstaff(null);
        new ApiRequest.PutBuilder(shift.getSelfPath())
                .withBody(shift)
                .withParams(Pairs.newBuilder().calculateConstraints(false).buildMap())
                .withUser(user)
                .withStatus(status)
                .withMessage(message)
                .send();
    }

    private void editShift(Shift shift, User user, int status, String message) {
        editShiftTime(shift, shift.getDateTimeInterval().offsetByMinutes(-30), user, status, message);
    }

    private static boolean isTable(int omId, ShiftTimePosition timePosition, ImmutablePair<LocalDate, LocalDate> dates) {
        boolean table = false;
        if (timePosition.getShiftsDateInterval().equals(ShiftTimePosition.PAST.getShiftsDateInterval()) || timePosition.getShiftsDateInterval().equals(ShiftTimePosition.PREVIOUS_MONTH.getShiftsDateInterval()) && dates.right.isBefore(LocalDate.now())) {
            if (RosterRepository.getZeroRoster(omId, timePosition.getShiftsDateInterval()).getVersion() == 0) {
                table = true;
            }
        }
        return table;
    }

    /**
     * Дублирование смены
     *
     * @param sourceEp сотрудник, чьи смены копируются
     * @param targetEp сотрудник, которому дублируются смены
     * @param source   дата дублируемой смены
     * @param target   дата, на которую дублируется смена
     */
    @Step("Копировать одну смену от {sourceEp} за {source} к сотруднику с именем {targetEp} за {target}")
    private void copyShift(int omId, EmployeePosition sourceEp, EmployeePosition targetEp,
                           LocalDate source, LocalDate target, ShiftTimePosition timePosition) {
        requestForShiftsInteraction(omId, sourceEp, targetEp, source, source, target, target, timePosition, null, COPY, 200, null);
    }

    private void copyShifts(int omId, EmployeePosition sourceEp, EmployeePosition targetEp,
                            ImmutablePair<LocalDate, LocalDate> sourceDates,
                            ImmutablePair<LocalDate, LocalDate> targetDates, ShiftTimePosition timePosition) {
        requestForShiftsInteraction(omId, sourceEp, targetEp,
                                    sourceDates.left, sourceDates.right,
                                    targetDates.left, targetDates.right,
                                    timePosition, null, COPY, 200, null);
    }

    @Step("Переместить одну смену от {employeePositionFrom} за {dateFrom} к сотруднику с именем {employeePositionTo} за {dateTo}")
    private void transferOneShift(int omId, EmployeePosition sourceEp, EmployeePosition targetEp,
                                  LocalDate source, LocalDate target, ShiftTimePosition timePosition) {
        requestForShiftsInteraction(omId, sourceEp, targetEp, source, source, target, target, timePosition, null, CHANGE, 200, null);
    }

    private void transferShifts(int omId, EmployeePosition sourceEp, EmployeePosition targetEp,
                                ImmutablePair<LocalDate, LocalDate> sourceDates,
                                ImmutablePair<LocalDate, LocalDate> targetDates, ShiftTimePosition timePosition) {
        requestForShiftsInteraction(omId, sourceEp, targetEp,
                                    sourceDates.left, sourceDates.right,
                                    targetDates.left, targetDates.right,
                                    timePosition, null, CHANGE, 200, null);
    }

    /**
     * Запрос на копирование или перемещение смены
     *
     * @param sourceEp   сотрудник, чьи смены копируются или перемещаются
     * @param targetEp   сотрудник, которому копируются или перемещаются смены
     * @param sourceFrom первая дата списка копируемых или перемещаемых смен
     * @param sourceTo   последняя дата списка копируемых или перемещаемых смен
     * @param targetFrom первая из списка дат, на которые копируются или перемещаются смены
     * @param targetTo   последняя из списка дат, на которые копируются или перемещаются смены
     * @param type       тип запроса: COPY или CHANGE
     */
    private void requestForShiftsInteraction(int omId, EmployeePosition sourceEp, EmployeePosition targetEp,
                                             LocalDate sourceFrom, LocalDate sourceTo,
                                             LocalDate targetFrom, LocalDate targetTo, ShiftTimePosition timePosition, User user, String type, int status, String message) {
        boolean table = isTable(omId, timePosition, new ImmutablePair<>(sourceFrom, sourceTo));
        Map<String, Object> map = HelperMethods.getMapForShiftActions(sourceEp, targetEp,
                                                                      sourceFrom, sourceTo,
                                                                      targetFrom, targetTo, table);
        new ApiRequest.PutBuilder(makePath(SHIFTS, type + "-" + SHIFTS))
                .withBody(map)
                .withUser(user)
                .withStatus(status)
                .withMessage(message)
                .send();
    }

    @Step("Проверить отображение сотрудника {employee} в списке свободной смены. Сотрудник отображается: {displayed}")
    private void freeShiftEmployeesCheck(EmployeePosition employee, Shift freeShift, boolean displayed) {
        changeStepNameDependingOnParameter(displayed, String.format("Проверить, что сотрудник %s отображается в API", employee),
                                           String.format("Проверить, что сотрудник %s не отображается в API", employee));
        Integer employeeId = employee.getId();
        List<Integer> employeePositionsIds = new ArrayList<>();
        List<String> employeesForAttachment = new ArrayList<>();
        String urlEnding = makePath(Links.EMPLOYEE_POSITIONS, PLAIN, Links.SHIFT, freeShift.getId());
        JSONObject object = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, urlEnding);
        if (!object.isEmpty()) {
            JSONObject embedded = object.getJSONObject(EMBEDDED);
            JSONArray suggestedEmployeePositions = embedded.getJSONArray("plainEmployeePositionResList");
            for (int i = 0; i < suggestedEmployeePositions.length(); i++) {
                JSONObject temp = suggestedEmployeePositions.getJSONObject(i).getJSONObject(EMBEDDED);
                Integer epId = suggestedEmployeePositions.getJSONObject(i).getInt(ID);
                String epName = temp.getString("employeeLastName") + " " + temp.getString("employeeFirstName");
                employeePositionsIds.add(epId);
                employeesForAttachment.add(epName);
            }
        }
        LOG.info(String.format("Список id сотрудников в списке созданной свободной смены: %s", employeePositionsIds));
        Allure.addAttachment("Список сотрудников в списке созданной свободной смены", employeesForAttachment.toString());
        if (displayed) {
            Assert.assertTrue(employeePositionsIds.contains(employeeId), "Сотрудник не появился в списке созданной свободной смены");
        } else {
            Assert.assertFalse(employeePositionsIds.contains(employeeId), "Сотрудник отображается в списке");
        }
    }

    @Step("Проверить, что у дневной смены нет подтипа третьей ночной")
    private void assertDayShiftWithoutSubtype(Shift dayShift) {
        String status = dayShift.getStatus();
        String subType = dayShift.getSubtype();
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertTrue(Objects.isNull(status) || status.equals("APPROVED"), "У созданной дневной смены статус " + status);
        softAssert.assertTrue(Objects.isNull(subType), "У созданной дневной смены подтип " + subType);
        softAssert.assertAll();
    }

    @Step("Проверить, что пользователь получил уведомление о публикации графика")
    public void assertNotificationRecieved(User user, String title, String text, boolean shouldHaveNotification) {
        changeStepNameDependingOnParameter(shouldHaveNotification, "Проверить, что пользователь получил уведомление о публикации графика",
                                           "Проверить, что пользователь не получил уведомление о публикации графика");
        List<LinkedHashMap> response;
        LinkedHashMap actualNotification = new LinkedHashMap<>();
        Map<String, String> pairs = Pairs.newBuilder().size(10000)
                .received(false)
                .deleted(false)
                .clientZoneOffset("+05:00")
                .buildMap();
        String path = RequestFormers.makePath(API, NOTIFY_LINK, Links.NOTIFICATIONS, USER, user.getId());
        int i = 0;
        do {
            systemSleep(2);
            ApiRequest request = new ApiRequest.GetBuilder(path)
                    .withHeaders(Collections.singletonMap("Wfm-Internal", getToken()))
                    .withParams(pairs)
                    .withUser(user)
                    .send();
            response = request.returnJsonValue(RequestFormers.makeJsonPath(EMBEDDED, "notificationResList"));
            if (response != null) {
                LOG.info(response.toString());
                actualNotification = response.stream()
                        .filter(linkedHashMap -> linkedHashMap.get(TITLE).toString().trim().equals(title) && linkedHashMap.get(TEXT).toString().equals(text))
                        .findAny()
                        .orElseGet(LinkedHashMap::new);
            } else {
                LOG.info(String.format("У пользователя с логином %s нет уведомлений", user.getUsername()));
            }
            i++;
        } while (actualNotification.isEmpty() && i < 5);
        if (shouldHaveNotification) {
            Assert.assertFalse(actualNotification.isEmpty(), "Пользователь не получил уведомление, несмотря на наличие пермишенов");
        } else {
            Assert.assertTrue(actualNotification.isEmpty(), "Пользователь получил уведомление, несмотря на отсутствие пермишенов");
        }
    }

    private JSONObject createShiftJsonObject(OrgUnit orgUnit, EmployeePosition emp, LocalDate startDate, LocalDate endDate,
                                             LocalTime startTime, LocalTime endTime) {
        JSONObject shiftJson = new JSONObject();
        shiftJson.put(STATUS, ScheduleRequestStatus.APPROVED);
        shiftJson.put(ORG_UNIT_OUTER_ID, orgUnit.getOuterId());
        shiftJson.put(EMPLOYEE_OUTER_ID, emp.getEmployee().getOuterId());
        shiftJson.put(START_DATE, startDate);
        shiftJson.put(END_DATE, endDate);
        shiftJson.put(START_TIME, startTime);
        shiftJson.put(END_TIME, endTime);
        return shiftJson;
    }

    @Test(groups = {"TEST-165", G0, SCHED21,
            "@Before disable pre-publication checks"},
            description = "Отправка графика на утверждение")
    @Link(name = "Статья: \"Расписание\"", url = "https://wiki.goodt.me/x/RAOrCg")
    @Severity(SeverityLevel.CRITICAL)
    @TmsLink("61617")
    @Tag("TEST-165")
    @Tag(SCHED21)
    public void sendRosterForApproval() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        disablePublishSystemPropertiesIfNoLimitIsSet(unit);
        PresetClass.publishGraphPreset(GraphStatus.NOT_PUBLISH, unit);
        Allure.step("Отправить запрос на утверждение графика", () -> PresetClass.publishGraphPreset(GraphStatus.ON_APPROVAL, unit));
        Roster rosterAfter = RosterRepository.getActiveRosterThisMonth(omId);
        assertTime(rosterAfter.getOnApprovalTime(), unit, ZonedDateTime.now());
        assertPut(rosterAfter, Collections.singletonMap("onApproval", true));
    }

    @Test(groups = {"TEST-166", G0, SCHED12,
            "@Before disable pre-publication checks"},
            description = "Публикация графика")
    @Link(name = "Статья: \"Расписание\"", url = "https://wiki.goodt.me/x/RAOrCg")
    @Severity(SeverityLevel.CRITICAL)
    @TmsLink("61616")
    @Tag("TEST-166")
    @Tag(SCHED12)
    public void publishRoster() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        disablePublishSystemPropertiesIfNoLimitIsSet(unit);
        PresetClass.publishGraphPreset(GraphStatus.NOT_PUBLISH, unit);
        Allure.step("Отправить запрос на публикацию графика", () -> PresetClass.publishGraphPreset(GraphStatus.PUBLISH, unit));
        Roster rosterAfter = RosterRepository.getActiveRosterThisMonth(omId);
        assertTime(rosterAfter.getPublicationTime(), unit, ZonedDateTime.now());
        assertPut(rosterAfter, Collections.singletonMap("published", true));
    }

    @Test(groups = {"TEST-132", SHIFTS, G0, SCHED9,
            "@Before set default shift duration",
            "@Before disable start time check for worked shifts"},
            description = "Создание смен")
    @Link(name = "Статья: \"Расписание\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=178979652")
    @Severity(SeverityLevel.CRITICAL)
    @TmsLink("61650")
    @Tag("TEST-132.1")
    @Tag(SCHED9)
    public void createShift() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        PresetClass.changeOrSetMathParamValue(omId, MathParameterValues.ACCESS_TO_OVERWORK, true, true);
        PresetClass.kpiAndFteChecker(omId);
        EmployeePosition employeePosition = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        LocalDate date = PresetClass.getFreeDateFromNow(employeePosition);
        ShiftTimePosition timePosition = ShiftTimePosition.FUTURE;
        List<Shift> before = ShiftRepository.getShifts(employeePosition, timePosition);
        Shift addedShift = Allure.step("Добавить смену", () -> PresetClass.presetForMakeShiftDate(employeePosition, date, false, timePosition));
        List<Shift> after = ShiftRepository.getShifts(employeePosition, timePosition);
        assertPost(before, after, addedShift);
    }

    @Test(groups = {"TEST-133", SHIFTS, G0, SCHED9,
            "@Before set default shift duration",
            "@Before disable mandatory comments when deleting worked shift"}, description = "Удаление смен")
    @Link(name = "Удаление смены за первое число месяца", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=178979652")
    @Severity(SeverityLevel.CRITICAL)
    @TmsLink("60279")
    @Tag(SCHED9)
    @Tag("TEST-133")
    public void deleteShift() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        PresetClass.kpiAndFteChecker(omId);
        EmployeePosition employeePosition = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        ShiftTimePosition timePosition = ShiftTimePosition.DEFAULT;
        Shift shift = PresetClass.defaultShiftPreset(employeePosition);
        List<Shift> before = ShiftRepository.getShifts(employeePosition, timePosition);
        new ApiRequest.DeleteBuilder(shift).send();
        List<Shift> after = ShiftRepository.getShifts(employeePosition, timePosition);
        assertDelete(before, after, shift);
    }

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
        PresetClass.kpiAndFteChecker(omId);
        EmployeePosition employeePosition = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        ShiftTimePosition timePosition = ShiftTimePosition.FUTURE;
        Shift shift = PresetClass.defaultShiftPreset(employeePosition, timePosition);
        List<Shift> before = ShiftRepository.getShifts(employeePosition, timePosition);
        editShift(shift, null, 201, null);
        List<Shift> after = ShiftRepository.getShifts(employeePosition, timePosition);
        assertPut(before, after, Collections.singletonMap(DATE_TIME_INTERVAL, shift.getDateTimeInterval()));
    }

    @Test(groups = {"TEST-133.1", SHIFTS, G1, SCHED9,
            "@Before disable check of worked roster before adding shift",
            "@Before disable mandatory comments when deleting worked shift"},
            description = "Удаление смены за первое число месяца")
    @Link(name = "Удаление смены за первое число месяца", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=178979652")
    @Owner(BUTINSKAYA)
    @TmsLink("61649")
    @TmsLink("60279")
    @Tag("TEST-133.1")
    @Tag(SCHED9)
    public void deleteShiftFirstDayOfMonth() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        PresetClass.kpiAndFteChecker(unit.getId());
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(unit.getId(), null, false);
        Shift shift = ShiftRepository.getFirstDayMonthShift(ep);
        ShiftTimePosition timePosition = ShiftTimePosition.PAST;
        if (getServerDateTime().toLocalDate().equals(LocalDateTools.getFirstDate())) {
            timePosition = ShiftTimePosition.FUTURE;
        }
        List<Shift> shiftsBefore = ShiftRepository.getShifts(ep, timePosition);
        new ApiRequest.DeleteBuilder(shift).send();
        List<Shift> shiftsAfter = ShiftRepository.getShifts(ep, timePosition);
        assertDelete(shiftsBefore, shiftsAfter, shift);
    }

    @Test(groups = {"TEST-128.1", SHIFTS, G1, SCHED9,
            "@Before disable check of worked roster before adding shift",
            "@Before disable check of worked shifts against plan",
            "@Before disable cutting of worked shifts to fit the plan"},
            description = "Копирование смены на смену другого сотрудника")
    @Link(name = "Статья: \"Расписание\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=178979652")
    @TmsLink("61654")
    @Tag("TEST-128.1")
    @Owner(MATSKEVICH)
    @Tag(SCHED9)
    @Severity(SeverityLevel.NORMAL)
    public void copyShiftToShift() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = orgUnit.getId();
        PresetClass.changeOrSetMathParamValue(omId, MathParameterValues.ACCESS_TO_OVERWORK, true, true);
        ImmutablePair<EmployeePosition, EmployeePosition> twoEmployee = EmployeePositionRepository.getTwoRandomEmployeeWithCheckByApi(omId);
        EmployeePosition firstEmp = twoEmployee.left;
        EmployeePosition secondEmp = twoEmployee.right;
        ShiftTimePosition timePosition = ShiftTimePosition.DEFAULT;
        LocalDate firstShiftDate = PresetClass.defaultShiftPreset(firstEmp).getStartDate();
        LocalDate secondShiftDate = PresetClass.presetForMakeShiftTime(secondEmp, LocalTime.of(8, 0), LocalTime.of(17, 0), timePosition).getStartDate();
        copyShift(omId, firstEmp, secondEmp, firstShiftDate, secondShiftDate, ShiftTimePosition.DEFAULT);
        assertCopyShift(firstEmp, firstShiftDate, secondEmp, secondShiftDate);
    }

    @Test(groups = {"TEST-128.2", SHIFTS, G1, SCHED9, POCHTA,
            "@Before disable check of worked roster before adding shift",
            "@Before disable check of worked shifts against plan",
            "@Before disable cutting of worked shifts to fit the plan"},
            description = "Копирование смены на смену одного сотрудника")
    @Link(name = "Статья: \"Расписание\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=178979652")
    @TmsLink("61654")
    @Tag("TEST-128.2")
    @Owner(MATSKEVICH)
    @Tag(SCHED9)
    @Severity(SeverityLevel.NORMAL)
    public void copyShiftToShiftForOneEmp() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = orgUnit.getId();
        PresetClass.changeOrSetMathParamValue(omId, MathParameterValues.ACCESS_TO_OVERWORK, true, true);
        EmployeePosition emp = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        ShiftTimePosition timePosition = ShiftTimePosition.DEFAULT;
        LocalDate firstShiftDate = PresetClass.defaultShiftPreset(emp).getStartDate();
        LocalDate secondShiftDate = PresetClass.presetForMakeShiftTime(emp, LocalTime.of(8, 0), LocalTime.of(17, 0), timePosition, firstShiftDate).getStartDate();
        copyShift(omId, emp, emp, firstShiftDate, secondShiftDate, timePosition);
        assertCopyShift(emp, firstShiftDate, emp, secondShiftDate);
    }

    @Test(groups = {"TEST-125.1", SHIFTS, G1, SCHED9,
            "@Before disable check of worked roster before adding shift", "@Before disable check of worked shifts against plan"},
            description = "Копирование смены, которая заканчивается следующим днем, на пустую ячейку другого сотрудника")
    @Link(name = "Статья: \"Расписание\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=178979652")
    @TmsLink("61657")
    @Tag("TEST-125.1")
    @Owner(MATSKEVICH)
    @Tag(SCHED9)
    @Severity(SeverityLevel.NORMAL)
    public void copyOneShiftEndInNextDay() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = orgUnit.getId();
        PresetClass.changeOrSetMathParamValue(omId, MathParameterValues.ACCESS_TO_OVERWORK, true, true);
        ImmutablePair<EmployeePosition, EmployeePosition> twoEmployee = EmployeePositionRepository.getTwoRandomEmployeeWithCheckByApi(omId);
        EmployeePosition firstEmp = twoEmployee.left;
        EmployeePosition secondEmp = twoEmployee.right;
        ShiftTimePosition shiftTimePosition = ShiftTimePosition.DEFAULT;
        LocalDate firstShiftDate = PresetClass.shiftDateEndTomorrowInFuturePreset(firstEmp, shiftTimePosition).getStartDate();
        LocalDate freeDate = PresetClass.getFreeDateForEmployeeShiftPreset(secondEmp);
        copyShift(omId, firstEmp, secondEmp, firstShiftDate, freeDate, shiftTimePosition);
        assertCopyShift(firstEmp, firstShiftDate, secondEmp, freeDate);
    }

    @Test(groups = {"TEST-124.1", SHIFTS, G0, SCHED9,
            "@Before disable check of worked roster before adding shift", "@Before disable check of worked shifts against plan"},
            description = "Копирование одиночной смены на свободную ячейку другого сотрудника")
    @Link(name = "Статья: \"Расписание\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=178979652")
    @TmsLink("61658")
    @Tag("TEST-124.1")
    @Owner(MATSKEVICH)
    @Tag(SCHED9)
    @Severity(SeverityLevel.CRITICAL)
    public void copyOneShiftOnEmptyCellAnotherEmployee() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = orgUnit.getId();
        PresetClass.changeOrSetMathParamValue(omId, MathParameterValues.ACCESS_TO_OVERWORK, true, true);
        ImmutablePair<EmployeePosition, EmployeePosition> twoEmployee = EmployeePositionRepository.getTwoRandomEmployeeWithCheckByApi(omId);
        EmployeePosition firstEmp = twoEmployee.left;
        EmployeePosition secondEmp = twoEmployee.right;
        LocalDate firstShiftDate = PresetClass.defaultShiftPreset(firstEmp).getStartDate();
        LocalDate freeDate = PresetClass.getFreeDateForEmployeeShiftPreset(secondEmp);
        copyShift(omId, firstEmp, secondEmp, firstShiftDate, freeDate, ShiftTimePosition.DEFAULT);
        assertCopyShift(firstEmp, firstShiftDate, secondEmp, freeDate);
    }

    @Test(groups = {"TEST-124.2", SHIFTS, G1, SCHED9, POCHTA,
            "@Before disable check of worked roster before adding shift", "@Before disable check of worked shifts against plan"},
            description = "Копирование одиночной смены на свободную ячейку одного сотрудника")
    @Link(name = "Статья: \"Расписание\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=178979652")
    @TmsLink("61658")
    @Tag("TEST-124.2")
    @Owner(MATSKEVICH)
    @Tag(SCHED9)
    @Severity(SeverityLevel.NORMAL)
    public void copyOneShiftOnEmptyCellSameEmployee() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = orgUnit.getId();
        PresetClass.changeOrSetMathParamValue(omId, MathParameterValues.ACCESS_TO_OVERWORK, true, true);
        EmployeePosition emp = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        LocalDate firstShiftDate = PresetClass.defaultShiftPreset(emp).getStartDate();
        LocalDate freeDate = PresetClass.getFreeDateForEmployeeShiftPreset(emp, firstShiftDate);
        copyShift(omId, emp, emp, firstShiftDate, freeDate, ShiftTimePosition.DEFAULT);
        assertCopyShift(emp, firstShiftDate, emp, freeDate);
    }

    @Test(groups = {"ABCHR3900", "ABCHR3900-3", G1, SHIFTS, SCHED9,
            "@Before disable check of worked roster before adding shift",
            "@Before forbid roster edits in past",
            "@Before disallow timesheet editing for past months", "@Before disable all shift comments",
            "@Before disable check of worked shifts against plan",
            "@Before disable cutting of worked shifts to fit the plan"},
            dataProvider = "true/false", dataProviderClass = DataProviders.class,
            description = "Копирование смены в табеле")
    @Link(name = "Статья: \"3900_Добавить права на редактирование табеля за прошлое\"", url = "https://wiki.goodt.me/x/RwHND")
    @TmsLink("60329")
    @Tag("ABCHR3900-3")
    @Owner(MATSKEVICH)
    @Tag(SCHED9)
    @Severity(SeverityLevel.NORMAL)
    private void copyShiftInWorkedRoster(boolean hasAccess) {
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
        User user = getUserWithPermissions(permissions, unit);
        ShiftTimePosition shiftTimePosition = ShiftTimePosition.PAST;
        LocalDate shiftDate = PresetClass.defaultShiftPreset(ep, shiftTimePosition).getStartDate();
        LocalDate freeDate = PresetClass.getFreeDateForEmployeeShiftPreset(ep, shiftTimePosition, shiftDate);
        LocalDate earlierDate = shiftDate.isBefore(freeDate) ? shiftDate : freeDate;
        if (hasAccess) {
            Allure.step(String.format("У сотрудника %s копировать смену с даты %s на дату %s", ep, shiftDate, freeDate),
                        () -> requestForShiftsInteraction(omId, ep, ep, shiftDate, shiftDate, freeDate, freeDate, shiftTimePosition, user, COPY, 200, null));
            assertCopyShift(ep, shiftDate, ep, freeDate);
        } else {
            Allure.step(String.format("Провалить попытку скопировать смену сотрудника %s с даты %s на дату %s", ep, shiftDate, freeDate),
                        () -> requestForShiftsInteraction(omId, ep, ep, shiftDate, shiftDate, freeDate, freeDate, shiftTimePosition, user, COPY, 400,
                                                          String.format("Changing the shift related to this date is not allowed %s. Perhaps the selected period has become unavailable for editing, we recommend refreshing the page to update the display of the closed period.",
                                                                        earlierDate.format(DateTimeFormatter.ofPattern("dd.MM.uuuu")))));
        }
    }

    @Test(groups = {"TEST-127.1", SHIFTS, G1, SCHED9,
            "@Before disable check of worked roster before adding shift",
            "@Before disable check of worked shifts against plan",
            "@Before disable cutting of worked shifts to fit the plan"},
            description = "Обмен одной смены разных сотрудников")
    @Link(name = "Статья: \"Расписание\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=178979652")
    @TmsLink("61655")
    @Tag("TEST-127.1")
    @Owner(MATSKEVICH)
    @Tag(SCHED9)
    @Severity(SeverityLevel.NORMAL)
    public void moveOneShiftToTakenCellAnotherEmployee() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = orgUnit.getId();
        PresetClass.changeOrSetMathParamValue(omId, MathParameterValues.ACCESS_TO_OVERWORK, true, true);
        ImmutablePair<EmployeePosition, EmployeePosition> twoEmployee = EmployeePositionRepository.getTwoRandomEmployeeWithCheckByApi(omId);
        EmployeePosition firstEmp = twoEmployee.left;
        EmployeePosition secondEmp = twoEmployee.right;
        ShiftTimePosition timePosition = ShiftTimePosition.DEFAULT;
        Shift fromShift = PresetClass.defaultShiftPreset(firstEmp);
        LocalDate firstShiftDate = fromShift.getStartDate();
        Shift toShift = PresetClass.presetForMakeShiftTime(secondEmp, LocalTime.of(8, 0), LocalTime.of(17, 0), timePosition, firstShiftDate);
        transferOneShift(omId, firstEmp, secondEmp, firstShiftDate, toShift.getStartDate(), timePosition);
        assertTransferShifts(firstEmp, secondEmp, fromShift, toShift);
    }

    @Test(groups = {"TEST-121.2", SHIFTS, G1, SCHED9,
            "@Before disable check of worked roster before adding shift", "@Before disable check of worked shifts against plan",
            "@Before disable all shift comments"},
            description = "Перенос одной смены на свободную ячейку одного сотрудника")
    @Link(name = "Статья: \"Расписание\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=178979652")
    @TmsLink("61661")
    @Tag("TEST-121.2")
    @Owner(MATSKEVICH)
    @Tag(SCHED9)
    @Severity(SeverityLevel.NORMAL)
    public void moveOneShiftToEmptyCellSameEmployee() {
        checkCertainNumberOfDaysInFuture(LocalDateTools.getLastDate().minusDays(2));
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = orgUnit.getId();
        PresetClass.changeOrSetMathParamValue(omId, MathParameterValues.ACCESS_TO_OVERWORK, true, true);
        EmployeePosition emp = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        ShiftTimePosition timePosition = ShiftTimePosition.FUTURE;
        Shift firstShift = PresetClass.defaultShiftPreset(emp, timePosition);
        LocalDate freeDate = PresetClass.getFreeDateFromNow(emp, firstShift.getDateTimeInterval().getStartDate());
        transferOneShift(omId, emp, emp, firstShift.getStartDate(), freeDate, timePosition);
        assertTransferShift(emp, emp, firstShift, freeDate);
    }

    @Test(groups = {"TEST-122.1", SHIFTS, G1, SCHED9,
            "@Before disable check of worked roster before adding shift",
            "@Before disable check of worked shifts against plan",
            "@Before disable cutting of worked shifts to fit the plan"},
            description = "Перенос смены, которая заканчивается следующим днем, на пустую ячейку другого сотрудника")
    @Link(name = "Статья: \"Расписание\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=178979652")
    @TmsLink("61660")
    @Tag("TEST-122.1")
    @Owner(MATSKEVICH)
    @Tag(SCHED9)
    @Severity(SeverityLevel.NORMAL)
    public void moveOneOvernightShiftToEmptyCellAnotherEmployee() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = orgUnit.getId();
        PresetClass.changeOrSetMathParamValue(omId, MathParameterValues.ACCESS_TO_OVERWORK, true, true);
        ImmutablePair<EmployeePosition, EmployeePosition> twoEmployee = EmployeePositionRepository.getTwoRandomEmployeeWithCheckByApi(omId);
        EmployeePosition firstEmp = twoEmployee.left;
        EmployeePosition secondEmp = twoEmployee.right;
        ShiftTimePosition timePosition = ShiftTimePosition.DEFAULT;
        Shift firstShift = PresetClass.shiftDateEndTomorrowInFuturePreset(firstEmp, timePosition);
        LocalDate freeDate = PresetClass.getFreeDateForEmployeeShiftPreset(secondEmp);
        transferOneShift(omId, firstEmp, secondEmp, firstShift.getStartDate(), freeDate, timePosition);
        assertTransferShift(firstEmp, secondEmp, firstShift, freeDate);
    }

    @Test(groups = {"TEST-127.2", SHIFTS, G1, SCHED9,
            "@Before disable check of worked roster before adding shift",
            "@Before disable check of worked shifts against plan",
            "@Before disable cutting of worked shifts to fit the plan"},
            description = "Обмен одной смены одного сотрудника")
    @Link(name = "Статья: \"Расписание\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=178979652")
    @TmsLink("61655")
    @Tag("TEST-127.2")
    @Owner(MATSKEVICH)
    @Tag(SCHED9)
    @Severity(SeverityLevel.NORMAL)
    public void moveOneShiftToTakenCellSameEmployee() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = orgUnit.getId();
        PresetClass.changeOrSetMathParamValue(omId, MathParameterValues.ACCESS_TO_OVERWORK, true, true);
        EmployeePosition emp = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        Shift fromShift = PresetClass.defaultShiftPreset(emp);
        LocalDate firstShiftDate = fromShift.getStartDate();
        ShiftTimePosition timePosition = ShiftTimePosition.DEFAULT;
        Shift toShift = PresetClass.presetForMakeShiftTime(emp, LocalTime.of(8, 0), LocalTime.of(17, 0), timePosition, firstShiftDate);
        transferOneShift(omId, emp, emp, firstShiftDate, toShift.getStartDate(), timePosition);
        assertTransferShifts(emp, emp, fromShift, toShift);
    }

    @Test(groups = {"TEST-121.1", SHIFTS, G0, SCHED9,
            "@Before disable all shift comments", "@Before disable check of worked shifts against plan"},
            description = "Перенос одной смены на свободную ячейку другого сотрудника")
    @Link(name = "Статья: \"Расписание\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=178979652")
    @TmsLink("61661")
    @Tag("TEST-121.1")
    @Owner(MATSKEVICH)
    @Tag(SCHED9)
    @Severity(SeverityLevel.CRITICAL)
    public void moveOneShiftToEmptyCellAnotherEmployee() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = orgUnit.getId();
        PresetClass.changeOrSetMathParamValue(omId, MathParameterValues.ACCESS_TO_OVERWORK, true, true);
        ImmutablePair<EmployeePosition, EmployeePosition> twoEmployee = EmployeePositionRepository.getTwoRandomEmployeeWithCheckByApi(omId);
        EmployeePosition firstEmp = twoEmployee.left;
        EmployeePosition secondEmp = twoEmployee.right;
        ShiftTimePosition timePosition = ShiftTimePosition.FUTURE;
        Shift firstShift = PresetClass.defaultShiftPreset(firstEmp, timePosition);
        LocalDate freeDate = PresetClass.getFreeDateFromNow(secondEmp);
        transferOneShift(omId, firstEmp, secondEmp, firstShift.getStartDate(), freeDate, timePosition);
        assertTransferShift(firstEmp, secondEmp, firstShift, freeDate);
    }

    @Test(groups = {"ABCHR3900", G1, SHIFTS, SCHED9,
            "@Before disable check of worked roster before adding shift", "@Before forbid roster edits in past",
            "@Before disallow timesheet editing for past months", "@Before disable all shift comments", "@Before disable check of worked shifts against plan"},
            dataProvider = "true/false", dataProviderClass = DataProviders.class,
            description = "Перемещение смены в табеле")
    @Link(name = "Статья: \"3900_Добавить права на редактирование табеля за прошлое\"", url = "https://wiki.goodt.me/x/RwHND")
    @TmsLink("60329")
    @Tag("ABCHR3900-4")
    @Owner(MATSKEVICH)
    @Tag(SCHED9)
    @Severity(SeverityLevel.NORMAL)
    private void moveShiftInWorkedRoster(boolean hasAccess) {
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

        User user = getUserWithPermissions(permissions, unit);
        ShiftTimePosition timePosition = ShiftTimePosition.PAST;
        Shift shift = PresetClass.defaultShiftPreset(ep, timePosition);
        LocalDate shiftDate = shift.getStartDate();
        LocalDate freeDate = PresetClass.getFreeDateForEmployeeShiftPreset(ep, timePosition, shiftDate);
        if (hasAccess) {
            Allure.step(String.format("Переместить смену сотрудника %s с даты %s на дату %s", ep, shiftDate, freeDate),
                        () -> requestForShiftsInteraction(omId, ep, ep, shiftDate, shiftDate, freeDate, freeDate, timePosition, user, CHANGE, 200, null));
            assertTransferShift(ep, ep, shift, freeDate);
        } else {
            Allure.step(String.format("Провалить попытку перемещения смены сотрудника %s с даты %s на дату %s", ep, shiftDate, freeDate),
                        () -> requestForShiftsInteraction(omId, ep, ep, shiftDate, shiftDate, freeDate, freeDate, timePosition, user, CHANGE, 400, String.format("Changing the shift related to this date is not allowed .*")));
        }
    }

    @Test(groups = {"ABCHR4195-2", G1, LIST1,
            "@Before keep shifts under requests",
            "@Before move to exchange not only shifts from exchange"},
            dataProvider = "true/false", dataProviderClass = DataProviders.class,
            description = "Применение признака \"Отдавать смену на биржу\" при создании запроса в расписании")
    @Link(name = "4195_При проставлении отпуска или больничного не отдавать смены на биржу", url = "https://wiki.goodt.me/x/0Qf6D")
    @Severity(SeverityLevel.NORMAL)
    @TmsLink("60278")
    @Tag("ABCHR4195-2")
    @Tag(LIST1)
    public void createVacationRequestWithMoveToExchange(boolean doMoveToExchange) {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = orgUnit.getId();
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        ScheduleRequestType requestType = ScheduleRequestType.VACATION;
        changeSystemListEnableValue(requestType, true);
        changeSystemListMoveToExchangeValue(requestType, doMoveToExchange);
        ShiftTimePosition timePosition = ShiftTimePosition.FUTURE;
        Shift shift = PresetClass.presetForMakeShift(ep, false, timePosition);
        LocalDate date = shift.getDateTimeInterval().getStartDate();
        List<Shift> shiftsBefore = ShiftRepository.getShifts(ep, timePosition);
        List<Shift> freeShiftsBefore = ShiftRepository.getFreeShifts(omId, date);
        List<ScheduleRequest> before = ScheduleRequestRepository
                .getEmployeeScheduleRequestsByType(ep.getEmployee().getId(), timePosition.getShiftsDateInterval(), omId, requestType);
        ScheduleRequest request = PresetClass.createScheduleRequestForDate(ScheduleRequestStatus.APPROVED, date,
                                                                           ep, requestType);
        List<Shift> shiftsAfter = ShiftRepository.getShifts(ep, timePosition);
        List<ScheduleRequest> after = ScheduleRequestRepository
                .getEmployeeScheduleRequestsByType(ep.getEmployee().getId(), timePosition.getShiftsDateInterval(), omId, requestType);
        List<Shift> freeShiftsAfter = ShiftRepository.getFreeShifts(omId, date);
        assertPost(before, after, request);
        assertDelete(shiftsBefore, shiftsAfter, shift);
        if (doMoveToExchange) {
            assertPost(freeShiftsBefore, freeShiftsAfter, shift.setEmployeePositionId(0));
        } else {
            assertNotChanged(freeShiftsBefore, freeShiftsAfter);
        }
    }

    @Test(groups = {"SE-3", G1, SCHED32,
            "@Before show shift hiring reason",
            "@Before forbid shift exchange use job title"},
            description = "Создание свободой смены в расписании при наличии свободных смен в выбранный день")
    @Link(name = "Статья: \"Свободные смены. Биржа смен\"",
            url = "https://wiki.goodt.me/x/2gUtD")
    @Severity(SeverityLevel.NORMAL)
    @TmsLink("60277")
    @Tag("SE-3")
    @Tag(SCHED32)
    public void createFreeShift() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        PositionGroup posGroup = PositionGroupRepository.randomPositionGroup();
        PositionCategory posCat = PositionCategoryRepository.randomDynamicPositionCategory();
        ShiftHiringReason reason = PresetClass.setupHiringReasonAndEntityPropertyForOrgUnit(omId);
        LocalDate freeShiftDate = ShiftTimePosition.FUTURE.getShiftsDateInterval().getRandomDateBetween();
        Shift freeShift = PresetClass.makeFreeShift(freeShiftDate, omId, null, posGroup, posCat, reason, null, null, null);
        DateTimeInterval interval = freeShift.getDateTimeInterval().offsetByMinutes(60);
        freeShift.setId(null)
                .setLinks(null)
                .setEmployeePositionId(null)
                .setDateTimeInterval(interval)
                .setPositionGroup(posGroup.setLinks(null))
                .setPositionCategory(posCat.setLinks(null));

        List<Shift> freeShiftsBefore = ShiftRepository.getFreeShifts(omId, freeShiftDate);
        new ApiRequest.PostBuilder(SHIFTS)
                .withBody(freeShift)
                .withParams(Pairs.newBuilder().calculateConstraints(true).buildMap())
                .send();
        List<Shift> freeShiftsAfter = ShiftRepository.getFreeShifts(omId, freeShiftDate);
        freeShift.setEmployeePositionId(0);
        assertPost(freeShiftsBefore, freeShiftsAfter, freeShift);
    }

    @Test(groups = {"SE-4", G1, SCHED32,
            "@Before disable roster single edited version"},
            description = "Удаление свободной смены")
    @Link(name = "Статья: \"Свободные смены. Биржа смен\"",
            url = "https://wiki.goodt.me/x/2gUtD")
    @Severity(SeverityLevel.NORMAL)
    @TmsLink("60277")
    @Tag("SE-4")
    @Tag(SCHED32)
    public void deleteFreeShift() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        PositionGroup posGroup = PositionGroupRepository.randomPositionGroup();
        PositionCategory posCat = PositionCategoryRepository.randomDynamicPositionCategory();
        LocalDate freeShiftDate = ShiftTimePosition.FUTURE.getShiftsDateInterval().getRandomDateBetween();
        Shift freeShift = PresetClass.makeFreeShift(freeShiftDate, omId, null, posGroup, posCat, null, null, null, null);
        freeShift.setEmployeePositionId(null);
        editShift(freeShift, null, 201, null);
        List<Shift> freeShiftsBefore = ShiftRepository.getFreeShifts(omId, freeShiftDate);

        new ApiRequest.DeleteBuilder(freeShift).send();
        List<Shift> freeShiftsAfter = ShiftRepository.getFreeShifts(omId, freeShiftDate);
        assertDelete(freeShiftsBefore, freeShiftsAfter, freeShift);
    }

    @Test(groups = {"SE-6", G1, SCHED32,
            "@Before allow free shifts for external employees",
            "@Before show additional information on shift exchange",
            "@Before enable indication of exchange shifts",
            //"@Before check if last day of month",
            "@Before disable pre-publication checks",
            "@Before publish without checking for yearly overtime limit violation"},
            description = "Назначение на свободную смену сотрудника из другого подразделения")
    @Link(name = "Статья: \"Свободные смены. Биржа смен\"",
            url = "https://wiki.goodt.me/x/2gUtD")
    @Severity(SeverityLevel.NORMAL)
    @TmsLink("60277")
    @Tag("SE-6")
    @Tag(SCHED32)
    public void assignEmployeeOnFreeShiftFromOtherOrgUnit() {
        ImmutablePair<OrgUnit, OrgUnit> units = OrgUnitRepository.getTwoOrgUnitsForShifts();
        OrgUnit mainUnit = units.left;
        int mainOmId = mainUnit.getId();
        int secondOmId = units.right.getId();
        EmployeePosition ep = EmployeePositionRepository.getEmployeePositionWithCardNumber(secondOmId, null, true);
        PositionGroup posGroup = PositionGroupRepository.randomPositionGroup();
        PositionCategory posCat = PositionCategoryRepository.randomDynamicPositionCategory();
        PresetClass.changePosition(ep, posCat, posGroup, null);
        ep = PresetClass.setRate(ep, 0.5);
        PresetClass.addRandomTagToEmployeeAndOrgUnit(mainUnit, ep.getEmployee());
        ShiftTimePosition timePosition = ShiftTimePosition.FUTURE;
        LocalDate freeShiftDate = timePosition.getShiftsDateInterval().getRandomDateBetween();
        Shift freeShift = PresetClass.makeFreeShift(freeShiftDate, mainOmId, null, posGroup, posCat, null, null, null, null);
        PresetClass.prepareShifts(ep, freeShiftDate, false, true, secondOmId);

        List<Shift> freeShiftsBefore = ShiftRepository.getFreeShifts(mainOmId, freeShiftDate);
        EmployeePosition epBefore = EmployeePositionRepository.getEmployeePosition(ep.getEmployee().getFullName(), mainOmId);
        List<Shift> shiftsBefore = epBefore == null ? new ArrayList<>() : ShiftRepository.getShifts(epBefore, timePosition);
        List<EmployeePosition> employeePositionsBefore = EmployeePositionRepository.getEmployeePositions(mainOmId);

        new ApiRequest.PostBuilder(makePath(SHIFTS, EXCHANGE, freeShift.getId()))
                .withBody(makeLinksForAssignEmployeeOnFreeShift(ep, freeShift))
                .withStatus(200)
                .send();
        assertAssignEmployeeOnFreeShift(shiftsBefore, freeShiftsBefore, freeShift, ep, mainOmId);
        List<EmployeePosition> employeePositionsAfter = EmployeePositionRepository.getEmployeePositions(mainOmId);
        assertPost(employeePositionsBefore, employeePositionsAfter, ep);
    }

    @Test(groups = {"SE-6.1", G1, SCHED32,
            "@Before show additional information on shift exchange",
            "@Before enable indication of exchange shifts",
            "@Before allow free shifts for own employees",
            //"@Before check if last day of month",
            "@Before disable pre-publication checks",
            "@Before publish without checking for yearly overtime limit violation"},
            description = "Назначение на свободную смену сотрудника")
    @Link(name = "Статья: \"Свободные смены. Биржа смен\"",
            url = "https://wiki.goodt.me/x/2gUtD")
    @Severity(SeverityLevel.NORMAL)
    @TmsLink("60277")
    @Tag("SE-6.1")
    @Tag(SCHED32)
    public void assignEmployeeOnFreeShiftFromSameOrgUnit() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        PositionGroup posGroup = PositionGroupRepository.randomPositionGroup();
        PositionCategory posCat = PositionCategoryRepository.randomDynamicPositionCategory();
        PresetClass.changePosition(ep, posCat, posGroup, null);
        LocalDate freeShiftDate = ShiftTimePosition.FUTURE.getShiftsDateInterval().getRandomDateBetween();
        List<EmployeePosition> epsBefore = EmployeePositionRepository.getEmployeePositionsOnDate(ep.getEmployee().getFullName(), freeShiftDate, omId);
        PresetClass.prepareShifts(ep, freeShiftDate, false, false, omId);
        Shift freeShift = PresetClass.makeFreeShift(freeShiftDate, omId, null, posGroup, posCat, null, null, null, null);
        ShiftTimePosition timePosition = ShiftTimePosition.FUTURE;
        List<Shift> shiftsBefore = ShiftRepository.getShifts(epsBefore, timePosition);
        List<Shift> freeShiftsBefore = ShiftRepository.getFreeShifts(omId, freeShiftDate);
        new ApiRequest.PostBuilder(makePath(SHIFTS, EXCHANGE, freeShift.getId()))
                .withBody(makeLinksForAssignEmployeeOnFreeShift(ep, freeShift))
                .withStatus(200)
                .send();
        assertAssignEmployeeOnFreeShift(shiftsBefore, freeShiftsBefore, freeShift, ep, omId);
    }

    @Test(groups = {"SE-7", G1, SCHED40, MAGNIT,
            "@Before allow shift exchange use job title",
            "@Before show shift hiring reason",
            "@Before check if last day of month"},
            description = "Создание свободной смены для внештатного сотрудника в расписании")
    @Link(name = "Статья: \"Свободные смены. Биржа смен\"",
            url = "https://wiki.goodt.me/x/2gUtD")
    @Severity(SeverityLevel.NORMAL)
    @TmsLink("60277")
    @Tag("SE-7")
    @Tag(SCHED40)
    public void createFreeShiftForOutstaffEmployee() {
        //настройки для магнита
        changeProperty(SystemProperties.LINK_IN_THE_PERSONAL_ACCOUNT_OF_THE_COUNTERPARTY, "test");
        PresetClass.setSystemPropertyValue(SystemProperties.BPMN_URL, "");
        PresetClass.setSystemPropertyValue(SystemProperties.BPMN_PROCESS_DEFINITION_SHIFT_EXCHANGE, "");
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        EntityProperty prop = EntityPropertyRepository.getAllPropertiesFromUnit(omId)
                .stream()
                .filter(e -> e.getPropKey().equals(ORG_UNIT_FORMAT))
                .findFirst()
                .orElseThrow(() -> new AssertionError("У подразделения нет атрибута \"Принадлежность подразделения\""));
        ExchangeRule rule = ExchangeRuleRepository.getAllRules().stream()
                .filter(r -> r.getEmployeeValue().equals(prop.getValue().toString()))
                .findAny()
                .orElse(new ExchangeRule(prop.getValue().toString(), true));
        JobTitle jobTitle = getClassObjectFromJson(JobTitle.class, getJsonFromUri(Projects.WFM, rule.getLink(JOB_TITLE_SHIFT)));
        ShiftHiringReason hiringReason = ShiftHiringReasonRepository.getRandomShiftHiringReasonsForJobTitle(omId, jobTitle.getId());
        ShiftTimePosition timePosition = ShiftTimePosition.FUTURE;
        LocalDate freeShiftDate = timePosition.getShiftsDateInterval().getRandomDateBetween();
        PresetClass.removeFreeShifts(omId, freeShiftDate);
        DateTimeInterval interval = new DateTimeInterval(freeShiftDate.atTime(14, 0), freeShiftDate.atTime(22, 0));
        Shift freeShift = new Shift();
        Roster activeRoster = RosterRepository.getActiveRosterThisMonth(omId);
        freeShift.setId(null)
                .setLinks(new JSONObject(makeLinksForExchange(rule, activeRoster)))
                .setEmployeePositionId(null)
                .setDateTimeInterval(interval)
                .setJobTitle(jobTitle.getFullName())
                .setRosterId(activeRoster.getId())
                .setPositionCategoryRosterId(getPositionCategoryRosterId(omId, timePosition))
                .setHiringReasonText(hiringReason.getTitle())
                .setOutstaff(true);
        List<Shift> freeShiftsBefore = ShiftRepository.getFreeShifts(omId, freeShiftDate);
        new ApiRequest.PostBuilder(SHIFTS)
                .withBody(freeShift)
                .withParams(Pairs.newBuilder().calculateConstraints(true).buildMap())
                .send();
        Shift addedShift = ShiftRepository.getFreeShift(omId, interval, jobTitle, true);
        Allure.step(String.format("Отправить запрос на добавление к свободной смене причины привлечения \"%s\"", hiringReason.getTitle()),
                    () -> PresetClass.addHiringReason(addedShift, hiringReason));
        List<Shift> freeShiftsAfter = ShiftRepository.getFreeShifts(omId, freeShiftDate);
        freeShift.setEmployeePositionId(0);
        assertPost(freeShiftsBefore, freeShiftsAfter, freeShift);
    }

    @Test(groups = {"ABCHR4430-1", SHIFTS, G1, SCHED37, MAGNIT,
            "@Before enable additional work",
            "@Before display additional work only with chosen statuses"},
            description = "Создание смены с доп. работой в статусе \"Запланировано\"")
    @Link(name = "Статья: \"4430_Доработать статусы к Доп. работам\"",
            url = "https://wiki.goodt.me/x/_hX6D")
    @Severity(SeverityLevel.NORMAL)
    @TmsLink("60244")
    @Tag("ABCHR4430-1")
    @Tag(SCHED37)
    public void createShiftWithAddWorkWithStatus() {
        ImmutablePair<OrgUnit, EmployeePosition> unitAndEmp = OrgUnitRepository.getRandomOrgUnitWithRandomEmployeeForShiftPair(false);
        OrgUnit unit = unitAndEmp.left;
        int omId = unit.getId();
        EmployeePosition ep = unitAndEmp.right;
        PresetClass.changeOrSetMathParamValue(omId, MathParameterValues.ACCESS_TO_OVERWORK, true, true);
        AdditionalWork addWork = AdditionalWorkRepository.getTestAddWork(true, "test_addWork_withStatus");
        AddWorkRule.getAllRulesOfAddWork(addWork.getId()).forEach(w -> new ApiRequest.DeleteBuilder(w).send());
        PresetClass.disableRequiredAddWorks();
        addWork.setDisabled(false)
                .setHasStatuses(true);
        new ApiRequest.PutBuilder(addWork.getSelfPath()).withBody(addWork).send();
        ShiftTimePosition timePosition = ShiftTimePosition.DEFAULT;
        LocalDate date = timePosition.getShiftsDateInterval().getRandomDateBetween();
        PresetClass.addRuleToAdditionalWork(addWork, omId, date);
        PresetClass.makeClearDate(ep, date);
        Shift shift = new Shift();
        DateTimeInterval interval = new DateTimeInterval(date.atTime(14, 0), date.atTime(22, 0));
        shift.setId(null)
                .setLinks(null)
                .setEmployeePositionId(ep.getId())
                .setDateTimeInterval(interval)
                .setPositionCategoryRosterId(getPositionCategoryRosterId(omId, timePosition));
        shift = new ApiRequest.PostBuilder(SHIFTS)
                .withBody(shift)
                .withParams(Pairs.newBuilder().calculateConstraints(true).buildMap())
                .send()
                .returnCreatedObject();
        ShiftAddWorkLink addWorkLink = new ShiftAddWorkLink();
        addWorkLink
                .setShiftId(null)
                .setWorkTypeId(null)
                .setFrom(shift.getDateTimeInterval().getStartDateTime().plusMinutes(30))
                .setTo(shift.getDateTimeInterval().getStartDateTime().plusMinutes(60))
                .setShiftAddWorkStatus(AddWorkStatus.PLANNED.getStatusName())
                .setLinks(new JSONObject(makeLinksForShiftAddWorkLink(addWork, shift)));

        List<ShiftAddWorkLink> additionalWorkBefore = shift.getAdditionalWork();
        new ApiRequest.PostBuilder(SHIFTS_ADD_WORK_LINK)
                .withBody(addWorkLink)
                .send();
        List<ShiftAddWorkLink> additionalWorkAfter = shift.getAdditionalWork();
        addWorkLink.setId(additionalWorkAfter.get(0).getId());
        assertPost(additionalWorkBefore, additionalWorkAfter, addWorkLink);
    }

    @Test(groups = {"ABCHR4275-2", SHIFTS, G2, SCHED37, MAGNIT,
            "@Before enable additional work",
            "@Before display all additional work"},
            description = "Редактирование смены с изменением типа дополнительной работы с правом \"Редактирование доп. работ в смене\"")
    @Link(name = "Статья: 4275_Добавить права на блок \"Типы доп. работ\"",
            url = "https://wiki.goodt.me/x/gQr6D")
    @Severity(SeverityLevel.MINOR)
    @TmsLink("60243")
    @Tag("ABCHR4275-2")
    @Tag(SCHED37)
    public void editShiftWithAddWork() {
        ImmutablePair<OrgUnit, EmployeePosition> unitAndEmp = OrgUnitRepository.getRandomOrgUnitWithRandomEmployeeForShiftPair(false);
        OrgUnit unit = unitAndEmp.left;
        EmployeePosition ep = unitAndEmp.right;
        int omId = unit.getId();
        AdditionalWork addWork = AdditionalWorkRepository.getTestAddWork(false, "test_addWork");
        Shift shift = PresetClass.defaultShiftPreset(ep, ShiftTimePosition.FUTURE);
        AddWorkRule.getAllRulesOfAddWork(addWork.getId()).forEach(w -> new ApiRequest.DeleteBuilder(w).send());
        PresetClass.disableRequiredAddWorks();
        addWork.setDisabled(false)
                .setHasStatuses(false);
        PresetClass.addRuleToAdditionalWork(addWork, omId, shift.getStartDate());
        ShiftAddWorkLink addWorkLink = new ShiftAddWorkLink();
        addWorkLink
                .setShiftId(null)
                .setWorkTypeId(null)
                .setShiftAddWorkStatus(null)
                .setFrom(shift.getDateTimeInterval().getStartDateTime().plusMinutes(30))
                .setTo(shift.getDateTimeInterval().getStartDateTime().plusMinutes(60))
                .setLinks(new JSONObject(makeLinksForShiftAddWorkLink(addWork, shift)));

        User user = getUserWithPermissions(Arrays.asList(PermissionType.ROSTER_SHIFT_EDIT_OR_CREATE,
                                                         PermissionType.SET_SHIFT_ADD_WORK), unit);

        List<ShiftAddWorkLink> additionalWorkBefore = shift.getAdditionalWork();
        addWorkLink = new ApiRequest.PostBuilder(SHIFTS_ADD_WORK_LINK)
                .withBody(addWorkLink)
                .withUser(user)
                .send()
                .returnCreatedObject();
        List<ShiftAddWorkLink> additionalWorkAfter = shift.getAdditionalWork();
        assertPost(additionalWorkBefore, additionalWorkAfter, addWorkLink);
    }

    @Test(groups = {"ABCHR5586-2", G1, SHIFTS, SCHED13, MAGNIT},
            description = "Массовое удаление смен и запросов для сотрудников функциональной роли")
    @Severity(SeverityLevel.NORMAL)
    @Link(name = "Статья: \"5586_Возможность массового удаления нескольких смен по собственному персоналу (с указанием периода)\"",
            url = "https://wiki.goodt.me/x/fzn0DQ")
    @TmsLink("60276")
    @Tag("ABCHR5586-2")
    @Tag(SCHED13)
    private void batchShiftDeletionByFunctionalRoles() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        PositionGroup posGroup = PositionGroupRepository.getAllPositionGroupsFromOrgUnit(omId).stream().collect(randomItem());
        List<EmployeePosition> epWithPosGroup = EmployeePositionRepository.getAllEmployeesByPositionGroup(omId, posGroup);
        LocalDate start = LocalDateTools.getFirstDate();
        LocalDate end = start.plusWeeks(2);
        int rosterId = RosterRepository.getActiveRosterThisMonth(omId).getId();
        List<Shift> shiftsBefore = ShiftRepository.getShiftsForRoster(rosterId, new DateInterval(start, end));
        List<Integer> epWithPosGroupIds = epWithPosGroup.stream().map(EmployeePosition::getId).collect(Collectors.toList());
        List<Shift> deletedShifts = shiftsBefore
                .stream()
                .filter(s -> epWithPosGroupIds.contains(s.getEmployeePositionId()))
                .collect(Collectors.toList());
        if (deletedShifts.size() < 2) {
            Shift firstAddedShift = PresetClass.presetForMakeShiftDate(getRandomFromList(epWithPosGroup), start, false, ShiftTimePosition.ALLMONTH);
            Shift secondAddedShift = PresetClass.presetForMakeShiftDate(getRandomFromList(epWithPosGroup), start, false, ShiftTimePosition.ALLMONTH);
            deletedShifts.addAll(Arrays.asList(firstAddedShift, secondAddedShift));
            shiftsBefore.addAll(Arrays.asList(firstAddedShift, secondAddedShift));
        }
        User user = getUserWithPermissions(Arrays.asList(PermissionType.SCHEDULE_EDIT,
                                                         PermissionType.SCHEDULE_VIEW,
                                                         PermissionType.CAN_DELETE_SHIFT_BATCH,
                                                         PermissionType.POSITION_GROUPS_VIEW), unit);
        String epIds = epWithPosGroup
                .stream()
                .map(ep -> String.valueOf(ep.getId()))
                .collect(Collectors.joining(","));
        Map<String, String> map = Pairs.newBuilder()
                .employeePositionIds(epIds)
                .from(start)
                .to(end)
                .buildMap();
        new ApiRequest.DeleteBuilder(makePath(SHIFTS, SELECTIVE_DELETE, omId))
                .withParams(map)
                .withUser(user)
                .send();
        Allure.addAttachment("Функциональная роль", posGroup.getName());
        rosterId = RosterRepository.getActiveRosterThisMonth(omId).getId();
        List<Shift> shiftsAfter = ShiftRepository.getShiftsForRoster(rosterId, new DateInterval(start, end));
        assertBatchDelete(shiftsBefore, shiftsAfter, deletedShifts);
    }

    @Test(groups = {"ABCHR3638-1", G1, SCHED9, POCHTA},
            description = "Создание смены для сотрудника в декрете",
            expectedExceptions = AssertionError.class,
            expectedExceptionsMessageRegExp = ANY + "Employee on maternity leave" + ANY)
    @Severity(SeverityLevel.NORMAL)
    @Link(name = "Статья: \"3638_Запрет создания смены у декретников\"",
            url = "https://wiki.goodt.me/pages/viewpage.action?pageId=204282613")
    @Owner(MATSKEVICH)
    @TmsLink("60258")
    @Tag("ABCHR3638-1")
    @Tag(SCHED9)
    private void creatingShiftEmployeeDecree() {
        OrgUnit unit = OrgUnitRepository.getOrgUnitForShiftsCalculation();
        int omId = unit.getId();
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        PresetClass.assignMaternityLeaveStatus(ep, LocalDateTools.now(), LocalDateTools.getLastDate());
        LocalDate start = LocalDateTools.getFirstDate();
        Allure.step(String.format("Запустить расчет для подразделения %s с %s", unit.getName(), start), () ->
                PresetClass.calculateRoster(omId, start, LocalDateTools.getLastDate(), false, false));
        LocalDateTime secondShiftStart = ShiftTimePosition.FUTURE.getShiftsDateInterval().getRandomDateBetween().atStartOfDay();
        Allure.step(String.format("Создать сотруднику %s смену на %s - в период декрета", ep.getEmployee().getFullName(), secondShiftStart.toLocalDate()),
                    () -> PresetClass.presetForMakeShiftDateTime(ep, secondShiftStart, secondShiftStart.plusHours(4), ShiftTimePosition.FUTURE));
    }

    @Test(groups = {"ABCHR6375-1", G2, SHIFTS, SCHED41, POCHTA,
            "@Before disallow timesheet editing for past months",
            "@Before comments on shifts",
            "@Before deletion not request",
            "@Before comments on deleting shifts"},
            description = "Обязательность указания комментария при удалении смены из табеля")
    @Severity(SeverityLevel.MINOR)
    @Link(name = "Статья: \"6375_[Расписание] Удалять смены в табеле не указав комментарий\"",
            url = "https://wiki.goodt.me/pages/viewpage.action?pageId=249107258")
    @Owner(MATSKEVICH)
    @TmsLink("60245")
    @Tag("ABCHR6375-1")
    @Tag(SCHED41)
    private void deleteShiftsWithSpecifyingComment() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitWithWorkedRosters(false);
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(unit.getId(), null, true);
        ShiftTimePosition timePosition = ShiftTimePosition.PAST;
        Shift shift = PresetClass.defaultShiftPreset(ep, timePosition);
        List<Shift> shiftsBefore = ShiftRepository.getShifts(ep, timePosition);
        User user = getUserWithPermissions(Arrays.asList(
                PermissionType.SCHEDULE_VIEW,
                PermissionType.SCHEDULE_EDIT,
                PermissionType.SCHEDULE_EDIT_WORKED), unit);
        ShiftEditReason reason = getRandomFromList(ShiftEditReasonRepository.getShiftEditReasons());
        Allure.step("Удалить выбранную смену с комментарием", () ->
                new ApiRequest.DeleteBuilder(shift)
                        .withUser(user)
                        .withCommentText(reason.getTitle())
                        .send());
        List<Shift> shiftsAfter = ShiftRepository.getShifts(ep, timePosition);
        Allure.step("Проверить, что смена удалена", () -> assertDelete(shiftsBefore, shiftsAfter, shift));
    }

    @Test(groups = {"ABCHR6375-2", G2, SHIFTS, SCHED41, POCHTA,
            "@Before disallow timesheet editing for past months",
            "@Before comments on shifts",
            "@Before deletion not request",
            "@Before disable mandatory comments when deleting worked shift"},
            description = "Не обязательность указания комментария при удалении смены из табеля")
    @Severity(SeverityLevel.MINOR)
    @Link(name = "Статья: \"6375_[Расписание] Удалять смены в табеле не указав комментарий\"",
            url = "https://wiki.goodt.me/pages/viewpage.action?pageId=249107258")
    @Owner(MATSKEVICH)
    @TmsLink("60245")
    @Tag("ABCHR6375-2")
    @Tag(SCHED41)
    private void deleteShiftsWithoutSpecifyingComment() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitWithWorkedRosters(false);
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(unit.getId(), null, true);
        ShiftTimePosition timePosition = ShiftTimePosition.PAST;
        Shift shift = PresetClass.defaultShiftPreset(ep, timePosition);
        List<Shift> shiftsBefore = ShiftRepository.getShifts(ep, timePosition);
        User user = getUserWithPermissions(Arrays.asList(
                PermissionType.SCHEDULE_VIEW,
                PermissionType.SCHEDULE_EDIT,
                PermissionType.SCHEDULE_EDIT_WORKED), unit);
        Allure.step("Удалить выбранную смену без комментария", () -> new ApiRequest.DeleteBuilder(shift).withUser(user).withComment(false).send());
        List<Shift> shiftsAfter = ShiftRepository.getShifts(ep, timePosition);
        Allure.step("Проверить, что смена удалена", () -> assertDelete(shiftsBefore, shiftsAfter, shift));
    }

    @Test(groups = {"ABCHR4579", G1, SHIFTS, LIST20, MAGNIT,
            "@Before disable check of worked roster before adding shift",
            "@Before disable cutting of worked shifts to fit the plan",
            "@Before disable worked shift comments",
            "@Before disable start time check for worked shifts",
            "@Before disable typed limits check",
            "@After remove table rule",
            "@Before disable roster single edited version"},
            description = "Создание смены в доступном периоде по правилу редактирования табеля")
    @Link(name = "Статья: \"4579_Добавить системный список \"Настройки редактирования табеля\"\"", url = "https://wiki.goodt.me/x/fxf6D")
    @TmsLink("60299")
    @Tag("ABCHR4579-2")
    @Tag(LIST20)
    public void addShiftInUnlockedPeriodOfWorkedRosterPerTableRule() {
        checkFirstDayOfMonth();
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitWithWorkedRosters(true);
        int omId = unit.getId();
        PresetClass.changeOrSetMathParamValue(omId, MathParameterValues.ACCESS_TO_OVERWORK, true, true);
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        ShiftTimePosition timePosition = ShiftTimePosition.PAST;
        LocalDate date = timePosition.getShiftsDateInterval().getRandomDateBetween();
        PresetClass.makeClearDate(ep, date);
        int deepEdit = LocalDate.now().compareTo(date);
        PresetClass.addTableRuleToOrgUnit(omId, deepEdit + 1, null, null, TableRuleShiftType.TIMESHEET);
        User user = getUserWithPermissions(getBasicSchedulePermissions(), unit);
        List<Shift> before = ShiftRepository.getShifts(ep, timePosition);
        Shift shift = HelperMethods.createShift(omId, ep, date, false, timePosition, user);
        List<Shift> after = ShiftRepository.getShifts(ep, timePosition);
        assertPost(before, after, shift.setOutstaff(false));
    }

    @Test(groups = {"ABCHR4579", G1, SHIFTS, LIST20, MAGNIT,
            "@Before disable check of worked roster before adding shift",
            "@Before disable cutting of worked shifts to fit the plan",
            "@Before disable worked shift comments",
            "@Before disable start time check for worked shifts",
            "@Before disable typed limits check",
            "@After remove table rule",
            "@Before disable roster single edited version"},
            description = "Редактирование смены в доступном периоде по правилу редактирования табеля")
    @Link(name = "Статья: \"4579_Добавить системный список \"Настройки редактирования табеля\"\"", url = "https://wiki.goodt.me/x/fxf6D")
    @TmsLink("60299")
    @Tag("ABCHR4579-3")
    @Tag(LIST20)
    public void editShiftInUnlockedPeriodOfWorkedRosterPerTableRule() {
        checkFirstDayOfMonth();
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitWithWorkedRosters(true);
        int omId = unit.getId();
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        ShiftTimePosition timePosition = ShiftTimePosition.PAST;
        LocalDate date = timePosition.getShiftsDateInterval().getRandomDateBetween();
        Shift shift = PresetClass.presetForMakeShiftDate(ep, date, false, timePosition);
        int deepEdit = LocalDate.now().compareTo(date);
        PresetClass.addTableRuleToOrgUnit(omId, deepEdit + 1, null, null, TableRuleShiftType.TIMESHEET);
        DateTimeInterval interval = shift.getDateTimeInterval();
        LocalDateTime start = interval.getStartDateTime().plusMinutes(10);
        LocalDateTime end = interval.getEndDateTime().minusMinutes(10);
        DateTimeInterval newInterval = new DateTimeInterval(start, end);
        shift.setDateTimeInterval(newInterval);
        shift.setOutstaff(null);
        User user = getUserWithPermissions(getBasicSchedulePermissions(), unit);
        List<Shift> before = ShiftRepository.getShifts(ep, timePosition);
        new ApiRequest.PutBuilder(shift.getSelfPath()).withBody(shift).withUser(user).withStatus(201).send();
        List<Shift> after = ShiftRepository.getShifts(ep, timePosition);
        Map<String, DateTimeInterval> changedProperties = new HashMap<>();
        changedProperties.put(DATE_TIME_INTERVAL, newInterval);
        assertPut(before, after, changedProperties);
    }

    @Test(groups = {"ABCHR4579", G1, SHIFTS, LIST20, MAGNIT,
            "@Before disable check of worked roster before adding shift",
            "@Before disable cutting of worked shifts to fit the plan",
            "@Before disable mandatory comments when deleting worked shift",
            "@Before disable worked shift comments",
            "@Before disable start time check for worked shifts",
            "@Before disable typed limits check",
            "@After remove table rule",
            "@Before disable roster single edited version"},
            description = "Удаление смены в доступном периоде по правилу редактирования табеля")
    @Link(name = "Статья: \"4579_Добавить системный список \"Настройки редактирования табеля\"\"", url = "https://wiki.goodt.me/x/fxf6D")
    @TmsLink("60299")
    @Tag("ABCHR4579-4")
    @Tag(LIST20)
    public void deleteShiftInUnlockedPeriodOfWorkedRosterPerTableRule() {
        checkFirstDayOfMonth();
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitWithWorkedRosters(true);
        int omId = unit.getId();
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        ShiftTimePosition timePosition = ShiftTimePosition.PAST;
        LocalDate date = timePosition.getShiftsDateInterval().getRandomDateBetween();
        Shift shift = PresetClass.presetForMakeShiftDate(ep, date, false, timePosition);
        int deepEdit = LocalDate.now().compareTo(date);
        PresetClass.addTableRuleToOrgUnit(omId, deepEdit + 1, null, null, TableRuleShiftType.TIMESHEET);
        User user = getUserWithPermissions(getBasicSchedulePermissions(), unit);
        List<Shift> before = ShiftRepository.getShifts(ep, timePosition);
        new ApiRequest.DeleteBuilder(shift).withUser(user).send();
        List<Shift> after = ShiftRepository.getShifts(ep, timePosition);
        assertDelete(before, after, shift);
    }

    @Test(groups = {"ABCHR4579", G1, SHIFTS, LIST20, MAGNIT,
            "@Before disable check of worked roster before adding shift",
            "@Before disable cutting of worked shifts to fit the plan",
            "@Before disable worked shift comments",
            "@Before disable start time check for worked shifts",
            "@Before disable typed limits check",
            "@After remove table rule",
            "@Before disable roster single edited version"},
            description = "Создание и редактирование смены в закрытом для редактирования периоде табеля")
    @Link(name = "Статья: \"4579_Добавить системный список \"Настройки редактирования табеля\"\"", url = "https://wiki.goodt.me/x/fxf6D")
    @TmsLink("60299")
    @Tag("ABCHR4579-8")
    @Tag(LIST20)
    public void addOrEditShiftInLockedPeriodOfWorkedRosterPerTableRule() {
        checkFirstDayOfMonth();
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitWithWorkedRosters(true);
        int omId = unit.getId();
        PresetClass.changeOrSetMathParamValue(unit.getId(), MathParameterValues.ACCESS_TO_OVERWORK, true, true);
        ShiftTimePosition timePosition = ShiftTimePosition.PAST;
        DateInterval interval = timePosition.getShiftsDateInterval();
        LocalDate date = interval.getRandomDateBetween();
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        PresetClass.makeClearDate(ep, date);
        User user = getUserWithPermissions(getBasicSchedulePermissions(), unit);
        int deepEdit = LocalDate.now().compareTo(date);
        PresetClass.addTableRuleToOrgUnit(omId, deepEdit - 1, null, null, TableRuleShiftType.TIMESHEET);
        Shift basicShift = HelperMethods.assembleBasicShift(omId, ep, date, false, timePosition);
        new ApiRequest.PostBuilder(SHIFTS)
                .withBody(basicShift)
                .withParams(Pairs.newBuilder().calculateConstraints(true).buildMap())
                .withUser(user)
                .withStatus(400)
                .withMessage(String.format("Changing the shift related to this date is not allowed %s.*",
                                           date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))))
                .send();

        Shift shift = ShiftRepository.getShiftsForRoster(RosterRepository.getZeroRoster(omId, interval).getId(), interval)
                .stream()
                .filter(s -> !s.getDateTimeInterval().getStartDate().isAfter(date) &&
                        s.getEmployeePositionId() != 0)
                .findAny()
                .orElse(PresetClass.presetForMakeShiftDate(ep, date, false, timePosition));
        new ApiRequest.DeleteBuilder(shift).withUser(user).withStatus(400).withMessage(String.format("Changing the shift related to this date is not allowed %s.*",
                                                                                                     shift.getStartDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))).send();
    }

    @Ignore(NOT_ACTUAL)
    @Test(groups = {"ABCHR4579", G1, SHIFTS, LIST20, MAGNIT,
            "@Before disable check of worked roster before adding shift", "@Before disable cutting of worked shifts to fit the plan", "@Before allow worked shift editing",
            "@Before disable payout days", "@Before disable worked shift comments",
            "@Before disable start time check for worked shifts"},
            description = "Доступность редактирования табеля в подразделении, для которого не создано правило")
    @Link(name = "Статья: \"4579_Добавить системный список \"Настройки редактирования табеля\"\"", url = "https://wiki.goodt.me/x/fxf6D")
    @TmsLink("60299")
    @Tag("ABCHR4579-12")
    @Owner(SCHASTLIVAYA)
    @Tag(LIST20)
    public void editWorkedRosterWithNoTableRule() {
        checkFirstDayOfMonth();
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitWithWorkedRosters(true);
        int omId = unit.getId();
        PresetClass.changeOrSetMathParamValue(omId, MathParameterValues.ACCESS_TO_OVERWORK, true, true);
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        ShiftTimePosition timePosition = ShiftTimePosition.PAST;
        LocalDate date = timePosition.getShiftsDateInterval().getRandomDateBetween();
        Shift shift = PresetClass.presetForMakeShiftDate(ep, date, false, timePosition);
        DateTimeInterval interval = shift.getDateTimeInterval();
        LocalDateTime start = interval.getStartDateTime().plusMinutes(10);
        LocalDateTime end = interval.getEndDateTime().minusMinutes(10);
        DateTimeInterval newInterval = new DateTimeInterval(start, end);
        shift.setDateTimeInterval(newInterval);
        shift.setOutstaff(null);
        User user = getUserWithPermissions(getBasicSchedulePermissions(), unit);
        List<Shift> before = ShiftRepository.getShifts(ep, timePosition);
        new ApiRequest.PutBuilder(shift.getSelfPath()).withBody(shift).withUser(user).withStatus(201).send();
        List<Shift> after = ShiftRepository.getShifts(ep, timePosition);
        Map<String, DateTimeInterval> changedProperties = new HashMap<>();
        changedProperties.put(DATE_TIME_INTERVAL, newInterval);
        assertPut(before, after, changedProperties);
    }

    @Test(groups = {"TK2686-4", SHIFTS, G2, LIST2,
            "@After delete test shift comment"},
            description = "Использование созданного комментария при редактировании смены из табеля")
    @Severity(SeverityLevel.MINOR)
    @Link(name = "Статья: \" 2686_Администратор может добавлять и скрывать комментарии к сменам\"",
            url = "https://wiki.goodt.me/x/oQPFCw")
    @Owner(SCHASTLIVAYA)
    @TmsLink("60246")
    @Tag("TK2686-4")
    @Tag(LIST2)
    public void addExistingCommentWhenEditingWorkedShift() {
        checkFirstDayOfMonth();
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitWithWorkedRosters(true);
        int omId = unit.getId();
        PresetClass.changeOrSetMathParamValue(omId, MathParameterValues.ACCESS_TO_OVERWORK, true, true);
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        Shift shift = PresetClass.defaultShiftPreset(ep, ShiftTimePosition.PAST);
        String name = "Тестовый комментарий";
        PresetClass.createShiftEditReason(name);
        Map<String, String> body = new HashMap<>();
        body.put("comment", name);
        new ApiRequest.PostBuilder(makePath(shift.getSelfPath(), "comment"))
                .withBody(body)
                .withStatus(200)
                .send();
        assertPut(shift.refreshShift(), body);
    }

    @Test(groups = {"ABCHR4654-4", "ABCHR4654", G2, SCHED9,
            "@Before disallow timesheet editing for past months"},
            description = "Создание плановой смены в прошлом для пользователя",
            dataProviderClass = DataProviders.class, dataProvider = "true/false")
    @Link(name = "Статья: \"4654_Добавить системную настройку \"Кол-во дней корректировки планового графика в прошлом\"", url = "https://wiki.goodt.me/x/ZRf6D")
    @TmsLink("60227")
    @Tag(SCHED9)
    @Owner(SCHASTLIVAYA)
    public void addPlanShiftInPastAsUser(boolean hasAccess) {
        changeTestIDDependingOnParameter(hasAccess, "ABCHR4654-1", "ABCHR4654-4.1",
                                         "Создание плановой смены в прошлом в закрытом для редактирования периоде");
        checkFirstDayOfMonth();
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        List<PermissionType> permissions = Arrays.asList(PermissionType.SCHEDULE_VIEW, PermissionType.SCHEDULE_EDIT, PermissionType.EDIT_PLAN_PAST);
        User user = getUserWithPermissions(permissions, unit);
        ShiftTimePosition timePosition = ShiftTimePosition.ALLMONTH;
        // Два разных timeposition сочетаются для того, чтобы взять плановую смену (за счет ALLMONTH) из прошлого.
        // Если просто поставить PAST, то смены будут из табеля
        LocalDate date = PresetClass.getFreeDateForEmployeeShiftPreset(ep, timePosition, ShiftTimePosition.PAST.getShiftsDateInterval());
        if (hasAccess) {
            allowPlanShiftEditing();
            List<Shift> shiftsBefore = ShiftRepository.getShifts(ep, timePosition);
            Shift addedShift = HelperMethods.createShift(omId, ep, date, false, timePosition, user);
            List<Shift> shiftsAfter = ShiftRepository.getShifts(ep, timePosition);
            assertPost(shiftsBefore, shiftsAfter, addedShift.setOutstaff(false));
        } else {
            int days = LocalDate.now().compareTo(date) - 1;
            String errorMessage = days == 0 ? "Period is closed for edit" : String.format("Changing the shift related to this date is not allowed %s. Perhaps the selected period has become unavailable for editing, we recommend refreshing the page to update the display of the closed period.",
                                                                                          date.format(DateTimeFormatter.ofPattern("dd.MM.uuuu")));
            int errorCode = days == 0 ? 500 : 400;
            changeProperty(SystemProperties.PLAN_EDIT_PAST_DAYS, days);
            deletionNotRequest();
            Shift shift = assembleBasicShift(omId, ep, date, false, timePosition);
            new ApiRequest.PostBuilder(SHIFTS)
                    .withBody(shift)
                    .withParams(Pairs.newBuilder().calculateConstraints(true).buildMap())
                    .withUser(user)
                    .withStatus(errorCode)
                    .withMessage(errorMessage)
                    .send();
        }
    }

    @Test(groups = {"ABCHR4654", G2, SCHED9,
            "@Before allow plan shift editing",
            "@Before disallow timesheet editing for past months",
            "@Before disable roster single edited version",
            "@Before disable check of worked roster before adding shift",
            "@Before disable cutting of worked shifts to fit the plan"},
            description = "Редактирование плановой смены в прошлом для пользователя")
    @Link(name = "Статья: \"4654_Добавить системную настройку \"Кол-во дней корректировки планового графика в прошлом\"", url = "https://wiki.goodt.me/x/ZRf6D")
    @TmsLink("60227")
    @Owner(SCHASTLIVAYA)
    @Tag("ABCHR4654-2")
    @Tag(SCHED9)
    public void editPlanShiftInPastAsUser() {
        checkFirstDayOfMonth();
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        List<PermissionType> permissions = Arrays.asList(PermissionType.SCHEDULE_VIEW,
                                                         PermissionType.SCHEDULE_EDIT,
                                                         PermissionType.EDIT_PLAN_PAST,
                                                         PermissionType.SCHEDULE_REPORT_CARD);
        User user = getUserWithPermissions(permissions, unit);
        ShiftTimePosition timePosition = ShiftTimePosition.ALLMONTH;
        List<Shift> shifts = ShiftRepository.getShifts(ep, timePosition);
        Shift shift = shifts
                .stream()
                .filter(s -> s.getDateTimeInterval().getStartDate().isBefore(LocalDate.now()))
                .findAny()
                .orElseGet(() -> PresetClass.presetForMakeShiftDate(ep, LocalDateTools.getFirstDate(), false, timePosition, shifts));
        shift = shift.refreshShift();
        List<Shift> shiftsBefore = ShiftRepository.getShifts(ep, timePosition);
        editShift(shift, user, 201, null);
        List<Shift> shiftsAfter = ShiftRepository.getShifts(ep, timePosition);
        assertPut(shiftsBefore, shiftsAfter, Collections.singletonMap(DATE_TIME_INTERVAL,
                                                                      shift.getDateTimeInterval()));
    }

    @Test(groups = {"ABCHR4654", G2, SCHED9,
            "@Before deletion not request", "@Before disallow timesheet editing for past months"},
            description = "Удаление плановой смены в прошлом для пользователя",
            dataProviderClass = DataProviders.class, dataProvider = "true/false")
    @Link(name = "Статья: \"4654_Добавить системную настройку \"Кол-во дней корректировки планового графика в прошлом\"", url = "https://wiki.goodt.me/x/ZRf6D")
    @TmsLink("60227")
    @Tag(SCHED9)
    @Owner(SCHASTLIVAYA)
    public void deletePlanShiftInPastAsUser(boolean hasAccess) {
        changeTestIDDependingOnParameter(hasAccess, "ABCHR4654-3", "ABCHR4654-4.2",
                                         "Удаление плановой смены в прошлом в закрытом для редактирования периоде");
        checkFirstDayOfMonth();
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        List<PermissionType> permissions = Arrays.asList(PermissionType.SCHEDULE_VIEW, PermissionType.SCHEDULE_EDIT, PermissionType.EDIT_PLAN_PAST);
        User user = getUserWithPermissions(permissions, unit);
        ShiftTimePosition timePosition = ShiftTimePosition.ALLMONTH;
        List<Shift> shiftsBefore = ShiftRepository.getShifts(ep, timePosition);
        Shift shift = shiftsBefore
                .stream()
                .filter(s -> !s.getDateTimeInterval().getStartDate().isAfter(LocalDate.now()))
                .findAny()
                .orElse(PresetClass.presetForMakeShiftDate(ep, LocalDateTools.getFirstDate(), false, timePosition));
        if (!shiftsBefore.contains(shift)) {
            shiftsBefore.add(shift);
        }
        if (hasAccess) {
            allowPlanShiftEditing();
            new ApiRequest.DeleteBuilder(shift).withUser(user).send();
            List<Shift> shiftsAfter = ShiftRepository.getShifts(ep, timePosition);
            assertDelete(shiftsBefore, shiftsAfter, shift);
        } else {
            changeProperty(SystemProperties.PLAN_EDIT_PAST_DAYS, LocalDate.now().compareTo(shift.getStartDate()) - 1);
            new ApiRequest.DeleteBuilder(shift).withUser(user).withStatus(500).withMessage("Plan edit in allowed only.*").send();
        }
    }

    @Test(groups = {"ABCHR4654", G2, SCHED9,
            "@Before deletion not request", "@Before disallow timesheet editing for past months"},
            description = "Недоступность редактирования планового графика в прошлом, если настройка не задана")
    @Link(name = "Статья: \"4654_Добавить системную настройку \"Кол-во дней корректировки планового графика в прошлом\"", url = "https://wiki.goodt.me/x/ZRf6D")
    @TmsLink("60227")
    @Owner(SCHASTLIVAYA)
    @Tag("ABCHR4654-6")
    @Tag(SCHED9)
    public void editPlanShiftInPastAsUserIfPropertyNotSet() {
        checkFirstDayOfMonth();
        changeProperty(SystemProperties.PLAN_EDIT_PAST_DAYS, 0);
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        List<PermissionType> permissions = Arrays.asList(PermissionType.SCHEDULE_VIEW, PermissionType.SCHEDULE_EDIT, PermissionType.EDIT_PLAN_PAST);
        User user = getUserWithPermissions(permissions, unit);
        ShiftTimePosition timePosition = ShiftTimePosition.ALLMONTH;
        List<Shift> shiftsBefore = ShiftRepository.getShifts(ep, timePosition);
        Shift shift = shiftsBefore
                .stream()
                .filter(s -> !s.getDateTimeInterval().getStartDate().isAfter(LocalDate.now()))
                .findAny()
                .orElse(PresetClass.presetForMakeShiftDate(ep, LocalDateTools.getFirstDate(), false, timePosition));
        editShift(shift, user, 500, "Plan edit in allowed only.*");
    }

    @Test(groups = {"ABCHR4654", G2, SCHED9,
            "@Before allow plan shift editing", "@Before disallow timesheet editing for past months",
            "@Before allow editing plan shifts in future", "@Before disable all shift comments"},
            description = "Редактирование смены на текущий день без разрешения \"Редактирование плана в прошлом\"")
    @Link(name = "Статья: \"4654_Добавить системную настройку \"Кол-во дней корректировки планового графика в прошлом\"", url = "https://wiki.goodt.me/x/ZRf6D")
    @TmsLink("60227")
    @Owner(SCHASTLIVAYA)
    @Tag("ABCHR4654-7")
    @Tag(SCHED9)
    public void editTodayShiftWithoutPastPlanEditingPermission() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        List<PermissionType> permissions = Arrays.asList(PermissionType.SCHEDULE_VIEW, PermissionType.SCHEDULE_EDIT);
        User user = getUserWithPermissions(permissions, unit);
        ShiftTimePosition timePosition = ShiftTimePosition.ALLMONTH;
        LocalDateTime endDateTime = getServerDate().atTime(23, 59);
        Shift shift = PresetClass.presetForMakeShiftDateTime(ep, endDateTime.minusHours(8), endDateTime, timePosition);
        List<Shift> shiftsBefore = ShiftRepository.getShifts(ep, timePosition);
        editShift(shift, user, 201, null);
        List<Shift> shiftsAfter = ShiftRepository.getShifts(ep, timePosition);
        assertPut(shiftsBefore, shiftsAfter, Collections.singletonMap(DATE_TIME_INTERVAL,
                                                                      shift.getDateTimeInterval()));
    }

    @Test(groups = {"ABCHR4654", G2, SCHED9,
            "@Before deletion not request", "@Before allow plan shift editing", "@Before disallow timesheet editing for past months",
            "@Before disable mandatory comment when editing or deleting shift", "@Before disable all shift comments"},
            description = "Удаление смены на текущий день без разрешения \"Редактирование плана в прошлом\"")
    @Link(name = "Статья: \"4654_Добавить системную настройку \"Кол-во дней корректировки планового графика в прошлом\"", url = "https://wiki.goodt.me/x/ZRf6D")
    @TmsLink("60227")
    @Owner(SCHASTLIVAYA)
    @Tag("ABCHR4654-8")
    @Tag(SCHED9)
    public void deleteTodayShiftWithoutPastPlanEditingPermission() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        List<PermissionType> permissions = Arrays.asList(PermissionType.SCHEDULE_VIEW, PermissionType.SCHEDULE_EDIT);
        User user = getUserWithPermissions(permissions, unit);
        ShiftTimePosition timePosition = ShiftTimePosition.ALLMONTH;
        LocalDateTime endDateTime = getServerDate().atTime(23, 59);
        Shift shift = PresetClass.presetForMakeShiftDateTime(ep, endDateTime.minusHours(8), endDateTime, timePosition);
        List<Shift> shiftsBefore = ShiftRepository.getShifts(ep, timePosition);
        shift.setDateTimeInterval(shift.getDateTimeInterval().offsetByMinutes(-30));
        new ApiRequest.DeleteBuilder(shift).withUser(user).send();
        List<Shift> shiftsAfter = ShiftRepository.getShifts(ep, timePosition);
        assertDelete(shiftsBefore, shiftsAfter, shift);
    }

    @Test(groups = {"ABCHR3789", G1, SCHED32,
            "@Before disable roster single edited version"},
            description = "Создание повторяющейся свободой смены в расписании с периодичностью - ежедневно",
            dataProvider = "daily, weekly periodicity")
    @Link(name = "Статья: \"3789_Добавить возможность создавать повторяющиеся смены на бирже\"", url = "https://wiki.goodt.me/x/uQLND")
    @TmsLink("60225")
    @Tag(SCHED32)
    public void createRepeatingFreeShift(Periodicity p) {
        changeTestIDDependingOnParameter(p == Periodicity.DAILY, "ABCHR3789-1", "ABCHR3789-2",
                                         "Создание повторяющейся свободной смены в расписании с периодичностью - еженедельно");
        LocalDate lastAcceptableDate = LocalDateTools.getLastDate().minusDays(p.getRepeatEveryValues() + 1);
        checkCertainNumberOfDaysInFuture(lastAcceptableDate);
        changeTestSeverityDependingOnParameter(p == Periodicity.DAILY, SeverityLevel.MINOR, SeverityLevel.NORMAL);
        checkLastDayOfMonth();
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        LocalDate date = new DateInterval(getServerDate(), lastAcceptableDate).getRandomDateBetween();
        RepeatRule rule = new RepeatRule(p)
                .setEndDate(date.plusDays(p.getRepeatEveryValues() + 1))
                .setCustom(false);
        DateInterval interval = new DateInterval(date, rule.getEndDate());
        ShiftRepository.getFreeShifts(omId, interval).forEach(PresetClass::deleteRequest);
        List<Shift> freeShifts = HelperMethods.createRepeatingFreeShift(omId, date, false, rule, ShiftTimePosition.FUTURE, null)
                .stream().map(f -> f.refreshShift()).collect(Collectors.toList());
        List<Shift> after = ShiftRepository.getFreeShifts(omId, interval).stream().map(a -> a.refreshShift()).collect(Collectors.toList());
        assertPost(new ArrayList<>(), after, freeShifts);
    }

    @Test(groups = {"ABCHR3789", G1, SCHED32},
            description = "Редактирование серии свободных смен",
            dataProvider = "true/false", dataProviderClass = DataProviders.class)
    @Link(name = "Статья: \"3789_Добавить возможность создавать повторяющиеся смены на бирже\"", url = "https://wiki.goodt.me/x/uQLND")
    @TmsLink("60225")
    @Owner(SCHASTLIVAYA)
    @Tag(SCHED32)
    @Severity(SeverityLevel.NORMAL)
    public void editFreeShiftSeries(boolean series) {
        changeTestIDDependingOnParameter(series, "ABCHR3789-3", "ABCHR3789-3.1", "Редактирование одной свободной смены из серии");
        Periodicity p = Periodicity.DAILY;
        LocalDate lastAcceptableDate = LocalDateTools.getLastDate().minusDays(p.getRepeatEveryValues() + 1);
        checkCertainNumberOfDaysInFuture(lastAcceptableDate);
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        LocalDate date = new DateInterval(getServerDate(), lastAcceptableDate).getRandomDateBetween();
        RepeatRule rule = new RepeatRule(p).setEndDate(date.plusDays(p.getRepeatEveryValues() + 1));
        ShiftRepository.getFreeShifts(omId, new DateInterval(date, rule.getEndDate())).forEach(PresetClass::deleteRequest);
        List<Shift> freeShifts = HelperMethods.createRepeatingFreeShift(omId, date, false, rule, ShiftTimePosition.FUTURE, null);
        Shift shift = getRandomFromList(freeShifts);
        Roster activeRoster = RosterRepository.getActiveRosterThisMonth(omId);
        shift.setDateTimeInterval(shift.getDateTimeInterval().offsetByMinutes(-30))
                .setRosterId(activeRoster.getId())
                .setRepeatRule(rule)
                .setLinks(shift.getLinks().put(ROSTER, activeRoster.getLinkWrappedInJson(SELF)));
        List<Shift> before = ShiftRepository.getFreeShifts(omId, new DateInterval());
        int changedNumber;
        String path;
        String step;
        int status;
        if (series) {
            path = shift.getPath(EXCHANGE_RULE);
            step = "Отправить запрос на редактирование серии свободных смен";
            changedNumber = 3;
            status = 200;
        } else {
            path = shift.getSelfPath();
            step = "Отправить запрос на редактирование одиночной свободной смены";
            changedNumber = 1;
            status = 201;
        }
        Allure.step(step, () ->
                new ApiRequest.PutBuilder(path)
                        .withBody(shift)
                        .withParams(Pairs.newBuilder().calculateConstraints(false).buildMap())
                        .withStatus(status)
                        .send());
        List<Shift> after = ShiftRepository.getFreeShifts(omId, new DateInterval());
        assertPut(before, after, DATE_TIME_INTERVAL, Collections.singletonMap("toTimeInterval", shift.getDateTimeInterval()), changedNumber);
    }

    @Test(groups = {"ABCHR3789", G1, SCHED32},
            description = "Удаление серии свободных смен",
            dataProvider = "true/false", dataProviderClass = DataProviders.class)
    @Link(name = "Статья: \"3789_Добавить возможность создавать повторяющиеся смены на бирже\"", url = "https://wiki.goodt.me/x/uQLND")
    @TmsLink("60225")
    @Owner(SCHASTLIVAYA)
    @Tag(SCHED32)
    @Severity(SeverityLevel.NORMAL)
    public void deleteFreeShiftSeries(boolean series) {
        changeTestIDDependingOnParameter(series, "ABCHR3789-4", "ABCHR3789-4.1", "Удаление одной свободной смены из серии");
        Periodicity p = Periodicity.DAILY;
        LocalDate lastAcceptableDate = LocalDateTools.getLastDate().minusDays(p.getRepeatEveryValues() + 1);
        checkCertainNumberOfDaysInFuture(lastAcceptableDate);
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        LocalDate date = new DateInterval(getServerDate(), lastAcceptableDate).getRandomDateBetween();
        RepeatRule rule = new RepeatRule(p).setEndDate(date.plusDays(p.getRepeatEveryValues() + 1));
        ShiftRepository.getFreeShifts(omId, new DateInterval(date, rule.getEndDate())).forEach(PresetClass::deleteRequest);
        List<Shift> freeShifts = HelperMethods.createRepeatingFreeShift(omId, date, false, rule, ShiftTimePosition.FUTURE, null)
                // По логике фичи, смены серии не из будущего удаляться не должны, поэтому убираем их из этого списка
                .stream()
                .filter(s -> s.getDateTimeInterval().getStartDateTime().toLocalDate().isAfter(getServerDate()))
                .collect(Collectors.toList());
        Shift shift = getRandomFromList(freeShifts).setRepeatRule(rule);
        List<Shift> before = ShiftRepository.getFreeShifts(omId, new DateInterval());
        if (series) {
            Allure.step("Отправить запрос на удаление серии свободных смен", () ->
                    new ApiRequest.DeleteBuilder(shift.getLink(EXCHANGE_RULE)).send());
            List<Shift> after = ShiftRepository.getFreeShifts(omId, new DateInterval());
            assertBatchDelete(before, after, freeShifts);
        } else {
            Allure.step("Отправить запрос на удаление одиночной свободной смены", () ->
                    new ApiRequest.DeleteBuilder(shift).send());
            List<Shift> after = ShiftRepository.getFreeShifts(omId, new DateInterval());
            assertDelete(before, after, shift);
        }
    }

    @Test(groups = {"ABCHR-6311", G2, SHIFTS, SCHED41,
            "@Before comments on deleting shifts",
            "@Before comments on plan shifts",
            "@Before disable strong lock plan"},
            dataProviderClass = DataProviders.class, dataProvider = "true/false",
            description = "Разрешен свободный ввод комментария при массовом удалении смен в плане, если включено “Расписание. Свободный ввод комментария к смене”")
    @Link(name = "Статья: \"6311_Ограничить свободный ввод комментария при удалении смены\"", url = "https://wiki.goodt.me/x/fiRJDw")
    @TmsLink("60232")
    @Tag(SCHED41)
    public void allowCommentsOnMassShiftDeleteInPlan(boolean hasPermission) {
        changeTestIDDependingOnParameter(hasPermission, "ABCHR-6311-2", "ABCHR-6311-4",
                                         "Запрещен свободный ввод комментария при массовом удалении смен в плане, если не включено “Расписание. Свободный ввод комментария к смене”");
        if (LocalDate.now().isAfter(LocalDateTools.getLastDate().minusDays(2))) {
            throw new AssertionError(NO_VALID_DATE + "В плане не хватает дней");
        }
        ShiftTimePosition position = ShiftTimePosition.FUTURE;
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(unit.getId(), null, true);
        List<PermissionType> permissions = new ArrayList<>(Arrays.asList(PermissionType.SCHEDULE_EDIT, PermissionType.SCHEDULE_VIEW));
        if (hasPermission) {
            permissions.add(PermissionType.SHIFT_ALLOW_CUSTOM_COMMENT);
        }
        User user = getUserWithPermissions(permissions, unit);
        LocalDate start = LocalDate.now();
        LocalDate end = new DateInterval(start.plusDays(1), LocalDateTools.getLastDate()).getRandomDateBetween();
        List<LocalDate> dates = new DateInterval(start, end).getBetweenDatesList();
        changeProperty(SystemProperties.PLAN_EDIT_FUTURE_DAYS, end.compareTo(LocalDate.now().minusDays(1)));
        List<Shift> deletedShifts = new ArrayList<>(PresetClass.presetForMakeShiftsDates(ep, dates, position));
        List<Shift> shiftsBefore = ShiftRepository.getShifts(ep, position);
        Map body = makeBodyForMassShiftDelete(new DateInterval(start, end), ep, false);
        List<Shift> shiftsAfter;
        ApiRequest.Builder apiRequest = new ApiRequest.DeleteBuilder(makePath(SHIFTS, DELETE_SHIFTS)).withBody(body).withUser(user);
        if (hasPermission) {
            apiRequest.send();
            shiftsAfter = ShiftRepository.getShifts(ep, position);
            assertBatchDelete(shiftsBefore, shiftsAfter, deletedShifts);
        } else {
            apiRequest
                    .withStatus(500)
                    .withMessage("Custom comment not allowed")
                    .send();
            shiftsAfter = ShiftRepository.getShifts(ep, position);
            assertNotChanged(shiftsBefore, shiftsAfter);
        }
    }

    @Test(groups = {"ABCHR-6311", G2, SHIFTS, SCHED41,
            "@Before comments on deleting shifts",
            "@Before comments on shifts",
            "@Before disable check of worked roster before adding shift",
            "@Before disable strong lock plan",
            "@Before disable cutting of worked shifts to fit the plan"},
            dataProviderClass = DataProviders.class, dataProvider = "true/false",
            description = "Разрешен свободный ввод комментария при массовом удалении смен в табеле, если включено “Расписание. Свободный ввод комментария к смене”")
    @Link(name = "Статья: \"6311_Ограничить свободный ввод комментария при удалении смены\"", url = "https://wiki.goodt.me/x/fiRJDw")
    @TmsLink("60232")
    @Tag(SCHED41)
    public void allowCommentsOnMassShiftDeleteInTable(boolean hasPermission) {
        changeTestIDDependingOnParameter(hasPermission, "ABCHR-6311-2", "ABCHR-6311-4",
                                         "Запрещен свободный ввод комментария при массовом удалении смен в табеле, если не включено “Расписание. Свободный ввод комментария к смене”");
        checkCertainNumberOfDaysInPast(LocalDateTools.getFirstDate().plusDays(2));
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitWithWorkedRosters(true);
        int omId = unit.getId();
        PresetClass.changeOrSetMathParamValue(omId, MathParameterValues.ACCESS_TO_OVERWORK, true, true);
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        List<PermissionType> permissions = new ArrayList<>(Arrays.asList(PermissionType.SCHEDULE_EDIT,
                                                                         PermissionType.SCHEDULE_VIEW,
                                                                         PermissionType.SCHEDULE_EDIT_WORKED));
        if (hasPermission) {
            permissions.add(PermissionType.SHIFT_ALLOW_CUSTOM_COMMENT);
        }
        ShiftTimePosition position = ShiftTimePosition.PAST;
        User user = getUserWithPermissions(permissions, unit);
        LocalDate start = LocalDateTools.getFirstDate();
        LocalDate end = new DateInterval(start.plusDays(1), LocalDate.now().minusDays(1)).getRandomDateBetween();
        List<LocalDate> dates = new DateInterval(start, end).getBetweenDatesList();
        List<Shift> deletedShifts = new ArrayList<>(PresetClass.presetForMakeShiftsDates(ep, dates, position));
        List<Shift> shiftsBefore = ShiftRepository.getShifts(ep, position);
        Map body = makeBodyForMassShiftDelete(new DateInterval(start, end), ep, true);
        ApiRequest.Builder apiRequest = new ApiRequest.DeleteBuilder(makePath(SHIFTS, DELETE_SHIFTS)).withBody(body).withUser(user);
        List<Shift> shiftsAfter;
        if (hasPermission) {
            apiRequest.send();
            shiftsAfter = ShiftRepository.getShifts(ep, position);
            assertBatchDelete(shiftsBefore, shiftsAfter, deletedShifts);
        } else {
            apiRequest
                    .withStatus(500)
                    .withMessage("Custom comment not allowed")
                    .send();
            shiftsAfter = ShiftRepository.getShifts(ep, position);
            assertNotChanged(shiftsBefore, shiftsAfter);
        }
    }

    @Test(groups = {"ABCHR3900", "ABCHR3900-1", G1, SHIFTS, SCHED9,
            "@Before disable check of worked roster before adding shift", "@Before forbid roster edits in past",
            "@Before disallow timesheet editing for past months", "@Before disable all shift comments",
            "@Before disable cutting of worked shifts to fit the plan"},
            dataProvider = "true/false", dataProviderClass = DataProviders.class,
            description = "Создание смены в табеле")
    @Link(name = "Статья: \"3900_Добавить права на редактирование табеля за прошлое\"", url = "https://wiki.goodt.me/x/RwHND")
    @TmsLink("60329")
    @TmsLink("60005")
    @Tag("ABCHR3900-1")
    @Tag(SCHED9)
    private void addShiftInWorkedRoster(boolean hasAccess) {
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
        User user = getUserWithPermissions(permissions, unit);
        ShiftTimePosition timePosition = ShiftTimePosition.PAST;
        LocalDate date = PresetClass.getFreeDateForEmployeeShiftPreset(ep, timePosition);
        List<Shift> before = ShiftRepository.getShifts(ep, timePosition);
        Shift shift = assembleBasicShift(omId, ep, date, false, timePosition);
        ApiRequest.Builder builder = new ApiRequest.PostBuilder(SHIFTS)
                .withBody(shift)
                .withParams(Pairs.newBuilder().calculateConstraints(true).buildMap())
                .withUser(user);
        if (hasAccess) {
            builder.send();
            List<Shift> after = ShiftRepository.getShifts(ep, timePosition);
            assertPost(before, after, shift.setOutstaff(false));
        } else {
            builder.withStatus(400)
                    .withMessage(String.format("Changing the shift related to this date is not allowed %s. Perhaps the selected period has become unavailable for editing, we recommend refreshing the page to update the display of the closed period.",
                                               date.format(DateTimeFormatter.ofPattern("dd.MM.uuuu"))))
                    .send();
            List<Shift> after = ShiftRepository.getShifts(ep, timePosition);
            assertNotChanged(before, after);
        }
    }

    @Test(groups = {"TEST-137", G1, SHIFTS, SCHED9,
            "@Before set default shift duration"},
            description = "Редактирование смены заканчивающейся в следующем дне")
    @Link(name = "Статья: \"Расписание\"", url = "https://wiki.goodt.me/x/RAOrCg")
    @TmsLink("60233")
    @Tag("TEST-137")
    @Tag(SCHED9)
    @TmsLink("#60005")
    @Owner(SCHASTLIVAYA)
    public void editOvernightShift() {
        checkLastDayOfMonth();
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = orgUnit.getId();
        PresetClass.kpiAndFteChecker(omId);
        EmployeePosition employeePosition = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        ShiftTimePosition timePosition = ShiftTimePosition.FUTURE;
        Shift shift = PresetClass.shiftDateEndTomorrowInFuturePreset(employeePosition, timePosition);
        List<Shift> before = ShiftRepository.getShifts(employeePosition, timePosition);
        editShift(shift, null, 201, null);
        List<Shift> after = ShiftRepository.getShifts(employeePosition, timePosition);
        assertPut(before, after, Collections.singletonMap(DATE_TIME_INTERVAL, shift.getDateTimeInterval()));
    }

    @Test(groups = {"ABCHR2885-1", SHIFTS, G1, SCHED9,
            "@Before disable check of worked roster before adding shift",
            "@Before disable start time check for worked shifts", "@Before disable all shift comments"},
            description = "Массовое копирование смен на свободные ячейки одного сотрудника")
    @Link(name = "Статья: \"Расписание\"", url = "https://wiki.goodt.me/x/RAOrCg")
    @Tag("ABCHR2885-1")
    @Tag(SCHED9)
    @TmsLink("60005")
    @Owner(SCHASTLIVAYA)
    public void batchCopyShiftsToEmptyCellsSameEmployee() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = orgUnit.getId();
        PresetClass.changeOrSetMathParamValue(omId, MathParameterValues.ACCESS_TO_OVERWORK, true, true);
        EmployeePosition employee = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        ShiftTimePosition timePosition = ShiftTimePosition.DEFAULT;
        Shift[] sourceShifts = PresetClass.massShiftPresetCheckForEmployee(employee);
        ImmutablePair<LocalDate, LocalDate> sourceDates = new ImmutablePair<>(sourceShifts[0].getStartDate(), sourceShifts[1].getStartDate());
        ImmutablePair<LocalDate, LocalDate> targetDates = PresetClass.twoFreeDaysChecker(employee, timePosition, sourceDates.left, sourceDates.right);
        copyShifts(omId, employee, employee, sourceDates, targetDates, timePosition);
        assertCopyShift(employee, sourceDates.left, employee, targetDates.left);
        assertCopyShift(employee, sourceDates.right, employee, targetDates.right);
    }

    @Test(groups = {"ABCHR2885-2", SHIFTS, G1, SCHED9,
            "@Before disable check of worked roster before adding shift",
            "@Before disable start time check for worked shifts"},
            description = "Массовое копирование смен на занятые ячейки одного сотрудника")
    @Link(name = "Статья: \"Расписание\"", url = "https://wiki.goodt.me/x/RAOrCg")
    @Tag("ABCHR2885-2")
    @Tag(SCHED9)
    @TmsLink("60005")
    @Owner(SCHASTLIVAYA)
    public void batchCopyShiftsToTakenCellsSameEmployee() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = orgUnit.getId();
        PresetClass.changeOrSetMathParamValue(omId, MathParameterValues.ACCESS_TO_OVERWORK, true, true);
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        Shift[] sourceShifts = PresetClass.massShiftPresetCheckForEmployee(ep);
        ImmutablePair<LocalDate, LocalDate> sourceDates = new ImmutablePair<>(sourceShifts[0].getStartDate(), sourceShifts[1].getStartDate());
        Shift[] targetShifts = PresetClass.massShiftPresetCheckForEmployee(ep, sourceShifts);
        ImmutablePair<LocalDate, LocalDate> targetDates = new ImmutablePair<>(targetShifts[0].getStartDate(),
                                                                              targetShifts[1].getStartDate());
        ShiftTimePosition timePosition = ShiftTimePosition.DEFAULT;
        copyShifts(omId, ep, ep, sourceDates, targetDates, timePosition);
        assertCopyShift(ep, sourceDates.left, ep, targetDates.left);
        assertCopyShift(ep, sourceDates.right, ep, targetDates.right);
    }

    @Test(groups = {"TEST-130.1", SHIFTS, G1, SCHED9,
            "@Before disable check of worked roster before adding shift",
            "@Before disable start time check for worked shifts", "@Before disable all shift comments"},
            description = "Массовое копирование смен на пустые ячейки другого сотрудника")
    @Link(name = "Статья: \"Расписание\"", url = "https://wiki.goodt.me/x/RAOrCg")
    @Tag("TEST-130.1")
    @Tag(SCHED9)
    @TmsLink("61652")
    @TmsLink("60005")
    @Owner(SCHASTLIVAYA)
    public void batchCopyShiftsToEmptyCellsAnotherEmployee() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = orgUnit.getId();
        ImmutablePair<EmployeePosition, EmployeePosition> twoEmployee = EmployeePositionRepository.getTwoRandomEmployeeWithCheckByApi(omId);
        PresetClass.changeOrSetMathParamValue(omId, MathParameterValues.ACCESS_TO_OVERWORK, true, true);
        EmployeePosition source = twoEmployee.left;
        EmployeePosition target = twoEmployee.right;
        Shift[] shifts = PresetClass.massShiftPresetCheckForEmployee(source);
        ShiftTimePosition timePosition = ShiftTimePosition.DEFAULT;
        ImmutablePair<LocalDate, LocalDate> sourceDates = new ImmutablePair<>(shifts[0].getStartDate(), shifts[1].getStartDate());
        ImmutablePair<LocalDate, LocalDate> targetDates = PresetClass.twoFreeDaysChecker(target, timePosition);
        copyShifts(omId, source, target, sourceDates, targetDates, timePosition);
        assertCopyShift(source, sourceDates.left, target, targetDates.left);
        assertCopyShift(source, sourceDates.right, target, targetDates.right);
    }

    @Test(groups = {"TEST-131.1", SHIFTS, G1, SCHED9,
            "@Before disable check of worked roster before adding shift",
            "@Before disable start time check for worked shifts", "@Before disable all shift comments"},
            description = "Массовый перенос смен на свободные ячейки другого сотрудника")
    @Link(name = "Статья: \"Расписание\"", url = "https://wiki.goodt.me/x/RAOrCg")
    @Tag("TEST-131.1")
    @Tag(SCHED9)
    @TmsLink("61651")
    @TmsLink("60005")
    @Owner(SCHASTLIVAYA)
    public void batchMoveShiftsToEmptyCellsAnotherEmployee() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = orgUnit.getId();
        ImmutablePair<EmployeePosition, EmployeePosition> twoEmployee = EmployeePositionRepository.getTwoRandomEmployeeWithCheckByApi(omId);
        PresetClass.changeOrSetMathParamValue(omId, MathParameterValues.ACCESS_TO_OVERWORK, true, true);
        EmployeePosition source = twoEmployee.left;
        EmployeePosition target = twoEmployee.right;
        Shift[] sourceShifts = PresetClass.massShiftPresetCheckForEmployee(source);
        ShiftTimePosition timePosition = ShiftTimePosition.DEFAULT;
        ImmutablePair<LocalDate, LocalDate> dates = new ImmutablePair<>(sourceShifts[0].getStartDate(), sourceShifts[1].getStartDate());
        PresetClass.makeClearDate(target, dates.left, dates.right);
        transferShifts(omId, source, target, dates, dates, timePosition);
        assertTransferShift(source, target, sourceShifts[0], dates.left);
        assertTransferShift(source, target, sourceShifts[1], dates.right);
    }

    @Test(groups = {"TEST-131.2", SHIFTS, G1, SCHED9,
            "@Before disable check of worked roster before adding shift"},
            description = "Массовый перенос смен на занятые ячейки другого сотрудника")
    @Link(name = "Статья: \"Расписание\"", url = "https://wiki.goodt.me/x/RAOrCg")
    @Tag("TEST-131.2")
    @Tag(SCHED9)
    @TmsLink("61651")
    @TmsLink("60005")
    @Owner(SCHASTLIVAYA)
    public void batchMoveShiftsToTakenCellsAnotherEmployee() {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = orgUnit.getId();
        PresetClass.changeOrSetMathParamValue(omId, MathParameterValues.ACCESS_TO_OVERWORK, true, true);
        ImmutablePair<EmployeePosition, EmployeePosition> twoEmployee = EmployeePositionRepository.getTwoRandomEmployeeWithCheckByApi(omId);
        EmployeePosition source = twoEmployee.left;
        EmployeePosition target = twoEmployee.right;
        Shift[] sourceShifts = PresetClass.massShiftPresetCheckForEmployee(source);
        ShiftTimePosition timePosition = ShiftTimePosition.DEFAULT;
        ImmutablePair<LocalDate, LocalDate> dates = new ImmutablePair<>(sourceShifts[0].getStartDate(), sourceShifts[1].getStartDate());
        Shift[] targetShifts = PresetClass.massShiftPresetAtSameDays(target, new LocalDate[]{dates.left, dates.right});
        transferShifts(omId, source, target, dates, dates, timePosition);
        assertTransferShifts(source, target, sourceShifts[0], targetShifts[0]);
        assertTransferShifts(source, target, sourceShifts[1], targetShifts[1]);
    }

    @Test(groups = {"ABCHR-7560", CONFLICTS, G2, SCHED33, EFES,
            "@Before check if last day of month",
            "@Before activate the conflict indicator",
            "@Before disable calculate conflicts",
            "@Before disable mandatory comment when editing or deleting shift",
            "@Before disable roster single edited version"},
            description = "Отображение конфликта \"У сотрудника превышен лимит сверхурочной работы\"")
    @Link(name = "Статья: \"7560_EFES_РусАгро. Изменение конфликта \"У сотрудника превышен лимит сверхурочной работы\"",
            url = "https://wiki.goodt.me/pages/viewpage.action?pageId=256474985")
    @Owner(MATSKEVICH)
    @TmsLink("60223")
    @Tag("ABCHR-7560")
    @Tag(SCHED33)
    public void conflictOfExceedingOvertimeLimitOnEFES() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts(false, false, false, false, false);
        int unitId = unit.getId();
        PresetClass.changeOrSetMathParamValue(unitId, MathParameterValues.CHECK_VIOLATIONS, true, true);
        PresetClass.enableConflictCalculationInSysList(ConstraintViolations.A_LOT_OF_OVERTIME);
        PresetClass.setPriorityLevelToConstraintViolation(ConstraintViolations.A_LOT_OF_OVERTIME,
                                                          ConstraintViolationLevel.HIGH, false);
        PresetClass.deleteLocalConstraintViolationSetting(unitId, "A_LOT_OF_OVERTIME");
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(unitId, null, true);
        ImmutablePair<LocalDate, LocalDate> dates = PresetClass.twoFreeDaysChecker(ep, ShiftTimePosition.FUTURE,
                                                                                   LocalDateTools.getLastDate());
        LocalDate date = dates.left;
        LocalDateTime firstDayTime = date.atTime(10, 0);
        LocalDateTime secondDayTime = firstDayTime.plusDays(1);
        int rosterId = RosterRepository.getActiveRosterThisMonth(unitId).getId();
        String step = "Создать запрос сверхурочной работы на %d часа на %s";
        Allure.step(String.format(step, 2, date), () -> PresetClass.createOutsidePlanResourceEFES(firstDayTime, ep,
                                                                                                  rosterId, 2));
        Allure.step(String.format(step, 2, secondDayTime.toLocalDate()),
                    () -> PresetClass.createOutsidePlanResourceEFES(secondDayTime, ep, rosterId, 2));
        PresetClass.runConstViolationsCalc(RosterRepository.getActiveRosterThisMonth(unitId).getId());
        Assert.assertThrows(java.lang.AssertionError.class, () ->
                checkConstraintViolationOnEFES(unitId, ep, date, "превышен лимит сверхурочной работы"));
        Allure.step(String.format("Удалить запрос сверхурочной работы на %s", date),
                    () -> PresetClass.makeClearDate(ep, date));
        Allure.step(String.format(step, 3, date), () -> PresetClass.createOutsidePlanResourceEFES(firstDayTime, ep,
                                                                                                  RosterRepository.getActiveRosterThisMonth(unitId).getId(), 3));
        PresetClass.runConstViolationsCalc(RosterRepository.getActiveRosterThisMonth(unitId).getId());
        checkConstraintViolationOnEFES(unitId, ep, date, "превышен лимит сверхурочной работы");
    }

    @Test(groups = {"MAGNIT-17", G1, MAGNIT, SCHED32,
            "@Before show shift hiring reason",
            "@Before disable roster single edited version",
            "@Before check if last day of month"},
            description = "Назначение сотрудника на одну из смен серии повторяющихся свободных смен в расписании с периодичностью - ежедневно")
    @Link(name = "Статья: \"Биржа смен в расписании\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=270096571")
    @TmsLink("99311")
    @Owner(KHOROSHKOV)
    @Tag(SCHED32)
    @Tag("MAGNIT-17")
    public void assignEmployeeToFirstAvailableShift() {
        Periodicity p = Periodicity.DAILY;
        LocalDate date = LocalDate.now();
        RepeatRule rule = new RepeatRule(p).setEndDate(date.plusDays(1));
        ImmutablePair<OrgUnit, EmployeePosition> unitAndEmp = OrgUnitRepository.getRandomOrgUnitWithRandomEmployeeForShiftPair(false);
        OrgUnit unit = unitAndEmp.getLeft();
        EmployeePosition emp = unitAndEmp.getRight().getEmployeePosition();
        int omId = unit.getId();
        DateTimeInterval interval = new DateTimeInterval(date.atTime(8, 0), rule.getEndDate().atTime(21, 0));
        Position pos = emp.getPosition();
        PositionGroup posGroup = PositionGroupRepository.getPositionGroupById(pos.getPositionGroupId());
        PositionType postType = PositionTypeRepository.getPositionTypeById(pos.getPositionTypeId());
        PositionCategory posCat = PositionCategoryRepository.getPositionCategoryById(pos.getPositionCategoryId());
        ShiftHiringReason hiringReason = ShiftHiringReasonRepository
                .getRandomShiftHiringReasonsForJobTitle(omId, JobTitleRepository.getJob(pos.getName()).getId());
        LocalDate startDate = interval.toDateInterval().getStartDate();
        LocalDate endDate = interval.toDateInterval().getEndDate();
        while (!startDate.isAfter(endDate)) {
            PresetClass.removeFreeShifts(omId, startDate);
            startDate = startDate.plusDays(1);
        }
        Roster roster = RosterRepository.getActiveRosterThisMonth(omId);
        PresetClass.makeFreeShift(date, omId, roster, posGroup, posCat, hiringReason, interval, postType, rule);
        PresetClass.makeClearDate(emp, date);
        PresetClass.publishGraphPreset(GraphStatus.PUBLISH, unit);
        List<Shift> freeShiftsBefore = ShiftRepository.getFreeShifts(omId, date);
        List<Shift> shiftsBefore = ShiftRepository.getShifts(emp, ShiftTimePosition.FUTURE);
        Shift freeShift = freeShiftsBefore.stream().findFirst()
                .orElseThrow(() -> new AssertionError("Свободная смена за " + date + " не найдена"));
        new ApiRequest.PostBuilder(makePath(SHIFTS, EXCHANGE, freeShift.getId()))
                .withBody(makeLinksForAssignEmployeeOnFreeShift(emp, freeShift))
                .withStatus(200)
                .send();
        assertAssignEmployeeOnFreeShift(shiftsBefore, freeShiftsBefore, freeShift, emp, omId);
    }

    @Test(groups = {"SE-10", SCHED32, POCHTA, G1,
            "@Before disable shift exchange mobile approve",
            "@Before publish with exceeding norms"},
            description = "Назначение сотрудника мобильной группы на свободную смену")
    @Link(name = "Статья: \"Свободные смены. Биржа смен\"",
            url = "https://wiki.goodt.me/x/2gUtD")
    @TmsLink("60096")
    @Owner(KHOROSHKOV)
    @Tag("SE-10")
    @Tag(SCHED32)
    public void assignMobileGroupEmployeeToFreeShift() {
        changeProperty(SystemProperties.CLIENT_CODE, "R_POST");
        ImmutablePair<OrgUnit, OrgUnit> units = OrgUnitRepository.getTwoOrgUnitsWithSameParent();
        OrgUnit sourceUnit = units.getLeft();
        OrgUnit targetUnit = units.getRight();
        int targetOmId = targetUnit.getId();
        int sourceOmId = sourceUnit.getId();
        EmployeePosition emp = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(sourceOmId, null, true);
        Employee employee = emp.getEmployee();
        String tag = targetUnit.getTags().stream().findAny().orElse("test_tag");
        PresetClass.addTagForOrgUnit(targetUnit, tag);
        PresetClass.addTagForEmployee(employee, tag);
        PresetClass.toggleEmployeeMobileGroupStatus(employee, true);
        Shift sourceShift = PresetClass.defaultShiftPreset(emp, ShiftTimePosition.FUTURE);
        LocalDate date = sourceShift.getStartDate();
        PresetClass.removeFreeShifts(targetOmId, date);
        PositionGroup posGroup = PositionGroupRepository.getPositionGroupById(emp.getPosition().getPositionGroupId());
        PositionCategory posCat = PositionCategoryRepository.getPositionCategoryById(emp.getPosition().getPositionCategoryId());
        Roster roster = RosterRepository.getActiveRosterThisMonth(targetOmId);
        PresetClass.makeFreeShift(date, targetOmId, roster, posGroup, posCat, null, sourceShift.getDateTimeInterval(), null, null);
        PresetClass.publishGraphPreset(GraphStatus.PUBLISH, sourceUnit);
        PresetClass.publishGraphPreset(GraphStatus.PUBLISH, targetUnit);
        String employeeFullName = emp.getEmployee().getFullName();
        Shift freeShift = ShiftRepository.getFreeShifts(targetOmId, date)
                .stream().findFirst().orElseThrow(() -> new AssertionError("Свободная смена за " + date + " не найдена"));
        List<EmployeePosition> employeePositions = EmployeePositionRepository.getAvailableEmployeesForShiftFreeAssignment(freeShift.getId());
        EmployeePosition employeePosition = employeePositions.stream()
                .filter(e -> e.getEmployee().getFullName().equals(employeeFullName))
                .findFirst().orElseThrow(() -> new AssertionError(
                        "Сотрудник " + employeeFullName + " не найден в списке сотрудник доступных на назначение на свободную смену"));
        List<Shift> freeShiftsBefore = ShiftRepository.getFreeShifts(targetOmId, date);
        List<Shift> shiftsBefore = ShiftRepository.getShifts(employeePosition, ShiftTimePosition.FUTURE);
        new ApiRequest.PostBuilder(makePath(SHIFTS, EXCHANGE, freeShift.getId()))
                .withBody(makeLinksForAssignEmployeeOnFreeShift(employeePosition, freeShift))
                .withStatus(200)
                .send();
        assertOnCreatedAppointmentPositionTitle(emp.refreshEmployeePosition(), sourceUnit);
        assertAssignEmployeeOnFreeShiftMobileGroup(shiftsBefore, freeShiftsBefore, freeShift, employeePosition, targetOmId, sourceOmId);
    }

    @Step("Проверить добавление конфликта для сотрудника {ep} за дату {date}")
    private void checkConstraintViolationOnEFES(int omId, EmployeePosition ep, LocalDate date, String text) {
        String name = ep.getEmployee().getFullName();
        List<String> textConstrViolations = CommonRepository.getConstraintViolations(omId, ep, ShiftTimePosition.ALLMONTH);
        Assert.assertNotNull(textConstrViolations);
        Assert.assertTrue(textConstrViolations.stream().anyMatch(t -> t.contains(String.format("У сотрудника %s %s", name, text))
                                  && t.contains(date.toString())
                                  && t.contains(date.plusDays(1).toString())),
                          String.format("Конфликт для сотрудника %s не отразился в API", ep));
        Allure.addAttachment("Конфликт в API", String.format("В API содержатся следующие сообщения о конфликтах %s", textConstrViolations));
    }

    @Ignore("Переведён на ui")
    @Test(groups = {"ABCHR4537-1.1", "ABCHR4537-1", G2, SCHED32,
            "@Before allow free shifts for own employees",
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
        Shift freeShift = PresetClass.makeFreeShift(freeShiftDay, omId, null, posGroup, posCat, reason, null, null, null);
        PresetClass.removeAllTagsFromOrgUnit(unit);
        freeShiftEmployeesCheck(ep, freeShift, displayed);
    }

    @Ignore("Переведён на ui")
    @Test(groups = {"ABCHR4537-1.2", "ABCHR4537-1", G2, SCHED32,
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
        Shift freeShift = PresetClass.makeFreeShift(freeShiftDay, mainOmId, null, posGroup, posCat, reason, null, null, null);
        PresetClass.prepareShifts(ep, freeShiftDay, shiftInPublished, shiftInActive, secondOmId);
        freeShiftEmployeesCheck(ep, freeShift, displayed);
    }

    private List<ImmutablePair<LocalDate, ShiftTimePosition>> searchForThreeSuitableDates(EmployeePosition ep, String dataProviderValue) {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate tomorrow = today.plusDays(1);
        List<ImmutablePair<LocalDate, ShiftTimePosition>> result = new ArrayList<>();
        ImmutablePair<LocalDate, LocalDate> nightShiftDates;
        ImmutablePair<LocalDate, ShiftTimePosition> firstNightShiftDate;
        ImmutablePair<LocalDate, ShiftTimePosition> secondNightShiftDate;
        ImmutablePair<LocalDate, ShiftTimePosition> dayShiftDate;
        ShiftTimePosition planShiftTimePosition = ShiftTimePosition.FUTURE.getShiftsDateInterval().getBetweenDatesList().size() >= 4 ? ShiftTimePosition.FUTURE : ShiftTimePosition.NEXT_MONTH;
        ShiftTimePosition factShiftTimePosition = ShiftTimePosition.PAST.getShiftsDateInterval().getBetweenDatesList().size() >= 4 ? ShiftTimePosition.PAST : ShiftTimePosition.PREVIOUS_MONTH;
        ShiftTimePosition secondPlanShiftTimePosition = ShiftTimePosition.FUTURE;
        switch (dataProviderValue) {
            case "AFTER_PLAN":
                nightShiftDates = PresetClass.twoFreeDaysChecker(ep, planShiftTimePosition, planShiftTimePosition.getShiftsDateInterval().getEndDate());
                dayShiftDate = new ImmutablePair<>(nightShiftDates.right.plusDays(1), planShiftTimePosition);
                firstNightShiftDate = new ImmutablePair<>(nightShiftDates.left, planShiftTimePosition);
                secondNightShiftDate = new ImmutablePair<>(nightShiftDates.right, planShiftTimePosition);
                break;
            case "AFTER_FACT":
                nightShiftDates = PresetClass.twoFreeDaysChecker(ep, factShiftTimePosition, factShiftTimePosition.getShiftsDateInterval().getEndDate());
                dayShiftDate = new ImmutablePair<>(nightShiftDates.right.plusDays(1), factShiftTimePosition);
                firstNightShiftDate = new ImmutablePair<>(nightShiftDates.left, factShiftTimePosition);
                secondNightShiftDate = new ImmutablePair<>(nightShiftDates.right, factShiftTimePosition);
                break;
            case "BEFORE_PLAN":
                nightShiftDates = PresetClass.twoFreeDaysChecker(ep, planShiftTimePosition, planShiftTimePosition.getShiftsDateInterval().getStartDate());
                dayShiftDate = new ImmutablePair<>(nightShiftDates.left.minusDays(1), planShiftTimePosition);
                firstNightShiftDate = new ImmutablePair<>(nightShiftDates.left, planShiftTimePosition);
                secondNightShiftDate = new ImmutablePair<>(nightShiftDates.right, planShiftTimePosition);
                break;
            case "BEFORE_FACT":
                nightShiftDates = PresetClass.twoFreeDaysChecker(ep, factShiftTimePosition, factShiftTimePosition.getShiftsDateInterval().getStartDate());
                dayShiftDate = new ImmutablePair<>(nightShiftDates.left.minusDays(1), factShiftTimePosition);
                firstNightShiftDate = new ImmutablePair<>(nightShiftDates.left, factShiftTimePosition);
                secondNightShiftDate = new ImmutablePair<>(nightShiftDates.right, factShiftTimePosition);
                break;
            case "BETWEEN_PLAN":
                nightShiftDates = PresetClass.twoFreeDaysChecker(ep, planShiftTimePosition, planShiftTimePosition.getShiftsDateInterval().getEndDate());
                dayShiftDate = new ImmutablePair<>(nightShiftDates.right, planShiftTimePosition);
                firstNightShiftDate = new ImmutablePair<>(nightShiftDates.left, planShiftTimePosition);
                secondNightShiftDate = new ImmutablePair<>(dayShiftDate.left.plusDays(1), planShiftTimePosition);
                break;
            case "BETWEEN_FACT":
                nightShiftDates = PresetClass.twoFreeDaysChecker(ep, factShiftTimePosition, factShiftTimePosition.getShiftsDateInterval().getStartDate(), factShiftTimePosition.getShiftsDateInterval().getEndDate());
                dayShiftDate = new ImmutablePair<>(nightShiftDates.right, factShiftTimePosition);
                firstNightShiftDate = new ImmutablePair<>(nightShiftDates.left, factShiftTimePosition);
                secondNightShiftDate = new ImmutablePair<>(dayShiftDate.left.plusDays(1), factShiftTimePosition);
                break;
            case "AFTER_PLAN_FACT":
                factShiftTimePosition = (yesterday.lengthOfMonth() == yesterday.getDayOfMonth()) ? ShiftTimePosition.PREVIOUS_MONTH : ShiftTimePosition.PAST;
                planShiftTimePosition = (tomorrow.getDayOfMonth() == 1) ? ShiftTimePosition.NEXT_MONTH : ShiftTimePosition.FUTURE;
                firstNightShiftDate = new ImmutablePair<>(yesterday, factShiftTimePosition);
                secondNightShiftDate = new ImmutablePair<>(today, secondPlanShiftTimePosition);
                dayShiftDate = new ImmutablePair<>(tomorrow, planShiftTimePosition);
                PresetClass.makeClearDate(ep, firstNightShiftDate.right, firstNightShiftDate.left);
                PresetClass.makeClearDate(ep, secondNightShiftDate.right, secondNightShiftDate.left);
                PresetClass.makeClearDate(ep, dayShiftDate.right, dayShiftDate.left);
                break;
            case "BETWEEN_PLAN_FACT":
                factShiftTimePosition = (yesterday.lengthOfMonth() == yesterday.getDayOfMonth()) ? ShiftTimePosition.PREVIOUS_MONTH : ShiftTimePosition.PAST;
                planShiftTimePosition = (tomorrow.getDayOfMonth() == 1) ? ShiftTimePosition.NEXT_MONTH : ShiftTimePosition.FUTURE;
                firstNightShiftDate = new ImmutablePair<>(yesterday, factShiftTimePosition);
                secondNightShiftDate = new ImmutablePair<>(tomorrow, planShiftTimePosition);
                dayShiftDate = new ImmutablePair<>(today, secondPlanShiftTimePosition);
                PresetClass.makeClearDate(ep, firstNightShiftDate.right, firstNightShiftDate.left);
                PresetClass.makeClearDate(ep, secondNightShiftDate.right, secondNightShiftDate.left);
                PresetClass.makeClearDate(ep, dayShiftDate.right, dayShiftDate.left);
                break;
            default:
                firstNightShiftDate = null;
                secondNightShiftDate = null;
                dayShiftDate = null;
        }
        result.add(firstNightShiftDate);
        result.add(secondNightShiftDate);
        result.add(dayShiftDate);
        Allure.addAttachment("Даты для создания ночных смен", String.format("Были выбраны следующие даты: %s, %s", result.get(0).left, result.get(1).left));
        Allure.addAttachment("Дата для дневной смены", result.get(2).left.toString());
        PresetClass.presetForMakeShiftDate(ep, result.get(0).left, true, result.get(0).right);
        PresetClass.presetForMakeShiftDate(ep, result.get(1).left, true, result.get(1).right);
        return result;
    }

    @Test(groups = {"ABCHR92529", G2, SCHED38, EFES,
            "@Before disable check of worked roster before adding shift",
            "@Before disable roster single edited version",
            "@Before disable all shift comments",
            "@Before allow plan shift editing",
            "@After remove table rule",
            "@Before disable pre-publication checks"},
            dataProvider = "roleAndShiftPosition (create)")
    @Link(name = "Статья: \"92529_ [Расписание] EFES. Дневная смена определяется как третья ночная\"", url = "https://wiki.goodt.me/x/upzGE")
    @TmsLink("107124")
    @Tag(SCHED38)
    public void createDayShiftNearTwoNightShifts(OrgUnit orgUnit, Role role, String shiftPosition, String testName, Integer testId) {
        excludeParametersAndRecalculateHistoryId(Arrays.asList("arg0", "arg1"));
        changeTestName(String.format("Создание дневной смены %s", testName));
        addTag(String.format("ABCHR92529-%d", testId));
        int omId = orgUnit.getId();
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        User user = PresetClass.addCustomRoleToUser(role, RandomStringUtils.randomAlphanumeric(8), LocalDate.now().plusDays(1), orgUnit);
        List<ImmutablePair<LocalDate, ShiftTimePosition>> datesAndShiftPositions = Allure.step("Подобрать три подходящие даты для создания смен",
                                                                                               () -> searchForThreeSuitableDates(ep, shiftPosition));
        ShiftTimePosition dayShiftPosition = datesAndShiftPositions.get(2).right;
        LocalDate dayShiftDate = datesAndShiftPositions.get(2).left;
        if (shiftPosition.contains("FACT") && !shiftPosition.contains("PLAN_FACT")) {
            PresetClass.addTableRuleToOrgUnit(omId, new DateInterval(datesAndShiftPositions.get(2).right.getShiftsDateInterval().getStartDate(), LocalDate.now()).difference() + 1, null, null, TableRuleShiftType.TIMESHEET);
        }
        PresetClass.makeClearDate(ep, dayShiftPosition, dayShiftDate);
        PresetClass.publishGraphPreset(GraphStatus.PUBLISH, orgUnit);
        Shift dayShift = HelperMethods.createShift(omId, ep, datesAndShiftPositions.get(2).left, false, datesAndShiftPositions.get(2).right, user);
        assertDayShiftWithoutSubtype(dayShift);
    }

    @Test(groups = {"ABCHR92529", G2, SCHED38, EFES,
            "@Before disable check of worked roster before adding shift",
            "@Before disable roster single edited version",
            "@Before disable all shift comments",
            "@Before allow plan shift editing",
            "@After remove table rule",
            "@Before disable pre-publication checks"},
            dataProvider = "roleAndShiftPosition (copy or change)")
    @Link(name = "Статья: \"92529_ [Расписание] EFES. Дневная смена определяется как третья ночная\"", url = "https://wiki.goodt.me/x/upzGE")
    @TmsLink("107124")
    @Tag(SCHED38)
    public void copyDayShiftNearTwoNightShifts(OrgUnit orgUnit, Role role, String shiftPosition, String testName, Integer testId) {
        excludeParametersAndRecalculateHistoryId(Arrays.asList("arg0", "arg1"));
        changeTestName(String.format("Создание дневной смены %s", testName));
        addTag(String.format("ABCHR92529-%d", testId));
        String actionNameForStep = testName.contains("копированием") ? "Копировать" : "Переместить";
        String actionNameForRequest = testName.contains("копированием") ? COPY : CHANGE;
        int omId = orgUnit.getId();
        User user = PresetClass.addCustomRoleToUser(role, RandomStringUtils.randomAlphanumeric(8), LocalDate.now().plusDays(1), orgUnit);
        List<EmployeePosition> allEmployees = EmployeePositionRepository.getAllEmployeesWithCheckByApi(omId, null, true);
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeFromList(allEmployees);
        EmployeePosition secondEmployee = ep;
        Shift shiftToCopy;
        List<ImmutablePair<LocalDate, ShiftTimePosition>> datesAndShiftPositions = Allure.step("Подобрать три подходящие даты для создания смен",
                                                                                               () -> searchForThreeSuitableDates(ep, shiftPosition));
        List<LocalDate> exceptionsDates = datesAndShiftPositions.stream().map(item -> item.left).collect(Collectors.toList());
        if (shiftPosition.contains("FACT") && !shiftPosition.contains("PLAN_FACT")) {
            PresetClass.addTableRuleToOrgUnit(omId, new DateInterval(datesAndShiftPositions.get(2).right.getShiftsDateInterval().getStartDate(),
                                                                     LocalDate.now()).difference() + 1, null, null, TableRuleShiftType.TIMESHEET);
        }
        ShiftTimePosition dayShiftPosition = datesAndShiftPositions.get(2).right;
        LocalDate dayShiftDate = datesAndShiftPositions.get(2).left;
        DateInterval shiftTimePositionDates = dayShiftPosition.getShiftsDateInterval();
        LocalDate dateForCopiedShift;
        if (exceptionsDates.size() >= shiftTimePositionDates.getBetweenDatesList().size()) {
            allEmployees.remove(ep);
            secondEmployee = EmployeePositionRepository.getRandomEmployeeFromList(allEmployees);
            dateForCopiedShift = getRandomFromList(shiftTimePositionDates.getBetweenDatesList());
        } else {
            dateForCopiedShift = getRandomFromList(shiftTimePositionDates.subtract(exceptionsDates));
        }
        PresetClass.makeClearDate(secondEmployee, dayShiftPosition, dateForCopiedShift);
        shiftToCopy = PresetClass.presetForMakeShiftDate(secondEmployee, dateForCopiedShift, false, dayShiftPosition);
        PresetClass.makeClearDate(ep, dayShiftPosition, dayShiftDate);
        PresetClass.publishGraphPreset(GraphStatus.PUBLISH, orgUnit);
        Shift finalShiftToCopy = shiftToCopy;
        EmployeePosition finalSecondEmployee = secondEmployee;
        Allure.step(String.format("%s одну смену от %s за %s к сотруднику с именем %s за %s", actionNameForStep, finalSecondEmployee,
                                  shiftToCopy.getStartDate(), ep, dayShiftDate),
                    () -> requestForShiftsInteraction(omId, finalSecondEmployee, ep, finalShiftToCopy.getStartDate(), finalShiftToCopy.getEndDate(),
                                                      dayShiftDate, dayShiftDate, dayShiftPosition, user, actionNameForRequest, 200, null));
        Integer shiftId = ShiftRepository.getShift(ep, dayShiftDate, dayShiftPosition).getId();
        Shift dayShift = new ApiRequest.GetBuilder(makePath(SHIFTS, shiftId)).send().returnPOJO(Shift.class);
        assertDayShiftWithoutSubtype(dayShift);
    }

    @Test(groups = {"ABCHR7911", G2, SCHED12, IN_PROGRESS,
            "@Before disable pre-publication checks",
            "@Before disable publication notifications for managers"},
            description = "Отправка уведомления о публикации графика всем у кого есть права",
            dataProviderClass = DataProviders.class, dataProvider = "true/false")
    @Link(name = "Статья: 7911_Уведомления. Выделить отдельный пермишен на отправку уведомлений при публикации смен.", url = "https://wiki.goodt.me/x/d766Dw")
    @TmsLink("117351")
    @Tag(SCHED12)
    public void sendPublicationNotificationToUsersWithPermissions(boolean hasPermissions) {
        changeTestIDDependingOnParameter(hasPermissions, "ABCHR7911-2", "ABCHR7911-3",
                                         "Уведомления о публикации графика не приходят при выключенных настройках");
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int orgUnitId = orgUnit.getId();
        String orgUnitName = orgUnit.getName();
        Employee employee = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(orgUnitId, null, false).getEmployee();
        User user = employee.getUser();
        if (!user.getRolesIds().isEmpty()) {
            PresetClass.clearUserFromRoles(user.getRolesIds(), user);
        }
        Allure.addAttachment("Выбор сотрудника для проверки", String.format("Был выбран сотрудник %s, работающий в подразделении %s",
                                                                            employee.getFullName(), orgUnitName));
        List<PermissionType> permissions = new ArrayList<>(Arrays.asList(PermissionType.SCHEDULE_VIEW,
                                                                         PermissionType.SCHEDULE_EDIT,
                                                                         PermissionType.SCHEDULE_PUBLISH_SHIFTS,
                                                                         PermissionType.NOTIFY_VIEW));
        if (hasPermissions) {
            permissions.add(PermissionType.NOTIFY_MANAGER);
            permissions.add(PermissionType.NOTIFY_ON_SCHEDULE_PUBLISH_SHIFTS);
        }
        Role role = PresetClass.createCustomPermissionRole(permissions);
        user = PresetClass.addCustomRoleToUser(role, RandomStringUtils.randomAlphanumeric(8), orgUnit,
                                               LocalDate.now().plusDays(1), employee.getUser());
        PresetClass.publishGraphPreset(GraphStatus.NOT_PUBLISH, orgUnit);
        Roster notPublishedRoster = RosterRepository.getActiveRosterThisMonth(orgUnitId);
        String path = makePath(URL_BASE, API_V1, ROSTERS, notPublishedRoster.getId(), PUBLISH);
        Allure.step(String.format("Опубликовать график в подразделении %s", orgUnitName), () -> {
            new ApiRequest.PutBuilder(path)
                    .withParams(Collections.singletonMap("force", "false"))
                    .withBody(notPublishedRoster.setPublished(true).setOnApproval(false))
                    .send();
        });
        String title = String.format("Расписание для %s утверждено", orgUnitName);
        String text = String.format("График работы ОПС «%s», на «%s» %dг. %d опубликован. Необходимо распечатать график, подписать его с работниками.",
                                    orgUnitName,
                                    getServerDate().getMonth().getDisplayName(TextStyle.FULL_STANDALONE, Locale.forLanguageTag("ru")).toLowerCase(),
                                    LocalDate.now().getYear(),
                                    RosterRepository.getActiveRosterThisMonth(orgUnitId).getVersion());
        assertNotificationRecieved(user, title, text, hasPermissions);
    }

    @Test(groups = {"ABCHR8221-1", G2, SCHED9, "@Before disable pre-publication checks"},
            description = "Отображение интервала блокировки из plan.edit.future.days в api/v1/roster-edit-conditions")
    @Link(name = "8221_[Редактирование расписания] Условия редактирования планового графика", url = "https://wiki.goodt.me/x/mkgZE")
    @TmsLink("118294")
    @Tag("ABCHR8221-1")
    @Tag(SCHED9)
    public void showPlannedDisabledIntervalsInApi() {
        changeProperty(SystemProperties.PLAN_EDIT_PAST_DAYS, 0);
        LocalDate today = LocalDate.now();
        LocalDate endBlockDate = ShiftTimePosition.FUTURE.getShiftsDateInterval().getRandomDateBetween();
        changeProperty(SystemProperties.PLAN_EDIT_FUTURE_STRONG, true);
        changeProperty(SystemProperties.PLAN_EDIT_FUTURE_DAYS, endBlockDate.getDayOfMonth() - today.getDayOfMonth());
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        User user = getUserWithPermissions(Arrays.asList(PermissionType.SCHEDULE_VIEW, PermissionType.SCHEDULE_EDIT), orgUnit);
        PresetClass.publishGraphPreset(GraphStatus.PUBLISH, orgUnit);
        Map<String, String> pairs = Pairs.newBuilder()
                .orgUnitId(orgUnit.getId())
                .from(LocalDateTools.getFirstDate())
                .to(LocalDateTools.getLastDate()).buildMap();
        ApiRequest request = new ApiRequest.GetBuilder(makePath(URL_BASE, API_V1, "roster-edit-conditions"))
                .withParams(pairs)
                .withUser(user)
                .send();
        String actualToday = request.returnJsonValue("plannedDisabledIntervals[0].startDate");
        String actualEndBlockDate = request.returnJsonValue("plannedDisabledIntervals[0].endDate");
        Allure.step(String.format("Проверить, что плановый график заблокирован с %s по %s", today, endBlockDate), () -> {
            Assert.assertEquals(actualToday, today.toString(), "Дата начала блокировки планового графика не совпадает с ожидаемой");
            Assert.assertEquals(actualEndBlockDate, endBlockDate.toString(), "Дата окончания блокировки планового графика не совпадает с ожидаемой");
        });
    }

    @Test(groups = {"ABCHR-7018", G2, SCHED9, IN_PROGRESS, EFES,
            "@Before disable typed limits check",
            "@Before disable roster publish without conflicts",
            "@Before disable check of worked roster before adding shift",
            "@Before publish without checking for yearly overtime limit violation"},
            description = "Запись смен через API при опубликованном графике",
            dataProviderClass = DataProviders.class, dataProvider = "true/false")
    @Link(name = "7018_EFES. API для записи в таблицу SHIFT", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=256462115")
    @TmsLink("118291")
    @Tag(SCHED9)
    @Owner(KHOROSHKOV)
    public void recordingShiftsWithGraphs(boolean hasPublishGraphs) {
        changeTestIDDependingOnParameter(hasPublishGraphs, "ABCHR-7018-1", "ABCHR-7018-3",
                                         "Запись смен через API без ростера или без опубликованного графика");
        ImmutablePair<OrgUnit, EmployeePosition> unitAndEmp = hasPublishGraphs ?
                OrgUnitRepository.getRandomOrgUnitWithRandomEmployeeForShiftPair(false) :
                OrgUnitRepository.getRandomOrgUnitWithRandomEmployeeForNotPublishRoster();
        OrgUnit orgUnit = unitAndEmp.getLeft();
        int omId = orgUnit.getId();
        EmployeePosition emp = unitAndEmp.getRight();
        ShiftTimePosition timePosition = ShiftTimePosition.FUTURE;
        LocalDate date = timePosition.getShiftsDateInterval().getRandomDateBetween();
        if (hasPublishGraphs) {
            PresetClass.makeClearDate(emp, date);
            PresetClass.publishGraphPreset(GraphStatus.PUBLISH, orgUnit);
        }
        JSONObject shiftJson = createShiftJsonObject(orgUnit, emp, date, date,
                LocalTime.of(8, 0), LocalTime.of(20, 0));
        shiftJson.put(STATUS, ScheduleRequestStatus.APPROVED);
        shiftJson.put(LUNCH, 60.0);
        shiftJson.put(ADD_WORKS, JSONObject.NULL);
        shiftJson.put(SUBTYPE, "WEEKEND_WORK_PAY");
        JSONArray jsonArray = new JSONArray().put(shiftJson);
        List<Roster> rostersBefore = RosterRepository.getRosters(omId);
        List<Shift> shiftsBefore = null;
        ApiRequest.Builder request = new ApiRequest.PostBuilder(makePath(INTERACTION, SHIFTS, PLANNED))
                .withBody(jsonArray.toString());
        if (!rostersBefore.isEmpty()) {
            shiftsBefore = ShiftRepository.getShifts(emp, timePosition);
        }
        if (hasPublishGraphs) {
            request.withStatus(200).send();
            List<Shift> shiftsAfter = ShiftRepository.getShifts(emp, timePosition);
            Shift shift = shiftsAfter.stream().filter(s -> s.getStartDate().equals(date))
                    .findFirst().orElseThrow(() -> new AssertionError("Смена за " + date + " не найдена"));
            Allure.step("Проверить, что смена добавлена в расписание сотруднику");
            assertPost(shiftsBefore, shiftsAfter, shift);
        } else {
            request.withStatus(400).withMessage("Published or Active roster not found Stop-on-error is false").send();
            List<Roster> rostersAfter = RosterRepository.getRosters(omId);
            assertNotChanged(rostersBefore, rostersAfter);
            if (shiftsBefore != null) {
                List<Shift> shiftsAfter = ShiftRepository.getShifts(emp, ShiftTimePosition.ALLMONTH);
                assertNotChanged(shiftsBefore, shiftsAfter);
            }
        }
    }

    @Test(groups = {"ABCHR-7762-1", G2, SCHED9, EFES,
            "@Before disable typed limits check",
            "@Before disable roster publish without conflicts",
            "@Before disable check of worked roster before adding shift",
            "@Before publish without checking for yearly overtime limit violation"},
            description = "Удаление смены через API",
            dataProviderClass = DataProviders.class, dataProvider = "true/false")
    @Link(name = "7762_EFES. Добавить метод DELETE в АПИ", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=262538383&moved=true")
    @TmsLink("118292")
    @Tag(SCHED9)
    @Owner(KHOROSHKOV)
    public void deletingShift(boolean timeSheet) {
        OrgUnit orgUnit = OrgUnitRepository.getRandomOrgUnitWithWorkedRosters(true, false);
        int omId = orgUnit.getId();
        EmployeePosition emp = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        ShiftTimePosition timePosition = timeSheet ? ShiftTimePosition.PAST : ShiftTimePosition.FUTURE;
        LocalDate date = timePosition.getShiftsDateInterval().getRandomDateBetween();
        String callType = timeSheet ? "WORKSHIFT_DELETE" : "SHIFT_DELETE";
        String graphs = timeSheet ? WORKED : PLANNED;

        Shift shift = PresetClass.presetForMakeShiftDate(emp, date, false, timePosition);
        PresetClass.publishGraphPreset(GraphStatus.PUBLISH, orgUnit);
        JSONObject shiftJson = createShiftJsonObject(orgUnit, emp,
                shift.getStartDate(), shift.getEndDate(), shift.getStartTime(), shift.getEndTime());
        JSONArray jsonArray = new JSONArray().put(shiftJson);

        DateInterval dateInterval = timePosition.getShiftsDateInterval();
        Roster roster = RosterRepository.getNeededRosterId(timePosition, dateInterval, orgUnit.getId());
        List<Shift> shiftsBefore = ShiftRepository.getShiftsForRoster(roster.getId(), dateInterval);

        ApiRequest responseTimesheet = new ApiRequest.PostBuilder(makePath(INTERACTION, SHIFTS, graphs, DELETE))
                .withBody(jsonArray.toString())
                .withStatus(200)
                .send();

        List<Shift> shiftsAfter = ShiftRepository.getShiftsForRoster(roster.getId(), dateInterval);
        Allure.step(String.format("Проверить, что callType в запросах вернулись со статусом %s", callType), () ->
                Assert.assertEquals(callType, responseTimesheet.returnJsonValue(CALL_TYPE)));
        assertDelete(shiftsBefore, shiftsAfter, shift);
    }
}

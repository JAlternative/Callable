package apitest;

import io.qameta.allure.Allure;
import io.qameta.allure.Step;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.asserts.SoftAssert;
import utils.Projects;
import utils.authorization.CsvLoader;
import utils.tools.Pairs;
import wfm.ApiRequest;
import wfm.components.schedule.ScheduleRequestStatus;
import wfm.components.schedule.ShiftTimePosition;
import wfm.components.utils.Role;
import wfm.models.*;
import wfm.repository.CommonRepository;
import wfm.repository.PositionCategoryRepository;
import wfm.repository.PositionGroupRepository;
import wfm.repository.RosterRepository;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.preemptive;
import static utils.Links.*;
import static utils.Params.*;
import static utils.tools.RequestFormers.*;
import static wfm.repository.CommonRepository.getPositionCategoryRosterId;

public class HelperMethods {
    static String type = "application/hal+json";
    static String[] credentials = CsvLoader.loginReturner(Projects.WFM, Role.ADMIN);
    static final RequestSpecification adminRequestSpec = new RequestSpecBuilder()
            .setAccept(type)
            .setContentType(type)
            .setBaseUri(CommonRepository.URL_BASE)
            .setAuth(preemptive().basic(credentials[0], credentials[1]))
            .setBasePath(Projects.WFM.getApi())
            .build()
            .log()
            .all();

    @Step("Проверить, что объект был удален")
    public static <T> void assertDelete(List<T> before, List<T> after, T deletedObject) {
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(after.size(), before.size() - 1, "Размер списка не уменьшился");
        softAssert.assertFalse(after.contains(deletedObject), "Объект не был удален");
        softAssert.assertAll();
        Allure.addAttachment("Проверка удаления объекта", "После теста список объектов уменьшился на один. Удаленный объект исчез из списка.");
    }

    @Step("Проверить, что группа объектов была удалена")
    public static <T> void assertBatchDelete(List<T> before, List<T> after, List<T> deletedObjects) {
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(after.size(), before.size() - deletedObjects.size(), "Были удалены не все элементы");
        deletedObjects.forEach(o -> softAssert.assertFalse(after.contains(o), "Объект не был удален"));
        softAssert.assertAll();
        Allure.addAttachment("Проверка удаления объектов", "После теста список объектов уменьшился на количество удаленных объектов. Удаленные объекты исчезли из списка.");
    }

    @Step("Проверить, что объект был добавлен")
    public static <T> void assertPost(List<T> before, List<T> after, T addedObject) {
        Assert.assertEquals(after.size(), before.size() + 1, "Размер списка не увеличился");
        after.removeAll(before);
        T addedO = after.iterator().next();
        Assert.assertEquals(addedO, addedObject, "Найденный объект не соответствует добавленному в ходе теста");
        Allure.addAttachment("Проверка добавления объекта", String.format("После выполнения теста был добавлен объект %s", addedObject.toString()));
    }

    @Step("Проверить, что объекты были добавлены")
    public static <T> void assertPost(List<T> before, List<T> after, List<T> addedObjects) {
        Assert.assertEquals(after.size(), before.size() + addedObjects.size(), "Размер списка не увеличился");
        after.removeAll(before);
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(after.size(), addedObjects.size(), "Количество добавленных элементов не совпало");
        for (T addedObject : addedObjects) {
            softAssert.assertTrue(after.contains(addedObject), String.format("Объект %s не был добавлен", addedObject));
            Allure.addAttachment("Проверка добавления объекта", String.format("После выполнения теста был добавлен объект %s", addedObject));
        }
        softAssert.assertAll();
    }

    public static <T, valueClass> void assertPut(List<T> before, List<T> after, Map<String, valueClass> changedProperties) {
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(before.size(), after.size(), "Размер списка изменился");
        after.removeAll(before);
        T changedO = after.iterator().next();
        compareFieldValues(softAssert, changedO, changedProperties);
        softAssert.assertAll();
    }

    /**
     * Вариант assertPut для тех случаев, когда изменено поле объекта, не входящее в его equals
     * (т.е. объект будет удален при выполнении after.removeAll(before))
     *
     * @param changedO          Измененный объект. Он должен быть подтянут из апи уже после внесения изменений
     * @param changedProperties Меп с измененными полями и их новыми значениями
     */
    public static <T, valueClass> void assertPut(T changedO, Map<String, valueClass> changedProperties) {
        SoftAssert softAssert = new SoftAssert();
        compareFieldValues(softAssert, changedO, changedProperties);
        softAssert.assertAll();
    }

    @Step("Проверить, что {changedNumber} объектов было изменено")
    public static <T, valueClass> void assertPut(List<T> before, List<T> after, String parentFieldName, Map<String, valueClass> changedProperties, int changedNumber) {
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(before.size(), after.size(), "Размер списка изменился");
        after.removeAll(before);
        softAssert.assertEquals(after.size(), changedNumber, "Изменилось неверное количество элементов: ");
        for (T changedO : after) {
            compareFieldValues(softAssert, changedO, parentFieldName, changedProperties);
        }
        softAssert.assertAll();
    }

    @Step("Проверить, что объект был обновлен")
    private static <T, valueClass> void compareFieldValues(SoftAssert softAssert, T changedO, Map<String, valueClass> changedProperties) {
        for (Map.Entry<String, valueClass> entry : changedProperties.entrySet()) {
            String key = entry.getKey();
            Field field;
            try {
                field = changedO.getClass().getDeclaredField(key);
            } catch (NoSuchFieldException e) {
                throw new RuntimeException(e);
            }
            field.setAccessible(true);
            valueClass value;
            try {
                value = (valueClass) field.get(changedO);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            softAssert.assertEquals(value, entry.getValue(),
                                    String.format("Найденный объект не отражает изменения, внесенные в ходе теста: различаются значения в поле \"%s\"", key));
        }
    }

    @Step("Проверить, что объект был обновлен")
    private static <T, valueClass, finalValue> void compareFieldValues(SoftAssert softAssert, T changedO, String parentFieldName, Map<String, valueClass> changedProperties) {
        for (Map.Entry<String, valueClass> entry : changedProperties.entrySet()) {
            Field field;
            try {
                field = changedO.getClass().getDeclaredField(parentFieldName);
            } catch (NoSuchFieldException e) {
                throw new RuntimeException(e);
            }
            field.setAccessible(true);
            valueClass value;
            finalValue finalExpectedValue;
            finalValue finalActualValue;
            String key = entry.getKey();
            try {
                value = (valueClass) field.get(changedO);
                finalExpectedValue = (finalValue) value.getClass().getMethod(key).invoke(value);
                finalActualValue = (finalValue) entry.getValue().getClass().getMethod(key).invoke(value);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
            softAssert.assertEquals(finalExpectedValue, finalActualValue,
                                    String.format("Найденный объект не отражает изменения, внесенные в ходе теста: различаются значения в поле \"%s\"", parentFieldName));
        }
    }

    @Step("Проверить, что изменений не было")
    public static <T> void assertNotChanged(List<T> before, List<T> after) {
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(before.size(), after.size(), "Размер списка изменился");
        after.removeAll(before);
        softAssert.assertTrue(after.size() == 0, "Объекты в списках различаются");
        softAssert.assertAll();
        Allure.addAttachment("Проверка отсутствия изменений", "После теста список объектов не изменился.");
    }

    /**
     * Добавляет запрос расписания на весь день в соответствии с указанными параметрами
     *
     * @param status статус запроса
     * @param ep     сотрудник, для которого создается запрос
     * @param alias  тип запроса
     * @param date   дата запроса
     * @return объект запроса
     */
    public static ScheduleRequest createScheduleRequest(ScheduleRequestStatus status, EmployeePosition ep, ScheduleRequestAlias alias, LocalDate date) {
        ScheduleRequest request = new ScheduleRequest()
                .setStatus(status.toString())
                .setAlias(alias)
                .setDateTimeInterval(new DateTimeInterval(date.atStartOfDay(),
                                                          date.atTime(23, 59, 59)))
                .setType(alias.getType())
                .setAliasCode(alias.getOuterId())
                .setEmployeeId(ep.getEmployee().getId())
                .setPositionId(ep.getPosition().getId());
        return new ApiRequest.PostBuilder(SCHEDULE_REQUESTS)
                .withBody(request)
                .withParams(Pairs.newBuilder().calculateConstraints(true).buildMap())
                .send()
                .returnCreatedObject();
    }

    @Step("Создать смену для сотрудника {ep} на {date}")
    public static Shift assembleBasicShift(int omId, EmployeePosition ep, LocalDate date, boolean nextDay, ShiftTimePosition timePosition) {
        Shift shift = new Shift();
        LocalDateTime start;
        LocalDateTime end;
        if (nextDay) {
            LocalDate nextDate = date.plusDays(1);
            start = LocalDateTime.of(date, LocalTime.of(22, 0));
            end = LocalDateTime.of(nextDate, LocalTime.of(10, 0));
        } else {
            start = LocalDateTime.of(date, LocalTime.of(10, 0));
            end = LocalDateTime.of(date, LocalTime.of(22, 0));
        }
        DateTimeInterval interval = new DateTimeInterval(start, end);
        if (ep != null) {
            shift.setEmployeePositionId(ep.getId());
        }
        return shift.setId(null)
                .setLinks(null)
                .setDateTimeInterval(interval)
                .setPositionCategoryRosterId(getPositionCategoryRosterId(omId, timePosition))
                .setRosterId(RosterRepository.getNeededRosterId(timePosition, new DateInterval(), omId).getId());
    }

    public static Shift createShift(int omId, EmployeePosition ep, LocalDate date, boolean nextDay, ShiftTimePosition timePosition, User user) {
        Shift shift = assembleBasicShift(omId, ep, date, nextDay, timePosition);
        return new ApiRequest.PostBuilder(SHIFTS)
                .withBody(shift)
                .withParams(Pairs.newBuilder().calculateConstraints(true).buildMap())
                .withUser(user)
                .send()
                .returnCreatedObject();
    }

    @Step("Создать свободную смену, повторяющуюся с периодичностью \"{rule.name}\" в период с {date} по {rule.endDate}")
    public static List<Shift> createRepeatingFreeShift(int omId, LocalDate date, boolean nextDay, RepeatRule rule, ShiftTimePosition timePosition, User user) {
        Shift shift = assembleBasicShift(omId, null, date, nextDay, timePosition);
        String path = "shift-exchange-rule";
        JSONObject links = new JSONObject();
        PositionGroup posGroup = PositionGroupRepository.randomPositionGroup();
        PositionCategory posCat = PositionCategoryRepository.randomDynamicPositionCategory();
        links.put(REL_POSITION_GROUP, posGroup.getLinkWrappedInJson(SELF));
        links.put(SELF, new JSONObject().put(HREF, setUri(Projects.WFM, CommonRepository.URL_BASE, path)));
        links.put(ROSTER, RosterRepository.getActiveRosterThisMonth(omId).getLinkWrappedInJson(SELF));
        shift.setPositionCategory(posCat)
                .setPositionGroup(posGroup)
                .setRepeatRule(rule)
                .setLinks(links);
        int shiftExchangeRuleId = new ApiRequest.PostBuilder(path)
                .withBody(shift)
                .withParams(Pairs.newBuilder().calculateConstraints(false).buildMap())
                .withUser(user)
                .send()
                .returnCreatedObjectId();
        return new ApiRequest.GetBuilder(makePath(path, shiftExchangeRuleId, SHIFTS))
                .send()
                .returnList(Shift.class, makeJsonPath(EMBEDDED, SHIFTS));
    }

    public static Shift createOutStaffShift(int omId, LocalDate date, boolean nextDay, ShiftTimePosition timePosition,
                                            PositionGroup posGroup, PositionCategory posCat, User user) {
        Shift shift = assembleBasicShift(omId, null, date, nextDay, timePosition)
                .setPositionGroup(posGroup.setLinks(null))
                .setPositionCategory(posCat.setLinks(null))
                .setOutstaff(true);
        return new ApiRequest.PostBuilder(SHIFTS)
                .withBody(shift)
                .withParams(Pairs.newBuilder().calculateConstraints(true).buildMap())
                .withUser(user)
                .send()
                .returnCreatedObject();
    }

    @Step("Проверить, что время публикации/отправки на утверждение было обновлено в ходе теста")
    public static void assertTime(LocalDateTime actualTime, OrgUnit unit, ZonedDateTime now) {
        ZonedDateTime actualZonedTime;
        try {
            actualZonedTime = actualTime.atZone(unit.getTimeZone());
        } catch (NullPointerException e) {
            actualZonedTime = actualTime.atZone(ZoneId.of("UTC"));
        }
        long differentTimes = Math.abs(ChronoUnit.MINUTES.between(actualZonedTime, now));
        Assert.assertTrue(differentTimes < 3, "Время не совпадает больше, чем на 2 минуты, " +
                "локальное время: " + now + ", время графика на сервере: " + actualZonedTime);
    }

    public static Map<String, Object> getMapForShiftActions(EmployeePosition sourceEp, EmployeePosition targetEp,
                                                            LocalDate sourceFrom, LocalDate sourceTo,
                                                            LocalDate targetFrom, LocalDate targetTo, boolean table) {
        Map<String, Object> result = new HashMap<>();
        result.put(EMPLOYEE_POSITION_ID + "1", sourceEp.getId());
        result.put(EMPLOYEE_POSITION_ID + "2", targetEp.getId());
        result.put("srcFrom", sourceFrom);
        result.put("srcTo", sourceTo);
        result.put("targetFrom", targetFrom);
        result.put("targetTo", targetTo);
        result.put("calculateConstraints", true);
        result.put("table", table);
        result.put("comment", null);
        return result;
    }
}

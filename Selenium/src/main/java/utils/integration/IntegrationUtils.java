package utils.integration;

import org.apache.commons.text.CaseUtils;
import wfm.PresetClass;
import wfm.components.schedule.ScheduleRequestStatus;
import wfm.components.schedule.ScheduleRequestType;
import wfm.components.schedule.ShiftTimePosition;
import wfm.models.*;
import wfm.repository.ScheduleRequestRepository;
import wfm.repository.ShiftRepository;

import java.lang.reflect.Field;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class IntegrationUtils {

    /**
     * Создает либо смену, либо запрос отсутствия в зависимости от переданного класса
     *
     * @param tClass       что создаем: смену или отсутствие
     * @param ep           позиция, для которой создаем запрос
     * @param timePosition когда создаем: в прошлом или в будущем
     * @param unit         подразделение сотрудника
     * @return созданный запрос
     */
    public static Object createScheduleRequestDependingOnClass(Class tClass, EmployeePosition ep, ShiftTimePosition timePosition, OrgUnit unit) {
        if (Shift.class.equals(tClass)) {
            return PresetClass.presetForMakeShift(ep, false, timePosition);
        } else {
            LocalDate date = timePosition.getShiftsDateInterval().getRandomDateBetween();
            PresetClass.makeClearDate(ep, date);
            return PresetClass.createScheduleRequestForDate(ScheduleRequestStatus.APPROVED, date, ep, ScheduleRequestType.SICK_LEAVE);
        }
    }

    /**
     * Получает значение поля объекта
     *
     * @param object объект, у которого ищем значение поля
     * @param field  название поля
     * @param <T>    класс объекта
     * @param <R>    класс поля
     * @return значение поля
     */
    public static <T, R> R getFieldValue(T object, String field) {
        Field objectId;
        try {
            objectId = object.getClass().getDeclaredField(field);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        objectId.setAccessible(true);
        try {
            return (R) objectId.get(object);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Задает значение поля объекта
     *
     * @param object объект, которому значение поля
     * @param field  название поля
     * @param value новое значение поля
     */
    public static void setFieldValue(Object object, String field, Object value) {
        Field objectId;
        try {
            objectId = object.getClass().getDeclaredField(field);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        objectId.setAccessible(true);
        try {
            objectId.set(object, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Конвертирует Timestamp из БД в LocalDateTime
     */
    public static LocalDateTime timestampConverter(Object timestamp) {
        Timestamp dateTime = (Timestamp) timestamp;
        return LocalDateTime.of(new Date(dateTime.getTime()).toLocalDate(),
                                new Time(dateTime.getTime()).toLocalTime());
    }

    /**
     * Конвертирует тип запроса из БД в название класса
     */
    public static String convertDBTypeToClassName(Object result) {
        return CaseUtils.toCamelCase(String.valueOf(result), true, '_');
    }

    /**
     * Конвертирует название класса в тип запроса из БД
     */
    public static String convertClassNameToDBType(Class tClass) {
        return tClass.getSimpleName().replaceAll("([A-Z]+)([A-Z][a-z])", "$1_$2")
                .replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase();
    }

    /**
     * Конвертирует название класса в строку, которую можно подставить в ссылку для запроса в апи
     */
    public static String convertClassNameToLinkPart(Class tClass) {
        return tClass.getSimpleName()
                .replaceAll("([A-Z]+)([A-Z][a-z])", "$1-$2")
                .replaceAll("([a-z])([A-Z])", "$1-$2")
                .replaceAll("^(.*)$", "$1s")
                .toLowerCase();
    }

    /**
     * Достает запрос расписания по его айди и классу. Для запросов отсутствия нужна еще дата
     */
    public static Object fetchScheduleRequestDependingOnClass(Class tClass, EmployeePosition ep, long id, LocalDate date) {
        if (Shift.class.equals(tClass)) {
            return ShiftRepository.getShiftById(Integer.parseInt(String.valueOf(id)));
        } else {
            List<ScheduleRequest> requests = ScheduleRequestRepository.getEmployeeSelfScheduleRequests(ep.getEmployee().getId(), new DateInterval(date));
            if (requests.isEmpty()) {
                throw new AssertionError("У сотрудника нет запросов в WFM");
            } else {
                return requests.get(0);
            }
        }
    }
}

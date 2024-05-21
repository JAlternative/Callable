package wfm.repository;

import org.apache.http.NameValuePair;
import org.json.JSONException;
import org.json.JSONObject;
import utils.Projects;
import utils.tools.Pairs;
import wfm.components.schedule.ScheduleRequestType;
import wfm.models.DateInterval;
import wfm.models.ScheduleRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static utils.Links.EMPLOYEES;
import static utils.Links.SCHEDULE_REQUESTS;
import static utils.tools.CustomTools.getListFromJsonObject;
import static utils.tools.RequestFormers.getJsonFromUri;
import static utils.tools.RequestFormers.makePath;

/**
 * @author Evgeny Gurkin 19.08.2020
 */
public class ScheduleRequestRepository {

    private ScheduleRequestRepository() {}

    /**
     * Берет массив запросов в апи за выбранные даты для выбранного оргюнита
     *
     * @param omNumber - номер оргюнита
     * @return - массив запросов JSON
     */
    public static List<ScheduleRequest> getScheduleRequests(int omNumber, DateInterval dateInterval) {
        List<NameValuePair> nameValuePairs = Pairs.newBuilder()
                .orgUnitIds(omNumber)
                .from(dateInterval.startDate)
                .to(dateInterval.endDate.minusDays(1))
                .calculated(false)
                .build();
        JSONObject someObject = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, SCHEDULE_REQUESTS, nameValuePairs);
        return getListFromJsonObject(someObject, ScheduleRequest.class);
    }

    /**
     * Возвращает список запросов у сотрудников за указанный временной интервал
     *
     * @param employeeId   айди сотрудника
     * @param dateInterval временной интервал для поиска запросов
     * @param omId         айди подразделения
     */
    public static List<ScheduleRequest> getEmployeeScheduleRequests(int employeeId, DateInterval dateInterval, int omId) {
        List<NameValuePair> nameValuePairs = Pairs.newBuilder()
                .calculated(false)
                .from(dateInterval.startDate)
                .orgUnitIds(omId)
                .to(dateInterval.endDate)
                .build();
        JSONObject someObject = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, makePath(SCHEDULE_REQUESTS), nameValuePairs);
        return getListFromJsonObject(someObject, ScheduleRequest.class)
                .stream()
                .filter(e -> e.getEmployee().getId() == employeeId)
                .collect(Collectors.toList());
    }

    /**
     * Возвращает список запросов определенного типа у сотрудника за указанный временной интервал
     *
     * @param employeeId   айди сотрудника
     * @param dateInterval временной интервал для поиска запросов
     * @param omId         айди подразделения
     * @param type         искомый тип запроса
     */
    public static List<ScheduleRequest> getEmployeeScheduleRequestsByType(int employeeId, DateInterval dateInterval, int omId, ScheduleRequestType type) {
        return getEmployeeScheduleRequests(employeeId, dateInterval, omId)
                .stream()
                .filter(r -> r.getType().equals(type))
                .collect(Collectors.toList());
    }

    /**
     * Возвращает список запросов для оргюнита за текущий месяц
     *
     * @param omId - айди оргюнита
     */
    public static List<ScheduleRequest> getScheduleRequestsThisMonth(int omId) {
        return getScheduleRequests(omId, new DateInterval());
    }

    /**
     * берем запросы для сотрудника в указанном диапазоне
     *
     * @param employeeId   - айди сотрудника
     * @param dateInterval - временной интвервал для поиска
     */
    public static List<ScheduleRequest> getEmployeeSelfScheduleRequests(int employeeId, DateInterval dateInterval) {
        List<NameValuePair> nameValuePairs = Pairs.newBuilder()
                .from(dateInterval.startDate)
                .to(dateInterval.endDate)
                .excludeOther(true)
                .build();
        JSONObject someObject = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, makePath(EMPLOYEES, employeeId, SCHEDULE_REQUESTS), nameValuePairs);
        return getListFromJsonObject(someObject, ScheduleRequest.class);
    }

    /**
     * Возвращает список длинных запросов расписания в орг юните (более 40 часов)
     *
     * @param omId
     * @return
     */
    public static List<ScheduleRequest> getLongScheduleRequests(int omId) {
        try {
            List<ScheduleRequest> scheduleRequests = ScheduleRequestRepository.getScheduleRequests(omId, new DateInterval());
            return scheduleRequests.stream()
                    .filter(request -> request.getDateTimeInterval().getLengthInHours() > 40)
                    .collect(Collectors.toList());
        } catch (JSONException e) {
            return new ArrayList<>();
        }
    }
}

package wfm.repository;

import com.mchange.util.AssertException;
import org.apache.http.Consts;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.Projects;
import utils.tools.CustomTools;
import utils.tools.LocalDateTools;
import utils.tools.Pairs;
import wfm.models.DeviationFromStandard;
import wfm.models.EmployeePosition;
import wfm.models.Roster;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static utils.ErrorMessagesForReport.NO_TEST_DATA;
import static utils.Links.DEVIATION_FROM_STANDARD;
import static utils.Links.ROSTERS;
import static utils.Params.*;
import static utils.tools.RequestFormers.*;
import static wfm.repository.CommonRepository.URL_BASE;

public class DeviationFromStandardRepository {
    private static final Logger LOG = LoggerFactory.getLogger(DeviationFromStandardRepository.class);

    /**
     * Возвращает список DeviationFromStandard - норма и фактическое время для каждого сотрудника
     *
     * @param activeRoster - активный ростер
     * @param workedRoster - нулевой ростер
     */
    public static List<DeviationFromStandard> getDeviationFromStandard(Roster activeRoster, Roster workedRoster) {
        LocalDate firstDate = LocalDateTools.getFirstDate();
        String path = makePath(DEVIATION_FROM_STANDARD);
        URI uri = setUri(Projects.WFM, URL_BASE, path);
        Map<String, JSONObject> jsonMap = new HashMap<>();
        LocalDate now;
        if (workedRoster != null) {
            now = LocalDate.now();
            JSONObject dateTimeIntervalJson = new JSONObject();
            if (LocalDate.now().equals(firstDate)) {
                dateTimeIntervalJson.put(START_DATE, now);
                dateTimeIntervalJson.put(END_DATE, now);
            } else {
                dateTimeIntervalJson.put(START_DATE, firstDate);
                dateTimeIntervalJson.put(END_DATE, now.minus(1, ChronoUnit.DAYS));
            }
            jsonMap.put(Integer.toString(workedRoster.getId()), dateTimeIntervalJson);
        } else {
            now = firstDate;
        }
        JSONObject dateTimeIntervalJson = new JSONObject();
        dateTimeIntervalJson.put(START_DATE, now);
        dateTimeIntervalJson.put(END_DATE, LocalDateTools.getLastDate());
        jsonMap.put(Integer.toString(activeRoster.getId()), dateTimeIntervalJson);
        JSONObject object = new JSONObject(jsonMap);
        LOG.info("Отправлен JSON: {}", object);
        HttpResponse response = requestMaker(uri, object, RequestBuilder.post(), ContentType.create("application/hal+json", Consts.UTF_8));
        JSONObject deviationObject;
        List<DeviationFromStandard> deviationList = null;
        try {
            JSONObject jsonObject = new JSONObject(EntityUtils.toString(response.getEntity()));
            deviationObject = jsonObject.getJSONObject("_embedded");
            deviationList = CustomTools.getListFromJsonObject(deviationObject, DeviationFromStandard.class);
        } catch (IOException e) {
            LOG.error("Не выполнился запрос", e);
        }
        return deviationList;
    }

    /**
     * Возвращает объект DeviationFromStandard
     *
     * @param rosterId  - айди ростера
     * @param startDate - дата, с которой начинается расчет deviation
     * @param endDate   - дата, на которой заканчивается расчет нормы deviation
     */
    public static List<DeviationFromStandard> getDeviationFromStandard(int rosterId, LocalDate startDate, LocalDate endDate) {
        String urlEnding = makePath(ROSTERS, rosterId, DEVIATION_FROM_STANDARD);
        List<NameValuePair> nameValuePairs = Pairs.newBuilder()
                .from(startDate)
                .to(endDate)
                .build();
        JSONObject json = getJsonFromUri(Projects.WFM, URL_BASE, urlEnding, nameValuePairs);
        JSONArray array = json.getJSONObject(EMBEDDED).getJSONArray("deviationFromStandard");
        return CustomTools.getListFromJsonArray(array, DeviationFromStandard.class);
    }

    /**
     * Возвращает объект DeviationFromStandard для сотрудника
     *
     * @param rosterId  - айди ростера
     * @param startDate - дата, с которой начинается расчет deviation
     * @param endDate   - дата, на которой заканчивается расчет deviation
     * @param position  - позиция сотрудника
     */
    public static DeviationFromStandard getDeviation(int rosterId, LocalDate startDate, LocalDate endDate, EmployeePosition position) {
        List<DeviationFromStandard> deviationList = getDeviationFromStandard(rosterId, startDate, endDate);
        LOG.info("DeviationList: {}", deviationList);
        LOG.info("PositionId: {}", position.getId());
        return deviationList.stream()
                .filter(dev -> dev.getEmployeePositionId() == position.getId())
                .findAny()
                .orElseThrow(() -> new AssertException(NO_TEST_DATA + "Не нашли в апи сотрудника с employeePositionId: " + position.getId()));
    }

    /**
     * Возвращает норму и фактическое время для сотрудника
     *
     * @param activeRoster - ростер
     * @param workedRoster - ростер
     * @param position     - позиция сотрудника
     */

    public static DeviationFromStandard getDeviation(Roster activeRoster, Roster workedRoster, EmployeePosition position) {
        List<DeviationFromStandard> deviationList = getDeviationFromStandard(activeRoster, workedRoster);
        LOG.info("DeviationList: {}", deviationList);
        LOG.info("PositionId: {}", position.getId());
        if (deviationList == null) {
            return null;
        }
        return deviationList.stream()
                .filter(dev -> dev.getEmployeePositionId() == position.getId())
                .findAny()
                .orElseThrow(() -> new AssertException(NO_TEST_DATA + "Не нашли в апи сотрудника с employeePositionId: " + position.getId()));
    }

    /**
     * Возвращает норму, округленную до одного знака после запятой
     *
     * @param rosterId  - айди ростера
     * @param startDate - дата, с которой начинается расчет нормы
     * @param endDate   - дата, на которой заканчивается расчет нормы
     * @param position  - позиция сотрудника
     */
    public static double getRoundedStandardDeviation(int rosterId, LocalDate startDate, LocalDate endDate, EmployeePosition position) {
        double rounded = (double) Math.round(getDeviation(rosterId, startDate, endDate, position).getStandard() * 10) / 10;
        LOG.info("Норма сотрудника {}: {}", position, rounded);
        return rounded;
    }

}

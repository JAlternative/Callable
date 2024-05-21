package wfm.repository;

import com.mchange.util.AssertException;
import org.apache.http.NameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;
import utils.Projects;
import utils.tools.Pairs;
import wfm.models.DateInterval;
import wfm.models.FteOperationValuesModel;
import wfm.models.KpiList;
import wfm.models.OrgUnit;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static utils.ErrorMessagesForReport.NO_TEST_DATA;
import static utils.Links.FTE_OPERATION_VALUES;
import static utils.Links.ORG_UNITS;
import static utils.Params.EMBEDDED;
import static utils.tools.LocalDateTools.getFirstDate;
import static utils.tools.LocalDateTools.getLastDate;
import static utils.tools.RequestFormers.getJsonFromUri;
import static utils.tools.RequestFormers.makePath;

/**
 * @author Evgeny Gurkin 20.08.2020
 */
public class FteOperationValuesRepository {

    private FteOperationValuesRepository() {}

    /**
     * Задает названия типов эвентов и названия кпи для модели результата расчета рабочей нагрузки
     *
     * @param fteOperationValuesModel - модель
     * @param eventTypeNames          - имена типов эвентов
     * @param kpiList                 - KPI параметры
     * @return - обновленная модель
     */
    public static FteOperationValuesModel setEventNameAndKpi(FteOperationValuesModel fteOperationValuesModel,
                                                             List<String> eventTypeNames, List<KpiList> kpiList) {
        List<String> eventAndKpi = fteOperationValuesModel.getEventAndKpi();
        List<String> events = eventAndKpi.stream().filter(eventTypeNames::contains)
                .collect(Collectors.toList());
        fteOperationValuesModel.setEventNames(events);
        List<KpiList> kpiValues = kpiList.stream().filter(kpi -> eventAndKpi.contains(kpi.getName())).collect(Collectors.toList());
        fteOperationValuesModel.setKpiNames(kpiValues);
        return fteOperationValuesModel;
    }

    /**
     * метод берет модель FteOperationValuesModel по указанному оргюниту и параметрам и сразу же сеттит его значения
     */
    public static FteOperationValuesModel getAndSetFteOperationValues(OrgUnit orgUnit, List<NameValuePair> pairs) {
        List<String> eventTypeNames = new ArrayList<>(CommonRepository.getEventTypes().values());
        List<KpiList> kpiValues = KpiListRepository.getKpiTypes();
        FteOperationValuesModel model = getFteOperationValuesWithPairs(orgUnit, pairs);
        return setEventNameAndKpi(model, eventTypeNames, kpiValues);
    }

    /**
     * для указанного оргюнита берет результат расчета рабочей нагрузки за последний год
     */
    public static FteOperationValuesModel checkGroupFteLastYear(OrgUnit orgUnit) {
        LocalDate start = getFirstDate().minusYears(1);
        LocalDate end = getLastDate();
        List<NameValuePair> pairs = Pairs.newBuilder().from(start).to(end).build();
        return getFteOperationValuesWithPairs(orgUnit, pairs);
    }

    /**
     * для указанного оргюнита берет результат расчета рабочей нагрузки с учетом query параметров
     */
    private static FteOperationValuesModel getFteOperationValuesWithPairs(OrgUnit orgUnit, List<NameValuePair> pairs) {
        String path = makePath(ORG_UNITS, orgUnit.getId(), FTE_OPERATION_VALUES);
        JSONObject jsonObject = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, path, pairs);
        JSONArray fteOperationValue = jsonObject.getJSONObject(EMBEDDED).getJSONArray("fteOperationValueReportList");
        return new FteOperationValuesModel(fteOperationValue);
    }

    /**
     * Метод составляет query парамтры для запроса в fte-operation-values
     */
    public static List<NameValuePair> getPairsFormParamNames(DateInterval dateInterval, String eventType, KpiList kpiList, String roleName) {
        Pairs.Builder pairs = Pairs.newBuilder()
                .from(dateInterval.startDate)
                .to(dateInterval.endDate);
        if (eventType != null) {
            Map<Integer, String> eventTypes = CommonRepository.getEventTypes();
            int eventId = eventTypes.keySet().stream().filter(integer -> eventTypes.get(integer).equals(eventType)).findAny()
                    .orElseThrow(() -> new AssertException(NO_TEST_DATA + "В апи не нашли евент:" + eventType));
            pairs.eventIds(eventId);
        }
        if (kpiList != null) {
            pairs.kpiIds(kpiList.getKpiId());
        }
        if (roleName != null) {
            Map<Integer, String> positionGroups = CommonRepository.getPositionGroups();
            int groupId = positionGroups.keySet().stream().filter(integer -> positionGroups.get(integer).equals(roleName)).findAny()
                    .orElseThrow(() -> new AssertException(NO_TEST_DATA + "В апи не нашли группу позиций:" + roleName));
            pairs.positionGroupIds(groupId);
        }
        return pairs.build();
    }
}

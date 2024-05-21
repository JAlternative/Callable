package wfm.repository;

import org.json.JSONObject;
import utils.Projects;
import wfm.components.analytics.KpiType;
import wfm.models.KpiList;

import java.util.List;

import static utils.ErrorMessagesForReport.NO_TEST_DATA;
import static utils.Links.KPI_LIST;
import static utils.tools.CustomTools.getListFromJsonObject;
import static utils.tools.RequestFormers.getJsonFromUri;

/**
 * @author Evgeny Gurkin 19.08.2020
 */
public class KpiListRepository {

    private KpiListRepository() {
    }

    /**
     * возвращает списко типов KPI
     */
    public static List<KpiList> getKpiTypes() {
        JSONObject jsonObject = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, KPI_LIST);
        return getListFromJsonObject(jsonObject, KpiList.class);
    }

    /**
     * берет список видимых KPI объектов
     */
    public static List<KpiList> getAllVisibleKpiTypes() {
        JSONObject array = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, KPI_LIST);
        List<KpiList> kpiList = getListFromJsonObject(array, KpiList.class);
        List<String> visibleIds = CommonRepository.getVisibleKpiOuterIds();
        kpiList.removeIf(k -> !visibleIds.contains(k.getOuterId()));
        return kpiList;
    }

    /**
     * Берет KPI по айди
     */
    public static KpiList getKpiId(int id) {
        return getAllVisibleKpiTypes().stream().filter(kpiList -> kpiList.getKpiId() == id).findAny()
                .orElseThrow(() -> new AssertionError(NO_TEST_DATA + "kpi с id " + id + "не был найден в Api"));
    }

    public static KpiList getKpiByType(KpiType type) {
        return getAllVisibleKpiTypes().stream().filter(kpiList -> kpiList.getKpiType().equals(type)).findAny()
                .orElseThrow(() -> new AssertionError(String.format("%s kpi с типом \"%s\" не был найден в api", NO_TEST_DATA, type.getType())));
    }
}

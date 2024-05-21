package wfm.repository;

import org.json.JSONArray;
import org.json.JSONObject;
import utils.Projects;
import wfm.PresetClass;
import wfm.models.AdditionalWork;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static utils.ErrorMessagesForReport.NO_TEST_DATA;
import static utils.Links.SHIFTS_ADD_WORK;
import static utils.Params.EMBEDDED;
import static utils.tools.CustomTools.getListFromJsonArray;
import static utils.tools.RequestFormers.getJsonFromUri;
import static utils.tools.RequestFormers.setUri;

public class AdditionalWorkRepository {

    private AdditionalWorkRepository() {
    }

    /**
     * Возвращает все доп работы (на фронте Системные списки -> Типы доп. работ)
     */
    public static List<AdditionalWork> getAdditionalWorks() {
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, SHIFTS_ADD_WORK);
        JSONObject object = getJsonFromUri(Projects.WFM, uri);
        if (object.length() == 0) {
            return new ArrayList<>();
        }
        JSONArray array = object.getJSONObject(EMBEDDED).getJSONArray("shiftAddWorkResList");
        return getListFromJsonArray(array, AdditionalWork.class);
    }

    public static AdditionalWork getAdditionalWorkByOuterId(String outerId) {
        return getAdditionalWorks()
                .stream()
                .filter(w -> w.getOuterId().equals(outerId))
                .findFirst()
                .orElseThrow(() -> new AssertionError(String.format("%sНе найдена доп. работа с outerId= %s", NO_TEST_DATA, outerId)));
    }

    public static AdditionalWork getAdditionalWorkById(int id) {
        return getAdditionalWorks()
                .stream()
                .filter(w -> w.getId() == id)
                .findFirst()
                .orElseThrow(() -> new AssertionError(String.format("%sНе найдена доп. работа с Id= %s", NO_TEST_DATA, id)));
    }

    public static AdditionalWork getRandomAdditionalWorkWithStatuses() {
        return getAdditionalWorks()
                .stream()
                .filter(AdditionalWork::isHasStatuses)
                .filter(a -> !a.isDisabled())
                .findAny()
                .orElse(null);
    }

    public static AdditionalWork getRandomAdditionalWorkWithoutStatuses() {
        return getAdditionalWorks()
                .stream()
                .filter(additionalWork -> !additionalWork.isHasStatuses())
                .filter(a -> !a.isDisabled())
                .findAny()
                .orElse(null);
    }

    public static AdditionalWork getTestAddWork(boolean status, String addWorkName) {
        return getAdditionalWorks()
                .stream()
                .filter(additionalWork -> additionalWork.getTitle().equals(addWorkName))
                .findAny()
                .orElseGet(() -> PresetClass.addAdditionalWork(status, addWorkName));
    }
}

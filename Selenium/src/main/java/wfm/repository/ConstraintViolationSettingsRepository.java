package wfm.repository;

import org.apache.http.NameValuePair;
import org.json.JSONObject;
import utils.Projects;
import utils.tools.Pairs;
import wfm.models.ConstraintViolationSettings;

import java.util.List;

import static utils.ErrorMessagesForReport.NO_TEST_DATA;
import static utils.Links.*;
import static utils.tools.CustomTools.getListFromJsonObject;
import static utils.tools.RequestFormers.*;

public class ConstraintViolationSettingsRepository {

    /**
     * Возвращает список настроек конфликтов из системного списка "Установки конфликтов"
     */
    public static List<ConstraintViolationSettings> getConstraintViolationSettings() {
        JSONObject settings = getJsonFromUri(Projects.WFM, setUri(Projects.WFM, CommonRepository.URL_BASE, CONSTRAINT_VIOLATIONS_GLOBAL_SETTINGS));
        return getListFromJsonObject(settings, ConstraintViolationSettings.class);
    }

    /**
     * Возвращает список настроек конфликтов у орг юнита
     */
    public static List<ConstraintViolationSettings> getConstraintViolationSettingsByOrgUnit(int omId) {
        List<NameValuePair> pairs = Pairs.newBuilder()
                .orgUnitId(omId)
                .build();
        JSONObject settings = getJsonFromUri(Projects.WFM, setUri(Projects.WFM, CommonRepository.URL_BASE, CONSTRAINT_VIOLATIONS_SETTINGS, pairs));
        return getListFromJsonObject(settings, ConstraintViolationSettings.class);
    }

    /**
     * Возвращает объект ConstraintViolation
     *
     * @param type строковый идентификатор конфликта
     */
    public static ConstraintViolationSettings getConstraintViolationByType(List<ConstraintViolationSettings> constrViolationList, String type) {
        return constrViolationList.stream()
                .filter(constr -> constr.getType().equals(type))
                .findFirst()
                .orElseThrow(() -> new AssertionError(String.format("%sКонфликт с указанным типом %s не найден", NO_TEST_DATA, type)));
    }
}

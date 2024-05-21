package wfm.repository;

import io.qameta.allure.Allure;
import org.apache.http.NameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;
import utils.Projects;
import utils.tools.Pairs;
import wfm.PresetClass;
import wfm.models.ShiftHiringReason;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static utils.ErrorMessagesForReport.NO_TEST_DATA;
import static utils.Links.*;
import static utils.Params.EMBEDDED;
import static utils.Params.SHIFT_HIRING_REASON_LIST;
import static utils.tools.CustomTools.getListFromJsonArray;
import static utils.tools.RequestFormers.*;

public class ShiftHiringReasonRepository {
    private static final Random RANDOM = new Random();

    private ShiftHiringReasonRepository() {
    }

    /**
     * Возвращает все имеющиеся на стенде причины привлечения сотрудников
     */
    public static List<ShiftHiringReason> getShiftHiringReasons() {
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, SHIFT_HIRING_REASON);
        JSONObject jsonObject = getJsonFromUri(Projects.WFM, uri);
        if (jsonObject.length() == 0) {
            return new ArrayList<>();
        } else {
            JSONArray array = jsonObject.getJSONObject(EMBEDDED).getJSONArray("shiftHiringReasonResList");
            return getListFromJsonArray(array, ShiftHiringReason.class);
        }
    }

    public static ShiftHiringReason getReasonByTitle(String title) {
        return getShiftHiringReasons()
                .stream()
                .filter(r -> r.getTitle().equals(title))
                .findFirst()
                .orElseThrow(() -> new AssertionError(String.format("%sПричина привлечения сотрудника \"%s\" не найдена", NO_TEST_DATA, title + "\"")));
    }

    /**
     * Берет со стенда любую причину привлечения сотрудника. Если их нет, добавляет случайную.
     *
     * @return причина привлечения
     */
    public static ShiftHiringReason getRandomShiftHiringReason() {
        List<ShiftHiringReason> reasons = ShiftHiringReasonRepository.getShiftHiringReasons();
        int size = reasons.size();
        if (size == 0) {
            return PresetClass.addShiftHiringReason();
        } else {
            ShiftHiringReason reason = reasons.get(RANDOM.nextInt(size));
            LOG.info("Выбрана случайная причина привлечения сотрудника: {}", reason.getTitle());
            Allure.addAttachment("Выбор случайной причины привлечения", reason.getTitle());
            return reason;
        }
    }

    /**
     * Вернуть причины привлечения сотрудника соответствующие должности
     */
    public static List<ShiftHiringReason> getShiftsHiringReasonsForJobTitle(int omId, int jobTitleId) {
        List<NameValuePair> nameValuePairs = Pairs.newBuilder()
                .jobTitleId(jobTitleId)
                .build();
        JSONObject jsonObject = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, makePath(SHIFT_HIRING_REASON, ORGANIZATION_UNIT, omId), nameValuePairs);
        if (jsonObject.has(EMBEDDED)) {
            return getListFromJsonArray(jsonObject.getJSONObject(EMBEDDED).getJSONArray(SHIFT_HIRING_REASON_LIST),
                                        ShiftHiringReason.class);
        } else {
            return new ArrayList<>();
        }
    }

    public static ShiftHiringReason getRandomShiftHiringReasonsForJobTitle(int omId, int jobTitleId) {
        List<ShiftHiringReason> reasons = getShiftsHiringReasonsForJobTitle(omId, jobTitleId);
        if (reasons.isEmpty()) {
            return null;
        }
        return reasons.get(RANDOM.nextInt(reasons.size()));
    }

}

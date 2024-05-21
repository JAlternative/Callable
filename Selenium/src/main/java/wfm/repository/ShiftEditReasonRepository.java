package wfm.repository;

import org.apache.http.NameValuePair;
import org.json.JSONObject;
import utils.Projects;
import utils.tools.Pairs;
import wfm.models.ShiftEditReason;

import java.util.List;

import static utils.ErrorMessagesForReport.NO_TEST_DATA;
import static utils.Links.SHIFTS_EDIT_REASON;
import static utils.tools.CustomTools.getListFromJsonObject;
import static utils.tools.RequestFormers.getJsonFromUri;

/**
 * @author Evgeny Gurkin 19.08.2020
 */
public class ShiftEditReasonRepository {

    private ShiftEditReasonRepository() {}

    /**
     * Берет все комментарии причины для изменения смены
     */
    public static List<ShiftEditReason> getShiftEditReasons() {
        List<NameValuePair> nameValuePairs = Pairs.newBuilder().size(1000).build();
        JSONObject jsonObject = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, SHIFTS_EDIT_REASON, nameValuePairs);
        return getListFromJsonObject(jsonObject, ShiftEditReason.class);
    }

    /**
     * Возвращает комментарий причины изменения смены по ее имени
     *
     * @param name - имя причины
     */
    public static ShiftEditReason getShiftEditReasonByName(String name) {
        return getShiftEditReasons().stream().filter(s -> s.getTitle().equals(name)).findAny()
                .orElseThrow(() -> new AssertionError(NO_TEST_DATA + "Не был найден комментарий к сменам с названием: " + name));
    }
}

package wfm.repository;

import org.apache.http.NameValuePair;
import org.json.JSONObject;
import utils.Links;
import utils.Projects;
import utils.tools.CustomTools;
import utils.tools.Pairs;
import wfm.components.schedule.ShiftTimePosition;
import wfm.models.OutsidePlanResource;

import java.time.LocalDate;
import java.util.List;

import static utils.Links.OUTSIDE_PLAN;
import static utils.tools.RequestFormers.getJsonFromUri;
import static utils.tools.RequestFormers.makePath;

public class OutsidePlanResourceRepository {

    private OutsidePlanResourceRepository() {
    }

    /**
     * Получает все запросы на сверхурочную работу и дежурства
     *
     * @param rosterId          id ростера, для которого нужно получить запросы
     * @param shiftTimePosition временной отрезок, за который нужны запросы
     * @return список запросов
     */
    public static List<OutsidePlanResource> getAllOutsideResources(int rosterId, ShiftTimePosition shiftTimePosition) {
        String path = makePath(OUTSIDE_PLAN, Links.ROSTER, rosterId);
        List<NameValuePair> pairs = Pairs.newBuilder()
                .from(shiftTimePosition.getShiftsDateInterval().getStartDate())
                .to(shiftTimePosition.getShiftsDateInterval().getEndDate())
                .build();
        JSONObject json = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, path, pairs);
        return CustomTools.getListFromJsonObject(json, OutsidePlanResource.class);
    }

    /**
     * Получает все запросы на сверхурочную работу и дежурства
     *
     * @param rosterId id ростера, для которого нужно получить запросы
     * @param from     начало периода, за который нужны запросы
     * @param to       конец периода, за который нужны запросы
     * @return список запросов
     */
    public static List<OutsidePlanResource> getAllOutsideResources(int rosterId, LocalDate from, LocalDate to) {
        String path = makePath(OUTSIDE_PLAN, Links.ROSTER, rosterId);
        List<NameValuePair> pairs = Pairs.newBuilder()
                .from(from)
                .to(to)
                .build();
        JSONObject json = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, path, pairs);
        return CustomTools.getListFromJsonObject(json, OutsidePlanResource.class);
    }
}

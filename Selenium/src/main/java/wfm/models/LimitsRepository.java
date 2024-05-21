package wfm.models;

import org.json.JSONArray;
import org.json.JSONObject;
import utils.Params;
import utils.Projects;
import utils.tools.CustomTools;
import wfm.components.systemlists.LimitType;
import wfm.repository.CommonRepository;

import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static utils.tools.RequestFormers.getJsonFromUri;
import static utils.tools.RequestFormers.setUri;

public class LimitsRepository {
    public static List<Limits> getAllLimits() {
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, "typed-limits");
        JSONObject json = getJsonFromUri(Projects.WFM, uri);
        if (json.has(Params.EMBEDDED)) {
            JSONArray array = json.getJSONObject(Params.EMBEDDED).getJSONArray("limitByTypeResList");
            return CustomTools.getListFromJsonArray(array, Limits.class);
        } else {
            return new ArrayList<>();
        }
    }

    public static Limits getLimitByOrgUnitIdAndPositionGroupId(int omId, int positionGroupId) {
        return getAllLimits()
                .stream()
                .filter(e -> e.getOrgUnitId() == omId && e.getPositionGroupId() == positionGroupId)
                .findFirst()
                .orElse(null);
    }

    public static List<Integer> getPosGroupIdsByLimitType(LimitType limitType) {
        return getAllLimits()
                .stream()
                .filter(e -> e.getLimitType().equals(limitType.toString()))
                .map(Limits::getPositionGroupId)
                .collect(Collectors.toList());
    }

    /**
     * @param ep       позиция сотрудника
     * @param property атрибут (тип) подразделения. Для Магнита - бизнес-направление (например, L, MM, M)
     * @return список дневных лимитов, установленных на сотрудника для указанной позиции
     */
    public static List<Limits> getDayLimitForPosition(EmployeePosition ep, EntityProperty property) {
        return LimitsRepository.getAllLimits().stream()
                .filter(lim -> lim.getJobTitleName().equals(ep.getPosition().getName()))
                .filter(lim -> lim.getOrgType().equals(property.getValue()))
                .filter(lim -> lim.getPeriod().equals("DAY"))
                .filter(lim -> (LocalDate.now().isBefore(lim.getTo().plusDays(1)) && LocalDate.now().isAfter(lim.getFrom().minusDays(1))))
                .collect(Collectors.toList());
    }
}

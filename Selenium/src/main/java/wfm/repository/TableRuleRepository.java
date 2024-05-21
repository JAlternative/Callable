package wfm.repository;

import org.json.JSONArray;
import org.json.JSONObject;
import utils.Params;
import utils.Projects;
import utils.tools.CustomTools;
import wfm.models.TableRule;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static utils.Links.TIMESHEET_EDIT_RULE;
import static utils.tools.RequestFormers.getJsonFromUri;
import static utils.tools.RequestFormers.setUri;

public class TableRuleRepository {

    private TableRuleRepository() {
    }

    public static List<TableRule> getAllRules() {
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, TIMESHEET_EDIT_RULE);
        JSONObject json = getJsonFromUri(Projects.WFM, uri);
        JSONArray array = json.getJSONObject(Params.EMBEDDED).getJSONArray("timesheetEditRuleResList");
        return CustomTools.getListFromJsonArray(array, TableRule.class);
    }

    public static List<TableRule> getAllRulesWhereOmIdNotNull() {
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, TIMESHEET_EDIT_RULE);
        JSONObject json = getJsonFromUri(Projects.WFM, uri);
        if (json.has(Params.EMBEDDED)) {
            JSONArray array = json.getJSONObject(Params.EMBEDDED).getJSONArray("timesheetEditRuleResList");
            int i = 0;
            while (i < array.length()) {
                if (array.getJSONObject(i).isNull("orgUnitId")) {
                    array.remove(i);
                } else {
                    i++;
                }
            }
            return CustomTools.getListFromJsonArray(array, TableRule.class);
        }
        return new ArrayList<>();
    }

    public static TableRule getRuleByValue(String value) {
        return getAllRulesWhereOmIdNotNull()
                .stream()
                .filter(e -> e.getValue().equals(value))
                .findFirst()
                .orElse(null);
    }

    public static List<TableRule> getRuleForOrgUnit(int omId) {
        return getAllRulesWhereOmIdNotNull()
                .stream()
                .filter(e -> e.getOrgUnitId() == omId)
                .collect(Collectors.toList());
    }
}

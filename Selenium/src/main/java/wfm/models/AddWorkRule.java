package wfm.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.json.JSONArray;
import org.json.JSONObject;
import utils.LinksAnnotation;
import utils.Projects;
import utils.tools.CustomTools;
import wfm.HasLinks;
import wfm.repository.CommonRepository;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static utils.Links.*;
import static utils.Params.EMBEDDED;
import static utils.Params.LINKS;
import static utils.tools.RequestFormers.*;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AddWorkRule implements HasLinks {
    private String format;
    @LinksAnnotation
    private JSONObject links;
    private JSONObject jobTitle;
    private String requiredType;

    public AddWorkRule(JSONObject jsonObject) {
        format = jsonObject.getString("format");
        links = jsonObject.getJSONObject(LINKS);
        jobTitle = jsonObject.optJSONObject("jobTitle");
        requiredType = jsonObject.optString("requiredType");
    }

    public AddWorkRule() {
    }

    public String getFormat() {
        return format;
    }

    public String getRequiredType() {
        return requiredType;
    }

    public JSONObject getLinks() {
        return links;
    }

    public static List<AddWorkRule> getAllRulesOfAddWork(int id) {
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(SHIFTS_ADD_WORK, id, RULES));
        JSONObject json = getJsonFromUri(Projects.WFM, uri);
        if (json.has(EMBEDDED)) {
            JSONArray array = json.getJSONObject(EMBEDDED).getJSONArray("shiftAddWorkRuleResList");
            return CustomTools.getListFromJsonArray(array, AddWorkRule.class);
        } else {
            return new ArrayList<>();
        }
    }

    public static AddWorkRule getRuleByFormat(int addWorkId, String format) {
        return getAllRulesOfAddWork(addWorkId)
                .stream()
                .filter(e -> e.getFormat().equals(format))
                .findFirst()
                .orElse(null);
    }

    public JobTitle getJobTitle() {
        return new JobTitle(jobTitle);
    }
}

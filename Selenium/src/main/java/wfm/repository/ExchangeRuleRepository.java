package wfm.repository;

import org.apache.http.NameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;
import utils.Params;
import utils.Projects;
import utils.tools.CustomTools;
import utils.tools.Pairs;
import wfm.models.ExchangeRule;

import java.util.List;

import static utils.Links.EXCHANGE_RULE_MG;
import static utils.tools.RequestFormers.getJsonFromUri;

public class ExchangeRuleRepository {
    private ExchangeRuleRepository() {
    }

    public static List<ExchangeRule> getAllRules() {
        List<NameValuePair> nameValuePairs = Pairs.newBuilder().size(10000).build();
        JSONObject json = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, EXCHANGE_RULE_MG, nameValuePairs);
        JSONArray array = json.getJSONObject(Params.EMBEDDED).getJSONArray(Params.EXCHANGE_RULE_RES_LIST);
        return CustomTools.getListFromJsonArray(array, ExchangeRule.class);
    }
}


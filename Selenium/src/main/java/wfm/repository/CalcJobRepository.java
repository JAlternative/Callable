package wfm.repository;

import org.apache.http.NameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import utils.Links;
import utils.Params;
import utils.Projects;
import utils.downloading.TypeOfBatch;
import utils.tools.CustomTools;
import utils.tools.Pairs;
import utils.tools.RequestFormers;
import wfm.models.CalcJob;
import wfm.models.OrgUnit;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

import static utils.Params.EMBEDDED;
import static utils.Params.LAST_OF_ORG_UNITS;

public class CalcJobRepository {

    public static List<CalcJob> search(OrgUnit unit, TypeOfBatch type, LocalDate from, LocalDate to) {
        List<NameValuePair> pairs = Pairs.newBuilder()
                .orgUnitIdsSelf(unit.getId())
                .type(type.getName())
                .from(from)
                .to(to)
                .build();
        URI uri = RequestFormers.setUri(Projects.WFM, CommonRepository.URL_BASE, RequestFormers.makePath(Links.CALC_JOBS, LAST_OF_ORG_UNITS), pairs);
        try {
            JSONArray json = RequestFormers.getJsonFromUri(Projects.WFM, uri).getJSONObject(EMBEDDED).getJSONArray(Params.CALC_JOBS);
            return CustomTools.getListFromJsonArray(json, CalcJob.class);
        } catch (JSONException | NullPointerException e) {
            return null;
        }
    }

    public static CalcJob getLatestCalculation(int omId) {
        List<NameValuePair> pairs = Pairs.newBuilder().orgUnitIdsSelf(omId).build();
        URI uri = RequestFormers.setUri(Projects.WFM, CommonRepository.URL_BASE, RequestFormers.makePath(Links.CALC_JOBS, LAST_OF_ORG_UNITS), pairs);
        JSONArray json = RequestFormers.getJsonFromUri(Projects.WFM, uri).getJSONObject(EMBEDDED).getJSONArray(Params.CALC_JOBS);
        return CustomTools.getListFromJsonArray(json, CalcJob.class).stream().findFirst().orElse(null);
    }
}

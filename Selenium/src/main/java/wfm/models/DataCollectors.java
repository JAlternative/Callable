package wfm.models;

import org.json.JSONArray;

import java.util.HashSet;
import java.util.Set;

import static utils.Params.HREF;
import static utils.Params.LINKS;

public class DataCollectors {
    private final JSONArray array;

    protected DataCollectors(JSONArray array) {
        this.array = array;
    }

    public Set<Integer> collectData(String name) {
        Set<Integer> idsArray = new HashSet<>();
        for (int i = 0; i < array.length(); i++) {
            String link = array.getJSONObject(i).getJSONObject(LINKS).getJSONObject(name).getString(HREF);
            idsArray.add(Integer.parseInt(link.substring(link.lastIndexOf("/") + 1)));
        }
        return idsArray;
    }
}

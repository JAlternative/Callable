package wfm.models;

import org.json.JSONArray;

import java.util.Set;

public class MathParameters extends DataCollectors {

    public MathParameters(JSONArray array) {
        super(array);
    }

    public Set<Integer> getMathParamIds() {
        return collectData("mathParameter");
    }

}

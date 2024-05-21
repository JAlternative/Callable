package wfm.models;

import org.json.JSONArray;

import java.util.Set;

public class SecuredOperationDescriptor extends DataCollectors {

    public SecuredOperationDescriptor(JSONArray array) {
        super(array);
    }

    public Set<Integer> getPermissionIds() {
        return collectData("permission");
    }

    public Set<Integer> getSections() {
        return collectData("section");
    }
}

package wfm.repository;

import org.json.JSONObject;
import utils.Projects;
import utils.tools.CustomTools;
import utils.tools.RequestFormers;
import wfm.models.EmployeeStatusType;

import java.net.URI;
import java.util.List;

import static utils.Links.EMPLOYEES_STATUS_TYPES;
import static utils.tools.RequestFormers.setUri;

public class EmployeeStatusTypeRepository {

    private EmployeeStatusTypeRepository() {}

    public static List<EmployeeStatusType> getAllStatusTypes() {
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, EMPLOYEES_STATUS_TYPES);
        JSONObject object = RequestFormers.getJsonFromUri(Projects.WFM, uri);
        return CustomTools.getListFromJsonObject(object, EmployeeStatusType.class);
    }

    public static EmployeeStatusType getStatusTypeByOuterId(String id) {
        return getAllStatusTypes()
                .stream()
                .filter(s -> s.getOuterId().equals(id))
                .findFirst()
                .orElse(null);
    }
}

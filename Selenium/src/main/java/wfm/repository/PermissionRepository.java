package wfm.repository;

import org.apache.http.NameValuePair;
import org.json.JSONObject;
import utils.Projects;
import utils.tools.Pairs;
import wfm.models.Permission;

import java.util.List;

import static utils.Links.PERMISSIONS;
import static utils.tools.CustomTools.getListFromJsonObject;
import static utils.tools.RequestFormers.getJsonFromUri;

/**
 * @author Evgeny Gurkin 19.08.2020
 */
public class PermissionRepository {

    private PermissionRepository() {}

    /**
     * Берет все разрешения
     *
     * @return список разрешений
     */
    public static List<Permission> getPermissions() {
        List<NameValuePair> nameValuePairs = Pairs.newBuilder().size(1000).build();
        JSONObject permissions = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, PERMISSIONS, nameValuePairs);
        return getListFromJsonObject(permissions, Permission.class);
    }
}

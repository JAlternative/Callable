package wfm.repository;

import org.json.JSONObject;
import utils.Projects;
import wfm.models.User;
import wfm.models.UserDeputy;

import java.util.List;

import static utils.Links.USERS;
import static utils.tools.CustomTools.getListFromJsonObject;
import static utils.tools.RequestFormers.getJsonFromUri;
import static utils.tools.RequestFormers.makePath;

/**
 * @author Evgeny Gurkin 19.08.2020
 */
public class UserDeputyRepository {

    private UserDeputyRepository() {}

    /**
     * Берет всех заместителей указанного пользователя
     */
    public static List<UserDeputy> getUserDeputies(User user) {
        JSONObject jsonObject = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, makePath(USERS, user.getId(), "deputy"));
        return getListFromJsonObject(jsonObject, UserDeputy.class);
    }
}

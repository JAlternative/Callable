package wfm.repository;

import org.apache.http.NameValuePair;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.Projects;
import utils.tools.Pairs;
import wfm.models.User;
import wfm.models.UserRole;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static utils.ErrorMessagesForReport.NO_TEST_DATA;
import static utils.Links.USER_ROLES;
import static utils.tools.CustomTools.getListFromJsonObject;
import static utils.tools.CustomTools.getRandomFromList;
import static utils.tools.RequestFormers.getJsonFromUri;

/**
 * @author Evgeny Gurkin 19.08.2020
 */
public class UserRoleRepository {

    private UserRoleRepository() {
    }

    private static final Logger LOG = LoggerFactory.getLogger(UserRoleRepository.class);

    /**
     * Берет все ролевые модели
     */
    public static List<UserRole> getUserRoles() {
        List<NameValuePair> nameValuePairs = Pairs.newBuilder().size(1000).build()
                .stream()
                .filter(userRole -> !userRole.getName().contains("test_role_"))
                .collect(Collectors.toList());
        JSONObject roles = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, USER_ROLES, nameValuePairs);
        return getListFromJsonObject(roles, UserRole.class);
    }

    /**
     * Выбирает случайную роль которая отсутствует у юзера
     */
    public static UserRole getRandomAnotherRole(User user) {
        List<UserRole> userRoles = getUserRoles();
        if (user != null) {
            Set<Integer> rolesIds = user.getRolesIds();
            userRoles = userRoles.stream().filter(userRole -> !rolesIds.contains(userRole.getId()))
                    .filter(ur -> !ur.getName().contains("test_role"))
                    .collect(Collectors.toList());
        }
        UserRole rndUserRole = getRandomFromList(userRoles);
        LOG.info("Выбрана роль: {}", rndUserRole.getName());
        return rndUserRole;
    }

    /**
     * Выбирает роль по айди
     */
    public static UserRole getUserRoleById(int id) {
        return getUserRoles().stream()
                .filter(userRole -> userRole.getId() == id)
                .findAny()
                .orElseThrow(() -> new AssertionError(String.format("%sНе нашли роль по ID%d", NO_TEST_DATA, id)));
    }

}

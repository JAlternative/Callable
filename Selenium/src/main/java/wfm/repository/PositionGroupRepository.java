package wfm.repository;

import org.json.JSONObject;
import utils.Links;
import utils.Projects;
import utils.tools.CustomTools;
import utils.tools.RequestFormers;
import wfm.models.PositionGroup;

import java.net.URI;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

import static utils.tools.CustomTools.getRandomFromList;
import static utils.tools.CustomTools.randomItem;
import static utils.tools.RequestFormers.makePath;

public class PositionGroupRepository {

    private PositionGroupRepository() {}

    /**
     * Обращается в /api/v1/position-groups
     *
     * @return список всех функциональных ролей для конкретного стенда
     */

    public static List<PositionGroup> getAllPositionGroups() {
        URI uri = RequestFormers.setUri(Projects.WFM, CommonRepository.URL_BASE, Links.POSITION_GROUPS);
        JSONObject object = RequestFormers.getJsonFromUri(Projects.WFM, uri);
        return CustomTools.getListFromJsonObject(object, PositionGroup.class);
    }

    public static PositionGroup randomPositionGroup() {
        List<PositionGroup> tempList = getAllPositionGroups();
        return tempList.stream().filter(PositionGroup::getFtePositionGroup)
                                         .collect(randomItem());
    }

    public static PositionGroup getPositionGroupById(int id) {
        URI uri = RequestFormers.setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(Links.POSITION_GROUPS, id));
        JSONObject object = RequestFormers.getJsonFromUri(Projects.WFM, uri);
        return CustomTools.getClassObjectFromJson(PositionGroup.class, object);
    }

    public static PositionGroup getPositionGroupByName(String name) {
        return getAllPositionGroups()
                .stream()
                .filter(e -> e.getName().equals(name))
                .findFirst()
                .orElseThrow(NoSuchElementException::new);
    }

    public static PositionGroup getPositionGroupByPartialName(String name) {
        return getAllPositionGroups()
                .stream()
                .filter(e -> e.getName().contains(name))
                .findFirst()
                .orElseThrow(NoSuchElementException::new);
    }

    /**
     * Возвращает все функциональные роли сотрудников в заданном подразделении
     */
    public static Set<PositionGroup> getAllPositionGroupsFromOrgUnit(int omId) {
        return EmployeePositionRepository.getActualEmployeePositionsWithChief(omId).stream()
                .map(ep -> ep.getPosition().getPositionGroupId())
                .filter(id -> !id.equals(0))
                .sorted()
                .distinct()
                .map(PositionGroupRepository::getPositionGroupById)
                .collect(Collectors.toSet());
    }

    /**
     * Возвращает рэндомную функциональную роль, сотрудников с которой нет в подразделении с указанным omId
     */
    public static PositionGroup getAnotherPosGroup(int omId) {
        List<PositionGroup> allPosGroups = getAllPositionGroups();
        Set<PositionGroup> posGroups = getAllPositionGroupsFromOrgUnit(omId);
        allPosGroups.removeAll(posGroups);
        return getRandomFromList(allPosGroups);
    }

}

package wfm.components.orgstructure;

import org.json.JSONObject;
import utils.Projects;
import utils.tools.CustomTools;
import utils.tools.RequestFormers;
import wfm.repository.CommonRepository;
import wfm.repository.OrgUnitRepository;

import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import static utils.Links.ORGANIZATION_UNIT_TYPES;
import static utils.Params.*;
import static utils.tools.CustomTools.randomItem;

public class OrganizationUnitTypeId {
    private final String name;
    private final int level;
    private final int id;
    private final String outer_id;
    private final int countOfOrganizationUnits;

    public OrganizationUnitTypeId(JSONObject jsonObject) {
        this.id = jsonObject.optInt(ID);
        this.name = jsonObject.getString(NAME);
        this.level = jsonObject.getInt(LEVEL);
        this.outer_id = jsonObject.optString(OUTER_ID);
        this.countOfOrganizationUnits = jsonObject.optInt("countOfOrganizationUnits");
    }

    public static OrganizationUnitTypeId getRandomType() {
        return CustomTools.getRandomFromList(getAllOrgUnitTypes());
    }

    public static List<OrganizationUnitTypeId> getAllOrgUnitTypes() {
        URI uri = RequestFormers.setUri(Projects.WFM, CommonRepository.URL_BASE, ORGANIZATION_UNIT_TYPES);
        JSONObject object = RequestFormers.getJsonFromUri(Projects.WFM, uri);
        return CustomTools.getListFromJsonObject(object, OrganizationUnitTypeId.class);
    }

    public static OrganizationUnitTypeId getLowest() {
        return getAllOrgUnitTypes()
                .stream()
                .max(Comparator.comparing(OrganizationUnitTypeId::getLevel))
                .orElseThrow(NoSuchElementException::new);
    }

    public static OrganizationUnitTypeId getHighest() {
        return getAllOrgUnitTypes()
                .stream()
                .min(Comparator.comparing(OrganizationUnitTypeId::getLevel))
                .orElseThrow(NoSuchElementException::new);
    }

    public static OrganizationUnitTypeId getType(String name) {
        return getAllOrgUnitTypes()
                .stream()
                .filter(type -> type.getName().equals(name))
                .findFirst()
                .orElseThrow(NoSuchElementException::new);
    }

    public static OrganizationUnitTypeId getOrgUnitTypeByLevel(int level) {
        return getAllOrgUnitTypes()
                .stream()
                .filter(type -> type.getLevel() == level)
                .findFirst()
                .orElseThrow(NoSuchElementException::new);
    }

    public static List<OrganizationUnitTypeId> getAllOrgUnitTypesWithOrgUnits() {
        List<OrganizationUnitTypeId> orgUnitTypes = getAllOrgUnitTypes();
        return orgUnitTypes.stream().filter(orgUnitType -> orgUnitType.countOfOrganizationUnits > 0).collect(Collectors.toList());
    }

    public static OrganizationUnitTypeId getLowestOrgUnitTypeWithOrgUnits() {
        return getAllOrgUnitTypesWithOrgUnits()
                .stream()
                .max(Comparator.comparing(OrganizationUnitTypeId::getLevel))
                .orElseThrow(NoSuchElementException::new);
    }

    public static OrganizationUnitTypeId getRandomParentOrgUnitType(OrganizationUnitTypeId orgUnitType, boolean withOrgUnits) {
        List<OrganizationUnitTypeId> orgUnitTypes = withOrgUnits ? getAllOrgUnitTypesWithOrgUnits() : getAllOrgUnitTypes();
        return orgUnitTypes
                .stream()
                .filter(organizationUnitTypeId -> organizationUnitTypeId.level < orgUnitType.level)
                .collect(randomItem());
    }

    public static OrganizationUnitTypeId getRandomTypeFromList(List<OrganizationUnitTypeId> list) {
        return CustomTools.getRandomFromList(list);
    }

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }

    public int getLevel() {
        return level;
    }

    public String getOuterId() {
        return outer_id;
    }
}

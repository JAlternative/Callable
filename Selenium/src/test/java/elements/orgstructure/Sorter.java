package elements.orgstructure;

import org.json.JSONObject;
import utils.Links;
import utils.Projects;
import utils.tools.Pairs;
import wfm.components.orgstructure.OrganizationUnitTypeId;
import wfm.components.utils.SorterOptions;
import wfm.models.Employee;
import wfm.models.OrgUnit;
import wfm.models.UserRole;
import wfm.repository.EmployeeRepository;
import wfm.repository.OrgUnitRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static utils.Links.USER_ROLES;
import static utils.Params.ASC;
import static utils.Params.NAME;
import static utils.tools.CustomTools.getListFromJsonObject;
import static utils.tools.RequestFormers.getJsonFromUri;

public class Sorter {

    private static List<String> getEmployeesFromSorters(Pairs.Builder otherSorters, boolean reverse) {
        List<Employee> listFromJsonObject = EmployeeRepository.getEmployeesBySorter(otherSorters, reverse);
        return listFromJsonObject.stream().map(Employee::getFullName).collect(Collectors.toList());
    }

    private List<String> getOrgUnitsFromApi(Pairs.Builder otherSorters, boolean reverse) {
        String sort = reverse ? "desc" : ASC;
        otherSorters.size(20).sort(NAME + "," + sort);
        return OrgUnitRepository.getOrgUnitsBySorter(otherSorters).stream().map(OrgUnit::getName).collect(Collectors.toList());
    }

    private List<Integer> getOrgUnitsFromApi(Pairs.Builder otherSorters) {
        otherSorters.size(1000).sort("name," + ASC).includeDate(LocalDate.now());
        return OrgUnitRepository.getOrgUnitsBySorter(otherSorters).stream().map(OrgUnit::getId).collect(Collectors.toList());
    }

    private Pairs.Builder getBuilderFromSorter(SorterOptions[] options) {
        Pairs.Builder builder = Pairs.newBuilder();
        Stream.of(options).forEach(sorterOptions -> builder.doMergeBuilders(sorterOptions.getSorter()));
        return builder;
    }

    public List<String> getEmployees(boolean reverse, SorterOptions... options) {
        Pairs.Builder builder = getBuilderFromSorter(options);
        return getEmployeesFromSorters(builder, reverse);
    }

    public List<String> getEmployeesByPosition(SorterOptions options, boolean reverse, String byName) {
        Pairs.Builder builder = options.getSorter().positionNames(byName);
        return getEmployeesFromSorters(builder, reverse);
    }

    public List<String> getEmployeesByName(SorterOptions options, boolean reverse, String byName) {
        Pairs.Builder builder = options.getSorter().fullName(byName);
        return getEmployeesFromSorters(builder, reverse);
    }

    public List<String> getEmployees(SorterOptions options, boolean reverse, int orgUnitId) {
        Pairs.Builder builder = options.getSorter().orgUnitIds(orgUnitId);
        return getEmployeesFromSorters(builder, reverse);
    }

    public List<String> getOrgUnits(boolean reverse, SorterOptions... options) {
        Pairs.Builder builder = getBuilderFromSorter(options);
        return getOrgUnitsFromApi(builder, reverse);
    }

    public List<Integer> getOrgUnits(SorterOptions options, OrganizationUnitTypeId typeList) {
        Pairs.Builder builder = options.getSorter().orgUnitTypeIds(typeList.getId());
        return getOrgUnitsFromApi(builder);
    }

    public List<String> getOrgUnitsByName(SorterOptions options, boolean reverse, String byName) {
        Pairs.Builder builder = options.getSorter().name(byName);
        return getOrgUnitsFromApi(builder, reverse);
    }

    public List<String> getOrgUnitsByTag(SorterOptions options, boolean reverse, String byName) {
        Pairs.Builder builder = options.getSorter().tagIds(byName);
        return getOrgUnitsFromApi(builder, reverse);
    }

    public List<String> getOrgUnitsByTypeIds(SorterOptions options, boolean reverse, int ids) {
        Pairs.Builder builder = options.getSorter().orgUnitTypeIds(ids);
        return getOrgUnitsFromApi(builder, reverse);
    }

    public List<String> getOrgUnitsById(SorterOptions options, boolean reverse, int ids) {
        Pairs.Builder builder = options.getSorter().orgUnitIds(ids);
        return getOrgUnitsFromApi(builder, reverse);
    }

    public List<String> getRolesFromApi(boolean reverse) {
        String sort = reverse ? "desc" : ASC;
        Pairs.Builder builder = Pairs.newBuilder().size(20).sort(NAME + "," + sort);
        JSONObject object = getJsonFromUri(Projects.WFM, Links.getTestProperty("release"), USER_ROLES, builder.build());
        return getListFromJsonObject(object, UserRole.class).stream().map(UserRole::getName).collect(Collectors.toList());
    }
}




package apitest;

import io.qameta.allure.*;
import io.qameta.allure.testng.Tag;
import org.apache.commons.lang.RandomStringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.*;
import org.testng.asserts.SoftAssert;
import reporting.TestListener;
import testutils.BaseTest;
import utils.Params;
import wfm.ApiRequest;
import wfm.PresetClass;
import wfm.components.orgstructure.OrgUnitOptions;
import wfm.components.schedule.RosterTypes;
import wfm.components.schedule.ScheduleRequestType;
import wfm.components.utils.PermissionType;
import wfm.components.utils.Role;
import wfm.models.*;
import wfm.repository.*;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Predicate;

import static apitest.HelperMethods.*;
import static common.Groups.*;
import static utils.Links.*;
import static utils.Params.*;
import static utils.tools.CustomTools.*;
import static utils.tools.RequestFormers.*;

@Listeners({TestListener.class})
public class OrgStructureApiTests extends BaseTest {

    public Map<String, List<Integer>> makeOrgUnitObject(int orgUnit) {
        Map<String, List<Integer>> units = new HashMap<>();
        List<Integer> unitList = Collections.singletonList(orgUnit);
        units.put(Params.ORG_SELF, unitList);
        units.put(Params.ORG_CHILD, unitList);
        return units;
    }

    public static Map<String, Map<String, String>> makeLinksForUserRole(int userRoleId, int userId) {
        Map<String, Map<String, String>> links = new HashMap<>();
        Map<String, String> link1 = Collections.singletonMap(Params.HREF, makePath(CommonRepository.URL_BASE, API_V1, USER_ROLES, userRoleId));
        Map<String, String> link2 = Collections.singletonMap(Params.HREF, makePath(CommonRepository.URL_BASE, API_V1, USERS, userId, ROLES));
        links.put(Params.USER_ROLE, link1);
        links.put(Params.SELF, link2);
        return links;
    }

    /**
     * Выбирает другого сотрудника в подразделении согласно предикату
     *
     * @param ep        - позиция сотрудника в выбранном подразделении
     * @param predicate - предикат
     * @return сотрудник (Employee), работающий в том же подраздедении
     */
    private Employee getAnotherEmployee(EmployeePosition ep, Predicate<EmployeePosition> predicate) {
        List<EmployeePosition> eps = EmployeePositionRepository.getActualEmployeePositionsWithChief(ep.getOrgUnit().getId());
        EmployeePosition anotherEp = eps.stream()
                .filter(predicate)
                .collect(randomItem());
        Employee employee = anotherEp.getEmployee();
        LOG.info("Был выбран сотрудник: {}", employee.getFullName());
        return employee;
    }

    @Step("Проверить, что изменились права на тип запроса")
    private void assertChangeScheduleRequestRoster(List<RolePermission> rolePermissionsBefore, UserRole userRole,
                                                   ScheduleRequestType scheduleRequestType, RosterTypes rosterType) {

        List<RolePermission> rolePermissionsAfter = new ApiRequest.GetBuilder(userRole.getPath(REL_SCHEDULE_REQUESTS))
                .send()
                .returnList(RolePermission.class, makeJsonPath(Params.EMBEDDED, REL_SCHEDULE_REQUESTS));
        Assert.assertEquals(rolePermissionsAfter.size(), rolePermissionsBefore.size() + 1, "Внесенные изменения не сохранились.");
        rolePermissionsAfter.removeAll(rolePermissionsBefore);
        RolePermission rolePermission = rolePermissionsAfter.iterator().next();
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(rolePermission.getScheduleRequestType(), scheduleRequestType.toString(), "Изменен неверный тип запроса расписания.");
        softAssert.assertEquals(rolePermission.getRosterType(), rosterType.getValue(), "Выбран неверный тип ростера.");
        softAssert.assertAll();
        Allure.addAttachment("Проверка", String.format("У роли \"%s\" были изменены права для типа запроса расписания \"%s\" на тип ростера \"%s\"",
                                                       userRole.getName(), scheduleRequestType.getName(), rosterType.getName()));
    }

    /**
     * Добавить сотруднику тег из-под пользователя
     */
    private void addTagForEmployee(List<String> tagsBefore, String tag, Employee employee, User user) {
        Map<String, String> map = new HashMap<>();
        if (tagsBefore.isEmpty()) {
            map.put(Params.TAGS, tag);
        } else {
            map.put(Params.TAGS, String.join(", ", tagsBefore) + ", " + tag);
        }
        new ApiRequest.PutBuilder(makePath(EMPLOYEES, Params.TAGS, employee.getId()))
                .withBody(map)
                .withUser(user)
                .send();
    }

    @Step("Проверить, что тег {tag} добавился у сотрудника {employee}")
    private void assertAddTagForEmployee(List<String> tagsBefore, String tag, Employee employee) {
        List<String> tagsAfter = new ArrayList<>(employee.getActualTags());
        SoftAssert softAssert = new SoftAssert();
        if (!tagsBefore.contains(tag)) {
            softAssert.assertEquals(tagsAfter.size(), tagsBefore.size() + 1,
                                    String.format("Количество тегов у сотрудника \"%s\" не увеличелось", employee));
        }
        softAssert.assertTrue(tagsAfter.contains(tag),
                              String.format("Сотруднику \"%s\" не добавлен тег \"%s\"", employee, tag));
        softAssert.assertAll();
        Allure.addAttachment("Проверка", String.format("Сотруднику \"%s\" добавлен тег \"%s\"", employee, tag));
    }

    /**
     * Редактировать контактные данные
     */
    private void editEmail(String email, User user, OrgUnit unit, int status, String message) {
        String urlEnding = makePath(ORG_UNITS, unit.getId(), EMAIL);
        Map<String, String> map = new HashMap<>();
        map.put(EMAIL, email);
        new ApiRequest.PostBuilder(urlEnding)
                .withBody(map)
                .withStatus(status)
                .withUser(user)
                .withMessage(message)
                .send();
    }

    @Test(groups = {"ABCHR3263-1", G1, MIX3},
            description = "Добавление полномочий на запросы расписания")
    @Severity(SeverityLevel.NORMAL)
    @Link(name = "Статья: \"3263,3086_Добавить права на отдельные shedule request\"",
            url = "https://wiki.goodt.me/pages/viewpage.action?pageId=204280396")
    @Owner(MATSKEVICH)
    @TmsLink("60241")
    @Tag("ABCHR3263-1")
    @Tag(MIX3)
    private void addPermissionsToScheduleRequests() {
        UserRole userRole = getRandomFromList(UserRoleRepository.getUserRoles());
        int rolesId = userRole.getId();
        ScheduleRequestType scheduleRequestType = ScheduleRequestType.getRandomScheduleRequestType();
        RosterTypes rosterType = RosterTypes.getRandomRosterType();
        List<RolePermission> rolePermissionsBefore = new ApiRequest.GetBuilder(userRole.getPath(REL_SCHEDULE_REQUESTS)).send()
                .returnList(RolePermission.class, makeJsonPath(Params.EMBEDDED, REL_SCHEDULE_REQUESTS));
        Allure.step(String.format("Изменить у роли \"%s\" права на тип ростера \"%s\" для типа запроса расписания \"%s\"",
                                  userRole.getName(), rosterType.getName(), scheduleRequestType.getName()),
                    () -> PresetClass.changeScheduleRequestRoster(rolesId, scheduleRequestType, rosterType));
        assertChangeScheduleRequestRoster(rolePermissionsBefore, userRole, scheduleRequestType, rosterType);
    }

    @Test(groups = {"ABCHR4600-7", G2, MIX2},
            description = "Добавление тега в карточку другого сотрудника")
    @Severity(SeverityLevel.NORMAL)
    @Link(name = "Статья: \"4600_Добавить права на управление тегами\"",
            url = "https://wiki.goodt.me/pages/viewpage.action?pageId=223511977&moved=true")
    @Owner(MATSKEVICH)
    @TmsLink("60222")
    @Tag("ABCHR4600-7")
    @Tag(MIX2)
    private void editTagsForAnotherEmployee() {
        OrgUnit unit = OrgUnitRepository.getRandomOmWithEmployeeOptions(OrgUnitOptions.EMPLOYEES_WITH_POSITION);
        User user = getUserWithPermissions(Arrays.asList(PermissionType.ORGANIZATION_UNIT_VIEW,
                                                         PermissionType.ORGANIZATION_UNIT_EDIT,
                                                         PermissionType.ORG_EMPLOYEE_EDIT,
                                                         PermissionType.EDIT_EMPLOYEE_TAGS,
                                                         PermissionType.ORG_UNIT_EMPLOYEE_PERSONALISATION), unit);
        Employee employee = EmployeeRepository.getRandomWorkingEmployee(unit.getId(), user.getEmployee());
        List<String> tagsBefore = employee.getActualTags();
        String tag = CommonRepository.getRandomTagFromApi(tagsBefore);
        addTagForEmployee(tagsBefore, tag, employee, user);
        List<String> tagsAfter = employee.getActualTags();
        assertPost(tagsBefore, tagsAfter, tag);
    }

    @Test(groups = {"ABCHR4965-2", G2, MIX2},
            description = "Добавление роли сотруднику при наличии разрешения (пользователь)")
    @Link(name = "4965_Добавить права на блок \"Раздать права\"",
            url = "https://wiki.goodt.me/x/CwCqDQ")
    @Severity(SeverityLevel.MINOR)
    @TmsLink("60240")
    @Tag("ABCHR4965-2")
    @Tag(MIX2)
    public void addRoleToEmployeeWithPermission() {
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWorkingWithUser();
        Employee employee = ep.getEmployee();
        OrgUnit unit = ep.getOrgUnit();
        int omId = unit.getId();
        List<Integer> omIds = new ArrayList<>();
        omIds.add(omId);
        LOG.info(omIds.toString());
        User user = getUserWithPermissions(Arrays.asList(PermissionType.ROLES_DISTRIBUTION_VIEW,
                                                         PermissionType.ROLES_DISTRIBUTION_EDIT), unit);
        User anotherUser = employee.getUser();
        UserRole randomRole = UserRoleRepository.getRandomAnotherRole(anotherUser);
        User.RoleInUser roleInUser = new User.RoleInUser()
                .setStartRoleDate(LocalDate.now())
                .setOrgUnitList(omIds)
                .setLinks(new JSONObject(makeLinksForUserRole(randomRole.getId(), anotherUser.getId())));
        List<User.RoleInUser> rolesBefore = anotherUser.getRoles();
        Allure.step(String.format("Создать пользователю %s роль \"%s\"", employee.getFullName(), randomRole.getName()),
                    () -> new ApiRequest.PostBuilder(makePath(anotherUser.getSelfPath(), ROLES))
                            .withBody(roleInUser)
                            .withUser(user)
                            .send());
        Allure.step(String.format("Добавить к роли \"%s\" оргюнит \"%s\"", randomRole.getName(), unit.getName()),
                    () -> new ApiRequest.PostBuilder(makePath(ORG_UNIT_ROLE, employee.getUser().getLastRoleInUser().getOrgUnitRole(), ORG_UNITS))
                            .withBody(makeOrgUnitObject(omId))
                            .withStatus(200)
                            .send());
        User updatedUser = employee.getUser();
        User.RoleInUser updatedRoleInUser = updatedUser.getLastRoleInUser();
        List<User.RoleInUser> rolesAfter = updatedUser.getRoles();
        assertPost(rolesBefore, rolesAfter, updatedRoleInUser);
        assertPost(new ArrayList<>(), updatedRoleInUser.getOrgUnitList(), omId);
    }

    @Test(groups = {"ABCHR4965-3", G2, MIX2},
            description = "Удаление роли у сотрудника при наличии разрешения (пользователь)")
    @Link(name = "Статья: \"4965_Добавить права на блок \"Раздать права\"", url = "https://wiki.goodt.me/x/CwCqDQ")
    @Severity(SeverityLevel.MINOR)
    @TmsLink("60240")
    @Tag("ABCHR4965-3")
    @Tag(MIX2)
    public void deleteRoleInUserWithPermission() {
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWorkingWithUser();
        Employee employee = getAnotherEmployee(ep, e -> !e.getEmployee().getUser().getRoles().isEmpty() && !e.equals(ep));
        User userWithRole = employee.getUser();
        User user = getUserWithPermissions(Arrays.asList(PermissionType.ROLES_DISTRIBUTION_VIEW,
                                                         PermissionType.ROLES_DISTRIBUTION_EDIT), ep.getOrgUnit());
        List<User.RoleInUser> rolesBefore = userWithRole.getRoles();
        User.RoleInUser roleInUser = getRandomFromList(rolesBefore);
        int orgUnitRoleId = roleInUser.getOrgUnitRole();
        Allure.step(String.format("Удалить все орг юниты у роли \"%s\"", UserRoleRepository.getUserRoleById(roleInUser.getUserRoleId()).getName()),
                    () -> PresetClass.deleteOrgUnitsFromRole(orgUnitRoleId));
        new ApiRequest.DeleteBuilder(makePath(ORG_UNIT_ROLE, orgUnitRoleId)).withUser(user).send();
        List<User.RoleInUser> rolesAfter = employee.getUser().getRoles();
        assertDelete(rolesBefore, rolesAfter, roleInUser);
    }

}

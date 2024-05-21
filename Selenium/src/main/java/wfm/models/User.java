package wfm.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.json.JSONArray;
import org.json.JSONObject;
import utils.LinksAnnotation;
import utils.Projects;
import utils.serialization.LocalDateSerializer;
import utils.tools.CustomTools;
import wfm.HasLinks;

import java.net.URI;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static utils.Links.*;
import static utils.Params.*;
import static utils.tools.RequestFormers.*;

public class User implements HasLinks {

    private final String username;
    private final String fullName;
    private String password;
    private final boolean changePassword;
    private final JSONObject links;
    private final int id;
    private final JSONObject embeddedRoles;

    public User(JSONObject jsonObject) {
        this.links = jsonObject.getJSONObject(LINKS);
        this.fullName = jsonObject.optString(FULL_NAME);
        this.changePassword = jsonObject.getBoolean(CHANGE_PASSWORD);
        this.username = jsonObject.getString(USERNAME);
        this.password = null;
        this.id = jsonObject.getInt(ID);
        this.embeddedRoles = jsonObject.optJSONObject(EMBEDDED);
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }

    public boolean isChangePassword() {
        return changePassword;
    }

    /**
     * Возвращает роль (RoleInUser), действие которой начинается на текущий день
     */
    public RoleInUser getLastRoleInUser() {
        return getRoles().stream()
                .filter(role -> role.startRoleDate.equals(LocalDate.now()))
                .findAny()
                .orElse(null);
    }

    public Set<Integer> getRolesIds() {
        return getRoles().stream().map(RoleInUser::getUserRoleId).collect(Collectors.toSet());
    }

    public List<UserDeputy> getUserDeputies() {
        JSONObject userDeputies = getJsonFromUri(Projects.WFM, URI.create(getLink("userDeputys")));
        List<UserDeputy> userDeputyList = new ArrayList<>();
        if (!userDeputies.isNull(EMBEDDED)) {
            JSONArray deputies = userDeputies.getJSONObject(EMBEDDED).getJSONArray("userDeputys");
            userDeputyList = CustomTools.getListFromJsonArray(deputies, UserDeputy.class);
        }
        return userDeputyList;
    }

    public Employee getEmployee() {
        return new Employee(getJsonFromUri(Projects.WFM, URI.create(getLink(EMPLOYEE))));
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public List<RoleInUser> getRoles() {
        List<RoleInUser> roleInUsers = new ArrayList<>();
        if (embeddedRoles != null) {
            JSONArray roles = embeddedRoles.getJSONArray(ROLES);
            roleInUsers = CustomTools.getListFromJsonArray(roles, RoleInUser.class);
        }
        return roleInUsers;
    }

    public static RoleInUser getById(List<RoleInUser> roleInUsers, int id) {
        return roleInUsers.stream().filter(roleInUser -> roleInUser.getUserRoleId() == id).findAny().orElseThrow(AssertionError::new);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        User user = (User) o;
        return username.equals(user.getUsername())
                && fullName.equals(user.getFullName())
                && changePassword == user.changePassword
                && links.toString().equals(user.links.toString())
                && id == user.getId()
                && embeddedRoles.toString().equals(user.embeddedRoles.toString());
    }

    public User refresh() {
        URI uri = URI.create(this.getSelfLink());
        return CustomTools.getClassObjectFromJson(User.class, getJsonFromUri(Projects.WFM, uri));
    }

    /**
     * Соответствует JSON-объектам, возвращаемым по запросу /api/v1/org-unit-role/{id}
     */

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RoleInUser {
        @JsonProperty("from")
        @JsonSerialize(using = LocalDateSerializer.class)
        private LocalDate startRoleDate;
        @JsonProperty("to")
        @JsonSerialize(using = LocalDateSerializer.class)
        private LocalDate endRoleDate;
        @JsonProperty("orgUnitIds")
        private List<Integer> orgUnits;
        @JsonProperty("_replaceOrgUnits")
        private Boolean replaceOrgUnits;
        @JsonProperty("_selectedOrgUnits")
        private JSONArray selectedOrgUnits;
        @LinksAnnotation
        private JSONObject links;

        public RoleInUser(JSONObject jsonObject) {
            this.links = jsonObject.getJSONObject(LINKS);
            this.startRoleDate = !jsonObject.optString(FROM).isEmpty() ? LocalDate.parse(jsonObject.optString(FROM)) : null;
            this.endRoleDate = !jsonObject.optString(TO).isEmpty() ? LocalDate.parse(jsonObject.optString(TO)) : null;
            URI uri = setUri(Projects.WFM, "", makePath(links.getJSONObject(ORGANIZATION_UNIT_IDS).getString(HREF)));
            uri = URI.create(uri.toString().substring(1));
            this.orgUnits = getJsonFromUriForArrays(Projects.WFM, uri);
        }

        public RoleInUser() {
        }

        public LocalDate getStartRoleDate() {
            return startRoleDate;
        }

        public RoleInUser setStartRoleDate(LocalDate startRoleDate) {
            this.startRoleDate = startRoleDate;
            return this;
        }

        public LocalDate getEndRoleDate() {
            return endRoleDate;
        }

        public JSONArray getSelectedOrgUnits() {
            return selectedOrgUnits;
        }

        public RoleInUser setSelectedOrgUnits(JSONArray selectedOrgUnits) {
            this.selectedOrgUnits = selectedOrgUnits;
            return this;
        }

        public List<Integer> getOrgUnits() {
            return orgUnits;
        }

        public RoleInUser setOrgUnits(List<Integer> orgUnits) {
            this.orgUnits = orgUnits;
            return this;
        }

        public Boolean getReplaceOrgUnits() {
            return replaceOrgUnits;
        }

        public RoleInUser setReplaceOrgUnits(Boolean replaceOrgUnits) {
            this.replaceOrgUnits = replaceOrgUnits;
            return this;
        }

        @JsonIgnore
        public List<Integer> getOrgUnitList() {
            List<Integer> orgUnitIds = new ArrayList<>();
            if (orgUnits != null) {
                for (int i = 0; i < orgUnits.size(); i++) {
                    Integer temp = orgUnits.get(i);
                    orgUnitIds.add(temp);
                }
            }
            return orgUnitIds;
        }
        public RoleInUser setOrgUnitList(List<Integer> omIds) {
            this.orgUnits = omIds;
            return this;
        }
        public JSONObject getLinks() {
            return links;
        }

        @JsonIgnore
        public URI getLink(String name) {
            return URI.create(links.getJSONObject(name).getString(HREF));
        }

        public RoleInUser setLinks(JSONObject links) {
            this.links = links;
            return this;
        }

        @JsonIgnore
        public List<Integer> getOrgUnitArray() {
            return this.orgUnits;
        }

        /**
         * Вспомогательный метод
         *
         * @return id связанной с объектом UserRole
         */
        @JsonIgnore
        public int getUserRoleId() {
            String roleLink = links.getJSONObject(REL_USER_ROLE).getString(HREF);
            return Integer.parseInt(roleLink.substring(roleLink.lastIndexOf("/") + 1));
        }

        /**
         * Вспомогательный метод
         *
         * @return id, который парсится из ссылки SELF
         */
        @JsonIgnore
        public int getId() {
            String orgUnitRoleNumber = links.getJSONObject(SELF).getString(HREF);
            orgUnitRoleNumber = orgUnitRoleNumber.substring(orgUnitRoleNumber.indexOf(ORG_UNIT_ROLE) + ORG_UNIT_ROLE.length() + 1);
            return Integer.parseInt(orgUnitRoleNumber);
        }

        /**
         * Возвращает орг юнит роль. Используется для добавления/удаления оргюнитов у роли
         */
        @JsonIgnore
        public int getOrgUnitRole() {
            String link = getLink(SELF).toString();
            return Integer.parseInt(link.substring(link.lastIndexOf("/") + 1));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            RoleInUser roleInUser = (RoleInUser) o;
            boolean bool = startRoleDate.equals(roleInUser.startRoleDate) &&
                    this.getOrgUnitList().containsAll(roleInUser.getOrgUnitList());
            if (endRoleDate != null) {
                return bool && endRoleDate.equals(roleInUser.endRoleDate);
            }
            return bool;
        }
    }
}

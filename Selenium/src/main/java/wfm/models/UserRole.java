package wfm.models;

import org.json.JSONArray;
import org.json.JSONObject;
import utils.Projects;
import utils.tools.RequestFormers;
import wfm.HasLinks;
import wfm.repository.CommonRepository;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static utils.Links.USER_ROLES;
import static utils.Params.*;
import static utils.tools.RequestFormers.getJsonFromUri;
import static utils.tools.RequestFormers.makePath;

public class UserRole implements HasLinks {
    private final String name;
    private final String description;
    private final int id;
    private final JSONObject links;

    public UserRole(JSONObject jsonObject) {
        this.name = jsonObject.getString(NAME);
        this.description = jsonObject.getString(DESCRIPTION);
        this.links = jsonObject.getJSONObject(LINKS);
        this.id = jsonObject.getInt(ID);
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Integer getId() {
        return id;
    }

    public SecuredOperationDescriptor getSecuredOperationDescriptor() {
        JSONObject object = getJsonFromUri(Projects.WFM,
                URI.create(links.getJSONObject(SECURED_OPERATION_DESCRIPTORS).getString(HREF)));
        return object.isNull(EMBEDDED) ? new SecuredOperationDescriptor(new JSONArray())
                : new SecuredOperationDescriptor(object.getJSONObject(EMBEDDED).getJSONArray(SECURED_OPERATION_DESCRIPTORS));
    }

    public MathParameters getMathParameters() {
        JSONObject object = getJsonFromUri(Projects.WFM,
                URI.create(getMathParametersLink()));
        return object.isNull(EMBEDDED) ? new MathParameters(new JSONArray())
                : new MathParameters(object.getJSONObject(EMBEDDED).getJSONArray(MATH_PARAMETERS));
    }

    public String getSecuredOperationDescriptorLink() {
        return links.getJSONObject(SECURED_OPERATION_DESCRIPTORS).getString(HREF);
    }

    public String getMathParametersLink() {
        return links.getJSONObject(MATH_PARAMETERS).getString(HREF);
    }

    @Override
    public JSONObject getLinks() {
        return links;
    }

    public Map<String, String> getLinksMap() {
        HashMap<String, String> tempLinks = new HashMap<>();
        if (links != null) {
            links.keys().forEachRemaining(k -> tempLinks.put(k, links.getJSONObject(k).getString(HREF)));
        }
        return tempLinks;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UserRole userRole = (UserRole) o;
        return userRole.getId() == this.id
                && userRole.getName().equals(this.name)
                && userRole.getDescription().equals(this.description)
                && userRole.links.toString().equals(this.links.toString());
    }

    /**
     * Обновляет информацию о роли
     */
    public UserRole refreshUserRole() {
        UserRole userRole;
        try {
            userRole = new UserRole(RequestFormers.getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, makePath(USER_ROLES, getId())));
            return userRole;
        } catch (AssertionError e) {
            return null;
        }
    }
}

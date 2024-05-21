package wfm.models;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;
import wfm.components.utils.PermissionType;

import static utils.Params.*;

/**
 * @author Evgeny Gurkin 20.07.2020
 */
public class Permission {

    private final String permissionType;
    private final String name;
    private final JSONObject selfLink;

    public Permission(JSONObject jsonObject) {
        this.permissionType = jsonObject.getString("permissionType");
        this.name = jsonObject.getString("name");
        this.selfLink = jsonObject.getJSONObject(LINKS);
    }

    public String getPermissionStringType() {
        return permissionType;
    }

    public PermissionType getPermissionType() {
        PermissionType permissionTypeValue = null;
        try {
            permissionTypeValue = PermissionType.valueOf(permissionType);
        } catch (IllegalArgumentException e) {
            LoggerFactory.getLogger(Permission.class).info("Добавьте константу {}", e.getMessage());
        }
        return permissionTypeValue;
    }

    public String getName() {
        return name;
    }

    public String getSelfLink() {
        return selfLink.getJSONObject(SELF).getString(HREF);
    }

    public Integer getId() {
        String link = getSelfLink();
        return Integer.parseInt(link.substring(link.lastIndexOf('/') + 1));
    }

    /**
     * метод делит название разрешения на название группы и имя самого разрешения
     *
     * @return слева группа, справа имя разрешения
     */
    public ImmutablePair<String, String> getPermissionGroupAndName() {
        String[] s = name.split("\\.");
        String group = s[0].replace(".", "").trim();
        String groupName = s[1].replace(".", "").trim();
        return new ImmutablePair<>(group, groupName);
    }
}

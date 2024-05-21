package wfm.repository;

import org.json.JSONObject;
import utils.Projects;
import wfm.models.EntityPropertiesKey;

import java.net.URI;
import java.util.List;

import static utils.Links.*;
import static utils.tools.CustomTools.getListFromJsonObject;
import static utils.tools.RequestFormers.*;

public class EntityPropertyKeyRepository {

    private EntityPropertyKeyRepository() {
    }

    public static List<EntityPropertiesKey> getAllProperties(String keyLink) {
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(ENTITY_PROPERTIES_KEY, keyLink));
        JSONObject json = getJsonFromUri(Projects.WFM, uri);
        return getListFromJsonObject(json, EntityPropertiesKey.class);
    }

    public static EntityPropertiesKey getPropertyByKey(String keyLink, String key) {
        return getAllProperties(keyLink)
                .stream()
                .filter(e -> e.getKey().equals(key))
                .findFirst()
                .orElse(null);
    }

    public static EntityPropertiesKey getPropertyByDisplay(String keyLink, String display) {
        return getAllProperties(keyLink)
                .stream()
                .filter(e -> e.getDisplay().equals(display))
                .findFirst()
                .orElse(null);
    }

    public static EntityPropertiesKey getRandomProperty(String keyLink) {
        return getAllProperties(keyLink)
                .stream()
                .findAny()
                .orElse(null);
    }
}

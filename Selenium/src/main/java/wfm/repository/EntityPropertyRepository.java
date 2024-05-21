package wfm.repository;

import org.json.JSONException;
import org.json.JSONObject;
import utils.Projects;
import wfm.components.orgstructure.MathParameterEntities;
import wfm.models.EntityProperty;

import java.net.URI;
import java.util.List;

import static utils.Links.*;
import static utils.tools.CustomTools.getClassObjectFromJson;
import static utils.tools.CustomTools.getListFromJsonObject;
import static utils.tools.RequestFormers.*;

public class EntityPropertyRepository {

    private EntityPropertyRepository() {
    }

    public static List<EntityProperty> getAllPropertiesFromUnit(int omId) {
        return getAllPropertiesFromEntity(MathParameterEntities.ORGANIZATION_UNIT, omId);
    }

    public static List<EntityProperty> getAllPropertiesFromEntity(MathParameterEntities entity, int id) {
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(entity.getLink(), id, ENTITY_PROPERTIES));
        JSONObject json = getJsonFromUri(Projects.WFM, uri);
        return getListFromJsonObject(json, EntityProperty.class);
    }

    public static EntityProperty getEntityPropertyByKey(MathParameterEntities entity, int id, String key) {
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, makePath(entity.getLink(), id, ENTITY_PROPERTIES, key));
        JSONObject json;
        try {
            json = getJsonFromUri(Projects.WFM, uri);
        } catch (AssertionError | JSONException e) {
            return null;
        }
        if (json == null) {
            return null;
        } else {
            return getClassObjectFromJson(EntityProperty.class, json);
        }
    }
}

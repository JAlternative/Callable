package wfm.repository;

import org.json.JSONObject;
import utils.Projects;
import utils.tools.CustomTools;
import utils.tools.RequestFormers;
import wfm.models.PositionType;

import java.net.URI;
import java.util.List;
import java.util.NoSuchElementException;

import static utils.Links.POSITION_TYPES;
import static utils.tools.CustomTools.getRandomFromList;
import static utils.tools.CustomTools.randomItem;

public class PositionTypeRepository {

    private PositionTypeRepository() {}

    /**
     * Обращается в api/v1/position-types
     * @return список всех типов позиций для конкретного стенда
     */

    public static List<PositionType> getAllPositionTypes() {
        URI uri = RequestFormers.setUri(Projects.WFM, CommonRepository.URL_BASE, POSITION_TYPES);
        JSONObject object = RequestFormers.getJsonFromUri(Projects.WFM, uri);
        return CustomTools.getListFromJsonObject(object, PositionType.class);
    }

    public static PositionType randomPositionType() {
        return getRandomFromList(getAllPositionTypes());
    }

    public  static PositionType getPositionTypeById(int id) {
        return getAllPositionTypes()
                .stream()
                .filter(e -> e.getId() == id)
                .findFirst()
                .orElseThrow(NoSuchElementException:: new);
    }

    public static PositionType getPositionTypeByName(String name) {
        return getAllPositionTypes().stream()
                .filter(pt -> pt.getName().equals(name))
                .collect(randomItem());
    }

}

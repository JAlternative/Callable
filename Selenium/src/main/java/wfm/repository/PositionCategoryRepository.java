package wfm.repository;

import org.json.JSONObject;
import utils.Projects;
import utils.tools.CustomTools;
import utils.tools.Pairs;
import wfm.components.positioncategories.WorkGraphFilter;
import wfm.models.PositionCategory;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import static utils.Links.POSITION_CATEGORIES;
import static utils.tools.CustomTools.getRandomFromList;
import static utils.tools.RequestFormers.getJsonFromUri;

/**
 * @author Evgeny Gurkin 20.08.2020
 */
public class PositionCategoryRepository {

    private PositionCategoryRepository() {}

    /**
     * Возвращает список категорий позиций
     *
     * @param workGraphFilter - фильтр позиций
     */
    public static List<PositionCategory> getAllPositionCategoriesByFilter(WorkGraphFilter workGraphFilter) {
        Pairs.Builder pairs = Pairs.newBuilder().size(10000);
        if (workGraphFilter == WorkGraphFilter.FLOATING) {
            pairs.calculationMode("DYNAMIC");
        } else if (workGraphFilter == WorkGraphFilter.FIXED) {
            pairs.calculationMode("STATIC");
        }
        JSONObject jsonObject = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, POSITION_CATEGORIES, pairs.build());
        return CustomTools.getListFromJsonObject(jsonObject, PositionCategory.class);
    }

    public static PositionCategory randomPositionCategory() {
        return getRandomFromList(getAllPositionCategoriesByFilter(WorkGraphFilter.ALL));
    }

    public static PositionCategory randomDynamicPositionCategory() {
        return getRandomFromList(getAllPositionCategoriesByFilter(WorkGraphFilter.FLOATING));
    }

    public static PositionCategory getPositionCategoryById(int id) {
        return getAllPositionCategoriesByFilter(WorkGraphFilter.ALL)
                .stream()
                .filter(e -> e.getCategoryId() == id)
                .findFirst()
                .orElseThrow(NoSuchElementException :: new);
    }

    public static List<PositionCategory> getPositionCategoriesByPartialName(String namePart) {
        return getAllPositionCategoriesByFilter(WorkGraphFilter.ALL)
                .stream()
                .filter(e -> e.getName().contains(namePart))
                .collect(Collectors.toList());
    }
}

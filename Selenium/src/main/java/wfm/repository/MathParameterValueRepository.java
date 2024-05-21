package wfm.repository;

import org.apache.http.NameValuePair;
import org.json.JSONObject;
import utils.Params;
import utils.Projects;
import utils.tools.Pairs;
import wfm.components.orgstructure.MathParameterEntities;
import wfm.models.MathParameter;
import wfm.models.MathParameterValue;

import java.net.URI;
import java.util.List;

import static utils.Links.MATH_PARAMETER_VALUE;
import static utils.Links.MATH_PARAMETER_VALUES;
import static utils.tools.CustomTools.getClassObjectFromJson;
import static utils.tools.CustomTools.getListFromJsonObject;
import static utils.tools.RequestFormers.*;

public class MathParameterValueRepository {
    private MathParameterValueRepository() {
    }

    /**
     * Возвращает мэп с названиями и значениями мат параметров сотрудника
     *
     * @param entity тип сущности, параметры которой хотим получить
     * @param id     айди сущности
     */
    public static List<MathParameterValue> getMathParameterValuesForEntity(MathParameterEntities entity, int id) {
        List<NameValuePair> pairs = Pairs.newBuilder()
                .page(0)
                .size(1000)
                .build();
        String urlEnding = makePath(entity.getLink(), id, MATH_PARAMETER_VALUES);
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, urlEnding, pairs);
        JSONObject embedded = getJsonFromUri(Projects.WFM, uri);
        return getListFromJsonObject(embedded, MathParameterValue.class);
    }

    /**
     * Возвращает значение заданного матпараметра из списка имеющихся у сущности
     *
     * @param values имеющиеся значения матпараметров у сущности
     * @param param  матпараметр, для которого нужно найти значение
     */
    public static MathParameterValue getValueForParam(List<MathParameterValue> values, MathParameter param) {
        return values
                .stream()
                .filter(v -> v.getLink(Params.MATH_PARAMETER).equals(param.getSelfLink()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Объединяет два предыдущих метода
     *
     * @param entity тип сущности, параметры которой хотим получить
     * @param id     айди сущности
     * @param param  матпараметр, для которого нужно найти значение
     */
    public static MathParameterValue getMathParameterValueForEntity(MathParameterEntities entity, int id, MathParameter param) {
        return getValueForParam(getMathParameterValuesForEntity(entity, id), param);
    }
    /**
     * Возвращает мат параметр для подразделения (например, для функциональных ролей)
     *
     * @param entity тип сущности, параметры которой хотим получить
     * @param id     айди сущности
     * @param omId айи подразделения
     * @param outerId айди параметра
     */
    public static MathParameterValue getMathParameterValueForEntityInOrgUnit(MathParameterEntities entity, int id, int omId, String outerId) {
        List<NameValuePair> pairs = Pairs.newBuilder()
                .orgUnitId(omId)
                .outerId(outerId)
                .build();
        String urlEnding = makePath(entity.getLink(), id, MATH_PARAMETER_VALUE);
        URI uri = setUri(Projects.WFM, CommonRepository.URL_BASE, urlEnding, pairs);
        JSONObject embedded = getJsonFromUri(Projects.WFM, uri);
        return getClassObjectFromJson(MathParameterValue.class, embedded);
    }
}

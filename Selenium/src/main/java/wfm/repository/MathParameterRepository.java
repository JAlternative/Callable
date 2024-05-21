package wfm.repository;

import io.qameta.allure.Allure;
import org.apache.http.NameValuePair;
import org.json.JSONObject;
import utils.Params;
import utils.Projects;
import utils.tools.Pairs;
import wfm.components.orgstructure.MathParameterEntities;
import wfm.components.orgstructure.MathParameterValues;
import wfm.components.orgstructure.MathParameters;
import wfm.models.Employee;
import wfm.models.MathParameter;
import wfm.models.OrgUnit;

import java.util.List;
import java.util.stream.Collectors;

import static utils.Links.*;
import static utils.tools.CustomTools.*;
import static utils.tools.RequestFormers.*;

/**
 * @author Evgeny Gurkin 19.08.2020
 */
public class MathParameterRepository {

    private MathParameterRepository() {
    }

    /**
     * Возвращает список существующих матпараметров
     */
    public static List<MathParameter> getMathParameters() {
        List<NameValuePair> values = Pairs.newBuilder().size(1000).build();
        JSONObject params = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, makePath(MATH_PARAMETERS), values);
        return getListFromJsonObject(params, MathParameter.class);
    }

    /**
     * Возвращает список существующих матпараметров
     */
    public static List<MathParameter> getMathParametersWithEntity(MathParameterEntities entity) {
        List<MathParameter> mathParamList = getMathParameters();
        return mathParamList.stream()
                .filter(param -> param.getEntity().equals(entity.toString()))
                .filter(param -> !param.isHidden())
                .collect(Collectors.toList());
    }

    /**
     * Возвращает мат параметр по entity и outerId
     */
    public static MathParameter getMathParameterWithValue(MathParameterEntities entity, String outerId) {
        List<MathParameter> mathParamList = getMathParameters();
        return mathParamList.stream()
                .filter(param -> param.getEntity().equals(entity.toString()) && param.getOuterId().equals(outerId))
                .findAny()
                .orElse(null);
    }

    /**
     * Возвращает объект класса MathParameter по сущности и outerId
     */
    public static MathParameter getMathParameterWithValue(MathParameterValues mathParamValues) {
        List<MathParameter> mathParamList = getMathParameters();
        return mathParamList.stream()
                .filter(param -> param.getEntity().equals(mathParamValues.getEntity().toString()) && param.getOuterId().equals(mathParamValues.getOuterId()))
                .findAny()
                .orElse(null);
    }

    /**
     * Возвращает мат параметр по его айди
     */
    public static MathParameter getMathParameter(int id) {
        return new MathParameter(getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, makePath(MATH_PARAMETERS, id)));
    }

    /**
     * Берет все мат параметры у оргюнита
     */
    public static List<MathParameter> getMathParametersByOrgUnit(OrgUnit orgUnit) {
        JSONObject params = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, makePath(ORG_UNITS, orgUnit.getId(), MATH_PARAMETERS));
        return getListFromJsonObject(params, MathParameter.class);
    }

    /**
     * Берет случайное значение мат параметра отличное от того что уже есть у сотрудника,
     * работает только для мат параметров у которых значения на выбор из списка
     *
     * @param employee      - подопытный сотрудник
     * @param mathParameter - изменяемый параметр
     * @return класс значения содержащий имя значения и вариант значения
     */
    public static MathParameter.MathValue getAnotherMathValue(Employee employee, MathParameter mathParameter) {
        String path = makePath(EMPLOYEES, employee.getId(), MATH_PARAMETER_VALUES, mathParameter.getMathParameterId());
        List<MathParameter.MathValue> mathValues = mathParameter.getMathValues();
        try {
            JSONObject someObject = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, path);
            String apiValue = someObject.optString(Params.VALUE);
            if (apiValue != null) {
                mathValues = mathValues.stream().filter(mathValue -> !mathValue.getValue().equals(apiValue))
                        .collect(Collectors.toList());
            }
        } catch (AssertionError ignored) {
        }
        MathParameter.MathValue mathValue = getRandomFromList(mathValues);
        Allure.addAttachment("Выбор значения параметра: " + mathParameter.getName(), "text/plain",
                             "Для сотрудника " + employee.getFullName() + " было выбрано значение мат параметра \""
                                     + mathValue.getName() + "\" отличное от текущего.");
        return mathValue;
    }

    public static MathParameter.MathValue getValueFromMathParam(Employee employee, MathParameters mathParameter) {
        int employeeId = employee.getId();
        String path = makePath(EMPLOYEES, employeeId, MATH_PARAMETER_VALUES, mathParameter.getMathParamId());
        JSONObject someObject = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, path);
        return new MathParameter.MathValue(someObject);
    }

    public static String getMathParamValueFromOrgUnit(int omId, MathParameter mathParam) {
        return getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, makePath(ORG_UNITS, omId, MATH_PARAMETER_VALUES, mathParam.getMathParameterId())).toString();
    }

}

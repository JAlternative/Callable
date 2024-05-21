package wfm.repository;

import org.json.JSONObject;
import org.testng.Assert;
import utils.Links;
import utils.Projects;
import utils.downloading.TypeOfBatch;
import utils.tools.CustomTools;
import utils.tools.RequestFormers;
import wfm.models.Calculation;

import java.net.URI;
import java.util.List;

import static utils.tools.CustomTools.systemSleep;

public class CalculationRepository {

    private CalculationRepository() {}

    /** Возвращает список всех расчётов заданного типа
     *
     * @param type тип расчёта
     * */
    public static List<Calculation> getAllCalculations(TypeOfBatch type) {
        URI uri = RequestFormers.setUri(Projects.WFM, CommonRepository.URL_BASE, RequestFormers.makePath(Links.BATCH, type.getName()));
        JSONObject json = RequestFormers.getJsonFromUri(Projects.WFM, uri);
        return CustomTools.getListFromJsonObject(json, Calculation.class);
    }

    /** Возвращает список всех расчётов заданного типа в конкретном орюните
     *
     * @param type тип расчёта
     * @param omId id оргюнита
     * */
    public static Calculation getCalculationForOrgUnit(TypeOfBatch type, int omId) {
        URI uri = RequestFormers.setUri(Projects.WFM, CommonRepository.URL_BASE, RequestFormers.makePath(Links.BATCH, type.getName(), omId));
        JSONObject json;
        try {
            json = RequestFormers.getJsonFromUri(Projects.WFM, uri);
        } catch (AssertionError e) {
            throw new AssertionError("Расчет не появился в api");
        }
        return CustomTools.getClassObjectFromJson(Calculation.class, json);
    }

    /** Ожидает окончания расчёта заданного типа в конкретном орюните
     *
     * @param type тип расчёта
     * @param omId id оргюнита
     * @param timeout сколько ждем завершения расчета
     * */
    public static void waitForCalculation(TypeOfBatch type, int omId, int timeout) {
        Calculation calc;
        int i = 0;
        int wait = 10;
        do {
            calc = CalculationRepository.getCalculationForOrgUnit(type, omId);
            systemSleep(wait); //цикл
            i++;
        } while (!calc.isCompleted() && timeout / wait > i);
        if (!calc.isSuccessful()) {
            Assert.fail("Расчет завершился с ошибкой: " + calc.getError());
        }
    }
}
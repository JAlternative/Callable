package wfm.repository;

import org.json.JSONObject;
import utils.Projects;
import utils.tools.CustomTools;
import wfm.components.calculation.CalculationStatus;
import wfm.models.CalcJob;

import java.net.URI;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static utils.tools.CustomTools.systemSleep;
import static utils.tools.RequestFormers.getJsonFromUri;

public class CalculationStatusRepository {

    /**
     * Возвращает статус расчета
     *
     * @param header адрес расчета
     */
    public static CalculationStatus getCalculateStatus(String header) {
        JSONObject object = getJsonFromUri(Projects.WFM, URI.create(header));
        CalcJob calcJob = CustomTools.getClassObjectFromJson(CalcJob.class, object);
        String status = calcJob.getStatus();
        if (status != null) {
            CalculationStatus calculationStatus;
            if (status.equals("CREATED") || status.equals("RUNNING")) {
                calculationStatus = CalculationStatus.EXECUTING;
            } else if (status.equals("FINISHED") && calcJob.hasError()) {
                calculationStatus = CalculationStatus.ERROR;
            } else if (status.equals("FINISHED") && !calcJob.hasError()){
                    calculationStatus = CalculationStatus.SUCCESSFUL;
            } else {
                calculationStatus = CalculationStatus.OTHER;
            }
            return calculationStatus;
        }
        return null;
    }

    public static CalculationStatus waitForCalculation(String header, int timeout) {
        CalculationStatus status = getCalculateStatus(header);
        int wait = 10;
        for (int i = 0; (i < timeout / wait) && ((status == null) || (status.equals(CalculationStatus.EXECUTING))); i++) {
            systemSleep(wait); //цикл
            status = getCalculateStatus(header);
        }
        assertNotEquals(status, CalculationStatus.EXECUTING, "Расчет не завершился");
        assertNotEquals(status, CalculationStatus.ERROR, "Расчет завершился с ошибкой");
        assertEquals(status, CalculationStatus.SUCCESSFUL, "Расчет не найден в API");
        return status;
    }

    /**
     * Ожидает конца расчёта\перерасчёта расписания
     *
     * @param header адрес расчета
     * @return статус расчёта
     */
    public static CalculationStatus waitForCalculation(String header) {
        return waitForCalculation(header, 100);
    }

}

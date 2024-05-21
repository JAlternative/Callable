package wfm.repository;

import org.json.JSONObject;
import utils.Projects;
import utils.tools.Pairs;
import wfm.models.BioRecordResourceList;
import wfm.models.DateInterval;
import wfm.models.EmployeePosition;

import java.util.List;
import java.util.stream.Collectors;

import static utils.Links.BIO;
import static utils.Links.ROSTER;
import static utils.tools.CustomTools.getListFromJsonObject;
import static utils.tools.RequestFormers.getJsonFromUri;
import static utils.tools.RequestFormers.makePath;

/**
 * @author Evgeny Gurkin 19.08.2020
 */
public class BioRecordResourceListRepository {

    private BioRecordResourceListRepository() {}

    /**
     * Собирает отметки из био для указанного сотрудника, за временной отрезок
     */
    public static List<BioRecordResourceList> getRecordsResourceList(EmployeePosition employeePosition, DateInterval dateInterval) {
        int employeeId = employeePosition.getEmployee().getId();
        int roster = RosterRepository.getActiveRosterThisMonth(employeePosition.getOrgUnit().getId()).getId();
        JSONObject recordsJson = getJsonFromUri(Projects.WFM, CommonRepository.URL_BASE, makePath(BIO, "records", ROSTER, roster),
                Pairs.newBuilder().from(dateInterval.startDate).to(dateInterval.endDate).build());
        List<BioRecordResourceList> recordResource = getListFromJsonObject(recordsJson, BioRecordResourceList.class);
        return recordResource.stream().filter(rec -> rec.getEmployeeId() == employeeId).collect(Collectors.toList());
    }
}

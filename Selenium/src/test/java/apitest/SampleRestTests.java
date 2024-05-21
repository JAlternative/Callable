package apitest;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.parsing.Parser;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import utils.BuildInfo;
import utils.Params;
import utils.tools.Pairs;
import wfm.ApiRequest;
import wfm.PresetClass;
import wfm.components.schedule.ScheduleRequestStatus;
import wfm.models.*;
import wfm.repository.EmployeePositionRepository;
import wfm.repository.OrgUnitRepository;
import wfm.repository.ScheduleRequestAliasRepository;
import wfm.repository.ScheduleRequestRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static apitest.HelperMethods.*;
import static io.restassured.RestAssured.given;
import static utils.Links.*;
import static utils.Params.STATUS;
import static utils.tools.RequestFormers.makeJsonPath;
import static utils.tools.RequestFormers.makePath;

public class SampleRestTests {

    @BeforeClass(alwaysRun = true)
    public static void setUpSpecifications() {
        RestAssured.defaultParser = Parser.JSON;
    }

    @Test(groups = {"login_test"})
    public void loginTest() {
        given()
                .spec(adminRequestSpec)
                .accept(ContentType.ANY)
                .when()
                .get()
                .then()
                .statusCode(200);
    }

    @Test
    public void getBuildInfo() {
        BuildInfo info = given()
                .accept(ContentType.JSON)
                .auth().none()
                .when()
                .basePath("")
                .get("/build-info")
                .as(BuildInfo.class);
        System.out.println(info.getVersion());
    }

    @Test
    public void getOrgUnit() {
        OrgUnit unit = new ApiRequest.GetBuilder(makePath(ORG_UNITS, 186181)).send().returnPOJO(OrgUnit.class);
        System.out.println(unit.getName());
    }

    @Test
    public void getEp() {
        EmployeePosition ep = new ApiRequest.GetBuilder(makePath(EMPLOYEE_POSITIONS, 336266)).send().returnPOJO(EmployeePosition.class);
        System.out.println(ep.getEmployee().getLink("self"));
    }

    @Test
    public void getAdditionalWork() {
        AdditionalWork aw = new ApiRequest.GetBuilder(makePath(SHIFTS_ADD_WORK, 5)).send().returnPOJO(AdditionalWork.class);
        System.out.println(aw.getTitle());
    }

    @Test
    public void getAdditionalWorkRule() {
        AddWorkRule aw = new ApiRequest.GetBuilder(makePath(SHIFTS_ADD_WORK, 2, "rules", 2)).send().returnPOJO(AddWorkRule.class);
    }

    @Test
    public void getShift() {
        Shift s = new ApiRequest.GetBuilder(makePath(SHIFTS, 216879124)).send().returnPOJO(Shift.class);
        System.out.println(s);
    }

    @Test
    public void getRoster() {
        Roster r = new ApiRequest.GetBuilder(makePath(ROSTERS, 2234764)).send().returnPOJO(Roster.class);
        System.out.println(r);
    }

    @Test
    public void deserializeList() {
        Map<String, String> map = Pairs.newBuilder().from(LocalDate.of(2022, 5, 1))
                .to(LocalDate.of(2022, 5, 31)).buildMap();
        List<Roster> rosters = new ApiRequest.GetBuilder(makePath(ORG_UNITS, 183656, ROSTERS))
                .withParams(map)
                .send()
                .returnList(Roster.class, makeJsonPath(Params.EMBEDDED, ROSTERS));
        System.out.println(rosters.get(0).getCreationTime());
    }

    @Test
    public void editRequest() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        ScheduleRequest request = PresetClass.createScheduleRequestApi(ScheduleRequestStatus.APPROVED, false, omId);
        List<EmployeePosition> eps = EmployeePositionRepository.getEmployeePositions(omId);
        Employee employee = request.getEmployee();
        eps.removeIf(e -> e.getEmployee().equals(employee));
        request.setLinks(null).setStatus(ScheduleRequestStatus.NOT_APPROVED.name());
        List<ScheduleRequest> requestsBefore = ScheduleRequestRepository.getScheduleRequests(omId, new DateInterval());
        new ApiRequest.PutBuilder(makePath(SCHEDULE_REQUESTS, request.getId())).withBody(request).send();
        List<ScheduleRequest> requestsAfter = ScheduleRequestRepository.getScheduleRequests(omId, new DateInterval());
        Map<String, Object> changedValues = new HashMap<>();
        changedValues.put(STATUS, ScheduleRequestStatus.NOT_APPROVED.name());
        assertPut(requestsBefore, requestsAfter, changedValues);
    }

    @Test
    public void addRequest() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        LocalDate freeDate = PresetClass.getFreeDateFromNow(ep);
        int employeeId = ep.getEmployee().getId();
        ScheduleRequestAlias alias = ScheduleRequestAliasRepository.getRandomEnabledAlias();
        ScheduleRequest request = new ScheduleRequest()
                .setEmployeeId(employeeId)
                .setStatus(ScheduleRequestStatus.NOT_APPROVED.name())
                .setDateTimeInterval(new DateTimeInterval(LocalDateTime.now().withDayOfMonth(freeDate.getDayOfMonth()),
                                                          LocalDateTime.now().withDayOfMonth(freeDate.getDayOfMonth()).plusHours(5)))
                .setAlias(alias);
        List<ScheduleRequest> requestsBefore = ScheduleRequestRepository.getEmployeeScheduleRequests(employeeId, new DateInterval(), omId);
        new ApiRequest.PostBuilder(SCHEDULE_REQUESTS).withBody(request).send();
        List<ScheduleRequest> requestsAfter = ScheduleRequestRepository.getEmployeeScheduleRequests(employeeId, new DateInterval(), omId);
        assertPost(requestsBefore, requestsAfter, request);
    }
}
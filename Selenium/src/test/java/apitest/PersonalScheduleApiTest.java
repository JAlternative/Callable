package apitest;

import io.qameta.allure.*;
import io.qameta.allure.testng.Tag;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import reporting.TestListener;
import testutils.BaseTest;
import utils.Params;
import wfm.ApiRequest;
import wfm.PresetClass;
import wfm.components.schedule.ShiftTimePosition;
import wfm.components.schedule.SystemProperties;
import wfm.components.utils.PermissionType;
import wfm.models.EmployeePosition;
import wfm.models.OrgUnit;
import wfm.models.Shift;
import wfm.models.User;
import wfm.repository.EmployeePositionRepository;
import wfm.repository.OrgUnitRepository;
import wfm.repository.ShiftRepository;

import java.time.LocalDate;
import java.util.*;

import static common.Groups.*;
import static utils.Params.EMPLOYEE_POSITION_ID;
import static utils.Params.PUT_SHIFT_TO_EXCHANGE;
import static utils.tools.CustomTools.changeProperty;
import static wfm.repository.CommonRepository.URL_BASE;

@Listeners({TestListener.class})
public class PersonalScheduleApiTest extends BaseTest {

    public Map<String, ImmutablePair<String, String>> makeLinksForPuttingShiftToExchange(Shift shift) {
        Map<String, ImmutablePair<String, String>> links = new HashMap<>();
        links.put(Params.SELF, new ImmutablePair<>(Params.HREF, shift.getSelfLink()));
        return links;
    }

    @Test(groups = {"FS", G2, PS2,
            "@Before disable pre-publication checks",
            "@Before move to exchange not only shifts from exchange"},
            description = "Передача смены на биржу в Личном расписании")
    @Link(name = "Статья: \"Свободные смены\"", url = "https://wiki.goodt.me/x/_QUtD")
    @Tag(PS2)
    @Tag("FS-2")
    @Severity(SeverityLevel.MINOR)
    @Owner(SCHASTLIVAYA)
    @TmsLink("60205")
    public void moveShiftToExchange() {
        changeProperty(SystemProperties.ROSTER_PUBLISH_WITHOUT_NORMS_LACK, false);
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, true);
        Shift plannedShift = PresetClass.defaultShiftPreset(ep, ShiftTimePosition.FUTURE);
        LocalDate clickedDate = plannedShift.getStartDate();
        PresetClass.checkAndMakePublicationRoster(unit.getId());
        List<Shift> freeShiftsBefore = ShiftRepository.getFreeShifts(omId, clickedDate);
        User user = PresetClass.givePermissionsToTargetUser(Arrays.asList(PermissionType.SCHEDULE_PERSONAL,
                                                                          PermissionType.SCHEDULE_REQUEST_SHIFT_EXCHANGE),
                                                            ep.getEmployee().getUser(),
                                                            unit);
        ApiRequest.Builder builder = new ApiRequest.PutBuilder(plannedShift.getLink(PUT_SHIFT_TO_EXCHANGE))
                .withBody(makeLinksForPuttingShiftToExchange(plannedShift))
                .withUser(user);
        if (URL_BASE.contains("magnit")) {
            builder = builder.withStatus(201);
        }
        builder.send();
        Shift newShift = plannedShift.refreshShift();
        List<Shift> freeShiftsAfter = ShiftRepository.getFreeShifts(omId, clickedDate);
        HelperMethods.assertPut(newShift, Collections.singletonMap(EMPLOYEE_POSITION_ID, 0));
        HelperMethods.assertPost(freeShiftsBefore, freeShiftsAfter, newShift);
    }
}

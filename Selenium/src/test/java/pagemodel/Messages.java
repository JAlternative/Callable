package pagemodel;

import com.google.inject.Inject;
import guice.TestModule;
import io.qameta.allure.Allure;
import io.qameta.allure.Link;
import io.qameta.allure.Step;
import io.qameta.allure.TmsLink;
import io.qameta.allure.testng.Tag;
import io.qameta.atlas.webdriver.AtlasWebElement;
import org.openqa.selenium.NoSuchSessionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.Reporter;
import org.testng.annotations.*;
import org.testng.asserts.SoftAssert;
import pages.MessagesPage;
import reporting.TestListener;
import ru.yandex.qatools.matchers.webdriver.DisplayedMatcher;
import testutils.BaseTest;
import testutils.RoleWithCookies;
import utils.tools.Pairs;
import wfm.ApiRequest;
import wfm.PresetClass;
import wfm.components.orgstructure.MathParameterValues;
import wfm.components.schedule.ScheduleRequestStatus;
import wfm.components.schedule.ScheduleRequestType;
import wfm.components.schedule.ShiftTimePosition;
import wfm.components.schedule.SystemProperties;
import wfm.components.utils.PermissionType;
import wfm.components.utils.Role;
import wfm.components.utils.Section;
import wfm.models.*;
import wfm.repository.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static common.Groups.*;
import static utils.Links.SCHEDULE_REQUESTS;
import static utils.tools.CustomTools.changeProperty;
import static utils.tools.CustomTools.systemSleep;

@Guice(modules = {TestModule.class})
@Listeners({TestListener.class})
public class Messages extends BaseTest {

    private static final Section SECTION = Section.MESSAGES;
    private static final Logger LOG = LoggerFactory.getLogger(Messages.class);

    @Inject
    private MessagesPage msp;

    @AfterTest(alwaysRun = true, description = "Закрытие драйвера")
    private void closeDriver() {
        try {
            closeDriver(msp.getWrappedDriver());
        } catch (NoSuchSessionException e) {
            LOG.info("Сессии не существует");
        }
    }

    @AfterMethod(alwaysRun = true, description = "Очистить куки и подготовить новую вкладку")
    private void cleanCookies() {
        cleanCookiesAndOpenNewTab(msp.getWrappedDriver());
    }

    @BeforeMethod(alwaysRun = true,
            description = "Отключить двухфакторную аутентификацию на время теста")
    public void disableTwoFactorAuth() {
        changeProperty(SystemProperties.TWO_FACTOR_AUTH, false);
    }

    @BeforeMethod(alwaysRun = true, description = "Установка методики ожидания для драйвера")
    private void driverConfig() {
        setBrowserTimeout(msp.getWrappedDriver(), 30);
    }

    @Step("Кликнуть шеврон первого сообщения")
    private void clickMessageChevron(String subject, EmployeePosition ep, ScheduleRequestType requestType) {
        List<AtlasWebElement> messageChevrons = msp.messageListPanel().messageChevrons(subject);
        if (messageChevrons.isEmpty()) {
            Assert.fail("Уведомление о согласовании запроса отсутствия не пришло.");
        }
        messageChevrons.get(0).waitUntil("Список уведомлений пуст", DisplayedMatcher.displayed(), 5);
        messageChevrons.get(0).click();
        LOG.info("Уведомление должно содержать фразу: \"на привлечение {} {} на событие {}\"",
                 ep.getPosition().getName(), ep.getEmployee().getFullName(), requestType.getName());
        msp.messageListPanel().message(ep.getPosition().getName(), ep.getEmployee().getFullName(), requestType.getName())
                .should(String.format("Уведомление не содержит фразу: \"на привлечение %s %s на событие %s\"",
                                      ep.getPosition().getName(), ep.getEmployee().getFullName(), requestType.getName()), DisplayedMatcher.displayed(), 5);
        Allure.addAttachment("Фрагмент текста уведомления",
                             String.format("\"на привлечение %s %s на событие %s\"",
                                           ep.getPosition().getName(), ep.getEmployee().getFullName(), requestType.getName()));
    }

    @Step("Согласовать событие")
    private void clickApproveButton() {
        AtlasWebElement approveButton = msp.messageListPanel().approveButtons().get(0);
        approveButton.waitUntil("Кнопка согласования события не отобразилась", DisplayedMatcher.displayed(), 5);
        approveButton.click();
        systemSleep(4); //Тест падает до вызова метода
    }

    @Step("Проверка создания запроса \"{type.name}\" сотруднику {employeePosition} на дату {date} со статусом {status}")
    private void assertRequestAdded(EmployeePosition employeePosition, LocalDate date,
                                    ScheduleRequestType type, OrgUnit unit, ScheduleRequestStatus status) {
        List<ScheduleRequest> employeeRequest = ScheduleRequestRepository.getEmployeeScheduleRequests(employeePosition.getEmployee().getId(),
                                                                                                      new DateInterval(date), unit.getId());
        ScheduleRequest request = employeeRequest.size() != 0 ? employeeRequest.get(0) : null;
        Assert.assertNotNull(request, "Добавленный ранее запрос не был отображен в API");
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(request.getDateTimeInterval().getStartDate(), date, "Дата начала запроса не совпала");
        softAssert.assertEquals(type, request.getType(), "Тип запроса указанный при добавлении не совпал с текущим");
        if (status != null) {
            softAssert.assertEquals(status, request.getStatus(),
                                    String.format("Статус запроса не соответствует ожидаемому: %s", status));
        }
        softAssert.assertAll();
    }

    @Step("Проверка удаления смены сотрудника {ep} на дату {date} под согласованным событием")
    private void assertShiftDeleted(EmployeePosition ep, LocalDate date) {
        Assert.assertNull(ShiftRepository.getShift(ep, date, ShiftTimePosition.DEFAULT), "Смена под запросом отсутствия не удалилась");
    }

    @Test(groups = {"ABCHR5919-1", G2, SCHED9,
            "@Before disable merged view for planned and actual shifts",
            "@Before keep shifts under requests"},
            description = "Удаление смены под согласованным событием")
    @Link(name = "Статья: \"5919_Сохранение смены под согласованным событием. Создать АТ\"", url = "https://wiki.goodt.me/x/BIUtDg")
    @TmsLink("60297")
    @Tag("ABCHR5919-1")
    @Tag(SCHED9)
    private void deleteShiftUnderApprovedRequest() {
        ScheduleRequestType requestType = ScheduleRequestType.OFF_TIME;
        setUpRequest(requestType);
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts(true, false, false, true, false);
        if (unit.getChief(getServerDate()) == null) {
            PresetClass.appointEmployeeAChief(unit);
        }
        unit.refresh();
        PresetClass.changeOrSetMathParamValue(unit.getId(), MathParameterValues.ACCESS_TO_OVERWORK, true, true);
        User chief = unit.getChief(getServerDate()).getEmployee().getUser();
        List<EmployeePosition> positions = EmployeePositionRepository.getEmployeePositions(unit.getId())
                .stream().filter(e -> e.getEmployee().getUser().getId() != chief.getId()).collect(Collectors.toList());
        EmployeePosition requester = positions.stream().skip(new Random().nextInt(positions.size())).findFirst().orElse(null);
        Shift shift = PresetClass.defaultShiftPreset(requester, ShiftTimePosition.DEFAULT);
        LocalDate date = shift.getDateTimeInterval().getStartDate();
        List<PermissionType> permissionsRequester = new ArrayList<>(Arrays.asList(
                PermissionType.SCHEDULE_VIEW,
                PermissionType.SCHEDULE_EDIT,
                PermissionType.SCHEDULE_EDIT_WORKED,
                PermissionType.SCHEDULE_MANAGE_REQUESTS_CREATE,
                PermissionType.SCHEDULE_MANAGE_REQUESTS));
        List<PermissionType> permissionsApprover = new ArrayList<>(Arrays.asList(
                PermissionType.SCHEDULE_VIEW,
                PermissionType.SCHEDULE_EDIT,
                PermissionType.SCHEDULE_MANAGE_REQUESTS_APPROVE,
                PermissionType.ORG_UNIT_EMPLOYEE_PERSONALISATION,
                PermissionType.NOTIFY_APPROVE_REQUEST,
                PermissionType.NOTIFY_VIEW
        ));
        Role role = PresetClass.createSecondaryPermissionRole(permissionsApprover);
        new RoleWithCookies(msp.getWrappedDriver(), role, unit, chief).getPage(SECTION);
        createScheduleRequestAsUser(unit, requestType, requester, date, permissionsRequester);
        try {
            clickMessageChevron("Согласование запроса", requester, requestType);
        } catch (AssertionError er) {
            msp.getWrappedDriver().navigate().refresh();
            clickMessageChevron("Согласование запроса", requester, requestType);
        }
        clickApproveButton();
        assertShiftDeleted(requester, date);
        assertRequestAdded(requester, date, requestType, unit, ScheduleRequestStatus.APPROVED);
    }

    public void createScheduleRequestAsUser(OrgUnit unit,
                                            ScheduleRequestType requestType,
                                            EmployeePosition requestCreatorEp,
                                            LocalDate date,
                                            List<PermissionType> permissions) {
        //todo refactor apitest.HelperMethods.createScheduleRequest to take an optional user and use that if possible
        PresetClass.makeClearDate(requestCreatorEp, date);
        Role role = PresetClass.createCustomPermissionRole(permissions);
        User user = requestCreatorEp.getEmployee().getUser();
        PresetClass.givePermissionsToTargetUser(permissions, user, unit);
        PresetClass.addScheduleRequestTypeRights(role, requestType);

        Map<String, String> pairs = Pairs.newBuilder().calculateConstraints(true).buildMap();
        ScheduleRequestAlias alias = ScheduleRequestAliasRepository.getAlias(requestType);
        ScheduleRequest request = new ScheduleRequest()
                .setAlias(alias)
                .setAliasCode(alias.getOuterId())
                .setDateTimeInterval(new DateTimeInterval(date.atStartOfDay(), date.atTime(23, 59, 59)))
                .setEmployeeId(requestCreatorEp.getEmployee().getId())
                .setPositionId(requestCreatorEp.getPosition().getId())
                .setEmployeePositionId(requestCreatorEp.getId())
                .setStatus(ScheduleRequestStatus.APPROVED.name())
                .setRosterId(RosterRepository.getNeededRosterId(ShiftTimePosition.DEFAULT, new DateInterval(), unit.getId()).getId());
        new ApiRequest.PostBuilder(SCHEDULE_REQUESTS).withParams(pairs).withBody(request)
                .withUser(user)
                .send();
    }

    @Step("Пресет: выключить автоутверждение, включить согласование и привязку к назначению для запроса расписания типа \"{type}\"")
    public void setUpRequest(ScheduleRequestType type) {
        ScheduleRequestAlias alias = ScheduleRequestAliasRepository.getAlias(type);
        ITestContext c = Reporter.getCurrentTestResult().getTestContext();
        c.setAttribute(PresetClass.CHANGED_SCHEDULE_REQUEST_ALIAS, Collections.singletonList(alias.copy()));
        alias.setEnabled(true)
                .setAutoApprove(false)
                .setRequireApproval(true)
                .setBindToPosition(true);
        new ApiRequest.PutBuilder(alias.getSelfPath()).withBody(alias).send();
    }
}

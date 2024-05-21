package pagemodel;

import com.google.inject.Inject;
import guice.TestModule;
import io.qameta.allure.*;
import io.qameta.allure.testng.Tag;
import io.qameta.atlas.webdriver.AtlasWebElement;
import org.apache.commons.lang.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.*;
import pages.MessagesPage;
import reporting.TestListener;
import ru.yandex.qatools.matchers.webdriver.DisplayedMatcher;
import testutils.BaseTest;
import testutils.RoleWithCookies;
import utils.Links;
import utils.tools.Pairs;
import utils.tools.RequestFormers;
import wfm.ApiRequest;
import wfm.PresetClass;
import wfm.components.schedule.GraphStatus;
import wfm.components.schedule.ShiftTimePosition;
import wfm.components.schedule.SystemProperties;
import wfm.components.utils.PermissionType;
import wfm.components.utils.Role;
import wfm.components.utils.Section;
import wfm.models.*;
import wfm.repository.*;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;

import static common.Groups.*;
import static utils.Links.*;
import static utils.Params.*;
import static utils.tools.CustomTools.changeProperty;
import static utils.tools.CustomTools.systemSleep;
import static utils.tools.Format.UI_DATETIME_WITH_SPACE;
import static wfm.repository.CommonRepository.getToken;

@Guice(modules = {TestModule.class})
@Listeners({TestListener.class})
public class Notifications extends BaseTest {

    private static final Section SECTION = Section.MESSAGES;
    private static final Logger LOG = LoggerFactory.getLogger(ScheduleBoard.class);

    @Inject
    private MessagesPage msp;

    @BeforeMethod(alwaysRun = true,
            description = "Отключить двухфакторную аутентификацию на время теста")
    public void disableTwoFactorAuth() {
        changeProperty(SystemProperties.TWO_FACTOR_AUTH, false);
    }

    @BeforeMethod(alwaysRun = true, description = "Установка методики ожидания для драйвера")
    private void driverConfig() {
        setBrowserTimeout(msp.getWrappedDriver(), 30);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before enable shift exchange free notification"})
    public void enableFreeShiftsNotification() {
        changeProperty(SystemProperties.SHIFT_EXCHANGE_FREE_NOTIFICATION, true);
    }

    @AfterTest(alwaysRun = true, description = "Закрытие драйвера")
    private void closeDriver() {
        closeDriver(msp.getWrappedDriver());
    }

    @AfterMethod(alwaysRun = true, description = "Очистить куки и подготовить новую вкладку")
    private void cleanCookies() {
        cleanCookiesAndOpenNewTab(msp.getWrappedDriver());
    }

    @Step("Кликнуть шеврон первого сообщения c уведомлением о свободной смене")
    private void clickMessageChevron(String subject) {
        LOG.info("Кликаем на шеврон первого сообщения");
        List<AtlasWebElement> messageChevrons = msp.messageListPanel().messageChevrons(subject);
        if (messageChevrons.isEmpty()) {
            Assert.fail("Уведомление о согласовании запроса отсутствия не пришло.");
        }
        messageChevrons.get(0).waitUntil("Список уведомлений пуст", DisplayedMatcher.displayed(), 5);
        messageChevrons.get(0).click();
    }

    @Step("Проверить, что уведомление содержит информацию о подразделении, дате и времени смены")
    private void assertTextAndButtonInMessage(String subject, String msg) {
        assertMessageContent(msg);
        Assert.assertEquals(msp.messageListPanel().messageChevrons(subject).size(),
                            msp.messageListPanel().askForShiftButtons().size(),
                            "Уведомления о наличии свободной смены на бирже не содержат кнопку \"Запросить смену\"");
        Allure.addAttachment("Проверка", "Уведомление содержит кликабельную кнопку \"Запросить смену\"");

    }

    @Step("Проверить, что уведомление содержит текст \"{msg}\"")
    private void assertMessageContent(String msg) {
        String message = String.valueOf(msp.messageListPanel().allMessages().stream()
                                                .filter(m -> m.getAttribute("textContent").replaceAll("\n", "").contains(msg))
                                                .findAny()
                                                .orElse(null));
        Assert.assertNotNull(message,
                             String.format("Сообщение не содержит \"%s\"", msg));
        Allure.addAttachment("Проверка", String.format("Уведомление содержит текст %s", msg));
    }

    @Test(groups = {"ABCHR3584", G2, SCHED32,
            "@Before allow free shifts for own employees",
            "@Before enable shift exchange free notification",
            "@Before allow free shifts for own employees",
            "@Before disable pre-publication checks"},
            description = "Отправка уведомления о доступной смене на бирже в момент публикации")
    @Link(name = "Статья: \"3584_Уведомление о доступной смене на бирже всех, кто может запросить эту смену\"", url = "https://wiki.goodt.me/x/YQTND")
    @TmsLink("60196")
    @Tag("ABCHR3584-1")
    @Tag("ABCHR3584-2")
    @Tag(SCHED32)
    @Owner(BUTINSKAYA)
    private void sendNotificationOnAvailableFreeShift() {
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        int omId = unit.getId();
        PresetClass.publishGraphPreset(GraphStatus.NOT_PUBLISH, unit);
        EmployeePosition ep = EmployeePositionRepository.getRandomEmployeeWithCheckByApi(omId, null, false);
        String tag = unit.getTags().stream().findFirst().orElse(Links.getTestProperty(RELEASE) + "_" + unit.getOuterId());
        if (unit.getTags().isEmpty()) {
            PresetClass.addTagForOrgUnit(unit, tag);
        }
        if (ep.getEmployee().getActualTags().isEmpty() || !ep.getEmployee().getActualTags().contains(tag)) {
            PresetClass.addTagForEmployee(ep.getEmployee(), tag);
        }
        Position position = ep.getPosition();
        PositionCategory posCat = PositionCategoryRepository.getPositionCategoryById(ep.getPosition().getPositionCategoryId());
        if (position.getPositionGroupId() == 0) {
            PresetClass.changePosition(ep, posCat, PositionGroupRepository.randomPositionGroup(),
                                       PositionTypeRepository.getPositionTypeById(position.getPositionTypeId()));
            position = position.refreshPositions();
        }
        ep = ep.refreshEmployeePosition();
        PositionGroup posGroup = PositionGroupRepository.getPositionGroupById(position.getPositionGroupId());
        LocalDate freeShiftDate = ShiftTimePosition.FUTURE.getShiftsDateInterval().getRandomDateBetween();
        PresetClass.makeClearDate(ep, freeShiftDate);
        Shift freeShift = PresetClass.makeFreeShift(freeShiftDate, omId, null, posGroup, posCat, null, null, null, null);
        List<PermissionType> permissions = new ArrayList<>(Arrays.asList(PermissionType.NOTIFY_VIEW));
        Role role = PresetClass.createCustomPermissionRole(permissions);
        User user = PresetClass.addCustomRoleToUser(role, RandomStringUtils.randomAlphanumeric(10), unit, null, ep.getEmployee().getUser());
        String subject = "Есть свободная смена на Дату";
        PresetClass.publishGraphPreset(GraphStatus.PUBLISH, unit);
        waitForNotificationToAppearInApi(user, subject);
        new RoleWithCookies(msp.getWrappedDriver(), role, user).getPage(SECTION);
        clickMessageChevron(subject);
        DateTimeInterval interval = freeShift.getDateTimeInterval();
        String msg = String.format("В подразделении %s есть свободная смена с %s до %s.",
                                   unit.getName(), interval.getStartDateTime().format(UI_DATETIME_WITH_SPACE.getFormat()),
                                   interval.getEndDateTime().format(UI_DATETIME_WITH_SPACE.getFormat()));
        assertTextAndButtonInMessage(subject, msg);
    }

    private void waitForNotificationToAppearInApi(User user, String subject) {
        List response;
        String title = null;
        Map<String, String> pairs = Pairs.newBuilder().size(10000)
                .received(false)
                .deleted(false)
                .clientZoneOffset("+05:00")
                .buildMap();
        String path = RequestFormers.makePath(API, NOTIFY_LINK, Links.NOTIFICATIONS, USER, user.getId());
        int i = 0;
        do {
            systemSleep(2);
            ApiRequest request = new ApiRequest.GetBuilder(path)
                    .withHeaders(Collections.singletonMap("Wfm-Internal", getToken()))
                    .withParams(pairs)
                    .withUser(user)
                    .send();
            response = request.returnJsonValue(RequestFormers.makeJsonPath(EMBEDDED, "notificationResList"));
            if (response != null) {
                title = (String) response.stream()
                        .map(a -> ((LinkedHashMap) a).get(TITLE).toString().trim())
                        .filter(Objects::nonNull)
                        .filter(t -> t.toString().contains(subject))
                        .findAny()
                        .orElse(null);
            }
            i++;
        } while (title == null && i < 5);
        Assert.assertNotNull(title, String.format("Уведомление с темой \"%s\" не появилось в API", subject));
    }

    @Test(groups = {"ABCHR7911-1", G2, SCHED12, POCHTA,
            "@Before disable pre-publication checks", "@Before enable publication notifications for managers"},
            description = "Отправка уведомления о публикации графика начальнику родительского подразделения")
    @Link(name = "Статья: \"7911_Уведомления. Выделить отдельный пермишен на отправку уведомлений при публикации смен.\"", url = "https://wiki.goodt.me/x/d766Dw")
    @TmsLink("60195")
    @Tag("ABCHR7911-1")
    @Tag(SCHED12)
    @Owner(MATSKEVICH)
    private void sendNotificationChiefParentOrgUnitAboutSchedulePublication() {
        OrgUnit childUnit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        OrgUnit parentUnit = childUnit.getParentOrgUnit();
        EmployeePosition ep = parentUnit.getChief(getServerDate());
        if (ep == null) {
            ep = PresetClass.appointEmployeeAChief(parentUnit);
        }
        String subject = String.format("Расписание для %s утверждено", childUnit.getName());
        User user = ep.getEmployee().getUser();
        PresetClass.nonPublishAndApproveChecker(childUnit.getId());
        List<PermissionType> permissions = new ArrayList<>(Arrays.asList(PermissionType.SCHEDULE_VIEW,
                                                                         PermissionType.SCHEDULE_EDIT,
                                                                         PermissionType.SCHEDULE_PUBLISH_SHIFTS,
                                                                         PermissionType.NOTIFY_VIEW,
                                                                         PermissionType.NOTIFY_MANAGER,
                                                                         PermissionType.NOTIFY_ON_SCHEDULE_PUBLISH_SHIFTS));
        Role role = PresetClass.createCustomPermissionRole(permissions);
        RoleWithCookies rwc = new RoleWithCookies(msp.getWrappedDriver(), role, parentUnit, user);
        PresetClass.checkAndMakePublicationRoster(childUnit.getId());
        waitForNotificationToAppearInApi(user, subject);
        rwc.getPage(SECTION);
        clickMessageChevron(subject);
        String content = String.format("«%s», на «%s» 2023г. %d опубликован. Необходимо распечатать график, подписать его с работниками.",
                                       childUnit.getName(),
                                       getServerDate().getMonth().getDisplayName(TextStyle.FULL_STANDALONE, Locale.forLanguageTag("ru")).toLowerCase(),
                                       RosterRepository.getActiveRosterThisMonth(childUnit.getId()).getVersion());
        assertMessageContent(content);
    }
}

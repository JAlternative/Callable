package pagemodel;

import com.google.inject.Inject;
import common.DataProviders;
import guice.TestModule;
import io.qameta.allure.Link;
import io.qameta.allure.Step;
import io.qameta.allure.TmsLink;
import io.qameta.allure.testng.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.Guice;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import pages.StaffNumberPage;
import reporting.TestListener;
import ru.yandex.qatools.matchers.webdriver.DisplayedMatcher;
import testutils.BaseTest;
import testutils.RoleWithCookies;
import wfm.PresetClass;
import wfm.components.utils.PermissionType;
import wfm.components.utils.Role;
import wfm.components.utils.Section;
import wfm.models.Employee;
import wfm.models.OrgUnit;
import wfm.repository.EmployeeRepository;
import wfm.repository.OrgUnitRepository;

import java.util.ArrayList;
import java.util.List;

import static common.Groups.*;
import static utils.tools.CustomTools.systemSleep;

@Listeners({TestListener.class})
@Guice(modules = {TestModule.class})
public class StaffNumber extends BaseTest {
    private static final Section SECTION = Section.STAFF_NUMBER;

    private static final Logger LOG = LoggerFactory.getLogger(StaffNumber.class);

    @Inject
    private StaffNumberPage ns;

    @Step("Проверить, что переход не выполнился, отображается страница без заголовка или происходит переход на \"Расписание\"")
    private void assertModuleNotOpen() {
        LOG.info("Проверяем, что переход в раздел не выполнился");
        systemSleep(5); //Прогружает, нечем заменить sleep
        String url = ns.getWrappedDriver().getCurrentUrl();
        Assert.assertFalse(url.contains(SECTION.getUrlEnding()), "Есть доступ к разделу");
    }

    @Step("Проверить, что орг юнит \"{unit.name}\" совпадает с орг юнитом, на который у пользователя есть права")
    private void assertUnitLinkExists(OrgUnit unit) {
        LOG.info("Проверяем, что отображенный оргюнит {} совпадает с оргюнитом, на который у пользователя есть права", unit.getName());
        ns.staffCard().waitUntil("Список орг юнитов пуст", DisplayedMatcher.displayed(), 5);
        Assert.assertEquals(ns.staffCard().unitLink().get(0).getText(), unit.getName(), "Названия орг юнитов не совпали");
    }

    @Test(groups = {"ABCHR-8251", G2, OTHER13},
            description = "Доступность модуля \"Численность персонала\" в зависимости от пермишена",
            dataProvider = "true/false", dataProviderClass = DataProviders.class)
    @Link(name = "8251_Добавление пермишена для просмотра Численности персонала", url = "https://wiki.goodt.me/x/Puq6Dw")
    @Tag("ABCHR-8251")
    @TmsLink("60223")
    @Tag(OTHER13)
    public void accessToStaffNumber(boolean hasAccess) {
        List<PermissionType> permissions = new ArrayList<>();
        if (hasAccess) {
            permissions.add(PermissionType.OTHER_VIEW_CALCULATED_NUMBER_OF_STAFF);
        }
        Role role = PresetClass.createCustomPermissionRole(permissions);
        OrgUnit unit = OrgUnitRepository.getRandomOrgUnitsForShifts();
        Employee employee = EmployeeRepository.getRandomEmployeeWithAccount(false);
        RoleWithCookies rwc = new RoleWithCookies(ns.getWrappedDriver(), role, unit, employee.getUser());
        if (hasAccess) {
            rwc.getPage(SECTION);
            assertUnitLinkExists(unit);
        } else {
            ns.getWrappedDriver().manage().deleteAllCookies();
            //без этого не работает
            rwc.getPageWithoutWait(SECTION);
            assertModuleNotOpen();
        }

    }

    @AfterTest(alwaysRun = true, description = "Закрытие драйвера")
    private void closeDriver() {
        closeDriver(ns.getWrappedDriver());
    }

}

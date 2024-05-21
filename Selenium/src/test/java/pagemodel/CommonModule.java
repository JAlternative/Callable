package pagemodel;

import com.google.inject.Inject;
import common.DataProviders;
import guice.TestModule;
import io.qameta.allure.*;
import io.qameta.allure.testng.Tag;
import org.apache.commons.lang.RandomStringUtils;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.*;
import pages.LoginPage;
import reporting.TestListener;
import ru.yandex.qatools.matchers.webdriver.DisplayedMatcher;
import testutils.BaseTest;
import testutils.GoToPageSection;
import testutils.RoleWithCookies;
import utils.Links;
import utils.Projects;
import utils.authorization.CsvLoader;
import utils.tools.CustomTools;
import wfm.PresetClass;
import wfm.components.schedule.SystemProperties;
import wfm.components.utils.PermissionType;
import wfm.components.utils.Role;
import wfm.components.utils.RolesSections;
import wfm.components.utils.Section;
import wfm.models.*;
import wfm.repository.EmployeeRepository;
import wfm.repository.OrgUnitRepository;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static common.Groups.*;
import static org.hamcrest.Matchers.containsString;
import static ru.yandex.qatools.matchers.webdriver.TextMatcher.text;
import static utils.authorization.CookieTools.updateCookieDateTime;
import static utils.tools.CustomTools.changeProperty;

@Guice(modules = {TestModule.class})
@Listeners({TestListener.class})
public class CommonModule extends BaseTest {

    private final static String URL = Links.getTestProperty("release");
    private final static String URL_LOGIN = URL + "/login";
    private final static String URL_LOGIN_ERROR = URL_LOGIN + "?error";
    private final static String URL_PASSWORD_CHANGE = URL + "/password-change";
    private final static String COOKIE_DIR = "datainput/wfm/Cookies_WFM_0_superuser.data";
    private static WebDriverWait wait;
    private static final Logger LOG = LoggerFactory.getLogger(CommonModule.class);

    @Inject
    private LoginPage lp;

    @AfterClass(alwaysRun = true, description = "Закрытие драйвера")
    private void tearDown() {
        lp.getWrappedDriver().manage().deleteAllCookies();
        lp.getWrappedDriver().quit();
    }

    @DataProvider(name = "RolesList")
    private static Object[][] rolesAccessData() {
        int index = 0;
        Object[][] array = new Object[getSizeWithoutAccess()][];
        for (Role role : Role.values()) {
            for (RolesSections section : role.getRolesSections()) {
                array[index] = new Object[]{role, section};
                index++;
            }
        }
        return array;
    }

    @DataProvider(name = "RolesListWithoutAccess")
    private static Object[][] rolesWithoutAccessData() {
        int index = 0;
        Object[][] array = new Object[Role.values().length * RolesSections.values().length - getSizeWithoutAccess()][];
        for (Role role : Role.values()) {
            List<RolesSections> withoutAccess = new ArrayList<>(Arrays.asList(RolesSections.values()));
            withoutAccess.removeAll(role.getRolesSections());
            for (RolesSections section : withoutAccess) {
                array[index] = new Object[]{role, section};
                index++;
            }
        }
        return array;
    }

    @DataProvider(name = "endedPermission")
    private static Object[][] endedPermission() {
        RolesSections[] allValues = RolesSections.values();
        Object[][] array = new Object[allValues.length][];
        for (RolesSections sections : allValues) {
            array[sections.ordinal()] = new Object[]{sections};
        }
        return array;
    }

    @DataProvider(name = "login")
    private static Object[][] login() {
        Object[][] array = new Object[3][];
        array[0] = new Object[]{RandomStringUtils.randomAlphanumeric(10), null};
        array[1] = new Object[]{null, RandomStringUtils.randomAlphanumeric(10)};
        array[2] = new Object[]{RandomStringUtils.randomAlphanumeric(10), RandomStringUtils.randomAlphanumeric(10)};
        return array;
    }

    @BeforeGroups(alwaysRun = true, value = {"TEST-1022", "PD-Roles-2"})
    @Step("Авторизоваться под юзером, у которого закончились полномочия на просмотр модулей")
    private void auth() {
        new RoleWithCookies(lp.getWrappedDriver(), Role.THIRD, LocalDate.now().minusDays(new Random().nextInt(100) + 1))
                .getPage(Section.WELCOME);
    }

    @AfterGroups(value = "TI-6.3", description = "Удалить файл с куки", alwaysRun = true)
    private void changeCookieDate() {
        updateCookieDateTime(new File(COOKIE_DIR), LocalDateTime.now().minusHours(1));
    }

    @BeforeMethod(alwaysRun = true, description = "Установка методики ожидания для драйвера")
    private void driverConfig() {
        wait = new WebDriverWait(lp.getWrappedDriver(), 15);
        lp.getWrappedDriver().manage().timeouts().implicitlyWait(15, TimeUnit.SECONDS);
    }

    @BeforeMethod(alwaysRun = true, onlyForGroups = {"@Before change password on enter"})
    public void allowShiftExchangeOutstaffWithAccept() {
        changeProperty(SystemProperties.CHANGE_PASSWORD_ON_ENTER, true);
    }

    private static int getSizeWithoutAccess() {
        int size = 0;
        for (Role role : Role.values()) {
            size += role.getRolesSections().size();
        }
        return size;
    }

    private User addRoleToUser(User user, String password, List<PermissionType> permissions, boolean changePassword) {
        return PresetClass.addRoleToTargetUser(PresetClass.createCustomPermissionRole(permissions),
                                               OrgUnitRepository.getRandomAvailableOrgUnit(),
                                               LocalDate.now().plusMonths(1),
                                               PresetClass.changePassword(user, password, changePassword));
    }

    private void goTo(Section section) {
        new GoToPageSection(lp).getPage(section, 60);
    }

    @Step("Авторизоваться под пользователем: {username}, с паролем {password}")
    private void loginStep(String username, String password) {
        lp.loginPage().waitUntil("Форма для логина не отображена", DisplayedMatcher.displayed(), 15);
        if (username != null) {
            lp.loginPage().loginField().sendKeys(username);
        }
        if (password != null) {
            lp.loginPage().passwordField().sendKeys(password);
        }
    }

    @Step("Нажать на кнопку \"Войти\"")
    private void loginButtonClick() {
        lp.loginPage().loginButton().click();
    }

    @Step("Проверка что пользователь авторизовался")
    private void checkSuccessLogin() {
        WebDriverWait wait = new WebDriverWait(lp.getWrappedDriver(), 20);
        wait.until(ExpectedConditions.titleContains("Расписание"));
        lp.header().headerTitle().waitUntil("Не отобразился заголовк с надписью \"Расписание\"",
                                            text(containsString("Расписание")), 15);
    }

    @Step("Проверить, что пользователь НЕ авторизовался")
    private void checkNotSuccessLogin() {
        String message = "Неверный логин или пароль";
        Assert.assertTrue(lp.getWrappedDriver().getCurrentUrl().equals(URL_LOGIN_ERROR), "Не был совершен переход на страницу с ошибкой авторизации");
        Allure.addAttachment("Проверка перехода на страницу с ошибкой авторизации", String.format("Перешли на страницу с ошибкой авторизации %s", URL_LOGIN_ERROR));
        Assert.assertTrue(lp.loginPage().errorMessage().getText().contains(message),
                          String.format("Сообщение не содержит фразу \"%s\"", message));
        Allure.addAttachment("Проверка сообщения", String.format("Сообщение содержит фразу \"%s\"", message));

    }

    @Step("Проверить, что переход не выполнился, отображается страница без заголовка или происходит переход на \"Расписание\"")
    private void assertModuleNotOpen(boolean hasAccessScheduleBoard) {
        wait.withMessage("Текущий url адресс не содержит \"welcome\"")
                .until(ExpectedConditions.urlContains("/schedule-board"));
        String headerText = "";
        if (hasAccessScheduleBoard) {
            headerText = "Расписание";
        }
        lp.header().headerTitle().waitUntil("Отобразился неправильный заголовок",
                                            text(containsString(headerText)), 10);
    }

    @Step("Проверить, что переход не выполнился, отображается страница \"Добро пожаловать\"")
    private void assertModuleNotOpen() {
        wait.withMessage("Текущий url адресс не содержит \"welcome\"")
                .until(ExpectedConditions.urlContains("/schedule-board"));
        lp.header().headerTitle().waitUntil("Отобразился заголовок",
                                            text(containsString("")), 10);
    }

    @Step("Проверить, что гиперссылка \"Восстановление пароля\" не отображается")
    private void assertIsRecoveryPasswordButtonNotDisplayed() {
        Assert.assertThrows(ElementNotInteractableException.class, () -> {
            lp.loginPage().recoveryPassword().click();
        });
    }

    @Step("Нажать на кнопку раскрытия меню (три параллельные линии)")
    private void clickMenuButton() {
        lp.header().sectionSelectionMenu().click();
    }

    @Step("Нажать на \"Выход\"")
    private void clickLogoutButton() {
        lp.header().logoutButton().click();
    }

    @Step("В появившемся окне, подверждения завершения сеанса, нажать \"Да\"")
    private void confirmLogout() {
        lp.header().confirmLogoutButton().click();
    }

    @Step("Проверить, что был осуществлен выход из системы, отображется раздел авторизации")
    private void checkSystemLogout() {
        WebDriverWait wait = new WebDriverWait(lp.getWrappedDriver(), 20);
        wait.until(ExpectedConditions.titleIs("Вход"));
        lp.loginPage().loginButton().should("Не был осуществлен переход на страницу авторизации",
                                            DisplayedMatcher.displayed());
    }

    @Step("Перейти по URL: {url}")
    private void goToLogin(String url) {
        lp.getWrappedDriver().manage().deleteAllCookies();
        lp.getWrappedDriver().get(url);
        LOG.info("Перешли по URL {}", url);
    }

    @Test(groups = "login")
    public void loginTest() {
        Projects project = Projects.WFM;
        Role role = Role.ADMIN;
        String[] pairForLogin = CsvLoader.loginReturner(project, role);
        goTo(Section.LOGIN);
        loginStep(pairForLogin[0], pairForLogin[1]);
        loginButtonClick();
        checkSuccessLogin();
    }

    @Step("Изменить пароль при входе")
    private void changePassword(String password) {
        new WebDriverWait(lp.getWrappedDriver(), 5).until(ExpectedConditions.urlContains(URL_PASSWORD_CHANGE));
        lp.loginPage().changePasswordButton().waitUntil("Кнопка дя смены пароля не отобразилась",
                                                        DisplayedMatcher.displayed(), 5);
        lp.loginPage().newPassword().sendKeys(password);
        lp.loginPage().confirmPassword().sendKeys(password);
        lp.loginPage().changePasswordButton().click();
    }

    @Test(dataProvider = "RolesList", groups = {"TEST-949", "PD-Roles-1"}, description = "Просмотр модулей с доступом")
    private void rolesCheck(Role role, RolesSections section) {
        new RoleWithCookies(lp.getWrappedDriver(), role).getPage(section.getSection());
    }

    @Test(dataProvider = "endedPermission", groups = {"TEST-1022", "PD-Roles-3"}, description = "Просмотр при окончании срока действия роли")
    private void endedRolesCheck(RolesSections section) {
        goTo(section.getSection());
        assertModuleNotOpen();
    }

    @Test(dataProvider = "RolesListWithoutAccess", groups = {"TEST-949", "PD-Roles-2"}, description = "Просмотр модулей без доступа")
    private void rolesWithoutAccessCheck(Role role, RolesSections section) {
        new RoleWithCookies(lp.getWrappedDriver(), role).getPageWithoutWait(section.getSection());
        assertModuleNotOpen(role.getRolesSections().contains(RolesSections.SCHEDULE_BOARD));
    }

    @Test(groups = "TI-6.3", description = "Выход из системы")
    private void logout() {
        new RoleWithCookies(lp.getWrappedDriver(), Role.ADMIN).getPage(Section.WELCOME);
        clickMenuButton();
        clickLogoutButton();
        confirmLogout();
        checkSystemLogout();
    }

    @Test(groups = {"ABCHR3152", AUTH1, G2},
            description = "Ввод неверного логина",
            dataProvider = "login")
    @Link(name = "Статья: \"3152_Выводить информацию о том, что введен не верный логин и пароль\"", url = "https://wiki.goodt.me/x/ugotD")
    @Owner(BUTINSKAYA)
    @TmsLink("95356")
    @Tag(AUTH1)
    public void enterIncorrectLogin(String username, String password) {
        if (password == null) {
            addTag("ABCHR3152-1");
        } else if (username == null) {
            addTag("ABCHR3152-2");
            changeTestName("Ввод неверного пароля");
        } else {
            addTag("ABCHR3152-3");
            changeTestName("Ввод неверного логина и пароля");
        }
        goToLogin(URL_LOGIN);
        loginStep(username, password);
        loginButtonClick();
        checkNotSuccessLogin();
    }

    @Test(groups = {"ABCHR5531-1", AUTH1, G2},
            description = "Отсутствие гиперссылки \"Восстановление пароля\" в окне авторизации")
    @Link(name = "Статья: \"5531_Почта. Восстановление пароля\"", url = "https://wiki.goodt.me/pages/viewpage.action?pageId=234103686")
    @Owner(KHOROSHKOV)
    @TmsLink("100982")
    @Tag(AUTH1)
    @Tag("ABCHR5531-1")
    public void recoveryLinkAbsenceInLoginWindow() {
        changeProperty(SystemProperties.SHOW_PASSWORD_RECOVERY_ON_LOGIN, false);
        User user = EmployeeRepository.getRandomEmployeeWithAccount(false).getUser();
        goToLogin(URL_LOGIN);
        loginStep(user.getUsername(), RandomStringUtils.randomAlphanumeric(10));
        loginButtonClick();
        checkNotSuccessLogin();
        assertIsRecoveryPasswordButtonNotDisplayed();
    }

    @Test(groups = {"ABCHR4184-3", AUTH1, G2, IN_PROGRESS,
            "@Before disable change password in specified period",
            "@Before change password on enter"},
            description = "Смена пароля пользователя при входе с настройкой \"Нужна смена пароля\"")
    @Link(name = "Статья: \"4184_Добавить галку сброса пароля для пользователя\"", url = "https://wiki.goodt.me/x/vwj6D")
    @Owner(BUTINSKAYA)
    @TmsLink("118686")
    @Tag(AUTH1)
    @Tag("ABCHR4184-3")
    public void loginWithoutPasswordChange() {
        lp.getWrappedDriver().manage().deleteAllCookies();
        List<PermissionType> permissions = new ArrayList<>(Arrays.asList(PermissionType.SCHEDULE_VIEW));
        String oldPassword = RandomStringUtils.randomAlphanumeric(5);
        Employee employee = EmployeeRepository.getRandomEmployeeWithAccount(false);
        User user = addRoleToUser(employee.getUser(), oldPassword, permissions, false);
        goToLogin(URL_LOGIN);
        loginStep(user.getUsername(), oldPassword);
        loginButtonClick();
        checkSuccessLogin();
    }

    @Test(groups = {"ABCHR4184-3", AUTH1, G2, IN_PROGRESS,
            "@Before disable change password in specified period",
            "@Before change password on enter"},
            description = "Смена пароля пользователя при входе с настройкой \"Нужна смена пароля\"")
    @Link(name = "Статья: \"4184_Добавить галку сброса пароля для пользователя\"", url = "https://wiki.goodt.me/x/vwj6D")
    @Owner(BUTINSKAYA)
    @TmsLink("118686")
    @Tag(AUTH1)
    @Tag("ABCHR4184-3")
    public void loginWithPasswordChange() {
        lp.getWrappedDriver().manage().deleteAllCookies();
        List<PermissionType> permissions = new ArrayList<>(Arrays.asList(PermissionType.SCHEDULE_VIEW));
        String oldPassword = RandomStringUtils.randomAlphanumeric(5);
        Employee employee = EmployeeRepository.getRandomEmployeeWithAccount(false);
        User user = addRoleToUser(employee.getUser(), oldPassword, permissions, true);
        goToLogin(URL_LOGIN);
        loginStep(user.getUsername(), oldPassword);
        loginButtonClick();
        String newPassword = CustomTools.generatePassword();
        changePassword(newPassword);
        loginStep(user.getUsername(), newPassword);
        loginButtonClick();
        checkSuccessLogin();
    }
}
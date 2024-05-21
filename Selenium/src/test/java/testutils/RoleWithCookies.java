package testutils;

import io.qameta.allure.Allure;
import io.qameta.allure.Step;
import io.qameta.atlas.core.Atlas;
import io.qameta.atlas.webdriver.WebDriverConfiguration;
import org.apache.commons.lang.RandomStringUtils;
import org.hamcrest.Matchers;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pages.LoginPage;
import ru.yandex.qatools.matchers.webdriver.DisplayedMatcher;
import utils.Links;
import utils.Projects;
import utils.tools.CustomTools;
import wfm.PresetClass;
import wfm.components.schedule.SystemProperties;
import wfm.components.utils.PermissionType;
import wfm.components.utils.Role;
import wfm.components.utils.Section;
import wfm.models.OrgUnit;
import wfm.models.SystemProperty;
import wfm.models.User;
import wfm.repository.SystemPropertyRepository;

import java.time.LocalDate;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static common.ErrorMessagesForRegExp.FAILED_TO_GET_PAGE;
import static org.hamcrest.Matchers.*;
import static ru.yandex.qatools.matchers.webdriver.TextMatcher.text;
import static utils.authorization.CookieRW.getCookieWithCheck;

public class RoleWithCookies {

    private final static String BASE_URL = Links.getTestProperty("release");
    private static final Logger LOG = LoggerFactory.getLogger(RoleWithCookies.class);
    private final LoginPage lp;
    private final WebDriver driver;
    private final Role role;
    private final User user;

    /**
     * Заходит под ролью
     */
    public RoleWithCookies(WebDriver driver, Role role) {
        this.driver = driver;
        this.lp = new Atlas(new WebDriverConfiguration(driver)).create(driver, LoginPage.class);
        this.user = Role.ADMIN != role ? PresetClass.addCustomRoleToUser(role, RandomStringUtils.randomAlphanumeric(10), null, (OrgUnit[]) null) : null;
        this.role = role;
    }

    /**
     * Заходит под ролью у которой есть дата окончания ее действия
     */
    public RoleWithCookies(WebDriver driver, Role role, LocalDate endDate) {
        this.driver = driver;
        this.lp = new Atlas(new WebDriverConfiguration(driver)).create(driver, LoginPage.class);
        this.user = Role.ADMIN != role ? PresetClass.addCustomRoleToUser(role, RandomStringUtils.randomAlphanumeric(10), endDate, (OrgUnit[]) null) : null;
        this.role = role;
    }

    /**
     * Заходит под ролью у которой есть доступ к некоторым оргюнитам
     */
    public RoleWithCookies(WebDriver driver, Role role, OrgUnit... unit) {
        this.driver = driver;
        this.lp = new Atlas(new WebDriverConfiguration(driver)).create(driver, LoginPage.class);
        this.user = Role.ADMIN != role ? PresetClass.addCustomRoleToUser(role, RandomStringUtils.randomAlphanumeric(10), null, unit) : null;
        this.role = role;
    }

    /**
     * Заходит под ролью конкретного юзера у которого есть доступ к оргюниту
     */
    public RoleWithCookies(WebDriver driver, Role role, OrgUnit unit, User user) {
        this.driver = driver;
        this.lp = new Atlas(new WebDriverConfiguration(driver)).create(driver, LoginPage.class);
        this.user = Role.ADMIN != role ? PresetClass.addCustomRoleToUser(role, RandomStringUtils.randomAlphanumeric(10), unit, null, user) : null;
        this.role = role;
    }

    /**
     * Заходит под конкретным юзером, не проводя с ним никаких манипуляций
     */
    public RoleWithCookies(WebDriver driver, Role role, User user) {
        this.driver = driver;
        this.lp = new Atlas(new WebDriverConfiguration(driver)).create(driver, LoginPage.class);
        this.user = user;
        this.role = role;
    }

    @Step("Перейти в раздел \"{section.name}\"")
    public void getPage(Section section, int timeOutInSeconds, boolean withWait) {
        Cookie cookie = user != null ? getCookieWithCheck(Projects.WFM, role, user) : getCookieWithCheck(Projects.WFM);
        CustomTools.checkBrowserAvailability(driver);
        driver.get(BASE_URL + section.getUrlEnding());
        driver.manage().timeouts().pageLoadTimeout(3, TimeUnit.SECONDS);
        driver.get(BASE_URL + "/login");
        lp.loginPage().waitUntil("Страница авторизации не загрузилась.",
                                 DisplayedMatcher.displayed(), 3);
        driver.manage().deleteCookie(cookie);
        driver.manage().addCookie(cookie);
        driver.manage().timeouts().pageLoadTimeout(3, TimeUnit.SECONDS);
        driver.get(BASE_URL + section.getUrlEnding());
        if (withWait) {
            lp.header().headerTitle().waitUntil("Не был осуществлен переход. Страница не загрузилась.",
                                                Matchers.allOf(DisplayedMatcher.displayed(), Matchers.not(containsString(""))), timeOutInSeconds);
            lp.header().headerTitle().should("Не был осуществлен переход. Названия не совпали " + section.getName(),
                                             text(containsString(section.getName())), timeOutInSeconds);
        }
        String name = lp.header().headerTitle().getText();
        LOG.info("Перешли в раздел {}", name);
        addUserPermissionsToReport(name);
    }

    /**
     * Осуществляет переход в раздел с ожиданием прогрузки страницы
     *
     * @param section - раздел
     */
    public void getPage(Section section) {
        getPage(section, 35, true);
    }

    /**
     * Осуществляет переход в раздел без ожидания прогрузки страницы
     *
     * @param section - раздел
     */
    public void getPageWithoutWait(Section section) {
        getPage(section, 0, false);
    }

    /**
     * Переходит в раздел Расписание с правами указанными в конструкторе класса,
     *
     * @param orgUnitId - ID подразделения в расписание которого осуществляется вход
     */

    public void getSectionPageForSpecificOrgUnit(int orgUnitId, Section section) {
        boolean defaultValue = setTwoFactorAuth(false);
        Cookie cookie = user != null ? getCookieWithCheck(Projects.WFM, role, user) : getCookieWithCheck(Projects.WFM);
        CustomTools.checkBrowserAvailability(driver);
        driver.get(BASE_URL + section.getUrlEnding());
        driver.manage().timeouts().pageLoadTimeout(5, TimeUnit.SECONDS);
        driver.get(BASE_URL + "/login");
        lp.loginPage().waitUntil("Страница авторизации не загрузилась.",
                                 DisplayedMatcher.displayed(), 3);
        driver.manage().deleteCookie(cookie);
        driver.manage().addCookie(cookie);
        driver.manage().timeouts().pageLoadTimeout(3, TimeUnit.SECONDS);
        driver.get(BASE_URL + section.getUrlEnding() + "/" + orgUnitId);
        setTwoFactorAuth(defaultValue);
        driver.manage().addCookie(cookie);
        lp.header().headerTitle().waitUntil("Не был осуществлен переход. Страница не загрузилась.",
                                            Matchers.allOf(DisplayedMatcher.displayed(), Matchers.not(containsString(""))), 40);
        lp.header().headerTitle().waitUntil(FAILED_TO_GET_PAGE + section.getName(),
                                            text(containsString(section.getName())), 40);
        String name = lp.header().headerTitle().getText();
        assert user != null;
        LOG.info("Перешли в раздел {}. Пользователь {} с набором разрешений: {}", name, user.getUsername(), role.getPermissions());
        addUserPermissionsToReport(name);
    }

    /**
     * Добавление пользовательских прав в отчёт
     */
    private void addUserPermissionsToReport(String name) {
        String permissionList = "\nСписок разрешений пользователя: " +
                role.getPermissions().stream()
                        .map(PermissionType::getTitle)
                        .collect(Collectors.joining(", "));
        String attachment = (Role.ADMIN != role ?
                String.format("Перешли под юзером: %s с ролью \"%s\"\nОтображается раздел: \"%s\"",
                              user.getUsername(), role.getName(), name) : "Перешли под superuser") + permissionList;
        Allure.addAttachment("Пользовательские права", attachment);
    }

    /**
     * Устанавливает значение TWO_FACTOR_AUTH.
     * Если @Param value false, то после авторизации не появляются окна с надписью
     * "Выберите способ подтверждения".
     */
    private boolean setTwoFactorAuth(boolean value) {
        SystemProperties prop = SystemProperties.TWO_FACTOR_AUTH;
        SystemProperty systemProperty = SystemPropertyRepository.getSystemProperty(prop);
        Boolean defaultValue = (Boolean) systemProperty.getValue();
        if (value != defaultValue) {
            PresetClass.setSystemPropertyValue(prop, value);
        }
        return defaultValue;
    }
}




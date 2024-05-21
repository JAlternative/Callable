package testutils;

import io.qameta.allure.Allure;
import io.qameta.allure.Step;
import io.qameta.atlas.core.Atlas;
import io.qameta.atlas.webdriver.WebDriverConfiguration;
import io.qameta.atlas.webdriver.WebPage;
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
import wfm.components.utils.Section;
import wfm.models.OrgUnit;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.containsString;
import static ru.yandex.qatools.matchers.webdriver.TextMatcher.text;
import static utils.authorization.CookieRW.getCookieWithCheck;
import static wfm.components.utils.Section.LOGIN;

/**
 * @author Evgeny Gurkin 27.07.2020
 */

public class GoToPageSection {
    private final LoginPage lp;
    private WebDriver driver;
    private static final Logger LOG = LoggerFactory.getLogger(GoToPageSection.class);
    private final String BASE_URL = Links.getTestProperty("release");

    public GoToPageSection(WebPage page) {
        this.driver = page.getWrappedDriver();
        Atlas atlas = new Atlas(new WebDriverConfiguration(driver));
        this.lp = atlas.create(driver, LoginPage.class);
    }

    @Step("Перейти в раздел \"{section.name}\"")
    public void getPage(Section section, int timeOutInSeconds) {
        Cookie cookie = getCookieWithCheck(Projects.WFM);
        String url = BASE_URL + section.getUrlEnding();
        CustomTools.checkBrowserAvailability(driver);
        driver.get(url);
        driver.manage().timeouts().pageLoadTimeout(5, TimeUnit.SECONDS);
        driver.get(BASE_URL + "/login");
        lp.loginPage().waitUntil("Страница авторизации не загрузилась.",
                                 DisplayedMatcher.displayed(), 3);
        driver.manage().deleteCookieNamed("WFM_SESSION_ID");
        driver.manage().addCookie(cookie);
        driver.manage().timeouts().pageLoadTimeout(3, TimeUnit.SECONDS);
        driver.get(url);
        String name;
        if (section == LOGIN) {
            name = section.getName();
        } else {
            lp.header().headerTitle().waitUntil("Не был осуществлен переход. Страница не загрузилась.",
                                                Matchers.allOf(DisplayedMatcher.displayed(), Matchers.not(containsString(""))), timeOutInSeconds);
            lp.header().headerTitle()
                    .waitUntil(String.format("Не был осуществлен переход. Названия не совпали. %s", section.getName()),
                               text(containsString(section.getName())), timeOutInSeconds);
            name = lp.header().headerTitle().getText();
        }
        Allure.addAttachment("Переход по адресу", "Перешли по адресу: " + url);
        LOG.info("Перешли в раздел {}", name);
    }

    @Step("Перейти в раздел \"{section.name}\", в ОМ: {orgUnit.name}")
    public void goToOmWithoutUI(OrgUnit orgUnit, Section section) {
        Cookie cookie = getCookieWithCheck(Projects.WFM);
        String url1 = BASE_URL + section.getUrlEnding();
        CustomTools.checkBrowserAvailability(driver);
        driver.get(url1);
        driver.manage().timeouts().pageLoadTimeout(3, TimeUnit.SECONDS);
        driver.get(BASE_URL + "/login");
        lp.loginPage().waitUntil("Страница авторизации не загрузилась.",
                                 DisplayedMatcher.displayed(), 3);
        driver.manage().deleteCookieNamed("WFM_SESSION_ID");
        driver.manage().addCookie(cookie);
        Allure.addAttachment("Добавить куки", String.format("Куки: %s", driver.manage().getCookies()));
        LOG.debug("Куки: {}", driver.manage().getCookies());
        String url = BASE_URL + section.getUrlEnding() + "/" + orgUnit.getId();
        driver.get(url);
        lp.header().headerTitle().waitUntil("Не был осуществлен переход. Страница не загрузилась.",
                                            Matchers.allOf(DisplayedMatcher.displayed(), Matchers.not(containsString(""))), 30);
        String expectedName = section.getName() + ": " + orgUnit.getName();
        lp.header().headerTitle().waitUntil("Не был осуществлен переход. Названия не совпали. "
                                                    + section.getName(), text(containsString(expectedName)), 10);
        String name = lp.header().headerTitle().getText();
        Allure.addAttachment("Переход по адресу", "Перешли по адресу: " + url);
        LOG.info("Перешли в раздел {} ID № {}", name, orgUnit.getId());
    }
}

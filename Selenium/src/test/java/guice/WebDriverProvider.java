package guice;

import com.google.inject.Provider;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.Links;
import utils.tools.LocaleKeys;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;

import static org.openqa.selenium.remote.CapabilityType.TAKES_SCREENSHOT;

public class WebDriverProvider implements Provider<WebDriver> {

    private static final Logger LOG = LoggerFactory.getLogger(WebDriverProvider.class);
    private final boolean remoteSwitcher = Boolean.parseBoolean(LocaleKeys.getAssertProperty("driverType"));
    private final boolean localBrowser = Boolean.parseBoolean(LocaleKeys.getAssertProperty("localBrowser"));

    @Override
    public WebDriver get() {
        WebDriver driver = null;
        if (remoteSwitcher) {
            try {
                if (localBrowser) {
                    driver = new ChromeDriver(getChromeCapabilities());
                } else {
                    URL hubUrl = new URL(Links.getTestProperty("hubUrl"));
                    driver = new RemoteWebDriver(hubUrl, getRemoteDriverCapabilities());
                    // Раскомментируйте строку ниже, чтобы получать логи браузера прямо в IDE. Уровень логирования можно менять
                    //                        ((RemoteWebDriver) driver).setLogLevel(Level.SEVERE);
                }
            } catch (MalformedURLException e) {
                LOG.debug(e.toString());
            }
        } else {
            driver = new ChromeDriver(getChromeCapabilities());
        }
        return driver;
    }

    /**
     * implement a strategy to read the chrome capabilities
     *
     * @return опции для старта хрома для отладки в установленном стандарте fullHD
     */
    private ChromeOptions getChromeCapabilities() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--start-maximized");
        options.addArguments("--window-position=0,0");
        options.addArguments("--window-size=1920,1080");
        return options;
    }

    /**
     * implement a strategy to read the remote driver capabilities
     *
     * @return настройки для запуска драйвера на гриде
     */
    private Capabilities getRemoteDriverCapabilities() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--start-fullscreen");
        options.addArguments("--start-maximized");
        options.addArguments("--window-size=1600,900");
        options.addArguments("--proxy-server='direct://'");
        options.addArguments("--proxy-bypass-list=*");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-notifications");
        DesiredCapabilities capabilities = new DesiredCapabilities();
        capabilities.setCapability(CapabilityType.BROWSER_VERSION, "117.0");
        capabilities.setCapability(CapabilityType.PLATFORM_NAME, "LINUX");
        capabilities.setCapability(TAKES_SCREENSHOT, true);
        capabilities.setCapability("sessionTimeout", "5m");
        capabilities.setCapability("screenResolution", "1600x900x24");
        capabilities.setCapability(CapabilityType.SUPPORTS_APPLICATION_CACHE, false);

        /*
         * Меняйте следующие параметры при необходимости отладки
         * enableVNC включает трансляцию того, что происходит в окне браузера. Смотреть на selenoid-ui (порт 8080)
         * enableLog сохраняет файл лога из контейнера по пути *адрес селеноида*:4444/logs
         * name добавляет тесту тег с указанным именем на selenoid-ui
         */
        capabilities.setCapability("enableVNC", false);
        capabilities.setCapability("enableLog", false);
        //        capabilities.setCapability("name", "myTest");
        LoggingPreferences logPrefs = new LoggingPreferences();
        logPrefs.enable(LogType.BROWSER, Level.ALL);
        capabilities.setCapability(CapabilityType.LOGGING_PREFS, logPrefs);
        capabilities.setCapability(ChromeOptions.CAPABILITY, options);
        return capabilities;
    }
}

package guice.pagesproviders;

import com.google.inject.Inject;
import com.google.inject.Provider;
import io.qameta.atlas.core.Atlas;
import io.qameta.atlas.webdriver.WebDriverConfiguration;
import org.openqa.selenium.WebDriver;
import pages.SystemSettingsPage;
import utils.Links;

public class SystemSettingProvider implements Provider<SystemSettingsPage> {

    private WebDriver driver;

    @Inject
    public SystemSettingProvider(WebDriver driver) {
        this.driver = driver;
    }

    @Override
    public SystemSettingsPage get() {
        return new Atlas(new WebDriverConfiguration(driver, Links.getTestProperty("release"))).create(driver, SystemSettingsPage.class);
    }
}

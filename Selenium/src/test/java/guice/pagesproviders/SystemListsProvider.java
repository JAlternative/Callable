package guice.pagesproviders;

import com.google.inject.Inject;
import com.google.inject.Provider;
import io.qameta.atlas.core.Atlas;
import io.qameta.atlas.webdriver.WebDriverConfiguration;
import org.openqa.selenium.WebDriver;
import pages.SystemListsPage;
import utils.Links;

public class SystemListsProvider implements Provider<SystemListsPage> {
    private WebDriver driver;

    @Inject
    public SystemListsProvider(WebDriver driver) {
        this.driver = driver;
    }

    @Override
    public SystemListsPage get() {
        return new Atlas(new WebDriverConfiguration(driver, Links.getTestProperty("release"))).create(driver, SystemListsPage.class);
    }
}

package guice.pagesproviders;

import com.google.inject.Inject;
import com.google.inject.Provider;
import io.qameta.atlas.core.Atlas;
import io.qameta.atlas.webdriver.WebDriverConfiguration;
import org.openqa.selenium.WebDriver;
import pages.AnalyticsPage;
import utils.Links;

public class AnalyticsIntProvider implements Provider<AnalyticsPage> {

    private WebDriver driver;

    @Inject
    public AnalyticsIntProvider(WebDriver driver) {
        this.driver = driver;
    }

    @Override
    public AnalyticsPage get() {
        return new Atlas(new WebDriverConfiguration(driver, Links.getTestProperty("release"))).create(driver, AnalyticsPage.class);
    }
}

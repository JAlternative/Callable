package guice.pagesproviders;

import com.google.inject.Inject;
import com.google.inject.Provider;
import io.qameta.atlas.core.Atlas;
import io.qameta.atlas.webdriver.WebDriverConfiguration;
import org.openqa.selenium.WebDriver;
import pages.AddOrgUnitPage;
import pages.ReportsPage;
import utils.Links;

public class ReportsProvider implements Provider<ReportsPage> {

    private WebDriver driver;

    @Inject
    public ReportsProvider(WebDriver driver) {
        this.driver = driver;
    }

    @Override
    public ReportsPage get() {
        return new Atlas(new WebDriverConfiguration(driver, Links.getTestProperty("release"))).create(driver, ReportsPage.class);
    }
}

package guice.pagesproviders;

import com.google.inject.Inject;
import com.google.inject.Provider;
import io.qameta.atlas.core.Atlas;
import io.qameta.atlas.webdriver.WebDriverConfiguration;
import org.openqa.selenium.WebDriver;
import pages.RolesPage;
import utils.Links;

public class RolesManageProvider implements Provider<RolesPage> {

    private WebDriver driver;

    @Inject
    public RolesManageProvider(WebDriver driver) {
        this.driver = driver;
    }

    @Override
    public RolesPage get() {
        return new Atlas(new WebDriverConfiguration(driver, Links.getTestProperty("release"))).create(driver, RolesPage.class);
    }
}

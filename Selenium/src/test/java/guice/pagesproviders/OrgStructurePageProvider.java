package guice.pagesproviders;

import com.google.inject.Inject;
import com.google.inject.Provider;
import io.qameta.atlas.core.Atlas;
import io.qameta.atlas.webdriver.WebDriverConfiguration;
import org.openqa.selenium.WebDriver;
import pages.OrgStructurePage;
import utils.Links;

public class OrgStructurePageProvider implements Provider<OrgStructurePage> {

    private WebDriver driver;

    @Inject
    public OrgStructurePageProvider(WebDriver driver) {
        this.driver = driver;
    }

    @Override
    public OrgStructurePage get() {
        return new Atlas(new WebDriverConfiguration(driver, Links.getTestProperty("release"))).create(driver, OrgStructurePage.class);
    }
}

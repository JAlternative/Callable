package guice.pagesproviders;

import com.google.inject.Inject;
import com.google.inject.Provider;
import io.qameta.atlas.core.Atlas;
import io.qameta.atlas.webdriver.WebDriverConfiguration;
import org.openqa.selenium.WebDriver;
import pages.AddOrgUnitPage;
import utils.Links;

public class AddOrgUnitProvider implements Provider<AddOrgUnitPage> {

    private WebDriver driver;

    @Inject
    public AddOrgUnitProvider(WebDriver driver) {
        this.driver = driver;
    }

    @Override
    public AddOrgUnitPage get() {
        return new Atlas(new WebDriverConfiguration(driver, Links.getTestProperty("release"))).create(driver, AddOrgUnitPage.class);
    }
}

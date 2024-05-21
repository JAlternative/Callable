package guice.pagesproviders;

import com.google.inject.Inject;
import com.google.inject.Provider;
import io.qameta.atlas.core.Atlas;
import io.qameta.atlas.webdriver.WebDriverConfiguration;
import org.openqa.selenium.WebDriver;
import pages.BioPage;
import utils.Links;

public class BioControlModuleProvider implements Provider<BioPage> {

    private WebDriver driver;

    @Inject
    public BioControlModuleProvider(WebDriver driver) {
        this.driver = driver;
    }

    @Override
    public BioPage get() {
        return new Atlas(new WebDriverConfiguration(driver, Links.getTestProperty("release"))).create(driver, BioPage.class);
    }
}

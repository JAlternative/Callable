package guice.pagesproviders;

import com.google.inject.Inject;
import com.google.inject.Provider;
import io.qameta.atlas.core.Atlas;
import io.qameta.atlas.webdriver.WebDriverConfiguration;
import org.openqa.selenium.WebDriver;
import pages.SupportPage;
import utils.Links;

public class SupportProvider implements Provider<SupportPage> {

    private WebDriver driver;

    @Inject
    public SupportProvider(WebDriver driver) {
        this.driver = driver;
    }

    @Override
    public SupportPage get() {
        return new Atlas(new WebDriverConfiguration(driver, Links.getTestProperty("release"))).create(driver, SupportPage.class);
    }
}

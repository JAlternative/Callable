package guice.pagesproviders;

import com.google.inject.Inject;
import com.google.inject.Provider;
import io.qameta.atlas.core.Atlas;
import io.qameta.atlas.webdriver.WebDriverConfiguration;
import org.openqa.selenium.WebDriver;
import pages.StaffNumberPage;
import utils.Links;

public class StaffNumberProvider implements Provider<StaffNumberPage> {

    private WebDriver driver;

    @Inject
    public StaffNumberProvider(WebDriver driver) {
        this.driver = driver;
    }

    @Override
    public StaffNumberPage get() {
        return new Atlas(new WebDriverConfiguration(driver, Links.getTestProperty("release"))).create(driver, StaffNumberPage.class);
    }
}


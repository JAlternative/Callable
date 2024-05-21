package guice.pagesproviders;

import com.google.inject.Inject;
import com.google.inject.Provider;
import io.qameta.atlas.core.Atlas;
import io.qameta.atlas.webdriver.WebDriverConfiguration;
import org.openqa.selenium.WebDriver;
import pages.PositionTypesPage;
import utils.Links;

public class PositionTypesProvider implements Provider<PositionTypesPage> {

    private WebDriver driver;

    @Inject
    public PositionTypesProvider(WebDriver driver) {
        this.driver = driver;
    }

    @Override
    public PositionTypesPage get() {
        return new Atlas(new WebDriverConfiguration(driver, Links.getTestProperty("release"))).create(driver, PositionTypesPage.class);
    }
}
package guice.pagesproviders;

import com.google.inject.Inject;
import com.google.inject.Provider;
import io.qameta.atlas.core.Atlas;
import io.qameta.atlas.webdriver.WebDriverConfiguration;
import org.openqa.selenium.WebDriver;
import pages.BatchCalculationPage;
import utils.Links;

public class BatchCalculationProvider implements Provider<BatchCalculationPage> {

    private WebDriver driver;

    @Inject
    public BatchCalculationProvider(WebDriver driver) {
        this.driver = driver;
    }

    @Override
    public BatchCalculationPage get() {
        return new Atlas(new WebDriverConfiguration(driver, Links.getTestProperty("release"))).create(driver, BatchCalculationPage.class);
    }
}

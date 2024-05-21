package guice.pagesproviders;

import com.google.inject.Inject;
import com.google.inject.Provider;
import io.qameta.atlas.core.Atlas;
import io.qameta.atlas.webdriver.WebDriverConfiguration;
import org.openqa.selenium.WebDriver;
import pages.FteOperationValuesPage;
import utils.Links;

public class FteOperationValuesProvider implements Provider<FteOperationValuesPage> {

    private WebDriver driver;

    @Inject
    public FteOperationValuesProvider(WebDriver driver) {
        this.driver = driver;
    }

    @Override
    public FteOperationValuesPage get() {
        return new Atlas(new WebDriverConfiguration(driver, Links.getTestProperty("release"))).create(driver, FteOperationValuesPage.class);
    }
}

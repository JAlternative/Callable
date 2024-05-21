package guice.pagesproviders;

import com.google.inject.Inject;
import com.google.inject.Provider;
import io.qameta.atlas.core.Atlas;
import io.qameta.atlas.webdriver.WebDriverConfiguration;
import org.openqa.selenium.WebDriver;
import pages.MathParametersPage;
import utils.Links;

public class MathParametersProvider implements Provider<MathParametersPage> {

    private WebDriver driver;

    @Inject
    public MathParametersProvider(WebDriver driver) {
        this.driver = driver;
    }

    @Override
    public MathParametersPage get() {
        return new Atlas(new WebDriverConfiguration(driver, Links.getTestProperty("release"))).create(driver, MathParametersPage.class);
    }
}

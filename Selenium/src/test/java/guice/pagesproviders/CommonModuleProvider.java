package guice.pagesproviders;

import com.google.inject.Inject;
import com.google.inject.Provider;
import io.qameta.atlas.core.Atlas;
import io.qameta.atlas.webdriver.WebDriverConfiguration;
import org.openqa.selenium.WebDriver;
import pages.LoginPage;
import utils.Links;

public class CommonModuleProvider implements Provider<LoginPage> {

    private WebDriver driver;

    @Inject
    public CommonModuleProvider(WebDriver driver) {
        this.driver = driver;
    }

    @Override
    public LoginPage get() {
        return new Atlas(new WebDriverConfiguration(driver, Links.getTestProperty("release"))).create(driver, LoginPage.class);
    }
}

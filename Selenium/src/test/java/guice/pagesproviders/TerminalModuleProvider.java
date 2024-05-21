package guice.pagesproviders;

import com.google.inject.Inject;
import com.google.inject.Provider;
import io.qameta.atlas.core.Atlas;
import io.qameta.atlas.webdriver.WebDriverConfiguration;
import org.openqa.selenium.WebDriver;
import pages.TerminalPage;
import utils.Links;

public class TerminalModuleProvider implements Provider<TerminalPage> {

    private WebDriver driver;

    @Inject
    public TerminalModuleProvider(WebDriver driver) {
        this.driver = driver;
    }

    @Override
    public TerminalPage get() {
        return new Atlas(new WebDriverConfiguration(driver, Links.getTestProperty("release"))).create(driver, TerminalPage.class);
    }
}
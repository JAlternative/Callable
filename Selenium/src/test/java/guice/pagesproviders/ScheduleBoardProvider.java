package guice.pagesproviders;

import com.google.inject.Inject;
import com.google.inject.Provider;
import io.qameta.atlas.core.Atlas;
import io.qameta.atlas.webdriver.WebDriverConfiguration;

import org.openqa.selenium.WebDriver;
import pages.ScheduleBoardPage;
import utils.Links;

public class ScheduleBoardProvider implements Provider<ScheduleBoardPage> {

    private WebDriver driver;

    @Inject
    public ScheduleBoardProvider(WebDriver driver) {
        this.driver = driver;
    }

    @Override
    public ScheduleBoardPage get() {
        return new Atlas(new WebDriverConfiguration(driver, Links.getTestProperty("release"))).create(driver, ScheduleBoardPage.class);
    }
}

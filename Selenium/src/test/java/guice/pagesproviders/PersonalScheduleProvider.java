package guice.pagesproviders;

import com.google.inject.Inject;
import com.google.inject.Provider;
import io.qameta.atlas.core.Atlas;
import io.qameta.atlas.webdriver.WebDriverConfiguration;
import org.openqa.selenium.WebDriver;
import pages.PersonalSchedulePage;
import utils.Links;

public class PersonalScheduleProvider implements Provider<PersonalSchedulePage> {

    private WebDriver driver;

    @Inject
    public PersonalScheduleProvider(WebDriver driver) {
        this.driver = driver;
    }

    @Override
    public PersonalSchedulePage get() {
        return new Atlas(new WebDriverConfiguration(driver, Links.getTestProperty("release"))).create(driver, PersonalSchedulePage.class);
    }
}

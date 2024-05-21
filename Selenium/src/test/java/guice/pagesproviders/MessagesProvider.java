package guice.pagesproviders;

import com.google.inject.Inject;
import com.google.inject.Provider;
import io.qameta.atlas.core.Atlas;
import io.qameta.atlas.webdriver.WebDriverConfiguration;
import org.openqa.selenium.WebDriver;
import pages.MessagesPage;
import utils.Links;

public class MessagesProvider implements Provider<MessagesPage> {
    private WebDriver driver;

    @Inject
    public MessagesProvider(WebDriver driver) {
        this.driver = driver;
    }

    @Override
    public MessagesPage get() {
        return new Atlas(new WebDriverConfiguration(driver, Links.getTestProperty("release"))).create(driver, MessagesPage.class);
    }
}

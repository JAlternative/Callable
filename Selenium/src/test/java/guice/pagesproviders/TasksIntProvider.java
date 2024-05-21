package guice.pagesproviders;

import com.google.inject.Inject;
import com.google.inject.Provider;
import io.qameta.atlas.core.Atlas;
import io.qameta.atlas.webdriver.WebDriverConfiguration;
import org.openqa.selenium.WebDriver;
import pages.TasksPage;
import utils.Links;

public class TasksIntProvider implements Provider<TasksPage> {

    private WebDriver driver;

    @Inject
    public TasksIntProvider(WebDriver driver) {
        this.driver = driver;
    }

    @Override
    public TasksPage get() {
        return new Atlas(new WebDriverConfiguration(driver, Links.getTestProperty("release"))).create(driver, TasksPage.class);
    }
}

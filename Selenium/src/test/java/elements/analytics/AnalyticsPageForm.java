package elements.analytics;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface AnalyticsPageForm extends AtlasWebElement {

    @Name("Текст попАПа значения колонны графика")
    @FindBy(".//div[@show.bind=\"hint.show\"]//span[contains(last(),'')]")
    AtlasWebElement textOfNameParameter();
}

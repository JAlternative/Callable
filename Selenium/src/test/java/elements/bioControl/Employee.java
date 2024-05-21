package elements.bioControl;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface Employee extends AtlasWebElement {
    @Name("Имена")
    @FindBy(".//div[@class=\"grow scrolled\"]//div[@class=\"row\"]/div[1]/div[@class=\"text-large\"]")
    ElementsCollection<AtlasWebElement> empNames();


}
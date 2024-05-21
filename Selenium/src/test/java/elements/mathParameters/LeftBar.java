package elements.mathParameters;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface LeftBar extends AtlasWebElement {

    @Name("Кнопка, позволяет выбрать страницу")
    @FindBy(".//div[@class=\"mdl-layout__drawer-button\"]")
    AtlasWebElement menuButton();

}

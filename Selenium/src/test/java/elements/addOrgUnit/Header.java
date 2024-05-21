package elements.addOrgUnit;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface Header extends AtlasWebElement {

    @Name("Кнопка меню")
    @FindBy("//div[contains(@class, 'drawer-button')]")
    AtlasWebElement menuButton();

    @Name("Заголовок страницы")
    @FindBy("//span[contains(@title.bind, 'sanitizeHTML')]")
    AtlasWebElement text();

}

package elements.mathParameters;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface ResultBar extends AtlasWebElement {

    @Name("ПопАП с сообщением об ошибке")
    @FindBy("./div")
    AtlasWebElement errorPopUp();
}

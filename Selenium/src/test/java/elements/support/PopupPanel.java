package elements.support;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface PopupPanel extends AtlasWebElement {

    @Name("Поп-ап")
    @FindBy("./div[@class = 'mdl-snackbar__text']")
    AtlasWebElement popupText();


}

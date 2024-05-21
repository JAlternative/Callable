package elements.systemSettings;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface DialogWindow extends AtlasWebElement {

    @Name("Область ввода значения системного параметра")
    @FindBy("//textarea[@class='mdl-textfield__input au-target'][@id='system-property-value']")
    AtlasWebElement inputBox();

    @Name("Кнопка { itemTitle } ")
    @FindBy(".//button[@class='mdl-button mdl-button--primary au-target mdl-js-button'][contains(text(),'{{ itemTitle }}')]")
    AtlasWebElement popUpWindowButton(@Param("itemTitle") String itemTitle);
}



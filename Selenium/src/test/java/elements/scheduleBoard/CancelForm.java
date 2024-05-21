package elements.scheduleBoard;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface CancelForm extends AtlasWebElement {

    @Name("Кнопка \"Отменить\"")
    @FindBy(".//button[@class='mdl-snackbar__action']")
    AtlasWebElement buttonCancel();

}

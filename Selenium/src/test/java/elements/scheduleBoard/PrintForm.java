package elements.scheduleBoard;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface PrintForm extends AtlasWebElement {

    @Name("Кнопка \"Печать\"")
    @FindBy("//button[@click.trigger = 'print()']")
    AtlasWebElement printButton();

    @Name("Радиобатон \"Только график\"")
    @FindBy(".//label[2]/span[4]")
    AtlasWebElement radioButtonOnlySchedule();
}

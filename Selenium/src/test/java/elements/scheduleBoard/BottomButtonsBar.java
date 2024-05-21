package elements.scheduleBoard;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface BottomButtonsBar extends AtlasWebElement {

    @Name("Переключить на следующий месяц")
    @FindBy(".//button[3]")
    AtlasWebElement buttonBottomNext();

}

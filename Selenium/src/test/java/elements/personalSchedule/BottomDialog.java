package elements.personalSchedule;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface BottomDialog extends AtlasWebElement {

    @FindBy(".//button[@class='mdl-snackbar__action']")
    AtlasWebElement buttonAction();

}

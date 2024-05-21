package elements.personalSchedule;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface LeftPanel extends AtlasWebElement {

    @FindBy(".//button[@id='action-selected-date']")
    AtlasWebElement selectDate();

}

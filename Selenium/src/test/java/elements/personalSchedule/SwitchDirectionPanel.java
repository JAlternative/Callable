package elements.personalSchedule;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface SwitchDirectionPanel extends AtlasWebElement {

    @Name("Кнопка переключения вправо")
    @FindBy(".//button[@show.bind = 'showPreviousButton']")
    AtlasWebElement buttonPrevious();

    @Name("Кнопка переключения влево")
    @FindBy(".//button[@show.bind = 'showNextButton']")
    AtlasWebElement buttonNext();

}

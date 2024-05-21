package elements.scheduleBoard;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface ListOfTimetables extends AtlasWebElement {

    @Name("Кнопка опубликованной версии графика")
    @FindBy(".//span[contains(text(), \"публ.\")]/../../../ancestor::div[@click.trigger=\"selectRoster(roster)\"]")
    AtlasWebElement elementVerifyEdition();
}

package elements.orgstructure;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface UnitActionForm extends AtlasWebElement {
    @Name("Кнопка \"Добавить подразделение\"")
    @FindBy(".//button[@id ='org-unit-plus']")
    AtlasWebElement addUnitButton();
}

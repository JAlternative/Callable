package elements.fteOperationValues;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface EventTypeForm extends AtlasWebElement {

    @Name("Кнопка \"Сбросить\"")
    @FindBy(".//button[@click.trigger ='resetFilter(item)']")
    AtlasWebElement resetFilterButton();

    @Name("Кнопка \"Выбрать\"")
    @FindBy(".//button[@click.trigger ='applyFilter(item)']")
    AtlasWebElement applyFilterButton();

    @Name("Чекбокс эвента \"{eventType}\"")
    @FindBy(".//div[text() ='{{ eventType }}']/..//span[3]")
    AtlasWebElement checkboxByType(@Param("eventType") String eventType);

}

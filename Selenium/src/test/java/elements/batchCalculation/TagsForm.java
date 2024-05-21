package elements.batchCalculation;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.AtlasWebElement;

import java.util.List;

public interface TagsForm extends AtlasWebElement {

    @Name("Чекбокс для конкретного тега")
    @FindBy(".//div[text()='{{ tagName }}']/following-sibling::div/label[@mdl='checkbox']")
    AtlasWebElement tagCheckbox(@Param("tagName") String tagName);

    @Name("Кнопка \"Выбрать\"")
    @FindBy(".//button[@t='common.actions.select']")
    AtlasWebElement selectTagsButton();

    @Name("Кнопка \"Очистить\"")
    @FindBy(".//button[@t='common.actions.clear']")
    AtlasWebElement clearTagsButton();
}

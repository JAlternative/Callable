package elements.scheduleBoard;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface AttributeForm extends AtlasWebElement {
    @Name("Поле для ввода значения атрибута {title}")
    @FindBy("//label[contains(text(),'{{ title }}')]/../div")
    AtlasWebElement attributeValueInput(@Param("title") String title);

    @Name("Кнопка \"Сохранить\"")
    @FindBy("//div[contains(@class,'mdl-dialog--not-fullscreen-api-render au-target') and not(contains(@class,'aurelia-hide'))]//button[@click.trigger = 'save()']")
    AtlasWebElement saveButton();
}

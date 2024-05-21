package elements.positionTypes;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface DialogForm extends AtlasWebElement {

    @Name("Название типа позиции в которую мы прешли")
    @FindBy(".//h5")
    AtlasWebElement positionTypeName();

    @Name("Строка ввода имени типа позиции")
    @FindBy(".//input[@value.bind = 'positionType.name']")
    AtlasWebElement inputTypePositionName();

    @Name("Кнопка \"Сохранить\"")
    @FindBy(".//div[@class = 'mdl-list__item']//button[@click.trigger = 'save()']")
    AtlasWebElement saveButton();

    @Name("Поле ввода параметра: {parameterName}")
    @FindBy(".//*[contains(text(), '{{ parameterName }}')]/../input")
    AtlasWebElement inputParameter(@Param("parameterName") String parameterName);

    @Name("Список параметров в просмотре типа позиции")
    @FindBy(".//div[@class = 'mdl-list__item-primary-content']")
    ElementsCollection<AtlasWebElement> allParametersInView();


}

package elements.orgstructure;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface TagsForm extends AtlasWebElement {

    @Name("Кнопка \"Выбрать\"")
    @FindBy("./div[3]/button[1]")
    AtlasWebElement choseButton();

    @Name("Кнопка \"Сохранить\"")
    @FindBy("./div[4]/button[1]")
    AtlasWebElement saveButton();

    @Name("Кнопка \"Сбросить\"")
    @FindBy("./div[3]/button[2]")
    AtlasWebElement resetButton();

    @Name("Список названия параметров")
    @FindBy(".//div[contains(@class, 'primary')]")
    ElementsCollection<AtlasWebElement> listOfParametersText();

    @FindBy(".//input[@id]")
    ElementsCollection<AtlasWebElement> listOfTagsSearch();

    @Name("Список соответвующих чекбоксов для тегов")
    @FindBy(".//div[contains(text(), '{{ name }}')]/..//label")
    AtlasWebElement tagByName(@Param("name") String name);

    @Name("Поле ввода данных параметра \"{paramName}\"")
    @FindBy(".//div[text()='{{ paramName }}']/..//input")
    AtlasWebElement paramNameInput(@Param("paramName") String paramName);

    @Name("Кнопка \"Сохранить\" в форме параметров")
    @FindBy("//div[@class='mdl-dialog mdl-dialog--fit-content']//button[text()='Сохранить ']")
    AtlasWebElement saveParamButton();

}

package elements.orgstructure;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface OmCreateCardForm extends AtlasWebElement {
    @Name("Поле ввода названия подразделения")
    @FindBy(".//input[@id='org-unit-name']")
    AtlasWebElement employeeNameField();

    @Name("Поле ввода outerId")
    @FindBy(".//input[@id='org-unit-outerId']")
    AtlasWebElement outerIdField();

    @Name("Выпадающий список типов подразделений")
    @FindBy(".//div[@click.delegate='selectType(type)']")
    ElementsCollection<AtlasWebElement> unitTypeList();

    @Name("Тип подразделения в выпадающем списке")
    @FindBy(".//div[@menu='org-unit-type']/div[normalize-space(text())='{{ typeName }}']")
    AtlasWebElement unitType(@Param("typeName") String typeName);

    @Name("Поле ввода даты открытия или закрытия")
    @FindBy(".//label[contains(text(),'{{ openOrClose }}')]/preceding-sibling::input")
    AtlasWebElement dateOpenOrCloseInput(@Param("openOrClose") String openOrClose);

    @Name("Поле ввода типа подразделения")
    @FindBy(".//input[contains(@id, 'org-unit-type')]")
    AtlasWebElement unitTypeField();

    @Name("Поле ввода родительского подразделения")
    @FindBy(".//input[contains(@id, 'org-unit-parent')]")
    AtlasWebElement unitParentField();

    @Name("Поле \"Поиск\" родительского подразделения")
    @FindBy(".//input[@placeholder='Поиск']")
    AtlasWebElement searchField();

    @Name("Найденные элементы")
    @FindBy(".//div[@class='menu__item menu__item--wrap au-target']")
    ElementsCollection<AtlasWebElement> allSearchResults();

    @Name("Кнопка \"Создать\"")
    @FindBy(".//button[@click.trigger='add()' and normalize-space(text())='Создать']")
    AtlasWebElement createButton();

}

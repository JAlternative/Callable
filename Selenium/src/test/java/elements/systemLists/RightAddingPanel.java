package elements.systemLists;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface RightAddingPanel extends AtlasWebElement {

    @Name("Кнопка \"Создать\"")
    @FindBy(".//button[contains(@click.trigger, 'save')]")
    AtlasWebElement saveButton();

    @Name("Кнопка \"Создать\"")
    @FindBy(".//button[@click.trigger='remove()']")
    AtlasWebElement removeButton();

    @Name("Поле для ввода кода")
    @FindBy("//input[@id='shift-comment-code']")
    AtlasWebElement codeInputField();

    @Name("Поле для ввода кода")
    @FindBy("//input[@id='shift-comment-title']")
    AtlasWebElement nameInputField();

    @Name("Поле \"Функциональная роль\"")
    @FindBy("//input[@id='break-rule-position-group-select']")
    AtlasWebElement functionalRoles();

    @Name("Выпадающий список доступных функциональных ролей")
    @FindBy("//div[contains(@class, 'au-target is-visible')]//div[@class='menu__item au-target']")
    ElementsCollection<AtlasWebElement> getFunctionalRolesDropdownOptions();
}

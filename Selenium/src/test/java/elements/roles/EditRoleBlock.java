package elements.roles;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface EditRoleBlock extends AtlasWebElement {

    @Name("Поле выбора роли для сотрудника")
    @FindBy(".//input[@value.bind = 'getRoleName(role._links.userRole.href)']")
    AtlasWebElement roleInput();

    @Name("Название роли")
    @FindBy(".//div[contains(@class.bind, 'role._links.userRole.href')]")
    AtlasWebElement roleValue();

    @Name("Кнопка календаря {dateVariant}")
    @FindBy(".//div[@class='mdl-list__item mdl-list__item--flat']//label[text()='{{ dateVariant }}']/following-sibling::button[not(contains(@class,'aurelia-hide'))]")
    AtlasWebElement calendarButton(@Param("dateVariant") String dateVariant);

    @Name("Строка ввода {dateVariant} роли")
    @FindBy(".//div[@class='mdl-list__item mdl-list__item--flat']//label[text()='{{ dateVariant }}']/preceding-sibling::input")
    AtlasWebElement calendarInput(@Param("dateVariant") String dateVariant);

    @Name("Кнопка \"Выбрать ОЮ\"")
    @FindBy(".//*[contains(@class,'mdl-button') and contains(text(), 'выбрать ОЮ')]")
    AtlasWebElement selectOrgUnitButton();

    @Name("Кнопка \"Добавить ОЮ\"")
    @FindBy(".//*[contains(@class,'mdl-button') and contains(text(), 'добавить ОЮ')]")
    AtlasWebElement addOrgUnitButton();

    @Name("Поля выбора оргюнита для роли сотрудника")//fixme broken?
    @FindBy("//div[@show.bind]//div[not(contains(@class,'-'))]//input[@value.bind = 'orgUnit.name' ]")
    ElementsCollection<AtlasWebElement> orgUnitInput();

    @Name("Кнопка удаления роли")
    @FindBy(".//button[@click.trigger='deleteItem(model.roles, $index)']")
    AtlasWebElement roleDeleteButton();

    @Name("Кнопки удаления оргюнитов")
    @FindBy("//span[text()='Подразделение']/../../button")
    ElementsCollection<AtlasWebElement> orgUnitDeleteButtons();

}

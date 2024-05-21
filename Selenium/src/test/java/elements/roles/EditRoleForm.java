package elements.roles;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface EditRoleForm extends AtlasWebElement {
    @Name("Все блоки редактирования ролей сотрудника")
    @FindBy("//label[text()='Роль' and @class='mdl-textfield__label au-target']/../../../../..")
    ElementsCollection <EditRoleBlock> roleEditBlocks();

    @Name("Блок редактирования {order}-й роли сотрудника")
    @FindBy("(//label[text()='Роль' and @class='mdl-textfield__label au-target']/../../../../..)[{{ order }}]")
    EditRoleBlock roleEditBlock(@Param("order") int order);

    @Name("Все поля выбора роли для сотрудника")
    @FindBy(".//input[@value.bind = 'getRoleName(role._links.userRole.href)']")
    ElementsCollection<AtlasWebElement> allRoleInputs();

    @Name("Кнопки выбора ролей")//broken
    @FindBy(".//div[contains(@class, 'is-visible')]/div[contains(@click.trigger, 'addUniqRole') ]")
    ElementsCollection<AtlasWebElement> allRoles();

    @Name("Кнопка роли с названием: {roleName}")//broken
    @FindBy(".//div[contains(@class, 'visible')]/div[normalize-space(text()) = '{{ roleName }}']")
    AtlasWebElement roleNameButton(@Param("roleName") String roleName);

    @Name("Кнопка \"Изменить\"")
    @FindBy(".//button[@click.trigger = 'save(0)']")
    AtlasWebElement saveButton();

    @Name("Кнопка \"Отменить\"")
    @FindBy(".//button[@click.trigger = 'editTrigger(0)']")
    AtlasWebElement cancelButton();

    @Name("Нижний блок")
    @FindBy(".//button[@click.trigger = 'editTrigger(0)']/..")
    AtlasWebElement buttonsBlock();

    @Name("Спиннер для меню роли")
    @FindBy("//div[@class='mdl-spinner__circle']")
    AtlasWebElement spinner();

    @Name("Красная подсветка строки оргюнита")
    @FindBy(".//div[contains(@class, 'invalid') and contains (@class.bind, 'orgUnit.name')]")
    AtlasWebElement redLineInOrgUnitInput();

    @Name("Красная подсветка строки роли")
    @FindBy(".//div[contains(@class, 'invalid') and contains (@class.bind, 'role.role.name')]")
    AtlasWebElement redLineInRoleInput();

    @Name("Надпись в блоке ролей")
    @FindBy(".//div[@t ='common.noData']")
    AtlasWebElement titleInRoleBlock();

    @Name("Красный текст ошибки под датой начала")
    @FindBy(".//div[contains(@class, 'invalid')]/label[text()='Дата начала']/..//input[contains(@id, 'date-input')]/..//span[@class = 'mdl-textfield__error']")
    AtlasWebElement redTextUnderStartDate();

    @Name("Красный текст ошибки под датой окончания")
    @FindBy(".//div[contains(@class, 'invalid')]/label[text()='Дата окончания']/..//input[contains(@id, 'date-input')]/..//span[@class = 'mdl-textfield__error']")
    AtlasWebElement redTextUnderEndDate();

}

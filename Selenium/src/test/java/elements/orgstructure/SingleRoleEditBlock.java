package elements.orgstructure;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface SingleRoleEditBlock extends AtlasWebElement {
    @Name("Поле выбора роли для сотрудника")
    @FindBy(".//input[contains(@value.bind , 'getRoleName')]")
    AtlasWebElement roleInput();

    @Name("Пункты выпадающего списка ролей")
    @FindBy(".//div[contains(@class, 'is-visible')]/div[contains(@click.trigger, 'addUniqRole') ]")
    ElementsCollection<AtlasWebElement> allRoles();

    @Name("Кнопка роли с названием \"{roleName}\" в выпадающем списке")
    @FindBy(".//div[contains(@class, 'visible')]/div[normalize-space(text()) = '{{ roleName }}']")
    AtlasWebElement roleNameButton(@Param("roleName") String roleName);

    @Name("Кнопка календаря {dateVariant}")
    @FindBy(".//label[text() = '{{ dateVariant }}']/../button[not(contains(@class, 'aurelia-hide'))]")
    AtlasWebElement calendarButton(@Param("dateVariant") String dateVariant);

    @Name("Строка ввода {dateVariant}")
    @FindBy(".//label[text() = '{{ dateVariant }}']/../input")
    AtlasWebElement calendarInput(@Param("dateVariant") String dateVariant);

    @Name("Кнопка удаления роли")
    @FindBy(".//button[@click.trigger='deleteItem(model.roles, $index)'][not(@style='visibility: hidden;')]")
    AtlasWebElement roleDeleteButton();

    @Name("Кнопки удаления оргюнитов роли")
    @FindBy(".//button[contains(@click.trigger, 'deleteItem(role.orgUnits')]")
    ElementsCollection<AtlasWebElement> orgUnitDeleteButton();

    @Name("Кнопка \"Больше\"")
    @FindBy(".//button[@click.trigger='loadOrgUnits(role)']")
    AtlasWebElement seeMoreButton();

    @Name("Красная подсветка строки оргюнита")
    @FindBy(".//div[contains(@class, 'invalid') and contains (@class.bind, 'orgUnit.name')]")
    AtlasWebElement redLineInOrgUnitInput();

    @Name("Красная подсветка строки роли")
    @FindBy(".//div[contains(@class , 'is-invalid')]//span[@class = 'mdl-textfield__error']")
    AtlasWebElement redLineInRoleInput();

    @Name("Красный текст ошибки под датой")
    @FindBy(".//div[contains(@class, 'invalid')]/span[@class = 'mdl-textfield__error']")
    AtlasWebElement redTextUnderDate();
}

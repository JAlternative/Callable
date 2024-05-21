package elements.orgstructure;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.AtlasWebElement;

import java.util.List;
/**
 * @author Evgeny Gurkin 17.07.2020
 */
public interface RoleForm extends AtlasWebElement {

    @Name("Поле ввода названия роли")
    @FindBy(".//input[@id ='role-name']")
    AtlasWebElement roleNameInput();

    @Name("Поле ввода описания роли")
    @FindBy(".//input[@id ='role-description']")
    AtlasWebElement roleDescriptionInput();

    @Name("Кнопка \"Создать\"")
    @FindBy(".//button[@t= 'common.actions.create']")
    AtlasWebElement createButton();

    @Name("Кнопка \"Изменить\" при изменении названия и описания роли")
    @FindBy(".//button[@click.trigger= 'save(0)']")
    AtlasWebElement changeButton();

    @Name("Строка описания Роли")
    @FindBy(".//span[@class = 'mdl-list__item-sub-title']")
    AtlasWebElement roleDescription();

    @Name("Строка описание Роли")
    @FindBy(".//i[@class = 'mdi mdi-account-key mdl-list__item-icon']/../span[1]")
    AtlasWebElement roleName();

    @Name("Кнопка удаления роли")
    @FindBy(".//button[@click.trigger = 'remove()']")
    AtlasWebElement deleteRoleButton();

    @Name("Кнопка редактирования роли")
    @FindBy(".//button[@click.trigger = 'editTrigger(0, groups[0])']")
    AtlasWebElement editRolePencilButton();

    @Name("Шеврон группы разрешений \"{groupName}\"")
    @FindBy(".//*[text() = '{{ groupName }}']/../button[2]")
    AtlasWebElement permissionChevron(@Param("groupName") String groupName);

    @Name("Кнопка редактирования группы разрешений \"{groupName}\"")
    @FindBy(".//*[text() = '{{ groupName }}']/../button[1]")
    AtlasWebElement permissionPencilButton(@Param("groupName") String groupName);

    @Name("Чекбокс разрешения \"{name}\"")
    @FindBy(".//div[@class ='mdl-list__group au-target']//*[text() = '{{ name }}']/../..//span[contains(@class ,'mdl-checkbox__ripple')]")
    AtlasWebElement permissionCheckBox(@Param("name") String name);

    @Name("Кнопка \"Изменить\" при добавлении или удаления разрешений")
    @FindBy(".//div[@class ='mdl-list__group au-target']//button[@click.trigger= 'save($index + 1)']")
    AtlasWebElement changePermissionButton();

    @Name("Галочка у разрешения группы \"{group}\" с названием \"{name}\"")
    @FindBy(".//*[text() ='{{ group }}']/../..//div[@show.bind = 'show[$index + 1]']//span[text()= '{{ name }}']/../..//i[@class.bind]")
    AtlasWebElement permissionCheck(@Param("group") String group, @Param("name") String name);

    @Name("Кнопка раскрытия выпадающего списка ролей")
    @FindBy(".//i[@class='mdi mdi-menu-down']")
    AtlasWebElement roleTypeListDropdownButton();

    @Name("Элементы выпадающего списка типов ролей")
    @FindBy(".//div[@click.delegate='setOrgUnitType(type)']")
    List<AtlasWebElement> roleTypes();

}

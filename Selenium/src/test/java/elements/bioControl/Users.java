package elements.bioControl;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface Users extends AtlasWebElement {

    @Name("Кнопка {operation} ")
    @FindBy(".//a[contains(text(), '{{ operation }}')]")
    AtlasWebElement operationWithUsersButton(@Param("operation") String operation);

    @Name("Поле ввода поиска пользователя")
    @FindBy(".//input[@placeholder= 'Поиск по ...']")
    AtlasWebElement inputSearchUser();

    @Name("Кнопка добавить пользователя")
    @FindBy(".//span[contains(text(), 'Добавить пользователя')]")
    AtlasWebElement addUserButton();

    @Name("Кнопка выбора варианта создания нового пользователя")
    @FindBy(".//p[contains(@t, '{{ operation }}')]")
    AtlasWebElement createNewUserVariantButton(@Param("operation") String operation);

    @Name("Кнопка \"Сотрудники без оргюнита\"")
    @FindBy("(//span[text() = ' Неприсоединённые ']/../..//span)[1]")
    AtlasWebElement usersWithoutOrgButton();

    @Name("Сотрудники без оргюнита")
   // @FindBy("//div[@style = 'position:relative']//div[@class ='text-large']")
    @FindBy(".//li[@click.delegate = 'onSelectPerson(person)']")
    ElementsCollection<AtlasWebElement> allUsersWithoutOrg();

    @Name("Поле ввода при создании нового сотрудника")
    @FindBy(".//input[contains(@value.bind ,'{{ input }}')]")
    AtlasWebElement inputCreateNewUser(@Param("input") String input);

    @Name("Кнопка \"сохранить\" логин и пароль")
    @FindBy(".//a[@t = 'manageusers:save']")
    AtlasWebElement saveLoginAndPassButton();

    @Name("Кнопка \"закрыть\" формы логин и пароль")
    @FindBy(".//a[@t = 'manageusers:closewindow']")
    AtlasWebElement closeLoginAndPassButton();

    @Name("Строки с именем пользователей")
    @FindBy(".//tr[@click.delegate = 'onSelect(row)']/td[3]")
    ElementsCollection<AtlasWebElement> allUsersName();

    @Name("Строки с логином пользователей")
    @FindBy(".//tr[@click.delegate = 'onSelect(row)']/td[2]")
    ElementsCollection<AtlasWebElement> allUserLogins();

    @Name("Кнопка сотрудника: {name}")
    @FindBy("//td[contains(text(), '{{ name }}')]/../.")
    AtlasWebElement userNameButton(@Param("name") String name);

    @Name("Кнопка \"выбрать\" в форме выбора сотрудника")
    @FindBy("(//*[@click.delegate = 'onSave()'])[1]")
    AtlasWebElement chooseUser();

    @Name("Кнопка \"выбрать\" в форме выбора сотрудника")
    @FindBy("(//*[@click.delegate = 'onSave()'])[1]")
    AtlasWebElement chooseUser2();

    @Name("Кнопка карандаша в форме сотрудника")
    @FindBy(".//a[text() = 'Управлять']")
    AtlasWebElement pencilButton();

    @Name("Кнопка \"Добавить разрешение\" в форме сотрудника")
    @FindBy(".//a[text() = 'Добавить']")
    AtlasWebElement addPermissionButton();

    @Name("Кнопка разрешения: {permission}")
    @FindBy(".//td[contains(text(), '{{ permission }}')]")
    AtlasWebElement permissionButton(@Param("permission") String permission);

    @Name("Кнопка разрешения подсвеченная синим")
    @FindBy(".//tr[contains(@class, 'selectable has-background-primary')]")
    AtlasWebElement permissionHighlightInBlue();

    @Name("Кнопка \"Выбрать\" в форме разрешений")
    @FindBy("//a[@click.delegate='state.onSelect(state.selected)']")
    AtlasWebElement selectPermissionButton();

    @Name("Кнопка \"Изменить список оргюнитов\"")
    @FindBy(".//a[text() = 'Изменить список оргюнитов']")
    AtlasWebElement changeListOrgNameButton();

    @Name("Кнопка \"Сохранить\" в форме разрешений")
    @FindBy(".//a[@click.delegate = 'onSave()']")
    AtlasWebElement savePermission();

    @Name("Данные о оргюинитах в разрешении сотрудника")
    @FindBy(".//table[@class = 'table is-bordered']//td[@width]")
    ElementsCollection<AtlasWebElement> tableUserOrgNamePermission();

    @Name("Cписок разрешений сотрудника")
    @FindBy(".//div[@class = 'modal-content']//tr[@click.delegate = 'onSelect(row)']")
    ElementsCollection<AtlasWebElement> tableUserPermission();

    @Name("Строка ввода поиска по оргмодулю")
    @FindBy(".//input[@placeholder ='Поиск по оргмодулю']")
    AtlasWebElement inputOrgModuleSearch();

    @Name("Чекбокс оргюнита выданного по поиску")
    @FindBy(".//span[@click.delegate ='onCheckOrgunit(row.uuid)']")
    AtlasWebElement findingOrgNameCheckBox();

    @Name("Кнопка \"Сохранить\" в окне фильтра по оргюниту")
    @FindBy("(//div[contains(@class, 'modal is-active')]//a[contains(text(), 'Сохранить')])[3]")
    AtlasWebElement saveInOrgNameFilterButton();

    @Name("Кнопка \"Удалить\" в форме разрешения юзера")
    @FindBy("//a[@click.delegate = 'onDelete()']")
    AtlasWebElement deleteInUserPermissionButton();

    @Name("Кнопка \"Редактировать\" в форме разрешения юзера")
    @FindBy(".//a[@t = 'permissions:edit']")
    AtlasWebElement editInUserPermissionButton();

    @Name("Значения оргюнитов для которых имеется доступ в форме настроек разрешения")
    @FindBy(".//div[@class = 'modal is-active']//tr[@click.delegate = 'onSelect(row)']/td[3]")
    AtlasWebElement restrictedOrgName();

}

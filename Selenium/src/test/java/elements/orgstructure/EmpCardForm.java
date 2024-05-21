package elements.orgstructure;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface EmpCardForm extends AtlasWebElement {

    @Name("Шеврон {employeeInfoName}")
    @FindBy(".//span[text()='{{ employeeInfoName }}']//..//..//i[contains(@class, 'mdi-chevron')]")
    AtlasWebElement showButton(@Param("employeeInfoName") String employeeInfoName);

    @Name("Кнопка редактирования данных {employeeInfoName}")
    @FindBy(".//span[text()='{{ employeeInfoName }}']//..//..//i[contains(@class, 'mdi-pencil')]")
    AtlasWebElement pencilButton(@Param("employeeInfoName") String employeeInfoName);

    @Name("Кнопка редактирования сотрудника")
    @FindBy("//button[@class='mdl-button mdl-button--icon au-target mdl-js-button']//i[@class='mdi mdi-pencil']")
    AtlasWebElement empPencilButton();

    @Name("Кнопка \"Изменить\"")
    @FindBy("//div[(contains(@show.bind, 'edit') or contains(@show.bind, 'Edit')) and not(contains(@class, 'hide'))]//button[contains(@t, 'change')]")
    AtlasWebElement osCardChangeButton();

    @Name("Кнопка изменить выбранного {employeeInfoName}")
    @FindBy("//span[text()='{{ employeeInfoName }}']/../../../div//button[contains(@click.trigger, 'save') and not(contains(@click.trigger, 'group'))]")
    AtlasWebElement changeButton(@Param("employeeInfoName") String employeeInfoName);

    @Name("Кнопка изменить выбранного {employeeInfoName}")
    @FindBy("//span[text()='{{ employeeInfoName }}']/../../../div//button[contains(@t, 'cancel') and not(contains(@click.trigger, 'group'))]")
    AtlasWebElement cancelButton(@Param("employeeInfoName") String employeeInfoName);

    @Name("Чекбокс стажерской программы")
    @FindBy("//span[text()='Стажерская программа']//..//..//..//label")
    AtlasWebElement internCheckBox();

    //сотрудники
    @Name("ФИО сотрудника в режиме чтения")
    @FindBy("//span[contains(@class.bind,'groupsErrors[0]')]")
    AtlasWebElement employeeName();

    @Name("Поле ввода выбранного типа")
    @FindBy(".//input[{{ type }}]")
    AtlasWebElement employeeFieldByType(@Param("type") String type);

    @Name("Поле выбранной даты {type}")
    @FindBy(".//label[contains(text(), '{{ type }}')]/../input")
    AtlasWebElement employeeDataFieldByType(@Param("type") String type);

    @Name("Кнопка календаря {name} ")
    @FindBy(".//label[contains(text(), '{{ name }}')]/..//button")
    AtlasWebElement calendarButtons(@Param("name") String name);

    @Name("Гендер на выбор мужской или женский")
    @FindBy(".//div[@menu='gender']/div[contains(text(), '{{ gender }}')]")
    AtlasWebElement genderButton(@Param("gender") String gender);

    @Name("Поле с текущим логином пользваотеля")
    @FindBy("//span[contains(text(), 'Имя пользователя')]/../span[1]")
    AtlasWebElement testNewLogin();

    @Name("Выбранное имя пользователя")
    @FindBy("//div[contains(@class, 'item--selected')]//div")
    AtlasWebElement userSelected();

    @Name("Поле имени сотрудника")
    @FindBy(".//*[contains(@class, 'avatar')]/../span")
    AtlasWebElement employeeNameField();

    @Name("Поле с выбранным типом")
    @FindBy(".//span[text() = '{{ type }}']/../span[1]")
    AtlasWebElement employeeFieldsData(@Param("type") String type);

    @Name("Кнопки выбора тегов при вводе")
    @FindBy(".//div[@click.trigger = 'plusOne(item)']")
    ElementsCollection<AtlasWebElement> tagsVariantWhenSending();

    @Name("Отображаемые теги у сотрудника")
    @FindBy(".//span[@t = 'models.task.tags']/../span[1]")
    AtlasWebElement employeeTags();

    @Name("Кнопка удаления тега {tag}")
    @FindBy(".//span[text() ='{{ tag }}']/../button[@click.trigger = 'remove(item)']")
    AtlasWebElement removeTagButton(@Param("tag") String tag);

    @Name("Поле с выбранным типом")
    @FindBy(".//label[contains(@for, '{{ type }}' )]/ancestor::div/input")
    AtlasWebElement contactsFieldByType(@Param("type") String type);

    @Name("Кнопка с названием типа адреса")
    @FindBy(".//div[contains(@class, 'is-visible')]/div[contains(text(), '{{ name }}')]")
    AtlasWebElement addressTypeButton(@Param("name") String name);

    //Элементы блока место работы
    @Name("Все назначения сотрудника")
    @FindBy("//div[@show.bind='show[8]']/div[contains(@class, 'mdl-list__item--flex')]/div[contains(@class,'primary-content')]")
    ElementsCollection<AtlasWebElement> allEmployeePositions();
    //элементы блока параметров входа в систему

    @Name("Поле логина сотрудника")
    @FindBy("//input[@id = 'username-0']")
    AtlasWebElement employeeLoginField();

    @Name("Поле пароля сотрудника")
    @FindBy("//input[@id = 'password-0']")
    AtlasWebElement employeePassField();

    @Name("Поле подтверждения пароля сотрудника")
    @FindBy("//input[@id = 'confirm-password']")
    AtlasWebElement employeeConformPassField();

    //элементы навыки
    @Name("Чексбокс выбранного навыка")
    @FindBy("//span[contains(text(), '{{ skillName }}')]/../../div[2]/label/span[3]")
    AtlasWebElement skillCheckBox(@Param("skillName") String skillName);

    @Name("Поля с параметрами")
    @FindBy("//label[contains(text(),'{{ name }}')]/../input")
    AtlasWebElement matchParameters(@Param("name") String name);

    @Name("Варианты в матпараметре")
    @FindBy(".//div[contains(@class,'is-visible')]//div[contains(text(),'{{ var }}')]")
    AtlasWebElement variantsInMatchParam(@Param("var") String var);

    //элементы блока "Статусы"
    @Name("Поля ввода статуса сотрудника")
    @FindBy("//input[contains(@focus.trigger,'employee.status')]")
    ElementsCollection<AtlasWebElement> employeeStatusInputs();

    @Name("Строка ввода {dateVariant}")
    @FindBy(".//label[text() = '{{ dateVariant }}']/../input")
    ElementsCollection<AtlasWebElement> dateInputs(@Param("dateVariant") String dateVariant);

    @Name("Пункт выпадающего списка {item}")
    @FindBy("//div[contains(@click.delegate, 'selectStatus')][contains(text(),'{{ item }}')]")
    ElementsCollection<AtlasWebElement> statusOptions(@Param("item") String item);

    @Name("Пункт списка в блоке {section}")
    @FindBy("//span[text()='{{ section }}']/../../..//i[contains(@class, 'clock')]/../../../div[contains(@class, 'two-line')]")
    ElementsCollection<EmployeeInfoBlock> employeeInfoListItem(@Param("section") String section);

    //элементы блока "Бухгалтерия"

    @Name("Поле ввода СНИЛСа")
    @FindBy(".//input[@id='snils']")
    AtlasWebElement inputSnils();

    @Name("СНИЛС отображаемый на странице")
    @FindBy(".//span[contains(text(), 'СНИЛС')]/../span[1]")
    AtlasWebElement outputSnils();
}

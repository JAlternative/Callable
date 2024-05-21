package elements.scheduleBoard;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface EmployeeDataMenu extends AtlasWebElement {

    @Name("Шеврон {employeeInfoName}")
    @FindBy(".//*[text()='{{ employeeInfoName }}']/../..//i[contains(@class,'chevron')]")
    AtlasWebElement showButton(@Param("employeeInfoName") String employeeInfoName);

    @Name("Поле с интервалом замещения по имени сотрудника")
    @FindBy("//span[@t='common.essentials.alternates']/../../span[contains(@class, 'text--primary')]/../..//div[@class='mdl-list__group au-target']//span[contains(text(), '{{ name }}')]/../span[2]/span[2]")
    AtlasWebElement deputyDateIntervalByName(@Param("name") String name);

    @Name("Кнопка редактирования данных {employeeInfoName}")
    @FindBy(".//span[text()='{{ employeeInfoName }}']/../../button[1]")
    AtlasWebElement pencilButton(@Param("employeeInfoName") String employeeInfoName);
    //На текущий момент работает для карандаша напротив свойства Параметры
    @Name("Кнопка редактирования данных {employeeInfoName}")
    @FindBy(".//span[text()='{{ employeeInfoName }}']/following-sibling::button")
    AtlasWebElement parametersPencilButton(@Param("employeeInfoName") String employeeInfoName);

    @Name("Форма с параметрами")
    @FindBy("//div[@class='mdl-dialog__head mdl-dialog__head--secondary']/../../div[@class='mdl-dialog mdl-dialog--fit-content']")
    EmployeeParametersMenu employeeParametersMenu();

    @Name("Поле ввода E-mail")
    @FindBy(".//input[@value.bind='employee.email']")
    AtlasWebElement emailFieldInput();

    @Name("Поле со значением email")
    @FindBy(".//contacts-section//span[text()='e-mail']/preceding-sibling::span")
    AtlasWebElement emailField();

    @Name("Поле со значением телефона")
    //todo необходимо иметь в виду, что может быть несколько телефонов одного типа.
    // Когда это начнет вызывать проблемы, нужно начать возвращать этим локатором список и фильтровать результаты в тесте
    @FindBy(".//contacts-section//span[text()='{{ phoneType }}']/preceding-sibling::input")
    AtlasWebElement phoneField(@Param("phoneType") String phoneType);

    @Name("Поле с текстом требуется наставник")
    @FindBy("//span[@t='models.employee.needMentor']")
    AtlasWebElement needMentorField();

    @Name("Поле ввода типа адреса")
    @FindBy(".//input[@value.bind='address.type.name']")
    AtlasWebElement addressTypeInput();

    @Name("Чекбокс стажерской программы")
    @FindBy(".//span[text()='Требуется наставник']/ancestor::div[2]//span[@class = 'mdl-checkbox__ripple-container mdl-js-ripple-effect mdl-ripple--center']")
    AtlasWebElement internCheckBox();

    @Name("Поле ввода даты окончания стажерской программы")
    @FindBy("//label[contains(text(), 'программы')]/../input")
    AtlasWebElement internEndDateInput();

    @Name("Список менторов стажерской программы")
    @FindBy("//div[@click.delegate='addMentor(possibleMentor)']")
    ElementsCollection<AtlasWebElement> mentorsList();

    @Name("Поле наставника")
    @FindBy("//div[@class='mdl-list__group au-target']//div//div[@class='mdl-list__item mdl-list__item--two-line au-target']")
    AtlasWebElement mentorsField();

    @Name("Кнопка изменить выбранного {{employeeInfoName}} ")
    @FindBy("//span[text()='{{ employeeInfoName }}']/../../../div//button[contains(@click.trigger, 'save') and not(contains(@click.trigger, 'group'))]")
    AtlasWebElement changeButton(@Param("employeeInfoName") String employeeInfoName);

    @Name("Кнопка календарь")
    @FindBy("//div[@class='mdl-list__item-primary-content mdl-list__item-primary-content--right-shift']//i[@class='mdi mdi-calendar']")
    AtlasWebElement calendarButton();

    @Name("Рыскрытие списка менторов")
    @FindBy("//input[contains(@id, 'mentor') and (contains(@type, 'text'))]")
    AtlasWebElement mentorsListOpenButton();

    @Name("Кнопка раскрытия списка заместителей")
    @FindBy("//div[@class='mdl-list__item mdl-list__item--flat']/../../div[{{ index }}]//label[@t='common.essentials.deputy']/../input")
    AtlasWebElement deputyListOpenButton(@Param("index") int index);

    @Name("Первый результат в поиске заместителя")
    @FindBy("//div[contains(@class, 'menu') and (contains(@class, 'visible'))]//div[contains(@class, 'item') and (not (contains(@show.bind, 'loading')))][1]")
    AtlasWebElement firstDeputyInSearch();

    @Name("Поле с текстом ошибки, указанного поля с датой")
    @FindBy("//label[text()='{{ inputName }}']/../span")
    AtlasWebElement errorField(@Param("inputName") String inputName);

    @Name("Поле ввода имени заместителя")
    @FindBy("//div[contains(@class, 'menu') and (contains(@class, 'visible'))]//input[@ref='searchInputElement']")
    AtlasWebElement deputySearchInput();

    @Name("Поле ввода имени заместителя")
    @FindBy("//input[contains(@id, 'deputy')]/../../../..//button[contains(@click.trigger, 'delete') and not (contains(@style, 'hidden'))]")
    AtlasWebElement deleteDeputyButton();

    @Name("Сообщение данные отсутствуют, на вкладке заметсителей")
    @FindBy("//span[contains(text(), 'Заместители')]/../../..//div[@t='common.noData']")
    AtlasWebElement noDataField();

    @Name("Список всех блоков в карточке сотрудника, кроме главной информации")
    @FindBy("//div[@class='mdl-list mdl-list--no-margin']/div[@show.bind='id']/div[@class='au-target']")
    ElementsCollection<AtlasWebElement> listOfEmpFields();

    @Name("Элемент для определения статуса шеврона {employeeInfoName}")
    @FindBy(".//span[text()='{{ employeeInfoName }}']/../../button[2][not(@disabled)]/i")
    AtlasWebElement chevronButton(@Param("employeeInfoName") String employeeInfoName);

    @Name("Поле ввода даты окончания работы")
    @FindBy("//label[contains(text(), 'Дата окончания работы')]/../input")
    AtlasWebElement endWorkDateInput();

    @Name("Кнопка \"Сохранить\" в основной информации о сотруднике")
    @FindBy("//button[@click.trigger='save(0)']")
    AtlasWebElement mainSaveButton();

    @Name("Кнопка карандаш основной информации")
    @FindBy("//div[@class='mdl-list__group au-target']//button[1]")
    AtlasWebElement mainPencilButton();

    @Name("Поле имени сотрудника")
    @FindBy(".//*[contains(@class, 'avatar')]/../span")
    AtlasWebElement employeeNameField();

    @Name("Чексбокс выбранного навыка")
    @FindBy("//span[contains(text(), '{{ skillName }}')]/../../div[2]/label/span[3]")
    AtlasWebElement skillCheckBox(@Param("skillName") String skillName);

    @Name("Список из текущих навыков у сотрудника")
    @FindBy("//i[contains(@class, 'account-star')]/../span")
    ElementsCollection<AtlasWebElement> skillsNamesField();

    @Name("Поле логина сотрудника")
    @FindBy("//input[@id = 'username-0']")
    AtlasWebElement employeeLoginField();

    @Name("Поле даты")
    @FindBy("//div[@class='mdl-list__item mdl-list__item--flat']/../../div[{{ index }}]//label[text()='{{ name }}']/../input")
    AtlasWebElement dateInput(@Param("index") int index, @Param("name") String name);

    @Name("Поле пароля сотрудника")
    @FindBy("//input[@id = 'password-0']")
    AtlasWebElement employeePassField();

    @Name("Поле подтверждения пароля сотрудника")
    @FindBy("//input[@id = 'confirm-password']")
    AtlasWebElement employeeConformPassField();

    @Name("Список поле с данными о сотруднике")
    @FindBy("//div[@class='mdl-list__group au-target']/div")
    ElementsCollection<AtlasWebElement> employeeDataList();

    @Name("Аватарка")
    @FindBy(".//div[@class = 'slim-file-hopper']")
    AtlasWebElement avatarIcon();

    @Name("Список из неактивированных чекбоксов")
    @FindBy(".//label[contains(@class, 'checkbox') and not(contains(@class, 'is-checked'))]")
    ElementsCollection<AtlasWebElement> freeCheckBoxList();

    @Name("Список из активированных чекбоксов")
    @FindBy(".//label[contains(@class, 'checkbox') and (contains(@class, 'is-checked'))]")
    ElementsCollection<AtlasWebElement> activeCheckBoxes();

    @Name("Поля с параметрами")
    @FindBy("//label[contains(text(),'{{ name }}')]/../input")
    AtlasWebElement matchParameters(@Param("name") String name);

    @Name("Варианты в матпараметре")
    @FindBy(".//div[contains(@class,'is-visible')]//div[contains(text(),'{{ var }}')]")
    AtlasWebElement variantsInMatchParam(@Param("var") String var);

    @Name("Кнопка \"Сохранить\" в форме параметров")
    @FindBy("//div[@class='mdl-dialog mdl-dialog--fit-content']//button[text()='Сохранить ']")
    AtlasWebElement saveParamButton();

}

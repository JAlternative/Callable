package elements.scheduleBoard;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface AddNewEmployeeForm extends AtlasWebElement {
    //Сотрудник
    @Name("Список выбора сотрудника на должность")
    @FindBy(".//div[contains(@click.trigger,'config.onClick')]")
    ElementsCollection<AtlasWebElement> listOfFreeEmployees();

    @Name("Кнопка выбора сотрудника")
    @FindBy("//button[@id='employee-name-button']")
    AtlasWebElement selectEmployeeButton();

    @Name("Поле выбора сотрудника")
    @FindBy("//searchable-menu[@config.bind=\"employeesMenu\"]//input")
    AtlasWebElement nameField();

    @Name("Сотрудник в списке по имени : {name}")
    @FindBy("//div[@ref and contains(@class,'shadow-16dp')]/div[@ref]/div[contains(text(),'{{ name }}')]")
    AtlasWebElement employeeButton(@Param("name") String name);

    @Name("Строка ввода, появляющаяся при поиске сотрудника")
    @FindBy("//input[contains(@id, 'search-input-')]")
    AtlasWebElement employeeSearchInput();


    //Название должности
    @Name("Поле выбора названия должности")//works
    @FindBy("//input[@id='position-job-title']")
    AtlasWebElement inputJobCategory();

    @Name("Позиция {jobTitle} из выпадающего списка")
    @FindBy(".//div[@click.delegate='selectJobTitle(jobTitle)'][normalize-space()='{{ jobTitle }}']")
    AtlasWebElement jobTitle(@Param("jobTitle") String jobTitle);

    //Тип должности
    @Name("Поле типа должности")
    @FindBy("//input[@id=\"position-position-type\"]")
    AtlasWebElement positionTypeField();

    @Name("Тип должности {positionType} из выпадающего списка")
    @FindBy("//div[@menu='position-position-type']/div[@click.delegate='employeePosition.position.positionType = positionType'][normalize-space(text()) = '{{ positionType }}']")
    AtlasWebElement positionTypeListItem(@Param("positionType") String positionType);

    //outerId
    @Name("Поле outerId должности")
    @FindBy("//input[@id='positionOuterId']")
    AtlasWebElement outerIdJobTitleField();

    //Категория должности
    @Name("Шеврон выбора категории должности")
    @FindBy("//label[@for='position-category']/../button")
    AtlasWebElement categoryOfJobButton();

    @Name("Поле категории должности")
    @FindBy("//input[@id='position-category']")
    AtlasWebElement categoryOfJobInput();

    @Name("Категория : {category} из выпадающего списка")
    @FindBy("//label[contains(text(), 'Категория должности')]/../div[contains(@class, 'is-visible')]/div[contains(text(), '{{ category }}')]")
    AtlasWebElement categoryOfJob(@Param("category") String category);


    //Функциональная роль
    @Name("Поле функциональной роли")
    @FindBy("//input[@id='position-group']")
    AtlasWebElement functionalRoleInput();

    @Name("Кнопка выбора функциональной роли")
    @FindBy("//input[@value.bind=\"employeePosition.position.positionGroup.name\"]")
    AtlasWebElement functionalRoleButton();

    @Name("Кнопка варианта функциональной роли : {nameOfItemGroup}")
    @FindBy(".//input[@value.bind='employeePosition.position.positionGroup.name']/../div/*[contains(text(),'{{ nameOfItemGroup }}')]")
    AtlasWebElement variantsOfJobsItemGroup(@Param("nameOfItemGroup") String nameOfItemGroup);

    @Name("Кнопка выбора функциональной роли")
    @FindBy("//div[contains(@class, 'is-visible')]//div[normalize-space(text()) = '{{ name }}']")
    AtlasWebElement funcRoleButton(@Param("name") String name);


    //Прочие элементы
    @Name("Поле с элементом загрузки, во время поиска")
    @FindBy("//searchable-menu//div[@show.bind='loading' and not (contains(@class, 'hide'))]")
    AtlasWebElement loadingField();

    @Name("Позиция : {variant} из выпадающего списка")
    @FindBy(".//div[contains(@hide.bind, '!jobTitleDisabled')]//div[{{ variant }}]")
    AtlasWebElement variantsOfJobs(@Param("variant") int variant);

    @Name("Кнопка \"Сохранить\"")
    @FindBy(".//button[@click.trigger='save()']")
    AtlasWebElement saveButton();

    @Name("Кнопка \"Отменить\"")
    @FindBy(".//button[@type='button'][contains(text(),'Отменить')]")
    AtlasWebElement cancelButton();

    @Name("Кнопка календаря для выбора даты начала работы")
    @FindBy("//label[text()='{{ name }}']/..//i/..")
    AtlasWebElement calendarButton(@Param("name") String name);

    @Name("Ввод даты в {inputVariants}")
    @FindBy(".//label[text()='{{ inputVariants }}']/../input")
    AtlasWebElement inputVariantDate(@Param("inputVariants") String inputVariants);

    @Name("Предупреждение в {inputVariants}")
    @FindBy(".//label[text()='{{ inputVariants }}']/../input/../span")
    AtlasWebElement alertDate(@Param("inputVariants") String inputVariants);

    @Name("Кнопка корзины, для очищения даты окончания должности")
    @FindBy(".//label[text()='Дата окончания должности']/../button/i[contains(@class, 'delete')]")
    AtlasWebElement clearDateEndPosition();

    @Name("Кнопка кисточки, для очищения даты окончания работы")
    @FindBy("//label[text()='Окончание работы']/ancestor::div[contains(@class, 'slim')]//button[contains(@title, 'Очистить')]//i")
    AtlasWebElement clearDateEndWork();

    @Name("Радиобатон \"Руководитель\"")
    @FindBy(".//label[contains(@class.bind,'employeePosition.is')]")
    AtlasWebElement leaderCheckBox();

    @Name("Поле \"Табельный номер\"")
    @FindBy(".//input[@value.bind='employeePosition.cardNumber']")
    AtlasWebElement cardNumberField();

    @Name("Поле \"Ставка\"")
    @FindBy(".//input[@value.bind='employeePosition.rate']")
    AtlasWebElement rateField();

    @Name("Поле \"Закрепление за залом\"")
    @FindBy(".//input[@id='operational-zones']")
    AtlasWebElement assignmentToHallField();

    @Name("Снэкбар с изменением/добавлением должности")
    @FindBy("//div[@ref='snack']//div[@class='mdl-snackbar__text']")
    AtlasWebElement addPositionSnackBar();

    @Name("Сообщение \"Поле не может быть пустым\"")
    @FindBy(".//span[@class='mdl-textfield__error'][normalize-space()='Поле не может быть пустым']")
    AtlasWebElement emptyFieldErrorMessage();

}

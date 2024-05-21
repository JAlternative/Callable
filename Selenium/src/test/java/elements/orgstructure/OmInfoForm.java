package elements.orgstructure;

import elements.scheduleBoard.AttributeForm;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;

import java.util.List;

public interface OmInfoForm extends AtlasWebElement {

    //элементы карточки OM

    @Name("Кнопка \"Карандаш\" для редактирования основных данных оргЮнита")
    @FindBy("//i[@class='mdi mdi-pencil']/../../button[contains(@show.bind, 'editMode') and not(contains(@class, 'hide'))]")
    AtlasWebElement editingButton();

    @Name("Кнопка \"Х\" для закрытия формы редактирования подразделения")
    @FindBy("//div[@class='mdl-list__group au-target']//i[contains(@class, 'close')]")
    AtlasWebElement buttonClose();

    @Name("Поле с именем оргЮнита в свойствах подразделения")
    @FindBy("//i[@class='mdi mdi-store mdl-list__item-icon']/../span[1]")
    AtlasWebElement omName();

    @Name("Поле с текущим outerId оргюнита")
    @FindBy("//span[contains(text(), 'OuterId')]/../span[1]")
    AtlasWebElement outerId();

    @Name("Поле с текущим типом оргЮнита")
    @FindBy("//i[contains(@class, 'store')]/../span[2]")
    AtlasWebElement omType();

    @Name("Поле с датой открытия ом")
    @FindBy("//span[contains(text(), 'Дата открытия')]/../span[1]")
    AtlasWebElement omStartDate();

    @Name("Поле с датой закрытия ом")
    @FindBy(".//span[contains(@t, 'dateTo')]/preceding-sibling::span")
    AtlasWebElement omClosedDate();

    @Name("Поле с текущим руководителем оргЮнита")
    @FindBy("//span[contains(text(), 'Руководитель')]/../span[1]")
    AtlasWebElement omManagerName();

    @Name("Имя заместителя в карточке ом")
    @FindBy(".//span[@t='common.essentials.deputy']/preceding-sibling::span")
    AtlasWebElement omDeputyName();

    @Name("Поле с датой начала замещения")
    @FindBy(".//span[contains(@t, 'deputyStartDate')]/preceding-sibling::span")
    AtlasWebElement omDeputyStartDate();

    @Name("Поле с датой окончания замещения")
    @FindBy(".//span[contains(@t, 'deputyEndDate')]/preceding-sibling::span")
    AtlasWebElement omDeputyEndDate();

    @Name("Текст \"Родительское подразделение\"")
    @FindBy("//span[@t='common.essentials.parentOrgUnit']")
    AtlasWebElement parentOmFields();

    @Name("Название родительского ом в карточке")
    @FindBy(".//span[@t='common.essentials.parentOrgUnit']/preceding-sibling::span")
    AtlasWebElement omParentName();

    @Name("Выбор родительского ОМ {{ParentOm}}")
    @FindBy(".//div[@menu='org-unit-parent']/div[contains(text(), '{{ ParentOm }}')]")
    AtlasWebElement parentFromDropDownMenu(@Param("ParentOm") String ParentOm);

    @Name("Варианты родительского ОМ")
    @FindBy("//div[@menu='org-unit-parent']/div")
    List<AtlasWebElement> listOfParentOM();

    @Name("Выбранный оргюнит")
    @FindBy("//div[contains(@class, 'item--selected')]//div")
    AtlasWebElement orgNameSelected();

    @Name("Поле с названием тэгов в карточке")
    @FindBy(".//span[@t='models.task.tags']/preceding-sibling::span")
    AtlasWebElement tagsFieldOrgUnitCard();

    @Name("Кнопка удаления тега по его названию")
    @FindBy("//span[contains(text(), '{{ tagName }}')]/../button")
    AtlasWebElement tagNameDeleteButton(@Param("tagName") String tagName);

    @Name("Надпись \"Участвует в расчете\" в карточке ом")
    @FindBy(".//div[@class='mdl-list__group au-target']//span[@t='models.orgUnit.availableForCalculation']")
    AtlasWebElement omCalcFieldName();

    @Name("Чекбокс участвует в расчете")
    @FindBy(".//input[contains(@id, 'available')]/../span[3]")
    AtlasWebElement availableForCalculation();

    @Name("Шеврон раскрытия свойства подразделения : {infoNames}")
    @FindBy("//span[text()='{{ infoNames }}']//../../button[not(contains(@class,'hide'))]")
    AtlasWebElement showButton(@Param("infoNames") String infoNames);

    @Name("Кнопка \"Карандаш\" для редактирования данных оргЮнита")
    @FindBy(".//span[text()='{{ omInfoName }}']/ancestor::div[1]//button[1]/i[@class = 'mdi mdi-pencil']")
    AtlasWebElement editButton(@Param("omInfoName") String omInfoName);

    //элементы блока контактов

    @Name("Поле с типом телефона в контактах")
    @FindBy(".//i[contains(@class, 'phone')]/../span[2]")
    AtlasWebElement testPhoneType();

    @Name("Кнопки удаления данных адреса")
    @FindBy(".//button[contains(@click.trigger, 'addresses')][not(contains(@style, 'hidden'))]/i[@class='mdi mdi-delete']")
    ElementsCollection<AtlasWebElement> addressDeleteButtons();

    //элементы блока сотрудников

    @Name("Кнопка плюс, добавить сотрудника")
    @FindBy("//i[contains(@class, 'mdi-plus')]/../../button[@click.trigger='positionDialogTrigger()']")
    AtlasWebElement plusButtonEmployee();

    @Name("Имя сотрудника")
    @FindBy("//div[contains(@show.bind,'employeePosition.show') and not(contains(@class, 'hide'))]//span[@click.trigger='openEmployee(employeePosition.employee.id)']")
    ElementsCollection<AtlasWebElement> employeesNames();

    @Name("Поле с должностью сотрудника")
    @FindBy("//span[contains(text(), '{{ name }}')]/../span[3]")
    AtlasWebElement posJob(@Param("name") String name);

    @Name("Названия пустой должности")
    @FindBy("//i[text()='Сотрудник не назначен']/../span[@class='mdl-list__item-sub-title']")
    ElementsCollection<AtlasWebElement> emptyPositionsNamesRelease();

    @Name("Названия пустой должности")
    @FindBy("//i[text()='Сотрудник не назначен']/../span[contains(@class,'mdl-list__item-sub-title')]")
    ElementsCollection<AtlasWebElement> emptyPositionsNamesMaster();

    @Name("Список названий пустых позиций")
    @FindBy("//div[contains(@show.bind,'employeePosition.show') and not(contains(@class, 'hide'))]//i[@t='dialogs.orgUnit.employeeUnassigned']/../span[2]")
    ElementsCollection<AtlasWebElement> emptyPositionsNamesList();

    @Name("Список сотрудников с должностью")
    @FindBy("//div[contains(@show.bind,'employeePosition.show') and not(contains(@class, 'hide'))]//span[contains(@class, 'sub-title')]/../..//span[@click.trigger='openEmployee(employeePosition.employee.id)']")
    ElementsCollection<AtlasWebElement> employeesListWithPosition();

    @Name("Кнопки редактирования пустой должности (три точки) ")
    @FindBy("//i[text()='Сотрудник не назначен']/../../div[@class='mdl-list__item-secondary-action']")
    ElementsCollection<AtlasWebElement> threeDotEmptyPositionButtons();

    @Name("Все троеточия во вкладке \"Сотрудники\"")
    @FindBy("//div[@class='mdl-list__item mdl-list__item--two-line mdl-list__item--overflow au-target']//i[@class='mdi mdi-dots-vertical']")
    ElementsCollection<AtlasWebElement> allThreeDots();

    @Name("Кнопка троеточия по имени сотрудника")
    @FindBy("//div[contains(@show.bind,'employeePosition.show') and not(contains(@class, 'hide'))]//span[contains(text(), '{{ name }}')]/../..//button")
    AtlasWebElement threeDotsByNameOfEmployee(@Param("name") String name);

    @Name("Список кнопок троеточия у должностей без сотрудников")
    @FindBy("//div[contains(@show.bind,'employeePosition.show') and not(contains(@class, 'hide'))]//i[@t='dialogs.orgUnit.employeeUnassigned']/../..//button")
    ElementsCollection<AtlasWebElement> emptyPositionsThreeDotsList();

    @Name("Кнопка редактировать у выбранного сотрудника")
    @FindBy("//div[@class='menu menu--right au-target is-visible']//div[@class='menu__item au-target'][contains(text(),'Редактировать')]")
    AtlasWebElement employeeEditButton();

    @Name("Кнопка \"Атрибуты позиции\" у выбранного сотрудника")
    @FindBy("//div[@class='menu menu--right au-target is-visible']//div[@class='menu__item au-target'][contains(text(),'Атрибуты позиции')]")
    AtlasWebElement positionAttributeEditButton();

    @Name("Вариант в многоточии у выбранного сотрудника")
    @FindBy(".//span[normalize-space(text())='{{ name }}']/../../div/div[contains(@class,'is-visible')]/div[@t='{{ variant }}']")
    AtlasWebElement employeeButton(@Param("name") String name, @Param("variant") String variant);

    @Name("Кнопка редактирования должности")
    @FindBy("//div[@class='menu menu--right au-target is-visible']/div[text()='Редактировать ']")
    AtlasWebElement editPositionButton();

    @Name("Кнопка удаление должности")
    @FindBy("//div[@class='menu menu--right au-target is-visible']/div[text()='Удалить должность']")
    AtlasWebElement deletePositionButton();

    @Name("Подтверждение удаление должности")
    @FindBy("//div[text()='Должность удалена']")
    AtlasWebElement positionDeleted();

    @Name("Поп-ап панелька удачного изменения должности")
    @FindBy("//div[contains(text(), 'Должность изменена')]")
    AtlasWebElement popUpPositionChanged();

    @Name("Кнопки троеточий пустых должностей с определенным именем")
    @FindBy("//i[text()='Сотрудник не назначен']/../span[contains(text(), '{{ name }}')]/../../div[@class='mdl-list__item-secondary-action']")
    ElementsCollection<AtlasWebElement> emptyPositionsByName(@Param("name") String name);

    //элементы блока режима работы

    @Name("Кнопка троеточие")
    @FindBy("//button[@id='business-hours-menu']/i")
    AtlasWebElement threeDotsButton();

    @Name("Все поля названий графиков")
    @FindBy("//div[@click.trigger='chooseBusinessHoursList(businessHoursList)']/span[@class = 'menu__item-sub-title']")
    List<AtlasWebElement> allAvailableSchedules();

    @Name("Даты активности графиков")
    @FindBy("//div[@click.trigger='chooseBusinessHoursList(businessHoursList)']/span/span/..")
    List<AtlasWebElement> scheduleDates();

    @Name("Поле названия выбранного графика")
    @FindBy("//span[@class = 'menu__icon mdi au-target mdi-check']/ancestor::div[@click.trigger='chooseBusinessHoursList(businessHoursList)']/span[@class = 'menu__item-sub-title']")
    AtlasWebElement activeSchedule();

    @Name("Кнопка редактирования графика роботы")
    @FindBy("//div[contains(text(),'Редактировать график работы')]")
    AtlasWebElement changeScheduleButton();

    @Name("Кнопка выбрать график работы")
    @FindBy("//div[contains(text(),'Управление списком графиков работы')]")
    AtlasWebElement selectScheduleButton();

    @Name("Период действия графика")
    @FindBy("//div[@show.bind='businessHoursList']/../div[@class='mdl-list__item']/div/span")
    AtlasWebElement chosenScheduleTimePeriod();

    @Name("Название активного графика")
    @FindBy("//div[@show.bind='permissionShowService && businessHoursList']/div/span")
    AtlasWebElement chosenScheduleType();

    @Name("Время начала у дня с определённым номером")
    @FindBy("//div[@class='mdl-list__group au-target']/div[{{ dayNumber }}]//label[contains(text(), 'Время начала')]/../input")
    AtlasWebElement dayStartTimeField(@Param("dayNumber") int dayNumber);

    @Name("Время окончания у дня с определённым номером")
    @FindBy("//div[@class='mdl-list__group au-target']/div[{{ dayNumber }}]//label[contains(text(), 'Время окончания')]/../input")
    AtlasWebElement dayEndTimeField(@Param("dayNumber") int dayNumber);

    @Name("Тип дня у определённого дня недели")
    @FindBy("//span[contains(text(), '{{ name }}')]/../span[2]")
    AtlasWebElement dayTypeField(@Param("name") String name);

    @Name("Список типов дней")
    @FindBy("//div[contains(@class, 'mdl-list__group')and not(contains(@class, 'hide'))]//input[contains(@id, 'business-hours-type-')]")
    ElementsCollection<AtlasWebElement> daysTypes();

    @Name("Кнопка дня рабочий/выходной")
    @FindBy("//div[@class='menu au-target is-visible']//div[@class='menu__item au-target'][contains(text(),'{{ dayType }}')]")
    AtlasWebElement dayTypeButton(@Param("dayType") String dayType);

    @Name("Тип дня по порядковому номеру")
    @FindBy("//div[@class='mdl-list__group au-target']/div[{{ dayNumber }}]//input[contains(@id, 'business-hours-type-')]")
    AtlasWebElement dayType(@Param("dayNumber") int dayNumber);

    @Name("Кнопка изменить в редактировании расписания")
    @FindBy("//div[@class='mdl-list__group au-target']//button[@class='mdl-button mdl-button--primary au-target mdl-js-button'][contains(text(),'Изменить')]")
    AtlasWebElement editionScheduleSaveButton();

    @Name("Кнопка отменить в редактировании расписания")
    @FindBy("//div[@class='mdl-list__group au-target']//button[@class='mdl-button mdl-button--primary au-target mdl-js-button'][contains(text(),'Отменить')]")
    AtlasWebElement cancelScheduleCancelButton();

    @Name("Список текущих исключений")
    @FindBy("//div[(@class='mdl-list__item mdl-list__item--flat au-target')and not(@class='mdl-list__item mdl-list__item--flat au-target aurelia-hide')]/button[contains(@click.trigger, 'deleteItem') and not (contains(@style, 'hidden'))]")
    ElementsCollection<AtlasWebElement> specialDaysList();

    @Name("Кнопка календаря")
    @FindBy("//div[@class='mdl-list__item mdl-list__item--flat au-target'][{{ index }}]//div[@class='au-target mdl-textfield mdl-textfield--floating-label mdl-list__text-field mdl-list__text-field--25 mdl-js-textfield has-placeholder is-upgraded']//i[@class='mdi mdi-calendar']/..")
    AtlasWebElement specialDaysCalendarButton(@Param("index") int index);

    @Name("Кнопка выбора времени начала исключения")
    @FindBy("//div[@class='mdl-list__item mdl-list__item--flat au-target'][{{ index }}]//label[contains(text(), 'Время начала')]//..//button")
    AtlasWebElement specialDaysTimeOpenButton(@Param("index") int index);

    @Name("Кнопка выбора времени конца исключения")
    @FindBy("//div[@class='mdl-list__item mdl-list__item--flat au-target'][{{ index }}]//label[contains(text(), 'Время окончания')]//..//button")
    AtlasWebElement specialDaysTimeCloseButton(@Param("index") int index);

    @Name("Поле выбора типа исключения")
    @FindBy("//div[@class='mdl-list__item mdl-list__item--flat au-target'][{{ index }}]//input[contains(@id , 'type')]")
    AtlasWebElement specialDaysSelectTypeField(@Param("index") int index);

    //элементы блока параметров

    @Name("Поле ввода параметра")
    @FindBy("//label[contains(text(), '{{ param }}')]/../*[local-name()='input' or local-name()='textarea']")
    AtlasWebElement paramInputField(@Param("param") String param);

    @Name("Окно для ввода значений атрибутов")
    @FindBy("//h4[text()='Атрибуты']/../../../div[@class='mdl-dialog mdl-dialog--fit-content']")
    AttributeForm attributeForm();

    //элементы блока контактов

    @Name("Строка номера")
    @FindBy("//i[contains(@class, '{{ phoneOrFaxNumber }}' )]/ancestor::div/input")
    AtlasWebElement phoneOrFaxNumberString(@Param("phoneOrFaxNumber") String phoneOrFaxNumber);

    @Name("Строка типа телефона")
    @FindBy("//i[contains(@class, 'phone' )]/ancestor::div/span")
    AtlasWebElement phoneTypeString();

    @Name("Кнопка с названием типа телефонного номера")
    @FindBy(".//div[contains(@class, 'is-visible')]/div[contains(text(), '{{ name }}')]")
    AtlasWebElement phoneTypeButton(@Param("name") String name);

    @Name("Кнопка удаления телефона")
    @FindBy(".//i[contains(@class, 'phone')]/ancestor::div[@class = 'mdl-list__item mdl-list__item--flat']//i[@class = 'mdi mdi-delete']")
    AtlasWebElement phoneDelete();

    @Name("Строка e-mail")
    @FindBy("//span[text() = 'e-mail']/ancestor::div/span[1]")
    AtlasWebElement emailString();

    @Name("Строка адреса")
    @FindBy("//contacts-section//div[@class='mdl-list__item-primary-content']/span[1]")
    ElementsCollection<AtlasWebElement> addressStringList();

    @Name("Строка типа адреса")
    @FindBy("//contacts-section//div[@class='mdl-list__item-primary-content']/span[2]")
    ElementsCollection<AtlasWebElement> addressTypeStringList();

    @Name("Кнопка удаления адреса")
    @FindBy(".//i[contains(@class, 'map')]/ancestor::div[@class = 'mdl-list__item mdl-list__item--flat']//i[@class = 'mdi mdi-delete'][1]")
    AtlasWebElement addressDelete();

}

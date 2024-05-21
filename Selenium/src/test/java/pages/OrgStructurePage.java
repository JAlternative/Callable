package pages;

import elements.general.DatePickerForm;
import elements.orgstructure.*;
import elements.orgstructure.card.OrgStructureFilterForm;
import elements.scheduleBoard.*;
import io.qameta.atlas.webdriver.WebPage;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface OrgStructurePage extends WebPage {
    //левая часть экрана
    @Name("Переключение между вкладками ОМ и сотрудники")
    @FindBy("//div[@class='org-structure__filters']/div[1]")
    SwitchToTabs osSwitchToTabs();

    @Name("Форма с действиями на вкладке \"Сотрудники\"")
    @FindBy("//div[@class = 'org-structure__actions']")
    EmpActionForm empActionForm();

    /*
    @Name("Форма с действиями на вкладке \"Подразделения\"")
    @FindBy("//div[@class = 'org-structure__actions']")
    UnitActionForm unitActionForm();
    */
    @Name("Кнопка \"Добавить подразделение\"")
    @FindBy(".//i[@class ='mdi au-target mdi-store-plus']")
    AtlasWebElement addUnitButton();

    @Name("Форма с фильтром")
    @FindBy("//div[@class='org-structure__filters']")
    OrgStructureFilterForm osFilterForm();

    @Name("Форма с типами фильтров для ОМ")
    @FindBy("//div[@hide.bind=\"type === 'userRoles'\"]/div[@class='mdl-list']")
    FilterTypeOmForm filterTypeOmForm();

    @Name("Форма \"Подразделения\" с деревом ОМ вкладка ОМ и сотрудники")
    @FindBy("//schedule-org-unit-select-dialog[contains(@view-model.ref, 'orgUnitDialogBy')]/mdl-dialog/dialog[@open]")
    FilterOmForm filterOmForm();

    @Name("Форма \"Должность\" на вкладке сотрудники")
    @FindBy("//div[@class='mdl-dialog mdl-dialog--fit-content']//h4[text()='Должность']/../..")
    FilterEmpPositionForm filterEmpPositionForm();

    @Name("Вся форма с полем поиска и результатами поиска")
    @FindBy("//div[@class='org-structure__items mdl-shadow--4dp']")
    OrgStructureSearchForm osSearchForm();

    //правая часть экрана
    @Name("Главная карточка сотрудника")
    @FindBy("//div[contains(@class, 'org-structure__card')]/div[contains(@class, 'mdl-list')]")
    EmpCardForm osCardForm();

    @Name("Карточка создания сотрудника")
    @FindBy(".//div[@show.bind='edit[0]']")
    EmpCreateCardForm empCreateCardForm();

    @Name("Карточка создания подразделения")
    @FindBy(".//div[@show.bind='edit[0]']")
    OmCreateCardForm omCreateCardForm();

    @Name("Карточка ОМ")
    @FindBy("//div[contains(@class, 'org-structure__card')]/div[contains(@class, 'mdl-list')]")
    OmInfoForm omInfoForm();

    @Name("Кнопка варианта выбора типа формирования табеля {param}")
    @FindBy(".//div[normalize-space(text()) = '{{ param }}']")
    AtlasWebElement buttonTableType(@Param("param") String param);

    @Name("Карточка ОМ")
    @FindBy("//div[contains(@class, 'org-structure__card')]/div[contains(@class, 'mdl-list')]")
    OmEditingForm omEditingForm();

    @Name("Форма добавления или редактирования должности")
    @FindBy("//div[contains(@class.bind, 'permissionOrgUnitExtPositionEdit')]//div[@show.bind='showContent']")
    AddNewEmployeeForm addNewEmployeeForm();

    @Name("Форма календаря для выбора даты в меню сотрудника")
    @FindBy("//*[contains(@class, 'datetimepicker--open')]")
    DatePickerForm datePickerFormInEmployee();

    @Name("Форма на вкладке \"Подразделения\" \"Теги\"")
    @FindBy("//div[not(contains(@class, 'hide'))]/div[@class='mdl-dialog mdl-dialog--fit-content']")
    TagsForm tagsForm();

    @Name("Форма с данными сотрудника")
    @FindBy("//div[contains(@class,'org-structure__card mdl-shadow--4dp')]")
    EmployeeDataMenu employeeData();

    @Name("Форма выбора и добавления графика работы")
    @FindBy("//h4[@t = 'dialogs.businessHours.title']/../..")
    SelectScheduleForm selectScheduleForm();

    @Name("Форма редактирования роли сотрудника")
    @FindBy("//div[@class = 'org-structure__card mdl-shadow--4dp au-target']")
    elements.orgstructure.EditRoleForm editRoleForm();

    @Name("Форма добавления и редактирования роли")
    @FindBy("//div[contains (@class, 'org-structure__card')]//div[@class = 'mdl-list mdl-list--no-margin']")
    RoleForm roleForm();

    //общие элементы
    @Name("Спиннеры во всем расписании")
    @FindBy("//div")
    SpinnerLoader spinnerLoader();

    @Name("Сообщение \"Ошибка доступа\"")
    @FindBy("//div[text()='Доступ запрещен' or text()='Ошибка доступа']")
    AtlasWebElement accessErrorMessage();

    @Name("Уведомление c заданным текстом")
    @FindBy("//div[@class='mdl-dialog__content']/div[starts-with(text(), \"Нет прав\") and contains(text(), '{{ errorText }}')]")
    AtlasWebElement notificationMessage(@Param("errorText") String errorText);

}

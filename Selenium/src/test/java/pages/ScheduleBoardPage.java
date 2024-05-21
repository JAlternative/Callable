package pages;

import elements.common.Header;
import elements.general.DatePickerForm;
import elements.general.TimePickerForm;
import elements.orgstructure.OmInfoForm;
import elements.reports.MainHeader;
import elements.scheduleBoard.*;
import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.WebPage;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;

public interface ScheduleBoardPage extends WebPage {

    @Name("Module button {{moduleNameButton}}")
    @FindBy("//a[@href='{{ moduleNameButton }}']")
    AtlasWebElement moduleButton(@Param("moduleNameButton") String moduleNameButton);

    @Name("Верхняя панель с датами")
    @FindBy("//div[contains(@class, 'gantt-chart__top mdl-shadow--')]")
    TopBar formTopBar();

    @Name("Настройки подразделения когда нажимаешь на карандаш в форме Свойства подразделения")
    @FindBy("//div[@show.bind=\"show[0]\"]/..")
    OmEditingForm omEditingForm();

    @Name("Форма, выпадающая при нажатии на красный квадратик")
    @FindBy("//div[@show.bind='showScheduleRequestDialog' and not (contains(@class, 'hide'))]")
    ListOfRequest formListOfRequest();

    @Name("Варианты в троеточии")
    @FindBy("//div[@class='menu menu--shadow-16dp au-target is-visible']")
    OrgUnitMenu formOrgUnitMenu();

    @Name("Форма расчета смен")
    @FindBy("//div[@class='mdl-layout__right mdl-layout__right--396 mdl-shadow--16dp au-target']/div/div")
    SetResetTimetableForm formSetResetTimetable();

    @Name("Форма ошибки расчета")
    @FindBy("//dialog[contains(@class, 'result')]")
    PostSRsDialog formPostSRsDialog();

    @Name("Форма публикации в троеточии")
    @FindBy("//div[@show.bind='showShiftsPublishedDialog' and not(contains(@class, 'hide'))]")
    PublishForm formPublishForm();

    @Name("Форма при нажатии на какой либо день в расписании")
    @FindBy("//div[@show.bind='showScheduleRequestDialog' and not(contains(@class, 'hide'))]")
    EditFormSchedule formEditForm();

    @Name("Форма всей страницы не считая шапки")
    @FindBy("//div[@class='mdl-layout__content']")
    Layout formLayout();

    @Name("Окно с ошибкой в процессе выполнения расчета в расчетах смен")
    @FindBy("//h4[@class=\"mdl-dialog__title\"]/..")
    ErrorForm formErrorForm();

    @Name("Окно запроса подтверждения корректировки")
    @FindBy("//dialog[@class='mdl-dialog mdl-dialog--fit-content au-target']/div/h4[text()='Подтверждение корректировки']/../..")
    CorrectionConfirmationDialog correctionConfirmationDialog();

    @Name("Форма описывающая элементы комментариев к версиям расчета и дням")
    @FindBy("//div[contains(@show.bind, 'CommentDialog') and not (contains(@class, 'hide'))]")
    CommentsForm formCommentsForm();

    @Name("Поле ввода комментария")
    @FindBy("//textarea[@id='schedule-request-comment-text']")
    AtlasWebElement commentInput();

    @Name("Форма свойств подразделения")
    @FindBy("//div[@class='mdl-layout__right mdl-layout__right--500 mdl-shadow--16dp']//div[contains(@class,'white')]")
    OmInfoForm subdivisionProperties();

    @Name("Форма должности (после нажатия плюсика в свойствах подразделения в сотрудниках)")
    @FindBy("//h4[@t=\"common.essentials.position\"]/../..")
    AddNewEmployeeForm addNewEmployeeForm();

    @Name("Форма данных о сотруднике, отрыкрывающаяся при нажатии на сотрудника слева от расписания")
    @FindBy("//div[@class= 'load au-target aurelia-hide']/ancestor::div[@class='mdl-layout__content mdl-color--white']")
    EmployeeDataMenu employeeDataMenu();

    @Name("Форма для работы с календарем на масштабе год")
    @FindBy("//div[@class='datetimepicker datetimepicker--date au-target datetimepicker--open']")
    DatePickerForm datePickerForm();

    @Name("Форма выбора и добавления графика работы")
    @FindBy("//h4/../../div[@class='mdl-dialog mdl-dialog--fit-content au-target']")
    SelectScheduleForm selectScheduleForm();

    @Name("Форма для работы с параметрами в свойствах подразделения")
    @FindBy("//div[@class='mdl-dialog mdl-dialog--fit-content']")
    ParameterForm parameterForm();

    @FindBy("//div[@class='datetimepicker datetimepicker--time au-target datetimepicker--open']")
    TimePickerForm timePickerForm();

    @Name("Хедер для всей страницы")
    @FindBy("//header")
    MainHeader mainHeader();

    @Name("Форма при нажатии на печать в троеточии")
    @FindBy("//h4[@t=\"common.essentials.print\"]/../..")
    PrintForm printForm();

    @Name("когда формируем PDF файл , нужно его отдельный интерфейс")
    @FindBy("//html//body")
    PrintPDF printPDFForm();

    @Name("Форма создания события")
    @FindBy("//div[@show.bind='showEventDialog' and not(contains(@class, 'hide'))]")
    EventForm eventForm();

    @Name("Режим сравнения графиков")
    @FindBy("//div[@class='mdl-snackbar au-target mdl-snackbar--active']")
    CompareScheduleMode compareScheduleMode();

    @Name("Режим фильтра сотрудников")
    @FindBy("//div[@show.bind='showEmployeeFilterDialog' and not (contains(@class, 'hide'))]")
    EmployeesFilterMode employeesFilterMode();

    @Name("Спиннеры во всем расписании")
    @FindBy("//div")
    SpinnerLoader spinnerLoader();

    @Name("Карточка ОМ")
    @FindBy("//div[contains(@class, 'mdl-layout__right mdl-layout') and not (contains(@class, 'hide'))]")
    OmInfoForm omInfoForm();

    @Name("Боковая панель навигации по разделам")
    @FindBy("//h4[@t ='dialogs.rejectPublishingRoster.title']/..")
    RejectPublishingRosterDialog rejectPublishingRosterDialog();

    @Name("Форма выгрузки графика")
    @FindBy("//div[@show.bind = 'allowFormatSelection']/../..")
    DownloadForm downloadForm();

    @Name("Поле ввода поиска подразделения")
    @FindBy("//div[@class='menu menu--searchable menu--shadow-16dp au-target is-visible']//input[@class='mdl-textfield__input au-target']")
    AtlasWebElement searchInputFieldOrgUnit();

    @Name("Вся динамическая страница")
    @FindBy("//div[@class='gantt-chart']/div[@class='gantt-chart__chart gantt__container']")
    AtlasWebElement allTable();

    @Name("Верхнее панель для меню выбора разделов и названия страницы")
    @FindBy("//header[@class='mdl-layout__header au-target is-casting-shadow']")
    Header commonHeader();

    @Name("Окно с ошибкой при создании смены")
    @FindBy("//dialog//div[@class='mdl-dialog__content']/div[contains(text(), '{{ errorText }}')]")
    AtlasWebElement errorMessage(@Param("errorText") String errorText);

    @Name("Кнопка закрыть в окне с ошибкой при создании смены")
    @FindBy("//div[contains(text(), '{{ errorText }}')]/ancestor::dialog/div[@class='mdl-dialog__actions']/button[@click.trigger='close()']")
    AtlasWebElement errorMessageClose(@Param("errorText") String errorText);

    @Name("")
    @FindBy("//dialog[@class='mdl-dialog mdl-dialog--fit-content au-target']//div[@class='mdl-dialog__head au-target']")
    ConfirmationWindow confirmationWindow();

    @Name("Окно подтверждения начала расчета")
    @FindBy("//dialog[@ref='dialogElement']")
    CalculationConfirmationWindow calculationConfirmationWindow();

    @Name("Окно со списком свободных смен")
    @FindBy(".//div[@class='mdl-dialog mdl-dialog--scroll mdl-dialog--800']")
    FreeShiftWindow freeShiftList();

    @Name("Форма \"Мастер планирования\"")
    @FindBy(".//div[@show.bind='showScheduleWizardDialog']/div/div/div[@class='schedule-wizard']")
    ScheduleWizardForm scheduleWizardForm();

    @Name("Окно с ошибкой при возникновении конфликта при утверждении/публикации графика")
    @FindBy(".//h4[text()='Ошибка']/..")
    ConstraintViolationDialog constraintViolationDialog();

    //TODO сообщения пока оставлю здесь. Если их положить в ConstraintViolationDialog, при выполнении теста список сообщений оказывается пустым.
    @Name("Список конфликтов в сообщении об ошибке при утверждении/публикации графика")
    @FindBy(".//button[@show.bind='list.collapsing']/../span")
    ElementsCollection<AtlasWebElement> constrViolationMessages();

    @Name("Список нарушений, открывающийся после нажатия на шеврон сообщения о конфликте")
    @FindBy(".//span[text()='{{ message }}']/../../ul/li")
    ElementsCollection<AtlasWebElement> constrList(@Param("message") String message);

    @Name("Форма утверждения табеля в троеточии")
    @FindBy("//div[@show.bind='showRosterPublishedDialog' and not(contains(@class, 'hide'))]")
    ApprovalForm formApprovalForm();

    @Name("Диалог ввода комментария при удалении смены")
    @FindBy("//shift-deletion-dialog")
    ShiftDeletionDialog shiftDeletionDialog();

    @Name("Ячейка свободной смены для определенного дня месяца")
    @FindBy("(//*[name()='text' and @x and @y and @text-anchor and @class])[{{ day }}]")
    AtlasWebElement freeShiftCellForSpecificDayOfMonth(@Param("day") int day);
}

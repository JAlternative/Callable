package elements.scheduleBoard;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;

import java.time.LocalDate;

public interface Layout extends AtlasWebElement {

    @Name("Всплывающее окно с временем отметки не было отображено")
    @FindBy("//div[@class='gantt-chart__hint au-target']/div")
    ElementsCollection<AtlasWebElement> recordHints();

    @Name("панель с текущими данными, нужна для валидации загрузки страницы")
    @FindBy("//div[@class='gantt-chart__indicator au-target' and not(contains(@class, 'hide'))]")
    AtlasWebElement loadConfirmPanel();

    @Name("Тип индикатора количества смен")
    @FindBy("//span[normalize-space(text()) = '{{ typeShift }}']")
    AtlasWebElement shiftCounterDesignation(@Param("typeShift") String typeShift);

    @Name("Элемент смены или запроса конкретного сотрудника")
    @FindBy(".//*[contains(@id, 'gantt__bar--{{ orderEmployee }}') and @data-start-date='{{ date }}' and @data-item-kind='{{ type }}']")
    AtlasWebElement shiftOrRequestElement(@Param("orderEmployee") int orderEmployee, @Param("date") LocalDate date, @Param("type") String type);

    @Name("Элемент смены")
    @FindBy(".//*[contains(@id, 'gantt__bar--')and @data-start-date and @data-item-kind]")
    AtlasWebElement shiftElement();

    @Name("Элемент смены или запроса конкретного сотрудника")
    @FindBy(".//*[contains(@id, 'gantt__bar--{{ orderEmployee }}-') and @data-start-date='{{ date }}' and @data-item-kind='{{ type }}']")
    ElementsCollection<AtlasWebElement> shiftOrRequestElements(@Param("orderEmployee") int orderEmployee, @Param("date") LocalDate date, @Param("type") String type);

    @Name("Элемент смены конкретного сотрудника")
    @FindBy(".//*[contains(@id, 'gantt__bar--{{ orderEmployee }}-') and @data-start-date='{{ date }}' and @data-item-kind='SHIFT' and @data-worked='{{ worked }}']/*[contains(@class,'gantt__bar')]")
    ElementsCollection<AtlasWebElement> shiftElementWorked(@Param("orderEmployee") int orderEmployee, @Param("date") LocalDate date, @Param("worked") boolean worked);

    @Name("Элемент смены, переходящей между месяцами")
    @FindBy(".//*[contains(@id, 'gantt__bar--{{ orderEmployee }}-') and @data-start-date='{{ date }}' and @data-item-kind='SHIFT' and @data-worked='{{ worked }}']/*[contains(@class,'gantt__bar gantt__outside-close')]")
    ElementsCollection<AtlasWebElement> shiftElementOutside(@Param("orderEmployee") int orderEmployee, @Param("date") LocalDate date, @Param("worked") boolean worked);

    @Name("Элемент смены, переходящей между месяцами")
    @FindBy(".//*[contains(@id, 'gantt__bar--{{ orderEmployee }}-') and @data-start-date='{{ date }}' and @data-item-kind='SHIFT' and @data-worked='{{ worked }}']/*[contains(@class,'gantt__bar gantt__default')]")
    ElementsCollection<AtlasWebElement> shiftElementDefault(@Param("orderEmployee") int orderEmployee, @Param("date") LocalDate date, @Param("worked") boolean worked);

    @Name("Элемент, обозначающий отличие смены от плана")
    @FindBy(".//*[contains(@id, 'gantt__bar--{{ orderEmployee }}-') and @data-start-date='{{ date }}' and @data-item-kind='SHIFT' and @data-worked='true']/*[contains(@class, 'gantt__plan-mismatch')]")
    ElementsCollection<AtlasWebElement> shiftPlanMismatch(@Param("orderEmployee") int orderEmployee, @Param("date") LocalDate date);

    @Name("Элемент доп. работы конкретного сотрудника")
    @FindBy(".//*[contains(@id, 'gantt__bar--{{ orderEmployee }}-') and @data-start-date='{{ date }}' and @data-additional-work]")
    ElementsCollection<AtlasWebElement> additionalWorkElement(@Param("orderEmployee") int orderEmployee, @Param("date") LocalDate date);

    @Name("Цвет или ширина смены на панели расписания")
    @FindBy(".//*[@id='{{ idShift }}']/*[contains(@class, 'gantt__bar gantt')]")
    AtlasWebElement getInfo(@Param("idShift") String idShift);

    @Name("Кнопки имен сотрудников")
    @FindBy(".//div[contains(@show.bind,'!employeePosition._hidden') and not (contains(@class, 'aurelia-hide'))]//span[contains(@click.trigger, 'openEmployeeDialog(employeePosition)')]")
    ElementsCollection<AtlasWebElement> employeeNameButtons();

    @Name("Названия должностей аутстафф-сотрудников")
    @FindBy(".//div[@class='gantt-chart__title']")
    ElementsCollection<AtlasWebElement> outStaffPositions();

    @Name("Кнопки имен сотрудников, включая имена с иницалами и проч.")
    @FindBy(".//div[contains(@class, 'mdl-list__item mdl-list__item--overflow gantt-chart__item-shift mdl-list__item--two-line au-target') and not (contains(@class, 'aurelia-hide'))]//div[contains(@id, 'schedule-board-full-name-')]")
    ElementsCollection<AtlasWebElement> allEmployeeNameButtons();

    @Name("Всплывающее окно подсказки с ФИО сотрудника")
    @FindBy("//div[@class='mdl-tooltip au-target is-active']")
    AtlasWebElement fullNameHint();

    @Name("Кнопка должности у сотрудника")
    @FindBy(".//div[@id='schedule-board-full-name-{{ employeeId }}']/ancestor::div/span/span[@class='gantt-chart__sub-title-text au-target link']")
    AtlasWebElement employeePositionsButtons(@Param("employeeId") int employeeId);

    @Name("Тег у сотрудника")
    @FindBy(".//div[@id='schedule-board-full-name-{{ employeeId }}']/ancestor::div/compose/div/div[contains(@class,'gantt-chart__tag au-target')]")
    ElementsCollection<AtlasWebElement> employeeTags(@Param("employeeId") int employeeId);

    @Name("Количество часов отображаемое в поле сотрудника")
    @FindBy(".//div[@id='schedule-board-full-name-{{ employeeId }}']/ancestor::div/span/span/span/span[@class.bind]")
    AtlasWebElement employeeHoursWorked(@Param("employeeId") int employeeId);

    @Name("Кнопка редактирования данных")
    @FindBy(".//div[@id='schedule-board-full-name-{{ employeeId }}']/span/span/span[1]/..")
    AtlasWebElement nameButton(@Param("employeeId") int employeeId);

    @Name("Горизонтальные линии")
    @FindBy("//*[@class = 'gantt__line gantt__line--horizontal']")
    ElementsCollection<AtlasWebElement> horizontalLine();

    @Name("Вертикальные линии")
    @FindBy("//*[@class = 'gantt__line gantt__line--vertical']")
    ElementsCollection<AtlasWebElement> verticalLine();

    @Name("Рамка выделения смен при перемещении и копировании")
    @FindBy("//*[contains(@class, 'gantt__selector')]")
    AtlasWebElement blueFrame();

    @Name("Панель с кнопками удаления, перемещения, дублирования смен")
    @FindBy(".//div[@class = 'gantt-chart__hint au-target']")
    AtlasWebElement shiftActionPanel();

    @Name("Кнопка \"плюс\" при наведении мыши на ячейку смены")
    @FindBy(".//div[contains(@show.bind, 'EXPAND')]")
    AtlasWebElement addRequestButton();

    @Name("Кнопка \"Переместить\" при перемещении одной смены")
    @FindBy(".//button[contains(@click.trigger, 'replaceShift()')]")
    AtlasWebElement replaceShift();

    @Name("Кнопка \"Дублировать\" при перемещении одной смены")
    @FindBy(".//button[contains(@click.trigger, 'duplicateShift()')]")
    AtlasWebElement duplicateShift();

    @Name("Кнопка \"Переместить\" при перемещении нескольких смен")
    @FindBy(".//button[contains(@click.trigger, 'moveScheduleRequests(replaceShifts)')]")
    AtlasWebElement replaceMassShift();

    @Name("Кнопка \"Дублировать\" при перемещении нескольких смен")
    @FindBy(".//button[contains(@click.trigger, 'moveScheduleRequests(duplicateShifts)')]")
    AtlasWebElement duplicateMassShift();

    @Name("Кнопка удаления смен")
    @FindBy(".//button[contains(@click.trigger, 'deleteShifts()')]")
    AtlasWebElement deleteMassShift();

    @Name("Отметка у сотрудника по его индексу")
    @FindBy("//*[@id='gantt__bar--{{ empIndex }}-{{ recordIndex }}' and (contains(@class, 'mark'))]")
    AtlasWebElement record(@Param("empIndex") int empIndex, @Param("recordIndex") int recordIndex);

    @Name("Pop-up с сообщением о пересечении смены и запроса")
    @FindBy("//div[contains(text(), 'Пересечение с запросом расписания в подразделении')]")
    AtlasWebElement popUpShiftAndRequestOverlap();

    @Name("Pop-up после удаления нескольких смен")
    @FindBy("//div[text()= 'Смены удалены']")
    AtlasWebElement popUpForMassDeleteShift();

    @Name("Pop-up после создания смен/запросов расписания через мастер планирования")
    @FindBy("//div[text()= 'Смены успешно созданы']")
    AtlasWebElement popUpForScheduleWizard();

    @Name("Pop-up после изменения события")
    @FindBy("//div[text()= 'Событие изменено']")
    AtlasWebElement popUpForEditEvent();

    @Name("Pop-up после создания события")
    @FindBy("//div[text()= 'Событие создано']")
    AtlasWebElement popUpForCreateEvent();

    @Name("Pop-up после нажатия кнопки На утверждение")
    @FindBy("//div[contains(@class, 'mdl-snackbar') and not(contains(@aria-hidden, 'true'))]/div")
    AtlasWebElement popUpForApproval();

    @Name("Pop-up после создания смены в мастере планирования")
    @FindBy("//div[contains(text(), 'Сумма часов за дату {{ date }} у сотрудника {{ employeeFullName }} более 24 часов')]")
    AtlasWebElement popUpForHourLimitExceeded(@Param("date") LocalDate date, @Param("employeeFullName") String employeeFullName);

    @Name("Все элементы эвентов")
    @FindBy("//*[contains(@class, 'gantt__event gantt__interactive')]")
    ElementsCollection<AtlasWebElement> allEventElements();

    @Name("Кнопка изменить в меню события")
    @FindBy("//button[@click.trigger = 'edit(event)' and text() = 'Изменить ']")
    ElementsCollection<AtlasWebElement> eventChangeButton();

    @Name("Серая кнопка добавления смены")
    @FindBy("//*[@id = 'gantt-add']")
    AtlasWebElement greyAddShiftButton();

    @Name("Какая-то панелька под расписанием, вероятно в будущем легенды")
    @FindBy("//div[@class ='gantt-chart__legend au-target']")
    AtlasWebElement underTable();

    @Name("Количество фактических часов у сотрудника")
    @FindBy(".//div[@id='schedule-board-full-name-{{ employeeId }}']/ancestor::div/span/span/span[@mouseover.trigger]/span[1]")
    AtlasWebElement employeeFactWorkingHours(@Param("employeeId") int employeeId);

    @Name("Количество плановых часов у сотрудника")
    @FindBy(".//div[@id='schedule-board-full-name-{{ employeeId }}']/ancestor::div/span/span/span[@mouseover.trigger]/span[2]")
    AtlasWebElement employeePlanWorkingHours(@Param("employeeId") int employeeId);

    @Name("Строки в всплывающем окне информации часов у сотрудника")
    @FindBy(".//span[contains(@class,'row-header')]//span[contains(@t , 'views.shifts')]|//span[contains(@class,'row-header')]//span[contains(@t , 'common.essentials')]")
    ElementsCollection<AtlasWebElement> linesInEmployeeWorkingHours();

    /**
     * Внимание: структура страницы для разных строк списка немного отличается. Необходимо проверять работу локатора для каждой строки
     */
    @Name("Количество часов в определенной строке")
    @FindBy(".//span[contains(@class,'row-header') and contains(@style, 'display: block')]//span[contains(text(),'{{ line }}')]/..")
    AtlasWebElement numberOfHoursInEmployeeToolTip(@Param("line") String line);

    @Name("Всплывающий элемент с количеством часов у сотрудника")
    @FindBy("//span[contains(@style,'display: block')]")
    AtlasWebElement employeeWorkingHoursPopUp();

    @Name("Цветные строки над расписанием со статусом графика")
    @FindBy("//*[contains(@class, 'gantt__indicator-gantt')][@width]")
    ElementsCollection<AtlasWebElement> colorLinesOnTopSchedule();

    @Name("Название табеля учета и планового графика")
    @FindBy("//*[starts-with(@class,'gantt__indicator-gantt-text--')]")
    ElementsCollection<AtlasWebElement> timesheetIndicator();

    @Name("Фон названия табеля учета и планового графика")
    @FindBy("//*[starts-with(@class,'gantt__indicator-gantt--')]")
    ElementsCollection<AtlasWebElement> timesheetIndicatorBackground();

    @Name("Серый прямоугольник с названием табеля")
    @FindBy(".//*[contains(@class,'gantt__default')][contains(@class,'gantt__indicator-gantt')]")
    ElementsCollection<AtlasWebElement> greyTimesheetIndicator();

    @Name("Индикаторы количества свободных смен")
    @FindBy(".//*[contains(@class,'gantt__withe-text')]")
    ElementsCollection<AtlasWebElement> freeShiftIndicators();

    @Name("Плюс, появляющийся над сменой при наведении на нее мышкой")
    @FindBy(".//div[@class='gantt-chart__hint au-target']/button")
    AtlasWebElement plusButton();

    @Name("Кнопка \"Назад\"")
    @FindBy(".//button[contains(@show.bind, 'canNavigateBackward')]")
    AtlasWebElement navigateBackButton();

    @Name("Кнопка \"Вперед\"")
    @FindBy(".//button[@show.bind='!scheduleBoardMultiSelector']")
    AtlasWebElement navigateForwardButton();

    @Name("Индикаторы над расписанием")
    @FindBy("//*[contains(@class, 'gantt-chart__indicator-name')]")
    ElementsCollection<AtlasWebElement> indicators();

    @Name("Кружочек с конфликтом над расписанием")
    @FindBy(".//*[contains(@class, 'gantt__conflict')]")
    ElementsCollection<AtlasWebElement> conflictCircles();

    @Name("Надпись \"Конфликты\" над расписанием")
    @FindBy(".//span[@t.bind='indicator.properties.name' and text()='Конфликты']")
    AtlasWebElement textConflicts();

    @Name("Текст конфликта")
    @FindBy("//div[contains(text(), '{{ text }} {{ name }}')]")
    ElementsCollection<AtlasWebElement> textInConflictCircle(@Param("text") String text, @Param("name") String name);

    @Name("Текст конфликта")
    @FindBy("//div[contains(text(), 'У сотрудника {{ name }} {{ text }}')]")
    ElementsCollection<AtlasWebElement> textInConflictCircleOnEFES(@Param("text") String text, @Param("name") String name);

    @Name("Жирный шрифт кнопки сотрудника")
    @FindBy(".//div[@id='schedule-board-full-name-{{ employeeId }}']/span/span[contains(@class,'mdl-typography--font-bold')]")
    AtlasWebElement employeeButtonFontBold(@Param("employeeId") int employeeId);

    @Name("Атрибуты во всплывающем окне при наведении на норму часов сотрудника")
    @FindBy(".//div[@id='schedule-board-full-name-{{ employeeId }}']/../../../..//span[@class.bind = 'value._style']")
    ElementsCollection<AtlasWebElement> attributesInPopup(@Param("employeeId") int employeeId);

    @Name("Отметка о присутствии сотрудника")
    @FindBy(".//*[contains(@id, 'gantt__bar--{{ orderEmployee }}-') and contains(@class, 'gantt__mark  gantt__interactive')]")
    AtlasWebElement presenceMark(@Param("orderEmployee") int orderEmployee);

    @Name("Надпись \"Предупреждение\" при изменении лимита часов на подразделение")
    @FindBy("//div[@innerhtml.bind='model.text']")
    ElementsCollection<AtlasWebElement> limitWarning();

    @Name("Окно с ошибкой об отсутствии пермишена")
    @FindBy("//div[@innerhtml.bind='model.text']")
    AtlasWebElement warning();

    @Name("Надпись все смены")
    @FindBy("//span[contains(text(),'Все смены')]")
    AtlasWebElement allShifts();

    @Name("Должность сотрудника по его имени (актуально для мобильных смен)")
    @FindBy("//span[contains(text(),'{{ name }}')]/ancestor::*[4]//span[contains(text(), '{{ positions }}')]")
    ElementsCollection<AtlasWebElement> employeePositionsByName(@Param("name") String name, @Param("positions") String positions);
    @Name("Часы в ячейке смены")
    @FindBy(".//*[contains(@id, 'gantt__bar--{{ orderEmployee }}-') and @data-start-date='{{ date }}' and @data-item-kind='SHIFT' and @data-worked='{{ worked }}']/*[2]/*[{{ firstOrSecond }}]")
    AtlasWebElement shiftHours(@Param("orderEmployee") int orderEmployee, @Param("date") LocalDate date, @Param("worked") boolean isWorked, @Param("firstOrSecond") int firstOrSecond);
}

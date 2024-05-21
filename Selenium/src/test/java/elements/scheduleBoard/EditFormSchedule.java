package elements.scheduleBoard;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface EditFormSchedule extends AtlasWebElement {

    @Name("Меню троеточия")
    @FindBy(".//button[@id='schedule-request-menu']")
    AtlasWebElement buttonDotsMenu();

    @Name("Кнопка крестика, закрыть меню редактирования смены")
    @FindBy(".//div[@class='menu__container']/..//i[contains(@class, 'mdi-close')]")
    AtlasWebElement closeMenu();

    @Name("Спиннер")
    @FindBy(".//div[contains(@class,'mdl-spinner')]")
    AtlasWebElement spinner();

    @Name("Поле \"Сотрудник\" при нажатии на которое выходят все сотрудники")
    @FindBy(".//input[@id='schedule-request-employee']")
    AtlasWebElement employeeNameListOpen();

    @Name("Поле \"Название должности\" при нажатии на которое выходят все варианты должностей")
    @FindBy(".//input[@id='schedule-request-job-title-select']")
    AtlasWebElement jobTitleOpen();

    @Name("Все варианты должностей")
    @FindBy(".//input[@id='schedule-request-job-title-select']//..//div[@class='menu__item au-target']")
    ElementsCollection<AtlasWebElement> jobTitleList();

    @Name("Кнопка \"Удалить\" в меню троеточия")
    @FindBy("//div[@class='menu__item au-target'][contains(text(),'Удалить')]")
    AtlasWebElement buttonDelete();

    @Name("Кнопка \"Изменить\" в меню троеточия")
    @FindBy("//div[@class='menu__item au-target'][contains(text(),'Изменить')]")
    AtlasWebElement buttonChange();

    @Name("Кнопка \"Изменить\" в меню смены")
    @FindBy("//button[@class = 'mdl-button mdl-button--primary au-target mdl-js-button']//span[contains(text(),'Изменить')]")
    AtlasWebElement buttonChangeInShiftEdit();

    @Name("Кнопка \"Подтвердить\" в меню троеточия")
    @FindBy("//div[@class='menu__item au-target'][contains(text(),'Подтвердить')]")
    AtlasWebElement buttonConfirm();

    @Name("Кнопка \"Сохранить\"")
    @FindBy(".//button[@class='mdl-button mdl-button--primary au-target mdl-js-button']//span[@t='common.actions.changeButton']")
    AtlasWebElement buttonConfirmEdit();

    @Name("Кнопка \"Создать\"")
    @FindBy("//*[@click.trigger = 'createScheduleRequest()']")
    AtlasWebElement buttonCreateShift();
    @Name("Кнопка \"Изменить\"")
    @FindBy("//*[@click.trigger='editScheduleRequest()']")
    AtlasWebElement buttonChangeShift();

    @Name("Кнопка выбора времени начала смены")
    @FindBy(".//label[text()='{{ start }}']/ancestor::div/../div[contains(@class,'mdl-list__text-field--33')]/button")
    AtlasWebElement buttonStartTimeShift(@Param("start") String start);

    @Name("Кнопка выбора времени конца смены")
    @FindBy("(.//label[text()='{{ end }}']/ancestor::div/../div[contains(@class,'mdl-list__text-field--33')]/button)[2]")
    AtlasWebElement buttonEndTimeShift(@Param("end") String end);

    @Name("Поле выбора времени начала смены")
    @FindBy(".//label[text()='{{ start }}']/ancestor::div/../div[contains(@class,'mdl-list__text-field--33')]/input")
    AtlasWebElement inputStartTimeShift(@Param("start") String start);

    @Name("Поле выбора времени конца смены")
    @FindBy("(.//label[text()='{{ end }}']/ancestor::div/../div[contains(@class,'mdl-list__text-field--33')]/input)[2]")
    AtlasWebElement inputEndTimeShift(@Param("end") String end);

    @Name("Поле ввода даты начала или конца")
    @FindBy(".//label[text()='{{ endOrStart }}']/../input")
    AtlasWebElement dateStartOrEndInput(@Param("endOrStart") String endOrStart);

    @Name("Шеврон выбора типа смены")
    @FindBy("//label[@for='schedule-request-type']/../button")
    AtlasWebElement selectTypeButton();

    @Name("Кнопки с вариантами типов смен")
    @FindBy(".//div[@class='menu au-target is-visible']//div[@click.delegate='selectScheduleRequestType(type)' and (contains(text(), '{{ type }}'))]")
    AtlasWebElement typeButton(@Param("type") String type);

    @Name("Временная кнопка для запроса смен")
    @FindBy("//div[@click.delegate='selectScheduleRequestType(type)'][2]")
    AtlasWebElement scheduleRequestButton();

    @Name("Кнопки с вариантами типов смен")
    @FindBy(".//div[@class='menu au-target is-visible']//div[@click.delegate='selectScheduleRequestType(type)']")
    ElementsCollection<AtlasWebElement> typeButtons();

    @Name("Иконка графика")
    @FindBy(".//i[@class = 'mdi mdi-chart-line mdl-list__item-icon']")
    AtlasWebElement graphIcon();

    @Name("Меню с действиями")
    @FindBy("//div[@class='menu menu--right au-target is-visible']")
    AtlasWebElement menu();

    @Name("Текст с ошибкой подсвеченное красным")
    @FindBy(".//div[contains(@class, 'is-invalid')]//span[contains(@class, 'error')]")
    AtlasWebElement errorTextField();

    @Name("Поле выбора комментария к смене")
    @FindBy("//textarea[@id = 'schedule-request-comment-text']")
    AtlasWebElement commentInputField();

    @Name("Кнопка выбора комментария по его имени")
    @FindBy("//div[contains(@class, 'visible')]/div[contains(text(), '{{ name }}')]")
    AtlasWebElement commentByNameButton(@Param("name") String name);

    @Name("Кнопка \"добавить +\" вид работы")
    @FindBy(".//button[@click.trigger='addEmptyAdditionalWork()' and not(@disabled)]")
    AtlasWebElement addAdditionalWorkButton();

    @Name("Кнопка удаления доп. работы")
    @FindBy(".//button[contains(@id, 'schedule-item-additional-work-menu-')]")
    AtlasWebElement deleteAdditionalWorkButton();

    @Name("Строка в меню удаления доп. работы")
    // fixme их может быть несколько, если у смены несколько доп. работ. В этом случае надо будет докрутить механизм распознавания того, какую работу надо удалить.
    @FindBy(".//div[@click.delegate='removeAdditionalWork(additionalWork)']")
    AtlasWebElement lineInDeleteAdditionalWorkButtonMenu();

    @Name("Поле ввода типа дополнительной работы")
    @FindBy(".//input[contains(@id, 'shift-additional-work-type-0')]")
    AtlasWebElement additionalWorkTypeInput();

    @Name("Тип дополнительной работы из выпадающего списка")
    @FindBy(".//input[contains(@id, 'shift-additional-work-type-0')]/following-sibling::div" +
            "/div[contains(@click.delegate,'setAdditionalWorkType') and contains(text(), '{{ name }}')]")
    AtlasWebElement additionalWorkTypeListItem(@Param("name") String name);

    @Name("Поле ввода даты начала или конца доп. работы")
    @FindBy(".//span[text()='Вид работы'][@t='common.essentials.additionalWork']/../following-sibling::div" +
            "//label[text()='{{ endOrStart }}']/../input")
    AtlasWebElement dateStartOrEndInputAdditionalWork(@Param("endOrStart") String endOrStart);

    @Name("Кнопка выбора времени конца или начала допработы")
    @FindBy(".//span[text()='Вид работы'][@t='common.essentials.additionalWork']/../following-sibling::div" +
            "//label[text()='{{ endOrStart }}']/../../div[contains(@class,'mdl-list__text-field--33')]/button")
    AtlasWebElement buttonStartOrEndTimeAdditionalWork(@Param("endOrStart") String endOrStart);

    @Name("Поле выбора времени конца или начала допработы")
    @FindBy(".//span[text()='Вид работы'][@t='common.essentials.additionalWork']/../following-sibling::div" +
            "//label[text()='{{ endOrStart }}']/../../div[contains(@class,'mdl-list__text-field--33')]/input")
    AtlasWebElement inputStartOrEndTimeAdditionalWork(@Param("endOrStart") String endOrStart);

    @Name("Кнопка для выбора статуса доп. работы")
    @FindBy(".//input[contains(@id, 'shift-additional-work-status')]/../button")
    AtlasWebElement additionalWorkStatusChevron();

    @Name("Строка с названием нужного статуса")
    @FindBy(".//div[normalize-space(text())='{{ status }}']")
    AtlasWebElement statusName(@Param("status") String status);

    @Name("Поле ввода/выбора причины привлечения сотрудника")
    @FindBy(".//textarea[@id='schedule-request-hiring-reason-text']")
    AtlasWebElement hiringReasonInput();

    @Name("Причина привлечения сотрудника из выпадающего списка")
    @FindBy(".//div[@menu='schedule-request-hiring-reason-text']/div[contains(text(),'{{ reason }}')]")
    AtlasWebElement hiringReasonOption(@Param("reason") String reason);

    @Name("Причина привлечения сотрудника из выпадающего списка")
    @FindBy(".//div[@menu='schedule-request-hiring-reason-text']/div")
    ElementsCollection<AtlasWebElement> hiringReasonOptions();

    @Name("Поле ввода/выбора категории позиции сотрудника")
    @FindBy(".//input[@id='schedule-request-position-category']")
    AtlasWebElement positionCategoryInput();

    @Name("Категория позиции сотрудника из выпадающего списка")
    @FindBy(".//div[@click.delegate='selectPositionCategory(positionCategory)' and contains(text(), '{{ category }}')]")
    AtlasWebElement positionCategoryOption(@Param("category") String category);

    @Name("Поле ввода/выбора категории позиции сотрудника")
    @FindBy(".//input[@id='schedule-request-position-group']")
    AtlasWebElement positionGroupInput();

    @Name("Категория позиции сотрудника из выпадающего списка")
    @FindBy(".//div[@click.delegate='selectPositionGroup(item)' and contains(text(), '{{ group }}')]")
    AtlasWebElement positionGroupOption(@Param("group") String group);

    @Name("Поле выбора периодичности для неявки")
    @FindBy(".//input[@id='schedule-request-periodicity']")
    AtlasWebElement periodicityInput();

    @Name("Выбор периодичности запроса отсутствия из выпадающего списка")
    @FindBy(".//div[normalize-space(text())='{{ period }}' and @click.delegate='selectPeriodicityType(item)']")
    AtlasWebElement requestPeriod(@Param("period") String period);

    @Name("Поле ввода даты окончания повтора")
    @FindBy(".//i[@class='mdi mdi-repeat mdl-list__item-icon']/../../..//label[normalize-space(text())='Дата окончания повтора']/../input")
    AtlasWebElement endRepeatDate();

    @Name("Выпадающий текст с ошибкой")
    @FindBy(".//label[contains(text(),'{{ fieldName }}')]/following-sibling::span[@class='mdl-textfield__error' and string-length(text()) > 0]")
    AtlasWebElement errorMessage(@Param("fieldName") String fieldName);

    @Name("Выпадающий список, после нажатия кнопки удаления")
    @FindBy("//h4[contains(text(),'Подтверждение корректировки')]")
    AtlasWebElement confirmCorrection();

    @Name("Сохранить изменения корректировки")
    @FindBy("//button[contains(text(),'сохранить изменения')]")
    AtlasWebElement saveChangesCorrection();

    @Name("Не сохранять изменения корректировки")
    @FindBy("//button[contains(text(),'не сохранять')]")
    AtlasWebElement notSaveChangesCorrection();
}

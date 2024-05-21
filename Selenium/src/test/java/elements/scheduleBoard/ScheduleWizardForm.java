package elements.scheduleBoard;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface ScheduleWizardForm extends AtlasWebElement {

    @Name("Список сотрудников")
    @FindBy(".//div[contains(@click.trigger,'config.onClick')]")
    ElementsCollection<AtlasWebElement> employeeList();

    @Name("Чекбокс для выбора сотрудника {{ employeeName }}")
    @FindBy(".//span[normalize-space(text())='{{ employeeName }}' and contains (@id, 'schedule-wizard-full-name')]/../following-sibling::div/label/span[@class='mdl-checkbox__box-outline']/following-sibling::span")
    AtlasWebElement employeeCheckbox(@Param("employeeName") String employeeName);

    @Name("Поле ввода даты начала цикла или окончания")
    @FindBy(".//label[text()='{{ startOrEnd }}']/../input")
    AtlasWebElement dateStartOrEndInput(@Param("startOrEnd") String startOrEnd);

    @Name("Поле ввода времени")
    @FindBy(".//label[text()='{{ time }}']/preceding-sibling::input[contains(@id, 'time-range-input')]")
    AtlasWebElement timeInput(@Param("time") String time);

    @Name("Кнопка \"добавить +\" вид работы")
    @FindBy(".//button[@click.trigger='addAdditionalWork(0)' and not(@disabled)]")
    AtlasWebElement addAdditionalWorkButton();

    @Name("Рычажок \"задать цикличность\"")
    @FindBy(".//span[text()='Задать цикличность']/../div/mdl-checkbox/label/span[contains(@class, 'mdl-switch__ripple-container')]")
    AtlasWebElement setCycleArm();

    @Name("Поле ввода \"{{ typeOfDays }}\"")
    @FindBy(" .//span[text()='{{ typeOfDays }}']/preceding-sibling::input")
    AtlasWebElement typeOfDaysNumber(@Param("typeOfDays") String typeOfDays);

    @Name("Кнопка \"добавить +\" перерыв")
    @FindBy(".//span[contains(text(), 'Перерыв')]//..//i[contains(@class,'plus')]")
    AtlasWebElement addBreakButton();

    @Name("Кнопка \"+\" для добавления перерыва")
    @FindBy(".//button[@click.trigger='addWorkBreak()']/i")
    AtlasWebElement addBreakButtonPlus();

    @Name("Шеврон для выбора типа запроса")
    @FindBy(".//label[contains(@for, 'schedule-wizard-schedule-request-type')]/following-sibling::button")
    AtlasWebElement addRequestTypeChevron();

    @Name("Кнопки с вариантами типов смен")
    @FindBy(".//div[@class='menu menu--short au-target is-visible']/div[@click.delegate='setScheduleRequestAlias(alias)']")
    ElementsCollection<AtlasWebElement> typeButtons();

    @Name("Шеврон для выбора типа работ")
    @FindBy(".//label[contains(@for, 'schedule-wizard-add-work-type')]/following-sibling::button")
    AtlasWebElement addWorkTypeChevron();

    @Name("Тип доп работы {{ addWorkTitle }} в выпадающем списке")
    @FindBy(".//div[contains(@menu, 'schedule-wizard-add-work-type')]/div[contains(normalize-space(text()), '{{ addWorkTitle }}')]")
    AtlasWebElement addWorkTypeInList(@Param("addWorkTitle") String addWorkTitle);

    @Name("Время доп работы")
    @FindBy(".//div[contains(@menu, 'schedule-wizard-add-work-type')]/../../../following-sibling::div/div/div/input[contains(@id,'time-range-input')]")
    AtlasWebElement addWorkTypeTime();

    @Name("Количество рабочих смен в цикле")
    @FindBy(".//input[@value.bind='model.days']")
    AtlasWebElement workDaysInCycle();

    @Name("Количество выходных в цикле")
    @FindBy(".//input[@value.bind='model.freeDays']")
    AtlasWebElement freeDaysInCycle();

    @Name("Кнопка \"Сформировать\"")
    @FindBy(".//button[contains(text(), 'Сформировать')]")
    AtlasWebElement formButton();
}

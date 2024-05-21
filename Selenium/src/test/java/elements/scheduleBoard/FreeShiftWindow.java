package elements.scheduleBoard;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;

import java.util.List;

public interface FreeShiftWindow extends AtlasWebElement {
    @Name("Время начала конкретной смены")
    @FindBy(".//input[@id='free-shifts-start-date-{{ number }}']")
    AtlasWebElement startDate(@Param("number") int number);

    @Name("Время окончания конкретной смены")
    @FindBy(".//input[@id='free-shifts-end-date-{{ number }}']")
    AtlasWebElement endDate(@Param("number") int number);

    @Name("Выпадающий список сотрудников для конкретной смены")
    @FindBy(".//input[@id='free-shifts-employee-{{ number }}']")
    AtlasWebElement employee(@Param("number") int number);

    @Name("Позиция конкретной смены")
    @FindBy(".//label[@for='free-shifts-employee-{{ number }}']")
    AtlasWebElement position(@Param("number") int number);

    @Name("Время начала всех смен")
    @FindBy(".//input[contains(@id,'free-shifts-start-date-')]")
    List<AtlasWebElement> startDates();

    @Name("Время окончания всех смен")
    @FindBy(".//input[contains(@id,'free-shifts-end-date-')]")
    List<AtlasWebElement> endDates();

    @Name("Выпадающие списки сотрудников для всех смен")
    @FindBy(".//input[contains(@id,'free-shifts-employee-')]")
    List<AtlasWebElement> employees();

    @Name("Позиции всех смен")
    @FindBy(".//label[contains(@for,'free-shifts-employee-')]")
    List<AtlasWebElement> positions();

    @Name("Кнопка меню смены")
    @FindBy(".//button[contains(@id,'free-shifts-menu-{{ order }}')]")
    AtlasWebElement threeDotsMenu(@Param("order") int order);

    @Name("Кнопка меню показываемой смены")
    @FindBy(".//div[contains(@menu,'free-shifts-menu-') and contains(@class, 'visible')]/div[contains(text(), '{{ item }}')]")
    AtlasWebElement threeDotsMenuItem(@Param("item") String item);

    @Name("Список сотрудников, которых можно назначить на выбранную свободную смену")
    @FindBy(".//employee-position-info-item[@click.delegate='selectEmployeePosition(shift, employeePosition)']/div[contains(@class, 'menu__item')]")
    ElementsCollection<AtlasWebElement> availableEmployees();

    @Name("Строка загрузки списка сотрудников")
    @FindBy(".//div[contains(@class, 'is-visible')]/div[@show.bind='!shift.employeePositions.length && !loadAdditionalInfo']")
    AtlasWebElement loading();

    @Name("Все сотрудники в выпадающем списке")
    @FindBy(".//div[@class='menu menu--short menu--264 au-target is-visible']//employee-position-info-item[@class='au-target']")
    ElementsCollection<AtlasWebElement> getAllEmployeesElements();

    @Name("Загрузка списка сотрудников")
    @FindBy("//div[@class='menu menu--short menu--264 au-target is-visible']//div[contains(text(),'Загрузка...')]")
    AtlasWebElement employeeListLoader();

    @Name("Кнопка \"Отменить\"")
    @FindBy(".//div[@class='mdl-dialog__actions']//button[contains(text(),'Отменить')]")
    AtlasWebElement cancel();

    @Name("Кнопка \"Сохранить\"")
    @FindBy(".//div[@class='mdl-dialog__actions']//button[contains(text(),'Сохранить')]")
    AtlasWebElement save();
}

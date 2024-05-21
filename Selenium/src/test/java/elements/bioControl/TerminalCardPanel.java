package elements.bioControl;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface TerminalCardPanel extends AtlasWebElement {
    @Name("Кнопка карандаша в верхней части экрана")
    @FindBy("//div[contains(@class, 'shrink')]//i[contains(@class, 'pencil')]")
    AtlasWebElement pencilTerminalButton();

    @Name("Поле с названием терминала во время редактирования")
    @FindBy("//div[contains(text(), 'Название')]/../input")
    AtlasWebElement terminalEditNameField();

    @Name("Поле с названием терминала во время редактирования")
    @FindBy("//div[contains(text(), 'Серийный №')]/../input")
    AtlasWebElement terminalEditSNumberField();

    @Name("Кнопка сохранения информации о терминале")
    @FindBy("//span[contains(text(), 'Сохранить')]/..")
    AtlasWebElement terminalInfoSaveButton();

    @Name("Статус пинкода в карточке терминала")
    @FindBy("//div[contains(text(), 'Пин-код')]/../div[2]")
    AtlasWebElement pinStatus();

    @Name("Кнопка \"Добавить оргЮнит\"")
    @FindBy("//span[contains(text(), 'Добавить оргюнит')]/..")
    AtlasWebElement addOrgunitButton();

    @Name("Кнопка сохранить в выборе оргЮнита")
    @FindBy("//div[contains(@class, 'popup')]//span[contains(text(), 'Сохранить')]")
    AtlasWebElement selectOrgUnitSaveButton();

    @Name("Кнопка переключения статуса")
    @FindBy("//label[contains(@class, 'switch')]/i")
    AtlasWebElement statusRadioButton();

    @Name("Текущий статус терминала при переключении")
    @FindBy("//label[contains(@class, 'switch')]/span")
    AtlasWebElement currentRadioButtonStatus();

    @Name("Текущий статус терминала на его панели")
    @FindBy("//div[contains(@class, 'selectable')]//div[contains(text(), '{{ terminalID }}')]/../..//span[contains(@t, 'status')]")
    AtlasWebElement currentTerminalStatus(@Param("terminalID") String terminalID);

    @Name("Поле ввода пинкода")
    @FindBy("//div[contains(text(), 'Пин-код')]/../input")
    AtlasWebElement pinInputField();

    @Name("Поле ввода времени автономной работы")
    @FindBy("//input[@value.bind='t.blockTimeout']")
    AtlasWebElement blockTimeoutInputFiled();

    @Name("Поле с текущим id терминала")
    @FindBy("//div[contains(text(), 'ID терминала')]/../div[2]")
    AtlasWebElement currentTerminalIdField();

    @Name("Карандаш редактирования сотрудников в терминале")
    @FindBy("//div[@click.delegate='onEditPersonalClicked()']/i")
    AtlasWebElement terminalEmployeesEditPencilButton();

    @Name("Панель загрузки")
    @FindBy("//div[@class='au-target grow is-loading']")
    AtlasWebElement loadingPanel();

    @Name("Панель с текущим статусом автономной работы")
    @FindBy("//div[text()='Автономная работа']/../div[2]")
    AtlasWebElement batteryStatus();

    @Name("Режим редактирования")
    @FindBy("//div[contains(text(), 'Редактировать терминал')]")
    AtlasWebElement editMode();

    @Name("Кнопка перехода в редактирование конфиг файла")
    @FindBy(".//i[contains(@class, 'settings')]")
    AtlasWebElement settingsConfigButton();

    @Name("Список из прикрепленных к терминалу людей по критерию")
    @FindBy("//label[text()='{{ name }}']/../../div[2]")
    ElementsCollection<AtlasWebElement> terminalPersonsList(@Param("name") String name);

}

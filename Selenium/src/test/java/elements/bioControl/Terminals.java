package elements.bioControl;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;
import pages.TreeBehavior;

public interface Terminals extends AtlasWebElement, TreeBehavior {

    @Name("Поле ввода на вкладке таблица")
    @FindBy(".//input[@placeholder = 'Поиск по оргмодулю']")
    AtlasWebElement inputSearch();

    @Name("Поле ввода на вкладке таблица")
    @FindBy("//div[contains(@class, 'popup')]//input[@placeholder = 'Поиск по оргмодулю']")
    AtlasWebElement inputSearchOrgUnitPopUp();

    @Name("Кнопка добавить терминал")
    @FindBy("//span[contains(text(), 'Добавить терминал')]/..")
    AtlasWebElement addNewTerminalButton();

    @Name("Кнопка \"Свободные терминалы\"")
    @FindBy(".//div[@click.delegate = 'onSelectFreeTerminals()']")
    AtlasWebElement freeTerminalButton();

    @Override
    @Name("Шеврон выбранного Юнита")
    @FindBy("//compose[@model.bind = 'permissionprofilingstate']//span[contains(text(), '{{ name }}')]/../..//a")
    AtlasWebElement chevronButton(@Param("name") String name);

    @Override
    @Name("Чек бокс Юнита")
    //возможно раньше были чекбоксы, но сейчас их нет дублирует элемент unitPanel это касается раздела терминал
//    @FindBy("//div[contains(text(), '{{ name }}')]//span[@click.delegate='onCheckOrgunit(row.uuid)' and not(contains(@class, 'marked')) ]")
    @FindBy("//span[contains(text(), '{{ name }}')]/../..//span[@click.delegate = 'onCheckOrgunit(row.uuid)']")
    AtlasWebElement checkBoxButton(@Param("name") String name);

    @Name("Чек бокс Юнита")
    @FindBy("//div[normalize-space(text()) = '{{ name }}']//span[@click.delegate='onCheckOrgunit(row.uuid)' and not(contains(@class, 'marked'))]")
    AtlasWebElement unitCheckBox(@Param("name") String name);

    @Name("Панель выбранного юинта")
    @FindBy("//div[contains(text(), '{{ name }}')]")
    AtlasWebElement unitPanel(@Param("name") String name);

    @Name("Панель с терминалами")
    @FindBy("//div[@class='vertical-bank h-100v pad-left-6 pad-right-6']")
    AtlasWebElement terminalsPanel();

    @Name("Список из айди терминалов")
    @FindBy("//div[@class='text-small'][1]")
    ElementsCollection<AtlasWebElement> terminalsIdList();

    @Name("Список из серийных номер терминалов")
    @FindBy("//div[@class='text-small'][2]")
    ElementsCollection<AtlasWebElement> sNumbersTerminalsList();

    @Name("Список из статусов терминалов")
    @FindBy("//div[@click.delegate=\"onSelect(row)\"]//div[@class=\"col col-2-12\"][2]//span")
    ElementsCollection<AtlasWebElement> terminalsStatusList();

    @Name("Список из названий терминалов")
    @FindBy("//div[@class='text-large'][1]")
    ElementsCollection<AtlasWebElement> terminalsNamesList();

    @Name("Панель перехода в терминал по айди")
    @FindBy("//div[contains(text(), '{{ name }}')]/../../../..")
    AtlasWebElement terminalButtonById(@Param("name") String name);

    @Name("Строка ввода поиска по оргмодулю")
    @FindBy(".//input[@placeholder ='Поиск по оргмодулю']")
    AtlasWebElement inputOrgUnitSearchField();

    @Name("Панель по названию оргЮнита")
    @FindBy("//div[contains(text(), '{{ value }}')]")
    AtlasWebElement orgUnitPanelByName(@Param("value") String value);

    //окно конфиг файла
    @Name("Панель с содержимым конфиг файла")
    @FindBy("//textarea[@innerhtml.bind='getPrettyJson(editedjson,forceUpdate)']")
    AtlasWebElement configFile();

    @Name("Кнопка редактировать")
    @FindBy("//span[@click.delegate='doEdit()']")
    AtlasWebElement editButton();

    @Name("Кнопка сохранить")
    @FindBy("//span[@click.delegate='saveConfig()']")
    AtlasWebElement saveButton();

    @Name("Шеврон выбранного Юнита")
    @FindBy("//div[contains(text(), '{{ name }}')]/../../../..//a[contains(@class, 'chevron')]")
    AtlasWebElement chevronByName(@Param("name") String name);

}

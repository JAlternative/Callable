package elements.bioControl;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface Journal extends AtlasWebElement {

    @Name("Кнопка \"Найти\"")
    @FindBy(".//button[@click.delegate='onRequestNewJournal()']")
    AtlasWebElement findButton();

    @Name("Поле ввода ФИО")
    @FindBy("//input[@value.bind='searchrequest.personname']")
    AtlasWebElement nameInput();

    @Name("Поле ввода поиска от даты")
    @FindBy(".//input[@value.bind = 'getSearchFromDate']")
    AtlasWebElement dateStartInput();

    @Name("Поле ввода поиска до даты")
    @FindBy(".//input[@value.bind = 'searchrequest.to']")
    AtlasWebElement dateEndInput();

    @Name("Кнопка троеточие события")
    @FindBy(".//button[@click.delegate = 'onOpenEventChecker()']")
    AtlasWebElement tripleDotEventButton();

    @Name("Кнопка троеточие Фильтра по оргюниту")
    @FindBy(".//button[@click.delegate = 'selectOrgunits()']")
    AtlasWebElement tripleDotOrgNameButton();

    @Name("Название столбцов")
    @FindBy(".//div[@class = 'row mar-top-3']/div[contains(@class, '12')]")
    ElementsCollection<AtlasWebElement> allColumnTitle();

    @Name("Все строки в журнале")
    @FindBy(".//div[@style ='word-break:break-all']")
    ElementsCollection<AtlasWebElement> allLineInTable();

    @Name("Все чекбоксы выбора события")
    @FindBy(".//label/div/div[2]")
    ElementsCollection<AtlasWebElement> allCheckBoxes();

    @Name("Чекбокс события {eventName}")
    @FindBy(".//div[contains(text(), '{{ eventName }}')]")
    AtlasWebElement checkBoxByName(@Param("eventName") String eventName);

    @Name("Кнопка \"Закрыть\" в форме выбора события")
    @FindBy(".//button[@t = 'detectionjournal:close']")
    AtlasWebElement closeEventBoxes();

    @Name("Кнопка \"Выполнено, обновить\"")
    @FindBy(".//button[contains(text(), 'Выполнено, обновить')]")
    AtlasWebElement doneRepeatButton();

    @Name("Кнопка \"Очистить журнал\"")
    @FindBy(".//button[contains(text(), 'очистить журнал')]")
    AtlasWebElement clearJournalButton();

    @Name("Спиннер обновления журнала")
    @FindBy(".//div[contains(@class, 'is-loading')]")
    AtlasWebElement journalSpinner();

    @Name("Строка ввода поиска по оргмодулю")
    @FindBy(".//input[@placeholder ='Поиск по оргмодулю']")
    AtlasWebElement inputOrgModuleSearch();

    @Name("Кнопка \"Сохранить\" в окне фильтра по оргюниту")
    @FindBy(".//span[@click.delegate ='onSave()']")
    AtlasWebElement saveInOrgNameFilterButton();

    @Name("Чекбокс оргюнита выданного по поиску")
    @FindBy(".//span[@click.delegate ='onCheckOrgunit(row.uuid)']")
    AtlasWebElement findingOrgNameCheckBox();

    @Name("Все строки столбца \"Дата - время ваше\"")
    @FindBy(".//div[@style = 'word-break:break-all']//div[contains(@class, 'col')][1]")
    ElementsCollection<AtlasWebElement> allYoursData();

    @Name("Все строки столбца \"Дата - время на месте\"")
    @FindBy(".//div[@style = 'word-break:break-all']//div[contains(@class, 'col')][2]")
    ElementsCollection<AtlasWebElement> allPlaceData();

    @Name("Все строки столбца \"Терминал\"")
    @FindBy(".//div[@style = 'word-break:break-all']//div[contains(@class, 'col')][3]")
    ElementsCollection<AtlasWebElement> allTeminals();

    @Name("Кнопка \"Скачать как zip\"")
    @FindBy("//a[@click.delegate='getImagesFromReportInZIP()']")
    AtlasWebElement downloadZiButton();

    @Name("Все строки столбца \"Событие:\"")
    @FindBy(".//div[@style = 'word-break:break-all']//div[contains(@class, 'col')][5]")
    ElementsCollection<AtlasWebElement> allEvents();

}

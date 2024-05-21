package elements.reports;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;
import pages.TreeBehavior;

public interface ReportWorkField extends AtlasWebElement, TreeBehavior {

    @Name("Кнопка \"Численность по графикам\"")
    @FindBy("./div[contains(@show.bind, 'selectedReport')]/a[contains(@t, 'scheduleNumber')]")
    AtlasWebElement numberOfGraphs();

    @Name("Поле для поиска ОМ в отчетах \"Квоты выходных дней\", \"Анализ средней конверсии\""
            + "\"Плановая и фактическая конверсия\", \"Численность персонала\", \"Значения  используемых параметров\"")
    @FindBy(".//input[contains(@id, 'report-custom-search-text')]")
    AtlasWebElement inputOmSearchField();

    @Name("Поле для поиска ОМ в отчетах \"Квоты выходных дней\", \"Анализ средней конверсии\""
            + "\"Плановая и фактическая конверсия\", \"Численность персонала\", \"Значения  используемых параметров\"")
    @FindBy(".//input[@id = 'org-unit']")
    AtlasWebElement inputOmFiled();

    @Name("Поле для поиска ОМ внутри раскрываемого списка")
    @FindBy(".//input[@ref = 'searchInputElement']")
    AtlasWebElement inputIntoFiled();

    @Name("Кнопка магазина {orgName} в списке выбора")
    @FindBy(".//div[normalize-space(text()) = '{{ orgName }}']")
    AtlasWebElement certainOmButton(@Param("orgName") String orgName);

    @Name("Поле для поиска ОМ в отчетах, где присутствует дерево")
    @FindBy("//input[@id='report-custom-search-text']")
    AtlasWebElement largeOmInputSearchField();

    @Name("Кнопка в виде домика для выбора ОМ в отчетах аналогично ReportWorkField.inputOmSearchField")
    @FindBy(".//button[@id='org-units']")
    AtlasWebElement omButton();

    @Name("Кнопка \"Просмотреть отчет\" в отчетах аналогично ReportWorkField.inputOmSearchField")
    @FindBy("./div/div/a[not(contains(@class, 'hide'))]")
    AtlasWebElement checkReport();

    @Name("Поле для поиска ОМ в дополнительной форме в отчетах аналогично ReportWorkField.inputOmSearchField")
    @FindBy(".//input[@ref='searchInputElement']")
    AtlasWebElement searchInputFields();

    @Name("Список результатов поиска по ОМ в отчетах аналогично ReportWorkField.inputOmSearchField")
    @FindBy(".//div[@ref='element']")
    SearchOmList listOmFromSearch();

    @Name("Чекбокс магазина {orgName} в списке выбора")
    @FindBy("//a[contains(text(), '{{ orgName }}')]/../mdl-checkbox//span[contains(@class,'mdl-checkbox__ripple-container')]")
    AtlasWebElement certainOmCheckBox(@Param("orgName") String orgName);

    @Override
    @Name("Шеврон соответсвующего ОМ в дереве")
    @FindBy(".//label[contains(text(), '{{ name }}')]/../..//button/i")
    AtlasWebElement chevronButton(@Param("name") String name);

    @Name("Чекбокс соответсвующего ОМ в дереве")
    @FindBy("//span[contains(text(), '{{ name }}')]/../mdl-checkbox/label")
    AtlasWebElement checkBoxButton(@Param("name") String name);

    @Name("Чекбокс соответсвующего ОМ в дереве")
    @FindBy("//span[contains(text(), '{{ name }}')]/../mdl-checkbox/label")
    AtlasWebElement checkBoxButtonNew(@Param("name") String name);

    @Name("Название текущего отчета")
    @FindBy("//h5[@show.bind='selectedReport']")
    AtlasWebElement nameOfReport();

    @Name("Серый фон и иконка загрузки оргюнитов")
    @FindBy("//div[@class = 'load load--in au-target']")
    AtlasWebElement spinnerLoadingOm();

    @Name("Список отображаемых оргюнитов")
    @FindBy("//div[@class='mdl-list__item reports__org-unit-item']/span/label")
    ElementsCollection<AtlasWebElement> displayedOrgUnits();

    @Name("Серый фон и иконка загрузки оргюнитов")
    @FindBy("//button[@show.bind = 'searchString']")
    AtlasWebElement cleanButton();

    @Name("Кнопка поиска по тегам")
    @FindBy("//button[@click.trigger = 'showTagsFilterDialog()']")
    AtlasWebElement tagButton();
}

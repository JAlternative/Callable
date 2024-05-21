package elements.analytics;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;

import java.util.List;

public interface KpiForecastForm extends AtlasWebElement {

    @Name("Поле ввода типа KPI")
    @FindBy(".//input[@id='kpi-forecast-kpi-ids']")
    AtlasWebElement kpiTypeList();

    @Name("Тип KPI {type}")
    @FindBy(".//div[@click.delegate='toggleKpi(kpi.kpiId)']/span[text()='{{ type }}']/..")
    AtlasWebElement kpiType(@Param("type") String type);

    @Name("KPI по {type} (для импорта)")
    @FindBy(".//span[contains(@class.bind, 'kpiForecast.historyKpiId')]/following-sibling::span[text() ='{{ type }}']")
    AtlasWebElement kpiValueType(@Param("type") String type);

    @Name("Статус галочки KPI по {type}")
    @FindBy(".//span[text() ='{{ type }}']/../span[1]")
    AtlasWebElement kpiCheckValueType(@Param("type") String type);


    @Name("Раскрыть лист kpi (для импорта)")
    @FindBy(".//input[@id='kpi-forecast-history-kpi']")
    AtlasWebElement kpiValueButton();

    /**
     * 1st - date from
     * 2nd - date to
     */
    @Name("Период прогноза дата начала и дата конца 4 поля")
    @FindBy(".//div[contains(@t, 'dialogs.kpiForecast.subtitles.range' )]/../..//label[contains(text(), 'Дата')]/../input")
    List<AtlasWebElement> kpiForecastRangeList();

    @Name("Список из четырех кнопок календарей по порядку")
    @FindBy(".//h5[contains(text(), 'Расчёт прогноза')]/../../..//label[contains(text(), 'Дата')]/../button[not(contains(@class, 'aurelia-hide'))]")
    List<AtlasWebElement> calendarsList();

    @Name("Подразделение для импорта")
    @FindBy(".//input[@id='org-unit-for-import-kpi']")
    AtlasWebElement orgUnitImport();

    @Name("Подразделение для импорта")
    @FindBy(".//input[@t='[placeholder]components.searchableMenu.search']")
    AtlasWebElement orgUnitImportInput();

    @Name("Лист подразделений для импорта")
    @FindBy(".//div[@click.trigger = 'config.onClick(item, $target)']")
    ElementsCollection<AtlasWebElement> orgUnitImportList();

    /**
     * 1st - date from
     * 2nd - date to
     */

    @Name("Алгоритм прогноза")
    @FindBy(".//input[@id='kpi-forecast-algorithm']")
    AtlasWebElement kpiForecastAlgorithm();

    @Name("Алгоритм прогноза по дням или по месяцам")
    @FindBy(".//div[@menu='kpi-forecast-algorithm']/div")
    List<AtlasWebElement> kpiForecastAlgorithmList();

    @Name("Мин значение трафика")
    @FindBy(".//input[@id='min-kpi']")
    AtlasWebElement kpiMin();

    @Name("Макс значение трафика")
    @FindBy(".//input[@id='max-kpi']")
    AtlasWebElement kpiMax();

    @Name("Тренд прогноза трафика в процентах поле для ввода")
    @FindBy(".//input[@id='forecast-month-sum-coefficient']/ancestor::div[2]/following-sibling::div//input")
    AtlasWebElement kpiTrendField();

    @Name("Тренд прогноза трафика в процентах полоса")
    @FindBy(".//input[@id='forecast-month-sum-coefficient']")
    AtlasWebElement kpiTrendSlider();

    @Name("Создать расчет")
    @FindBy(".//div/button[contains(@t, 'common.actions.create')]")
    AtlasWebElement kpiForecastCreate();

    @Name("Кнопка закрытия формы крестик")
    @FindBy(".//h5[@t='routes.kpiForecast']/ancestor::div[2]/button")
    AtlasWebElement kpiForecastCloseFrom();

    @Name("Подразделение для импорта")
    @FindBy(".//input[@id='org-unit-for-import-kpi']")
    AtlasWebElement kpiImport();

    @Name("Поле для ввода тренда")
    @FindBy(".//input[@value.bind=\"monthSumCoefficientPercent.number\"]")
    AtlasWebElement trendForm();

    @Name("Поп-ап некорректная дата ввода")
    @FindBy("//div[contains(text(),'Не удалось выполнить расчет')]")
    AtlasWebElement errorNullData();

    @Name("Кнопка 'Создать'")
    @FindBy("//button[contains(text(), normalize-space('Создать')) or @t='common.actions.create']")
    AtlasWebElement createButton();

    @Name("Поле для ввода даты начала или конца для расчета прогноза")
    @FindBy("//div[@t='dialogs.kpiForecast.subtitles.range']/../following-sibling::div[{{ order }}]//label[contains(text(), '{{ startOrEnd }}')]/preceding-sibling::input[contains(@id,'date-input')]")
    AtlasWebElement dateInputFieldForKpiForecast(@Param("order") Integer order, @Param("startOrEnd") String startOrEnd);

}

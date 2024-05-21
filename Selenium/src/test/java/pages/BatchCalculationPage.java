package pages;

import elements.analytics.KpiForecastForm;
import elements.batchCalculation.*;
import elements.general.DatePickerForm;
import io.qameta.atlas.webdriver.WebPage;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface BatchCalculationPage extends WebPage {

    @Name("Форма всей страницы без нижней панели")
    @FindBy("//div[contains(@class, 'orgunits-container')]")
    MainPanel mainPanel();

    @Name("Кнопка вызова бокового меню")
    @FindBy("//div[contains(@class, 'mdl-layout__drawer-button')]")
    AtlasWebElement menuButton();

    @Name("Форма тегов в массовом расчете")
    @FindBy("//div[@class='mdl-dialog--not-fullscreen-api-render au-target']//div[@class='mdl-dialog mdl-dialog--fit-content']")
    TagsForm tagForm();

    @Name("Форма нижней панели")
    @FindBy("//div[contains(@class, 'batch-calculation__controls')]")
    BottomPanel bottomPanel();

    @Name("Форма расчета прогноза")
    @FindBy("//div[@class=\"mdl-layout__right mdl-shadow--8dp au-target\"]")
    CalculationTheForecast calculationTheForecast();

    @Name("Форма выбора месяца")
    @FindBy("//div[contains(@class, 'datetimepicker--open' )]")
    DatePickerForm datePickerForm();

    @Name("Форма элементов типа расчета")
    @FindBy("//ul[@for='batch-calculation-type-select']")
    OptionsForm optionsForm();

    @Name("Форма расчета прогноза KPI")
    @FindBy("//div[contains(@show.bind, 'forecast')]")
    KpiForecastForm kpiForecastForm();

    @Name("Форма расчета прогноза FTE")
    @FindBy("//div[@class='mdl-layout__right mdl-shadow--8dp au-target']")
    FteForecastForm fteForecastForm();

    @Name("Форма расчета прогноза смены")
    @FindBy("//div[@class='mdl-layout__right mdl-shadow--8dp au-target']")
    ShiftForecastForm shiftForecastForm();

    @Name("Форма расчета плановой численности")
    @FindBy("//div[@class='mdl-layout__right mdl-shadow--8dp au-target']")
    PlannedStrengthForecastForm plannedStrengthForecastForm();

    @Name("Форма опций и статусов типов расчетов ")
    @FindBy("//div[@class='mdl-list__item batch-tree-view__item au-target']")
    FormOptionsAndStatuses formOptionsAndStatuses();

    @Name("Форма информации об ошибке")
    @FindBy("//error-data-dialog/dialog[@class = 'mdl-dialog au-target']")
    ErrorForm errorForm();

    @Name("Форма скачивания результатов расчетов")
    @FindBy("//body")
    DownloadForm downloadForm();

    @Name("Форма выбора месяца")
    @FindBy("//month-picker-dialog/div[contains(@class, 'datetimepicker')][not(contains(@class, 'aurelia-hide'))]")
    DatePickerForm calendar();

    @Name("Все чекбоксы, отображенные на странице")
    @FindBy("//input[@type='checkbox' and@change.trigger=\"toggleSelection()\"]//..")
    ElementsCollection<AtlasWebElement> unitCheckboxes();
}
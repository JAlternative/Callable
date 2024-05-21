package pages;

import elements.analytics.*;
import elements.general.DatePickerForm;
import elements.general.NewDatePikerForm;
import io.qameta.atlas.webdriver.WebPage;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import elements.scheduleBoard.SpinnerLoader;

public interface AnalyticsPage extends WebPage {

    @Name("Боковая панель элементов слева")
    @FindBy("//div[contains(@class, 'diagram-chart__left')]")
    LeftBarEdit leftBarEdit();

    @Name("Форма списка магазинов")
    @FindBy("//div[@ref='element']")
    SearchOmList searchOmList();

    @Name("Результат расчета, который появляется снизу после расчета")
    @FindBy("//div[@ref='snack' and @aria-hidden='false']")
    DownPanelResult downPanelResult();

    @Name("Устаревший календарь на момент 2019 года")
    @FindBy("//nav[contains(@class, 'type-switcher')]")
    DataNavSwitcher dataNavSwitcher();

    @Name("Элементы в правом нижнем углу, поменять правый и левый график местами, переключить месяц")
    @FindBy("//div[contains(@class, 'diagram-chart__ctrl')]")
    DiagramScopeSwitcher diagramSwitcher();

    @Name("Место для диаграмм с расчетом прогноза на месяц (правый/основной)")
    @FindBy("//div[@class='diagram-chart__view']")
    DiagramChart diagramChart();

    @Name("Форма диаграммы левого графика")
    @FindBy("//div[@class='diagram-chart__view au-target']")
    DiagramChartLeft diagramChartLeft();

    @Name("троеточие")
    @FindBy("//button[@id='diagram-chart-menu']/following-sibling::div")
    EditForm editFrom();

    @Name("Таблица данных и коррекции")
    @FindBy("//div[@show.bind='showEdit']")
    CorrectionTable correctionTable();

    @Name("Форма редактирования значения столбца коррекции")
    @FindBy("//div[@class='diagram-chart__edit-card mdl-card mdl-card--overflow mdl-shadow--2dp']")
    CorrectionSlider correctionSlider();

    @Name("Расчет прогноза")
    @FindBy("//div[contains(@show.bind, 'showKpiForecastDialog') and  not(contains(@class, 'hide'))]")
    KpiForecastForm kpiForecastForm();

    @Name("Публикация прогноза")
    @FindBy("//div[@show.bind='showKpiPublishedDialog' and not(contains(@class, 'hide'))]")
    KpiPublishedForm kpiPublishedForm();

    @Name("Изменения в прогнозе")
    @FindBy("//div[@show.bind='showHistory']")
    KpiForecastChanges kpiForecastChanges();

    @Name("Расчет ресурсного запроса")
    @FindBy("//div[@show.bind='showFteForecastDialog' and not(contains(@class, 'hide'))]")
    FteForm fteForm();

    @Name("Публикация ресурсной потребности")
    @FindBy("//div[@show.bind='showFtePublishedDialog' and not(contains(@class, 'hide'))]")
    FtePublishedForm ftePublishedForm();

    @Name("Форма Изменения в фактических данных")
    @FindBy("//div[@show.bind = 'showHistory']")
    FteChanges fteChanges();

    @Name("форма датапикера")
    @FindBy("//div[contains(@class, 'datetimepicker--open' )]")
    DatePickerForm datePickerForm();

    @Name("Форма нового датапикера")
    @FindBy("//div[@class='datetimepicker datetimepicker--date au-target datetimepicker--open']")
    NewDatePikerForm newDatePickerForm();

    @Name("Элементы формы информации")
    @FindBy("//div[@class=\"menu menu--shadow-16dp au-target is-visible\"]")
    InformationForm informationForm();

    @Name("Форма правого графика")
    @FindBy("//*[@ref='svgElement']")
    RightGraphicDiagramForm rightGraphicDiagramForm();

    @Name("Форма левого графика")
    @FindBy("//*[@ref='svgElementSecond']")
    LeftGraphDiagramForm leftGraphicDiagramForm();

    @Name("Форма всей страницы Аналитика без верней части")
    @FindBy("//div[@class='mdl-layout__content']")
    AnalyticsPageForm analyticsPageForm();

    @Name("Форма диалогового окна, открывающегося при нажатии на значок дискеты, добавления комментария к изменениям")
    @FindBy("//h4[contains(text(),'Сохранение изменений')]")
    SaveCommentForCorrectionForm saveCommentForCorrectionForm();

    @Name("Спиннеры")
    @FindBy("//body")
    SpinnerLoader spinnerLoader();

}

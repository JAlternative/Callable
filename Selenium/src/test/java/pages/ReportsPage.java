package pages;

import elements.general.DatePickerForm;
import elements.orgstructure.TagsForm;
import elements.reports.*;
import elements.reports.PublicationGraphTable;
import io.qameta.atlas.webdriver.WebPage;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import org.openqa.selenium.WebElement;
import elements.scheduleBoard.SpinnerLoader;

public interface ReportsPage extends WebPage {

    @Name("Форма со списком отчётов")
    @FindBy("//div[@show.bind='!selectedReport']")
    ReportTypePanel reportTypePanel();

    @Name("Форма справа для выбора даты,формата файла или печати")
    @FindBy("//div[@class='mdl-grid']/div/div[@style]")
    RightPanelInReport rightPanelInReport();

    @Name("Форма для выбора месяца и года")
    @FindBy("//div[contains(@class, 'datetimepicker--open' )]")
    DatePickerForm datePickerForm();

    @Name("Форма с выбором ОМ из дерева и поиска ОМ")
    @FindBy("//div[@class='mdl-grid']/div[not(@show.bind) and @style]")
    ReportWorkField reportWorkField();

    @Name("Форма при для работы с отчетами на новой вкладке")
    @FindBy("//div[@class='mdl-grid']")
    OpenReportTab openReportTab();

    @Name("Хедер для всей страницы")
    @FindBy("//header")
    MainHeader mainHeader();

    @Name("разделе \"Расписание\" по выбранному подразделению. ")
    @FindBy("//span[@class='mdl-layout-title au-target'][contains(text(),'{{ orgUnitName }}')]")
    MainHeader chapter(@Param("orgUnitName") String orgUnitName);

    @Name("Таблица статуса публикации графиков")
    @FindBy("//table")
    PublicationGraphTable publicationGraphTable();

    @Name("Таблица расписания")
    @FindBy("//div[@class='gantt-chart']")
    ScheduleTable scheduleTable();

    @Name("Окно \"режим сравнения\"  с кнопкой \"Выйти\"")
    @FindBy("//div[@ref='snack']")
    ScheduleTable comparisonWindow();

    @Name("Форма с названиями отчетов на страницах")
    @FindBy("//h5[@show.bind='selectedReport']")
    OpenReportTab nameReportField();

    @Name("Элементы загрузки")
    @FindBy("//div")
    SpinnerLoader loadingSpinner();

    @Name("Страница вкладки Целевая численность")
    @FindBy("//h5/../../div[@show.bind='selectedReport']/..")
    TargetNumberPage targetNumberPage();

    @Name("Форма тегов")
    @FindBy("//div[not(contains(@class, 'hide'))]/div[@class='mdl-dialog mdl-dialog--fit-content']")
    TagsForm tagsForm();

    @Name("Кнопка загрузить отчет")
    @FindBy("//button[@class='mdl-button mdl-button--raised au-target']//span[text()='Скачать отчет']")
    WebElement downloadReportButtonClick();

}

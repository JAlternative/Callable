package elements.reports;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface RightPanelInReport extends AtlasWebElement {

    @Name("Поля для взаимодествия с датой при выборе месяца и года")
    @FindBy("./div[1]/div[1]//input")
    AtlasWebElement dataInput();

    @Name("Тип файла для скачивания {{ typeOfDownloadFiles }} в отчетах \"Смены\"/ \"Табель учетного времени\"")
    @FindBy(".//span[text()='Скачать отчет']/../span/a[text()='{{ typeOfDownloadFiles }} ']")
    AtlasWebElement typeOfDownloadFiles(@Param("typeOfDownloadFiles") String typeOfDownloadFiles);

    @Name("Кнопка \"Печатные формы\" в отчете \"Печатные формы\"")
    @FindBy("./div/div[contains(@show.bind, 'print') and not(contains(@class,'hide'))]/button")
    AtlasWebElement printedForms();

    @Name("Кнопка скачать отчет (CSV) в отчете \"Качество исторических данных\"")
    @FindBy(".//a[contains(@click.trigger,'downloadKpiStatistics')]")
    AtlasWebElement csvReport();

    @Name("Кнопка \"Скачать отчет (XLSX)\" в отчете \"Целевая численность\"")
    @FindBy(".//a[contains(@click.trigger, 'downloadStuffAndOperationsReport')]")
    AtlasWebElement xlsxReport();

    @Name("Поля ввода для месяца начала (0) и месяца окончания (1)")
    @FindBy("(//div[not(contains(@class, '-hide')) and contains(@class,'mdl-list__item mdl-list__item--flat')]" +
            "/div/div/div/input)[{{ typeOfMonthForm }}]")
    AtlasWebElement monthInputFieldTargetNumber(@Param("typeOfMonthForm") Integer typeOfMonthForm);

    @Name("Поля ввода для отчет за месяц")
    @FindBy("//div[not(contains(@class, '-hide')) and contains(@class,'mdl-list__item mdl-list__item--flat')]"
            + "/div/div/div/input[contains(@id, 'month-input')]")
    AtlasWebElement monthInputField();

    @Name("Кнопка \"Статус публикации графиков\"")
    @FindBy(".//button[contains(@click.trigger, 'download')]")
    AtlasWebElement publicationStatus();
}

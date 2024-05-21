package elements.analytics;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;


public interface CorrectionTable extends AtlasWebElement {

    @Name("Все ячейки строк {column} столбца ")
    @FindBy(".//tr//td[{{ column }}]")
    ElementsCollection<AtlasWebElement> getColumnValues(@Param("column") int column);

    @Name("Ячейки столбца коррекции прогноза")
    @FindBy(".//tr//td[last()]")
    ElementsCollection<AtlasWebElement> forecastCorrectionValues();

    @Name("Ячейки столбца коррекции прогноза")
    @FindBy("//tr//span[contains(text(), '{{ time }}')]/../../td[{{ column }}]")
    AtlasWebElement historyCorrectionsOnTimeUnit(@Param("column") int column, @Param("time") String time);

    @Name("Ячейки столбца коррекции прогноза")
    @FindBy("//tr//span[contains(text(), '{{ time }}')]/../../td[last()]")
    AtlasWebElement forecastCorrectionsOnTimeUnit(@Param("time") String time);

    @Name("Ячейки столбца значения прогноза")
    @FindBy("//tr//span[contains(text(), '{{ time }}')]/../../td[last() - 1]")
    AtlasWebElement forecastOnTimeUnit(@Param("time") String time);

    @Name("Столбец с ножницами у диагностики")
    @FindBy(".//button[@title = 'Вырезать ']")
    ElementsCollection<AtlasWebElement> diagnosticsScissorsColumn();

    @Name("Кнопка Крестик закрыть форму")
    @FindBy(".//div[@class='mdl-data-table__alt-header-ctrl']//i[@class='mdi mdi-close']")
    AtlasWebElement closeCorrectionTable();

    @Name("Форма для ожидания коррекции")
    @FindBy("./div/table")
    AtlasWebElement correctionForm();

    @Name("Кнопка дискеты, сохранения изменений коррекции")
    @FindBy(".//i[@class='mdi mdi-content-save']")
    AtlasWebElement saveCorrectionButton();

    @Name("Кнопка корзины, удаления изменений коррекции")
    @FindBy(".//i[@class='mdi mdi-delete']")
    AtlasWebElement deleteCorrectionButton();

}

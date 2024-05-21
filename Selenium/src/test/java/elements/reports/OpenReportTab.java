package elements.reports;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface OpenReportTab extends AtlasWebElement {

    @Name("Название ОМ над таблицей")
    @FindBy(".//h5")
    AtlasWebElement nameOfUnit();

    @Name("Вся таблица целиком")
    @FindBy(".//div[@class='mdl-data-table__last-row']")
    AtlasWebElement mainTable();

    @Name("Определенная ячейка из таблицы - Дата")
    @FindBy(".//div[@class='mdl-data-table__last-row']//tbody/tr[{{ row }}]/td/span")
    AtlasWebElement certainCellFromTable(@Param("row") int row);

    @Name("Все элементы из первой колонки таблицы")
    @FindBy(".//tbody/tr//td/span")
    ElementsCollection<AtlasWebElement> allCellsFromFirstColumn();

    @Name("Стрелочки в правом нижнем углу таблицы")
    @FindBy("//div[@class='mdl-grid']//div[contains(@class, 'ctrl')]/button[i[contains(@class, '{{ direction }}')]]")
    AtlasWebElement tableSwitcher(@Param("direction") String direction);


}


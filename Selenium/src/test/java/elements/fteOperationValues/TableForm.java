package elements.fteOperationValues;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface TableForm extends AtlasWebElement {

    @Name("Все даты в левой части таблицы")
    @FindBy(".//table[@class = 'table']/tbody/tr/td[@class = 'table__cell']")
    ElementsCollection<AtlasWebElement> allDates();

    @Name("Список ролей в таблице")
    @FindBy(".//table[@class = 'table__sub']//th[@class = 'table__cell']")
    ElementsCollection<AtlasWebElement> allRoles();

    @Name("Список KPI разделов в таблице")
    @FindBy("(//table[@class = 'table table--head']//tr)[1]/th[not(contains(@class, 'table__cell'))]")
    ElementsCollection<AtlasWebElement> allKpiAndEvents();

}

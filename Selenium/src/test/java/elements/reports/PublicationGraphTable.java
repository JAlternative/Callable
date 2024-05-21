package elements.reports;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface PublicationGraphTable extends AtlasWebElement {

    @Name("Строка таблицы c нужной строкой = { subdivision }")
    @FindBy("//td//a[text()= '{{ subdivision }}']/../..")
    TableRow tableRow(@Param("subdivision") String subdivision);

    @Name("Дата формирования плановой численности")
    @FindBy("//span[contains(@t, 'month')]/..")
    AtlasWebElement currentDateMonth();

    @Name("Колонка OrgUnits")
    @FindBy("//td[1]")
    ElementsCollection<AtlasWebElement> orgUnitsList();

}

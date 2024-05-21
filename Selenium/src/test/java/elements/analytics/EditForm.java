package elements.analytics;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface EditForm extends AtlasWebElement {

    @Name("Выбор элементов из троеточия через параметр")
    @FindBy(".//span[text() = '{{ chartMenuType }}']")
    AtlasWebElement chartMenuType(@Param("chartMenuType") String chartMenuType);

    @Name("Лист элементов из троеточия")
    @FindBy("//div[@menu='diagram-chart-menu']/div[@class=\"menu__item au-target\"]")
    ElementsCollection<EditForm> listOfChartMenu();

}

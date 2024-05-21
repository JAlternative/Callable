package elements.reports;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface SearchOmList extends AtlasWebElement {

    @Name("Список магазинов")
    @FindBy(".//div[@ref='listElement']/div[not(@t='common.loading')]")
    ElementsCollection<AtlasWebElement> certainOm();

    @Name("Магазин {orgName} в списке выбора")
    @FindBy(".//div[@ref='listElement']/div[not(@t='common.loading')][normalize-space(text()) = '{{ orgName }}']")
    AtlasWebElement certainOm(@Param("orgName") String orgName);

}

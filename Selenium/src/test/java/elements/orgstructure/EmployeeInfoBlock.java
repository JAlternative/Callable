package elements.orgstructure;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface EmployeeInfoBlock extends AtlasWebElement {
    @Name("Название пункта списка")
    @FindBy(".//i/following-sibling::span[1]")
    AtlasWebElement title();

    @Name("Период действия")
    @FindBy(".//i/following-sibling::span[2]/span[2]")
    AtlasWebElement duration();
}

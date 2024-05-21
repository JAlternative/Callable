package elements.orgstructure;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface SwitchToTabs extends AtlasWebElement {

    @Name("Кнопка для переключения на вкладку ОМ")
    @FindBy(".//i[@class.bind='nav.icon'][contains(@class, 'mdi-store')]")
    AtlasWebElement omTab();

    @Name("Кнопка для переключения на вкладку сотрудники")
    @FindBy(".//i[@class.bind='nav.icon'][contains(@class, 'mdi-account-multiple')]")
    AtlasWebElement empTab();

    @Name("Кнопка для переключения на вкладку ролей")
    @FindBy(".//i[@class.bind='nav.icon'][contains(@class, 'mdi-account-key')]")
    AtlasWebElement roleTab();
}

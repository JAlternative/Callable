package elements.orgstructure;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface EmpActionForm extends AtlasWebElement {
    @Name("Кнопка добавить сотрудника")
    @FindBy(".//i[contains(@class, 'account-plus')]")
    AtlasWebElement addEmployeeButton();

    @Name("Кнопка создать роль или просмотр прав")
    @FindBy(".//i[contains(@class, 'account-key')]")
    AtlasWebElement addRoleButton();
}

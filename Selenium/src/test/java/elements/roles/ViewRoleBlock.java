package elements.roles;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface ViewRoleBlock extends AtlasWebElement {
    @Name("Название роли в списке ролей")
    @FindBy(".//div[@class='mdl-list__item-primary-content']/i[contains(@class, 'mdi-account-key')]/../span[1]")
    AtlasWebElement roleValue();

    @Name("Дата начала роли")
    @FindBy(".//div[@class='mdl-list__item-primary-content']/i[contains(@class, 'mdi-account-key')]/../span/span[1]")
    AtlasWebElement roleStartValue();

    @Name("Даты окончания ролей")
    @FindBy(".//div[@class='mdl-list__item-primary-content']/i[contains(@class, 'mdi-account-key')]/../span/span[3]")
    AtlasWebElement roleEndValue();

    @Name("Названия оргюнитов роли")
    @FindBy(".//span[text()='Подразделение']/../../span[1]")
    ElementsCollection<AtlasWebElement> orgUnitValues();

    @Name("Кнопка \"Больше\"")
    @FindBy(".//button[@click.trigger='loadOrgUnits(role)']")
    AtlasWebElement moreButton();

}

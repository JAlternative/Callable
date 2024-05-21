package elements.orgstructure;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface SingleRoleViewBlock extends AtlasWebElement {
    @Name("Название роли")
    @FindBy(".//i[@class = 'mdi mdi-account-key mdl-list__item-icon']/../span[1]")
    AtlasWebElement roleTitle();

    @Name("Дата начала роли")
    @FindBy(".//i[contains(@class, 'key')]/following-sibling::span[@class = 'mdl-list__item-sub-title']/span[1]")
    AtlasWebElement dateStartValue();

    @Name("Дата окончания роли")
    @FindBy(".//i[contains(@class, 'key')]/following-sibling::span[@class = 'mdl-list__item-sub-title']/span[3]")
    AtlasWebElement dateEndValue();

    @Name("Список оргюнитов роли")
    @FindBy(".//div[@class ='mdl-list__item mdl-list__item--two-line']/..//span[text() = 'Подразделение']/../../span[1]")
    ElementsCollection<AtlasWebElement> valuesOrgUnits();

    @Name("Кнопка \"Больше\"")
    @FindBy(".//button[@click.trigger='loadOrgUnits(role)']")
    AtlasWebElement seeMoreButton();
}

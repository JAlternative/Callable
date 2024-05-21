package elements.roles;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface ViewRoleForm extends AtlasWebElement {

    @Name("Кнопка редактирования роли (Карандаш)")
    @FindBy(".//i[@class = 'mdi mdi-pencil']")
    AtlasWebElement pencilButton();

    @Name("Блок информации об {order}-й роли")
    @FindBy("(//div[@class = 'org-structure__card mdl-shadow--4dp au-target']//div[@class='mdl-list__item-primary-content']/i[contains(@class, 'mdi-account-key')]/../../..)[{{ order }}]")
    ViewRoleBlock viewRoleBlock(@Param("order") int order);

    @Name("Блоки информации о ролях")
    @FindBy(".//div[@class='mdl-list__item-primary-content']/i[contains(@class, 'mdi-account-key')]/../../..")
    ElementsCollection <ViewRoleBlock> roleInfoBlocks();

    @Name("Названия ролей в списке ролей")
    @FindBy(".//div[@class='mdl-list__item-primary-content']/i[contains(@class, 'mdi-account-key')]/../span[1]")
    ElementsCollection<AtlasWebElement> roleValues();

    @Name("Даты начала ролей")
    @FindBy(".//div[@class='mdl-list__item-primary-content']/i[contains(@class, 'mdi-account-key')]/../span/span[1]")
    ElementsCollection<AtlasWebElement> roleStartValues();

    @Name("Даты окончания ролей")
    @FindBy(".//div[@class='mdl-list__item-primary-content']/i[contains(@class, 'mdi-account-key')]/../span/span[3]")
    ElementsCollection<AtlasWebElement> roleEndValues();

}

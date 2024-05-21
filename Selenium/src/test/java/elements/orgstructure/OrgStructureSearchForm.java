package elements.orgstructure;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface OrgStructureSearchForm extends AtlasWebElement {

    @Name("Выбранный ом из списка результатов поиска")
    @FindBy(".//div[2]/div[2]/div[contains(@class,'selected')]/div")
    AtlasWebElement osPickSelectedFromList();

    @Name("Третий результат поиска")
    @FindBy("./div[2]/div[2]/div[3]")
    AtlasWebElement osPickFromList();

    @Name("Строка поиска работника")
    @FindBy("//input[@ref='searchInput']")
    AtlasWebElement employeeSearchInput();

    @Name("Строка поиска оргЮнита")
    @FindBy("//input[@ref='searchInput']")
    AtlasWebElement orgUnitSearchInput();

    @Name("Все результаты поиска")
    @FindBy(".//div[contains(@class, 'org-structure__item mdl-list_')]")
    ElementsCollection<AtlasWebElement> allSearchResult();

    @Name("Первый результат поиска")
    @FindBy("//div[@class='org-structure__items-container au-target']//div[contains(@class, 'item--flat')][1]")
    AtlasWebElement firstSearchResult();

    @Name("Конкретный результат поиска")
    @FindBy(".//div[contains(@class, 'org-structure__item mdl-list_')]//div[normalize-space()='{{ name }}']/..")
    AtlasWebElement searchResult(@Param("name") String name);

    @Name("Поле с надписью текущего поиска")
    @FindBy("//div[@class='mdl-list__item-primary-content mdl-color-text--primary']/span[1]")
    AtlasWebElement structureField();

}

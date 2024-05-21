package elements.roles;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface RolesSearchForm extends AtlasWebElement {

    @Name("Выбранный ом из списка результатов поиска")
    @FindBy(".//div[2]/div[2]/div[contains(@class,'selected')]/div")
    AtlasWebElement mixPickSelectedFromList();

    @Name("Третий результат поиска ")
    @FindBy("./div[2]/div[2]/div[3]")
    AtlasWebElement mixPickFromList();

    @Name("Строка поиска работника")
    @FindBy("//input[@id='search1']")
    AtlasWebElement employeeSearchInput();

    @Name("Строка поиска оргЮнита")
    @FindBy("//input[@id='search']")
    AtlasWebElement orgUnitSearchInput();

    @Name("Все результаты поиска")
    @FindBy("./div[2]/div[2]/div[not(contains(@class,'aurelia-hide'))]/div")
    ElementsCollection<AtlasWebElement> allSearchResult();

    @Name("Спинер загрузки")
    @FindBy("//div[@class='load load--in load--white au-target aurelia-hide']")
    AtlasWebElement spinerLoader();

    @Name("Спинер загрузки")
    @FindBy("//div[@class='load load--in load--white au-target' and (contains(@show.bind, '!items'))]")
    AtlasWebElement loadPanel();

    @Name("Поле с надписью текущего поиска")
    @FindBy("//span[@class='mdl-list__item-primary-content mdl-color-text--primary']")
    AtlasWebElement structureField();

    @Name("Кнопка реверса списка")
    @FindBy("./div[1]/div[1]/button[1]")
    AtlasWebElement reverseButton();
}

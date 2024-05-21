package elements.roles;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;
import org.openqa.selenium.WebElement;

public interface OrgUnitSearchForm extends AtlasWebElement {
    @Name("Заголовок окна")
    @FindBy("./*/h4[contains(text(), 'Подразделения')]")
    AtlasWebElement header();

    @Name("Поле ввода поиска оргюнита")
    @FindBy(".//div[contains(@class, 'mdl-list__text-field') and not(contains(@class, 'aurelia-hide'))]//input[(@class ='mdl-textfield__input au-target') and @id='search-9']")
    AtlasWebElement inputOrgUnitSearch();

    @Name("Галочка \"Почта России\"")
    @FindBy(".//div[@class='mdl-list__item-primary-content au-target' and contains(text(), 'Почта России')]")
    AtlasWebElement russianPostTick();

    @Name("Список оргюнитов в окне поиска")
    @FindBy("./div[not(contains(@class, 'mdl-dialog__head mdl-dialog__head--secondary'))]//div[@class='mdl-list mdl-list--no-margin']//div[not(contains(@class, 'aurelia-hide'))]/div[@class='mdl-list__item-primary-content']")
    ElementsCollection<AtlasWebElement> orgUnitsList();

    @Name("Кнопка \"Сбросить\"")
    @FindBy(".//button[@click.trigger = 'resetFilter(item)']")
    AtlasWebElement resetFilterButton();

    @Name("Кнопка \"Изменить\"")
    @FindBy(".//button[@click.trigger = 'applyFilter(item)']")
    AtlasWebElement applyFilterButton();

    @Name("Чекбокс выбора оргюнита")
    @FindBy(".//div[not(contains(@class, 'mdl-dialog__head mdl-dialog__head--secondary'))]//div[@class='mdl-list mdl-list--no-margin']//div[not(contains(@class, 'aurelia-hide'))]/div[@class='mdl-list__item-primary-content']/../div[@class='mdl-list__item-secondary-action']/label")
    WebElement orgUnitCheckbox();
}

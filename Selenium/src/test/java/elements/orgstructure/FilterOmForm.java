package elements.orgstructure;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.AtlasWebElement;
import pages.TreeBehavior;

public interface FilterOmForm extends AtlasWebElement, TreeBehavior {
    @Name("Кнопка \"Применить\"")
    @FindBy(".//div[@class='mdl-dialog__actions']/button[@click.trigger='apply()']")
    AtlasWebElement omOkButton();

    @Name("Кнопка \"Отменить\"")
    @FindBy(".//div[@class='mdl-dialog__actions']/button[@click.trigger='visible = false']")
    AtlasWebElement omResetButton();

    @Name("Поисковая строка")
    @FindBy(".//div[@class='mdl-list__item']/div/input")
    AtlasWebElement omSearchBar();

    @Name("Кнопка \"Очистить\"")
    @FindBy(".//i[@class='mdi mdi-broom']")
    AtlasWebElement omClear();

    @Override
    @Name("Чекбокс соответствующего ОМ в дереве")
    @FindBy(".//span[@title='{{ name }}']//mdl-checkbox[@checked.bind='isSelected']")
    AtlasWebElement checkBoxButton(@Param("name") String name);

    @Override
    @Name("Шеврон соответствующего ОМ в дереве")
    @FindBy(".//span[@title='{{ name }}']/ancestor::div[contains(@class,'mdl-tree__item')]/span[contains(@class,'mdl-list__item-secondary-action')]/button/i[contains(@class,'mdi-chevron-')]")
    AtlasWebElement chevronButton(@Param("name") String name);

    @Name("шеврон первого оргюнита")
    @FindBy(".//mdl-tree-item/div/div[contains(@class,'mdl-list__item mdl-tree__item')]" +
            "/span[contains(@class,'mdl-list__item-secondary-action')]/button/i[contains(@class,'mdi-chevron-')]")
    AtlasWebElement topChevron();

}

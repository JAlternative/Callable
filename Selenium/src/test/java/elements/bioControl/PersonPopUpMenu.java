package elements.bioControl;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.AtlasWebElement;
import pages.TreeBehavior;

public interface PersonPopUpMenu extends AtlasWebElement, TreeBehavior {

    @Name("Поисковая строка")
    @FindBy("//div[@class='popup-dialog vertical-bank']//input[@placeholder ='Поиск по оргмодулю']")
    AtlasWebElement searchOrgUnitInput();

    @Name("Панель по названию оргЮнита")
    @FindBy(".//div[contains(text(), '{{ value }}')]")
    AtlasWebElement orgUnitPanelByName(@Param("value") String value);

    @Name("Кнопка действия {action} по имени сотрудника {name}")
    @FindBy("//div[contains(text(), '{{ name }}')]/../../..//a[contains(text(), '{{ action }}')]")
    AtlasWebElement actionWithUserButton(@Param("name") String name, @Param("action") String action);

    @Name("Кнопка сохранить в настройках прикрепленных к терминалу людей")
    @FindBy("//div[@class='popup popup-fixed']//div[@class='shrink']//span[1]")
    AtlasWebElement savePersonAttachedButton();

    @Override
    @Name("Чек бокс Юнита")
    @FindBy(".//div[@click.delegate='onSelectRow(row)' and (contains(text(), '{{ name }}'))]")
    AtlasWebElement checkBoxButton(@Param("name") String name);

    @Override
    @Name("Шеврон выбранного Юнита")
    @FindBy(".//div[contains(text(), '{{ name }}')]/../../../..//a[contains(@class, 'chevron')]")
    AtlasWebElement chevronButton(@Param("name") String name);
}

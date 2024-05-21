package elements.fteOperationValues;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface FilterPanel extends AtlasWebElement {

    @Name("Кнопка раскрытия списка оргюнитов")
    @FindBy(".//div[contains(@class.bind , 'orgUnit.name')]//i")
    AtlasWebElement openOrgUnitListButton();

    @Name("Строка ввода названия оргюнита")
    @FindBy(".//input[@value.bind = 'searchString']")
    AtlasWebElement searchOrgUnitInput();

    @Name("Кнопка выбора календаря {type}")
    @FindBy(".//label[text() = '{{ type }}']/..//button")
    AtlasWebElement filterCalendarButton(@Param("type") String type);

    @Name("Кнопка фильтрации по типу")
    @FindBy("(//button[@click.trigger = 'openFilter(item)'])[{{ filterOrder }}]")
    AtlasWebElement filterChevronButton(@Param("filterOrder") int filterOrder);

    @Name("Кнопка сброса фильтров")
    @FindBy(".//span[@click.trigger = 'reset()']")
    AtlasWebElement resetButton();

    @Name("Список кнопок всех оргюнитов в поиске")
    @FindBy(".//div[@click.trigger = 'config.onClick(item, $target)']")
    ElementsCollection<AtlasWebElement> allOrgNames();

    @Name("Кнопка выбора оргюнита: {orgName}")
    @FindBy(".//div[normalize-space(text()) = '{{ orgName }}']")
    AtlasWebElement orgNameButton(@Param("orgName") String orgName);

    @Name("Текст ошибки под Датой окончания")
    @FindBy(".//label[text() = 'Дата окончания']/..//span[@class = 'mdl-textfield__error']")
    AtlasWebElement endDateError();


}

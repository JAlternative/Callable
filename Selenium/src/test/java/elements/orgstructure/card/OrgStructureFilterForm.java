package elements.orgstructure.card;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface OrgStructureFilterForm extends AtlasWebElement {

    @Name("тип ОМ")
    @FindBy(".//div[3]//button")
    AtlasWebElement omType();

    @FindBy(".//div[5]//button")
    AtlasWebElement omButton();

    @Name("Кнопка \"Теги\"")
    @FindBy(".//div[7]//button")
    AtlasWebElement omTags();

    @Name("Кнопка перехода в фильтр сотрудников по должности")
    @FindBy("//div[text()='Должность']/../div/button")
    AtlasWebElement employeePositionButton();

    @Name("Точка активности фильтра позиций")
    @FindBy(".//div[4]//span[@class='org-structure__round au-target']")
    AtlasWebElement employeePositionFilterIsActive();

    @Name("Кнопка перехода в фильтр сотрудников по подразделению")
    @FindBy(".//div[6]//button")
    AtlasWebElement employeeOMButton();

    @Name("Кнопка \"Сброс\"")
    @FindBy("./div[1]/span[2]")
    AtlasWebElement resetButton();

    @Name("Переключатель по названию на панели фильтров")
    @FindBy(".//label/ancestor::div[2]/div[contains(text(), '{{ nameOfFlag }}')]/following-sibling::div")
    AtlasWebElement filterWithFlag(@Param("nameOfFlag") String nameOfFlag);

    @Name("активные фильтры выбранной вкладки")
    @FindBy("//label[contains(@class, 'switch') and (contains(@class, 'is-checked'))]")
    ElementsCollection<AtlasWebElement> activeFilters();

    @Name("Кнопка фильтра \"Подразделения\"")
    @FindBy("//div[contains(text(),'Подразделения')]/following-sibling::div/button[@click.trigger='openFilter(filter)']")
    AtlasWebElement orgUnitFilter();

}

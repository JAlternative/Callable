package elements.batchCalculation;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface BottomPanel extends AtlasWebElement {

    @Name("Кнопка для вызова панели Типов расчета")
    @FindBy("//button[@id='batch-calculation-type-select']")
    AtlasWebElement buttonBatchCalculationType();

    @Name("Список со всеми видами расчета")
    @FindBy("//li[@t.bind='calculationType.name']")
    ElementsCollection<AtlasWebElement> calculationTypesList();

    @Name("Лист видов расчета")
    @FindBy(".//li[@class='mdl-menu__item au-target mdl-js-ripple-effect'][{{ type }}]")
    AtlasWebElement listBatchCalculationType(@Param("type") int type);

    @Name("Кнопка выбора типа рассчета: {type}")
    @FindBy(".//li[text() = '{{ type }}']")
    AtlasWebElement batchCalculationType(@Param("type") String type);

    @Name("Кнопка \"Фильтр по тегам\"")
    @FindBy("./button[2]")
    AtlasWebElement buttonFilter();

    @Name("Кнопка \"Рассчитать\"")
    @FindBy("./button[@class='mdl-button mdl-button--raised au-target mdl-js-button'][contains(text(),'Рассчитать')]")
    AtlasWebElement buttonCalculate();

    @Name("Неактивная кнопка Расчитать, только при наличии неактивированных чекбоксов")
    @FindBy(".//button[@disabled]")
    AtlasWebElement inactiveCalculateButton();

    @Name("Поле с текущей датой")
    @FindBy(".//div[@style]/input")
    AtlasWebElement dateInput();

    @Name("Поле \"{{ fieldType }}\"")
    @FindBy(".//label[text()='{{ fieldType }}']/../input[contains(@id, 'month-input')]")
    AtlasWebElement dateInput(@Param("fieldType")String fieldType);

    @Name("Кнопка календаря у поля \"{{ fieldType }}\"")
    @FindBy(".//label[text()='{{ fieldType }}']/../input[contains(@id, 'month-input')]/../button")
    AtlasWebElement calendarButton(@Param("fieldType")String fieldType);

    @Name("Кнопка выбора месяца")
    @FindBy("./div[2]/button")
    AtlasWebElement buttonOpenCalendar();

}

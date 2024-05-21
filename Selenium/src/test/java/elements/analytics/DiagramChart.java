package elements.analytics;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface DiagramChart extends AtlasWebElement {

    @Name("Текущая дата на графике")
    @FindBy("./div[@class='diagram-chart__title']/div/span")
    AtlasWebElement currentChartDate();

    @Name("Список месяцев или номеров дней под графиком")
    @FindBy(".//div[@class='diagram-chart__link']")
    ElementsCollection<AtlasWebElement> indexGraphYearOrMonth();

    @Name("Список рабочих часов под графиком")
    @FindBy(".//div[@class='diagram-chart__domain au-target']/div/div")
    ElementsCollection<AtlasWebElement> indexGraphDay();

    @Name("Подписи столбцов правого графика (под графиком)")
    @FindBy(".//div[@class='diagram-chart__domain au-target']/div[{{ value }}]/div")
    AtlasWebElement dateForDiagramForm(@Param("value") int value);

    @Name("Название графика под датой")
    @FindBy(".//div[@class='diagram-chart__title']/div[2]")
    AtlasWebElement variantsOfInformation();

    @Name("Список баров на текущий день/час/месяц")
    @FindBy("//*[@ref='svgElement']/*/*[@id='diagram__bar--{{ value }}']/*")
    ElementsCollection<AtlasWebElement> barsFromCertainPosition(@Param("value") int value);

    @Name("Все столбцы цвета {color}")
    @FindBy("//*[@ref='svgElement']/*/*[@id]/*[contains(@class,'{{ color }}') and @height>=1]")
    ElementsCollection<AtlasWebElement> colorColumnsNum(@Param("color") String color);

}

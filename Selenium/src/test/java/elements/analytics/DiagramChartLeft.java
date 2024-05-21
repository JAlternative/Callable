package elements.analytics;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface DiagramChartLeft extends AtlasWebElement {
    @Name("Номер года, или название месяца, или номер дня, " +
            "отображается над графиком при открытии окна редактирования при клике на колонку графика, " +
            "изначально отображается год")
    @FindBy(".//span[contains(text(),'')]")
    AtlasWebElement currentStateValue();

    @Name("Лист элементов левого графика (под графиком)")
    @FindBy("./div[3]/div")
    ElementsCollection<AtlasWebElement> listDateForDiagramForm();

    @Name("Элементы левого графика (под графиком)")
    @FindBy("(./div[3]/div)[{{ value }}]")
    AtlasWebElement dateForDiagramForm(@Param("value") int value);

    @Name("Список месяцев или номеров дней под графиком")
    @FindBy(".//div[@class='diagram-chart__link']")
    ElementsCollection<AtlasWebElement> indexGraphYearOrMonth();

    @Name("Список рабочих часов под графиком")
    @FindBy(".//div[@class='diagram-chart__domain au-target']/div/div")
    ElementsCollection<AtlasWebElement> indexGraphDay();

    @Name("Список всех баров на текущем масштабе")
    @FindBy("//*[@ref='svgElementSecond']/*/*[@id]")
    ElementsCollection<AtlasWebElement> totalGraphBars();

    @Name("Список баров на текущий день/час/месяц")
    @FindBy("//*[@ref='svgElementSecond']/*/*[@id='diagram__bar--{{ value }}']/*")
    ElementsCollection<AtlasWebElement> barsFromCertainPosition(@Param("value") int value);

    @Name("Все столбцы цвета {color}")
    @FindBy("//*[@ref='svgElementSecond']/*/*[@id]/*[contains(@class,'{{ color }}') and @height>=1]")
    ElementsCollection<AtlasWebElement> colorColumnsNum(@Param("color") String color);

}

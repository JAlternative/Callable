package elements.analytics;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;

import java.util.List;

public interface RightGraphicDiagramForm extends AtlasWebElement {

    @Name("Лист элементов правого графика")
    @FindBy(".//*[name()='rect'][@height>=1]")
    ElementsCollection<AtlasWebElement> listColumnRightGraphicDiagram();

    @Name("Все первые столбцы на графике")
    @FindBy(".//*[contains(@id, 'diagram__bar--')]/*[1]")
    List<AtlasWebElement> allFirstColumn();

    @Name("Столбцы прогноза")
    @FindBy("//*[contains(@id, 'diagram__bar--')]/*[contains(@class, 'purple') and not(@height='0')]")
    ElementsCollection<AtlasWebElement> kpiForecast();

    @Name("Столбцы исторических данных")
    @FindBy("//*[contains(@id, 'diagram__bar--')]/*[contains(@class, 'orange') and not(@height='0') or (contains(@class, 'teal')) and not(@height='0')]")
    ElementsCollection<AtlasWebElement> kpiHistory();

    @Name("Линия ресурсной потребности")
    @FindBy("//*[contains(@class, 'diagram__line diagram__grey--')]")
    AtlasWebElement fteLine();

}

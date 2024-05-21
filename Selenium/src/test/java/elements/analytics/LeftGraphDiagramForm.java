package elements.analytics;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface LeftGraphDiagramForm extends AtlasWebElement {

    @Name("Все столбцы левого графика")
    @FindBy(".//*[name()='rect'][@height>=\"1\"]")
    ElementsCollection<AtlasWebElement> listColumnLeftGraphicDiagram();

}

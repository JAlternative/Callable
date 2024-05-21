package elements.batchCalculation;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface OptionsForm extends AtlasWebElement {
    @Name("Элемент типа расчета")
    @FindBy("./li[ {{ type }} ]")
    AtlasWebElement chooseOption(@Param("type") int type);

}

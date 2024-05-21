package elements.analytics;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface InformationForm extends AtlasWebElement {

    @Name("Лист из элементов формы информации")
    @FindBy(".//span[text()='{{ var }}']/..")
    AtlasWebElement variantsInInfo(@Param("var") String var);

    @Name("Лист галочек из элементов формы информации")
    @FindBy(".//span[text()='{{ var }}']/../span[1]")
    AtlasWebElement checkMarkVariants(@Param("var") String var);

}
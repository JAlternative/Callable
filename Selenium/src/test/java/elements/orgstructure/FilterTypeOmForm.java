package elements.orgstructure;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface FilterTypeOmForm extends AtlasWebElement {

    @Name("Выбор позиции {{type}} из списка")
    @FindBy(".//div/div[contains(text(), '{{ type }}')]/following-sibling::div//span[@class='mdl-checkbox__ripple-container mdl-js-ripple-effect mdl-ripple--center']")
    AtlasWebElement selectedOmType(@Param("type") String omType);

    @Name("кнопка сабмит в форме типа ОМ")
    @FindBy(".//button[1]")
    AtlasWebElement typeOmOkButton();

    @Name("кнопка сабмит в форме типа ОМ")
    @FindBy(".//button[2]")
    AtlasWebElement typeOmResetButton();

}

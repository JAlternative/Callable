package elements.personalSchedule;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface InformationForm extends AtlasWebElement {

    @Name("Элемент события {name}")
    @FindBy(".//span[@t.bind = 'name'][contains(text(),'{{ name }}')]")
    AtlasWebElement eventInformation(@Param("name") String name);

    @Name("Кнопка закрытия формы")
    @FindBy(".//div[@class='mdl-typography--text-right width--25']//button[@mdl='button']")
    AtlasWebElement closeButton();
}

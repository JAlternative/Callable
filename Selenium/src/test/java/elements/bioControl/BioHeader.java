package elements.bioControl;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface BioHeader extends AtlasWebElement {

    @Name("Кнопка раздела {section}")
    @FindBy(".//a[text() = '{{ section }}']")
    AtlasWebElement sectionButton(@Param("section") String section);

    @Name("Кнопка Выход")
    @FindBy("//div[@title='Выход']")
    AtlasWebElement exitButton();

}

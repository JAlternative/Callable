package elements.bioControl;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface Home extends AtlasWebElement {

    @Name("Кнопка раздела {section}")
    @FindBy(".//h2[contains(text(), '{{ section }}')]/../../..")
    AtlasWebElement sectionButton(@Param("section") String section);

    @Name("Кнопка раздела section")
    @FindBy("//div[@class='col au-target']")
    ElementsCollection<AtlasWebElement> cardsList();

    @Name("Все кнопки на меню слева")
    @FindBy("//div[@click.delegate]")
    ElementsCollection<AtlasWebElement> buttonsList();

    @Name("Все разделы")
    @FindBy("//div[@class='panel-body']//h2")
    ElementsCollection<AtlasWebElement> sectionsList();

}

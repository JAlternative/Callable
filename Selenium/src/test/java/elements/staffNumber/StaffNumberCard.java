package elements.staffNumber;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface StaffNumberCard extends AtlasWebElement{

    @Name("Ссылка на доступный орг юнит")
    @FindBy("//div[@class='mdl-list__item-primary-content au-target link']")
    ElementsCollection<AtlasWebElement> unitLink();
}

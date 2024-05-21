package elements.scheduleBoard;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface ListOfEmployeePosition extends AtlasWebElement {

    @Name("Лист c вариантами должностей сотрудников")
    @FindBy("./div")
    ElementsCollection<ListOfEmployeePosition> variantEmployeePosition();

}

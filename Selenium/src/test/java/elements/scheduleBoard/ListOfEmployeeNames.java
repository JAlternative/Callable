package elements.scheduleBoard;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface ListOfEmployeeNames extends AtlasWebElement {

    @Name("Список всех сотрудников")
    @FindBy("./div")
    ElementsCollection<ListOfEmployeeNames> elementEmployeeNames();
}

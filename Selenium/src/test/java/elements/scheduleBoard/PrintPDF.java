package elements.scheduleBoard;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface PrintPDF extends AtlasWebElement {

    @Name("Должности отображенные при печате")
    @FindBy("//label[contains(@class.bind, 'positionType') and (contains(@class, 'is-checked'))]/../..//div[@class='mdl-list__item-primary-content']")
    ElementsCollection<AtlasWebElement> beforePrintJobsList();

    @Name("Список всех активированных чекбоксов")
    @FindBy("//label[contains(@class.bind, 'positionType') and (contains(@class, 'is-checked'))]//span[3]")
    ElementsCollection<AtlasWebElement> positionsTypesCheckBoxesList();

}

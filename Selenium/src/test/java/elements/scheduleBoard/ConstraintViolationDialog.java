package elements.scheduleBoard;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface ConstraintViolationDialog extends AtlasWebElement {

    @Name("Список нарушений, открывающийся после нажатия на шеврон сообщения о конфликте")
    @FindBy(".//span[text()='{{ message }}']/../../ul/li")
    ElementsCollection<AtlasWebElement> constrList(@Param("message") String message);

    @Name("Список конфликтов в сообщении об ошибке при утверждении/публикации графика")
    @FindBy(".//dialog[@class='mdl-dialog mdl-dialog--fit-content au-target']//div/span")
    ElementsCollection<AtlasWebElement> messages();
}

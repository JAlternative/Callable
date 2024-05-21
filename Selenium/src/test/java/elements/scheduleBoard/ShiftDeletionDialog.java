package elements.scheduleBoard;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface ShiftDeletionDialog extends AtlasWebElement {

    @Name("Поле для ввода комментария")
    @FindBy("//textarea[@id='schedule-request-deletion-dialog-comment-text']")
    AtlasWebElement commentText();

    @Name("Кнопка \"Удалить\"")
    @FindBy(".//button[text()='Удалить']")
    AtlasWebElement deleteButton();

}

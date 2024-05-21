package elements.scheduleBoard;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface CorrectionConfirmationDialog extends AtlasWebElement  {
    @Name("Кнопка подтверждения")
    @FindBy(".//div[@class='mdl-dialog__actions']/button[@click.delegate='confirm()']")
    AtlasWebElement confirmationButton();

    @Name("Кнопка отмены")
    @FindBy(".//div[@class='mdl-dialog__actions']/button[@click.delegate='close()']")
    AtlasWebElement cancelButton();
}

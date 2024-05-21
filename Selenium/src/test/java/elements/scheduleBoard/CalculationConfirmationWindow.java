package elements.scheduleBoard;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface CalculationConfirmationWindow extends AtlasWebElement {
    @Name("Кнопка \"Продолжить\"")
    @FindBy(".//button[@click.trigger='onContinue()']")
    AtlasWebElement continueButton();

    @Name("Кнопка \"Отменить\"")
    @FindBy(".//button[@click.trigger='onCancel()']")
    AtlasWebElement abortButton();

    @Name("Подсказка перед началом расчета")
    @FindBy("//iframe[@type='application/pdf']")
    AtlasWebElement calculationHint();
}

package elements.scheduleBoard;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface RejectPublishingRosterDialog extends AtlasWebElement {

    @Name("Кнопка \"Отменить\"")
    @FindBy(".//button[@t ='common.actions.cancel']")
    AtlasWebElement cancelButton();

    @Name("Кнопка \"Сохранить\"")
    @FindBy(".//button[@t ='common.actions.save']")
    AtlasWebElement saveButton();

    @Name("Поле ввода комментария причины отклонения графика")
    @FindBy(".//textarea")
    AtlasWebElement commentaryInput();

    @Name("Ошибка в поле ввода")
    @FindBy(".//span[@class ='mdl-textfield__error']")
    AtlasWebElement errorInput();

}

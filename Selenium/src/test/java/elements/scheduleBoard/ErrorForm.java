package elements.scheduleBoard;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface ErrorForm extends AtlasWebElement {

    @Name("Кнопка \"Закрыть\" в окне ошибки")
    @FindBy(".//button")
    AtlasWebElement buttonCloseBarError();

    @Name("Окно c ошибкой после расчета")
    @FindBy(".//span[contains(@t, 'fail')]")
    AtlasWebElement layoutErrorForm();

    @Name("Сообщение ошибки")
    @FindBy("//div[contains(@t.bind, 'Result')]")
    AtlasWebElement errorTextField();
}

package elements.bioTerminal;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface CheckInPage extends AtlasWebElement {
    @Name("Поле с названием текущего режима авторизации")
    @FindBy("//i[contains(@class, 'message')]/../h1")
    AtlasWebElement checkInTypeField();

    @Name("Панель со иконкой камеры")
    @FindBy("//compose[contains(@class, 'camera')]")
    AtlasWebElement cameraContainer();

    @Name("Кнопка \"Отменить\" ")
    @FindBy("//span[contains(@t, 'cancel')]/..")
    AtlasWebElement cancelButton();
}

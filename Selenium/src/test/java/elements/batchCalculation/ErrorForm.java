package elements.batchCalculation;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface ErrorForm extends AtlasWebElement {

    @Name("Кнопка просмотра данных в новом окне")
    @FindBy(".//a[@href.bind = 'errorDataUrl']")
    AtlasWebElement newWindowErrorButton();

    @Name("Кнопка закрытия формы")
    @FindBy(".//button[@click.delegate = 'close()']")
    AtlasWebElement closeErrorFormButton();

    @Name("Текст ошибки в новом окне браузера")
    @FindBy("//pre")
    AtlasWebElement newWindowErrorText();
}

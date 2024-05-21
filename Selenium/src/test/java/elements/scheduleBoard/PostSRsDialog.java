package elements.scheduleBoard;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface PostSRsDialog extends AtlasWebElement {

    @Name("Кнопка \"Закрыть\"")
    @FindBy(".//button")
    AtlasWebElement srsDialogCloseButton();

    @Name("Результат расчета")
    @FindBy("./div[contains(@class, 'mdl-dialog__content')]/div")
    AtlasWebElement resultMessage();

    @Name("Описание ошибки расчета")
    @FindBy(".//div[@innerhtml.bind='message'][contains(text(), '{{ message }}')]")
    AtlasWebElement calcErrDescrMessage(@Param("message") String message);

    @Name("Кнопка в окне с ошибкой расчета при невозможности расставить пересменки")
    @FindBy(".//button[text()='{{ action }}']")
    AtlasWebElement calcResumeOrCancelButton(@Param("action") String action);

}

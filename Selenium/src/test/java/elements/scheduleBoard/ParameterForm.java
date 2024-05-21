package elements.scheduleBoard;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface ParameterForm extends AtlasWebElement {

    @Name("Поле ввода данных параметра \"{paramName}\"")
    @FindBy(".//div[text()='{{ paramName }}']/..//input")
    AtlasWebElement paramNameInput(@Param("paramName") String paramName);

    @Name("Кнопка \"Сохранить\" в форме параметров")
    @FindBy("//div[@class='mdl-dialog mdl-dialog--fit-content']//button[text()='Сохранить ']")
    AtlasWebElement saveParamButton();

}

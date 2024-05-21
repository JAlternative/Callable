package elements.orgstructure;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface FilterEmpPositionForm extends AtlasWebElement {

    @Name("Выбор позиции {{position}} из списка")
    @FindBy(".//div/div[text()='{{ position }}']/following-sibling::div//span[@class='mdl-checkbox__ripple-container mdl-js-ripple-effect mdl-ripple--center']")
    AtlasWebElement selectedEmployeePosition(@Param("position") String position);

    @Name("Кнопка \"Выбрать\"")
    @FindBy("//h4[text()='Должность']/../../div/button[text()='выбрать']")
    AtlasWebElement employeePositionOk();

    @Name("Кнопка \"Сбросить\"")
    @FindBy(".//div[3]//button[2]")
    AtlasWebElement employeePositionReset();
}

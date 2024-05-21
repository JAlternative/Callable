package elements.batchCalculation;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface CalculationTheForecast extends AtlasWebElement {

    @Name("крестик на панели расчета прогноза")
    @FindBy(".//div[@class='mdl-list__item'][1]/button[1]")
    AtlasWebElement closeCalculationTheForecast();

}

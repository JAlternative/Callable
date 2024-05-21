package elements.batchCalculation;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface DownloadForm extends AtlasWebElement {

    @Name("Текст с расчетом")
    @FindBy(".//pre[contains(text(),'{\"_embedded\":{\"calculations\":')]")
    AtlasWebElement downloadForecastForm();
}

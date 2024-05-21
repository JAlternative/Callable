package elements.batchCalculation;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.AtlasWebElement;

import java.util.List;

public interface FormOptionsAndStatuses extends AtlasWebElement {

    @Name("Элемент: Статус расчета Плановой численности")
    @FindBy(".//div[contains(@class,'batch-tree-view__status')]//div[contains(text(),'Расчет завершен ')]")
    List<AtlasWebElement> calculationStatusPlannedStrengthForecast();
}

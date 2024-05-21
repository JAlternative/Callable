package elements.analytics;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.AtlasWebElement;

import java.util.List;

public interface KpiForecastChanges extends AtlasWebElement {

    @Name("Кнопка Закрыть")
    @FindBy(".//i")
    AtlasWebElement kpiForecastChangesCloseForm();

    @Name("все элементы в форме которые можно проверить , выбраны ли они")
    @FindBy(".//tbody/tr/td[3]")
    List<AtlasWebElement> checkingElements();

}

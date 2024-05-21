package elements.reports;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface ScheduleTable extends AtlasWebElement {

    @Name("Индикатор режима сравнения")
    @FindBy("//div[text()='Режим сравнения']")
    AtlasWebElement comparisonModeIndicator();



}

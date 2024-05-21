package elements.analytics;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface DownPanelResult extends AtlasWebElement {

    @Name("Всплывающий поп-ап появляющийся после выполнения рассчета")
    @FindBy(".//div[@class='mdl-snackbar__text'][contains(text(),'{{ notificationName }}')]")
    AtlasWebElement textResult(@Param("notificationName") String notificationName);

    @Name("Всплывающий поп-ап появляющийся после выполнения рассчета")
    @FindBy(".//div[contains(@class, 'snackbar') and contains(text(), 'Расчёт выполнен')]")
    AtlasWebElement snackBar();

}

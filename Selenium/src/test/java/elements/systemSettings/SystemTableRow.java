package elements.systemSettings;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.AtlasWebElement;
import org.openqa.selenium.WebElement;

public interface SystemTableRow extends AtlasWebElement {
    @Name("Найти true/false")
    @FindBy(".//td[@class='mdl-data-table__cell--non-numeric'][contains(text(),'{{ content }}')]")
    WebElement trueOrFalse(@Param("content") String content);


}

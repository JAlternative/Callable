package elements.reports;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.AtlasWebElement;
import org.openqa.selenium.WebElement;

public interface TableRow extends AtlasWebElement {

    @Name("Ссылка сравнить для нужной строки")
    @FindBy(".//a[@t='common.actions.compare'][@class='au-target'][contains(text(),'Сравнить')]")
    AtlasWebElement subdivisionCompare();

    @Name("Ссылка Просмотреть для нужной строки")
    @FindBy(".//a[@t='common.actions.view'][@class='au-target'][contains(text(),'Просмотреть')]")
    AtlasWebElement subdivisionLook();

    @Name("Подразделение в таблице  {{ subdivision }}")
    @FindBy(".//a[@href.bind='getScheduleLink(orgUnit.id)'][text()='{{ subdivision }}']")
    AtlasWebElement subdivisionInTable(@Param("subdivision") String subdivision);
}

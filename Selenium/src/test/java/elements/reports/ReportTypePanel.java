package elements.reports;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.AtlasWebElement;

import java.util.List;

public interface ReportTypePanel extends AtlasWebElement {

    @Name("Выбор тип отчета из списка {{ typeOfReports }}")
    @FindBy("./div[{{ typeOfReports }}]//span")
    AtlasWebElement typeOfReports(@Param("typeOfReports") Integer typeOfReports);

    @Name("Кнопка выбора отчёта из списка {typeOfReports}")
    @FindBy("//span[contains(@class, 'item')]//a[text()='{{ name }}']")
    AtlasWebElement reportButtonByName(@Param("name") String name);

    @Name("Кнопка выбора отчёта из списка {typeOfReports}")
    @FindBy("//span[contains(@class, 'item')]//a")
    List<AtlasWebElement> reportsButtons();

}

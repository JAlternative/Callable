package elements.personalSchedule;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface Header extends AtlasWebElement {

    @Name("Кнопка переключения масштаба на \"{scopeName}\"")
    @FindBy(".//a[text() ='{{ scopeName }}']")
    AtlasWebElement scopeButton(@Param("scopeName") String scopeName);

    @Name("Заголовок хедера")
    @FindBy(".//span[@class = 'mdl-layout-title au-target']")
    AtlasWebElement headerTitle();

}

package elements.systemSettings;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface SystemSettingTable extends AtlasWebElement {

    @Name("Найти пункт настройки { itemTitle } ")
    @FindBy("//td[@class='mdl-data-table__cell--non-numeric au-target'][contains(text(),'{{ itemTitle }}')]")
    AtlasWebElement itemSystemSettingByTitle(@Param("itemTitle") String itemTitle);

    @Name("Найти значение { itemValue }")
    @FindBy("//td[@class='mdl-data-table__cell--non-numeric'][contains(text(), '{{ itemValue }}')]")
    AtlasWebElement itemValue(@Param("itemValue") String itemValue);
}

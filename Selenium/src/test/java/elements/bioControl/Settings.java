package elements.bioControl;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface Settings extends AtlasWebElement {

    @Name("Кнопка \"Выбрать файл\"")
    @FindBy(".//input[@id ='licfilechoose']")
    AtlasWebElement findButton();

    @Name("Текущее состояние лицензии")
    @FindBy("//td[@t='licence:state']/../td[2]")
    AtlasWebElement licenseStatus();

    @Name("Дата начала(2) и окончания(3)")
    @FindBy("//tr[{{ number }}]/td[2]")
    AtlasWebElement dateField(@Param("number") int number);
}

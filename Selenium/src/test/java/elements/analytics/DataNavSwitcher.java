package elements.analytics;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface DataNavSwitcher extends AtlasWebElement {

    @Name("Кнопка выбора Года")
    @FindBy("./a[@data-index='2']")
    AtlasWebElement yearScope();

    @Name("Кнопка выбора  Месяца")
    @FindBy("./a[@data-index='1']")
    AtlasWebElement monthScope();

    @Name("Кнопка выбора Дня")
    @FindBy("./a[@data-index='0']")
    AtlasWebElement dayScope();

}

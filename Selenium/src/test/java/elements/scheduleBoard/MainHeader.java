package elements.scheduleBoard;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface MainHeader extends AtlasWebElement {

    @Name("Кнопка выбора года")
    @FindBy("//a[@data-index='2']")
    AtlasWebElement yearScope();

    @Name("Кнопка выбора  месяца")
    @FindBy("//a[@data-index='1']")
    AtlasWebElement monthScope();

    @Name("Кнопка выбора дня")
    @FindBy("//a[@data-index='0']")
    AtlasWebElement dayScope();

    @Name("Кнопка выбора масштаба с индексом")
    @FindBy(".//a[@data-index='{{ scopeIndex }}']")
    AtlasWebElement dayScope(@Param("scopeIndex") String scopeIndex);

}

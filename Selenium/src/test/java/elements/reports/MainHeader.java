package elements.reports;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;

public interface MainHeader extends AtlasWebElement {

    @Name("Кнопка выбора дня")
    @FindBy("//a[@data-index='0']")
    AtlasWebElement dayScope();

    @Name("Кнопка выбора недели")
    @FindBy("//a[@data-index='1']")
    AtlasWebElement weekScope();

    @Name("Кнопка выбора  месяца")
    @FindBy("//a[@data-index='2']")
    AtlasWebElement monthScope();

    @Name("Кнопка выбора масштаба с индексом")
    @FindBy("//a[@data-index='{{ scopeIndex }}']")
    AtlasWebElement dayScope(@Param("scopeIndex") int scopeIndex);

    @Name("Активный масштаб")
    @FindBy("//a[@class = 'au-target mdl-navigation__link active']")
    AtlasWebElement actualScope();

    @Name("Текст в хедере")
    @FindBy("//header/div/span")
    AtlasWebElement headerText();

    @Name("Значок конверта")
    @FindBy("//unread-messages/span")
    AtlasWebElement envelope();

    @Name("Период на магните - день, неделя или месяц")
    @FindBy("//span[@id='navigation-select']")
    AtlasWebElement magnitPeriod();
}

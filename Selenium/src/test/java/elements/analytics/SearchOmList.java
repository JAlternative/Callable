package elements.analytics;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface SearchOmList extends AtlasWebElement {

    @Name("Поле ввода для магазина")
    @FindBy(".//input[@ref='searchInputElement']")
    AtlasWebElement inputSearch();

    @Name("Лист магазинов")
    @FindBy("//a[contains(text(),'{{ om }}')]")
    AtlasWebElement certainOm(@Param("om") String om);

    @Name("Лист магазинов")
    @FindBy("//a[contains(@class, 'menu__item au-target')]")
    ElementsCollection<AtlasWebElement> certainOm();

    @Name("Элемент загрузки листа")
    @FindBy(".//div[contains(@class, '-hide') and contains(@t, 'common.loading')]")
    AtlasWebElement loadingLine();
}

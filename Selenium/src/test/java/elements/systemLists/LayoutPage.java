package elements.systemLists;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface LayoutPage extends AtlasWebElement {

    @Name("Кнопка добавления \"Плюс\"")
    @FindBy("//i[@class='mdi mdi-plus']/../span")
    AtlasWebElement plusButton();

    @Name("Поле ввода")
    @FindBy("//input[@id='system-list']")
    AtlasWebElement inputSystemLists();

    @Name("Кнопка \"Комментарии к сменам\"")
    @FindBy("//div[@class='menu au-target is-visible']/div[2]")
    AtlasWebElement shiftCommentButton();

    @Name("Элемент выпадающего списка")
    @FindBy("//div[contains(text(), '{{ value }}')]")
    AtlasWebElement elementList(@Param("value") String value);

    @Name("Все элементы выпадающего списка")
    @FindBy("//div[@class='menu__item au-target']")
    ElementsCollection<AtlasWebElement> allElementsInList();
}

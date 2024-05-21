package elements.common;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface Header extends AtlasWebElement {

    @Name("Кнопка выбора меню выбора разделов (три параллельных линии)")
    @FindBy("//i[@class='material-icons']")
    AtlasWebElement sectionSelectionMenu();

    @Name("Содержит текст наименования страницы")
    @FindBy("//span[@class='mdl-layout-title au-target']")
    AtlasWebElement pageName();

    @Name("Тело выпадающего списка кнопки выбора меню выбора разделов (три параллельных линии)")
    @FindBy("//nav[@class='au-target mdl-navigation']")
    AtlasWebElement sectionNavigationMenu();

    @Name("Пункт меню (раздел)")
    @FindBy("//a[text()='{{ sectionName }}']")
    AtlasWebElement section(@Param("sectionName") String sectionName);

    @Name("Кнопка выхода")
    @FindBy("//a[@click.delegate='logout()']")
    AtlasWebElement logoutButton();

    @Name("Список компонентов меню выбора разделов")
    @FindBy("//nav[@class='au-target mdl-navigation']/a")
    ElementsCollection<AtlasWebElement> sectionSelectionMenuListItem();

    @Name("Кнопка, подверждения выхода из системы")
    @FindBy("//button[@click.delegate='submit()']")
    AtlasWebElement confirmLogoutButton();

    @Name("Текст заголовка страницы")
    @FindBy("//header//span[contains(@class, 'mdl-layout-title')]")
    AtlasWebElement headerTitle();
}

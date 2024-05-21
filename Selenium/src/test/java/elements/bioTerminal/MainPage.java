package elements.bioTerminal;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface MainPage extends AtlasWebElement {
    @Name("Поле с e-mail поддержки")
    @FindBy("//a[contains(@class, 'support')]")
    AtlasWebElement supportEmailField();

    @Name("Поле с текущей версией")
    @FindBy("//div[@class='version']")
    AtlasWebElement versionField();

    @Name("Кнопка перехлда в настройки")
    @FindBy("//i[contains(@class, 'settings')]/..")
    AtlasWebElement goToSettingsButton();

    @Name("Поле с текущим временем")
    @FindBy("//i[contains(@class, 'calendar')]/../h1")
    AtlasWebElement currentTimeField();

    @Name("Поле с текущей датой")
    @FindBy("//div[@class='app-date']/div[1]")
    AtlasWebElement currentDateField();

    @Name("Кнопка выбранного варианта отметки")
    @FindBy("//button[contains(@click.delegate, '{{ type }}')]")
    AtlasWebElement checkInButtonByType(@Param("type") String type);

    @Name("Список из кнопок авторизации")
    @FindBy("//button[@class='button is-info is-large button-action au-target']")
    ElementsCollection<AtlasWebElement> authButtonsList();

}

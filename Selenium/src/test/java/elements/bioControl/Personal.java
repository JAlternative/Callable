package elements.bioControl;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.AtlasWebElement;

import java.util.List;

public interface Personal extends AtlasWebElement {

    @Name("Поле ввода на вкладке таблица")
    @FindBy(".//input[@placeholder = 'Поиск по оргмодулю']")
    AtlasWebElement inputSearch();

    @Name("Кнопка \"Свободные терминалы\"")
    @FindBy(".//div[@click.delegate = 'onSelectFreePersons()']")
    AtlasWebElement freeTerminalButton();

    @Name("Ом выпадющие после поиска через поиск")
    @FindBy("//div[@class ='grow scrolled']//compose//div[@click.delegate=\"onSelectRow(row)\"]")
    List<AtlasWebElement> fieldsOM();
}

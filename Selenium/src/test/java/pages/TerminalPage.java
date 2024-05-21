package pages;

import elements.bioTerminal.CheckInPage;
import elements.bioTerminal.MainPage;
import elements.bioTerminal.SettingsPage;
import io.qameta.atlas.webdriver.WebPage;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;

public interface TerminalPage extends WebPage {

    @Name("Главная страница")
    @FindBy("//h1[contains(text(), 'Выберите действие')]/../../../..")
    MainPage mainPage();

    @Name("Раздел настроек")
    @FindBy("//span[contains(text(), 'Панель управления')]/../../..")
    SettingsPage settingsPage();

    @Name("Раздел авторизации")
    @FindBy("//compose[contains(@class, 'camera')]/../../../../..")
    CheckInPage checkInPage();

}

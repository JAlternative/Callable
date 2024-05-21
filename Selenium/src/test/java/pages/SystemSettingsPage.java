package pages;

import elements.systemSettings.DialogWindow;
import elements.systemSettings.SystemSettingTable;
import io.qameta.atlas.webdriver.WebPage;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;

public interface SystemSettingsPage extends WebPage {

    @Name("Таблица системных настроек")
    @FindBy("//div[@class='mdl-grid']")
    SystemSettingTable systemSettingTable();

    @Name("Окно ввода системного параметра")
    @FindBy("//div[@class='mdl-layout__right mdl-layout__right--500 mdl-shadow--16dp au-target']")
    DialogWindow dialogWindow();
}

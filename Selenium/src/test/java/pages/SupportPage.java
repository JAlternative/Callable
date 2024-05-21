package pages;

import elements.support.PopupPanel;
import elements.support.SupportPanel;
import io.qameta.atlas.webdriver.WebPage;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;

public interface SupportPage extends WebPage {

    @Name("Панель создания сообщения в службу поддержки")
    @FindBy("//div[contains(@class, 'mdl-card mdl-shadow')]")
    SupportPanel supportPanel();

    @Name("Поп-ап")
    @FindBy("//div[@class = 'mdl-snackbar au-target mdl-snackbar--active']")
    PopupPanel popupPanel();
}

package pages;

import elements.messages.MessageListPanel;
import io.qameta.atlas.webdriver.WebPage;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;

public interface MessagesPage extends WebPage {
    //Левая панель

    //Правая панель
    @Name("Панель со списком сообщений")
    @FindBy(".//table[@class='mdl-data-table au-target mdl-js-data-table']")
    MessageListPanel messageListPanel();

}

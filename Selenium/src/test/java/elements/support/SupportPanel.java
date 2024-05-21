package elements.support;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface SupportPanel extends AtlasWebElement {

    @Name("Поле ввода заголовка проблемы")
    @FindBy(".//input[@id = 'support-email-title']")
    AtlasWebElement inputProblemTitle();

    @Name("Поле ввода описания проблемы")
    @FindBy(".//textarea[@id = 'support-email-body']")
    AtlasWebElement inputProblemDescription();

    @Name("Кнопка \"Отправить\"")
    @FindBy(".//button[@click.trigger = 'sendEmail()']")
    AtlasWebElement sendButton();

}

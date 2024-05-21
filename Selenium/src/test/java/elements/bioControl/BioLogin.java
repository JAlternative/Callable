package elements.bioControl;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface BioLogin extends AtlasWebElement {

    @Name("Заголовок \"Авторизация\"")
    @FindBy(".//input[@value.bind='user.login']")
    AtlasWebElement header();

    @Name("Поле \"Логин\"")
    @FindBy(".//input[@value.bind='user.login']")
    AtlasWebElement loginField();

    @Name("Поле \"Пароль\"")
    @FindBy(".//input[@value.bind='user.password']")
    AtlasWebElement passwordField();

    @Name("Кнопка \"Войти\"")
    @FindBy("//button[text()='Войти'][not(@disabled)]")
    AtlasWebElement loginButton();

    @Name("Спиннер загрузки")
    @FindBy("//div[@class ='columns is-fullheight element is-loading']")
    AtlasWebElement loginSpinner();
}

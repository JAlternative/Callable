package elements.login;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface LoginForm extends AtlasWebElement {

    @Name("Поле \"Логин\"")
    @FindBy(".//input[@id='username']")
    AtlasWebElement loginField();

    @Name("Поле \"Пароль\"")
    @FindBy(".//input[@id='password']")
    AtlasWebElement passwordField();

    @Name("Кнопка \"Войти\"")
    @FindBy(".//button[@type = 'submit']")
    AtlasWebElement loginButton();

    @Name("Сообщение об ошибке авторизации")
    @FindBy(".//div[@id='error-message']")
    AtlasWebElement errorMessage();

    @Name("Кнопка восстановления пароля")
    @FindBy(".//a[contains(text(),'Восстановить пароль')]")
    AtlasWebElement recoveryPassword();

    @Name("Новый пароль")
    @FindBy("//input[@value.bind = 'user.password']")
    AtlasWebElement newPassword();

    @Name("Поле \"Пароль\"")
    @FindBy("//input[@value.bind = 'user.confirmPassword']")
    AtlasWebElement confirmPassword();

    @Name("Кнопка \"Изменить пароль\"")
    @FindBy("//button[@click.trigger = 'save()']")
    AtlasWebElement changePasswordButton();
}

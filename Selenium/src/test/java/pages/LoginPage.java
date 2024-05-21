package pages;

import elements.bioControl.Users;
import elements.common.Header;
import elements.login.LoginForm;
import io.qameta.atlas.webdriver.WebPage;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;

public interface LoginPage extends WebPage {


    @Name("Страница логина")
    @FindBy("//div[@class ='mdl-layout__content--center']")
    LoginForm loginPage();

    @Name("Раздел \"Пользователи\"")
    @FindBy("//div[@class='au-target']")
    Users users();

    @Name("Хэдер страниц")
    @FindBy("//div[contains(@class,'mdl-layout__header-row')]//span[contains(@class, 'mdl-layout-title')]")
    Header header();

}

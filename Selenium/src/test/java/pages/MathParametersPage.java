package pages;

//import com.sun.webkit.WebPage;

import elements.mathParameters.LeftBar;
import elements.mathParameters.MathParametersBar;
import elements.mathParameters.ResultBar;
import elements.mathParameters.RightBar;
import elements.scheduleBoard.SpinnerLoader;
import io.qameta.atlas.webdriver.WebPage;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;

public interface MathParametersPage extends WebPage {

    @Name("Левая панель страницы с кнопкой выбора страницы")
    @FindBy("//div[contains(@class, 'mdl-cell mdl-cell--3-col')]")
    LeftBar leftBar();

    @Name("Центральная часть страницы с математическими параметрами")
    @FindBy("//table[@mdl='data-table']//tbody")
    MathParametersBar mathParametersBar();

    @Name("ПопАП (ERROR), появляется при попытке сохранить изменения математического параметра")
    @FindBy("//div[@ref='snack']")
    ResultBar resultBar();

    @Name("Правая часть страницы, появляется после нажатия на математический параметр")
    @FindBy("//div[contains(@class, 'mdl-layout__content mdl-color--white')]")
    RightBar rightBar();

    @Name("Спиннеры во всем расписании")
    @FindBy("//body")
    SpinnerLoader spinnerLoader();
}

package elements.scheduleBoard;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface SpinnerLoader extends AtlasWebElement {

    @Name("Загрузка формы сотрудника в оргструктуре")
    @FindBy(".//div[@class='mdl-spinner is-active au-target mdl-js-spinner is-upgraded']")
    ElementsCollection<AtlasWebElement> loadingSpinnerInFormEmployee();

    @Name("Загрузка формы")
    @FindBy(".//div[@class='load load--in au-target']")
    AtlasWebElement loadingSpinnerInForm();

    //Используется на большинстве страниц
    @Name("Загрузка всей страницы")
    @FindBy(".//div[@class='load au-target']")
    AtlasWebElement grayLoadingBackground();

    //используется в расписании
    @Name("Загрузка всей страницы")
    @FindBy(".//div[@class='load load--under-dialog au-target']")
    AtlasWebElement loadingSpinnerPage();

    @Name("Элемент загрузки списков в оргструктуре")
    @FindBy(".//div[@class = 'load load--in load--transparent']")
    AtlasWebElement loadingForm();

    //добавил в этот раздел потому что создавать раздел ради одного поп-апа пока не вижу смысла
    @Name("Всплывающий поп-ап")
    @FindBy("//div[contains(@class, 'mdl-snackbar--active') and (contains(@aria-hidden, 'false'))]/div")
    AtlasWebElement popUp();
}

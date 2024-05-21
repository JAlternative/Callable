package pages;

import elements.bioControl.*;
import io.qameta.atlas.webdriver.WebPage;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;

public interface BioPage extends WebPage {

    @Name("Страница логина")
    @FindBy("//div[@id='login-form']")
    BioLogin bioLogin();

    @Name("Хедер")
    @FindBy("//div[@title='nav.workpanel']")
    BioHeader bioHeader();

    @Name("Раздел \"Главная панель\"")
    @FindBy("//div[@class='navigationpanel vertical-bank']")
    Home home();

    @Name("Раздел \"Терминалы\"")
    @FindBy("//div/*[text()='Терминалы']")
    Terminals terminals();

    @Name("Форма редактирования персонала")
    @FindBy("//div[@class='popup-dialog vertical-bank']")
    PersonPopUpMenu personPopUp();

    @Name("Раздел \"Персонал\"")
    @FindBy("//div[@class='au-target']")
    Personal personal();

    @Name("Раздел \"Пользователи\"")
    @FindBy("//div[@class='au-target']")
    Users users();

    @Name("Раздел \"Журнал событий\"")
    @FindBy("//div[@class='au-target']")
    Journal journal();

    @Name("Раздел \"Лицензирование\"")
    @FindBy("//div[@class='au-target']")
    Settings settings();

    @Name("Раздел редактирования информации о терминале")
    @FindBy("//div[contains(@class, 'terminalcardpanel')]")
    TerminalCardPanel terminalCardPanel();

    @Name("В форме сотрудники выпадающая форма при нажатии на ОМ")
    @FindBy("//div[@class='col col-9-12 shadow-3']")
    Employee employeeForm();

    @Name("В форме сотрудники выпадающая форма c фотографиями при нажатии на сотрудника")
    @FindBy("//div[@class=\"pad-left-5 pad-right-5 vertical-bank personcardpanel h-100v\"]")
    PictureForm pictureForm();
}

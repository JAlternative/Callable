package pages;

import elements.positionTypes.DialogForm;
import elements.positionTypes.Table;
import io.qameta.atlas.webdriver.WebPage;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import elements.scheduleBoard.SpinnerLoader;

public interface PositionTypesPage extends WebPage {

    @Name("Таблица со списком типов позиций")
    @FindBy("//table")
    Table table();

    @Name("Окно информации и редактирования типа позиции")
    @FindBy("//div[@class = 'mdl-layout']")
    DialogForm dialogForm();

    @Name("Спиннер")
    @FindBy("//body")
    SpinnerLoader spinner();

}

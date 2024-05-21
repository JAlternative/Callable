package pages;

import elements.systemLists.DataTable;
import elements.systemLists.LayoutPage;
import elements.systemLists.RightAddingPanel;
import io.qameta.atlas.webdriver.WebPage;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;

public interface SystemListsPage extends WebPage {

    @Name("Основная часть страницы")
    @FindBy("//div[@class='mdl-layout__container']")
    LayoutPage layout();

    @Name("Панель, открывающаяся справа")
    @FindBy("//div[contains(@class, 'layout__right') and not (contains(@class, 'aurelia-hide'))]")
    RightAddingPanel addingPanel();

    @Name("Таблица с данными по середине страницы")
    @FindBy("//table")
    DataTable shiftCommentTable();
}

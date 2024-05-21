package pages;

import elements.common.Header;
import io.qameta.atlas.webdriver.WebPage;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Page;

public interface MainPage extends WebPage {

    @FindBy("//header")
    @Name("Хэдер")
    Header header();

}

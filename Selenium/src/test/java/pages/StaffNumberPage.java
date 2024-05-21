package pages;

import elements.staffNumber.StaffNumberCard;
import io.qameta.atlas.webdriver.WebPage;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;

public interface StaffNumberPage extends WebPage {

    @Name("Карточка с доступными орг юнитами")
    @FindBy(".//div[contains(@class, 'mdl-layout-card')]")
    StaffNumberCard staffCard();

}

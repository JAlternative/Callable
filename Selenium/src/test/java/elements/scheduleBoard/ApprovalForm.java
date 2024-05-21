package elements.scheduleBoard;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface ApprovalForm extends AtlasWebElement {

    @Name("Кнопка \"Утвердить\"")
    @FindBy("//button[@t='common.actions.approve']")
    AtlasWebElement approveButton();

    @Name("Кнопка \"Закрыть форму\"")
    @FindBy("//div[@show.bind='showRosterPublishedDialog' and not(contains(@class, 'hide'))]//i[contains(@class, 'close')]/..")
    AtlasWebElement closeButton();

    @Name("Скрытая дата утверждения")
    @FindBy("//label[text()='{{ text }}']/../input[@disabled]")
    ElementsCollection<AtlasWebElement> disabledDate(@Param("text") String text);
}

package elements.scheduleBoard;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface ConfirmationWindow extends AtlasWebElement {
    @Name("Кнопка \"Вернуться к расписанию\" всплывающего окна подтверждения")
    @FindBy("//button[contains(text(),'Вернуться')]")
    AtlasWebElement buttonStay();

    @Name("Кнопка \"Всё равно уйти\" всплывающего окна подтверждения")
    @FindBy("//button[contains(text(),'Всё равно уйти')]")
    AtlasWebElement buttonQuit();

}

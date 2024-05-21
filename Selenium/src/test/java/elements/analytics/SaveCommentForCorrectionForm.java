package elements.analytics;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface SaveCommentForCorrectionForm extends AtlasWebElement {

    @Name("Поле ввода комментария")
    @FindBy("./..//textarea[@id='comment']")
    AtlasWebElement inputComment();

    @Name("Кнопка \"Сохранить\" комментарий")
    @FindBy("./..//button[contains(text(), 'Сохранить')]")
    AtlasWebElement saveButton();

    @Name("Кнопка \"Отменить\" ввод комментария")
    @FindBy("./..//button[contains(text(), 'Отменить')]")
    AtlasWebElement cancelButton();

}

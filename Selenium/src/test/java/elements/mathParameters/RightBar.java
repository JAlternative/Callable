package elements.mathParameters;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface RightBar extends AtlasWebElement {

    @Name("Поле имени математического параметра")
    @FindBy("//textarea[@id='short-name-1']")
    AtlasWebElement nameParameters();

    @Name("Чекбокс, позволяет скрывать объект")
    @FindBy(".//span[@class=\"mdl-checkbox__ripple-container mdl-js-ripple-effect mdl-ripple--center\"]")
    AtlasWebElement makeItemHidden();

    @Name("Кнопка сохранения")
    @FindBy(".//div[@class=\"mdl-list__item\"]//button[@click.trigger=\"save()\"]")
    AtlasWebElement buttonSave();

    @Name("Кнопка отмены")
    @FindBy(".//div[@class=\"mdl-list__item\"]//button[@click.trigger=\"close()\"]")
    AtlasWebElement buttonCancel();
}

package elements.personalSchedule;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface FreeShiftDialog  extends AtlasWebElement {
    @Name("Все свободные смены")
    @FindBy(".//div[@class='mdl-list__text-fields-container']")
    ElementsCollection<AtlasWebElement> freeShifts();

    @Name("Кнопка \"Взять\" для конкретной свободной смены")
    @FindBy(".//div[@class='mdl-list__text-fields-container'][1+{{ order }}]/div/button")
    AtlasWebElement takeFreeShiftButton(@Param("order") int order);

    @Name("Поле с информацией о конкретной свободной смене")
    @FindBy(".//div[@class='mdl-list__text-fields-container'][1+{{ order }}]/div/label[contains(text(), '{{ label }}')]/preceding-sibling::input")
    AtlasWebElement freeShiftField(@Param("order") int order, @Param("label") String label);
}

package elements.scheduleBoard;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface SetResetTimetableForm extends AtlasWebElement {

    @Name("Кнопки дата окончания и начала")
    @FindBy("//div[contains(@class,'mdl-layout__right') and not (contains(@class, 'hide'))]//label[text()='{{ endOrStart }}']/../button")
    AtlasWebElement buttonStartOrEnd(@Param("endOrStart") String endOrStart);

    @Name("Кнопка \"Рассчитать\"")
    @FindBy("//button[@class='mdl-button mdl-button--primary au-target mdl-js-button'][contains(text(),'Рассчитать')]")
    AtlasWebElement buttonSet();

    @Name("Чекбокс \"С минимальным отклонением\"")
    @FindBy("//div[@show.bind='showWithMinDeviation']//span[contains(@class, 'box-outline')]")
    AtlasWebElement elementCheckbox();

    @Name("Поле с информацией о текущем статусе FTE")
    @FindBy("//span[contains(text(),'FTE')]")
    AtlasWebElement elementFTESign();

    @Name("Кнопка закрытия формы")
    @FindBy(".//i[contains(@class, 'close')]/..")
    AtlasWebElement closeFormButton();

    @Name("Поле ввода даты начла и конца")
    @FindBy("//div[contains(@class,'mdl-layout__right') and not (contains(@class, 'hide'))]//label[text()='{{ endOrStart }}']/..//input")
    AtlasWebElement dateStartOrEndInput(@Param("endOrStart") String endOrStart);

}

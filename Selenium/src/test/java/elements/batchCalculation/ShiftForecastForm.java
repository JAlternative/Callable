package elements.batchCalculation;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface ShiftForecastForm extends AtlasWebElement {

    @Name("Чекбокс минимального отклонения")
    @FindBy(".//label[@class='mdl-checkbox au-target mdl-js-checkbox is-upgraded']")
    AtlasWebElement checkboxMIN();

    @Name("Кнопка рассчитать")
    @FindBy(".//button[contains(@t, 'common.actions.calculate') and contains(text(), normalize-space('Рассчитать')) and @show.bind='!loading']")
    AtlasWebElement calculateShift();

    @Name("Кнопка зарывающая форму не сохраняя изменения")
    @FindBy(".//button[@click.trigger=\"close()\"]")
    AtlasWebElement buttonCloseForm();

}

package elements.analytics;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface DiagramScopeSwitcher extends AtlasWebElement {

    @Name("Кнопка возврата на текущий временной промежуток графика")
    @FindBy(".//button[contains(@click.trigger, 'second.toCurrent()')]")
    AtlasWebElement resetButton();

    @Name("Кнопка \"R\" выбора правого графика")
    @FindBy(".//button[@click.trigger=\"chooseCtrl(true)\"]")
    AtlasWebElement selectRightChartButton();

    @Name("Кнопка \"L\" выбора левого графика")
    @FindBy(".//button[@click.trigger=\"chooseCtrl()\"]")
    AtlasWebElement selectLeftChartButton();

    @Name("Кнопка для загрузки предыдущего месяца")
    @FindBy(".//button[@show.bind = 'showPreviousButton' ]")
    AtlasWebElement leftGraph();

    @Name("Кнопка для загрузки следущего месяца")
    @FindBy(".//button[@show.bind = 'showNextButton' ]")
    AtlasWebElement rightGraph();
}

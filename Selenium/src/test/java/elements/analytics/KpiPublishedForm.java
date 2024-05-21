package elements.analytics;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface KpiPublishedForm extends AtlasWebElement {

    @Name("Кнопка Закрыть")
    @FindBy("//h5[@t='dialogs.kpiPublished.title']/ancestor::div[2]/button")
    AtlasWebElement kpiPublishedCloseForm();

    @Name("Кнопка Опубликовать")
    @FindBy("//h5[@t='dialogs.kpiPublished.title']/ancestor::div[3]//button[@t='common.actions.approve']")
    AtlasWebElement kpiPublishedSubmit();

    @Name("Кнопка Выбрать месяц")
    @FindBy("//h5[@t='dialogs.kpiPublished.title']/ancestor::div[3]//button/i[contains(@class, 'calendar')]")
    AtlasWebElement kpiPublishedMonth();

    @Name("Поле с текущим временным периодом")
    @FindBy("//h5[@t='dialogs.kpiPublished.title']/ancestor::div[3]//i[contains(@class, 'calendar')]/..//input")
    AtlasWebElement dateInputField();
}

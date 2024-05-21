package elements.analytics;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.AtlasWebElement;

import java.util.List;

public interface FteChanges extends AtlasWebElement {
    @Name("Кнопка Закрыть")
    @FindBy(".//div/button[@click.trigger='closeHistory()']")
    AtlasWebElement fteChangesCloseForm();

    @Name("Значения столбца даты")
    @FindBy(".//td[@class = 'mdl-data-table__cell--non-numeric']/span[1]")
    List<AtlasWebElement> dateColumnValue();

    @Name("Значения столбца комментариев")
    @FindBy(".//td[@class = 'mdl-data-table__cell--non-numeric'][3]")
    List<AtlasWebElement> commentsColumnValue();
}

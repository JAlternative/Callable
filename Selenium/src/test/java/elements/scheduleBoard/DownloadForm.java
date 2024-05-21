package elements.scheduleBoard;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.AtlasWebElement;

/**
 * @author Evgeny Gurkin 14.07.2020
 */
public interface DownloadForm extends AtlasWebElement {

    @Name("Кнопка \"Скачать\"")
    @FindBy(".//a[@mdl = 'button' or href.bind=\"downloadUrl\"]")
    AtlasWebElement downloadButton();

    @Name("Поле ввода выбора формата скачиваемого файла")
    @FindBy(".//input[@id = 'jasper-report-shifts-plan-format']")
    AtlasWebElement formatInput();

    @Name("Кнопка формата файла: {type}")
    @FindBy(".//div[contains(text(), '{{ type }}')]")
    AtlasWebElement reportTypeButton(@Param("type") String type);

    @Name("Поле ввода выбора формата скачиваемого файла для Т-13")
    @FindBy(".//input[@id = 'jasper-report-t13-format']")
    AtlasWebElement formatInputT13();

    @Name("Кнопка закрытия боковой панели")
    @FindBy(".//button[@click.trigger = 'close()']")
    AtlasWebElement closeSidebar();
}

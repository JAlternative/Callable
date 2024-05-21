package elements.batchCalculation;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface FteForecastForm extends AtlasWebElement {

    @Name("Лист повышения уровня сервиса")
    @FindBy("//div[@class='mdl-list__item mdl-list__item--flat'][1]//div[@menu]/div[{{ list }}]")
    AtlasWebElement upServiceList(@Param("list") int list);

    @Name("Поле повышения уровня сервиса")
    @FindBy(".//div[@class='mdl-list__item mdl-list__item--flat'][1]//button")
    AtlasWebElement upServiceInput();

    @Name("Лист метода")
    @FindBy(".//div[@class='mdl-list__item mdl-list__item--flat'][2]//div[@menu]/div[{{ type }}]")
    AtlasWebElement fteTypeList(@Param("type") int type);

    @Name("Поле метода")
    @FindBy(".//div[@class='mdl-list__item mdl-list__item--flat'][2]//button")
    AtlasWebElement fteTypeInput();

    @Name("Лист алгоритма")
    @FindBy(".//div[@class='mdl-list__item mdl-list__item--flat'][3]//div[@menu]/div[{{ list }}]")
    AtlasWebElement fteAlgorithmList(@Param("list") int list);

    @Name("Поле алгоритма")
    @FindBy(".//div[@class='mdl-list__item mdl-list__item--flat'][3]//button")
    AtlasWebElement fteAlgorithmInput();

    @Name("Кнопка рассчитать")
    @FindBy(".//button[@click.trigger=\"save()\" and contains(text(), normalize-space('Рассчитать'))  and not (contains(@show.bind, \"!loading\"))]")
    AtlasWebElement buttonCalculateFTE();

    @Name("Кнопка закрывающая форму без сохранения изменений")
    @FindBy(".//button[@click.trigger=\"close()\"]")
    AtlasWebElement buttonCloseFormFTE();

}

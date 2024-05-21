package elements.scheduleBoard;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface OrgUnitMenu extends AtlasWebElement {

    @Name("Выбор кнопки внутри троеточия")
    @FindBy(".//*[@t='{{ variant }}']")
    AtlasWebElement variantsOfFunctions(@Param("variant") String variant);

    //костыль до унификации эелементов в меню троеточия
    @Name("Плановый график внутри троеточия")
    @FindBy(".//span[text()='Плановый график' or text()='Выгрузить плановый график']")
    AtlasWebElement plannedSchedule();

    @Name("Выбор кнопки внутри троеточия по названию")
    @FindBy("//*[contains(@class, 'menu')]//*[contains(text(),'{{ name }}')]")
    AtlasWebElement textOfFunctions(@Param("name") String name);

    @Name("Выбор кнопки внутри троеточия")
    @FindBy(".//*[normalize-space() = '{{ variant }}']")
    AtlasWebElement variantsOfFunctionsHardcoded(@Param("variant") String variant);

    @Name("Все кнопки внутри троеточия")
    @FindBy("//div[@menu='org-unit-menu']//span[@t]")
    ElementsCollection<AtlasWebElement> allVariantsOfFunctions();

}

package elements.positionTypes;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface Table extends AtlasWebElement {

    @Name("Список названий позиций")
    @FindBy(".//td[@class = 'mdl-data-table__cell--non-numeric']")
    ElementsCollection<AtlasWebElement> allPositionNames();

    @Name("Кнопка троеточия позиции {positionName}")
    @FindBy(".//td[normalize-space(text()) = '{{ positionName }}']/..//button[contains(@id , 'context-menu-')]")
    AtlasWebElement tripleDotByName(@Param("positionName") String positionName);

    @Name("Кнопка перетаскивания позиции {positionName}")
    @FindBy(".//td[normalize-space(text()) = '{{ positionName }}']/..//button[contains(@t, 'drag')]")
    AtlasWebElement dragButtonByName(@Param("positionName") String positionName);

    @Name("Кнопка {variant} в меню выбора троеточия")
    @FindBy(".//div[@class = 'mdl-menu__container is-upgraded is-visible']//li[contains(text(), '{{ variant }}')]")
    AtlasWebElement inTripleDotButton(@Param("variant") String variant);

    @Name("Красная кнопка +")
    @FindBy("//button[@click.delegate = 'addType()']")
    AtlasWebElement redPlusButton();


    @Name("Всплывающее сообщение")
    @FindBy("//div[@class = 'mdl-snackbar__text']")
    AtlasWebElement popUP();


}

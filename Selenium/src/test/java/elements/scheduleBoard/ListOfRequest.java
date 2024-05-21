package elements.scheduleBoard;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface ListOfRequest extends AtlasWebElement {

    @Name("Кнопка треоточие в форме запроса")
    @FindBy("//button[@id='schedule-request-menu']")
    AtlasWebElement threeDotsButton();

    @Name("Кнопка \"Удалить у серии\"")
    @FindBy("//div[@class='load load--in au-target']//button[2]")
    AtlasWebElement deleteButton();

    @Name("Кнопка действия с запросом")
    @FindBy("//div[@class='menu menu--right au-target is-visible']")
    AtlasWebElement typeButtonsForm();

    @Name("Варианты действий с запросом")
    @FindBy("//div[@class='menu menu--right au-target is-visible']/div[contains(text(),'{{ type }}')]")
    AtlasWebElement typeButtons(@Param("type") String type);

    @Name("Радио кнопка \"Удалить серию\"")
    @FindBy("//label[@for='list-action-variant-2']")
    AtlasWebElement deleteAllRequestRadioButton();

    @Name("Радио кнопка \"Удалить запрос\"")
    @FindBy("//label[@for='list-action-variant-1']")
    AtlasWebElement deleteRequestRadioButton();

    @Name("Поле ввода даты окончания")
    @FindBy(".//div[contains(@show.bind, 'scheduleRequest.repeatRule.periodicity')]//input")
    AtlasWebElement dateEndInput();

    @Name("Кнопка раскрытия меню периодичности")
    @FindBy("(//label[@t='common.essentials.periodicity']/../button)[2]")
    AtlasWebElement periodicitySelectButton();

    @Name("Кнопка с выбранной периодичностью")
    @FindBy("//div[@class='menu menu--short menu--bottom au-target is-visible']//div")
    ElementsCollection<AtlasWebElement> periodicityTypeButtons();

    @Name("Панель выбора периодичности")
    @FindBy("//div[@menu ='schedule-request-periodicity']")
    AtlasWebElement periodicityPanel();


}

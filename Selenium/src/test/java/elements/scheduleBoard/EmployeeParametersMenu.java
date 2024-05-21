package elements.scheduleBoard;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;

import java.util.List;

public interface EmployeeParametersMenu extends AtlasWebElement {

    @Name("Список групп мат. параметров")
    @FindBy("//div[@click.delegate='selectList(item)']")
    ElementsCollection<AtlasWebElement> mathParamGroups();

    @Name("Шеврон для выбора группы мат. параметров")
    @FindBy("//label[@for='math-parameter-list']/following-sibling::button")
    AtlasWebElement mathParamGroupsChevron();

    @Name("Надпись {mathParameter}")
    @FindBy("//label[text()='{{ mathParameter }}']")
    AtlasWebElement mathParameterLabel(@Param("mathParameter") String mathParameter);

    @Name("Шеврон {mathParameter}")
    @FindBy("//*[text()='{{ mathParameter }}']/following-sibling::button")
    AtlasWebElement mathParameterDropdown(@Param("mathParameter") String mathParameter);

    @Name("Варианты в матпараметре")
    @FindBy("//div[contains(@class,'is-visible')]//div[contains(text(),'{{ var }}')]")
    AtlasWebElement variantsInMathParam(@Param("var") String var);

    @Name("Кнопка \"Сохранить\" в форме параметров")
    @FindBy("//div[@class='mdl-dialog mdl-dialog--fit-content']//button[normalize-space(text())='Сохранить']")
    AtlasWebElement saveParamButton();

    @Name("Названия мат параметров")
    @FindBy("//div[contains(@hide.bind, 'mathParameter.')]")
    ElementsCollection<AtlasWebElement> mathParamsLabels();

    @Name("Элемент с текстом мат. параметра {mathParameter}")
    @FindBy("//*[text() = '{{ mathParameter }}']")
    AtlasWebElement nameMathParam(@Param("mathParameter") String mathParameter);

    @Name("Поле для ввода текстового параметра {mathParameter}")
    @FindBy("//label[text() = '{{ mathParameter }}']//preceding-sibling::input[@value.bind='mathParameter.value']")
    AtlasWebElement textMathParam(@Param("mathParameter") String mathParameter);

    @Name("Все строковые мат параметры")
    @FindBy("//input[@value.bind='mathParameter.value'and @type='text']/following::label[contains(@for, 'math-parameter')]")
    ElementsCollection <AtlasWebElement> textMathParamList();
}

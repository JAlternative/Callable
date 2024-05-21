package elements.addOrgUnit;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;
import org.openqa.selenium.WebElement;

public interface AddUnitForm extends AtlasWebElement {

    @Name("Поле для выбора имени подразделения")
    @FindBy("//input[contains(@id, 'org-unit-name')]")
    AtlasWebElement unitName();

    @Name("Поле для выбора типа подразделения")
    @FindBy("//input[contains(@id, 'org-unit-type')]")
    AtlasWebElement unitType();

    @Name("Поле для выбора заместителя")
    @FindBy("//input[contains(@id, 'deputy')]")
    AtlasWebElement deputy();

    @Name("Окно для ввода даты начала и окончания работы подразделения")
    @FindBy(".//div/div[3]/div/div/div[{{ number }}]/input")
    AtlasWebElement dateFieldForUnit(@Param("number") String numberName);

    @Name("Окно для ввода даты начала и окончания работы заместителя")
    @FindBy(".//div/div[5]/div/div/div[{{ number }}]/input")
    AtlasWebElement dateFieldForDeputy(@Param("number") String numberName);

    @Name("ПопАп с сообщением об ошибке создания подразделения")
    @FindBy("//div[@aria-hidden ='false'][contains(@ref,'snack')]")
    AtlasWebElement errorMessage();

    @Name("Поле для выбора родитесльского подразделения")
    @FindBy("//input[contains(@id, 'parent')]")
    AtlasWebElement parentalOrgUnit();

    @Name("Чекбокс - подразделение участвует в расчете")
    @FindBy("//span[contains(@class, 'ripple-container')]")
    AtlasWebElement involvedInCalculationCheckBox();

    @Name("Кнопка создания подразделения")
    @FindBy("//button[contains(@t, 'create')]")
    AtlasWebElement createButton();

    @Name("Чекбокс отмечен")
    @FindBy(".//label[contains(@class,'is-checked')]")
    AtlasWebElement boxIsChecked();

    @Name("Список родительских подразделений")
    @FindBy("//div[contains(@click.delegate, 'orgUnit.parent')]")
    ElementsCollection<WebElement> parentalOrgUnitsList();

    @Name("Кнопка открывает окно для выбора даты работы подразделения")
    @FindBy(".//div/div[3]/div/div/div[{{ number }}]/button")
    AtlasWebElement calenderButtonForUnit(@Param("number") String numberName);

    @Name("Кнопка открывает окно для выбора даты работы заместителя")
    @FindBy(".//div/div[5]/div/div/div[{{ number }}]/button")
    AtlasWebElement calenderButtonForDeputy(@Param("number") String numberName);

    @Name("Ошибка при незаполненном поле названия подразделения")
    @FindBy("//div[@class='mdl-list__item mdl-list__item--flat']//span[@class='mdl-textfield__error'][contains(text(),'не может быть пустым')]")
    AtlasWebElement unitNameFieldError();

    @Name("Ошибка при незаполненном поле типа подразделения")
    @FindBy("//div[contains(@show.bind,'type')]//span[@class='mdl-textfield__error'][contains(text(),'не может быть пустым')]")
    AtlasWebElement typeFieldError();

    @Name("Ошибка при незаполненном поле начала работы подразделения")
    @FindBy("//div[contains(@show.bind,'dateFrom')]//span[@class='mdl-textfield__error'][contains(text(),'не может быть пустым')]")
    AtlasWebElement unitStartDateFieldError();

    @Name("Ошибка при незаполненном поле родительского подразделения")
    @FindBy("//div[contains(@show.bind,'parent')]//span[@class='mdl-textfield__error'][contains(text(),'не может быть пустым')]")
    AtlasWebElement parentalUnitFieldError();

    @Name("Ошибка: дата закрытия юнита до даты открытия")
    @FindBy(".//div/div[3]/div//span[contains(text(),'не')]")
    AtlasWebElement unitDateError();

    @Name("Ошибка: дата окончания работы заместителя до даты начала")
    @FindBy(".//div/div[5]/div//span[contains(text(),'не')]")
    AtlasWebElement deputyDateError();

    @Name("Список типов подразделения")
    @FindBy(".//div[@class='menu__item au-target'][contains(text(),'{{ typeName }}')]")
    AtlasWebElement unitTypeList(@Param("typeName") String type);

}

package elements.orgstructure;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface EmpCreateCardForm extends AtlasWebElement{
    @Name("Поле ввода {{ name }} сотрудника")
    @FindBy("//label[text() = '{{ name }}']//preceding-sibling::input")
    AtlasWebElement empNameField(@Param("name") String name);

    @Name("Поле ввода фамилии сотрудника")
    @FindBy(".//input[@id='last-name']")
    AtlasWebElement lastNameField();

    @Name("Поле ввода имени сотрудника")
    @FindBy(".//input[@id='first-name']")
    AtlasWebElement firstNameField();

    @Name("Поле ввода отчества сотрудника")
    @FindBy(".//input[@id='patronymic-name']")
    AtlasWebElement patronymicNameField();

    @Name("Поле ввода подразделения")
    @FindBy(".//input[contains(@id,'employee-org-unit')]")
    AtlasWebElement unitField();

    @Name("Поле \"Поиск\" подразделения")
    @FindBy(".//input[@placeholder='Поиск']")
    AtlasWebElement searchField();

    @Name("Название подразделения {{ name }} в поисковом запросе")
    @FindBy(".//div[@ref='listElement']/div[normalize-space(text())='{{ unitName }}']")
    AtlasWebElement unitNameInList(@Param("unitName") String unitName);

    @Name("Поле ввода должности")
    @FindBy(".//input[contains(@id,'employee-position-')]")
    AtlasWebElement jobTitleField();

    @Name("Должность в выпадающем списке")
    @FindBy(".//div[contains(normalize-space(text()),'{{ jobTitle }}') and @class='menu__item au-target']")
    AtlasWebElement jobTitle(@Param("jobTitle") String jobTitle);

    @Name("Дата {{ date }}")
    @FindBy(".//label[text()='{{ date }}']/preceding-sibling::input")
    AtlasWebElement workDate(@Param("date") String date);

    @Name("Кнопка \"Создать\"")
    @FindBy(".//button[@click.trigger='add()' and normalize-space(text())='Создать']")
    AtlasWebElement createButton();

}

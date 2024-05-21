package elements.scheduleBoard;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;

import java.util.List;

public interface OmEditingForm extends AtlasWebElement {

    @Name("Тип оргЮнита")
    @FindBy(".//div[@menu='org-unit-type']/div[contains(text(), '{{ type }}')]")
    AtlasWebElement omTypeButton(@Param("type") String type);

    @Name("Поле ввода {fieldType}")
    @FindBy(".//input[contains(@id, '{{ fieldType }}')]")
    AtlasWebElement omFieldInput(@Param("fieldType") String fieldType);

    @Name("Поле ввода {dataType}")
    @FindBy(".//label[contains(text(), '{{ dateType }}')]/preceding-sibling::input")
    AtlasWebElement omDateInput(@Param("dateType") String dateType);

    @Name("Область вокруг поля ввода {dataType}")
    @FindBy(".//label[contains(text(), '{{ dateType }}')]/preceding-sibling::input/..")
    AtlasWebElement omDateArea(@Param("dateType") String dateType);

    @Name("Сообщение об ошибки под полем ввода {dataType}")
    @FindBy(".//label[contains(text(), '{{ dateType }}')]/preceding-sibling::input/../span")
    AtlasWebElement omDateError(@Param("dateType") String dateType);

    @Name("Список заместителей в выпадающем списке")
    @FindBy(".//div[@click.delegate='selectDeputyEmployee(employee)']")
    ElementsCollection<AtlasWebElement> omDeputyEmployeeList();

    @Name("Кнопка с заместителем параметризованная по его имени")
    @FindBy(".//div[@menu='org-unit-deputy-employee']/div[contains(text(), '{{ DeputyUser }}')]")
    AtlasWebElement omDeputyEmployee(@Param("DeputyUser") String DeputyUser);

    @Name("Поле \"Дата окончания замещения\"")
    @FindBy(".//label[contains(text(), '{{ date }}')]/preceding-sibling::input")
    AtlasWebElement omDeputyDateInput(@Param("date") String date);

    @Name("Кнопка\"Изменить\"")
    @FindBy(".//button[@click.trigger=\"save(0)\"]")
    AtlasWebElement changeButton();

    @Name("Поле \"Теги\"")
    @FindBy("//input[@class='mdl-chip__input au-target']")
    AtlasWebElement tagSpace();

    @Name("Тег по имени")
    @FindBy("//label[text()='Теги']/..//span[contains(@class,'chip--deletable')]/span[text()='{{ tag }}']/../button")
    AtlasWebElement deleteButtonTag(@Param("tag") String tag);

    @Name("Тег по имени в выпадающем списке")
    @FindBy("//span[@class=\"mdl-chips__input\"]/div/div[contains(text(),\"{{ tag }}\")]")
    AtlasWebElement tagInDropdownList(@Param("tag") String tag);

    @Name("Кнопка +1 рядом с полем \"Теги\"")
    @FindBy(".//i[@click.trigger=\"plusOne()\"]")
    AtlasWebElement tagAddOne();

    @Name("Кнопки удаления тегов")
    @FindBy(".//button[@click.trigger = 'remove(item)']/i")
    ElementsCollection<AtlasWebElement> tagDeleteButtons();

    @Name("Кнопка\"Отменить\"")
    @FindBy("//div[contains(@show.bind, 'edit') and not(contains(@class, 'hide'))]//button[contains(@t, 'cancel')]")
    AtlasWebElement cancelButton();

    @Name("Поле даты в отделе \"Исключения\"")
    @FindBy("//div[(@show.bind='businessHoursList && (day.show || day.local)') and not(contains(@class,'hide'))]/div/div/div[1]/input[not(@readonly) and not(@disabled)]")
    AtlasWebElement exceptionDate();

    @FindBy("//div[contains(@menu,'special-days-type') and (contains(@class,'visible'))]/div[text()='{{ type }}']")
    AtlasWebElement typeOfException(@Param("type") String type);

    @FindBy("//div[(@show.bind='businessHoursList && (day.show || day.local)') and not(contains(@class,'hide'))]//div[contains(@class.bind,\"day.type.name\")]/button[not(contains(@class,'hide'))]/i")
    AtlasWebElement chooseType();

    @FindBy("//div[contains(@class, 'org-structure__card')]/div[contains(@class, 'mdl-list')]//div[(@show.bind='businessHoursList && (day.show || day.local)') and not(contains(@class,'hide'))]//input[@value.bind='rawDate' and @readonly and @disabled]/../..//*[text()='{{ type }}']/../input")
    ElementsCollection<AtlasWebElement> chooseTime(@Param("type") String type);

}

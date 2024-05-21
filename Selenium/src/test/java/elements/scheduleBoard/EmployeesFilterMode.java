package elements.scheduleBoard;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface EmployeesFilterMode extends AtlasWebElement {

    @Name("Кнопка \"Применить\"")
    @FindBy(".//button[@t='common.actions.apply']")
    AtlasWebElement applyButton();

    @Name("Кнопка закрытия формы")
    @FindBy(".//button[@click.trigger ='close()']")
    AtlasWebElement closeFilterModeButton();

    @Name("Список полей с именами сотрудников с активированными чекбоксами")
    @FindBy("//label[contains(@for, 'employee-position-filter-') and (contains(@class, 'is-checked'))]/../..//div[contains(@class,'mdl-list__item')]/span[not(contains(@class, 'mdl-list__item-sub-title')) and not(contains(@class, 'aurelia-hide'))]")
    ElementsCollection<AtlasWebElement> employeesWithActiveCheckBoxes();

    @Name("Список полей с именами сотрудников с деактивированными чекбоксами")
    @FindBy("//label[contains(@for, 'employee-position-filter-') and not (contains(@class, 'is-checked'))]/../..//div[contains(@class,'mdl-list__item')]/span[not(contains(@class, 'mdl-list__item-sub-title'))]")
    ElementsCollection<AtlasWebElement> employeesWithNoActiveCheckBoxes();

    @Name("Список полей с именами сотрудников")
    @FindBy("//label[contains(@for, 'employee-position-filter-')]/../..//div[contains(@class,'mdl-list__item')]/span[not(contains(@class, 'mdl-list__item-sub-title'))]")
    ElementsCollection<AtlasWebElement> allEmployees();

    @Name("Список из всех чекбоксов, кроме \"Выбрать все\"")
    @FindBy("//label[contains(@for, 'employee-position-filter-')]//input[not(contains(@id, 'employee-position-filter-all'))]/following-sibling::span[3]")
    ElementsCollection<AtlasWebElement> checkBoxesList();

    @Name("Чекбокс \"Выбрать все\"")
    @FindBy(".//div[contains(text(),'Выбрать все')][@class='mdl-list__item-primary-content']/..//label[@for='employee-position-filter-all']//span[3]")
    AtlasWebElement checkBoxesSelectAll();

    @Name("Шеврон кнопки раскрытия групп позиций")
    @FindBy(".//div[contains(@class.bind, 'selectedPositionGroup')]//button")
    AtlasWebElement positionGroupChevron();

    @Name("Кнопка выбора группы: {groupName}")
    @FindBy(".//div[@click.delegate = 'positionGroupChoose(item)'][normalize-space(text()) = '{{ groupName }}']")
    AtlasWebElement groupNameButton(@Param("groupName") String groupName);

    //только пятерочка
    @Name("Шеврон кнопки раскрытия типов персонала")
    @FindBy(".//div[contains(@class.bind, 'selectedPersonalTypes')]//button")
    AtlasWebElement personnelTypeChevron();

    @Name("Выбор локатора для <<функциональной роли>> в фильтре сотрудников в расписании ДЛЯ ПОЧТЫ: {groupName}")
    @FindBy(".//div[@click.delegate = 'positionGroupChoose(item)']//span[text()='{{ groupName }}']")
    AtlasWebElement groupItemButton(@Param("groupName") String groupName);

    @Name("Кнопка выбора типа персонала: {personnelType}")
    @FindBy(".//div[@click.delegate = 'personalTypeChoose(item)']/span[text() = '{{ personnelType }}']/..")
    AtlasWebElement personnelTypeButton(@Param("personnelType") String personnelType);

}

package pages;

import elements.fteOperationValues.*;
import elements.general.DatePickerForm;
import io.qameta.atlas.webdriver.WebPage;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;

public interface FteOperationValuesPage extends WebPage {

    @Name("Левая панель с фильтрами")
    @FindBy("//div[contains (@class, 'org-structure__filters')]")
    FilterPanel filterPanel();

    @Name("Окно выбора \"Типа событий\"")
    @FindBy("//h4[text() = 'Типы событий']/../../.")
    EventTypeForm eventTypeForm();

    @Name("Окно выбора \"KPI\"")
    @FindBy("//h4[text() = 'KPI']/../../.")
    KpiTypeForm kpiTypeForm();

    @Name("Окно выбора \"Функциональные роли\"")
    @FindBy("//h4[text() = 'Функциональные роли']/../../.")
    FunctionalRoleForm functionalRoleForm();

    @Name("Форма календаря для выбора даты")
    @FindBy("//div[contains(@class , 'datetimepicker--open')]")
    DatePickerForm datePickerForm();

    @Name("Форма таблицы")
    @FindBy("//table[@class = 'table']/..")
    TableForm tableForm();

}

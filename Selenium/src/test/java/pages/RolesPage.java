package pages;


import elements.general.DatePickerForm;
import elements.roles.EditRoleForm;
import elements.roles.OrgUnitSearchForm;
import elements.roles.RolesSearchForm;
import elements.roles.ViewRoleForm;
import io.qameta.atlas.webdriver.WebPage;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.AtlasWebElement;
import org.openqa.selenium.WebElement;

public interface RolesPage extends WebPage {

    @Name("Вся форма с полем поиска и результатими поиска")
    @FindBy("//div[@class='org-structure__items mdl-shadow--4dp']")
    RolesSearchForm rolesSearchForm();

    @Name("Форма календаря для выбора даты в меню роли")
    @FindBy("//*[contains(@class, 'datetimepicker--open')]")
    DatePickerForm datePickerFormInRole();

    @Name("Карточка с информацией о ролях")
    @FindBy("//div[@class = 'org-structure__card mdl-shadow--4dp au-target']")
    ViewRoleForm viewRoleForm();

    @Name("Форма редактирования роли")
    @FindBy("//div[@class = 'org-structure__card mdl-shadow--4dp au-target']")
    EditRoleForm editRoleForm();

    @Name("Кнопка перехода в раздел управления ролями")
    @FindBy("//*[@id='org-structure__big-btn--2']/i")
    WebElement rolesButton();

    @Name("Кнопка перехода в раздел управления сотрудниками")
    @FindBy("//*[@id='org-structure__big-btn--1']/i")
    AtlasWebElement employeesButton();

    @Name("Кнопка \"Раздать права\"")
    @FindBy("//*[@class='mdi mdi-account-key']")
    AtlasWebElement assignRolesButton();

    @Name("Форма поиска оргюнита")
    @FindBy("//div[@class='mdl-dialog--not-fullscreen-api-render org-structure__dialog au-target mdl-dialog--scroll']/div")
    OrgUnitSearchForm orgUnitSearchForm();

    @Name("Кнопка \"Роли\" с изображением человека и ключа")
    @FindBy("//div[@class='org-structure__big-btn au-target']/i[@class='org-structure__icon mdi mdi-account-key']")
    AtlasWebElement rolesKey();


}
package elements.orgstructure;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;

import java.util.List;

public interface EditRoleForm extends AtlasWebElement {

    //Элементы режима просмотра
    @Name("Блоки просмотра информации об отдельных ролях")
    @FindBy(".//i[@class='mdi mdi-account-key mdl-list__item-icon']/../../..")
    List<SingleRoleViewBlock> allRoleViewBlocks();

    @Name("Блок просмотра информации об одной роли")
    @FindBy("(//div[@class = 'org-structure__card mdl-shadow--4dp au-target']" +
            "//i[@class='mdi mdi-account-key mdl-list__item-icon']/../../..)[{{ order }}]")
    SingleRoleViewBlock roleViewBlock(@Param("order") int order);

    @Name("Кнопка редактирования роли (Карандаш)")
    @FindBy(".//i[@class = 'mdi mdi-pencil']")
    AtlasWebElement pencilButton();

    @Name("Названия всех ролей")
    @FindBy("//i[@class = 'mdi mdi-account-key mdl-list__item-icon']/../span[1]")
    List<AtlasWebElement> roleTitles();

    //Элементы режима редактирования
    @Name("Блоки редактирования отдельных ролей")
    @FindBy(".//label[text()='Роль']/../../../../..")
    List<SingleRoleEditBlock> allRoleEditBlocks();

    @Name("Блок редактирования одной роли")
    @FindBy("(//div[@class = 'org-structure__card mdl-shadow--4dp au-target']" +
            "//label[text()='Роль']/../../../../..)[{{ order }}]")
    SingleRoleEditBlock roleEditBlock(@Param("order") int order);

    @Name("Все поля выбора роли для сотрудника")
    @FindBy(".//input[contains(@value.bind , 'getRoleName')]")
    ElementsCollection<AtlasWebElement> allRoleInputs();

    @Name("Кнопка \"Изменить\"")
    @FindBy(".//button[@click.trigger = 'save(0)']")
    AtlasWebElement saveButton();

    @Name("Кнопка \"Выбрать ОЮ\"")
    @FindBy("(//button[@click.trigger = 'chooseUnits(role, true)'])[{{ order }}]")
    AtlasWebElement chooseOrgUnitButton(@Param("order") int order);

    @Name("Кнопка \"Отменить\"")
    @FindBy(".//button[@click.trigger = 'editTrigger(0)']")
    AtlasWebElement cancelButton();

    @Name("Нижний блок")
    @FindBy(".//button[@click.trigger = 'editTrigger(0)']/..")
    AtlasWebElement buttonsBlock();

    //Общие элементы
    @Name("Спиннер для меню роли")
    @FindBy(".//div[@class = 'org-structure__card mdl-shadow--4dp au-target']//div[@show.bind='show[0]']//div[@class='mdl-spinner__circle']")
    AtlasWebElement spinner();

    @Name("Надпись в блоке ролей")//fixme
    @FindBy(".//div[@t ='common.noData']")
    AtlasWebElement titleInRoleBlock();

    @Name("Статус выбора оргюнита из списка")//fixme
    @FindBy("(//div[@class ='mdl-list__item mdl-list__item--two-line']/..)[{{ order }}]//div[@class = 'mdl-list__item']//span")
    AtlasWebElement chosenOrgUnitText(@Param("order") int order);

}

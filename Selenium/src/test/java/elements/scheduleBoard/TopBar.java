package elements.scheduleBoard;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface TopBar extends AtlasWebElement {

    @Name("Кнопка троеточие")
    @FindBy("//button[@id='org-unit-menu']")
    AtlasWebElement buttonOrgUnitMenu();

    @Name("Кнопка выбора подразделения")
    @FindBy("//i[@class='mdi mdi-store']")
    AtlasWebElement storeSelectButton();

    @Name("Строка поиска оргюнита")
    @FindBy("//div[contains(@class, 'searchable') and (contains(@class, 'visible'))]//input")
    AtlasWebElement searchOrgUnitInput();

    @Name("Список подразделений")
    @FindBy(".//div[@class='menu__item menu__item--wrap au-target']")
    ElementsCollection<AtlasWebElement> storesList();

    @Name("Кнопка раскрывающая список версий графика")
    @FindBy(".//i[@class='mdi mdi-history']/..")
    AtlasWebElement buttonListOfTimetables();

    @Name("Список с текстом версии графика")
    @FindBy("//div[@click.trigger='selectRoster(roster)']//div[1]")
    ElementsCollection<AtlasWebElement> listOfTimeTablesVersions();

    @Name("Поле отображения месяца")
    @FindBy(".//span[@style = 'text-transform: capitalize']")
    AtlasWebElement monthSelected();

    @Name("Поле отображения текущей даты")
    @FindBy("//div[@class='gantt-chart__time']/span")
    AtlasWebElement currentTime();

    @Name("Числа месяца над графиком на текущем масштабе")
    @FindBy("//div[(contains(@class,'gantt-chart__link au-target') ) and not(contains(@class,'tick'))]")
    ElementsCollection<AtlasWebElement> dateAboveGraph();

    @Name("Числа часов над графиком в масштабе день")
    @FindBy(".//div[@class = 'gantt-chart__tick au-target']/div")
    ElementsCollection<AtlasWebElement> hourAboveGraph();

    @Name("Кнопка перехода в режим сранения табеля")
    @FindBy("//div[@class='menu menu--shadow-16dp au-target is-visible']/div[2]")
    AtlasWebElement timeSheetCompareButton();

    @Name("Кнопка перехода в режим сранения графиков")
    @FindBy("//div[@class='menu menu--shadow-16dp au-target is-visible']/div[1]")
    AtlasWebElement scheduleCompareButton();

    @Name("Кнопка отображения табеля или фактического посещения")
    @FindBy("//div[@click.trigger='timeSheetsTrigger()']")
    AtlasWebElement isTimeSheetButton();

    @Name("Список из всех неактивных ростеров")
    @FindBy("//div[@menu='history']/div[@class='menu__item menu__item--two-line au-target']//span[@class='menu__icon mdi au-target']/..")
    ElementsCollection<AtlasWebElement> nonActiveRostersList();

    @Name("Опубликованный ростер")
    @FindBy("//div[@menu='history']/div[@class='menu__item menu__item--two-line au-target']//span[@t='views.shifts.version_published']/..")
    AtlasWebElement publishedRoster();

    @Name("Кнопка фильтра сотрудников")
    @FindBy(".//button[@click.trigger = 'openEmployeeFilterDialog()']")
    AtlasWebElement employeeFilterButton();

    @Name("Кнопка меню (i)")
    @FindBy("//i[@class='mdi mdi-information-outline']")
    AtlasWebElement menuIButton();

    @Name("Набор значений падающего списка подразделений")
    @FindBy("//div[@class='menu menu--searchable menu--shadow-16dp au-target is-visible']//div[@class='menu__item menu__item--wrap au-target']")
    ElementsCollection<AtlasWebElement> dropDownList();

    @Name("Пункт { itemTitle }  падающего списка кнопки меню (i)")
    @FindBy("//div[contains(@class,'menu__item au-target') and span = '{{ itemTitle }}']")
    AtlasWebElement iButtonAdditionalInformationItem(@Param("itemTitle") String item);

    @Name("Набор пунктов меню, отмеченных крыжами ")
    @FindBy("//div[@class=\"menu__item au-target\"]//span[@class=\"menu__icon mdi au-target mdi-check\"]")
    ElementsCollection<AtlasWebElement> selectedElementList();
}

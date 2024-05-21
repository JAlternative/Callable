package elements.batchCalculation;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;
import pages.TreeBehavior;

public interface MainPanel extends AtlasWebElement, TreeBehavior {

    @Name("Список всех разворачивающих стрелок на странице")
    @FindBy(".//i[contains(@class, 'chevron-up')]/..")
    ElementsCollection<AtlasWebElement> arrayOfUnfoldingArrows();

    @Name("Список всех отображаемых названий подразделений")
    @FindBy(".//div[@class='mdl-list__item batch-tree-view__org-unit-container']")
    ElementsCollection<AtlasWebElement> listOfOrgUnitNames();

    @Name("Статус расчета прогноза")
    @FindBy("//div[@class='batch-calculation__orgunits-container']//div[{{ status }}]"
            + "//div[2][contains(text(),'Идет расчет')or( 'В очереди')]")
    AtlasWebElement calculationStatus(@Param("status") String status);

    @Name("Лист опций подразделений")
    @FindBy(".//div[@class='batch-tree-view__calculation-header'][{{ option }}]//div[1]//button")
    AtlasWebElement listOptions(@Param("option") int option);

    @Name("Форма выбора опции Прогноз")
    @FindBy(".//div[@class='mdl-menu__container is-upgraded is-visible']//li[{{ notificationName }}]")
    AtlasWebElement optionsMenuForecast(@Param("notificationName") int notificationName);

    @Name("Кнопка ОМ {name} в дереве")
    @FindBy(".//a[text() ='{{ name }}']")
    AtlasWebElement oneOmNameButton(@Param("name") String name);

    @Override
    @Name("Шеврон соответствующего ОМ в дереве")
    @FindBy("//span[@title = '{{ name }}']/..//following-sibling::button/i")
    AtlasWebElement chevronButton(@Param("name") String name);

    @Name("Главная часть не развернутого дерева")
    @FindBy("//div[contains(@class, 'batch-tree-view__org-unit-container')]")
    AtlasWebElement mainTreePanel();

    @Name("состояние прогноза")
    @FindBy(".//div[contains(text(),'Расчет завершен')]")
    AtlasWebElement kpiForecastCalculationStatus();

    @Name("Кнопка с надписью \"Ошибка\" в столбце прогноз")
    @FindBy(".//a[@click.delegate = \"showErrorData('forecast')\" and text()='Ошибка']")
    AtlasWebElement errorKpiButton();

    @Name("Кнопка с надписью \"Ошибка\" в столбце FTE")
    @FindBy(".//a[@click.delegate = \"showErrorData('fte')\" and text()='Ошибка']")
    AtlasWebElement errorFteButton();

    @Name("Кнопка с надписью \"Ошибка\" в столбце FTE")
    @FindBy(".//a[@click.delegate = \"showErrorData('rostering')\" and text()='Ошибка']")
    AtlasWebElement errorShiftsButton();

    @Name("Статус в столбце выбранного рассчета")
    @FindBy(".//batch-tree-view[@config.bind='getChildConfig(childOrgUnit)']//div[@class='batch-tree-view__status'][{{ name }}]")
    ElementsCollection<AtlasWebElement> orgStatus(@Param("name") int name);

    @Name("Сообщение о том что расчет параметра все еще идет")
    @FindBy(".//div[@class='batch-tree-view__status'][{{ name }}]//div[contains(text() , 'Идет рассчет')]")
    AtlasWebElement orgCalculateStatus(@Param("name") int name);

    @Name("Статус заданного типа расчета для конкретного подразделения")
    @FindBy("//span[@title='{{ orgUnitName }}']/../mdl-tree-item-statuses//div[@class='batch-tree-view__status'][{{ calculationType }}]")
    AtlasWebElement calculationStatus(@Param("orgUnitName") String orgUnitName, @Param("calculationType") String calculationType);

    @Name("Кнопка остановки расчета для подразделения")
    @FindBy("//a[@class='link au-target'][contains(text(),'Прервать')]")
    AtlasWebElement cancelCalculationButton(@Param("orgUnitName") String orgUnitName, @Param("calculationType") String calculationType);

    @Name("")
    @FindBy("//input[@id = 'report-custom-search-text']")
    AtlasWebElement inputOmSearchFiled();

    @Name("Чекбокс соответсвующего ОМ в дереве")
    @FindBy("//a[contains(text(), '{{ name }}')]/../mdl-checkbox/label")
    AtlasWebElement checkBoxButton(@Param("name") String name);

    @Name("Серый фон и иконка загрузки оргюнитов")
    @FindBy("//div[@class = 'load load--in au-target']")
    AtlasWebElement spinnerLoadingOm();

    @Name("Кнопка фильтра")
    @FindBy("//button/i[contains(@class,'mdi mdi-filter-variant')]")
    AtlasWebElement filterButton();

    @Name("\"Теги\" в выпадающем списке")
    @FindBy("//div[@click.delegate='showTagsFilterDialog()']")
    AtlasWebElement tagsButton();

    @Name("Нераскрытые шевроны")
    @FindBy("//i[contains(@class,'mdi-chevron-down')]")
    ElementsCollection<AtlasWebElement> closedChevrons();

    @Name("Кнопки \"Загрузить еще\"")
    @FindBy("//button[@click.trigger = 'loadChildrenFromNextUrl()']")
    ElementsCollection<AtlasWebElement> buttonsLoadMore();

    @Name("Отображенные орг юниты")
    @FindBy(".//a[contains(@class, 'mdl-navigation__pointer')]")
    ElementsCollection<AtlasWebElement>displayedOrgUnitNames();
}

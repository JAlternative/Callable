package elements.analytics;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface LeftBarEdit extends AtlasWebElement {

    @Name("Кнопка вызова листа выбора магазина \"дом\"")
    @FindBy("//button[@id='org-units']")
    AtlasWebElement homeButton();

    @Name("Кнопка вызова форма информации \"i\"")
    @FindBy(".//button[@id='indicators']")
    AtlasWebElement indicatorsButton();

    @Name("Кнопка для вызова формы правки результатов прогноза \"карандаш\"")
    @FindBy(".//i[contains(@class, 'pencil')]")
    AtlasWebElement pencilButton();

    @Name("Кнопка добавления графика сравнения")
    @FindBy(".//i[contains(@class, 'compare')]")
    AtlasWebElement compareButton();

    @Name("Кнопка для выбора действия рассчета, публикации и т.д. \"Троеточие\"")
    @FindBy(".//button[@id='diagram-chart-menu']")
    AtlasWebElement diagramChartMenuButton();

    @Name("Выпадающий список подразделений во вкладке \"Расписание\", после нажатия на кнопку вызова листа")
    @FindBy("//div[contains(@class, 'is-visible')]")
    AtlasWebElement listOfDivisions();
}

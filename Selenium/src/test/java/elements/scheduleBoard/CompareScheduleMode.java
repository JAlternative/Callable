package elements.scheduleBoard;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface CompareScheduleMode extends AtlasWebElement {

    @Name("Pop-up с текстом \"Режим сравнения\"")
    @FindBy("//div[@class='mdl-snackbar__text' and (contains(text(), 'Режим сравнения'))]")
    AtlasWebElement comparisonModeSnackBar();

    @Name("Лэйблы с надписью \"Табель учета\" при переходе в ржим сравнения табеля")
    @FindBy("//span[@t='routes.timeSheet']")
    ElementsCollection<AtlasWebElement> timeTableIconsList();

    @Name("Лэйблы с версиями графика")
    @FindBy("//div[@class='gantt-chart__item-legend']//span[@t='views.shifts.version']")
    ElementsCollection<AtlasWebElement> rosterVersionsIconsList();

    @Name("Лэйблы четные с версиями графиков")
    @FindBy("//div[@class='gantt-chart__item-legend'][position() mod 2 = 0]/span")
    ElementsCollection<AtlasWebElement> evenIcons();

    @Name("Лэйблы нечетные с версиями графиков")
    @FindBy("//div[@class='gantt-chart__item-legend'][position() mod 2 = 1]/span")
    ElementsCollection<AtlasWebElement> oddIcons();

}

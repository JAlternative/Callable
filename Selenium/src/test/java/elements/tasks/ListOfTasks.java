package elements.tasks;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.AtlasWebElement;

import java.util.List;

public interface ListOfTasks extends AtlasWebElement {

    @FindBy("//table[@class='mdl-data-table au-target mdl-js-data-table']//tbody")
    List<AtlasWebElement> allTasks();

    @FindBy("//table[@class='mdl-data-table au-target mdl-js-data-table']//tbody/tr/td[4]/span[1]")
    List<AtlasWebElement> allNamesForTask();

    @FindBy("//table[@class='mdl-data-table au-target mdl-js-data-table']//tbody//td[2]/div")
    List<AtlasWebElement> comparisonForStatus();

    @FindBy("//table[@class='mdl-data-table au-target mdl-js-data-table']//tbody//td[3]/span[1]")
    List<AtlasWebElement> comparisonForDate();

    @FindBy("//table[@class='mdl-data-table au-target mdl-js-data-table']//tbody//td[5]")
    List<AtlasWebElement> comparisonForSectionAndEmployee();

    @FindBy("//span[@class='mdl-chip']//span")
    List<AtlasWebElement> comparsionForTag();

    @FindBy("//i[@class='mdi mdi-dots-vertical']")
    AtlasWebElement statusOfTaskButton();

    @FindBy("//div[@class='menu menu--right au-target is-visible']/div")
    List<AtlasWebElement> statusesOfTask();

    @FindBy("//div[@class='mdl-snackbar au-target']//button[@type='button']")
    AtlasWebElement cancelPopUpWindow();

    @FindBy("//tbody//td[4]/span[1]")
    List<AtlasWebElement> compForNameTsk();
    ///////на редактирование


}


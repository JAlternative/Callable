package pages;

import elements.general.DatePickerForm;
import elements.general.TimePickerForm;
import elements.tasks.FormForPeriod;
import elements.tasks.LeftBarFilter;
import elements.tasks.ListOfTasks;
import elements.tasks.MakerNewTask;
import io.qameta.atlas.webdriver.WebPage;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.AtlasWebElement;

public interface TasksPage extends WebPage, AtlasWebElement {

    @FindBy("//div[@class='mdl-cell mdl-cell--relative mdl-cell--3-col']")
    LeftBarFilter leftBarFilter();

    @FindBy("//div[@class='mdl-layout']")
    MakerNewTask makerNewTak();

    @FindBy("//body/div[@aurelia-app='main']/dialog[@class='mdl-dialog au-target mdl-dialog--720']/div[1]")
    FormForPeriod formForPeriod();

    @FindBy("//div[@class='mdl-cell mdl-cell--relative mdl-cell--9-col au-target']")
    ListOfTasks listOfTasks();

    @FindBy("//*[@class='mddtp-picker mddtp-picker-date']")
    DatePickerForm datePickerForm();

    @FindBy("//*[@class='mddtp-picker mddtp-picker-time'][1]")
    TimePickerForm timePickerForm();

}
